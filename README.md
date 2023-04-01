<!--
SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# ![CC: Tweaked](doc/logo.png)
[![Current build status](https://github.com/cc-tweaked/CC-Tweaked/workflows/Build/badge.svg)](https://github.com/cc-tweaked/CC-Tweaked/actions "Current build status") [![Download CC: Tweaked on CurseForge](http://cf.way2muchnoise.eu/title/cc-tweaked.svg)][CurseForge]

CC: Tweaked is a mod for Minecraft which adds programmable computers, turtles and more to the game. A fork of the
much-beloved [ComputerCraft], it continues its legacy with better performance, stability, and a wealth of new features.

CC: Tweaked can be installed from [CurseForge] or [Modrinth]. It requires the [Minecraft Forge][forge] mod loader, but
[versions are available for Fabric][ccrestitched].

## Contributing
Any contribution is welcome, be that using the mod, reporting bugs or contributing code. If you want to get started
developing the mod, [check out the instructions here](CONTRIBUTING.md#developing).

## Community
If you need help getting started with CC: Tweaked, want to show off your latest project, or just want to chat about
ComputerCraft, do check out our [forum] and [GitHub discussions page][GitHub discussions]! There's also a fairly
populated, albeit quiet [IRC channel][irc], if that's more your cup of tea.

We also host fairly comprehensive documentation at [tweaked.cc](https://tweaked.cc/ "The CC: Tweaked website").

## Using
CC: Tweaked is hosted on my maven repo, and so is relatively simple to depend on. You may wish to add a soft (or hard)
dependency in your `mods.toml` file, with the appropriate version bounds, to ensure that API functionality you depend
on is present.

```groovy
repositories {
  maven {
    url "https://squiddev.cc/maven/"
    content {
      includeGroup("cc.tweaked")
      includeModule("org.squiddev", "Cobalt")
    }
  }
}

dependencies {
  // Vanilla (i.e. for multi-loader systems)
  compileOnly("cc.tweaked:cc-tweaked-$mcVersion-common-api")

  // Forge Gradle
  compileOnly("cc.tweaked:cc-tweaked-$mcVersion-core-api:$cctVersion")
  compileOnly(fg.deobf("cc.tweaked:cc-tweaked-$mcVersion-forge-api:$cctVersion"))
  runtimeOnly(fg.deobf("cc.tweaked:cc-tweaked-$mcVersion-forge:$cctVersion"))

  // Fabric Loom
  modCompileOnly("cc.tweaked:cc-tweaked-$mcVersion-fabric-api:$cctVersion")
  modRuntimeOnly("cc.tweaked:cc-tweaked-$mcVersion-fabric:$cctVersion")
}
```

You should also be careful to only use classes within the `dan200.computercraft.api` package. Non-API classes are
subject to change at any point. If you depend on functionality outside the API, file an issue, and we can look into
exposing more features.

We bundle the API sources with the jar, so documentation should be easily viewable within your editor. Alternatively,
the generated documentation [can be browsed online](https://tweaked.cc/javadoc/).

[computercraft]: https://github.com/dan200/ComputerCraft "ComputerCraft on GitHub"
[curseforge]: https://minecraft.curseforge.com/projects/cc-tweaked "Download CC: Tweaked from CurseForge"
[modrinth]: https://modrinth.com/mod/gu7yAYhd "Download CC: Tweaked from Modrinth"
[forge]: https://files.minecraftforge.net/ "Download Minecraft Forge."
[ccrestitched]: https://www.curseforge.com/minecraft/mc-mods/cc-restitched "Download CC: Restitched from CurseForge"
[forum]: https://forums.computercraft.cc/
[GitHub Discussions]: https://github.com/cc-tweaked/CC-Tweaked/discussions
[IRC]: https://webchat.esper.net/?channels=computercraft "#computercraft on EsperNet"
