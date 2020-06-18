local translate = require("cc.translate").translate

if term.isColour() then
    term.setTextColour(colours.yellow)
end
print(translate("cc.reboot.message"))
term.setTextColour(colours.white)

sleep(1)
os.reboot()
