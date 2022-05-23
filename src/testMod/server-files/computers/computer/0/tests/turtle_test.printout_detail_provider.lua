local details = turtle.getItemDetail(1, true)

test.assert(details, "Has details")
test.assert(details.printout, "Has printout meta")

test.eq("PAGE", details.printout.type)
test.eq("Example page", details.printout.title)
test.eq(1, details.printout.pages)
test.eq("Example                  ", details.printout.lines[1])
