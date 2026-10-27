<!--
SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

This is the documentation for CC: Tweaked $modVersion for Minecraft $mcVersion. Documentation for other versions of
Minecraft are available on the CC: Tweaked website:

- [Minecraft 1.20.1](/mc-1.20.x/javadoc/)
- [Minecraft 1.21.1](/mc-1.21.x/javadoc/)
- [Minecraft 26.3](/mc-26.3/javadoc/)

# Quick links

You probably want to start in the following places:

- [Registering new peripherals][dan200.computercraft.api.peripheral]
- [dan200.computercraft.api.lua.LuaFunction] and [dan200.computercraft.api.lua.IArguments] for adding methods to your
  peripheral or Lua objects.
- [Turtle upgrades][dan200.computercraft.api.turtle.ITurtleUpgrade]
- [Pocket upgrades][dan200.computercraft.api.pocket.IPocketUpgrade]

# Using
CC: Tweaked is hosted on my maven repo, and so is relatively simple to depend on. You may wish to add a soft (or hard)
dependency in your `mods.toml` file, with the appropriate version bounds, to ensure that API functionality you depend on
is present.

```groovy
repositories {
    maven {
        url "https://maven.squiddev.cc"
        content { includeGroup("cc.tweaked") }
    }
}

dependencies {
    // Vanilla (i.e. for multi-loader systems)
    compileOnly("cc.tweaked:cc-tweaked-$mcVersion-common-api:$modVersion")

    // Forge Gradle
    compileOnly("cc.tweaked:cc-tweaked-$mcVersion-core-api:$modVersion")
    compileOnly(fg.deobf("cc.tweaked:cc-tweaked-$mcVersion-forge-api:$modVersion"))
    runtimeOnly(fg.deobf("cc.tweaked:cc-tweaked-$mcVersion-forge:$modVersion"))

    // Fabric Loom
    modCompileOnly("cc.tweaked:cc-tweaked-$mcVersion-fabric-api:$modVersion")
    modRuntimeOnly("cc.tweaked:cc-tweaked-$mcVersion-fabric:$modVersion")
}
```

You should also be careful to only use classes within the `dan200.computercraft.api` package. Non-API classes are
subject to change at any point. If you depend on functionality outside the API (or need to mixin to CC:T), please
[start a discussion](https://github.com/cc-tweaked/CC-Tweaked/discussions/new/choose) to let me know!
