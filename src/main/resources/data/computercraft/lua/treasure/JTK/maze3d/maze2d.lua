--[[
	Project info:
	
	Name: Maze
	Creator: Jesusthekiller
	Language: Lua (CC)
	Website: None
	License: GNU GPL
		License file can be fount at www.jesusthekiller.com/license-gpl.html

	Version: 1.2
]]--

--[[
	Changelog:
	  1.0:
	    Initial Release
	  1.1:
	    Typos D:
	  1.2:
	    New logo
	    Time fixed
]]--

--[[
	LICENSE:
	
	Maze
	Copyright (c) 2013 Jesusthekiller

	This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
]]--

-- The maze

-- The cprint
local function cwrite(msg)
	msg = tostring(msg)
	local x, y = term.getCursorPos()
	term.setCursorPos((51-#msg)/2, y)
	write(msg)
end

local function cprint(msg)
	cwrite(msg.."\n")
end

-- The splash
term.setBackgroundColor(colors.black)
term.setTextColor(colors.white)
term.clear()

term.setCursorPos(27, 8)
print("Nano maze!")

paintutils.drawImage({[1]={[1]=1,[2]=1,[3]=1,[4]=1,[5]=1,[6]=1,[7]=1,[8]=1,[9]=1,[10]=1,[11]=0,[12]=1,[13]=0,[14]=0,[15]=1,[16]=0,[17]=0,[18]=1,[19]=0,[20]=0,[21]=1,[22]=0,[23]=0,[24]=1,[25]=0,[26]=0,[27]=1,},[2]={[1]=1,[2]=0,[3]=0,[4]=0,[5]=0,[6]=1,[7]=0,[8]=0,[9]=0,[10]=1,[11]=0,[12]=1,[13]=1,[14]=0,[15]=1,[16]=0,[17]=1,[18]=0,[19]=1,[20]=0,[21]=1,[22]=1,[23]=0,[24]=1,[25]=0,[26]=1,[27]=0,[28]=1,},[3]={[1]=1,[2]=1,[3]=1,[4]=1,[5]=0,[6]=1,[7]=1,[8]=1,[9]=0,[10]=1,[11]=0,[12]=1,[13]=0,[14]=1,[15]=1,[16]=0,[17]=1,[18]=0,[19]=1,[20]=0,[21]=1,[22]=0,[23]=1,[24]=1,[25]=0,[26]=0,[27]=1,},[4]={[1]=1,[2]=0,[3]=0,[4]=0,[5]=0,[6]=0,[7]=0,[8]=1,[9]=0,[10]=1,},[5]={[1]=1,[2]=0,[3]=1,[4]=1,[5]=1,[6]=1,[7]=0,[8]=1,[9]=0,[10]=1,[11]=0,[12]=1,[13]=0,[14]=0,[15]=0,[16]=1,[17]=0,[18]=0,[19]=1,[20]=0,[21]=0,[22]=1,[23]=1,[24]=0,[25]=0,[26]=1,[27]=1,[28]=1,},[6]={[1]=1,[2]=0,[3]=0,[4]=0,[5]=1,[6]=0,[7]=0,[8]=0,[9]=0,[10]=1,[11]=0,[12]=1,[13]=1,[14]=0,[15]=1,[16]=1,[17]=0,[18]=1,[19]=0,[20]=1,[21]=0,[22]=0,[23]=1,[24]=0,[25]=0,[26]=1,[27]=1,},[7]={[1]=1,[2]=1,[3]=1,[4]=1,[5]=1,[6]=1,[7]=1,[8]=1,[9]=1,[10]=1,[11]=0,[12]=1,[13]=0,[14]=1,[15]=0,[16]=1,[17]=0,[18]=1,[19]=0,[20]=1,[21]=0,[22]=0,[23]=1,[24]=1,[25]=0,[26]=1,[27]=1,[28]=1,},}, 13, 5)

parallel.waitForAny(
	function() coroutine.yield(); os.pullEvent("key"); coroutine.yield() end,
	function() term.setBackgroundColor(colors.black); term.setTextColor(colors.white) while true do term.setCursorPos(18, 14); term.write("Press any key.."); sleep(0.5); term.clearLine(); sleep(0.5) end end
)

-- The size
local size

repeat
	term.setCursorPos(1, 14)
	term.clearLine()

	cwrite("Enter maze size (5-99):")
	size = read()
	
	size = tonumber(size)
	if not size then
		size = 0
	end
until size > 4 and size < 100

-- The generate
local function mazeGen(mx, my)
	
	--[[
		Format:
		
		maze.x.y.(1/2/3/4) = true/false
		
		1 - top
		2 - bottom
		3 - right
		4 - left
	]]--
	
	local maze = {}
	for i = 1, mx do
		maze[i] = {}
		for j = 1, my do
			maze[i][j] = {}
			for k = 1, 4 do
				maze[i][j][k] = true
			end
		end
	end

	local vis = 1
	local tot = mx * my
	local curr = {}
	curr.x = math.random(1, mx)
	curr.y = math.random(1, my)
	local stack = {}

	while vis < tot do
		local intact = {}
		local x = curr.x
		local y = curr.y
		
		if x - 1 >= 1 and maze[x-1][y][1] and maze[x-1][y][2] and maze[x-1][y][3] and maze[x-1][y][4] then -- Check for full cells
			intact[#intact+1] = {x-1, y, 1}
		end
		
		if x + 1 <= mx and maze[x+1][y][1] and maze[x+1][y][2] and maze[x+1][y][3] and maze[x+1][y][4] then
			intact[#intact+1] = {x+1, y, 2}
		end
		
		if y + 1 <= my and maze[x][y+1][1] and maze[x][y+1][2] and maze[x][y+1][3] and maze[x][y+1][4] then
			intact[#intact+1] = {x, y+1, 3}
		end
		
		if y - 1 >= 1 and maze[x][y-1][1] and maze[x][y-1][2] and maze[x][y-1][3] and maze[x][y-1][4] then
			intact[#intact+1] = {x, y-1, 4}
		end
		
		if #intact > 0 then
			local i = math.random(1, #intact) -- Choose random
			
			if intact[i][3] == 1 then -- Set intact's attached wall to false
				maze[intact[i][1]][intact[i][2]][2] = false
			elseif intact[i][3] == 2 then
				maze[intact[i][1]][intact[i][2]][1] = false
			elseif intact[i][3] == 3 then
				maze[intact[i][1]][intact[i][2]][4] = false
			elseif intact[i][3] == 4 then
				maze[intact[i][1]][intact[i][2]][3] = false
			end
			
			maze[x][y][intact[i][3]] = false -- Set attached wall to false
			
			vis = vis + 1 -- Increase vis
			
			stack[#stack+1] = intact[i] -- Add to stack
		else
			local tmp = table.remove(stack) -- Get last cell
			curr.x = tmp[1]
			curr.y = tmp[2]
		end
	end
	
	return maze
end

local m = mazeGen(size, size)

-- The game init
local posx = 2
local posy = 2

local offsetx = 51/2-2
local offsety = 19/2-2

local stime = os.clock()

-- The maze-to-table
local tab = {}

for x = 1, size * 2 + 1 do
	tab[x] = {}
	
	for y = 1, size * 2 + 1 do
		if x % 2 == 0 and y % 2 == 0 then -- Fill cells (empty)
			tab[x][y] = false
		elseif x % 2 == 1 and y % 2 == 1 then -- Fill corners (full)
			tab[x][y] = true
		end
	end
end

for x, tV in ipairs(m) do
	for y, v in ipairs(tV) do
		tab[x*2-1][y*2] = v[1] -- Up
		tab[x*2+1][y*2] = v[2] -- Down
		tab[x*2][y*2+1] = v[3] -- Right
		tab[x*2][y*2-1] = v[4] -- Left
	end
end

-- The game itself
repeat
	-- Print map
	term.setBackgroundColor(colors.white)
	term.clear()
	
	if posx == 2 and posy == 2 then
		term.setCursorPos(1, 1)
		term.setTextColor(colors.black)
		print("Controls: WASD")
		print("Back to start: R")
		print("Quit: Q")
		print("Goal: Step on # (It's on bottom right corner)")
		print("\nGood Luck!")
	end
	
	--[[
		term.setTextColor(colors.black)
		term.setCursorPos(1, 19)
		write("X: "..posx.."   Y: "..posy)
	]]
	
	for x, tV in ipairs(tab) do -- Print the map
		for y, v in ipairs(tV) do
			if offsety+y > 20 then
				break
			end
			
			term.setCursorPos(offsetx+x, offsety+y)
			
			if v then
				term.setBackgroundColor(colors.black)
			else
				term.setBackgroundColor(colors.white)
			end
			
			if offsety+y < 20 and offsety+y > 0 and offsetx+x < 52 and offsetx+x > 0 then
				if x == size*2 and y == size*2 then
					if term.isColor() then
						term.setTextColor(colors.cyan)
					end
					write("#")
				else
					write(" ")
				end
			end
		end
		
		if offsetx+x > 51 then
			break
		end
	end 
	
	term.setCursorPos(51/2, 19/2)
	term.setBackgroundColor(colors.white)
	
	if term.isColor() then
		term.setTextColor(colors.red)
	else
		term.setTextColor(colors.black)
	end
	
	write("X")
	
	-- Wait for key
	
	local e, k = os.pullEvent("char")
	
	if k == "a" and (not tab[posx-1][posy]) then
		posx = posx - 1
		offsetx = offsetx + 1
	end
	
	if k == "d" and (not tab[posx+1][posy]) then
		posx = posx + 1
		offsetx = offsetx - 1
	end
	
	if k == "w" and (not tab[posx][posy-1]) then
		posy = posy - 1
		offsety = offsety + 1
	end
	
	if k == "s" and (not tab[posx][posy+1]) then
		posy = posy + 1
		offsety = offsety - 1
	end
	
	if k == "q" then
		break
	end
	
	if k == "r" then
		posx = 2
		posy = 2

		offsetx = 51/2-2
		offsety = 19/2-2
	end
until posx == size*2 and posy == size*2

-- The win/loose message
term.setBackgroundColor(colors.white)
term.setTextColor(colors.black)
term.clear()
term.setCursorPos(1, 1)

if posx == size*2 and posy == size*2 then
	local ntime = os.clock()
	write("\n")
	cprint("Congratulations!")
	cprint("You made it in")
	cprint(tostring(math.floor((ntime-stime)/60)).." minutes and "..tostring(math.ceil((ntime-stime)%60)).." seconds")
	cprint("Size of maze: "..size)
else
	write("\n")
	cprint("Oh noes D:")
end

parallel.waitForAny(
	function() coroutine.yield(); os.pullEvent("key"); coroutine.yield() end,
	function() term.setBackgroundColor(colors.white); term.setTextColor(colors.black) while true do term.setCursorPos(18, 14); term.write("Press any key.."); sleep(0.5); term.clearLine(); sleep(0.5) end end
)

term.setBackgroundColor(colors.black)
term.setTextColor(colors.white)
term.clear()
term.setCursorPos(1, 1)
cprint("  Maze by JTK. Thanks for playing!")