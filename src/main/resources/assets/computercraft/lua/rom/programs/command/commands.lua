local translate = require("cc.translate").translate

if not commands then
    printError(translate("cc.commands.requires_command_computer"))
    return
end

local tCommands = commands.list()
table.sort(tCommands)

if term.isColor() then
    term.setTextColor(colors.green)
end
print(translate("cc.commands.available_commands"))
term.setTextColor(colors.white)

textutils.pagedTabulate(tCommands)
