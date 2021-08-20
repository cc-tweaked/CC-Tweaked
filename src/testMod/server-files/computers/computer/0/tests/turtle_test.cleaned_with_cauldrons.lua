local old_details = turtle.getItemDetail(1, true)

test.assert(turtle.place(), "Dyed turtle")

local new_details = turtle.getItemDetail(1, true)
test.eq("computercraft:turtle_normal", new_details.name, "Still a turtle")
test.neq(old_details.nbt, new_details.nbt, "Colour has changed")
