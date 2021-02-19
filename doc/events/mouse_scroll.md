---
module: [kind=event] mouse_scroll
---

This event is fired when a mouse wheel is scrolled in the terminal.

## Return values
1. @{string}: The event name.
2. @{number}: The direction of the scroll. (-1 = up, 1 = down)
3. @{number}: The X-coordinate of the mouse when scrolling.
4. @{number}: The Y-coordinate of the mouse when scrolling.

## Example
Prints the direction of each scroll, and the position of the mouse at the time.

```lua
while true do
  local event, dir, x, y = os.pullEvent("mouse_scroll")
  print(("The mouse was scrolled in direction %s at %d, %d"):format(dir, x, y))
end
```
