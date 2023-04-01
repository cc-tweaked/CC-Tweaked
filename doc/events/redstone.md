---
module: [kind=event] redstone
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The @{event!redstone} event is fired whenever any redstone inputs on the computer change.

## Return values
1. @{string}: The event name.

## Example
Prints a message when a redstone input changes:
```lua
while true do
  os.pullEvent("redstone")
  print("A redstone input has changed!")
end
```
