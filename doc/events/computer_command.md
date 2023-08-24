---
module: [kind=event] computer_command
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`computer_command`] event is fired when the `/computercraft queue` command is run for the current computer.

## Return Values
1. [`string`]: The event name.
2. [`string`]<abbr title="Variable number of arguments">&hellip;</abbr>: The arguments passed to the command.

## Example
Prints the contents of messages sent:
```lua
while true do
  local event = {os.pullEvent("computer_command")}
  print("Received message:", table.unpack(event, 2))
end
```
