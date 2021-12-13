# New features in CC: Tweaked 1.99.1

* Add package.searchpath to the cc.require API. (MCJack123)
* Provide a more efficient way for the Java API to consume Lua tables in certain restricted cases.

Several bug fixes:
* Fix keys being "sticky" when opening the off-hand pocket computer GUI.
* Correctly handle broken coroutine managers resuming Java code with a `nil` event.
* Prevent computer buttons stealing focus from the terminal.
* Fix a class cast exception when a monitor is malformed in ways I do not quite understand.

# New features in CC: Tweaked 1.99.0

* Pocket computers in their offhand will open without showing a terminal. You can look around and interact with the world, but your keyboard will be forwarded to the computer. (Wojbie, MagGen-hub).
* Peripherals can now have multiple types. `peripheral.getType` now returns multiple values, and `peripheral.hasType` checks if a peripheral has a specific type.
* Add several missing keys to the `keys` table. (ralphgod3)
* Add feature introduction/changed version information to the documentation. (MCJack123)
* Increase the file upload limit to 512KiB.
* Rednet can now handle computer IDs larger than 65535. (Ale32bit)
* Optimise deduplication of rednet messages (MCJack123)
* Make `term.blit` colours case insensitive. (Ocawesome101)
* Add a new `about` program for easier version identification. (MCJack123)
* Optimise peripheral calls in `rednet.run`. (xAnavrins)
* Add dimension parameter to `commands.getBlockInfo`.
* Add `cc.pretty.pretty_print` helper function (Lupus590).
* Various translation updates (MORIMORI3017, Ale2Bit, mindy15963)

And several bug fixes:
* Fix various computer commands failing when OP level was 4.
* Various documentation fixes. (xXTurnerLP, MCJack123)
* Fix `textutils.serialize` not serialising infinity and nan values. (Wojbie)
* Wired modems now correctly clean up mounts when a peripheral is detached.
* Fix incorrect turtle and pocket computer upgrade recipes in the recipe book.
* Fix speakers not playing sounds added via resource packs which are not registered in-game.
* Fix speaker upgrades sending packets after the server has stopped.
* Monitor sizing has been rewritten, hopefully making it more stable.
* Peripherals are now invalidated when the computer ticks, rather than when the peripheral changes.

# New features in CC: Tweaked 1.98.2

* Add JP translation (MORIMORI0317)
* Migrate several recipes to data generators.

Several bug fixes:
* Fix volume speaker sounds are played at.

# New features in CC: Tweaked 1.98.1

Several bug fixes:
* Fix monitors not correctly resizing when placed.
* Update Russian translation (DrHesperus).

# New features in CC: Tweaked 1.98.0
* Add motd for file uploading.
* Add config options to limit total bandwidth used by the HTTP API.

And several bug fixes:
* Fix `settings.define` not accepting a nil second argument (SkyTheCodeMaster).
* Various documentation fixes (Angalexik, emiliskiskis, SkyTheCodeMaster).
* Fix selected slot indicator not appearing in turtle interface.
* Fix crash when printers are placed as part of world generation.
* Fix crash when breaking a speaker on a multiplayer world.
* Add a missing type check for `http.checkURL`.
* Prevent `parallel.*` from hanging when no arguments are given.
* Prevent issue in rednet when the message ID is NaN.
* Fix `help` program crashing when terminal changes width.
* Ensure monitors are well-formed when placed, preventing graphical glitches
  when using Carry On or Quark.
* Accept several more extensions in the websocket client.
* Prevent `wget` crashing when given an invalid URL and no filename.
* Correctly wrap string within `textutils.slowWrite`.

# New features in CC: Tweaked 1.97.0

* Update several translations (Anavrins, Jummit, Naheulf).
* Add button to view a computer's folder to `/computercraft dump`.
* Allow cleaning dyed turtles in a cauldron.
* Add scale subcommand to `monitor` program (MCJack123).
* Add option to make `textutils.serialize` not write an indent (magiczocker10).
* Allow comparing vectors using `==` (fatboychummy).
* Improve HTTP error messages for SSL failures.
* Allow `craft` program to craft unlimited items (fatboychummy).
* Impose some limits on various command queues.
* Add buttons to shutdown and terminate to computer GUIs.
* Add program subcompletion to several programs (Wojbie).
* Update the `help` program to accept and (partially) highlight markdown files.
* Remove config option for the debug API.
* Allow setting the subprotocol header for websockets.
* Add basic JMX monitoring on dedicated servers.
* Add support for MoreRed bundled.
* Allow uploading files by dropping them onto a computer.

And several bug fixes:
* Fix NPE when using a treasure disk when no treasure disks are available.
* Prevent command computers discarding command ouput when certain game rules are off.
* Fix turtles not updating peripherals when upgrades are unequipped (Ronan-H).
* Fix computers not shutting down on fatal errors within the Lua VM.
* Speakers now correctly stop playing when broken, and sound follows noisy turtles and pocket computers.
* Update the `wget` to be more resiliant in the face of user-errors.
* Fix exiting `paint` typing "e" in the shell.
* Fix coloured pocket computers using the wrong texture.
* Correctly render the transparent background on pocket/normal computers.
* Don't apply CraftTweaker actions twice on single-player worlds.

# New features in CC: Tweaked 1.97.0

* Update several translations (Anavrins, Jummit, Naheulf).
* Add button to view a computer's folder to `/computercraft dump`.
* Allow cleaning dyed turtles in a cauldron.
* Add scale subcommand to `monitor` program (MCJack123).
* Add option to make `textutils.serialize` not write an indent (magiczocker10).
* Allow comparing vectors using `==` (fatboychummy).
* Improve HTTP error messages for SSL failures.
* Allow `craft` program to craft unlimited items (fatboychummy).
* Impose some limits on various command queues.
* Add buttons to shutdown and terminate to computer GUIs.
* Add program subcompletion to several programs (Wojbie).
* Update the `help` program to accept and (partially) highlight markdown files.
* Remove config option for the debug API.
* Allow setting the subprotocol header for websockets.

