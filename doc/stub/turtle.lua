--[[- Craft a recipe based on the turtle's inventory.

The turtle's inventory should set up like a crafting grid. For instance, to
craft sticks, slots 1 and 5 should contain planks. _All_ other slots should be
empty, including those outside the crafting "grid".

@tparam[opt=64] number limit The maximum number of crafting steps to run.
@throws When limit is less than 1 or greater than 64.
@treturn[1] true If crafting succeeds.
@treturn[2] false If crafting fails.
@treturn string A string describing why crafting failed.
@since 1.4
]]
function craft(limit) end
