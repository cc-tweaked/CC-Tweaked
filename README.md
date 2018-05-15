# ![CC: Tweaked](logo.png)
[![Build Status](https://travis-ci.org/SquidDev-CC/CC-Tweaked.svg?branch=master)](https://travis-ci.org/SquidDev-CC/CC-Tweaked)

CC: Tweaked is a fork of ComputerCraft which aims to provide earlier access to the more experimental and in-development
features of the mod. For a more stable experience, I recommend checking out the
[original mod](https://github.com/dan200/ComputerCraft).

## What?
CC: Tweaked (or CC:T for short) does not aim to create a competing fork of ComputerCraft, nor am I planning to take it
in in a vastly different direction to the original mod. In fact, CC:T aims to be a nurturing ground for various
features, with a pull request against the original mod being the end goal.

CC:T also includes many pull requests from the community which have not yet been merged, offering a large number
of additional bug fixes and features over the original mod.

## Features
CC: Tweaked contains the all features of the latest alpha, as well as numerous fixes, performance improvements and
several additional features. I'd recommend checking out [the releases page](https://github.com/SquidDev-CC/CC-Tweaked/releases)
to see the full changes, but here's a couple of the more interesting changes:

 - Replace LuaJ with Cobalt.
 - Allow running multiple computers at the same time.
 - Websocket support in the HTTP library.
 - Wired modems and cables act more like multiparts.
 - Add map-like rendering for pocket computers.
 - Adds the `/computercraft` command, offering various diagnostic tools for server owners. This allows operators to
   track which computers are hogging resources, turn on and shutdown multiple computers at once and interact with
   computers remotely.
 - Add full-block wired modems, allowing one to wrap non-solid peripherals (such as turtles, or chests if Plethora is
   installed).

## Relation to CCTweaks?
This mod has nothing to do with CCTweaks, though there is no denying the name is a throwback to it. That being said,
several features have been included, such as full block modems, the Cobalt runtime and map-like rendering for pocket
computers.

## Contributing
Any contribution is welcome, be that using the mod, reporting bugs or contributing code. If you do wish to contribute
code, do consider submitting it to the ComputerCraft repository instead.

That being said, in order to start helping develop CC:T, you'll need to follow these steps:

 - **Clone the repository:** `git clone https://github.com/SquidDev-CC/CC-Tweaked.git && cd CC-Tweaked`
 - **Setup Forge:** `./gradlew setupDecompWorkspace`
 - **Test your changes:** `./gradlew runClient` (or run the `GradleStart` class from your IDE).

If you want to run CC:T in a normal Minecraft instance, run `./gradlew build` and copy the `.jar` from `build/libs`.