And several bug fixes:
* Fix NPE when using a treasure disk when no treasure disks are available.
* Prevent command computers discarding command ouput when certain game rules are off.
* Fix turtles not updating peripherals when upgrades are unequipped (Ronan-H).
* Fix computers not shutting down on fatal errors within the Lua VM.
* Speakers now correctly stop playing when broken, and sound follows noisy turtles and pocket computers.
* Update the `wget` to be more resiliant in the face of user-errors.
* Fix exiting `paint` typing "e" in the shell.

# New features in CC: Tweaked 1.96.0

* Use lightGrey for folders within the "list" program.
* Add getLimit to inventory peripherals.
* Expose the generic peripheral system to the public API.
* Add cc.expect.range (Lupus590).
* Allow calling cc.expect directly (MCJack123).
* Numerous improvements to documentation.

And several bug fixes:
* Fix paintutils.drawLine incorrectly sorting coordinates (lilyzeiset).
* Improve JEI's handling of turtle/pocket upgrade recipes.
* Correctly handle sparse arrays in cc.pretty.
* Fix crashes when a turtle places a monitor (baeuric).
* Fix very large resource files being considered empty.
* Allow turtles to use compostors.
* Fix dupe bug when colouring turtles.

# New features in CC: Tweaked 1.95.3

Several bug fixes:
* Correctly serialise sparse arrays into JSON (livegamer999)
* Fix hasAudio/playAudio failing on record discs.
* Fix rs.getBundledInput returning the output instead (SkyTheCodeMaster)
* Programs run via edit are now a little better behaved (Wojbie)
* Add User-Agent to a websocket's headers.

# New features in CC: Tweaked 1.95.2

* Add `isReadOnly` to `fs.attributes` (Lupus590)
* Many more programs now support numpad enter (Wojbie)

Several bug fixes:
* Fix some commands failing to parse on dedicated servers.
* Fix all disk recipes appearing to produce a white disk in JEI/recipe book.
* Hopefully improve edit's behaviour with AltGr on some European keyboards.
* Prevent files being usable after their mount was removed.
* Fix the `id` program crashing on non-disk items (Wojbie).
* Preserve registration order of turtle/pocket upgrades when displaying in JEI.

# New features in CC: Tweaked 1.95.1

Several bug fixes:
* Command computers now drop items again.
* Restore crafting of disks with dyes.
* Fix CraftTweaker integrations for damageable items.
* Catch reflection errors in the generic peripheral system, resolving crashes with Botania.

# New features in CC: Tweaked 1.95.0

* Optimise the paint program's initial render.
* Several documentation improvments (Gibbo3771, MCJack123).
* `fs.combine` now accepts multiple arguments.
* Add a setting (`bios.strict_globals`) to error when accidentally declaring a global. (Lupus590).
* Add an improved help viewer which allows scrolling up and down (MCJack123).
* Add `cc.strings` module, with utilities for wrapping text (Lupus590).
* The `clear` program now allows resetting the palette too (Luca0208).

And several bug fixes:
* Fix memory leak in generic peripherals.
* Fix crash when a turtle is broken while being ticked.
* `textutils.*tabulate` now accepts strings _or_ numbers.
* We now deny _all_ local IPs, using the magic `$private` host. Previously the IPv6 loopback interface was not blocked.
* Fix crash when rendering monitors if the block has not yet been synced. You will need to regenerate the config file to apply this change.
* `read` now supports numpad enter (TheWireLord)
* Correctly handle HTTP redirects to URLs containing escape characters.
* Fix integer overflow in `os.epoch`.
* Allow using pickaxes (and other items) for turtle upgrades which have mod-specific NBT.
* Fix duplicate turtle/pocket upgrade recipes appearing in JEI.

# New features in CC: Tweaked 1.94.0

* Add getter for window visibility (devomaa)
* Generic peripherals are no longer experimental, and on by default.
* Use term.blit to draw boxes in paintutils (Lemmmy).

And several bug fixes:
* Fix turtles not getting advancements when turtles are on.
* Draw in-hand pocket computers with the correct transparent flags enabled.
* Several bug fixes to SNBT parsing.
* Fix several programs using their original name instead of aliases in usage hints (Lupus590).

# New features in CC: Tweaked 1.93.1

* Various documentation improvements (Lemmmy).
* Fix TBO monitor renderer on some older graphics cards (Lemmmy).

# New features in CC: Tweaked 1.93.0

* Update Swedish translations (Granddave).
* Printers use item tags to check dyes.
* HTTP rules may now be targetted for a specific port.
* Don't propagate adjacent redstone signals through computers.

And several bug fixes:
* Fix NPEs when turtles interact with containers.

# New features in CC: Tweaked 1.92.0

* Bump Cobalt version:
  * Add support for the __pairs metamethod.
  * string.format now uses the __tostring metamethod.
* Add date-specific MOTDs (MCJack123).

And several bug fixes:
* Correctly handle tabs within textutils.unserailizeJSON.
* Fix sheep not dropping items when sheared by turtles.

# New features in CC: Tweaked 1.91.1

* Fix crash when turtles interact with an entity.

# New features in CC: Tweaked 1.91.0

* [Generic peripherals] Expose NBT hashes of items to inventory methods.
* Bump Cobalt version
  * Optimise handling of string concatenation.
  * Add string.{pack,unpack,packsize} (MCJack123)
* Update to 1.16.2

And several bug fixes:
* Escape non-ASCII characters in JSON strings (neumond)
* Make field names in fs.attributes more consistent (abby)
* Fix textutils.formatTime correctly handle 12 AM (R93950X)
* Fix turtles placing buckets multiple times.

# New features in CC: Tweaked 1.90.3

* Fix the selected slot indicator missing from the turtle GUI.
* Ensure we load/save computer data from the world directory, rather than a global one.

# New features in CC: Tweaked 1.90.2

* Fix generic peripherals not being registered outside a dev environment.
* Fix `turtle.attack()` failing.
* Correctly set styles for the output of `/computercraft` commands.

# New features in CC: Tweaked 1.90.1

* Update to Forge 32.0.69

# New features in CC: Tweaked 1.90.0

* Add cc.image.nft module, for working with nft files. (JakobDev)
* [experimental] Provide a generic peripheral for any tile entity without an existing one. We currently provide methods for working with inventories, fluid tanks and energy storage. This is disabled by default, and must be turned on in the config.
* Add configuration to control the sizes of monitors and terminals.
* Add configuration to control maximum render distance of monitors.
* Allow getting "detailed" information about an item, using `turtle.getItemDetail(slot, true)`. This will contain the same information that the generic peripheral supplies.

