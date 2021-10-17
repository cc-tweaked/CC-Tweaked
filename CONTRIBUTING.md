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
 - **[illuaminate]:** Checks Lua code for semantic and styleistic issues. See [the usage section][illuaminate-usage] for
   how to download and run it. You may need to generate the Java documentation stubs (see "Documentation" below) for all
   lints to pass.

### Documentation
When writing documentation for [CC: Tweaked's documentation website][docs], it may be useful to build the documentation
and preview it yourself before submitting a PR.

Building all documentation is, sadly, a multi-stage process (though this is largely hidden by Gradle). First we need to
convert Java doc-comments into Lua ones, we also generate some Javascript to embed. All of this is then finally fed into
illuaminate, which spits out our HTML.

#### Setting up the tooling
For various reasons, getting the environment set up to build documentation can be pretty complex. I'd quite like to
automate this via Docker and/or nix in the future, but this needs to be done manually for now.

This tooling is only needed if you need to build the whole website. If you just want to generate the Lua stubs, you can
skp this section.
 - Install Node/npm and install our Node packages with `npm ci`.
 - Install [illuaminate][illuaminate-usage] as described above.

#### Building documentation
Gradle should be your entrypoint to building most documentation. There's two tasks which are of interest:

 - `./gradlew luaJavadoc` - Generate documentation stubs for Java methods.
 - `./gradlew docWebsite` - Generate the whole website (including Javascript pages). The resulting HTML is stored at
   `./build/docs/lua/`.

#### Writing documentation
illuaminate's documentation system is not currently documented (somewhat ironic), but is _largely_ the same as
[ldoc][ldoc]. Documentation comments are written in Markdown,

Our markdown engine does _not_ support GitHub flavoured markdown, and so does not support all the features one might
expect (such as tables). It is very much recommended that you build and preview the docs locally first.

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

 - In-game (`./src/testMod/java/dan200/computercraft/ingame/`): These tests are run on an actual Minecraft server and client,
   using [the same system Mojang do][mc-test]. The aim of these is to test in-game behaviour of blocks and peripherals.

   These are run by `./gradlew testClient` and `./gradlew testServer`. You may want to run the client under `xvfb-run`
   or similar when running in a headless environment.

## CraftOS tests
CraftOS's tests are written using a test system called "mcfly", heavily inspired by [busted] (and thus RSpec). Groups of
tests go inside `describe` blocks, and a single test goes inside `it`.

Assertions are generally written using `expect` (inspired by Hamcrest and the like). For instance, `expect(foo):eq("bar")`
asserts that your variable `foo` is equal to the expected value `"bar"`.

[new-issue]: https://github.com/cc-tweaked/CC-Tweaked/issues/new/choose "Create a new issue"
[community]: README.md#Community "Get in touch with the community."
[checkstyle]: https://checkstyle.org/
[illuaminate]: https://github.com/SquidDev/illuaminate/ "Illuaminate on GitHub"
[illuaminate-usage]: https://github.com/SquidDev/illuaminate/blob/master/README.md#usage "Installing Illuaminate"
[weblate]: https://i18n.tweaked.cc/projects/cc-tweaked/minecraft/ "CC: Tweaked weblate instance"
[docs]: https://tweaked.cc/ "CC: Tweaked documentation"
[ldoc]: http://stevedonovan.github.io/ldoc/ "ldoc, a Lua documentation generator."
[mc-test]: https://www.youtube.com/watch?v=vXaWOJTCYNg
[busted]: https://github.com/Olivine-Labs/busted "busted: Elegant Lua unit testing."
