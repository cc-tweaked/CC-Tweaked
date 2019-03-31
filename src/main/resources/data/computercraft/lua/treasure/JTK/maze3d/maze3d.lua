--[[
	Project info:
	
	Name: Maze 3D
	Creator: Jesusthekiller
	Language: Lua (CC)
	Website: None
	License: GNU GPL
		License file can be fount at www.jesusthekiller.com/license-gpl.html

	Version: 2.1
]]--

--[[
	Big thanks to Gopher for 3D engine!
	http://www.computercraft.info/forums2/index.php?/topic/10786-wolf3d-style-3d-engine-proof-of-concept/page__hl__wolf3d
]]--

--[[
	Changelog:
	  1.0:
	    Initial Release
	  2.0:
	    No-HTTP version for Treasure disk
	  2.1:
	    No more temp files!
]]--

--[[
	LICENSE:
	
	Maze 3D
	Copyright (c) 2013 Jesusthekiller

	This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

	This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
]]--

-- The color check
if (not term.isColor()) or turtle then
	print("This program has to be run on advanced computer.")
	error()
end

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

paintutils.drawImage({[1]={[1]=1,[2]=0,[3]=0,[4]=0,[5]=1,[6]=0,[7]=0,[8]=1,[9]=1,[10]=0,[11]=0,[12]=0,[13]=1,[14]=1,[15]=1,[16]=1,[17]=0,[18]=1,[19]=1,[20]=1,[21]=1,},[2]={[1]=1,[2]=1,[3]=0,[4]=1,[5]=1,[6]=0,[7]=1,[8]=0,[9]=0,[10]=1,[11]=0,[12]=0,[13]=0,[14]=0,[15]=0,[16]=1,[17]=0,[18]=1,[19]=0,[20]=0,[21]=0,},[3]={[1]=1,[2]=0,[3]=1,[4]=0,[5]=1,[6]=0,[7]=1,[8]=1,[9]=1,[10]=1,[11]=0,[12]=0,[13]=0,[14]=1,[15]=1,[16]=0,[17]=0,[18]=1,[19]=1,[20]=1,[21]=0,},[4]={[1]=1,[2]=0,[3]=0,[4]=0,[5]=1,[6]=0,[7]=1,[8]=0,[9]=0,[10]=1,[11]=0,[12]=0,[13]=1,[14]=0,[15]=0,[16]=0,[17]=0,[18]=1,[19]=0,[20]=0,[21]=0,},[5]={[1]=1,[2]=0,[3]=0,[4]=0,[5]=1,[6]=0,[7]=1,[8]=0,[9]=0,[10]=1,[11]=0,[12]=0,[13]=1,[14]=1,[15]=1,[16]=1,[17]=0,[18]=1,[19]=1,[20]=1,[21]=1,},[6]={[1]=0,[2]=0,[3]=0,[4]=0,[5]=0,[6]=0,[7]=0,[8]=0,},[7]={[1]=0,[2]=0,[3]=0,[4]=16384,[5]=16384,[6]=16384,[7]=16384,[8]=0,[9]=0,[10]=0,[11]=0,[12]=512,[13]=512,[14]=512,[15]=512,[16]=0,[17]=0,[18]=0,[19]=0,[20]=0,[21]=0,},[8]={[1]=0,[2]=0,[3]=0,[4]=0,[5]=128,[6]=128,[7]=128,[8]=16384,[9]=0,[10]=0,[11]=0,[12]=512,[13]=128,[14]=128,[15]=128,[16]=512,[17]=0,[18]=0,[19]=0,[20]=0,[21]=0,},[9]={[1]=0,[2]=0,[3]=0,[4]=16384,[5]=16384,[6]=16384,[7]=16384,[8]=0,[9]=128,[10]=0,[11]=0,[12]=512,[13]=128,[14]=0,[15]=0,[16]=512,[17]=128,[18]=0,[19]=0,[20]=0,[21]=0,},[10]={[1]=0,[2]=0,[3]=0,[4]=0,[5]=128,[6]=128,[7]=128,[8]=16384,[9]=0,[10]=0,[11]=0,[12]=512,[13]=128,[14]=0,[15]=0,[16]=512,[17]=128,[18]=0,[19]=0,[20]=0,[21]=0,},[11]={[1]=0,[2]=0,[3]=0,[4]=16384,[5]=16384,[6]=16384,[7]=16384,[8]=0,[9]=128,[10]=0,[11]=0,[12]=512,[13]=512,[14]=512,[15]=512,[16]=128,[17]=128,[18]=0,[19]=0,[20]=0,[21]=0,},[12]={[1]=0,[2]=0,[3]=0,[4]=0,[5]=128,[6]=128,[7]=128,[8]=128,[9]=0,[10]=0,[11]=0,[12]=0,[13]=128,[14]=128,[15]=128,[16]=128,},}, 15, 3)

