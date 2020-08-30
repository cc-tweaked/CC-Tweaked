local tArgs = { ... }
local sTopic
if #tArgs > 0 then
    sTopic = tArgs[1]
else
    sTopic = "intro"
end

if sTopic == "index" then
    print("Help topics available:")
    local tTopics = help.topics()
    textutils.pagedTabulate(tTopics)
    return
end

local sFile = help.lookup(sTopic)
local file = sFile ~= nil and io.open(sFile) or nil
if file then
    local sContents = file:read("*a")
    file:close()

    local _, nHeight = term.getSize()
    textutils.pagedPrint(sContents, nHeight - 3)
else
    print("No help available")
end
