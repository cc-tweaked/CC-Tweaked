test.assert(turtle.place())

local has_block, block = turtle.inspect()
test.eq(true, has_block, "Has block")
test.eq("minecraft:oak_fence", block.name)
test.eq(true, block.state.waterlogged)
