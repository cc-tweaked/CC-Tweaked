local translate = require("cc.translate").translate

if not shell.openTab then
    printError(translate("cc.bg.requires_multishell"))
    return
end

local tArgs = { ... }
if #tArgs > 0 then
    shell.openTab(table.unpack(tArgs))
else
    shell.openTab("shell")
end
