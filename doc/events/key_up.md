---
module: [kind=event] key_up
see: keys For a lookup table of the given keys.
---

Fired whenever a key is released (or the terminal is closed while a key was being pressed).

This event returns a numerical "key code" (for instance, <kbd>F1</kbd> is 290). This value may vary between versions and
so it is recommended to use the constants in the @{keys} API rather than hard coding numeric values.

## Return values
1. @{string}: The event name.
2. @{number}: The numerical key value of the key pressed.

## Example
Prints each key released on the keyboard whenever a @{key_up} event is fired.

```lua
while true do
  local event, key = os.pullEvent("key_up")
  local name = keys.getName(key) or "unknown key"
  print(name .. " was released.")
end
```
