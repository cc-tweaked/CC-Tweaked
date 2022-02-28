---
module: [kind=event] mouse_click
---

This event is fired when the terminal is clicked with a mouse. This event is only fired on advanced computers (including
advanced turtles and pocket computers).

## Return values
1. @{string}: The event name.
2. @{number}: The mouse button that was clicked.
3. @{number}: The X-coordinate of the click.
4. @{number}: The Y-coordinate of the click.

## Mouse buttons
Several mouse events (@{mouse_click}, @{mouse_up}, @{mouse_scroll}) contain a "mouse button" code. This takes a
numerical value depending on which button on your mouse was last pressed when this event occurred.

<table class="pretty-table">
    <!-- Our markdown parser doesn't work on tables!? Guess I'll have to roll my own soonish :/. -->
    <tr><th>Button code</th><th>Mouse button</th></tr>
    <tr><td align="right">1</td><td>Left button</td></tr>
    <tr><td align="right">2</td><td>Right button</td></tr>
    <tr><td align="right">3</td><td>Middle button</td></tr>
</table>

## Example
Print the button and the coordinates whenever the mouse is clicked.

```lua
while true do
  local event, button, x, y = os.pullEvent("mouse_click")
  print(("The mouse button %s was pressed at %d, %d"):format(button, x, y))
end
```
