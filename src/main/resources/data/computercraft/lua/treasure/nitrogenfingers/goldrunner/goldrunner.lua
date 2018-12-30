--[[
		Gold Runner
		Inspired by the game by Doug Smith

		Written by: Nitrogen Fingers
]]--

w,h = term.getSize()
if not term.isColour() then
	printError("Requires an Advanced Computer")
	return
end


running = true
started = false
nextLevel = false

inLevelSelect = false
inLevelEditor = false
local levelEditName = nil
local hexnums = { [10] = "a", [11] = "b", [12] = "c", [13] = "d", [14] = "e" , [15] = "f" }

titleLoaded = false
local menSel = "none"
local titleOptions = { "New Game", "Select Level", "Create Level", "Quit" }
local inGameOptions = { "Restart", "Edit Level", "Back to Title", "Quit" }
local levelEditOptions = { "Save", "Play Level", "Save and Exit", "Discard and Exit" }
local menIndex = 1

local maxnamelen = 14

local drawOffsetX = 1
local drawOffsetY = 0

local map = {}
local goldMap = {}
local blockTimers = {}
local blockIntv = 5

local monks = {}
local monkTimer = -1
local monkSpawnIntv = 3
local monkTrapIntv = blockIntv/2

local goldCount = 0
local maxGoldCount = 0
local playerLives = 3
local playerScore = 0
local plspawnX = 0
local plspawnY = 0

local plX = 0
local plY = 0
local pfalling = false
local moveTimer = -1
local shootTimer = -1
local spawnTimer = -1
local moveIntv = 0.15

local exX = 0
local exY = 0

local levelList = {}
local currentLevel = 1
local levelLot = 1

local titleLevel = {
	"                                                 ";
	"                                  dddddddddddc   ";
	"                                 4           c   ";
	"      4                                    4 c   ";
	"  bbbbbbc                               bcbbbb   ";
	"  b 4 b c                                c       ";
	"  bbbbb c  4                  dd 0     4 c 4     ";
	"        bbcb                    bbb     bbbbc    ";
	"          c                                 c    ";
	"          c                             ddd c  eb";
	"     dddddc                          bcb   cbbbbb";
	"    c                                 c    c bbbb";
	"b4  c                                4c     bb44b";
	"bbb c    4  e                   bbbcbbbbbbbbbbbbb";
	"bbbbbbbbbbbbbbc           4        cbbbbb 4  bbbb";
	"bbbbbbfff44fbbc 4     cbbbbbbb     cbbbbbbb bbbbb";
	"bbbbffffbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb 4 bbbbbbb";
	"bbbffbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb5  bbbbbbbbb";
}

local function parseValue(x, y, lchar)
if tonumber(lchar, 16) then
	lchar = math.pow(2, tonumber(lchar,16))
	
	if lchar == colours.blue then
	  map[y][x] = 0
	elseif lchar == colours.brown then
	  map[y][x] = 'H'
	elseif lchar == colours.yellow then
	  goldMap[y][x] = 1
	  goldCount = goldCount + 1
	elseif lchar == colours.orange then
	  map[y][x] = 'V'
	elseif lchar == colours.green then
	  map[y][x] = '-'
	elseif lchar == colours.lightGrey then
	  map[y][x] = 'h'
	elseif lchar == colours.grey then
	  map[y][x] = '#'
	elseif lchar == colours.white then
	  plX = x
	  plspawnX = x
	  plY = y
	  plspawnY = y
	elseif lchar == colours.lime then
	  exX = x
	  exY = y
	elseif lchar == colours.red then
	  table.insert(monks, {
		--X and Y, clear enough
		x = x, y = y;
		--Where they spawn when they die
		spawnX = x, spawnY = y;
		-- Any gold they're carring- it's a 1 in 5
		gold = false;
		-- Whether or not they're falling
		falling = false;
		-- Timer if they're dead to respawn
		dead = nil;
		--Whether or not the monk has just spawned
		justSpawned = true;
		--Whether or not the monk has just escaped
		justEscaped = false;
		-- Current aim- it's "up", "down", "across" or "none"
		behaviour = "none";
		-- The desired x position to travel to, when one is necessary.
		desX = nil;
		-- The escape timer
		trapped = nil;
	  })
	end
  end
end

