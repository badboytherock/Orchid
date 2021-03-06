---
---

### Configuration

An example of single-module configuration is below. 

```yaml
kotlindoc:
  name: 'Project Name'
  sourceDirs: ['../module-one/src/main/kotlin', '../module-one/src/main/java']
  homePagePermalink: '...'
  sourcePagePermalink: '...'
  showRunnerLogs: false
  homePageOnly: false
  args: ['-packageOptions', 'com.example.hidden,+suppress;com.example.alsohidden,+suppress']
```

| Description                                                             | Option                | Default Value |
| ----------------------------------------------------------------------- | --------------------- | ------------- |
| The name of the module, which becomes the title of the module Homepage. | `name`                | `false`       |
| Relative paths to directories containing Kotlin or Java source code.    | `sourceDirs`          | `[]`          |
| Customize the URL path for the homepage.                                | `homePagePermalink`   | see below     |
| Customize the URL path for class and package pages.                     | `sourcePagePermalink` | see below     |
| Whether to print logs from the underlying Dokka CLI                     | `showRunnerLogs`      | `false`       |
| If true, do not run Dokka. Create docs a module with only a homepage.   | `homePageOnly`        | `false`       |
| Additional args to pass-through to the Dokka CLI.                       | `args`                | `[]`          |

### Permalinks

Permalinks can be customized for all pages generated by this plugin. For each module, you can set `homePagePermalink` or
`sourcePagePermalink` with your desired permalink string. The following dynamic path segments are available for these
pages (in addition to the standard permalink keys):

- `:moduleType` - The "type" of source code you're documenting (`kotlindoc`) 
- `:moduleGroup` - Not used for single-module docs 
- `:module` - Not used for single-module docs 
- `:sourceDocPath` - The path for each source code doc page, as defined by the Kotlindoc plugin

The default permalinks are as follows:

| Description       | Permalink pattern                                 | Example                              |
| ----------------- | ------------------------------------------------- | ------------------------------------ |
| Module home pages | `:moduleType/:moduleGroup/:module`                | `/kotlindoc`                         |
| Source code pages | `:moduleType/:moduleGroup/:module/:sourceDocPath` | `/kotlindoc/com/app/mainapplication` |

### Page Archetypes

The pages generated have several archetype keys available, for applying options to these pages in varying levels of 
granularity:

| Description                                               | Archetype Pattern           |
| --------------------------------------------------------- | --------------------------- |
| Both module home and source code pages for all languages  | `sourcedoc.pages`           |
| Just module home pages for all languages                  | `sourcedoc.moduleHomePages` |
| Just source code pages for all languages                  | `sourcedoc.sourcePages`     |
| Kotlin module home and source code pages                  | `kotlindoc.pages`           |
| Just Kotlin module home pages                             | `kotlindoc.moduleHomePages` |
| Just Kotlin source code pages                             | `kotlindoc.sourcePages`     |
| Just Kotlin Class source code pages                       | `kotlindoc.classesPages`    |
| Just Kotlin Package source code pages                     | `kotlindoc.packagesPages`   |

### Collections

SourceDocs generate a variety of collections which allow you to precisely query these pages. All collections will have a 
`collectionType` of the `moduleType` (`kotlindoc`).  

For a single module, you will get a collection with collectionId of the moduleType. For each of these collections, it 
will also contain related collections containing the source pages of a specific page types, `classes` and `packages`. 
In addition, you will have a `modules` collection, which contains the READMEs for each module.
 
| Description                 | Collection Type | Collection ID        |
| --------------------------- | --------------- | -------------------- |
| All Kotlindoc pages         | `kotlindoc`     | `kotlindoc`          |
| All Kotlindoc class pages   | `kotlindoc`     | `kotlindoc-classes`  |
| All Kotlindoc package pages | `kotlindoc`     | `kotlindoc-packages` |
| The Kotlindoc README page   | `kotlindoc`     | `kotlindoc-modules`  |

### Menu Items

There are 3 types of menu items available 

#### Modules

Create a menu item linking to the module home pages. Typically added to the Theme menu so it is added to all pages.

```yaml
theme:
  menu:
    - type: 'sourcedocModules'
      moduleType: 'kotlindoc'
```

#### Pages

Create a menu item linking to all the source pages for a single module. You can optionally filter it by page type, 
or include all source pages for that module. Typically added to the Theme menu so it is added to all pages.

```yaml
theme:
  menu:
    - type: 'sourcedocPages'
      moduleType: 'kotlindoc'
      node: 'classes' # optional
```

