New features in CC: Tweaked 1.101.2

* Error messages in `edit` are now displayed in red on advanced computers.
* Improvements to the display of errors in the shell and REPL.

Several bug fixes:
* Fix `import.lua` failing to upload a file.
* Fix several issues with sparse Lua tables (Shiranuit).
* Computer upgrades now accept normal computers, rather than uselessly allowing you to upgrade an advanced computer to an advanced computer!
* Correctly clamp speaker volume.
* Fix rednet queueing the wrong message when sending a message to the current computer.
* Fix the Lua VM crashing when a `__len` metamethod yields.
* Trim spaces from filesystem paths.
* Correctly format 12AM/PM with `%I`.

Type "help changelog" to see the full version history.
