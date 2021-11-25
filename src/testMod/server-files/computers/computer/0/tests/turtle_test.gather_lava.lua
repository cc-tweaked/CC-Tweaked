turtle.placeDown()

local item = turtle.getItemDetail()
test.eq("minecraft:lava_bucket", item.name)

local has_down, down = turtle.inspectDown()
test.eq(false, has_down, "Air below")
