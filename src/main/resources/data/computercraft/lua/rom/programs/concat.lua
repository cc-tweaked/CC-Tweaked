local tArgs = { ... }

if #tArgs < 2 then
  printError("Usage: concat <file> [...] <destination>")
end

-- The data to write
local sToWrite = ""
-- The file to which to write it
local sDestination = tArgs[#tArgs]
tArgs[#tArgs] = nil

for _, sItem in ipairs(tArgs) do
  local handle, sError = fs.open(shell.resolve(sItem), "rb")
  if not handle then
    printError(sItem .. ": " .. sError)
    return
  end
  sToWrite = sToWrite .. (handle.readAll() or "")
  handle.close()
end

local outHandle, sError = fs.open(shell.resolve(sDestination), "wb")
if not outHandle then
  printError(sDestination .. ": " .. sError)
  return
end

outHandle.write(sToWrite)
outHandle.close()
