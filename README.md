# CC:Restitched Patchwork
# This is a Work In Progress Port
*it runs and works-ish*
<img src="logo.png" alt="CC: Restitched" width="100%"/>

[![Current build status](https://github.com/Merith-TK/cc-restitched/workflows/Build/badge.svg)](https://github.com/o-iM-nI/cc-restitched/actions "Current build status") [![Download CC: Restitched  on CurseForge](http://cf.way2muchnoise.eu/title/cc-restitched.svg)](https://www.curseforge.com/minecraft/mc-mods/cc-restitched-updated "Download CC: Restitched on CurseForge")

This is an fork of [Zundrel/cc-tweaked-fabric](https://github.com/Zundrel/cc-tweaked-fabric) who's goal was to port [SquidDev-CC/CC-Tweaked](https://github.com/SquidDev-CC/CC-Tweaked) to fabric. I picked up maintaining the mod because the team working on Zundrel's fork, admitted they had gotten lazy so I picked it up to make it up to snuff with CC:T

## Resource Packs
This mod includes textures that are more in-line with the style of Mojang's new texture-artist, Jappa. If you prefer the original textures, enable the "Classic" resource pack provided by the mod.

We also have a second resourcepack made by [3prm3](https://github.com/3prm3), it features a complete overhaul and can be enabled by enabling the `overhaul` resource pack, go check out his resource pack over here!

<img src="https://raw.githubusercontent.com/3prm3/cc-pack/main/pack.png" alt="CC: Restitched" width="16"  height="16"/> [3prm3/cc-pack](https://github.com/3prm3/cc-pack/)

## Contributing
Any contribution is welcome, be that using the mod, reporting bugs or contributing code. In order to start helping develop CC:R there are a few rules
1) Any updates that port commits from CC:T, ***MUST*** follow the format defined in [patchwork.md](patchwork.md) otherwise they will not be accepted,
	* Commit Message must be the same as it is in CC:T,
	* patchwork.md must be updated in the following format
	> Comments, optional but useful if you had to do something 	differently than in CC:T (outside of Fabric/forge differences
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
2) Follow the fabirc programming guidelines as close as possible. This means you have to use `loom` mappings, 
3) You cannot intentionally implement bugs and security vulnerabilities
4) Unless the commit is a "patchwork" compliant commit, (IE: taken from CC:T), the lua code is off limits,s
## Bleeding Edge Builds
Bleeding edge builds can be found [here](https://github.com/Merith-TK/cc-restitched/actions) at github actions to simplify things 

## Community
If you need help getting started with CC: Tweaked, want to show off your latest project, or just want to chat about ComputerCraft, here is the [Forum](https://forums.computercraft.cc/) and the [Discord](https://discord.gg/H2UyJXe) 

## Known Issues
Main Known issue
* Mods that add blocks that can be used as peripherals for CC:T On forge, dont work with CC:R.
	* This is because of the differences between forge and fabric, and that mod devs, to my knowledge have not agreed upon a standard method in which to implement cross compatibility between mods,
* [Fixed (d10f297c): please report if bug persists]</br> ~~Storage Peripherals throw a java "StackOverflowError" when using `pushItems()`,~~ 
    * ~~Work around, you are probably using `pushItems(chest, 1)` or similar. please use `pushItems(chest, 1, nil, 1)`.~~ 
* Computers will not run built in commands, saying "File not found"
    * This is a know bug, dont know what causes it, but just restart the computer (`ctrl+r` for one second) and it will work again
    * Occurs when server runs `/reload` or a datapack is updated

## Known Working mods that add Peripherals
* Please let me know of other mods that work with this one
	* Better End
	* Better Nether
