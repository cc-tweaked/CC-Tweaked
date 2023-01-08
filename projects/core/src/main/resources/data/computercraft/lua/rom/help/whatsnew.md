New features in CC: Tweaked 1.102.1

Several bug fixes:
* Fix crash on Fabric when refuelling with a non-fuel item (emmachase).
* Fix crash when calling `pocket.equipBack()` with a wireless modem.
* Fix turtles dropping their inventory when moving (emmachase).
* Fix crash when inserting items into a full inventory (emmachase).
* Simplify wired cable breaking code, fixing items sometimes not dropping.
* Correctly handle double chests being treated as single threads under Fabric.
* Fix `mouse_up` not being fired under Fabric.
* Fix full-block Wired modems not connecting to adjacent cables when placed.
* Hide the search tab from the `itemGroups` item details.
* Fix speakers playing too loudly.
* Change where turtles drop items from, reducing the chance that items clip through blocks.
* Fix the `computer_threads` config option not applying under Fabric.

Type "help changelog" to see the full version history.
