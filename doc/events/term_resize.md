---
module: [kind=event] term_resize
---

The @{term_resize} event is fired when the main terminal is resized, mainly when a new tab is opened or closed in @{multishell}.

## Example
Prints :
```lua
while true do
  os.pullEvent("term_resize")
  local w, h = term.getSize()
  print("The term was resized to (" .. w .. ", " .. h .. ")")
end
```
