---
module: [kind=event] mouse_up
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

This event is fired when a mouse button is released or a held mouse leaves the computer's terminal.

## Return values
1. [`string`]: The event name.
2. [`number`]: The [mouse button](mouse_click.html#Mouse_buttons) that was released.
3. [`number`]: The X-coordinate of the mouse.
4. [`number`]: The Y-coordinate of the mouse.

## Example
Prints the coordinates and button number whenever the mouse is released.

```lua
while true do
  local event, button, x, y = os.pullEvent("mouse_up")
  print(("The mouse button %s was released at %d, %d"):format(button, x, y))
end
```