And several bug fixes:
* Add back config for allowing interacting with command computers.
* Fix write method missing from printers.
* Fix dupe bug when killing an entity with a turtle.
* Correctly supply port in the Host header (neumond).
* Fix `turtle.craft` failing when missing an argument.
* Fix deadlock when mistakenly "watching" an unloaded chunk.
* Fix full path of files being leaked in some errors.

# New features in CC: Tweaked 1.89.1

* Fix crashes when rendering monitors of varying sizes.

# New features in CC: Tweaked 1.89.0

* Compress monitor data, reducing network traffic by a significant amount.
* Allow limiting the bandwidth monitor updates use.
* Several optimisations to monitor rendering (@Lignum).
* Expose block and item tags to turtle.inspect and turtle.getItemDetail.

And several bug fixes:
* Fix settings.load failing on defined settings.
* Fix name of the `ejectDisk` peripheral method.

# New features in CC: Tweaked 1.88.1

* Fix error on objects with too many methods.

# New features in CC: Tweaked 1.88.0

* Computers and turtles now preserve their ID when broken.
* Add `peripheral.getName` - returns the name of a wrapped peripheral.
* Reduce network overhead of monitors and terminals.
* Add a TBO backend for monitors, with a significant performance boost.
* The Lua REPL warns when declaring locals (lupus590, exerro)
* Add config to allow using command computers in survival.
* Add fs.isDriveRoot - checks if a path is the root of a drive.
* `cc.pretty` can now display a function's arguments and where it was defined. The Lua REPL will show arguments by default.
* Move the shell's `require`/`package` implementation to a separate `cc.require` module.
* Move treasure programs into a separate external data pack.

And several bug fixes:
* Fix io.lines not accepting arguments.
* Fix settings.load using an unknown global (MCJack123).
* Prevent computers scanning peripherals twice.

# New features in CC: Tweaked 1.87.1

* Fix blocks not dropping items in survival.

# New features in CC: Tweaked 1.87.0

* Add documentation to many Lua functions. This is published online at https://tweaked.cc/.
* Replace to pretty-printer in the Lua REPL. It now supports displaying functions and recursive tables. This printer is may be used within your own code through the `cc.pretty` module.
* Add `fs.getCapacity`. A complement to `fs.getFreeSpace`, this returns the capacity of the supplied drive.
* Add `fs.getAttributes`. This provides file size and type, as well as creation and modification time.
* Update Cobalt version. This backports several features from Lua 5.2 and 5.3:
  - The `__len` metamethod may now be used by tables.
  - Add `\z`, hexadecimal (`\x00`) and unicode (`\u0000`) string escape codes.
  - Add `utf8` lib.
  - Mirror Lua's behaviour of tail calls more closely. Native functions are no longer tail called, and tail calls are displayed in the stack trace.
  - `table.unpack` now uses `__len` and `__index` metamethods.
  - Parser errors now include the token where the error occured.
* Add `textutils.unserializeJSON`. This can be used to decode standard JSON and stringified-NBT.
* The `settings` API now allows "defining" settings. This allows settings to specify a default value and description.
* Enable the motd on non-pocket computers.
* Allow using the menu with the mouse in edit and paint (JakobDev).
* Add Danish and Korean translations (ChristianLW, mindy15963)
* Fire `mouse_up` events in the monitor program.
* Allow specifying a timeout to `websocket.receive`.
* Increase the maximimum limit for websocket messages.
* Optimise capacity checking of computer/disk folders.

And several bug fixes:
* Fix turtle texture being incorrectly oriented (magiczocker10).
* Prevent copying folders into themselves.
* Normalise file paths within shell.setDir (JakobDev)
* Fix turtles treating waterlogged blocks as water.
* Register an entity renderer for the turtle's fake player.

# New features in CC: Tweaked 1.86.2

* Fix peripheral.getMethods returning an empty table
* Update to Minecraft 1.15.2. This is currently alpha-quality and so is missing
  missing features and may be unstable.

# New features in CC: Tweaked 1.86.1

* Add a help message to the Lua REPL's exit function
* Add more MOTD messages. (osmarks)
* GPS requests are now made anonymously (osmarks)
* Minor memory usage improvements to Cobalt VM.

And several bug fixes:
* Fix error when calling `write` with a number.
* Add missing assertion to `io.write`.
* Fix incorrect coordinates in `mouse_scroll` events.

# New features in CC: Tweaked 1.86.0

* Add PATCH and TRACE HTTP methods. (jaredallard)
* Add more MOTD messages. (JakobDev)
* Allow removing and adding turtle upgrades via CraftTweaker.

And several bug fixes:
* Fix crash when interacting with Wearable Backpacks.

# New features in CC: Tweaked 1.85.2

* Fix crashes when using the mouse with advanced computers.

# New features in CC: Tweaked 1.85.1

* Add basic mouse support to `read`

And several bug fixes:
* Fix turtles not having breaking particles.
* Correct rendering of monitors when underwater.
* Adjust the position from where turtle performs actions, correcting the behaviour of some interactions.
* Fix several crashes when the turtle performs some action.

# New features in CC: Tweaked 1.85.0

* Window.reposition now allows changing the redirect buffer
* Add cc.completion and cc.shell.completion modules
* command.exec also returns the number of affected objects, when exposed by the game.

And several bug fixes:
* Change how turtle mining drops are handled, improving compatibility with some mods.
* Fix several GUI desyncs after a turtle moves.
* Fix os.day/os.time using the incorrect world time.
* Prevent wired modems dropping incorrectly.
* Fix mouse events not firing within the computer GUI.

# New features in CC: Tweaked 1.84.1

* Update to latest Forge

# New features in CC: Tweaked 1.84.0

* Improve validation in rename, copy and delete programs
* Add window.getLine - the inverse of blit
* turtle.refuel no longer consumes more fuel than needed
* Add "cc.expect" module, for improved argument type checks
* Mount the ROM from all mod jars, not just CC's

And several bug fixes:
* Ensure file error messages use the absolute correct path
* Fix NPE when closing a file multiple times.
* Do not load chunks when calling writeDescription.
* Fix the signature of loadfile
* Fix turtles harvesting blocks multiple times
* Improve thread-safety of various peripherals
* Prevent printed pages having massive/malformed titles

