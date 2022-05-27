test.ok("initial")

local details = turtle.getItemDetail(1, true)

test.assert(details, "Has details")
test.assert(details.printout, "Has printout meta")
test.eq("PAGE", details.printout.type)
