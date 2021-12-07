<img src="logo.png" alt="CC: Restitched" width="100%"/>

[![Current build status](https://github.com/cc-tweaked/cc-restitched/workflows/Build/badge.svg)](https://github.com/cc-tweaked/cc-restitched/actions "Current build status")
[![Download CC: Restitched  on CurseForge](https://camo.githubusercontent.com/07622e6662ef5ead080c4840ef6514a34e079d63015f7e51c977a55b1881dfb9/687474703a2f2f63662e776179326d7563686e6f6973652e65752f7469746c652f63632d726573746974636865642e737667)](https://www.curseforge.com/minecraft/mc-mods/cc-restitched "Download CC:  Restitched on CurseForge")
[![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-908a85?logo=gitpod)](https://gitpod.io/#https://github.com/cc-tweaked/cc-restitched/tree/1.17.1)

# What is CC: Restitched?
This is a fabric port of [SquidDev-CC/CC-Tweaked](https://github.com/SquidDev-CC/CC-Tweaked). The work is a continuation of [Zundrel/cc-tweaked-fabric](https://github.com/Zundrel/cc-tweaked-fabric).

## CC: Restitched vs. CC: Tweaked
CC: R tries to maintain parity with CC: T, but may be behind or divergent in some areas. If you notice a disparity please open an issue. CC: R major and minor version numbers indicate parity with the major features of that version of CC: T. Patch version numbers will not align.

## Resource Packs
This mod includes textures by [Jummit](https://github.com/Jummit) that are more in line with the style of Mojang's new texture-artist, Jappa. If you prefer the original textures, enable the "Classic" resource pack.

<img src="https://raw.githubusercontent.com/3prm3/cc-pack/main/pack.png" alt="CC: Restitched" width="16"  height="16"/> We also have a second resourcepack made by [3prm3](https://github.com/3prm3), it features a complete overhaul and can be enabled by enabling the `overhaul` resource pack, go check out his resource pack over [here](https://github.com/3prm3/cc-pack/)!

## Contributing
Any contribution is welcome, be that using the mod, reporting bugs or contributing code. In order to start helping develop CC: R there are a few rules;
1) Make sure your code follows the checkstyle rules. You can test this by running `./gradle build` or `./gradle check`.
2)  Do not alter the lua code unless those changes are taken directly from CC: Tweaked. If you wish to contribute changes to the in game rom please contribute upstream at [CC-Tweaked](https://github.com/SquidDev-CC/CC-Tweaked).

# Does this work Fabric's many rendering mods?
* [ YES ] Sodium
* [ YES ] Optifine
	* Works with VBO Rendering (automatically set)
	* No issues
* [ EHH ] Iris Shaders
	* "Works" with TBO Rendering (Default)
	* Crashes with VBO Rendering
	* <details>
		<summary>Shaders are broken</summary>

		* Shaders will cause varrying results ranging from monitors being invisible, to straight up crashing.
		* Not using shaders will result in odd Z-Fighting of the monitor display and the transparent texture
			- ![](https://user-images.githubusercontent.com/10422110/136869483-91824c5f-841f-4316-bfb1-2412477a29ee.png)
			- ![](https://user-images.githubusercontent.com/10422110/136869535-a16581a3-5e0a-4632-923f-c8de8cc8a6ea.png)
		</details>
* [ YES ] Canvas
	* Works with TBO Rendering (Default)
	* Scuffed with VBO Rendering
	* <details>
		<summary>VBO is broken</summary>

  		Monitors are just... scuffed beyond belief.
		- ![](https://i.imgur.com/JVNZ2Pn.png)
		- ![](https://i.imgur.com/SXXpr54.png)
			* The content to the left is supposed to be on the monitors to the right, also the bottom one is supposed to `black/white` not colored.
		* Turtle Texture for some reason?
			- ![](https://i.imgur.com/OEmZXsx.png)
		</details>


## Bleeding Edge Builds
Bleeding edge builds can be found [here](https://github.com/cc-tweaked/cc-restitched/actions) at Github Actions.

## Community
If you need help getting started with CC: Restitched, want to show off your latest project, or just want to chat about ComputerCraft, here is the [Forum](https://forums.computercraft.cc/) and the [Discord](https://discord.gg/H2UyJXe).

## Perpheral mods
Unfortunately, CC: Restitched does not have as many peripherals mods available as CC: Tweaked. If you're an interested mod developer, please check out our `api` package. If you've already made a mod with CC: R peripheral support OR if you're a player who found a mod with ComputerCraft integration, please open an [issue here](https://github.com/cc-tweaked/cc-restitched/issues/new?assignees=&labels=peripheralShoutout&template=peripheral_shoutout.md) to let us know and we'll add it to the list!
