New features in CC: Tweaked 1.121.0

* Support arbitrary HTTP verbs in `http.request`.
* Allow wrapping non-slotted Fabric inventories as peripherals.
* Rewrite turtle/pocket computer upgrade API. Upgrades now preserve the item they were equipped with.

Several bug fixes:
* Fix multishell not culling after changing focus.
* Fix `turtle.place()` using items if Forge event was cancelled.
* Fix computers not refreshing redstone output on world load.
* Send websocket close messages on `Websocket.close()` rather than terminating the connection.
* Fix various compatibility issues with SNBT parsing in `textutils.unserialiseJSON`.
* Apply HTTP rules to IPv4-mapped addresses.

Type "help changelog" to see the full version history.