parallel.waitForAny(
	function() coroutine.yield(); os.pullEvent("key"); coroutine.yield() end,
	function() term.setBackgroundColor(colors.black); term.setTextColor(colors.white) while true do term.setCursorPos(18, 16); term.write("Press any key.."); sleep(0.5); term.clearLine(); sleep(0.5) end end
)

-- The size
local size

repeat
	term.setCursorPos(1, 16)
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

-- The maze-to-table
local tab = {}

for x = 1, size * 2 + 1 do
	tab[x] = {}
	
	for y = 1, size * 2 + 1 do
		if x % 2 == 0 and y % 2 == 0 then -- Fill cells (empty)
			tab[x][y] = " "
		elseif x % 2 == 1 and y % 2 == 1 then -- Fill corners (full)
			tab[x][y] = "1"
		end
	end
end

for x, tV in ipairs(m) do
	for y, v in ipairs(tV) do
		if x == size and y == size then
			v[1] = v[1] and "2" or " "
			v[2] = v[2] and "2" or " "
			v[3] = v[3] and "2" or " "
			v[4] = v[4] and "2" or " "
			tab[x*2-1][y*2] = v[1] -- Up
			tab[x*2+1][y*2] = v[2] -- Down
			tab[x*2][y*2+1] = v[3] -- Right
			tab[x*2][y*2-1] = v[4] -- Left
		else
			v[1] = v[1] and "1" or " "
			v[2] = v[2] and "1" or " "
			v[3] = v[3] and "1" or " "
			v[4] = v[4] and "1" or " "
			tab[x*2-1][y*2] = v[1] -- Up
			tab[x*2+1][y*2] = v[2] -- Down
			tab[x*2][y*2+1] = v[3] -- Right
			tab[x*2][y*2-1] = v[4] -- Left
		end
	end
end

local gtab = {}

