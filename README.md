<img src="logo.png" alt="CC: Restitched" width="100%"/>

[![Current build status](https://github.com/Merith-TK/cc-restitched/workflows/Build/badge.svg)](https://github.com/Merith-TK/cc-restitched/actions "Current build status")
[![Download CC: Restitched  on CurseForge](https://camo.githubusercontent.com/07622e6662ef5ead080c4840ef6514a34e079d63015f7e51c977a55b1881dfb9/687474703a2f2f63662e776179326d7563686e6f6973652e65752f7469746c652f63632d726573746974636865642e737667)](https://www.curseforge.com/minecraft/mc-mods/cc-restitched "Download CC:  Restitched on CurseForge")
[![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-908a85?logo=gitpod)](https://gitpod.io/#https://github.com/Merith-TK/cc-restitched/tree/1.17-alpha)

# CC: R Version VS CC: T Version
CC: R Strives to maintain perfect pairity with CC: T, however in some cases this is not possible, so CC: R might have a "newer" version than what CC: T has, these newer versions will be primarily bugfixes and the like because fabric is "weird" when porting a forge mod.

## What is CC: Restiched
This is an fork of [Zundrel/cc-tweaked-fabric](https://github.com/Zundrel/cc-tweaked-fabric) who's goal was to port [SquidDev-CC/CC-Tweaked](https://github.com/SquidDev-CC/CC-Tweaked) to the [Fabric](https://fabricmc.net/) modloader. I picked up maintaining the mod because the team working on Zundrel's fork can no longer mantain it so I picked it up to make it as equal as possible with CC: T.

## Resource Packs
This mod includes textures that are more in-line with the style of Mojang's new texture-artist, Jappa. If you prefer the original textures, enable the "Classic" resource pack.

<img src="https://raw.githubusercontent.com/3prm3/cc-pack/main/pack.png" alt="CC: Restitched" width="16"  height="16"/> [3prm3/cc-pack](https://github.com/3prm3/cc-pack/)
We also have a second resourcepack made by [3prm3](https://github.com/3prm3), it features a complete overhaul and can be enabled by enabling the `overhaul` resource pack, go check out his resource pack over here!

# Does this work with shaders/sodium?
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

  		Monitors are just... scuffed beyond beleif
		- ![](https://i.imgur.com/JVNZ2Pn.png)
		- ![](https://i.imgur.com/SXXpr54.png)
			* The content to the left is supposed to be on the monitors to the right, also the bottom one is supposed to `black/white` not colored.
		* Turtle Texture for some reason?
			- ![](https://i.imgur.com/OEmZXsx.png)
		</details>
		

## Contributing
Any contribution is welcome, be that using the mod, reporting bugs or contributing code. In order to start helping develop CC: R there are a few rules;
1) Follow the [Fabric](https://fabricmc.net/) programming guidelines as close as possible. This means you have to use [`loom`](https://fabricmc.net/wiki/tutorial:mappings) mappings, if you use anything else, your code will be rejected.
2) You cannot intentionally implement bugs and security vulnerabilities.
3) Unless the code is taken directly from CC:Tweaked, `lua` code is offlimits from alteration.

## Bleeding Edge Builds
Bleeding edge builds can be found [here](https://github.com/Merith-TK/cc-restitched/actions) at Github Actions.

## Community
If you need help getting started with CC: Restitched, want to show off your latest project, or just want to chat about ComputerCraft, here is the [Forum](https://forums.computercraft.cc/) and the [Discord](https://discord.gg/H2UyJXe).

## Known Bugs/Issues
Main Known issue
* Mods that add blocks that can be used as peripherals for CC: T on forge, don't work with CC: R.
	* This is because of the differences between forge and fabric, and that mod devs, to my knowledge have not agreed upon a standard method in which to implement cross compatibility between mods,
* Occasionally a computer will say "File not found" when running a built in program, this is normal just hold `crtl+r` to reboot it. 
	* We do not know what causes it directly, however it happens when the world is `/reload`ed, and currently running programs are not affected so nothing *should break*

## Perpherals
Unfortunately, unlike the original CC: Tweaked project, CC: Restitched, does not have any actual peripheral mods, currently the only one we have is an example for mod devs to get started with making/adding the peripheral API to their mods!

If your a mod dev made a mod with CC: R peripheral support, OR if your a player who found a mod with CC: R support, please open an [issue here](https://github.com/Merith-TK/cc-restitched/issues/new?assignees=&labels=peripheralShoutout&template=peripheral_shoutout.md) to let us know so we can add it to the list!

* ![icon](https://raw.githubusercontent.com/Toad-Dev/cc-peripheral-test/master/src/main/resources/assets/cc_test/textures/block/test_peripheral.png) [CC Peripheral Test [1.16.5]](https://github.com/Toad-Dev/cc-peripheral-test)
	* This is an example mod for how to make peripherals that work as a block, or as a turtle upgrade!
