# ![CC: Tweaked](logo.png)
[![Build Status](https://travis-ci.org/SquidDev-CC/CC-Tweaked.svg?branch=master)](https://travis-ci.org/SquidDev-CC/CC-Tweaked)

CC: Tweaked is a fork of ComputerCraft which aims to provide more earlier access to the more experimental and 
in-development features of the mod. For a more stable experience, I recommend checking out the 
[original mod](https://github.com/dan200/ComputerCraft).

## What?
CC: Tweaked does not aim to create a competing fork of ComputerCraft, nor am I planning to take it in in a vastly
different direction to the original mod. In fact, CC: Tweaked aims to be a nurturing ground for various features, with
a pull request against the original mod being the end goal.

CC: Tweaked also includes many pull requests from the community which have not yet been merged, offering a large number
of additional bug fixes and features over the original mod.

## Relation to CCTweaks?
This mod has nothing to do with CCTweaks, though there is no denying the name is a throwback to it. However, I do plan
to migrate some features of CCTweaks into CC: Tweaked.

## Contributing
Any contribution is welcome, be that using the mod, reporting bugs or contributing code. If you do wish to contribute
code, do consider submitting it to the ComputerCraft repository instead.

That being said, in order to start helping develop CC: Tweaked, you'll need to follow these steps:

 - **Clone the repository:** `git clone https://github.com/SquidDev-CC/CC-Tweaked.git && cd CC-Tweaked`
 - **Setup Forge:** `./gradlew setupDecompWorkspace`
 - **Test your changes:** `./gradlew runClient` (or run the `GradleStart` class from your IDE).
 
If you want to run CC: Tweaked in a normal Minecraft instance, run `./gradlew build` and copy the `.jar` from 
`build/libs`.
