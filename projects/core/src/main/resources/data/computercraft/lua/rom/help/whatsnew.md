New features in CC: Tweaked 1.105.0

* Optimise JSON string parsing.
* Add `colors.fromBlit` (Erb3).
* Upload file size limit is now configurable (khankul).
* Wired cables no longer have a distance limit.
* Java methods now coerce values to strings consistently with Lua.
* Add custom timeout support to the HTTP API.
* Support custom proxies for HTTP requests (Lemmmy).
* The `speaker` program now errors when playing HTML files.
* `edit` now shows an error message when editing read-only files.
* Update Ukranian translation (SirEdvin).

Several bug fixes:
* Allow GPS hosts to only be 1 block apart.
* Fix "Turn On"/"Turn Off" buttons being inverted in the computer GUI (Erb3).
* Fix arrow keys not working in the printout UI.
* Several documentation fixes (zyxkad, Lupus590, Commandcracker).
* Fix monitor renderer debug text always being visible on Forge.
* Fix crash when another mod changes the LoggerContext.
* Fix the `monitor_renderer` option not being present in Fabric config files.
* Pasting on MacOS/OSX now uses Cmd+V rather than Ctrl+V.
* Fix turtles placing blocks upside down when at y<0.

Type "help changelog" to see the full version history.
