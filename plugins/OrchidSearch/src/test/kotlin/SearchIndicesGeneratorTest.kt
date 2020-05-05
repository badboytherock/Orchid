package com.eden.orchid.search

import com.eden.orchid.impl.generators.HomepageGenerator
import com.eden.orchid.kotlindoc.KotlindocModule
import com.eden.orchid.pages.PagesModule
import com.eden.orchid.posts.PostsModule
import com.eden.orchid.sourcedoc.SourceDocModule
import com.eden.orchid.strikt.pageWasRendered
import com.eden.orchid.testhelpers.OrchidIntegrationTest
import com.eden.orchid.testhelpers.withGenerator
import com.eden.orchid.wiki.WikiModule
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains

@DisplayName("Tests page-rendering behavior of Kotlindoc generator")
class SearchIndicesGeneratorTest : OrchidIntegrationTest(
    SearchModule(),
    KotlindocModule(),
    SourceDocModule(),
    PostsModule(),
    PagesModule(),
    WikiModule(),
    withGenerator<HomepageGenerator>()
) {

    @Test
    @DisplayName("Kotlin and Java files are parsed, and pages are generated for each class and package.")
    fun test01() {
        configObject(
            "kotlindoc",
            """
            |{
            |    "sourceDirs": [
            |        "./../../OrchidKotlindoc/src/mockKotlin",
            |        "./../../OrchidJavadoc/src/mockJava",
            |    ]
            |}
            |""".trimMargin()
        )

        configObject(
            "theme",
            """
            |{
            |    "metaComponents": [
            |        {"type": "orchidSearch"}
            |    ]
            |}
            |""".trimMargin()
        )

        resource(
            "templates/layouts/index.peb",
            """
            |<!DOCTYPE HTML>
            |<html>
            |<head>
            |{% head %}
            |</head>
            |<body>
            |{% body %}
            |    {% page %}
            |    <section id="search" >
            |        <form data-orchid-search>
            |            <div class="input-group">
            |                <input type="text" name="query" id="query" class="form-control" placeholder="Search&hellip;">
            |                <span class="input-group-btn"><button type="submit" class="btn btn-default">Go</button></span>
            |            </div>
            |        </form>
            |        <div id="search-results" data-orchid-search-results style="display: none;">
            |            <b>Search Results</b>
            |            <ul></ul>
            |        </div>
            |        <div id="search-progress" data-orchid-search-progress style="display: none;">
            |            <div class="loader">Loading...</div>
            |        </div>
            |    </section>
            |{% endbody %}
            |</body>
            |</html>
            |""".trimMargin()
        )

        resource(
            "homepage.md",
            """
            |This is searchable text from the homepage
            """.trimMargin()
        )
        resource(
            "pages/page-one.md",
            """
            |This is searchable text from static page 1
            """.trimMargin()
        )
        resource(
            "posts/2020-04-22-post-one.md",
            """
            |This is searchable text from blog post 1
            """.trimMargin()
        )

        resource(
            "wiki/summary.md",
            """
            |This is searchable text from the wiki summary
            |[wiki entry one](wiki-one.md)
            """.trimMargin()
        )
        resource(
            "wiki/wiki-one.md",
            """
            |This is searchable text from the wiki entry page
            """.trimMargin()
        )

        expectThat(execute())
            // Module readme
            .pageWasRendered("/meta/home.index.json") {
                get { content }.contains("This is searchable text from the homepage")
            }
            .pageWasRendered("/meta/pages.index.json") {
                get { content }.contains("This is searchable text from static page 1")
            }
            .pageWasRendered("/meta/posts.index.json") {
                get { content }.contains("This is searchable text from blog post 1")
            }
            .pageWasRendered("/meta/wiki.index.json") {
                get { content }.contains("This is searchable text from the wiki summary").contains("This is searchable text from the wiki entry page")
            }
            .pageWasRendered("/meta/kotlindoc.index.json") {
                get { content }.contains("This class <i>freaking awesome<\\/i> class links to")
            }
    }
}