# New features in CC: Tweaked 1.83.1

* Add several new MOTD messages (JakobDev)

And several bug fixes:
* Fix type check in `rednet.lookup`
* Error if turtle and pocket computer programs are run on the wrong system (JakobDev)
* Do not discard varargs after a nil.

# New features in CC: Tweaked 1.83.0

* Add Chinese translation (XuyuEre)
* Small performance optimisations for packet sending.
* Provide an `arg` table to programs fun from the shell, similar to PUC Lua.
* Add `os.date`, and handle passing datetime tables to `os.time`, making them largely compatible with PUC Lua.
* `rm` and `mkdir` accept multiple arguments (hydraz, JakobDev).
* Rework rendering of in-hand pocket computers.
* Prevent rendering of a bounding box on a monitor's screen.
* Refactor Lua-side type checking code into a single method. Also include the function name in error messages.

And several bug fixes:
* Fix incorrect computation of server-tick budget.
* Fix list-based config options not reloading.
* Ensure `require` is usable within the Lua REPL.

# New features in CC: Tweaked 1.82.3

* Make computers' redstone input handling consistent with repeaters. Redstone inputs parallel to the computer will now be picked up.

And several bug fixes:
* Fix `turtle.compare*()` crashing the server.
* Fix Cobalt leaking threads when coroutines blocked on Java methods are discarded.
* Fix `rawset` allowing nan keys
* Fix several out-of-bounds exceptions when handling malformed patterns.

# New features in CC: Tweaked 1.82.2

* Don't tie `turtle.refuel`/the `refuel` script's limits to item stack sizes

And several bug fixes:
* Fix changes to Project:Red inputs not being detected.
* Convert non-modem peripherals to multiparts too, fixing crash with Proportional Destruction Particles
* Remove a couple of over-eager error messages
* Fix wired modems not correctly saving their attached peripherals

# New features in CC: Tweaked 1.82.1

* Make redstone updates identical to vanilla behaviour
* Update German translation

# New features in CC: Tweaked 1.82.0

* Warn when `pastebin put` potentially triggers spam protection (Lemmmy)
* Display HTTP errors on pastebin requests (Lemmmy)
* Attach peripherals on the main thread, rather than deferring to the computer thread.
* Computers may now be preemptively interrupted if they run for too long. This reduces the risk of malicious or badly written programs making other computers unusable.
* Reduce overhead of running with a higher number of computer threads.
* Set the initial multishell tab when starting the computer. This fixes the issue where you would not see any content until the first yield.
* Allow running `pastebin get|url` with the URL instead (e.g. `pastebin run https://pastebin.com/LYAxmSby`).
* Make `os.time`/`os.day` case insensitive.
* Add translations for several languages: Brazilian Portuguese (zardyh), Swedish (nothjarnan), Italian (Ale32bit), French(absolument), German (Wilma456), Spanish (daelvn)
* Improve JEI integration for turtle/pocket computer upgrades. You can now see recipes and usages of any upgrade or upgrade combination.
* Associate turtle/pocket computer upgrades with the mod which registered them. For instance, a "Sensing Turtle" will now be labelled as belonging to Plethora.
* Fire `key_up` and `mouse_up` events when closing the GUI.
* Allow limiting the amount of server time computers can consume.
* Add several new events for turtle refuelling and item inspection. Should allow for greater flexibility in add on mods in the future.
* `rednet.send` returns if the message was sent. Restores behaviour present before CC 1.6 (Luca0208)
* Add MCMP integration for wireless and ender modems.
* Make turtle crafting more consistent with vanilla behaviour.
* `commands.getBlockInfo(s)` now also includes NBT.
* Turtles will no longer reset their label when clicked with an unnamed name tag.

