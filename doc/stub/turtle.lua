-- SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- Craft a recipe based on the turtle's inventory.
The turtle's inventory should set up like a crafting grid. For instance, to
craft sticks, slots 1 and 5 should contain planks. _All_ other slots should be
empty, including those outside the crafting "grid".

`turtle.craft(0)` can be used to check whether the turtle contains a valid
recipe, without actually crafting it.

@tparam[opt=64] number limit The maximum number of crafting steps to run.
@throws When limit is less than 0 or greater than 64.
@treturn[1] true If crafting succeeds.
@treturn[2] false If crafting fails.
@treturn string A string describing why crafting failed.
@since 1.4
]]
function craft(limit) end
