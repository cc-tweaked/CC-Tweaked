New features in CC: Tweaked 1.108.4

* Rewrite `@LuaFunction` generation to use `MethodHandle`s instead of ASM.
* Refactor `ComputerThread` to provide a cleaner interface.
* Remove `disable_lua51_features` config option.
* Update several translations (Sammy).

Several bug fixes:
* Fix monitor peripheral becoming "detached" after breaking and replacing a monitor.
* Fix signs being empty when placed.
* Fix several inconsistencies with mount error messages.

Type "help changelog" to see the full version history.
