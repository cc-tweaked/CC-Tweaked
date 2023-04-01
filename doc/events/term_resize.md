---
module: [kind=event] term_resize
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The @{term_resize} event is fired when the main terminal is resized. For instance:
 - When a the tab bar is shown or hidden in @{multishell}.
 - When the terminal is redirected to a monitor via the "monitor" program and the monitor is resized.

When this event fires, some parts of the terminal may have been moved or deleted. Simple terminal programs (those
not using @{term.setCursorPos}) can ignore this event, but more complex GUI programs should redraw the entire screen.

## Return values
1. @{string}: The event name.

## Example
Print a message each time the terminal is resized.

```lua
while true do
  os.pullEvent("term_resize")
  local w, h = term.getSize()
  print("The term was resized to (" .. w .. ", " .. h .. ")")
end
```
