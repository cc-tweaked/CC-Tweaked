test.eq("modem", peripheral.getType("right"), "Starts with a modem")
turtle.equipRight()
test.eq("drive", peripheral.getType("right"), "Unequipping gives a drive")
