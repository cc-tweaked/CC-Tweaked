New features in CC: Tweaked 1.99.0

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

Type "help changelog" to see the full version history.
