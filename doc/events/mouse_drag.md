---
module: [kind=event] mouse_drag
see: mouse_click For when a mouse button is initially pressed.
---

This event is fired every time the mouse is moved while a mouse button is being held.

## Return values
1. @{string}: The event name.
2. @{number}: The [mouse button](mouse_click.html#Mouse_buttons) that is being pressed.
3. @{number}: The X-coordinate of the mouse.
4. @{number}: The Y-coordinate of the mouse.

## Example
Print the button and the coordinates whenever the mouse is dragged.

```lua
while true do
  local event, button, x, y = os.pullEvent("mouse_drag")
  print(("The mouse button %s was dragged at %d, %d"):format(button, x, y))
end
```
