test.assert(turtle.place())

local has_block, block = turtle.inspect()
test.assert(has_block, "Has block")
test.eq("computercraft:monitor_advanced", block.name)
test.eq("lr", block.state.state)