for k, v in ipairs(tab) do
	gtab[#gtab+1] = table.concat(v)
end

size = size * 2 + 1

--[[
local template = fs.open("maze3d_template", "r")
local game = fs.open("maze3d_game", "w")

game.writeLine("local mapH, mapW = "..size..","..size)
game.writeLine("local dir = "..(gtab[2]:sub(3,3) == " " and '0' or '88'))
game.writeLine("local map = {")

for k, v in ipairs(gtab) do
	game.writeLine('"'..v..'",')
end

game.writeLine("}")

game.writeLine(template.readAll())
game.close()
template.close()

shell.run("maze3d_game")

fs.delete("maze3d_game")
fs.delete("maze3d_template")]]

local mapH, mapW = size, size
local dir = gtab[2]:sub(3,3) == " " and '0' or '88'
local map = gtab
local startdir = dir

------------------------------------------------------------------------------------------------------
--GOPHER'S CODE HERE

local buffer=term
local loadedAPI=false

local stime = os.clock()

if redirect then
  buffer=redirect.createRedirectBuffer()
  print("redirect API found, using buffer")
else
  local pe=printError
  rawset(_G,"printError",error)  
  local ok, err=pcall(os.loadAPI,"redirect")
  if not ok then
    print("trying "..shell.dir().."/redirect")
    ok,err=pcall(os.loadAPI,shell.dir().."/redirect")
  end
  if ok then    
    print("Loaded redirect API, using buffer")
    buffer=redirect.createRedirectBuffer()
    loadedAPI=true
  else
    print("redirect API not found or could not be loaded, drawing directly; this may cause flickering.")
  end
  rawset(_G,"printError",pe)
end 

local colorSchemes = {
  {0,8}, --white+gray
  {3,11}, --blue
  {6,14}, --red
  {5,13}, --green
  {4,1}, --yellow/orange
}


local function cast(cx,cy,angle)
  --direction vector
  local vx,vy=math.cos(angle), math.sin(angle)
  local slope=vy/vx
  --next distance, x and y axis points
  local ndx, ndy
  --steps, distance and block
  local dsx, dsy, bsx, bsy
  if vx<0 then
    local x=(cx%1)
    bsx=-1
    ndx=math.sqrt(x*x*(1+slope*slope))
    dsx=math.sqrt((1+slope*slope))
  else
    local x=1-(cx%1)
    bsx=1
    ndx=math.sqrt(x*x*(1+slope*slope))
    dsx=math.sqrt((1+slope*slope))
  end

  if vy<0 then
    local y=(cy%1)
    bsy=-1
    ndy=math.sqrt(y*y*(1+1/(slope*slope)))
    dsy=math.sqrt((1+1/(slope*slope)))
  else
    local y=1-(cy%1)
    bsy=1
    ndy=math.sqrt(y*y*(1+1/(slope*slope)))
    dsy=math.sqrt((1+1/(slope*slope)))
  end

  local x,y=math.floor(cx),math.floor(cy)
  while x>0 and x<=mapW and y>0 and y<=mapH do
    local hitD
    local isX
    if ndx<ndy then
      --x crossing is next
      x=x+bsx
      isX=true
      hitD=ndx
      ndx=ndx+dsx
    else
      y=y+bsy
      isX=false
      hitD=ndy
      ndy=ndy+dsy
    end
    local wall=map[y]:sub(x,x)
    if wall~=" " then
      
      return colorSchemes[tonumber(wall)][isX and 1 or 2], hitD
    end
  end  
end

local w,h=term.getSize()
local centerX, centerY=math.floor((w+1)/2), math.floor((h+1)/2)

local px, py=2.5,2.5
--local dir=0
local fx,fy
local speed=.1
local turnSpeed=4

local function turn(amt)
  dir=dir+amt
  fx,fy=math.cos(math.rad(dir)), math.sin(math.rad(dir))
end

turn(0)

--build table of angles and base distances per scanline
local screenDist=.55*w
local scan={}

for x=1,w do
  local t={}
  scan[x]=t
  t.angle=math.atan2(x-centerX,screenDist)
  t.dist=((x-centerX)^2+screenDist^2)^.5/screenDist
end
  
local function redraw()
  local oldTerm
  if buffer.isBuffer then
    oldTerm = term.redirect(buffer)
  end
  for x=1,w do
    local wall,dist=cast(px,py,math.rad(dir)+scan[x].angle)
    if wall then
      --calc wall height based on distance
      local height=scan[x].dist/dist
      height=math.floor(math.min(height*centerY,(h+1)/2))
      term.setBackgroundColor(colors.gray)
      for y=1,(h+1)/2-height-1 do
        term.setCursorPos(x,y)
        term.write(" ")
      end
      for y=centerY+height+1,h do
        term.setCursorPos(x,y)
        term.write(" ")
      end
      term.setBackgroundColor(2^wall)
      for y=centerY-height,centerY+height do
        term.setCursorPos(x,y)
        term.write(" ")
      end
    end
  end
  if buffer.isBuffer then
    term.redirect(oldTerm)
    buffer.blit()
  end
end

local function clampCollision(x,y,radius)
  --am I *in* a block?
  local gx,gy=math.floor(x),math.floor(y)
  if map[gy]:sub(gx,gx)~=" " then
    --I am. Complete fail, do nothing.
    return x,y
  end
  
  --ok, check the neighbors.
  local right=math.floor(x+radius)>gx
  local left=math.floor(x-radius)<gx
  local front=math.floor(y-radius)<gy
  local back=math.floor(y+radius)>gy
  
  local pushed=false
  
  if right and map[gy]:sub(gx+1,gx+1)~=" " then
    --push left
    pushed=true
    x=gx+1-radius
  elseif left  and map[gy]:sub(gx-1,gx-1)~=" " then
    --push right
    pushed=true
    x=gx+radius
  end
  
  if front and map[gy-1]:sub(gx,gx)~=" " then
    --push back
    pushed=true
    y=gy+radius
  elseif back and map[gy+1]:sub(gx,gx)~=" " then
    --push forward
    pushed=true



    y=gy+1-radius
  end
 
  --if I wasn't pushed out on any side, I might be hitting a corner
  if not pushed then
    --square rad
    local r2=radius^2
    local pushx,pushy=0,0
    if left then
      if front and map[gy-1]:sub(gx-1,gx-1)~=" " then
        --check front-left
        local dist2=(gx-x)^2+(gy-y)^2
        if dist2<r2 then
          local pushd=(r2-dist2)/2^.5
          pushx,pushy=pushd,pushd
        end
      elseif back and map[gy+1]:sub(gx-1,gx-1)~=" " then
        local dist2=(gx-x)^2+(gy+1-y)^2
        if dist2<r2 then
          local pushd=(r2-dist2)/2^.5
          pushx,pushy=pushd,-pushd
        end
      end
    elseif right then
      if front and map[gy-1]:sub(gx+1,gx+1)~=" " then
        --check front-left
        local dist2=(gx+1-x)^2+(gy-y)^2
        if dist2<r2 then
          local pushd=(r2-dist2)/2^.5
          pushx,pushy=-pushd,pushd
        end
      elseif back and map[gy+1]:sub(gx+1,gx+1)~=" " then
        local dist2=(gx+1-x)^2+(gy+1-y)^2
        if dist2<r2 then
          local pushd=(r2-dist2)/2^.5
          pushx,pushy=-pushd,-pushd
        end
      end
    end
    x=x+pushx
    y=y+pushy
  end
  
  return x,y
end

term.setBackgroundColor(colors.black)
--term.setTextColor(colors.white)
term.clear()
term.setCursorPos(1, 1)

term.setTextColor(colors.yellow)
write("Move:                                          ")
term.setTextColor(colors.lime)
print("WASD")

term.setTextColor(colors.yellow)
write("Turn:                              ")
term.setTextColor(colors.lime)
print("Left/Right arrow")

term.setTextColor(colors.yellow)
write("Teleport to start:                                ")
term.setTextColor(colors.lime)
print("R")

term.setTextColor(colors.yellow)
write("Quit:                                             ")
term.setTextColor(colors.lime)
print("Q\n")

term.setTextColor(colors.white)
write("Goal: go to ")
term.setTextColor(colors.lightBlue)
write("blue")
term.setTextColor(colors.white)
print(" spot (opposite corner of the map)\n\n\n\n")

term.setTextColor(colors.white)
cprint("Press any key to start!")

os.pullEvent("key")

local frameTimer=os.startTimer(0.5)
local prevTick=0
local dirty=true
local win = false
while true do
  px,py=clampCollision(px,py,.25)
  if dirty then
    redraw()
    dirty=false
  end
  
  local e={os.pullEvent()}
  if e[1]=="key" then
    if e[2]==keys.left then
      turn(-turnSpeed)
      dirty=true
    elseif e[2]==keys.right then
      turn(turnSpeed)
      dirty=true
    elseif e[2]==keys.up or e[2]==keys.w then
      px=px+fx*speed
      py=py+fy*speed
      dirty=true
    elseif e[2]==keys.down or e[2]==keys.s then
      px=px-fx*speed
      py=py-fy*speed
      dirty=true
    elseif e[2]==keys.a then
      px=px+fy*speed
      py=py-fx*speed
      dirty=true
    elseif e[2]==keys.d then
      px=px-fy*speed
      py=py+fx*speed
      dirty=true
    elseif e[2]==keys.q then
      break
    elseif e[2]==keys.r then
      px,py = 2.5,2.5
      dir=startdir
      dirty=true
    end
    
    if px >= mapW-1 and py >= mapH-1 then
      win = true
      break
    end
  end
end


if loadedAPI then
  os.unloadAPI("redirect")
end

-- JESUS PART

-- The win/loose message
term.setBackgroundColor(colors.white)
term.setTextColor(colors.black)
term.clear()
term.setCursorPos(1, 1)

if win then
  local ntime = os.clock()
  write("\n")
  cprint("Congratulations!")
  cprint("You made it in")
  cprint(tostring(math.floor((ntime-stime)/60)).." minutes and "..tostring(math.ceil((ntime-stime)%60)).." seconds")
  cprint("Size of maze: "..(mapW-1)/2)
sleep(1)
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
cprint("  Maze 3D by JTK. Thanks for playing!")
cprint("3D engine by Gopher, He is A-W-E-S-O-M-E")
