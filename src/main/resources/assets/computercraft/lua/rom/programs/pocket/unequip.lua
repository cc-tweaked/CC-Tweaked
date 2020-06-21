local translate = require("cc.translate").translate

if not pocket then
    printError(translate("cc.pocket_unequip.requires_pocket"))
    return
end

local ok, err = pocket.unequipBack()
if not ok then
    printError(err)
else
    print(translate("cc.pocket_unequip.item_unequipped"))
end
