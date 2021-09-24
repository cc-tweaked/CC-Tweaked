<img src="logo.png" alt="CC: Restitched" width="100%"/>

[![Current build status](https://github.com/Merith-TK/cc-restitched/workflows/Build/badge.svg)](https://github.com/Merith-TK/cc-restitched/actions "Current build status")
[![Download CC: Restitched  on CurseForge](https://camo.githubusercontent.com/07622e6662ef5ead080c4840ef6514a34e079d63015f7e51c977a55b1881dfb9/687474703a2f2f63662e776179326d7563686e6f6973652e65752f7469746c652f63632d726573746974636865642e737667)](https://www.curseforge.com/minecraft/mc-mods/cc-restitched "Download CC:  Restitched on CurseForge")
[![Gitpod ready-to-code](https://img.shields.io/badge/Gitpod-ready--to--code-908a85?logo=gitpod)](https://gitpod.io/#https://github.com/Merith-TK/cc-restitched)

# CC: R Version VS CC: T Version
CC: R Strives to maintain perfect pairity with CC: T, however in some cases this is not possible, so CC: R might have a "newer" version than what CC: T has, these newer versions will be primarily bugfixes and the like because fabric is "weird" when porting a forge mod.

## What is CC: Restiched
This is an fork of [Zundrel/cc-tweaked-fabric](https://github.com/Zundrel/cc-tweaked-fabric) who's goal was to port [SquidDev-CC/CC-Tweaked](https://github.com/SquidDev-CC/CC-Tweaked) to the [Fabric](https://fabricmc.net/) modloader. I picked up maintaining the mod because the team working on Zundrel's fork can no longer mantain it so I picked it up to make it as equal as possible with CC: T.

## Resource Packs
This mod includes textures that are more in-line with the style of Mojang's new texture-artist, Jappa. If you prefer the original textures, enable the "Classic" resource pack.

<img src="https://raw.githubusercontent.com/3prm3/cc-pack/main/pack.png" alt="CC: Restitched" width="16"  height="16"/> [3prm3/cc-pack](https://github.com/3prm3/cc-pack/)
We also have a second resourcepack made by [3prm3](https://github.com/3prm3), it features a complete overhaul and can be enabled by enabling the `overhaul` resource pack, go check out his resource pack over here!

# Conflicts
Currently Iris and Canvas Shaders are Incompatible with this mod,
 - Iris has transparent monitors, and when a computer displays something on the monitor, the face becomes black.
 - Canvas... uhm...
	- Computer Terminals are 100% unusable and scuffed
		<img src="https://user-images.githubusercontent.com/16393543/120464345-ab619b00-c351-11eb-9dfb-e68ddc93de5e.png">
	- Monitors break with either rendering option.
		- TBO
			- Just crashes on world load.
		- VBO
			<img src="https://user-images.githubusercontent.com/16393543/120475933-d999a780-c35e-11eb-9d94-ef4e5988ad5f.png">
## Contributing
Any contribution is welcome, be that using the mod, reporting bugs or contributing code. In order to start helping develop CC: R there are a few rules;
1) Any updates that port commits from CC: T, ***MUST*** follow the format defined in [patchwork.md](patchwork.md) otherwise they will not be accepted,
	* Commit Message must be the same as it is in CC: T ,
	* patchwork.md must be updated in the following format.
	> Comments, optional but useful if you had to do something 	differently than in CC: T outside of [Fabric](https://fabricmc.net/)/[Forge](https://mcforge.readthedocs.io/en/1.16.x/) differences.
	>
	> \`\`\`
	>
	>commitID
	>
	> commit title
	>
	> commit desc
	>
	> \`\`\`
2) Follow the [Fabric](https://fabricmc.net/) programming guidelines as close as possible. This means you have to use [`loom`](https://fabricmc.net/wiki/tutorial:mappings) mappings,
3) You cannot intentionally implement bugs and security vulnerabilities.
4) Unless the commit is a ["patchwork"](https://github.com/Merith-TK/cc-restitched/blob/fabric/patchwork.md) compliant commit, (IE: taken from CC: T), the lua code is off limits.
## Bleeding Edge Builds
Bleeding edge builds can be found [here](https://github.com/Merith-TK/cc-restitched/actions) at Github Actions.

## Community
If you need help getting started with CC: Restitched, want to show off your latest project, or just want to chat about ComputerCraft, here is the [Forum](https://forums.computercraft.cc/) and the [Discord](https://discord.gg/H2UyJXe).

## Known Issues
Main Known issue
* Mods that add blocks that can be used as peripherals for CC: T on forge, don't work with CC: R.
	* This is because of the differences between forge and fabric, and that mod devs, to my knowledge have not agreed upon a standard method in which to implement cross compatibility between mods,
* [Fixed (d10f297c): please report if bug persists]</br> ~~Storage Peripherals throw a java "StackOverflowError" when using `pushItems()`,~~
    * ~~Work around, you are probably using `pushItems(chest, 1)` or similar. please use `pushItems(chest, 1, nil, 1)`.~~
* Computers will not run built in commands, saying "File not found".
    * This is a know bug, dont know what causes it, but just restart the computer (`ctrl+r` for one second) and it will work again.
    * Occurs when server runs `/reload` or a datapack is updated.

## Perpherals
Unfortunately, unlike the original CC: Tweaked project, CC: Restitched, does not have any actual peripheral mods, currently the only one we have is an example for mod devs to get started with making/adding the peripheral API to their mods!

If your a mod dev made a mod with CC: R peripheral support, OR if your a player who found a mod with CC: R support, please open an [issue here](https://github.com/Merith-TK/cc-restitched/issues/new?assignees=&labels=peripheralShoutout&template=peripheral_shoutout.md) to let us know so we can add it to the list!

* ![icon](https://raw.githubusercontent.com/Toad-Dev/cc-peripheral-test/master/src/main/resources/assets/cc_test/textures/block/test_peripheral.png) [CC Peripheral Test](https://github.com/Toad-Dev/cc-peripheral-test)
	* This is an example mod for how to make peripherals that work as a block, or as a turtle upgrade!
