---
module: [kind=event] task_complete
see: commands.execAsync To run a command which fires a task_complete event.
---

The @{task_complete} event is fired when a command run with @{commands.execAsync} or any function in @{commands.async} finishes.

This event is normally only available on Command Computers.

## Return Values
1. @{number}: The ID of the task as returned by @{commands.execAsync}.
2. @{boolean}: Whether the command succeeded.
3. @{string}: If the command failed, an error message explaining the failure. (This is not present if the command succeeded.)
...: Any parameters returned from the command.

## Example
Prints the results of an asynchronous command:
```lua
commands.execAsync("say Hello")
local event = {os.pullEvent("task_complete")}
if event[3] == true then
  print("Task " .. event[2] .. " succeeded:", table.unpack(event, 4))
else
  print("Task " .. event[2] .. " failed: " .. event[4])
end
```
