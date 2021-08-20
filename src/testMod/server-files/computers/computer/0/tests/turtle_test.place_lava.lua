test.assert(turtle.placeDown())

local ok, down = turtle.inspectDown()
test.assert(ok, "Has below")
test.eq("minecraft:lava", down.name, "Is lava")
