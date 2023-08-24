---
module: [kind=event] alarm
see: os.setAlarm To start an alarm.
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`alarm`] event is fired when an alarm started with [`os.setAlarm`] completes.

## Return Values
1. [`string`]: The event name.
2. [`number`]: The ID of the alarm that finished.

## Example
Starts a timer and then waits for it to complete.

```lua
local alarm_id = os.setAlarm(os.time() + 0.05)
local event, id
repeat
    event, id = os.pullEvent("alarm")
until id == alarm_id
print("Alarm with ID " .. id .. " was fired")
```
