---
module: [kind=event] turtle_inventory
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`turtle_inventory`] event is fired when a turtle's inventory is changed.

## Return values
1. [`string`]: The event name.

## Example
Prints a message when the inventory is changed:
```lua
while true do
  os.pullEvent("turtle_inventory")
  print("The inventory was changed.")
end
```
