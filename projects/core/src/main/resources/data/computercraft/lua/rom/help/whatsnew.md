New features in CC: Tweaked 1.109.3

* Command computers now display in the operator items creative tab.

Several bug fixes:
* Error if too many websocket messages are queued to be sent at once.
* Fix trailing-comma on method calls (e.g. `x:f(a, )` not using our custom error message.
* Fix internal compiler error when using `goto` as the first statement in an `if` block.
* Fix incorrect incorrect resizing of a tables' hash part when adding and removing keys.

Type "help changelog" to see the full version history.
