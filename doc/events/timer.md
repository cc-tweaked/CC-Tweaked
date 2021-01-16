---
module: [kind=event] timer
see: os.startTimer To start a timer.
---

The @{timer} event is fired when a timer started with @{os.startTimer} completes.

## Return Values
1. @{number}: The ID of the timer that finished.

## Example
Starts a timer and then prints its ID:
```lua
os.startTimer(2)
local event, id = os.pullEvent("timer")
print("Timer with ID " .. id .. " was fired")
```
