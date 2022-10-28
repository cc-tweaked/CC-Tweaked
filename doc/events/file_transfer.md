---
module: [kind=event] file_transfer
---

The @{file_transfer} event is queued when a user drags-and-drops a file on an open computer.

This event contains a single argument, that in turn has a single method @{TransferredFiles.getFiles|getFiles}. This
returns the list of files that are being transferred. Each file is a @{fs.BinaryReadHandle|binary file handle} with an
additional @{TransferredFile.getName|getName} method.

## Return values
1. @{string}: The event name
2. @{TransferredFiles}: The list of transferred files.

## Example
Waits for a user to drop files on top of the computer, then prints the list of files and the size of each file.

```lua
local _, files = os.pullEvent("file_transfer")
for _, file in ipairs(files.getFiles()) do
  -- Seek to the end of the file to get its size, then go back to the beginning.
  local size = file.seek("end")
  file.seek("set", 0)

  print(file.getName() .. " " .. file.getSize())
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
