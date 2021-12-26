---
module: [kind=event] term_resize
---

The @{term_resize} event is fired when the main terminal is resized, mainly when a new tab is opened or closed in @{multishell} or when the terminal out is being redirected by the "monitor" program and monitor size changes.

This event indicates that there is no guarantee of what is on the screen and where it is located. GUI based software should redraw its contents but simple terminal programs can safely ignore it.

## Example
Prints :
```lua
while true do
  os.pullEvent("term_resize")
  local w, h = term.getSize()
  print("The term was resized to (" .. w .. ", " .. h .. ")")
end
```
