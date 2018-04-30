# Contributing

Contributions of any kind are always welcome. Whether it is a code, design, testing, or documentation, or any other
change, please review the following guidelines for contributing before submitting your changes. 

## Overview 

This repository is comprised of many individual projects, which are all listed above. You can build and run any project
with Gradle from the project root, such as `gradle :OrchidCore:assemble` or `gradle :plugins:OrchidWiki:assemble`, or 
you may navigate to a particular project's subdirectory to run the Gradle commands directly. When contributing code, 
please indent using 4 spaces.

Orchid releases are deployed continuously. With any push to the `master` branch, the patch version is incremented, then
the compiled sources are uploaded to Bintray and a git tag created and uploaded to Github Releases. All code should be 
deployed via pull request. All Orchid plugins, themes, bundles, core, and Gradle plugins are released at the same 
version with every release.

Documentation updates and submissions for new themes or plugins should be made as PRs to the `docs` branch. 
Documentation is hosted on Netlify, and a deploy preview will be made for your PR before it is merged into `docs`. All
other changes should be made as PRs to the `dev` branch.

Orchid uses the [Gradle Semantic Versioning](https://github.com/vivin/gradle-semantic-build-versioning) plugin to set
the package version and create the git tag. The major and minor versions can be incremented by including `[major]` or
`[minor]` at the start of any commit message since the previous released version. Please refrain from using these 
special tags in your commit messages.

## Setup and Running

You will need a Java 8+ JDK installed on your system, then:

**Setup**
```bash
git clone https://github.com/JavaEden/Orchid.git
cd Orchid
```

**Run (serve, without Javadoc)**
```bash
./gradlew assemble :OrchidCore:orchidServe -PnoJavadoc=true
```

**Run (serve, with Javadoc)**
```bash
./gradlew :OrchidCore:assemble -PorchidRunTask=serve
```

**Run Tests**
```bash
./gradlew check
```

## Sub-Project Types

Orchid is maintained as a multi-project Gradle build. Each sub-project, with the exception of the Bundle projects, can 
run the above commands targeted at their specific repo. For testing functionality of a single plugin or theme, this may
be faster than testing the functionality in the Core sub-project.

**Core (`:OrchidCore`)**

This is the core project, which is required by all other projects. It contains the core APIs which all other 
sub-projects plug into. It also contains the sources for the main documentation site.

Core does not depend on any other projects for compiling, but includes all plugins and themes in its own Orchid build. 

**Themes (`:themes:[Theme Name]`)**

This group of projects has all the official Orchid themes. Each theme is dependant on the Core project, but no 
particular plugins. However, even though themed do not have a code dependency on any Plugin projects, they may include
snippets in its templates that are designed to be used with particular plugins (searching, for example). However, this
is not a strict dependency as any template may be overridden to remove this issue.

**Plugins (`:plugins:[Plugin Name]`)**

This group of projects has all the official Orchid plugins. Each theme is dependant on the Core project, but nothing 
else. No plugin should ever assume any other plugin is installed, either by code or by template, in the desire that each
plugin is entirely optional.

**Language Extensions (`:languageExtensions:[Plugin Name]`)**

Projects in the Language Extensions group are technically equivalent to the Plugins group, but have a different 
semantic meaning. Everything true of a Plugin sub-project is true of a Language Extension sub-project in that they must
be able to be used independently, but these sub-projects specifically add new languages to Orchid rather than features.

"Languages" are typically things that allow processing of new file extensions or add new Template Tags or Template 
Filters, things that the end-user would be interested in using rather than the site developer.

**Bundles (`:bundles:[Bundle Name]`)**

This group of projects has all the official Orchid bundles. Bundles themselves have no code and no templates, and just
serve as a grouping of other Orchid plugins and themes, to easily set up the various dependencies for common Orchid site
configurations.

**Runners (`buildSrc/`)**

The Orchid Runners is a special category of projects, which reside in the `buildSrc/` folder of the Gradle project. This
means that the sub-projects of `builSrc/` are not first-class projects of the root Gradle project, but rather its own
Gradle project which is included in the runtime of the root Gradle project.

The Gradle sub-projects in `buildSrc/` are the different Gradle plugins which run Orchid. Other runners may be added for
other build systems, but they will necessarily have to be managed outside of Gradle.

## Pull Requests

Orchid's release cycle is fully automated, so please be careful to set up your commits and pull requests properly. 

**Documentation Changes**

Documentation changes should be made as a PR to the `docs` branch. Documentation is built and hosted on 
[Netlify](https://www.netlify.com/). Each commit to `docs` will be immediately deployed, and each PR to `docs` will 
generate a preview deploy for you to verify your changes before they are accepted. It typically takes around 15 minutes 
for the full build to complete and be available to preview. For this reason, larger documentation changes should be run
and previewed locally before opening the PR. 

Documentation changes are regularly synced with the `dev` branch so that in the long-term, all branches have the same
content. In the short term, however, this separate branch allows us to keep unstable features from breaking our 
documentation, as well as allowing documentation updates to be deployed immediately.

**Code Changes**

Code changes should be made as a PR to the `dev` branch. Each commit to the `dev` branch will run tests on Travis CI, 
but will not run Orchid itself on the sub-projects. This is to keep the development feedback cycle quick. The Travis CI
build typically completes in around 5 minutes. A passing Travis CI build is required before a PR will be accepted.

After all tests are complete, test coverage reports will be uploaded to [Codacy](https://www.codacy.com), and the source
code analyzed for code quality. A positive Codacy report is not required for a PR to be accepted, but it is good to take
note of the issues it finds to see if there are any major issues with the PR.

## Deployment

A new version of Orchid will be created with each commit to the `master` branch. Travis CI will run all tests on the new
version, run Orchid on those projects, upload test coverage reports to Codacy, and then build and deploy artifacts to 
Bintray. It will also pull the generated Orchid sites from all sub-projects into a single archive, then create a Release
on Github with that archive attached. 

Orchid uses Semantic Versioning, and by default each new release is a "patch" release. Including `[minor]` or `[major]`
at the start of a commit message will increase the "minor" or "major" version instead.