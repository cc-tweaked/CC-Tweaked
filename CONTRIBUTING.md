# Contributing to CC: Tweaked
As with many open source projects, CC: Tweaked thrives on contributions from other people! This document (hopefully)
provides an introduction as to how to get started in helping out.

If you've any other questions, [just ask the community][community] or [open an issue][new-issue].

## Reporting issues
If you have a bug, suggestion, or other feedback, the best thing to do is [file an issue][new-issue]. When doing so,
do use the issue templates - they provide a useful hint on what information to provide.

## Translations
Translations are managed through [Weblate], an online interface for managing language strings. This is synced
automatically with GitHub, so please don't submit PRs adding/changing translations!

## Developing
In order to develop CC: Tweaked, you'll need to download the source code and then run it. This is a pretty simple
process. When building on Windows, Use `gradlew.bat` instead of `./gradlew`.

 - **Clone the repository:** `git clone https://github.com/cc-tweaked/CC-Tweaked.git && cd CC-Tweaked`
 - **Setup Forge:** `./gradlew build`
 - **Run Minecraft:** `./gradlew runClient` (or run the `GradleStart` class from your IDE).
 - **Optionally:** For small PRs (especially those only touching Lua code), it may be easier to use GitPod, which
   provides a pre-configured environment: [![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-2b2b2b?logo=gitpod)](https://gitpod.io/#https://github.com/cc-tweaked/CC-Tweaked/)

   Do note you will need to download the mod after compiling to test.

If you want to run CC:T in a normal Minecraft instance, run `./gradlew build` and copy the `.jar` from `build/libs`.
These commands may take a few minutes to run the first time, as the environment is set up, but should be much faster
afterwards.

The following sections describe the more niche sections of CC: Tweaked's build system. Some bits of these are
quite-complex, and (dare I say) over-engineered, so you may wish to ignore them. Well tested/documented PRs are always
preferred (and I'd definitely recommend setting up the tooling if you're doing serious development work), but for
small changes it can be a lot.

### Code linters
CC: Tweaked uses a couple of "linters" on its source code, to enforce a consistent style across the project. While these
are run whenever you submit a PR, it's often useful to run this before committing.

 - **[Checkstyle]:** Checks Java code to ensure it is consistently formatted. This can be run with `./gradlew build` or
   `./gradle check`.
 - **[illuaminate]:** Checks Lua code for semantic and styleistic issues. This can be run with `./gradlew lintLua`.

### Documentation
When writing documentation for [CC: Tweaked's documentation website][docs], it may be useful to build the documentation
and preview it yourself before submitting a PR.

Our documentation generation pipeline is rather complex, and involves invoking several external tools. Most of this
complexity is hidden by Gradle, but you will need to perform some initial setup:

 - Install [Node/npm][node].
 - Run `npm ci` to install our Node dependencies.

You can now run `./gradlew docWebsite`. This generates documentation from our Lua and Java code, writing the resulting
HTML into `./build/docs/site`.

#### Writing documentation
illuaminate's documentation system is not currently documented (somewhat ironic), but is _largely_ the same as
[ldoc][ldoc]. Documentation comments are written in Markdown,

Our markdown engine does _not_ support GitHub flavoured markdown, and so does not support all the features one might
expect. It is recommended that you build and preview the docs locally first.

When iterating on documentation, you can get Gradle to rebuild the website every time a file changes by running
`./gradlew docWebsite -t`. This will take a couple of seconds to run, but definitely beats running it manually!

### Testing
Thankfully running tests is much simpler than running the documentation generator! `./gradlew check` will run the
entire test suite (and some additional bits of verification).

Before we get into writing tests, it's worth mentioning the various test suites that CC: Tweaked has:
 - "Core" Java (`./src/test/java`): These test core bits of the mod which don't require any Minecraft interaction.
   This includes the `@LuaFunction` system, file system code, etc...

   These tests are run by `./gradlew test`.

 - CraftOS (`./src/test/resources/test-rom/`): These tests are written in Lua, and ensure the Lua environment, libraries
   and programs work as expected. These are (generally) written to be able to be run on emulators too, to provide some
   sort of compliance test.

   These tests are run by the '"Core" Java' test suite, and so are also run with `./gradlew test`.

 - In-game (`./src/testMod/java/dan200/computercraft/ingame/`): These tests are run on an actual Minecraft server, using
   the same system Mojang do][mc-test]. The aim of these is to test in-game behaviour of blocks and peripherals.

   These tests are run with `./gradlew testServer`.

## CraftOS tests
CraftOS's tests are written using a test system called "mcfly", heavily inspired by [busted] (and thus RSpec). Groups of
tests go inside `describe` blocks, and a single test goes inside `it`.

Assertions are generally written using `expect` (inspired by Hamcrest and the like). For instance, `expect(foo):eq("bar")`
asserts that your variable `foo` is equal to the expected value `"bar"`.

[new-issue]: https://github.com/cc-tweaked/CC-Tweaked/issues/new/choose "Create a new issue"
[community]: README.md#Community "Get in touch with the community."
[checkstyle]: https://checkstyle.org/
[illuaminate]: https://github.com/SquidDev/illuaminate/ "Illuaminate on GitHub"
[weblate]: https://i18n.tweaked.cc/projects/cc-tweaked/minecraft/ "CC: Tweaked weblate instance"
[docs]: https://tweaked.cc/ "CC: Tweaked documentation"
[ldoc]: http://stevedonovan.github.io/ldoc/ "ldoc, a Lua documentation generator."
[mc-test]: https://www.youtube.com/watch?v=vXaWOJTCYNg
[busted]: https://github.com/Olivine-Labs/busted "busted: Elegant Lua unit testing."
[node]: https://nodejs.org/en/ "Node.js"
