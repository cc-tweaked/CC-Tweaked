New features in CC: Tweaked 1.106.0

* Numerous documentation improvements (MCJack123, znepb, penguinencounter).
* Port `fs.find` to Lua. This also allows using `?` as a wildcard.
* Computers cursors now glow in the dark.
* Allow changing turtle upgrades from the GUI.
* Add option to serialize Unicode strings to JSON (MCJack123).
* Small optimisations to the `window` API.
* Turtle upgrades can now preserve NBT from upgrade item stack and when broken.
* Add support for tool enchantments and durability via datapacks. This is disabled for the built-in tools.

Several bug fixes:
* Fix turtles rendering incorrectly when upside down.
* Fix misplaced calls to IArguments.escapes.
* Lua REPL no longer accepts `)(` as a valid expression.
* Fix several inconsistencies with `require`/`package.path` in the Lua REPL (Wojbie).
* Fix turtle being able to place water buckets outside its reach distance.
* Fix private several IP address ranges not being blocked by the `$private` rule.
* Improve permission checks in the `/computercraft` command.

Type "help changelog" to see the full version history.
