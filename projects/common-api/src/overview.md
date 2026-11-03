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
# Updating from Minecraft 1.20.1 to 1.21.1

## Peripherals

- On NeoForge, the peripheral capability has migrated to NeoForge's new capability system.
  `dan200.computercraft.api.peripheral.PeripheralCapability` can be used to register a peripheral. `IPeripheralProvider`
  has also been removed, as capabilities can now be used for arbitrary blocks.

[Read more on registering peripherals][dan200.computercraft.api.peripheral].

## Turtle and pocket upgrades

Turtle and pocket upgrades have been migrated to use Minecraft's dynamic registries. While upgrades themselves have not
changed much, the interface for registering them is dramatically different.

- `TurtleUpgradeSerialiser` and `PocketUpgradeSerialiser` have been unified into a single
  [dan200.computercraft.api.upgrades.UpgradeType] class.

  - Replace `TurtleUpgradeSerialiser.registryId()` with [dan200.computercraft.api.turtle.ITurtleUpgrade#typeRegistry()]
    and `PocketUpgradeSerialiser.registryId()` with [dan200.computercraft.api.pocket.IPocketUpgrade#typeRegistry()].
  - Replace all other usages of `TurtleUpgradeSerialiser` and `PocketUpgradeSerialiser` with
    [dan200.computercraft.api.upgrades.UpgradeType].

- Upgrades are now (de)serialised using codecs, rather than manually reading from JSON and encoding/decoding network
  packets. Instead of subclassing [dan200.computercraft.api.upgrades.UpgradeType], it is recommended you use
  [dan200.computercraft.api.upgrades.UpgradeType#create] to create a new type from a `MapCodec`.

- Upgrades are no longer aware of their ID, and so cannot compute their adjective. The adjective must now either be
  hard-coded, or read as part of the codec.

- The upgrade data providers have been removed, in favour of mod loader's built-in support for dynamic registries. I'm
  afraid it's probably easier if you delete your existing upgrade datagen code and start from scratch. See
  [the `ITurtleUpgrade` documentation for an example](./dan200/computercraft/api/turtle/ITurtleUpgrade.html#datagen).

- Upgrades now store their additional data ([dan200.computercraft.api.turtle.ITurtleAccess#getUpgradeData],
  [dan200.computercraft.api.pocket.IPocketAccess#getUpgradeData()]) as an immutable component map, rather than a
  compound tag.

[Read more on registering turtle upgrades][dan200.computercraft.api.turtle.ITurtleUpgrade].
