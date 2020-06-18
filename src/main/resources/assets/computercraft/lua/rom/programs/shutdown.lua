local translate = require("cc.translate").translate

if term.isColour() then
    term.setTextColour(colours.yellow)
end
print(translate("cc.shutdown.message"))
term.setTextColour(colours.white)

sleep(1)
os.shutdown()
