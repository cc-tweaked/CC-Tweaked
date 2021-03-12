---
module: [kind=event] alarm
see: os.setAlarm To start an alarm.
---

The @{timer} event is fired when an alarm started with @{os.setAlarm} completes.

## Return Values
1. @{string}: The event name.
2. @{number}: The ID of the alarm that finished.

## Example
Starts a timer and then prints its ID:
```lua
local alarmID = os.setAlarm(os.time() + 0.05)
local event, id
repeat
    event, id = os.pullEvent("alarm")
until id == alarmID
print("Alarm with ID " .. id .. " was fired")
```
