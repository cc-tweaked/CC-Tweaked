-- TurtleTest.`Shears sheep`

turtle.placeDown()

local item = turtle.getItemDetail(2)
if item == nil then test.fail("Got no item") end
test.eq("minecraft:white_wool", item.name)

test.ok()
