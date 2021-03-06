package com.eden.orchid.netlifycms

import com.eden.common.util.EdenUtils
import com.eden.orchid.api.OrchidContext
import com.eden.orchid.api.compilers.TemplateTag
import com.eden.orchid.api.generators.FileCollection
import com.eden.orchid.api.generators.FolderCollection
import com.eden.orchid.api.generators.OrchidCollection
import com.eden.orchid.api.generators.OrchidGenerator
import com.eden.orchid.api.options.OptionsExtractor
import com.eden.orchid.api.options.annotations.BooleanDefault
import com.eden.orchid.api.options.annotations.Description
import com.eden.orchid.api.options.annotations.Option
import com.eden.orchid.api.options.annotations.StringDefault
import com.eden.orchid.api.render.RenderService
import com.eden.orchid.api.resources.resource.StringResource
import com.eden.orchid.api.theme.components.OrchidComponent
import com.eden.orchid.api.theme.menus.OrchidMenuFactory
import com.eden.orchid.api.theme.pages.OrchidPage
import com.eden.orchid.api.theme.pages.OrchidReference
import com.eden.orchid.netlifycms.model.NetlifyCmsModel
import com.eden.orchid.netlifycms.pages.NetlifyCmsAdminPage
import com.eden.orchid.netlifycms.util.getNetlifyCmsFields
import com.eden.orchid.netlifycms.util.toNetlifyCmsSlug
import com.eden.orchid.utilities.OrchidUtils
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Provider

@Description(
    "Add a fully-configured Netlify CMS to your site, that adapts to your content.",
    name = "Netlify CMS"
)
@JvmSuppressWildcards
class NetlifyCmsGenerator
@Inject
constructor(
    private val extractor: Provider<OptionsExtractor>,
    private val templateTags: Provider<Set<TemplateTag>>,
    private val components: Provider<Set<OrchidComponent>>,
    private val menuFactories: Provider<Set<OrchidMenuFactory>>
) : OrchidGenerator<NetlifyCmsModel>(GENERATOR_KEY, Stage.CONTENT) {

    companion object {
        const val GENERATOR_KEY = "netlifyCms"
    }

    @Option
    @Description("Arbitrary config options to add to the main CMS config file.")
    lateinit var config: JSONObject

    @Option
    @StringDefault("src/orchid/resources")
    @Description("The resource directory, relative to the git repo root, where all resources are located.")
    lateinit var resourceRoot: String

    @Option
    @StringDefault("assets/media")
    @Description("The resource directory, relative to the resourceRoot, where 'media' resources are located.")
    lateinit var mediaFolder: String

    @Option
    @BooleanDefault(true)
    @Description("Whether options of parent classes are included.")
    var includeInheritedOptions: Boolean = true

    @Option
    @BooleanDefault(true)
    @Description("Whether options of its own class are included.")
    var includeOwnOptions: Boolean = true

    @Option
    @BooleanDefault(false)
    @Description("Whether to use the Netlify Identity Widget for managing user accounts. Should be paired with the `git-gateway` backend.")
    var useNetlifyIdentityWidget: Boolean = false

    @Option
    @Description("A list of collections to include. Can be in the format of either 'collectionType' or 'collectionType:collectionId'")
    lateinit var filterCollections: List<String>

    override fun startIndexing(context: OrchidContext): NetlifyCmsModel {
        return NetlifyCmsModel(context, templateTags.get(), components.get(), menuFactories.get())
    }

    override fun startGeneration(context: OrchidContext, model: NetlifyCmsModel) {
        val includeCms: Boolean
        model.useNetlifyIdentityWidget = useNetlifyIdentityWidget

        if (model.isRunningLocally()) {
            config.put("backend", JSONObject())
            config.getJSONObject("backend").put("name", "orchid-server")
            resourceRoot = ""
            includeCms = true
        } else {
            includeCms = config.has("backend")
        }

        if (includeCms) {
            val adminResource = context.getResourceEntry("netlifyCms/admin.peb", null)!!
            adminResource.reference.stripFromPath("netlifyCms")
            val adminPage = NetlifyCmsAdminPage(adminResource, model)
            context.render(adminPage)

            val adminConfig = OrchidPage(
                StringResource(
                    OrchidReference(context, "admin/config.yml"),
                    getYamlConfig(context)
                ),
                RenderService.RenderMode.RAW,
                "admin",
                null
            )
            adminConfig.reference.isUsePrettyUrl = false
            context.render(adminConfig)
        }
    }

    private fun getYamlConfig(context: OrchidContext): String {
        var jsonConfig = JSONObject()

        jsonConfig.put("media_folder", OrchidUtils.normalizePath("$resourceRoot/$mediaFolder"))
        jsonConfig.put("public_folder", "/" + OrchidUtils.normalizePath(mediaFolder))
        jsonConfig.put("collections", JSONArray())

        context.getCollections(filterCollections)
            .forEach { collection ->
                var collectionData = JSONObject()
                val shouldContinue: Boolean = when (collection) {
                    is FolderCollection -> setupFolderCollection(context, collectionData, collection)
                    is FileCollection -> setupFileCollection(context, collectionData, collection)
                    else -> false
                }
                if (shouldContinue) {
                    collectionData.put("label", collection.title)
                    collectionData.put("name", "${collection.collectionType}_${collection.collectionId}")

                    // merge in collection-specific options
                    var queryStrings = arrayOf(
                        "${collection.collectionType}.collections",
                        "${collection.collectionType}.collections.${collection.collectionId}"
                    )

                    queryStrings.forEach { queryString ->
                        var configOptions = context.query(queryString)
                        if (EdenUtils.elementIsObject(configOptions)) {
                            val queryResult = configOptions.element as JSONObject
                            collectionData = EdenUtils.merge(collectionData, queryResult)
                        }
                    }

                    jsonConfig.getJSONArray("collections").put(collectionData)
                }
            }

        jsonConfig = EdenUtils.merge(jsonConfig, config)

        return context.serialize("yaml", jsonConfig.toMap())
    }

    private fun setupFolderCollection(context: OrchidContext, collectionData: JSONObject, collection: FolderCollection): Boolean {
        collectionData.put("folder", OrchidUtils.normalizePath("$resourceRoot/${collection.resourceRoot}"))
        collectionData.put("create", collection.isCanCreate)
        collectionData.put("delete", collection.isCanDelete)
        collectionData.put("slug", collection.slugFormat.toNetlifyCmsSlug())
        collectionData.put(
            "fields",
            extractor.get().describeOptions(
                collection.pageClass,
                includeOwnOptions,
                includeInheritedOptions
            ).getNetlifyCmsFields(context, 2)
        )

        return true
    }

    private fun setupFileCollection(context: OrchidContext, collectionData: JSONObject, collection: FileCollection): Boolean {
        collectionData.put("files", JSONArray())

        collection.getItems().forEach { page ->
            val pageData = JSONObject()
            pageData.put("label", page.title)
            pageData.put("name", page.resource.reference.originalFileName)
            pageData.put(
                "file",
                OrchidUtils.normalizePath("$resourceRoot/${page.resource.reference.originalFullFileName}")
            )
            pageData.put(
                "fields",
                extractor.get().describeOptions(
                    page.javaClass,
                    includeOwnOptions,
                    includeInheritedOptions
                ).getNetlifyCmsFields(context, 2)
            )

            collectionData.getJSONArray("files").put(pageData)
        }
        return true
    }
}
