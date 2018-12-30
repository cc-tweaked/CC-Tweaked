--[[
		3D Print
		A printing program for use with NPaintPro
		
		By NitrogenFingers
]]--

local activeCommander = -1
local operatingPrint = false

--Whether or not the print can be ended
local function endPrint()
	operatingPrint = false
end

--The list of all commands the printer can be ginve
local commandList = {
	["FW"] = { turtle.dig, turtle.forward };
	["BK"] = turtle.back;
	["UP"] = { turtle.digUp, turtle.up };
	["DW"] = { turtle.digDown, turtle.down };
	["TL"] = turtle.turnLeft;
	["TR"] = turtle.turnRight;
	["TU"] = { turtle.turnLeft, turtle.turnLeft };
	["PF"] = { turtle.dig, turtle.place };
	["PU"] = { turtle.digUp, turtle.placeUp };
	["PD"] = { turtle.digDown, turtle.placeDown };
	["SS"] = turtle.select;
	["RF"] = turtle.refuel;
	["DE"] = endPrint;
}

--Splits a string according to a pattern into a table				
local function split(str, pattern)
  local t = { }
  local fpat = "(.-)" .. pattern
  local last_end = 1
  local s, e, cap = str:find(fpat, 1)
  while s do
    if s ~= 1 or cap ~= "" then
      table.insert(t,cap)
    end
    last_end = e+1
    s, e, cap = str:find(fpat, last_end)
  end
  if last_end <= #str then
    cap = str:sub(last_end)
    table.insert(t, cap)
  end
  return t
end

--Listens for any instructions given referring to identification and activation. Once activated, the mode exits.
local function respondToQuery()
	while true do
		print("Listening for ACT/ID query")
		local id,key = rednet.receive()
		print("Received : "..key)
		
		if key == "$3DPRINT IDENTIFY" then
			print("Requested Identification")
			rednet.send(id, "$3DPRINT IDACK "..os.getComputerLabel())
		
		elseif key == "$3DPRINT ACTIVATE" then
			print("Requested Activation")
			activeCommander = id
			rednet.send(id, "$3DPRINT ACTACK")
			break
		end
	end
end

--Performs the print. Follows instrutions as given, and responds as necessary
local function performPrint()
	operatingPrint = true
	while operatingPrint do
		local id,msg = rednet.receive()
		print("Command : "..msg)
		
		if id == activeCommander and string.find(msg, "$PC") == 1 then
			local cmds = split(msg, " ")
			
			--It's a bit of a hack, but those are the 2 methods required for a refuel
			if turtle.getFuelLevel() == 0 and cmds[2] ~= "SS" and cmds[2] ~= "RF" then
				rednet.send(id, "$3DPRINT OOF")
			elseif (tonumber(cmds[3])) and turtle.getItemCount(tonumber(cmds[3])) == 0 and
					turtle.getFuelLevel() ~= 0 then
				rednet.send(id, "$3DPRINT DEP")
			else
				if cmds[2] == "RF" then cmds[3] = "64" end
				if type(commandList[cmds[2]]) == "function" then
					commandList[cmds[2]](tonumber(cmds[3]))
				elseif type(commandList[cmds[2]]) == "table" then
					for i=1,#commandList[cmds[2]] do
						commandList[cmds[2]][i](tonumber(cmds[3]))
					end
				end
			
				rednet.send(activeCommander, "$3DPRINT ACK")
			end
		end
	end
end

rednet.open("right")
term.clear()
term.setCursorPos(1,1)
if not os.getComputerLabel() then
	term.write("Name this computer:")
	os.setComputerLabel(io.read())
end
print("3D printer online")

while true do
	--Wait for activation
	respondToQuery()
	--Perform the print
	performPrint()
end