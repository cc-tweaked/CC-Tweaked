New features in CC: Tweaked 1.104.0

* Update to Minecraft 1.19.4.
* Turtles can now right click items "into" certain blocks (cauldrons and hives by default, configurable with the `computercraft:turtle_can_use` block tag).
* Update Cobalt to 0.7:
  * `table` methods and `ipairs` now use metamethods.
  * Type errors now use the `__name` metatag.
  * Coroutines no longer run on multiple threads.
  * Timeout errors should be thrown more reliably.
* `speaker` program now reports an error on common unsupported audio formats.
* `multishell` now hides the implementation details of its terminal redirect from programs.
* Use VBO monitor renderer by default.
* Improve syntax errors when missing commas in tables, and on trailing commas in parameter lists.
* Turtles can now hold flags.
* Update several translations (Alessandro, chesiren, Erlend, RomanPlayer22).

Several bug fixes:
* `settings.load` now ignores malformed values created by editing the `.settings` file by hand.
* Fix introduction dates on `os.cancelAlarm` and `os.cancelTimer` (MCJack123).
* Fix the REPL syntax reporting crashing on valid parses.
* Make writes to the ID file atomic.
* Obey stack limits when transferring items with Fabric's APIs.
* Ignore metatables in `textutils.serialize`.
* Correctly recurse into NBT lists when computing the NBT hash (Lemmmy).
* Fix advanced pocket computers rendering as greyscale.
* Fix stack overflow when using `shell` as a hashbang program.
* Fix websocket messages being empty when using a non-default compression settings.
* Fix `gps.locate` returning `nan` when receiving a duplicate location (Wojbie).
* Remove several thread safety issues inside Java-side argument parsing code.

Type "help changelog" to see the full version history.
