<!--
SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# Contributing to CC: Tweaked
As with many open source projects, CC: Tweaked thrives on contributions from other people! This document (hopefully)
provides an introduction as to how to get started with helping out.

If you've any other questions, [just ask the community][community] or [open an issue][new-issue].

## Table of Contents
 - [Reporting issues](#reporting-issues)
 - [Setting up a development environment](#setting-up-a-development-environment)
 - [Developing CC: Tweaked](#developing-cc-tweaked)
 - [Writing documentation](#writing-documentation)

## Reporting issues
If you have a bug, suggestion, or other feedback, the best thing to do is [file an issue][new-issue]. When doing so, do
use the issue templates - they provide a useful hint on what information to provide.

## Setting up a development environment
In order to develop CC: Tweaked, you'll need to download the source code and then run it.

 - Make sure you've got the following software installed:
   - Java Development Kit 17 (JDK). This can be downloaded from [Adoptium].
   - [Git](https://git-scm.com/).
   - [NodeJS 20 or later][node].

 - Download CC: Tweaked's source code:
   ```
   git clone https://github.com/cc-tweaked/CC-Tweaked.git
   cd CC-Tweaked
   ```

 - Build CC: Tweaked with `./gradlew build`. This will be very slow the first time it runs, as it needs to download a
   lot of dependencies (and decompile Minecraft several times). Subsequent runs should be much faster!

 - You're now ready to start developing CC: Tweaked. Running `./gradlew :forge:runClient` or
   `./gradle :fabric:runClient` will start Minecraft under Forge and Fabric respectively.

If you want to run CC:T in a normal Minecraft instance, run `./gradlew assemble` and copy the `.jar` from
`projects/forge/build/libs` (for Forge) or `projects/fabric/build/libs` (for Fabric).

## Developing CC: Tweaked
Before making any major changes to CC: Tweaked, I'd recommend you have a read of the [the architecture
document][architecture] first. While it's not a comprehensive document, it gives a good hint of where you should start
looking to make your changes. As always, if you're not sure, [do ask the community][community]!

### Testing
When making larger changes, it may be useful to write a test to make sure your code works as expected.

CC: Tweaked has several test suites, each designed to test something different:

 - In order to test CraftOS and its builtin APIs, we have a test suite written in Lua located at
   `projects/core/src/test/resources/test-rom/`. These don't rely on any Minecraft code, which means they can run on
   emulators, acting as a sort of compliance test.

   These tests are written using a test system called "mcfly", heavily inspired by [busted]. Groups of tests go inside
   `describe` blocks, and a single test goes inside `it`. Assertions are generally written using `expect` (inspired by
   Hamcrest and the like). For instance, `expect(foo):eq("bar")` asserts that your variable `foo` is equal to the
   expected value `"bar"`.

   These tests can be run with `./gradlew :core:test`.

 - In-game functionality, such as the behaviour of blocks and items, is tested using [Minecraft's gametest
   system][mc-test] (`projects/common/src/testMod`). These tests spin up a server, spawn a structure for each test, and
   then run some code on the blocks defined in that structure.

   These tests can be run with `./gradlew runGametest` (or `./gradle :forge:runGametest`/`./gradlew :fabric:runGametest`
   for a single loader).

For more information, [see the architecture document][architecture].

## Writing documentation
When writing documentation for [CC: Tweaked's documentation website][docs], it may be useful to build the documentation
and preview it yourself before submitting a PR.

You'll first need to [set up a development environment as above](#setting-up-a-development-environment).

Once this is set up, you can now run `./gradlew docWebsite`. This generates documentation from our Lua and Java code,
writing the resulting HTML into `./projects/web/build/site`, which can then be opened in a browser. When iterating on
documentation, you can instead run `./gradlew docWebsite -t`, which will rebuild documentation every time you change a
file.

Documentation is built using [illuaminate] which, while not currently documented (somewhat ironic), is largely the same
as [ldoc][ldoc]. Documentation comments are written in Markdown, though note that we do not support many GitHub-specific
markdown features. If you can, do check what the documentation looks like locally!

When writing long-form documentation (such as the guides in [doc/guides](doc/guides)), I find it useful to tell a
narrative. Think of what you want the user to learn or achieve, then start introducing a simple concept, and then talk
about how you can build on that until you've covered everything!

[new-issue]: https://github.com/cc-tweaked/CC-Tweaked/issues/new/choose "Create a new issue"
[community]: README.md#community "Get in touch with the community."
[Adoptium]: https://adoptium.net/temurin/releases?version=17 "Download OpenJDK 17"
[illuaminate]: https://github.com/SquidDev/illuaminate/ "Illuaminate on GitHub"
[docs]: https://tweaked.cc/ "CC: Tweaked documentation"
[ldoc]: http://stevedonovan.github.io/ldoc/ "ldoc, a Lua documentation generator."
[mc-test]: https://www.youtube.com/watch?v=vXaWOJTCYNg
[busted]: https://github.com/Olivine-Labs/busted "busted: Elegant Lua unit testing."
[node]: https://nodejs.org/en/ "Node.js"
[architecture]: projects/ARCHITECTURE.md
