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

    // NeoForge
    compileOnly("cc.tweaked:cc-tweaked-$mcVersion-forge-api:$modVersion")
    runtimeOnly("cc.tweaked:cc-tweaked-$mcVersion-forge:$modVersion")

    // Fabric
    compileOnly("cc.tweaked:cc-tweaked-$mcVersion-fabric-api:$modVersion")
    runtimeOnly("cc.tweaked:cc-tweaked-$mcVersion-fabric:$modVersion")
}
```

You should also be careful to only use classes within the `dan200.computercraft.api` package. Non-API classes are
subject to change at any point. If you depend on functionality outside the API (or need to mixin to CC:T), please
[start a discussion](https://github.com/cc-tweaked/CC-Tweaked/discussions/new/choose) to let me know!

# Updating from Minecraft 1.21.1 to 26.3

## Turtle and pocket upgrades
In CC: Tweaked 1.121.0, upgrades were changed to store the item they were originally equipped with (alongside their
upgrade data), along with changing how upgrade data is read/written. This was done in a backwards-compatible manner on
older Minecraft versions. The Minecraft 26.3 version finishes this migration, removing the deprecated methods for
interacting with upgrade data (e.g. `ITurtleAccess.setUpgradeData`). It is recommended you upgrade to CC: Tweaked 1.121.0
and fix any deprecations before updating Minecraft.

 - `UpgradeBase.getCraftingItem` has been removed in favour of [dan200.computercraft.api.upgrades.UpgradeBase#getCraftingIngredient()],

   - Remove implementations of `getUpgradeItem` from your upgrade. For compatibility with older saves, you may want to
     keep your implementation of [dan200.computercraft.api.upgrades.UpgradeBase#getUpgradeItem(DataComponentPatch)].
   - Replace usages of `AbstractTurtleUpgrade.StackAware` with `AbstractTurtleUpgrade` and similarly
     `AbstractPocketUpgrade.StackAware` with `AbstractPocketUpgrade.StackAware`.
   - Change your upgrade's constructor to accept an [ingredient][net.minecraft.world.item.crafting.Ingredient] instead of
     an item or item stack.
   - Replace usages of `UpgradeType.simpleWithCustomItem` with [dan200.computercraft.api.upgrades.UpgradeType#simpleWithCustomIngredient].

 - `ITurtleAccess.getUpgradeData`/`IPocketAccess.getUpgradeData` and `ITurtleAccess.setUpgradeData`/`IPocketAccess.setUpgradeData`
   have been removed. Replace these with a call to [dan200.computercraft.api.turtle.ITurtleAccess#getUpgradeHandle(TurtleSide)]/
   [dan200.computercraft.api.pocket.IPocketAccess#getUpgradeHandle()], then [dan200.computercraft.api.upgrades.UpgradeHandle#getData()]
   and [dan200.computercraft.api.upgrades.UpgradeHandle#setData(DataComponentPatch)].

   Note these new methods can only be called on the main/server thread. You should **NOT** interact with them from off-thread
   peripheral calls.
