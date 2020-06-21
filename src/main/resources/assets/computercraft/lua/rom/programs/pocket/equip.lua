local translate = require("cc.translate").translate

if not pocket then
    printError(translate("cc.pocket_equip.requires_pocket"))
    return
end

local ok, err = pocket.equipBack()
if not ok then
    printError(err)
else
    print(translate("cc.pocket_equip.item_equipped"))
end
