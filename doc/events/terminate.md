---
module: [kind=event] terminate
---

<!--
SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`terminate`] event is fired when <kbd>Ctrl-T</kbd> is held down.

This event is normally handled by [`os.pullEvent`], and will not be returned. However, [`os.pullEventRaw`] will return this event when fired.

[`terminate`] will be sent even when a filter is provided to [`os.pullEventRaw`]. When using [`os.pullEventRaw`] with a filter, make sure to check that the event is not [`terminate`].

## Return values
1. [`string`]: The event name.

## Example
Prints a message when Ctrl-T is held:
```lua
while true do
  local event = os.pullEventRaw("terminate")
  if event == "terminate" then print("Terminate requested!") end
end
```

Exits when Ctrl-T is held:
```lua
while true do
  os.pullEvent()
end
```
