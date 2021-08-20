test.assert(turtle.dig())

local has_block, block = turtle.inspect()
test.assert(has_block, "Has block")
test.eq("minecraft:farmland", block.name)
