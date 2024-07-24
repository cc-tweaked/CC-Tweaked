<!--
SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="./doc/logo-darkmode.png">
  <source media="(prefers-color-scheme: light)" srcset="./doc/logo.png">
  <img alt="CC: Tweaked" src="./doc/logo.png">
</picture>

[![Current build status](https://github.com/cc-tweaked/CC-Tweaked/workflows/Build/badge.svg)](https://github.com/cc-tweaked/CC-Tweaked/actions "Current build status")
[![Download CC: Tweaked on CurseForge](https://img.shields.io/static/v1?label=Download&message=CC:%20Tweaked&color=E04E14&logoColor=E04E14&logo=CurseForge)][CurseForge]
[![Download CC: Tweaked on Modrinth](https://img.shields.io/static/v1?label=Download&color=00AF5C&logoColor=00AF5C&logo=Modrinth&message=CC:%20Tweaked)][Modrinth]

CC: Tweaked is a mod for Minecraft which adds programmable computers, turtles and more to the game. A fork of the
much-beloved [ComputerCraft], it continues its legacy with improved performance and stability, along with a wealth of
new features.

CC: Tweaked can be installed from [CurseForge] or [Modrinth]. It runs on both [Minecraft Forge] and [Fabric].

## Contributing
Any contribution is welcome, be that using the mod, reporting bugs or contributing code. If you want to get started
developing the mod, [check out the instructions here](CONTRIBUTING.md#developing).

## Community
If you need help getting started with CC: Tweaked, want to show off your latest project, or just want to chat about
ComputerCraft, do check out our [GitHub discussions page][GitHub discussions]! There's also a fairly populated,
albeit quiet IRC channel on [EsperNet], if that's more your cup of tea. You can join `#computercraft` through your
desktop client, or online using [KiwiIRC].

We also host fairly comprehensive documentation at [tweaked.cc](https://tweaked.cc/ "The CC: Tweaked website").

## Using
CC: Tweaked is hosted on my maven repo, and so is relatively simple to depend on. You may wish to add a soft (or hard)
dependency in your `mods.toml` file, with the appropriate version bounds, to ensure that API functionality you depend
on is present.

```groovy
repositories {
  maven {
    url "https://maven.squiddev.cc"
    content {
      includeGroup("cc.tweaked")
    }
  }
}

dependencies {
  // Vanilla (i.e. for multi-loader systems)
  compileOnly("cc.tweaked:cc-tweaked-$mcVersion-common-api:$cctVersion")

  // Forge Gradle
  compileOnly("cc.tweaked:cc-tweaked-$mcVersion-core-api:$cctVersion")
  compileOnly(fg.deobf("cc.tweaked:cc-tweaked-$mcVersion-forge-api:$cctVersion"))
  runtimeOnly(fg.deobf("cc.tweaked:cc-tweaked-$mcVersion-forge:$cctVersion"))

  // Fabric Loom
  modCompileOnly("cc.tweaked:cc-tweaked-$mcVersion-fabric-api:$cctVersion")
  modRuntimeOnly("cc.tweaked:cc-tweaked-$mcVersion-fabric:$cctVersion")
}
```

When using ForgeGradle, you may also need to add the following:

```groovy
minecraft {
    runs {
        configureEach {
            property 'mixin.env.remapRefMap', 'true'
            property 'mixin.env.refMapRemappingFile', "${buildDir}/createSrgToMcp/output.srg"
        }
    }
}
```

You should also be careful to only use classes within the `dan200.computercraft.api` package. Non-API classes are
subject to change at any point. If you depend on functionality outside the API (or need to mixin to CC:T), please file
an issue to let me know!

We bundle the API sources with the jar, so documentation should be easily viewable within your editor. Alternatively,
the generated documentation [can be browsed online](https://tweaked.cc/javadoc/).

[computercraft]: https://github.com/dan200/ComputerCraft "ComputerCraft on GitHub"
[curseforge]: https://minecraft.curseforge.com/projects/cc-tweaked "Download CC: Tweaked from CurseForge"
[modrinth]: https://modrinth.com/mod/gu7yAYhd "Download CC: Tweaked from Modrinth"
[Minecraft Forge]: https://files.minecraftforge.net/ "Download Minecraft Forge."
[Fabric]: https://fabricmc.net/use/installer/ "Download Fabric."
[GitHub Discussions]: https://github.com/cc-tweaked/CC-Tweaked/discussions
[EsperNet]: https://www.esper.net/
[KiwiIRC]: https://kiwiirc.com/nextclient/#irc://irc.esper.net:+6697/#computercraft "#computercraft on EsperNet"
