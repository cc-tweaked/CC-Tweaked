board = {}
tArgs = { ... }
generation = 0
sleeptime = 0.5

if(tArgs[1] == "left" or tArgs[1] == "right" or tArgs[1] == "top" or tArgs[1] == "bottom" or tArgs[1] == "front" or tArgs[1] == "back")then
	mon = peripheral.wrap(tArgs[1])
else
	mon = term
end

if(mon.isColor() or mon.isColor)then
	colored = true
else
	colored = false
end

w, h = mon.getSize()
for x = 1, w do
	board[x] = {}
	for y = 1, h do
		board[x][y] = 0
	end
end

function drawScreen()
	w, h = mon.getSize()
	for x = 1, w do
		for y = 1, h do
			nei = getNeighbours(x, y)
			if(board[x][y] == 1)then
				if colored then
					if(nei < 2 or nei > 3)then
						mon.setBackgroundColor(colors.red)
					else
						mon.setBackgroundColor(colors.green)
					end
				else
					mon.setBackgroundColor(colors.white)
				end
			else
				if colored then
					if(nei == 3)then
						mon.setBackgroundColor(colors.yellow)
					else
						mon.setBackgroundColor(colors.black)
					end
				else
					mon.setBackgroundColor(colors.black)
				end
			end
			mon.setCursorPos(x, y)
			mon.write(" ")
		end
	end
	mon.setCursorPos(1,1)
	if colored then
		mon.setTextColor(colors.blue)
	end
	mon.write(generation)
end

function getNeighbours(x, y)
	w, h = mon.getSize()
	total = 0
	if(x > 1 and y > 1)then if(board[x-1][y-1] == 1)then total = total + 1 end end
	if(y > 1)then if(board[x][y-1] == 1)then total = total + 1 end end
	if(x < w and y > 1)then if(board[x+1][y-1] == 1)then total = total + 1 end end
	if(x > 1)then if(board[x-1][y] == 1)then total = total + 1 end end
	if(x < w)then if(board[x+1][y] == 1)then total = total + 1 end end
	if(x > 1 and y < h)then if(board[x-1][y+1] == 1)then total = total + 1 end end
	if(y < h)then if(board[x][y+1] == 1)then total = total + 1 end end
	if(x < w and y < h)then if(board[x+1][y+1] == 1)then total = total + 1 end end
	return total
end

function compute()
	w, h = mon.getSize()
	while true do
		newBoard = {}
		for x = 1, w do
			newBoard[x] = {}
			for y = 1, h do
				nei = getNeighbours(x, y)
				if(board[x][y] == 1)then
					if(nei < 2)then
						newBoard[x][y] = 0
					elseif(nei > 3)then
						newBoard[x][y] = 0
					else
						newBoard[x][y] = 1
					end
				else
					if(nei == 3)then
						newBoard[x][y] = 1
					end
				end
			end
		end
		board = newBoard
		generation = generation + 1
		sleep(sleeptime)
	end
end

function loop()
	while true do
		event, variable, xPos, yPos = os.pullEvent()
		if event == "mouse_click" or event == "monitor_touch" or event == "mouse_drag" then
			if variable == 1 then
				board[xPos][yPos] = 1
			else
				board[xPos][yPos] = 0
			end
		end
		if event == "key" then
			if tostring(variable) == "28" then
				return true
			elseif tostring(variable) == "57" then
				if(mon.isColor() or mon.isColor)then
					colored = not colored
				end
			elseif tostring(variable) == "200" then
				if sleeptime > 0.1 then
					sleeptime = sleeptime - 0.1
				end
			elseif tostring(variable) == "208" then
				if sleeptime < 1 then
					sleeptime = sleeptime + 0.1
				end
			end
		end
		drawScreen()
	end
end

function intro()
	mon.setBackgroundColor(colors.black)
	mon.clear()
	mon.setCursorPos(1,1)
	mon.write("Conway's Game Of Life")
	mon.setCursorPos(1,2)
	mon.write("It is a game which represents life.")
	mon.setCursorPos(1,3)
	mon.write("The game runs by 4 basic rules:")
	mon.setCursorPos(1,4)
	mon.write("1. If a cell has less than 2 neighbours, it dies.")
	mon.setCursorPos(1,5)
	mon.write("2. If a cell has 2 or 3 neightbours, it lives.")
	mon.setCursorPos(1,6)
	mon.write("3. If a cell has more than 3 neighbours, it dies.")
	mon.setCursorPos(1,7)
	mon.write("4. If a cell has exactly 3 neighbours it is born.")
	mon.setCursorPos(1,9)
	mon.write("At the top left is the generation count.")
	mon.setCursorPos(1,10)
	mon.write("Press spacebar to switch between color modes")
	mon.setCursorPos(1,11)
	mon.write("Press enter to start  the game")
	mon.setCursorPos(1,13)
	mon.write("Colors:")
	mon.setCursorPos(1,14)
	mon.write("Red - Cell will die in next generation")
	mon.setCursorPos(1,15)
	mon.write("Green - Cell will live in next generation")
	mon.setCursorPos(1,16)
	mon.write("Yellow - Cell will be born in next generation")
	mon.setCursorPos(1,18)
	mon.write("Press any key to continue!")
	event, variable, xPos, yPos = os.pullEvent("key")
end

intro()
drawScreen()
while true do
	loop()
	parallel.waitForAny(loop, compute)
end