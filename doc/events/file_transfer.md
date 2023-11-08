---
module: [kind=event] file_transfer
since: 1.101.0
---

<!--
SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

The [`file_transfer`] event is queued when a user drags-and-drops a file on an open computer.

This event contains a single argument of type [`TransferredFiles`], which can be used to [get the files to be
transferred][`TransferredFiles.getFiles`]. Each file returned is a [binary file handle][`fs.ReadHandle`] with an
additional [getName][`TransferredFile.getName`] method.

## Return values
1. [`string`]: The event name
2. [`TransferredFiles`]: The list of transferred files.

## Example
Waits for a user to drop files on top of the computer, then prints the list of files and the size of each file.

```lua
local _, files = os.pullEvent("file_transfer")
for _, file in ipairs(files.getFiles()) do
  -- Seek to the end of the file to get its size, then go back to the beginning.
  local size = file.seek("end")
  file.seek("set", 0)

  print(file.getName() .. " " .. size)
end
```

## Example
Save each transferred file to the computer's storage.

```lua
local _, files = os.pullEvent("file_transfer")
for _, file in ipairs(files.getFiles()) do
  local handle = fs.open(file.getName(), "wb")
  handle.write(file.readAll())

  handle.close()
  file.close()
end
```
