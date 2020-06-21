local translate = require("cc.translate").translate

if term.isColour() then
    term.setTextColour(2 ^ math.random(0, 15))
end
textutils.slowPrint(translate("cc.hello.text"))
term.setTextColour(colours.white)
