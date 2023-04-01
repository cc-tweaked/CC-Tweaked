-- SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

require "cc.completion"

print("Drop files to transfer them to this computer")

local files
while true do
    local event, arg = os.pullEvent()
    if event == "file_transfer" then
        files = arg.getFiles()
        break
    elseif event == "key" and arg == keys.q then
        return
    end
end

if #files == 0 then
  printError("No files to transfer")
  return
end

local ok, err = require("cc.internal.import")(files)
if not ok and err then printError(err) end
