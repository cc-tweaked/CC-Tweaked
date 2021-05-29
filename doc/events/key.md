---
module: [kind=event] key
---

This event is fired when any key is pressed while the terminal is focused.

This event returns a numerical "key code" (for instance, <kbd>F1</kbd> is 290). This value may vary between versions and
so it is recommended to use the constants in the @{keys} API rather than hard coding numeric values.

If the button pressed represented a printable character, then the @{key} event will be followed immediately by a @{char}
event. If you are consuming text input, use a @{char} event instead!

## Return values
1. @{string}: The event name.
2. @{number}: The numerical key value of the key pressed.
3. @{boolean}: Whether the key event was generated while holding the key (@{true}), rather than pressing it the first time (@{false}).

## Example
Prints each key when the user presses it, and if the key is being held.

```lua
while true do
  local event, key, is_held = os.pullEvent("key")
  print(("%s held=%s"):format(keys.getName(key), is_held))
end
```
