# CC:Restitched Patchwork
# This is a W.I.P Port

<img src="logo.png" alt="CC: Restitched" width="100%"/>

*it works and runs-ish*

[![Current build status](https://github.com/Merith-TK/cc-restitched/workflows/Build/badge.svg)](https://github.com/Merith-TK/cc-restitched/actions "Current build status") [![Download CC: Restitched  on CurseForge](http://cf.way2muchnoise.eu/title/cc-restitched.svg)](https://www.curseforge.com/minecraft/mc-mods/cc-restitched-updated "Download CC: Restitched on CurseForge")

## What?
This is an fork of [Zundrel/cc-tweaked-fabric](https://github.com/Zundrel/cc-tweaked-fabric) who's goal was to port [SquidDev-CC/CC-Tweaked](https://github.com/SquidDev-CC/CC-Tweaked) to the [Fabric](https://fabricmc.net/) modloader. I picked up maintaining the mod because the team working on Zundrel's fork, admitted they had gotten lethargic so I picked it up to make it equal with CC:T

## Resource Packs
This mod includes textures that are more in-line with the style of Mojang's new texture-artist, Jappa. If you prefer the original textures, enable the "Classic" resource pack provided by the mod.

<img src="https://raw.githubusercontent.com/3prm3/cc-pack/main/pack.png" alt="CC: Restitched" width="16"  height="16"/> [3prm3/cc-pack](https://github.com/3prm3/cc-pack/)
We also have a second resourcepack made by [3prm3](https://github.com/3prm3), it features a complete overhaul and can be enabled by enabling the `overhaul` resource pack, go check out his resource pack over here!

## Relation to Stitching?
The "stitched" is becuase of the name of the modloader ;the ["Fabric"](https://fabricmc.net/) modloader 
## Contributing
Any contribution is welcome, be that using the mod, reporting bugs or contributing code. In order to start helping develop CC:R there are a few rules
1) Any updates that port commits from CC:T, ***MUST*** follow the format defined in [patchwork.md](patchwork.md) otherwise they will not be accepted,
	* Commit Message must be the same as it is in CC:T,
	* patchwork.md must be updated in the following format
	> Comments, optional but useful if you had to do something 	differently than in CC:T outside of [Fabric](https://fabricmc.net/)/[Forge](https://mcforge.readthedocs.io/en/1.16.x/) differences
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
3) You cannot intentionally implement bugs and security vulnerabilities
4) Unless the commit is a ["patchwork"](https://github.com/Merith-TK/cc-restitched/blob/fabric/patchwork.md) compliant commit, (IE: taken from CC:T), the lua code is off limits
## Bleeding Edge Builds
Bleeding edge builds can be found [here](https://github.com/Merith-TK/cc-restitched/actions) at Github Actions.

## Community
If you need help getting started with CC: Tweaked, want to show off your latest project, or just want to chat about ComputerCraft, here is the [Forum](https://forums.computercraft.cc/) and the [Discord](https://discord.gg/H2UyJXe) 

## Known Issues
Main Known issue
* Mods that add blocks that can be used as peripherals for CC:T On forge, don't work with CC:R.
	* This is because of the differences between forge and fabric, and that mod devs, to my knowledge have not agreed upon a standard method in which to implement cross compatibility between mods,
* [Fixed (d10f297c): please report if bug persists]</br> ~~Storage Peripherals throw a java "StackOverflowError" when using `pushItems()`,~~ 
    * ~~Work around, you are probably using `pushItems(chest, 1)` or similar. please use `pushItems(chest, 1, nil, 1)`.~~ 
* Computers will not run built in commands, saying "File not found"
    * This is a know bug, dont know what causes it, but just restart the computer (`ctrl+r` for one second) and it will work again
    * Occurs when server runs `/reload` or a datapack is updated

## Known Working mods that add Peripherals
* Please let me know of other mods that work with this one
	* Better End 
      * adds chests that are compatible with ComputerCraft
	* Better Nether
	  * adds chests that are compatible with ComputerCraft