By default, all menu items will display the title of the linked page, which is typically a human-readable version of the 
documented element. You can set `itemTitleType: 'ID'` to have it display the actual page element ID.

```yaml
theme:
  menu:
    - type: 'sourcedocPages'
      moduleType: 'kotlindoc'
      node: 'classes'
      itemTitleType: 'ID'
``` 

In addition, you can provide an inline Pebble template to render/transform the menu item title however you need. You 
have access to the original `title`, the target `page`, and the `element` within that template.

```yaml
theme:
  menu:
    - type: 'sourcedocPages'
      moduleType: 'kotlindoc'
      node: 'classes'
      itemTitleType: 'ID'
      transform: '{{ title|replace({"com.eden.orchid": "c.e.o"}) }}'
```

#### Page Links

Page Links adds menu item links to a single SourceDoc Page. It links to each "section" on that page (such as `methods`, 
`constructors`, or `fields` on a `classes` page). Setting `includeItems: true` will optionally include all that items
for each section. It is typically added to a Page's menu item through one of the above Archetypes. 

Just like `sourcedocPages`, you can customize how the menu item text is rendered with `itemTitleType`. It can be `NAME`
for the element's human-readable name, `ID` for the actual element ID, or `signature` to display the fully-formatted 
element signature.

```yaml
kotlindoc:
  sourcePages:
    menu:
      - type: 'sourcedocPageLinks'
        itemTitleType: 'signature' # optional, one of [NAME, ID, SIGNATURE]
        includeItems: true # optional
```

### Filtering Docs

Orchid Kotlindoc uses Dokka to convert source code into documentation, so docs are filtered using the methods available
to Dokka: extra CLI args to configure per package, and `@suppress` comments in the code to ignore specific elements.

#### Dokka Extra CLI Args

Orchid runs Dokka from its CLI, and additional args can be passed to it for extra configuration. Most commonly, this 
is used for adding `-packageOptions` arguments to customizing which packages are documented, and the visibility level of
the elements in that package. You can also pass any of 
[Dokka's CLI args](https://github.com/kotlin/Dokka#using-the-command-line). They are passed as a freeform list of CLI 
args to the `args` property of the module configuration. 

```yaml
kotlindoc:
  name: 'Project Name' 
  sourceDirs: ['../module-one/src/main/kotlin', '../module-one/src/main/java']
  args:
    - '-packageOptions'
    - 'com.example.hidden,+suppress;com.example.alsohidden,+suppress'
```

The format of these args is: `['-packageOptions', '{semicolon-separated list of package options}']`, where:

- `{semicolon-separated list of package options}`: `{package prefix},{comma-separated list of options}`
- `{package prefix}`: specifies that a package and all of its subpackages will be configured
- `{comma-separated list of options}`: are listed below:

| Description                                                 | CLI value             | Default |
| ----------------------------------------------------------- | --------------------- | ------- |
| Include private and internal members                        | `+privateApi`         |         |
| Exclude private and internal members                        | `-privateApi`         |    ✔    |
| Log warnings for elements with missing KDoc comments        | `+warnUndocumented`   |         |
| Do not log warnings for elements with missing KDoc comments | `-warnUndocumented`   |    ✔    |
| Include elements annotated with `@Deprecated`               | `+deprecated`         |         |
| Do not include elements annotated with `@Deprecated`        | `-deprecated`         |    ✔    |
| Skip this package entirely                                  | `+suppress`           |         |
| Include this package                                        | `-suppress`           |    ✔    |

#### Dokka `@suppress` Comments

In addition to customizing the packageOptions through CLI args, you can add `@suppress` to any individual classes, 
constructors, properties, or functions to keep them out of the generated docs.

##### Suppress Classes

```kotlin
/**
 * @suppress
 */
class SuppressedKotlinClass

class VisibleKotlinClass
```

##### Suppress Constructors

```kotlin
class KotlinClass(val s1: String) {
    /**
     * @suppress
     */
    constructor() : this("")
}
```

##### Suppress Properties

```kotlin
class KotlinClass() {
    /**
     * @suppress
     */
    val suppressedProperty: String
    
    val visibleProperty: String

}
```

##### Suppress Functions

```kotlin
/**
 * @suppress
 */
fun suppressedTopLevelFunction: String = ""

fun visibleTopLevelFunction: String = ""

class KotlinClass() {
    /**
     * @suppress
     */
    fun suppressedFunction: String = ""
    
    fun visibleFunction: String = ""
}
```
