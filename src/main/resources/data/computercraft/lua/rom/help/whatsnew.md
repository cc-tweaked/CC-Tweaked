New features in CC: Tweaked 1.99.1

* Add package.searchpath to the cc.require API. (MCJack123)
* Provide a more efficient way for the Java API to consume Lua tables in certain restricted cases.

Several bug fixes:
* Fix keys being "sticky" when opening the off-hand pocket computer GUI.
* Correctly handle broken coroutine managers resuming Java code with a `nil` event.
* Prevent computer buttons stealing focus from the terminal.
* Fix a class cast exception when a monitor is malformed in ways I do not quite understand.

Type "help changelog" to see the full version history.
