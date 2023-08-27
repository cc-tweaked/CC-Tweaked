---
module: [kind=event] timer
see: os.startTimer To start a timer.
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`timer`] event is fired when a timer started with [`os.startTimer`] completes.

## Return Values
1. [`string`]: The event name.
2. [`number`]: The ID of the timer that finished.

## Example
Start and wait for a timer to finish.
```lua
local timer_id = os.startTimer(2)
local event, id
repeat
    event, id = os.pullEvent("timer")
until id == timer_id
print("Timer with ID " .. id .. " was fired")
```
