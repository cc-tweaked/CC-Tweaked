---
module: [kind=event] char
see: key To listen to any key press.
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: LicenseRef-CCPL
-->

The @{char} event is fired when a character is typed on the keyboard.

The @{char} event is different to a key press. Sometimes multiple key presses may result in one character being
typed (for instance, on some European keyboards). Similarly, some keys (e.g. <kbd>Ctrl</kbd>) do not have any
corresponding character. The @{key} should be used if you want to listen to key presses themselves.

## Return values
1. @{string}: The event name.
2. @{string}: The string representing the character that was pressed.


## Example
Prints each character the user presses:

```lua
while true do
    local event, character = os.pullEvent("char")
    print(character .. " was pressed.")
end
```
