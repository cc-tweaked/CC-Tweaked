---
module: [kind=event] setting_changed
see: settings
---

<!--
SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`setting_changed`] event is fired when a setting is modified with the [`settings`] API.

## Return Values
1. [`string`]: The event name.
2. [`string`]: The name of the setting that was changed.
3. [`any`]: The value the setting was set to.
4. [`any`]: The previous value of the setting.

## Example
[Update a setting][`settings.set`], and then wait for the corresponding
`setting_changed` event.

```lua
settings.set("my.setting", 123)
print(os.pullEvent("setting_changed"))
```