local function loadMap(_sPath)
  if not fs.exists(_sPath) then return false end
  map = {}
  goldMap = {}
  monks = {}
  goldCount = 0
    
  local file = fs.open(_sPath, "r")
  local line = file:readLine()
  while line do
    goldMap[#map+1] = {}
    map[#map+1] = {}
    for i=1,math.min(#line,49) do
      local lchar = string.sub(line,i,i)
      parseValue(i, #map, lchar)
    end
    if #map == 18 then break end
    line = file:readLine()
  end
  file:close()
  maxGoldCount = goldCount
  titleLoaded = false
  return true
end

--When something moves or something needs to be drawn, we
--just change the appropriate tile with this method.
local function updateMap(x,y)
	term.setCursorPos(x + drawOffsetX, y + drawOffsetY)
	term.setBackgroundColour(colours.black)
    if plX == x and plY == y and map[y][x] ~= 0 then
      term.setTextColour(colours.white)
	  if map[y][x] == 1 then term.setBackgroundColour(colours.lightBlue)
	  elseif map[y][x] == "V" then term.setBackgroundColour(colours.blue) end
      term.write("&")
    elseif map[y][x] == 'H' then
      term.setTextColour(colours.brown)
      term.write("H")
	--Level Editor stuff
	elseif map[y][x] == 'h' and (goldCount == 0 or inLevelEditor) then
	  if inLevelEditor then term.setTextColour(colours.lightGrey)
	  else term.setTextColour(colours.brown) end
	  term.write("H")
	elseif map[y][x] == '&' and inLevelEditor then
	  term.setTextColour(colours.pink)
	  term.write('&')
	elseif map[y][x] == 'V' then
	  term.setBackgroundColour(colours.blue)
	  if inLevelEditor then
	    term.setTextColour(colours.orange)
		term.write("V")
	  else
	    term.write(" ")
	  end
    elseif map[y][x] == '-' then
      term.setTextColour(colours.brown)
      term.write(map[y][x])
    elseif map[y][x] == '#' then
      term.setBackgroundColour(colours.grey)
      term.write(" ")
    elseif type(map[y][x]) == "number" then
      local uchar = ' '
	  if map[y][x] == 3 then
		term.setBackgroundColour(colours.lightBlue)
      elseif map[y][x] == 2 and goldMap[y][x] == 1 then
        term.setTextColour(colours.yellow)
        uchar = '$'
      elseif map[y][x] == 1 then
        term.setBackgroundColour(colours.lightBlue)
      elseif map[y][x] == 0 then
        term.setBackgroundColour(colours.blue)
      end
      term.write(uchar)
    elseif goldMap[y][x] == 1 then
      term.setTextColour(colours.yellow)
	  term.write("$")
    elseif exX == x and exY == y and (goldCount == 0 or inLevelEditor) then
      term.setTextColour(colours.lime)
      term.write("@")
    else
      term.write(" ")
    end
end

--It's silly to iterate through all monks when drawing tiles, so
--we do it separately.
local function drawMonk(monk)
	term.setCursorPos(monk.x + drawOffsetX, monk.y + drawOffsetY)
	if monk.justSpawned then term.setTextColour(colours.pink)
	else term.setTextColour(colours.red) end
	if map[monk.y][monk.x] == 1 then term.setBackgroundColour(colours.lightBlue)
	elseif map[monk.y][monk.x] == "V" then term.setBackgroundColour(colours.blue)
	else term.setBackgroundColour(colours.black) end
	term.write("&")
end

--Draws the map for the first time. It barely changes, so we really
--only call this the once.
local function drawMap()
  term.setBackgroundColour(colours.black)
  term.clear()
  for y=1,#map do
    for x=1,49 do
	  updateMap(x,y)
    end
  end
  for _,monk in pairs(monks) do drawMonk(monk)end
end

--When all coins have been collected, we add in invisble ladders and
--the end game portal.
local function drawEndgameMap()
  for y=1,#map do
    for x=1,49 do
	  if map[y][x] == 'h' or (exX == x and exY == y) then
		updateMap(x,y)
	  end
    end
  end
end

--Sets the map back to defaults, so we can start afresh
local function resetMap()
	goldCount = maxGoldCount
	for i=1,#goldMap do
		for j=1,49 do
			if goldMap[i][j] == 0 then goldMap[i][j] = 1 end
		end
	end
	for _,monk in pairs(monks) do
		monk.justSpawned = true
		monk.dead = nil
		monk.trapped = nil
		monk.justEscaped = false
		monk.falling = false
		monk.behaviour = "none"
		monk.x = monk.spawnX
		monk.y = monk.spawnY
	end
	
	for _,timer in pairs(blockTimers) do
		map[timer.y][timer.x] = 0
	end
	blockTimers = {}
	plX = plspawnX
	plY = plspawnY
	
	moveTimer = -1
	shootTimer = -1
	spawnTimer = -1
	monkTimer = -1
	pfalling = false
end

--Draws the HUD. This also rarely changes, so we update it when something happens.
local function drawHUD()
  term.setCursorPos(2,19)
  term.setBackgroundColour(colours.black)
  term.clearLine()
  term.setTextColour(colours.blue)
  term.write("Score: ")
  term.setTextColour(colours.yellow)
  term.write(string.rep("0", 5-math.floor(math.log(playerScore + 1,10)))
    ..playerScore)
  term.setTextColour(colours.yellow)
  term.setCursorPos(25 - #levelList[currentLevel]/2, 19)
  term.write(levelList[currentLevel])
  local lstr = "Men: "
  term.setCursorPos(50 - #lstr - math.floor(math.log(playerLives,10)), 19)
  term.setTextColour(colours.blue)
  term.write(lstr)
  term.setTextColour(colours.yellow)
  term.write(playerLives.."")
end

--Draws the list of levels known, with respect to screen
--real estate
local function drawLevelList()
	local minLev = ((levelLot-1) * 10 + 1)
	local maxLev = minLev + math.min(10, #levelList - (levelLot-1) * 10) - 1
	
	term.setCursorPos(7, 2)
	term.setBackgroundColour(colours.black)
	term.clearLine()
	for j = 1,49 do updateMap(j,2) end
	
	term.setBackgroundColour(colours.black)
	term.setTextColour(colours.white)
	term.setCursorPos(7, 2)
	local msg = "Levels "..minLev.." to "..maxLev.." of "..#levelList
	term.write(msg)
	
	term.setTextColour(colours.yellow)
	term.setCursorPos(4, 2)
	if levelLot > 1 then term.write("<-")
	else term.write("  ") end 
	
	term.setCursorPos(8 + #msg, 2)
	if maxLev < #levelList then term.write("->")
	else term.write(" ") end
	
	for i = 1,10 do
		term.setCursorPos(1, 3+i)
		for j = 1,49 do updateMap(j,3+i) end
		term.setTextColour(colours.white)
		term.setBackgroundColour(colours.black)
		term.setCursorPos(17, 3+i)
		if i + (levelLot-1)*10 - 1 < maxLev then
			term.write(levelList[10 * (levelLot-1) + i])
		end
	end
end

--Loads up and draws up the title screen, for a nice
--intro to Gold Runner
local function loadTitleScreen()
  map = {}
  goldMap = {}
  monks = {}
  goldCount = 0
  for i=1,#titleLevel do
	local line = titleLevel[i]
    goldMap[#map+1] = {}
    map[#map+1] = {}
    for i=1,math.min(#line,49) do
      local lchar = string.sub(line,i,i)
      parseValue(i, #map, lchar)
    end
    if #map == 18 then break end
  end
  maxGoldCount = goldCount
  
  drawMap()
  term.setCursorPos(1,19)
  term.setBackgroundColour(colours.blue)
  term.clearLine()
  
  menIndex = 1
  titleLoaded = true
end

--Opens an in-game menu to display a series of options.
local function inGameMenu(menuList)
	menIndex = 1
	
	local squareTop,squareBottom = 4,6 + #menuList * 2
	local squareSize = 0
	for i=1,#menuList do squareSize = math.max(squareSize, #menuList[i] + 6) end
	
	for y=squareTop,squareBottom do
		term.setCursorPos(w/2 - squareSize/2, y)
		term.setBackgroundColour(colours.lightBlue)
		term.write(string.rep(" ", squareSize))
		
		if y ~= squareTop and y ~= squareBottom then
			term.setCursorPos(w/2 - squareSize/2 + 1, y)
			term.setBackgroundColour(colours.black)
			term.write(string.rep(" ", squareSize - 2))
		end
		
		if y ~= squareTop and y ~= squareBottom and y % 2 == 0 then
			local opt = menuList[(y - squareTop) / 2]
			term.setCursorPos(w/2 - #opt/2, y)
			term.setTextColour(colours.white)
			term.write(opt)
		end
	end
	
	local p1 = nil
	repeat
		for i=1,#menuList do
			term.setBackgroundColour(colours.black)
			term.setTextColour(colours.yellow)
			if i == menIndex then
				term.setCursorPos(w/2 - squareSize/2 + 1, squareTop + i * 2)
				term.write(">")
				term.setCursorPos(w/2 + squareSize/2 - 2, squareTop + i * 2)
				term.write("<")
			else
				term.setCursorPos(w/2 - squareSize/2 + 1, squareTop + i * 2)
				term.write(" ")
				term.setCursorPos(w/2 + squareSize/2 - 2, squareTop + i * 2)
				term.write(" ")
			end
		end
		_,p1 = os.pullEvent("key")
		
		if p1 == keys.up and menIndex > 1 then menIndex = menIndex - 1
		elseif p1 == keys.down and menIndex < #menuList then menIndex = menIndex + 1 end
	until p1 == keys.enter
	
	return menuList[menIndex]
end

--Checks to see if any given desired move is legal. Monks and players both use this.
local function isLegalMove(initX,initY,finX,finY)
	if finY < 1 or finY > #map or finX < 1 or finX > 49 then 
		return false 
	end
	
	if map[finY][finX] ~= 0 and map[finY][finX] ~= '#' then
		--This reports 'self moves' as being illegal, but that's fine
		for _,monk in pairs(monks) do
			if monk.x == finX and monk.y == finY then return false end
		end

		if finY == initY-1 and (map[initY][initX] == "H" or (map[initY][initX] == "h" and goldCount == 0))
			then return true
		elseif finY == initY+1 and (map[finY][finX] == "H" or (map[finY][finX] == "h" and goldCount == 0)
				or (type(map[finY][finX]) == "number" and map[finY][finX] > 0) or map[finY][finX] == nil or
				map[finY][finX] == "V" or map[finY][finX] == "-" or (map[finY][finX] == 'h' and goldCount ~= 0)) 
			then return true
		elseif finX == initX-1 or finX == initX+1 then 
			return true 
		end
	end
end

--Moves the player to a given step.
local function movePlayer(x,y,ignoreLegal)
	if not ignoreLegal and not isLegalMove(plX,plY,x,y) then return false end
	
	local ox = plX
	local oy = plY
	plX = x
	plY = y
	
	updateMap(ox,oy)
	updateMap(x,y)
	if goldMap[y][x] == 1 then
		goldMap[y][x] = 0
		goldCount = goldCount - 1
		playerScore = playerScore + 5
		if started then drawHUD() end
		if (goldCount == 0) then
			drawEndgameMap()
		end
	elseif exX == plX and exY == plY and goldCount == 0 then
		started = false
		nextLevel = true
	end
	
	pfalling = (y < #map and map[y][x] ~= '-' and map[y][x] ~= 'H' and not (map[y][x] == 'h' and goldCount == 0) 
		and (map[y+1][x] == nil or map[y+1][x] == "V" or map[y+1][x] == 2 or map[y+1][x] == '-'))
	if (y < #map and map[y+1][x] == 'h' and goldCount ~= 0) then pfalling = true end
	for _,monk in pairs(monks) do
		if monk.x == plX and monk.y == plY + 1 then pfalling = false break end
	end
	
	return true
end

local function updateMonks()
	for _,monk in pairs(monks) do
		--Absolute first step- if he's trapped or dead, he's going nowhere
		if monk.trapped or monk.dead then
		--If he's just spawned he takes a second to orient himself
		elseif monk.justSpawned then
			monk.justSpawned = false
			--We evaluate their falling behaviour here (as freed monks CAN stand on air)
			monk.falling = (monk.y < #map and map[monk.y][monk.x] ~= '-' and map[monk.y][monk.x] ~= "H" and not
					(map[monk.y][monk.x] == 'h' and goldCount == 0) and (map[monk.y+1][monk.x] == nil or map[monk.y+1][monk.x] == "V" or
					map[monk.y+1][monk.x] == 2 or map[monk.y+1][monk.x] == '-') and type(map[monk.y][monk.x] ~= "number"))
			for _,omonk in pairs(monks) do
				if omonk.x == monk.x and omonk.y == monk.y + 1 then monk.falling = false break end
			end
			if monk.x == plX and monk.y == plY + 1 then monk.falling = false end
		--Then we consider if he's just gotten out of a hole
		elseif monk.justEscaped then
			monk.justEscaped = false
			--He tries the player side first
			local playerSide = (plX-monk.x) / math.abs(plX-monk.x)
			if isLegalMove(monk.x, monk.y, monk.x + playerSide, monk.y) then
				monk.x = monk.x + playerSide
				updateMap(monk.x - playerSide, monk.y)
			elseif isLegalMove(monk.x, monk.y, monk.x - playerSide, monk.y) then
				monk.x = monk.x - playerSide
				updateMap(monk.x + playerSide, monk.y)
			end
			drawMonk(monk)
		--Then we evaluate falling
		elseif monk.falling then
			monk.behaviour = "none"
			monk.y = monk.y + 1
			updateMap(monk.x, monk.y-1)
			drawMonk(monk)
			monk.desX = nil
			if type(map[monk.y][monk.x]) == "number" then
				monk.trapped = os.startTimer(monkTrapIntv)
				monk.falling = false
			else
				monk.falling = (monk.y < #map and map[monk.y][monk.x] ~= '-' and map[monk.y][monk.x] ~= "H" and not
					(map[monk.y][monk.x] == 'h' and goldCount == 0) and (map[monk.y+1][monk.x] == nil or map[monk.y+1][monk.x] == "V" or
					map[monk.y+1][monk.x] == 2 or map[monk.y+1][monk.x] == '-') and type(map[monk.y][monk.x] ~= "number"))
				for _,omonk in pairs(monks) do
					if omonk.x == monk.x and omonk.y == monk.y + 1 then monk.falling = false break end
				end
				if monk.x == plX and monk.y == plY + 1 then monk.falling = false end
				if monk.justEscaped then monk.falling = false end
			end
		--If he's on his feet and not trapped, he's allowed to think about where to move
		elseif monk.y == plY then
			--Is the monk on the same level as the player? How lucky! They'll just walk towards him
			monk.desX = plX
			monk.behaviour = "across"
		--Y difference takes precedence over X (as in the original, makes them a bit smarter)
		elseif monk.y < plY then
			--If they can move up, they will
			if isLegalMove(monk.x,monk.y,monk.x,monk.y+1) and not monk.justEscaped then
				monk.y = monk.y+1
				updateMap(monk.x, monk.y-1)
				drawMonk(monk)
				monk.desX = nil
				--A down move can lead to a fall, so we check if they're now falling.
				monk.falling = (monk.y < #map and map[monk.y][monk.x] ~= '-' and map[monk.y][monk.x] ~= "H" and not
					(map[monk.y][monk.x] == 'h' and goldCount == 0) and (map[monk.y+1][monk.x] == nil or map[monk.y+1][monk.x] == "V" or
					map[monk.y+1][monk.x] == 2 or map[monk.y+1][monk.x] == '-') and type(map[monk.y][monk.x] ~= "number"))
				for _,omonk in pairs(monks) do
					if omonk.x == monk.x and omonk.y == monk.y + 1 then monk.falling = false break end
				end
				if monk.x == plX and monk.y == plY + 1 then monk.falling = false end
			--Otherwise, it's off to the nearest ladder, monkey bars or perilous ledge to jump off
			--assuming they haven't found one already
			elseif monk.desX == nil then
				if monk.behaviour ~= "down" then monk.desX = nil end
				monk.behaviour = "down"
				monk.desX = nil
				local cmLeft = true
				local cmRight = true
				--We try to find the nearest by searching alternate left and right at variable distance
				for i=1,math.max(monk.x - 1, 49 - monk.x) do
					if monk.x-i > 0 and cmLeft then
						--If a wall blocks the monks path, they can't keep going left or right
						cmLeft = map[monk.y][monk.x-i] ~= 0
						--But if it's all clear, they look for something to climb/jump down
						if cmLeft and (map[monk.y+1][monk.x-i] == "H" or (map[monk.y+1][monk.x-i] == 'h' and goldCount == 0)
							or map[monk.y+1][monk.x-i] == nil or map[monk.y][monk.x-i] == '-') then
							monk.desX = monk.x-i
							break
						end
					end
					if monk.x+i < 50 and cmRight then
						--If a wall blocks the monks path, they can't keep going left or right
						cmRight = map[monk.y][monk.x+i] ~= 0
						--But if it's all clear, they look for something to climb/jump down
						if cmRight and (map[monk.y+1][monk.x+i] == "H" or (map[monk.y+1][monk.x+i] == 'h' and goldCount == 0)
							or map[monk.y+1][monk.x+i] == nil or map[monk.y][monk.x+i] == '-') then
							monk.desX = monk.x+i
							break
						end
					end
				end
			end
		elseif monk.y > plY then
			if monk.behaviour ~= "up" then monk.desX = nil end
			monk.behaviour = "up"
			--Same deal again- try moving up first
			if isLegalMove(monk.x,monk.y,monk.x,monk.y-1) then
				monk.y = monk.y-1
				updateMap(monk.x, monk.y+1)
				drawMonk(monk)
				monk.desX = nil
				--You can never move up and start falling, so we don't bother to check
			--Otherwise they need ladders to climb up
			elseif monk.desX == nil then
				monk.behaviour = "up"
				monk.desX = nil
				local cmLeft = true
				local cmRight = true
				--We try to find the nearest by searching alternate left and right at variable distance
				for i=1,math.max(monk.x - 1, 49 - monk.x) do
					if monk.x-i > 0 and cmLeft then
						--If a wall blocks the monks path or a pit is in the way, they can't keep going left or right
						cmLeft = map[monk.y][monk.x-i] ~= 0 and (monk.y == #map or map[monk.y+1][monk.x-i] ~= nil
								or map[monk.y][monk.x-i] == '-' or map[monk.y][monk.x-i] == "H" or (map[monk.y][monk.x-i] == "h"
								and goldCount == 0))
						--But if it's all clear, they look for a ladder
						if cmLeft and (map[monk.y][monk.x-i] == "H" or (map[monk.y][monk.x-i] == 'h' and goldCount == 0)) then
							monk.desX = monk.x-i
							break
						end
					end
					if monk.x+i < 50 and cmRight then
						cmRight = map[monk.y][monk.x+i] ~= 0 and (monk.y == #map or map[monk.y+1][monk.x+i] ~= nil
								or map[monk.y][monk.x+i] == '-' or map[monk.y][monk.x+i] == "H" or (map[monk.y][monk.x+i] == "h"
								and goldCount == 0))
						if cmRight and (map[monk.y][monk.x+i] == "H" or (map[monk.y][monk.x+i] == 'h' and goldCount == 0)) then
							monk.desX = monk.x+i
							break
						end
					end
				end
			end
		end
		
		if not (monk.trapped or monk.dead) then
			--Has the monk decided on moving left or right? If so we try to move him
			if monk.desX and not monk.falling then
				local mdir = monk.desX - monk.x
				local mdir = mdir / math.abs(mdir)
				if isLegalMove(monk.x,monk.y,monk.x+mdir,monk.y) then
					monk.x = monk.x + mdir
					updateMap(monk.x - mdir, monk.y)
					drawMonk(monk)
				else
					--This allows re-evaluations if they get stuck- not ideal but good enough
					monk.desX = nil
				end
			end
			monk.falling = (monk.y < #map and map[monk.y][monk.x] ~= '-' and map[monk.y][monk.x] ~= "H" and not
					(map[monk.y][monk.x] == 'h' and goldCount == 0) and (map[monk.y+1][monk.x] == nil or map[monk.y+1][monk.x] == "V" or
					map[monk.y+1][monk.x] == 2 or map[monk.y+1][monk.x] == '-') and type(map[monk.y][monk.x] ~= "number"))
			for _,omonk in pairs(monks) do
				if omonk.x == monk.x and omonk.y == monk.y + 1 then monk.falling = false break end
			end
			if monk.x == plX and monk.y == plY + 1 then monk.falling = false end
			--We have caught and killed the player
			if monk.x == plX and monk.y == plY and spawnTimer == -1 then
				spawnTimer = os.startTimer(2)
			end
		end
	end
end

local function updateBlockTimer(tid)
	local remAt = nil
	for i,v in ipairs(blockTimers) do
		if v.timer == tid then
			if map[v.y][v.x] == 3 then
				for _,monk in pairs(monks) do
					if monk.x == v.x and monk.y == v.y-1 then
						map[v.y][v.x] = 0
						remAt = i
						break
					end
				end
				if not remAt then
					map[v.y][v.x] = 2
					v.timer = os.startTimer(blockIntv)
				end
			elseif map[v.y][v.x] == 2 then
				map[v.y][v.x] = 1
				v.timer = os.startTimer(0.1)
			elseif map[v.y][v.x] == 1 then
				map[v.y][v.x] = 0
				--If the player is caught in a block, he dies
				if v.y == plY and v.x == plX then
					spawnTimer = os.startTimer(2)
				end
				for _,monk in pairs(monks) do
					if monk.x == v.x and monk.y == v.y then
						monk.dead = os.startTimer(monkSpawnIntv)
						--Easiest way to get them out of the way rather than evaluation
						monk.x = -1
						monk.y = -1
						monk.trapped = nil
					end
				end
				remAt = i
			end
			updateMap(v.x,v.y)
			break
		end
	end
	if remAt then table.remove(blockTimers,remAt) end
end

local function shootBlock(x,y)
	if y <= #map and map[y][x] == 0 and (map[y-1][x] == nil 
			or map[y-1][x] == 2 or (map[y-1][x] == 'h' and goldCount > 0)) then
		map[y][x] = 3
		table.insert(blockTimers, {x = x; y = y; timer = os.startTimer(0.1);} )
		updateMap(x,y)
	end
end

local function handleEvents()
	local id,p1,p2,p3 = os.pullEvent()
	
	if id == "key" then
		--Menu Handling
		if p1 == keys.up then
			if menIndex > 1 then menIndex = menIndex - 1 end
		elseif p1 == keys.down then
			if inLevelSelect then 
				if menIndex < math.min(10, #levelList - (levelLot-1)*10) then 
					menIndex = menIndex + 1
				end
			elseif menIndex < #titleOptions then menIndex = menIndex + 1 end
		elseif p1 == keys.left and inLevelSelect and levelLot > 1 then
			levelLot = levelLot - 1
			drawLevelList()
		elseif p1 == keys.right and inLevelSelect and levelLot * 10 < #levelList then
			levelLot = levelLot + 1
			drawLevelList()
		end
	
		--Game Handling
		if p1 == keys.a and moveTimer == -1 and spawnTimer == -1 then
			movePlayer(plX-1,plY)
			moveTimer = os.startTimer(moveIntv)
		elseif p1 == keys.d and moveTimer == -1 and spawnTimer == -1 then
			movePlayer(plX+1,plY)
			moveTimer = os.startTimer(moveIntv)
		elseif p1 == keys.w and moveTimer == -1 and spawnTimer == -1 then
			movePlayer(plX,plY-1)
			moveTimer = os.startTimer(moveIntv)
		elseif p1 == keys.s and moveTimer == -1 and spawnTimer == -1 then
			movePlayer(plX,plY+1)
			moveTimer = os.startTimer(moveIntv)
		elseif p1 == keys.q and shootTimer == -1 and not pfalling and spawnTimer == -1 then
			shootBlock(plX-1,plY+1)
			shootTimer = os.startTimer(moveIntv)
		elseif p1 == keys.e and shootTimer == -1 and not pfalling and spawnTimer == -1 then
			shootBlock(plX+1,plY+1)
			shootTimer = os.startTimer(moveIntv)
		elseif p1 == keys.space and started then
			started = false
		elseif p1 == keys.enter then
			if not started then
				if inLevelSelect then
					currentLevel = menIndex + (levelLot - 1) * 10
					menSel = "New Game"
				else
					menSel = titleOptions[menIndex]
				end
			else
				started = false
				menIndex = 1
				menSel = inGameMenu(inGameOptions)
			end
		end
	elseif id == "timer" then
		if p1 == shootTimer then shootTimer = -1
		elseif p1 == spawnTimer then
			started = false
		elseif p1 == moveTimer then
			if pfalling then
				movePlayer(plX,plY+1)
				moveTimer = os.startTimer(moveIntv)
			else
				moveTimer = -1
			end
		elseif p1 == monkTimer then
			updateMonks()
			monkTimer = os.startTimer(moveIntv * 2)
		elseif updateBlockTimer(p1) then
		else
			for _,monk in pairs(monks) do
				if p1 == monk.trapped then
					--You can stand on a monk to force them to be killed- so we check for that
					--along with being buried in tunnels, etc.
					local stillTrapped = map[monk.y-1][monk.x] == 0 or (plX == monk.x and plY == monk.y-1)
					for _,omonk in pairs(monks) do
						if omonk.x == monk.x and omonk.y == monk.y-1 then
							stillTrapped = true
							break
						end
					end
					--Perpetually trapped monks will try to excape much more quickly
					if stillTrapped then
						--This needs to be tweaked
						monk.trapped = os.startTimer(0.75)
					else
						--When free, they head in your general direction, re-evaluate later
						monk.y = monk.y - 1
						--This is necessary to stop 'double jumping'
						monk.desX = nil
						monk.trapped = nil
						monk.behaviour = "none"
						monk.justEscaped = true
						
						updateMap(monk.x, monk.y+1)
						drawMonk(monk)
					end
					break
				elseif p1 == monk.dead then
					--Same deal- you can camp spawn
					local stillDead = plX == monk.spawnX and plY == monk.spawnY
					for _,omonk in pairs(monks) do
						if omonk.x == monk.spawnX and omonk.y == monk.spawnY then
							stillDead = true
							break
						end
					end
					--They'll spawn the second you give them the chance
					if stillDead then
						monk.dead = os.startTimer(0.5)
					else
						monk.x = monk.spawnX
						monk.y = monk.spawnY
						monk.dead = nil
						monk.justSpawned = true
						monk.behaviour = "none"
						drawMonk(monk)
						break
					end
				end
			end
		end
	end
end

--[[			Level Editor			]]--

local pallette = {  { t = colours.black, b = colours.blue, s = " ", n = "Solid Ground", v = 0 },
				    { t = colours.orange, b = colours.blue, s = "V", n = "Trap Ground", v = "V" },
					{ t = colours.grey, b = colours.grey, s = " ", n = "Cement Ground", v = "#" },
					{ t = colours.brown, b = colours.black, s = "H", n = "Ladder", v = "H" },
					{ t = colours.brown, b = colours.black, s = "-", n = "Monkey Bars", v = "-" },
					{ t = colours.white, b = colours.black, s = "&", n = "Player Spawn", v = "player" },
					{ t = colours.red, b = colours.black, s = "&", n = "Mad Monk", v = "&" },
					{ t = colours.yellow, b = colours.black, s = "$", n = "Gold", v = "$" },
					{ t = colours.lightGrey, b = colours.black, s = "H", n = "Hidden Ladder", v = "h" },
					{ t = colours.lime, b = colours.black, s = "@", n = "Exit Portal", v = "@" },
					{ t = colours.red, b = colours.black, s = "ERASE", n = "Eraser", v = nil } }
local brushType = 1

local function getHexOf(colour)
	if not colour or not tonumber(colour) then 
		return " " 
	end
	local value = math.log(colour)/math.log(2)
	if value > 9 then 
		value = hexnums[value] 
	end
	return value
end

local function drawFooter()
	for i=1,h-1 do
		if i % 2 == 0 then term.setBackgroundColour(colours.grey)
		else term.setBackgroundColour(colours.yellow) end
		term.setCursorPos(1,i)
		term.write(" ")
		term.setCursorPos(w,i)
		term.write(" ")
	end
	
	term.setBackgroundColour(colours.black)
	term.setTextColour(colours.blue)
	term.setCursorPos(2,h)
	term.clearLine()
	term.write("Editor Mode: ")
	term.setTextColour(colours.yellow)
	term.write(levelEditName)
	local msg = "Tool: "..pallette[brushType].n.." "..pallette[brushType].s
	term.setCursorPos(w - #msg - 1, 19)
	term.setTextColour(colours.blue)
	term.write("Tool: ")
	term.setTextColour(colours.yellow)
	term.write(pallette[brushType].n.." ")
	term.setBackgroundColour(pallette[brushType].b)
	term.setTextColour(pallette[brushType].t)
	term.write(pallette[brushType].s)
end

local function drawPallette(xpos,ypos)
	local xdim = 7
	local ydim = 5
	local left = xpos
	local top  = ypos
	if xpos + xdim > w then left = left + (w - xpos - xdim) end
	if ypos + ydim > h then top = top + (h - ypos - ydim) end
	
	--There's no easy way to do this... so we draw it manually :(
	for i=0,4 do
		term.setCursorPos(left, top + i)
		term.setBackgroundColour(colours.black)
		term.setTextColour(colours.red)
		if i == 0 or i == 4 then term.write("*-----*")
		else term.write("*     *") end
	end
	
	for i=1,#pallette-1 do
		local ypl = 1
		local xmv = i
		if i > 5 then ypl = 2 xmv = i - 5 end
		
		term.setCursorPos(left + xmv, top+ypl)
		term.setBackgroundColour(pallette[i].b)
		term.setTextColour(pallette[i].t)
		term.write(pallette[i].s)
	end
	
	term.setCursorPos(left + 1, top + 3)
	term.setBackgroundColour(colours.red)
	term.setTextColour(colours.black)
	term.write("ERASE")
	
	local _,button,x,y = os.pullEvent("mouse_click")
	
	if button == 1 then
		if y == top + 1 and x > left and x < left + 6 then
			brushType = x-left
		elseif y == top + 2 and x > left and x < left + 6 then
			brushType = x-left+5
		elseif y == top + 3 and x > left and x < left + 6 then
			brushType = 11
		end
	end
	
	for y = top,top+ydim do
		for x = left,left+xdim do	
			--Not sure why the -2 is necessary
			if map[y+drawOffsetY] then updateMap(x-2,y+drawOffsetY) end
		end
	end
	drawFooter()
	return
end

local function saveCurrentMap(path)
	local file = io.open(shell.resolve(".").."/levels/"..path, "w")
	if not file then return false end
	
	drawMap()
	drawFooter()
	local msg = "Saving.."
	term.setCursorPos(w/2-#msg/2, 5)
	term.setTextColour(colours.yellow)
	term.setBackgroundColour(colours.blue)
	term.write(msg)
	term.setCursorPos(w/2-9, 6)
	term.setBackgroundColour(colours.red)
	term.write(string.rep(" ", 18))
	term.setCursorPos(w/2-9,6)
	term.setBackgroundColour(colours.lime)
	
	for y=1,#map do
		local xstr = ""
		for x=1,49 do
			--This again...
			    if map[y][x] == 0 then xstr = xstr..getHexOf(colours.blue)
			elseif map[y][x] == "V" then xstr = xstr..getHexOf(colours.orange)
			elseif map[y][x] == "#" then xstr = xstr..getHexOf(colours.grey)
			elseif map[y][x] == "H" then xstr = xstr..getHexOf(colours.brown)
			elseif map[y][x] == "h" then xstr = xstr..getHexOf(colours.lightGrey)
			elseif map[y][x] == "-" then xstr = xstr..getHexOf(colours.green)
			elseif map[y][x] == "&" then xstr = xstr..getHexOf(colours.red)
			elseif goldMap[y][x] == 1 then xstr = xstr..getHexOf(colours.yellow)
			elseif plX == x and plY == y then xstr = xstr..getHexOf(colours.white)
			elseif exX == x and exY == y then xstr = xstr..getHexOf(colours.lime)
			else xstr = xstr.." "
			end
		end
		file:write(xstr.."\n")
		term.write(" ")
		sleep(0)
	end
	file:close()
	return true
end

local function runLevelEditor()
	inLevelEditor = true
	term.setBackgroundColour(colours.black)
	term.clear()
	if not fs.exists(shell.resolve(".").."/levels/"..levelEditName) then
		map = {}
		goldMap = {}
		monks = {}
		for i=1,18 do map[i] = {} goldMap[i] = {} end
		plX = 2
		plY = 2
		plspawnX = plX
		plspawnY = plY
		exX = 48
		exY = 17
	else
		loadMap(shell.resolve(".").."/levels/"..levelEditName)
		for _,monk in pairs(monks) do
			map[monk.y][monk.x] = "&"
		end
		monks = {}
	end
	
	drawMap()
	drawFooter()
	
	while inLevelEditor do
		local id,button,x,y = os.pullEvent()
		if id == "mouse_click" or id == "mouse_drag" then
			if button == 2 then 
				drawPallette(x,y)
			elseif x > drawOffsetX and x <= 49 + drawOffsetX and y > drawOffsetY and y <= 18 + drawOffsetY then
				if pallette[brushType].v == "player" then
					local ox = plX
					local oy = plY
					if plX == exX and plY == exY then
						exX = ox
						exY = oy
					end
					plX = x - drawOffsetX
					plY = y - drawOffsetY
					map[plY][plX] = nil
					goldMap[plY][plX] = nil
					updateMap(ox,oy)
				elseif pallette[brushType].v == "@" then
					local ox = exX
					local oy = exY
					if plX == exX and plY == exY then
						plX = ox
						plY = oy
					end
					exX = x - drawOffsetX
					exY = y - drawOffsetY
					map[plY][plX] = nil
					goldMap[plY][plX] = nil
					updateMap(ox,oy)
				elseif pallette[brushType].v == "$" then
					goldMap[y-drawOffsetY][x-drawOffsetX] = 1
					map[y-drawOffsetY][x-drawOffsetX] = nil
				elseif pallette[brushType].v == nil then
					map[y-drawOffsetY][x-drawOffsetX] = nil
					goldMap[y-drawOffsetY][x-drawOffsetX] = nil
				else
					map[y-drawOffsetY][x-drawOffsetX] = pallette[brushType].v
					goldMap[y-drawOffsetY][x-drawOffsetX] = nil
					--term.setCursorPos(1,19)
					--print("At "..(x-drawOffsetX)..", "..(y-drawOffsetY).." have placed "..pallette[brushType].v)
				end
				updateMap(x-drawOffsetX, y-drawOffsetY)
			end
		elseif id == "mouse_scroll" then
			brushType = brushType + button
			if brushType == 0 then brushType = #pallette
			elseif brushType > #pallette then brushType = 1 end
			drawFooter()
		elseif id == "key" and button == keys.enter then
			menSel = inGameMenu(levelEditOptions)
			if menSel == "Save" then
				saveCurrentMap(levelEditName)
				drawMap()
				drawFooter()
			elseif menSel == "Save and Exit" then
				saveCurrentMap(levelEditName)
				menSel = "none"
				inLevelEditor = false
			elseif menSel == "Discard and Exit" then
				menSel = "none"
				inLevelEditor = false
			elseif menSel == "Play Level" then
				saveCurrentMap(levelEditName)
				inLevelEditor = false
			end
		end
	end
end


local function runLevelSelect()
	if not titleLoaded then
		loadTitleScreen()
		monkTimer = os.startTimer(moveIntv * 1.5)
	else 
		drawMap()
		drawEndgameMap()
		term.setCursorPos(1,19)
		term.setBackgroundColour(colours.blue)
		term.clearLine()
	end
	drawLevelList()
	
	menSel = "none"
	repeat
		handleEvents()
		
		term.setBackgroundColour(colours.black)
		term.setTextColour(colours.yellow)
		for i=1,10 do
			term.setCursorPos(16,3+i)
			if i == menIndex then
				term.write(">")
			else
				term.write(" ")
			end
		end
	until menSel ~= "none"
	inLevelSelect = false
	menSel = "New Game"
end

local function runTitle()
	loadTitleScreen()
	term.setCursorPos(15,3)
	term.setTextColour(colours.red)
	term.setBackgroundColour(colours.black)
	term.write("Gold Runner")
	term.setCursorPos(16,4)
	term.write("By Nitrogen Fingers")
	
	term.setTextColour(colours.white)
	for i=1,#titleOptions do
		term.setCursorPos(19, 5 + (i*2))
		term.write(titleOptions[i])
	end
	  
	term.setCursorPos(16, 7)
	term.setTextColour(colours.yellow)
	term.write("->")
	
	menSel = "none"
	monkTimer = os.startTimer(moveIntv * 1.5)
	
	repeat
		handleEvents()
		
		term.setBackgroundColour(colours.black)
		term.setTextColour(colours.yellow)
		for i=1,#titleOptions do
			term.setCursorPos(16, 5 + i*2)
			if menIndex == i then term.write("->")
			else term.write("  ") end
		end
	until menSel ~= "none"
end

local function playLevel()
	loadMap(shell.resolve(".").."/levels/"..levelList[currentLevel])
	running = true
	while running do
		drawMap()
		drawHUD()
		os.pullEvent("key")
		movePlayer(plX,plY,true)
		
		monkTimer = os.startTimer(moveIntv * 1.5)
		moveTimer = os.startTimer(moveIntv)
		shootTimer = -1
		spawnTimer = -1
		
		started = true
		while started do
			handleEvents()
		end
		
		if menSel == "Quit" or menSel == "Back to Title" or menSel == "Edit Level" then
			running = false
			return
		end
		menSel = "none"
		
		if nextLevel then
			if currentLevel == #levelList then 
				started = false
				running = false
				break
			else
				currentLevel = currentLevel + 1
				playerLives = playerLives + 1
				resetMap()
				loadMap(shell.resolve(".").."/levels/"..levelList[currentLevel])
			end
			nextLevel = false
		else
			playerLives = playerLives-1
			if playerLives > 0 then resetMap()
			else 
				running = false 
			end
		end
	end
	
	if nextLevel then
		local msg = "All levels defeated, Gold Runner!"
		term.setBackgroundColour(colours.black)
		term.setTextColour(colours.lime)
		term.setCursorPos(25 - #msg/2, 2)
		term.write(msg)
	else
		local msg = "Game over!"
		term.setBackgroundColour(colours.black)
		term.setTextColour(colours.red)
		term.setCursorPos(25 - #msg/2, 2)
		term.write(msg)
	end
	currentLevel = 1
	sleep(2)
end

term.clear()
if not fs.exists(shell.resolve(".").."/levels") then
	error("Level directory not present!")
end
levelList = fs.list(shell.resolve(".").."/levels")
if #levelList == 0 then
	error("Level directory is empty!")
end

runTitle()
menIndex = 1

while menSel ~= "Quit" do
	if menSel == "Select Level" then
		inLevelSelect = true
		runLevelSelect()
	elseif menSel == "New Game" then
		playerLives = 3
		playerScore = 0
		playLevel()
	elseif menSel == "Create Level" then
		--This is a bit lazy... well it's all been a bit lazy :P
		drawMap()
		term.setCursorPos(1,19)
		term.setBackgroundColour(colours.blue)
		term.clearLine()
		
		term.setCursorPos(16,10)
		term.setBackgroundColour(colours.black)
		term.setTextColour(colours.white)
		term.write("Enter level name:")
		term.setTextColour(colours.lime)
		term.setCursorPos(17,11)
		term.setCursorBlink(true)
		local levelName = ""
		
		local id,p1
		repeat
			id,p1 = os.pullEvent()
			if id == "key" and p1 == keys.backspace then
				levelName = string.sub(levelName, 1, #levelName - 1)
			elseif id == "timer" and p1 == monkTimer then 
				updateMonks()
				monkTimer = os.startTimer(moveIntv * 2)
			elseif id == "char" and #levelName < 14 then
				levelName = levelName..p1
			end
			term.setTextColour(colours.lime)
			term.setCursorPos(17,11)
			term.write(levelName..string.rep(" ",14 - #levelName))
			term.setCursorPos(17 + #levelName ,11)
		until id == "key" and p1 == keys.enter and #levelName > 0
		
		term.setCursorBlink(false)
		levelEditName = levelName
		runLevelEditor()
		
		if menSel == "Play Level" then
			currentLevel = nil
			levelList = fs.list(shell.resolve(".").."/levels")
			for num,name in pairs(levelList) do 
				if name == levelName then
					currentLevel = num
					break
				end
			end
			menSel = "New Game"
		else
			menSel = "none"
		end
	elseif menSel == "Edit Level" then
		levelEditName = levelList[currentLevel]
		runLevelEditor()
		term.setBackgroundColour(colours.black)
		term.clear()
		
		if menSel == "Play Level" then
			menSel = "New Game"
		else
			menSel = "none"
		end
	elseif menSel == "none" or menSel == "Back to Title" then
		runTitle()
	end
	menIndex = 1
end

term.setBackgroundColour(colours.black)
shell.run("clear")
term.setTextColour(colours.white)
print("Thanks for playing Gold Runner!")