And several bug fixes:
* Update Cobalt (fixes `load` not unwind the stack)
* Fix `commands.collapseArgs` appending a trailing space.
* Fix leaking file descriptors when closing (see [this JVM bug!](https://bugs.java.com/bugdatabase/view_bug.do?bug_id=JDK-8220477))
* Fix NPE on some invalid URLs
* Fix pocket computer API working outside of the player inventory
* Fix printing not updating the output display state.

# New features in CC: Tweaked 1.81.1

* Fix colour.*RGB using 8-bit values, rather than 0-1 floats.

# New features in CC: Tweaked 1.81.0

* Handle connection errors on websockets (Devilholk)
* Make `require` a little more consistent with PUC Lua, passing the required name to modules and improving error messages.
* Track how long each turtle action takes within the profiling tools
* Bump Cobalt version
  * Coroutines are no longer backed by threads, reducing overhead of coroutines.
  * Maximum stack depth is much larger (2^16 rather than 2^8)
  * Stack is no longer unwound when an unhandled error occurs, meaning `debug.traceback` can be used on dead coroutines.
* Reduce jar size by reducing how many extra files we bundle.
* Add `term.nativePaletteColo(u)r` (Lignum)
* Split `colours.rgb8` into `colours.packRGB` and `colours.unpackRGB` (Lignum)
* Printers now only accept paper and ink, rather than any item
* Allow scrolling through the multishell tab bar, when lots of programs are running. (Wilma456)

And several bug fixes:
* Fix modems not being advanced when they should be
* Fix direction of some peripheral blocks not being set
* Strip `\r` from `.readLine` on binary handles.
* Websockets handle pings correctly
* Fix turtle peripherals becoming desynced on chunk unload.
* `/computercraft` table are now truncated correctly.

# New features in CC: Tweaked 1.80pr1.14

* Allow seeking within ROM files.
* Fix not being able to craft upgraded turtles or pocket computers when Astral Sorcery was installed.
* Make several tile entities (modems, cables, and monitors) non-ticking, substantially reducing their overhead,

And several bug fixes:
* Fix cables not rendering the breaking steps
* Try to prevent `/computercraft_copy` showing up in auto-complete.
* Fix several memory leaks and other issues with ROM mounts.

# New features in CC: Tweaked 1.80pr1.13

* `websocket_message` and `.receive` now return whether a message was binary or not.
* `websocket_close` events may contain a status code and reason the socket was closed.
* Enable the `debug` library by default.
* Clean up configuration files, moving various properties into sub-categories.
* Rewrite the HTTP API to use Netty.
* HTTP requests may now redirect from http to https if the server requests it.
* Add config options to limit various parts of the HTTP API:
  * Restrict the number of active http requests and websockets.
  * Limit the size of HTTP requests and responses.
  * Introduce a configurable timeout
* `.getResponseCode` also returns the status text associated with that status code.

And several bug fixes:
* Fix being unable to create resource mounts from individual files.
* Sync computer state using TE data instead.
* Fix `.read` always consuming a multiple of 8192 bytes for binary handles.

# New features in CC: Tweaked 1.80pr1.12

* Using longs inside `.seek` rather than 32 bit integers. This allows you to seek in very large files.
* Move the `/computer` command into the main `/computercraft` command
* Allow copying peripheral names from a wired modem's attach/detach chat message.

And several bug fixes
* Fix `InventoryUtil` ignoring the stack limit when extracting items
* Fix computers not receiving redstone inputs sent through another block.
* Fix JEI responding to key-presses when within a computer or turtle's inventory.

# New features in CC: Tweaked 1.80pr1.11

* Rename all tile entities to have the correct `computercraft:` prefix.
* Fix files not being truncated when opened for a write.
* `.read*` methods no longer fail on malformed unicode. Malformed input is replaced with a fake character.
* Fix numerous issues with wireless modems being attached to wired ones.
* Prevent deadlocks within the wireless modem code.
* Create coroutines using a thread pool, rather than creating a new thread each time. Should make short-lived coroutines (such as iterators) much more performance friendly.
* Create all CC threads under appropriately named thread groups.

# New features in CC: Tweaked 1.80pr1.10

This is just a minor bugfix release to solve some issues with the filesystem rewrite
* Fix computers not loading if resource packs are enabled
* Fix stdin not being recognised as a usable input
* Return an unsigned byte rather than a signed one for no-args `.read()`

# New features in CC: Tweaked 1.80pr1.9

* Add German translation (Vexatos)
* Add `.getCursorBlink` to monitors and terminals.
* Allow sending binary messages with websockets.
* Extend `fs` and `io` APIs
   * `io` should now be largely compatible with PUC Lua's implementation (`:read("n")` is not currently supported).
   * Binary readable file handles now support `.readLine`
   * Binary file handles now support `.seek(whence: string[, position:number])`, taking the same arguments as PUC Lua's method.

And several bug fixes:
* Fix `repeat` program crashing when malformed rednet packets are received (gollark/osmarks)
* Reduce risk of deadlock when calling peripheral methods.
* Fix speakers being unable to play sounds.

# New features in CC: Tweaked 1.80pr1.8

* Bump Cobalt version
  * Default to using little endian in string.dump
  * Remove propagation of debug hooks to child coroutines
  * Allow passing functions to `debug.getlocal`, al-la Lua 5.2
* Add Charset support for bundled cables
* `/computercraft` commands are more generous in allowing computer selectors to fail.
* Remove bytecode loading disabling from bios.lua.

And several bug fixes:
* Fix stack overflow when using `turtle.place` with a full inventory
* Fix in-hand printout rendering causing visual glitches.

# New features in CC: Tweaked 1.80pr1.7

 * Add `.getNameLocal` to wired modems: provides the name that computer is exposed as on the network. This is mostly useful for working with Plethora's transfer locations, though could have other purposes.
 * Change turtle block breaking to closer conform to how players break blocks.
 * Rewrite rendering of printed pages, allowing them to be held in hand, and placed in item frames.

And several bug fixes:
 * Improve formatting of `/computercraft` when run by a non-player.
 * Fix pocket computer terminals not updating when being held.
 * Fix a couple of minor blemishes in the GUI textures.
 * Fix sign text not always being set when placed.
 * Cache turtle fakeplayer, hopefully proving some minor performance improvements.

# New features in CC: Tweaked 1.80pr1.6

* Allow network cables to work with compact machines.
* A large number of improvements to the `/computercraft` command, including:
  * Ensure the tables are correctly aligned
  * Remove the output of the previous invocation of that command when posting to chat.
  * `/computercraft track` is now per-user, instead of global.
  * We now track additional fields, such as the number of peripheral calls, http requests, etc... You can specify these as an optional argument to `/computercraft track dump` to see them.
* `wget` automatically determines the filename (Luca0208)
* Allow using alternative HTTP request methods (`DELETE`, `PUT`, etc...)
* Enable Gzip compression for websockets.
* Fix monitors not rendering when optifine shaders are enabled. There are still issues (they are tinted orange during the night), but it is an improvement.

And several bug fixes:
* Fix `.isDiskPresent()` always returning true.
* Fix peripherals showing up on wired networks when they shouldn't be.
* Fix `turtle.place()` crashing the server in some esoteric conditions.
* Remove upper bound on the number of characters than can be read with `.read(n: number)`.
* Fix various typos in `keys.lua` (hugeblank)

# New features in CC: Tweaked 1.80pr1.5

* Several additional fixes to monitors, solving several crashes and graphical glitches.
* Add recipes to upgrade computers, turtles and pocket computers.

# New features in CC: Tweaked 1.80pr1.4

* Verify the action can be completed in `copy`, `rename` and `mkdir` commands.
* Add `/rom/modules` so the package path.
* Add `read` to normal file handles - allowing reading a given number of characters.
* Various minor bug fixes.
* Ensure ComputerCraft peripherals are thread-safe. This fixes multiple Lua errors and crashes with modems monitors.
* Add `/computercraft track` command, for monitoring how long computers execute for.
* Add ore dictionary support for recipes.
* Track which player owns a turtle. This allows turtles to play nicely with various claim/grief prevention systems.
* Add config option to disable various turtle actions.
* Add an API for extending wired networks.
* Add full-block wired modems.
* Several minor bug fixes.

# New features in CC: Tweaked 1.80pr1.3

* Add `/computercraft` command, providing various diagnostic tools.
* Make `http.websocket` synchronous and add `http.websocketAsync`.
* Restore binary compatibility for `ILuaAPI`.

# New features in CC: Tweaked 1.80pr1.2

* Fix `term.getTextScale()` not working across multiple monitors.
* Fix computer state not being synced to client when turning on/off.
* Provide an API for registering custom APIs.
* Render turtles called "Dinnerbone" or "Grumm" upside*down.
* Fix `getCollisionBoundingBox` not using all AABBs.
* **Experimental:** Add map-like rendering for pocket computers.

# New features in CC: Tweaked 1.80pr1.1

* Large numbers of bug fixes, stabilisation and hardening.
* Replace LuaJ with Cobalt.
* Allow running multiple computers at the same time.
* Add config option to enable Lua's debug API.
* Add websocket support to HTTP library.
* Add `/computer` command, allowing one to queue events on command computers.
* Fix JEI's handling of various ComputerCraft items.
* Make wired cables act more like multiparts.
* Add turtle and pocket recipes to recipe book.
* Flash pocket computer's light when playing a note.

# New Features in ComputerCraft 1.80pr1:

* Update to Minecraft 1.12.2
* Large number of bug fixes and stabilisation.
* Allow loading bios.lua files from resource packs.
* Fix texture artefacts when rendering monitors.
* Improve HTTP whitelist functionality and add an optional blacklist.
* Add support for completing Lua's self calls (`foo:bar()`).
* Add binary mode to HTTP.
* Use file extensions for ROM files.
* Automatically add `.lua` when editing files, and handle running them in the shell.
* Add require to the shell environment.
* Allow startup to be a directory.
* Add speaker peripheral and corresponding turtle and pocket upgrades.
* Add pocket computer upgrades.
* Allow turtles and pocket computers to be dyed any colour.
* Allow computer and monitors to configure their palette. Also allow normal computer/monitors to use any colour converting it to greyscale.
* Add extensible pocket computer upgrade system, including ender modem upgrade.
* Add config option to limit the number of open files on a computer.
* Monitors glow in the dark.
* http_failure event includes the HTTP handle if available.
* HTTP responses include the response headers.

# New Features in ComputerCraft 1.79:

* Ported ComputerCraftEdu to Minecraft 1.8.9
* Fixed a handful of bugs in ComputerCraft

# New Features in ComputerCraft 1.77:

* Ported to Minecraft 1.8.9
* Added `settings` API
* Added `set` and `wget` programs
* Added settings to disable multishell, startup scripts, and tab completion on a per-computer basis. The default values for these settings can be customised in ComputerCraft.cfg
* All Computer and Turtle items except Command Computers can now be mounted in Disk Drives

# New Features in ComputerCraft 1.76:

* Ported to Minecraft 1.8
* Added Ender Modems for cross-dimensional communication
* Fixed handling of 8-bit characters. All the characters in the ISO 8859-1 codepage can now be displayed
* Added some extra graphical characters in the unused character positions, including a suite of characters for Teletext style drawing
* Added support for the new commands in Minecraft 1.8 to the Command Computer
* The return values of `turtle.inspect()` and `commands.getBlockInfo()` now include blockstate information
* Added `commands.getBlockInfos()` function for Command Computers
* Added new `peripherals` program
* Replaced the `_CC_VERSION` and `_MC_VERSION` constants with a new `_HOST` constant
* Shortened the length of time that "Ctrl+T", "Ctrl+S" and "Ctrl+R" must be held down for to terminate, shutdown and reboot the computer
* `textutils.serialiseJSON()` now takes an optional parameter allowing it to produce JSON text with unquoted object keys. This is used by all autogenerated methods in the `commands` api except for "title" and "tellraw"
* Fixed many bugs

# New Features in ComputerCraft 1.75:

* Fixed monitors sometimes rendering without part of their text.
* Fixed a regression in the `bit` API.

# New Features in ComputerCraft 1.74:

* Added tab completion to `edit`, `lua` and the shell.
* Added `textutils.complete()`, `fs.complete()`, `shell.complete()`, `shell.setCompletionFunction()` and `help.complete()`.
* Added tab completion options to `read()`.
* Added `key_up` and `mouse_up` events.
* Non-advanced terminals now accept both grey colours.
* Added `term.getTextColour()`, `term.getBackgroundColour()` and `term.blit()`.
* Improved the performance of text rendering on Advanced Computers.
* Added a "Run" button to the edit program on Advanced Computers.
* Turtles can now push players and entities (configurable).
* Turtles now respect server spawn protection (configurable).
* Added a turtle permissions API for mod authors.
* Implemented a subset of the Lua 5.2 API so programs can be written against it now, ahead of a future Lua version upgrade.
* Added a config option to disable parts of the Lua 5.1 API which will be removed when a future Lua version upgrade happens.
* Command Computers can no longer be broken by survival players.
* Fixed the "pick block" key not working on ComputerCraft items in creative mode.
* Fixed the `edit` program being hard to use on certain European keyboards.
* Added `_CC_VERSION` and `_MC_VERSION` constants.

# New Features in ComputerCraft 1.73:

* The `exec` program, `commands.exec()` and all related Command Computer functions now return the console output of the command.
* Fixed two multiplayer crash bugs.

# New Features in ComputerCraft 1.7:

* Added Command Computers
* Added new API: `commands`
* Added new programs: `commands`, `exec`
* Added `textutils.serializeJSON()`
* Added `ILuaContext.executeMainThreadTask()` for peripheral developers
* Disk Drives and Printers can now be renamed with Anvils
* Fixed various bugs, crashes and exploits
* Fixed problems with HD texture packs
* Documented the new features in the in-game help

# New Features in ComputerCraft 1.65:

* Fixed a multiplayer-only crash with `turtle.place()`
* Fixed some problems with `http.post()`
* Fixed `fs.getDrive()` returning incorrect results on remote peripherals

# New Features in ComputerCraft 1.64:

* Ported to Minecraft 1.7.10
* New turtle functions: `turtle.inspect()`, `turtle.inspectUp()`, `turtle.inspectDown()`, `turtle.getItemDetail()`
* Lots of bug and crash fixes, a huge stability improvement over previous versions

# New Features in ComputerCraft 1.63:

* Turtles can now be painted with dyes, and cleaned with water buckets
* Added a new game: Redirection - ComputerCraft Edition
* Turtle label nameplates now only show when the Turtle is moused-over
* The HTTP API is now enabled by default, and can be configured with a whitelist of permitted domains
* `http.get()` and `http.post()` now accept parameters to control the request headers
* New fs function: `fs.getDir( path )`
* Fixed some bugs

# New Features in ComputerCraft 1.62:

* Added IRC-style commands to the `chat` program
* Fixed some bugs and crashes

# New Features in ComputerCraft 1.6:

* Added Pocket Computers
* Added a multi-tasking system for Advanced Computers and Turtles
* Turtles can now swap out their tools and peripherals at runtime
* Turtles can now carry two tools or peripherals at once in any combination
* Turtles and Computers can now be labelled using Name Tags and Anvils
* Added a configurable fuel limit for Turtles
* Added hostnames, protocols and long distance routing to the rednet API
* Added a peer-to-peer chat program to demonstrate new rednet capabilities
* Added a new game, only on Pocket Computers: "falling" by GopherATL
* File system commands in the shell now accept wildcard arguments
* The shell now accepts long arguments in quotes
* Terminal redirection now no longer uses a stack-based system. Instead: `term.current()` gets the current terminal object and `term.redirect()` replaces it. `term.restore()` has been removed.
* Added a new Windowing API for addressing sub-areas of the terminal
* New programs: `fg`, `bg`, `multishell`, `chat`, `repeat`, `redstone`, `equip`, `unequip`
* Improved programs: `copy`, `move`, `delete`, `rename`, `paint`, `shell`
* Removed programs: `redset`, `redprobe`, `redpulse`
* New APIs: `window`, `multishell`
* New turtle functions: `turtle.equipLeft()` and `turtle.equipRight()`
* New peripheral functions: `peripheral.find( [type] )`
* New rednet functions: `rednet.host( protocol, hostname )`, `rednet.unhost( protocol )`, `rednet.locate( protocol, [hostname] )`
* New fs function: `fs.find( wildcard )`
* New shell functions: `shell.openTab()`, `shell.switchTab( [number] )`
* New event `term_resize` fired when the size of a terminal changes
* Improved rednet functions: `rednet.send()`, `rednet.broadcast()` and `rednet.receive()`now take optional protocol parameters
* `turtle.craft(0)` and `turtle.refuel(0)` now return true if there is a valid recipe or fuel item, but do not craft of refuel anything
* `turtle.suck( [limit] )` can now be used to limit the number of items picked up
* Users of `turtle.dig()` and `turtle.attack()` can now specify which side of the turtle to look for a tool to use (by default, both will be considered)
* `textutils.serialise( text )` now produces human-readable output
* Refactored most of the codebase and fixed many old bugs and instabilities, turtles should never ever lose their content now
* Fixed the `turtle_inventory` event firing when it shouldn't have
* Added error messages to many more turtle functions after they return false
* Documented all new programs and API changes in the `help` system

# New Features in ComputerCraft 1.58:

* Fixed a long standing bug where turtles could lose their identify if they travel too far away
* Fixed use of deprecated code, ensuring mod compatibility with the latest versions of Minecraft Forge, and world compatibility with future versions of Minecraft

# New Features in ComputerCraft 1.57:

* Ported to Minecraft 1.6.4
* Added two new Treasure Disks: Conway's Game of Life by vilsol and Protector by fredthead
* Fixed a very nasty item duplication bug

# New Features in ComputerCraft 1.56:

* Added Treasure Disks: Floppy Disks in dungeons which contain interesting community made programs. Find them all!
* All turtle functions now return additional error messages when they fail.
* Resource Packs with Lua Programs can now be edited when extracted to a folder, for easier editing.

# New Features in ComputerCraft 1.55:

* Ported to Minecraft 1.6.2
* Added Advanced Turtles
* Added `turtle_inventory` event. Fires when any change is made to the inventory of a turtle
* Added missing functions `io.close`, `io.flush`, `io.input`, `io.lines`, `io.output`
* Tweaked the screen colours used by Advanced Computers, Monitors and Turtles
* Added new features for Peripheral authors
* Lua programs can now be included in Resource Packs

# New Features in ComputerCraft 1.52:

* Ported to Minecraft 1.5.1

# New Features in ComputerCraft 1.51:

* Ported to Minecraft 1.5
* Added Wired Modems
* Added Networking Cables
* Made Wireless Modems more expensive to craft
* New redstone API functions: `getAnalogInput()`, `setAnalogOutput()`, `getAnalogOutput()`
* Peripherals can now be controlled remotely over wired networks. New peripheral API function: `getNames()`
* New event: `monitor_resize` when the size of a monitor changes
* Except for labelled computers and turtles, ComputerCraft blocks no longer drop items in creative mode
* The pick block function works in creative mode now works for all ComputerCraft blocks
* All blocks and items now use the IDs numbers assigned by FTB by default
* Fixed turtles sometimes placing blocks with incorrect orientations
* Fixed Wireless modems being able to send messages to themselves
* Fixed `turtle.attack()` having a very short range
* Various bugfixes

# New Features in ComputerCraft 1.5:

* Redesigned Wireless Modems; they can now send and receive on multiple channels, independent of the computer ID. To use these features, interface with modem peripherals directly. The rednet API still functions as before
* Floppy Disks can now be dyed with multiple dyes, just like armour
* The `excavate` program now retains fuel in it's inventory, so can run unattended
* `turtle.place()` now tries all possible block orientations before failing
* `turtle.refuel(0)` returns true if a fuel item is selected
* `turtle.craft(0)` returns true if the inventory is a valid recipe
* The in-game help system now has documentation for all the peripherals and their methods, including the new modem functionality
* A romantic surprise

# New Features in ComputerCraft 1.48:

* Ported to Minecraft 1.4.6
* Advanced Monitors now emit a `monitor_touch` event when right clicked
* Advanced Monitors are now cheaper to craft
* Turtles now get slightly less fuel from items
* Computers can now interact with Command Blocks (if enabled in ComputerCraft.cfg)
* New API function: `os.day()`
* A christmas surprise

# New Features in ComputerCraft 1.45:

* Added Advanced Computers
* Added Advanced Monitors
* New program: paint by nitrogenfingers
* New API: `paintutils`
* New term functions: `term.setBackgroundColor`, `term.setTextColor`, `term.isColor`
* New turtle function: `turtle.transferTo`

# New Features in ComputerCraft 1.43:

* Added Printed Pages
* Added Printed Books
* Fixed incompatibility with Forge 275 and above
* Labelled Turtles now keep their fuel when broken

# New Features in ComputerCraft 1.42:

* Ported to Minecraft 1.3.2
* Added Printers
* Floppy Disks can be dyed different colours
* Wireless Crafty Turtles can now be crafted
* New textures
* New forge config file
* Bug fixes

# New Features in ComputerCraft 1.4:

* Ported to Forge Mod Loader. ComputerCraft can now be ran directly from the .zip without extraction
* Added Farming Turtles
* Added Felling Turtles
* Added Digging Turtles
* Added Melee Turtles
* Added Crafty Turtles
* Added 14 new Turtle Combinations accessible by combining the turtle upgrades above
* Labelled computers and turtles can now be crafted into turtles or other turtle types without losing their ID, label and data
* Added a "Turtle Upgrade API" for mod developers to create their own tools and peripherals for turtles
* Turtles can now attack entities with `turtle.attack()`, and collect their dropped items
* Turtles can now use `turtle.place()` with any item the player can, and can interact with entities
* Turtles can now craft items with `turtle.craft()`
* Turtles can now place items into inventories with `turtle.drop()`
* Changed the behaviour of `turtle.place()` and `turtle.drop()` to only consider the currently selected slot
* Turtles can now pick up items from the ground, or from inventories, with `turtle.suck()`
* Turtles can now compare items in their inventories
* Turtles can place signs with text on them with `turtle.place( [signText] )`
* Turtles now optionally require fuel items to move, and can refuel themselves
* The size of the the turtle inventory has been increased to 16
* The size of the turtle screen has been increased
* New turtle functions: `turtle.compareTo( [slotNum] )`, `turtle.craft()`, `turtle.attack()`, `turtle.attackUp()`, `turtle.attackDown()`, `turtle.dropUp()`, `turtle.dropDown()`, `turtle.getFuelLevel()`, `turtle.refuel()`
* New disk function: disk.getID()
* New turtle programs: `craft`, `refuel`
* `excavate` program now much smarter: Will return items to a chest when full, attack mobs, and refuel itself automatically
* New API: `keys`
* Added optional Floppy Disk and Hard Drive space limits for computers and turtles
* New `fs` function: `fs.getFreeSpace( path )`, also `fs.getDrive()` works again
* The send and receive range of wireless modems now increases with altitude, allowing long range networking from high-altitude computers (great for GPS networks)
* `http.request()` now supports https:// URLs
* Right clicking a Disk Drive with a Floppy Disk or a Record when sneaking will insert the item into the Disk Drive automatically
* The default size of the computer screen has been increased
* Several stability and security fixes. LuaJ can now no longer leave dangling threads when a computer is unloaded, turtles can no longer be destroyed by tree leaves or walking off the edge of the loaded map. Computers no longer crash when used with RedPower frames.

# New Features in ComputerCraft 1.31:

* Ported to Minecraft 1.2.3
* Added Monitors (thanks to Cloudy)
* Updated LuaJ to a newer, less memory hungry version
* `rednet_message` event now has a third parameter, "distance", to support position triangulation.
* New programs: `gps`, `monitor`, `pastebin`.
* Added a secret program. Use with large monitors!
* New apis: `gps`, `vector`
* New turtle functions: `turtle.compare()`, `turtle.compareUp()`, `turtle.compareDown()`, `turtle.drop( quantity )`
* New `http` functions: `http.post()`.
* New `term` functions: `term.redirect()`, `term.restore()`
* New `textutils` functions: `textutils.urlEncode()`
* New `rednet` functions: `rednet.isOpen()`
* New config options: modem_range, modem_rangeDuringStorm
* Bug fixes, program tweaks, and help updates

# New Features in ComputerCraft 1.3:

* Ported to Minecraft Forge
* Added Turtles
* Added Wireless Modems
* Added Mining Turtles
* Added Wireless Turtles
* Added Wireless Mining Turtles
* Computers and Disk Drives no longer get destroyed by water.
* Computers and Turtles can now be labelled with the label program, and labelled devices keep their state when destroyed.
* Computers/Turtles can connect to adjacent devices, and turn them on and off
* User programs now give line numbers in their error messages
* New APIs: `turtle`, `peripheral`
* New programs for turtles: tunnel, excavate, go, turn, dance
* New os functions: `os.getComputerLabel()`, `os.setComputerLabel()`
* Added "filter" parameter to `os.pullEvent()`
* New shell function: `shell.getCurrentProgram()`
* New textutils functions: `textutils.serialize()`, `textutils.unserialize()`, `textutils.tabulate()`, `textutils.pagedTabulate()`, `textutils.slowWrite()`
* New io file function: `file:lines()`
* New fs function: `fs.getSize()`
* Disk Drives can now play records from other mods
* Bug fixes, program tweaks, and help updates

# New Features in ComputerCraft 1.2:

* Added Disk Drives and Floppy Disks
* Added Ctrl+T shortcut to terminate the current program (hold)
* Added Ctrl+S shortcut to shutdown the computer (hold)
* Added Ctrl+R shortcut to reboot the computer (hold)
* New Programs: `alias`, `apis`, `copy`, `delete`, `dj`, `drive`, `eject`, `id`, `label`, `list`, `move`, `reboot`, `redset`, `rename`, `time`, `worm`.
* New APIs: `bit`, `colours`, `disk`, `help`, `rednet`, `parallel`, `textutils`.
* New color functions: `colors.combine()`, `colors.subtract()`, `colors.test()`
* New fs functions: `fs.getName()`, new modes for `fs.open()`
* New os functions: `os.loadAPI()`, `os.unloadAPI()`, `os.clock()`, `os.time()`, `os.setAlarm()`, `os.reboot()`, `os.queueEvent()`
* New redstone function: `redstone.getSides()`
* New shell functions: `shell.setPath()`, `shell.programs()`, `shell.resolveProgram()`, `shell.setAlias()`
* Lots of updates to the help pages
* Bug fixes

# New Features in ComputerCraft 1.1:

* Added Multiplayer support throughout.
* Added connectivity with RedPower bundled cables
* Added HTTP api, enabled via the mod config, to allow computers to access the real world internet
* Added command history to the shell.
* Programs which spin in an infinite loop without yielding will no longer freeze minecraft
* Help updates and bug fixes

# New Features in ComputerCraft 1.0:

* First Release!
