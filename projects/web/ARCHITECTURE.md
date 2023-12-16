<!--
SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# Architecture
As mentioned in the main architecture guide, the web subproject is responsible for building CC: Tweaked's documentation
website. This is surprisingly more complex than one might initially assume, hence the need for this document at all!

## Web-based emulator
Most of the complexity comes from the web-based emulator we embed in our documentation. This uses [TeaVM] to compile
CC: Tweaked's core to Javascript, and then call out to it in the main site.

The code for this is split into three separate components:
 - `src/main`: This holds the emulator itself: this is a basic Java project which depends on CC:T's core, and exposes an
   interface for Javascript code.

   Some of our code (or dependencies) cannot be compiled to Javascript, for instance most of our HTTP implementation. In
   theses cases we provide a replacement class. These classes start with `T` (for instance `THttpRequest`), which are
   specially handled in the next step.

 - `src/builder`: This module consumes the above code and compiles everything to Javascript using TeaVM. There's a
    couple of places where we need to patch the bytecode before compiling it, so this also includes a basic ASM
    rewriting system.

 - `src/frontend`: This consumes the interface exposed by the main emulator, and actually embeds the emulator in the
   website.

## Static content
Rendering the static portion of the website is fortunately much simpler.

 - Doc generation: This is mostly handled in various Gradle files. The `common` project uses [cct-javadoc] to convert
   Javadoc on our peripherals and APIs to LDoc/[illuaminate] compatible documentation. This is then fed into illuaminate
   which spits out HTML.

 - `src/htmlTransform`: We do a small amount of post-processing on the HTML, which is performed by this tool. This includes
   syntax highlighting of non-Lua code blocks, and replacing special `<mc-recipe>` tags with a rendered view of a given
   Minecraft recipe.

[TeaVM]: https://github.com/konsoletyper/teavm "TeaVM - Compiler of Java bytecode to JavaScript"
[cct-javadoc]: https://github.com/cc-tweaked/cct-javadoc: "cct-javadoc - A Javadoc doclet to extract documentation from @LuaFunction methods."
[illuaminate]: https://github.com/Squiddev/illuaminate: "illuaminate - Very WIP static analysis for Lua"
