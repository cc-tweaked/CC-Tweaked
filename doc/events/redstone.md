---
module: [kind=event] redstone
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: LicenseRef-CCPL
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
