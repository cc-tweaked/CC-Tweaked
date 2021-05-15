
# ![CC: Restitched](logo.png)
This is an [fork](https://github.com/Zundrel/cc-tweaked-fabric) of a [fork](https://github.com/ArchivedProjects/cc-tweaked-fabric)  of a [fork/port](https://github.com/mystiacraft/cc-tweaked-fabric) of an [update/port/fork](https://github.com/SquidDev-CC/CC-Tweaked) of [ComputerCraft](https://github.com/dan200/ComputerCraft), adding programmable computers,
turtles and more to Minecraft.

## What?
ComputerCraft is the first mod that I have ever used and also one of my favorite mods.
The only problem I have with is... the old textures so I manually am fixing and tweaking them along with some other things.

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
* Storage Peripherals throw a java "StackOverflowError" when using `pushItems()`, 
    * Work around, you are probably using `pushItems(chest, 1)` or simular. please use `pushItems(chest, 1, nil, 1)`. 

## Known Working mods that add Peripherals
* Please let me know of other mods that work with this one
	* Better End
		* Chests and Inventories
	* Better Nether
		* Chests and Inventories
## [Building from sources](https://github.com/CaffeineMC/sodium-fabric#building-from-sources)

