
--  
--  Lua IDE
--  Made by GravityScore
--  


--  -------- Variables

-- Version
local version = "1.0"
local args = {...}

-- Editing
local w, h = term.getSize()
local tabWidth = 2

local autosaveInterval = 20
local allowEditorEvent = true
local keyboardShortcutTimeout = 0.4

-- Clipboard
local clipboard = nil

-- Theme
local theme = {}

-- Language
local languages = {}
local curLanguage = {}

-- Events
local event_distract = "luaide_distractionEvent"

-- Locations
--local updateURL = "https://raw.github.com/GravityScore/LuaIDE/master/luaide.lua"
local ideLocation = "/" .. shell.getRunningProgram()
local themeLocation = "/.LuaIDE-Theme"

local function isAdvanced() return term.isColor and term.isColor() end


--  -------- Utilities

local function modRead(properties)
	local w, h = term.getSize()
	local defaults = {replaceChar = nil, history = nil, visibleLength = nil, textLength = nil, 
		liveUpdates = nil, exitOnKey = nil}
	if not properties then properties = {} end
	for k, v in pairs(defaults) do if not properties[k] then properties[k] = v end end
	if properties.replaceChar then properties.replaceChar = properties.replaceChar:sub(1, 1) end
	if not properties.visibleLength then properties.visibleLength = w end

	local sx, sy = term.getCursorPos()
	local line = ""
	local pos = 0
	local historyPos = nil

	local function redraw(repl)
		local scroll = 0
		if properties.visibleLength and sx + pos > properties.visibleLength + 1 then 
			scroll = (sx + pos) - (properties.visibleLength + 1)
		end

		term.setCursorPos(sx, sy)
		local a = repl or properties.replaceChar
		if a then term.write(string.rep(a, line:len() - scroll))
		else term.write(line:sub(scroll + 1, -1)) end
		term.setCursorPos(sx + pos - scroll, sy)
	end

	local function sendLiveUpdates(event, ...)
		if type(properties.liveUpdates) == "function" then
			local ox, oy = term.getCursorPos()
			local a, data = properties.liveUpdates(line, event, ...)
			if a == true and data == nil then
				term.setCursorBlink(false)
				return line
			elseif a == true and data ~= nil then
				term.setCursorBlink(false)
				return data
			end
			term.setCursorPos(ox, oy)
		end
	end

	term.setCursorBlink(true)
	while true do
		local e, but, x, y, p4, p5 = os.pullEvent()

		if e == "char" then
			local s = false
			if properties.textLength and line:len() < properties.textLength then s = true
			elseif not properties.textLength then s = true end

			local canType = true
			if not properties.grantPrint and properties.refusePrint then
				local canTypeKeys = {}
				if type(properties.refusePrint) == "table" then
					for _, v in pairs(properties.refusePrint) do
						table.insert(canTypeKeys, tostring(v):sub(1, 1))
					end
				elseif type(properties.refusePrint) == "string" then
					for char in properties.refusePrint:gmatch(".") do
						table.insert(canTypeKeys, char)
					end
				end
				for _, v in pairs(canTypeKeys) do if but == v then canType = false end end
			elseif properties.grantPrint then
				canType = false
				local canTypeKeys = {}
				if type(properties.grantPrint) == "table" then
					for _, v in pairs(properties.grantPrint) do
						table.insert(canTypeKeys, tostring(v):sub(1, 1))
					end
				elseif type(properties.grantPrint) == "string" then
					for char in properties.grantPrint:gmatch(".") do
						table.insert(canTypeKeys, char)
					end
				end
				for _, v in pairs(canTypeKeys) do if but == v then canType = true end end
			end

			if s and canType then
				line = line:sub(1, pos) .. but .. line:sub(pos + 1, -1)
				pos = pos + 1
				redraw()
			end
		elseif e == "key" then
			if but == keys.enter then break
			elseif but == keys.left then if pos > 0 then pos = pos - 1 redraw() end
			elseif but == keys.right then if pos < line:len() then pos = pos + 1 redraw() end
			elseif (but == keys.up or but == keys.down) and properties.history then
				redraw(" ")
				if but == keys.up then
					if historyPos == nil and #properties.history > 0 then 
						historyPos = #properties.history
					elseif historyPos > 1 then 
						historyPos = historyPos - 1
					end
				elseif but == keys.down then
					if historyPos == #properties.history then historyPos = nil
					elseif historyPos ~= nil then historyPos = historyPos + 1 end
				end

				if properties.history and historyPos then
					line = properties.history[historyPos]
					pos = line:len()
				else
					line = ""
					pos = 0
				end

				redraw()
				local a = sendLiveUpdates("history")
				if a then return a end
			elseif but == keys.backspace and pos > 0 then
				redraw(" ")
				line = line:sub(1, pos - 1) .. line:sub(pos + 1, -1)
				pos = pos - 1
				redraw()
				local a = sendLiveUpdates("delete")
				if a then return a end
			elseif but == keys.home then
				pos = 0
				redraw()
			elseif but == keys.delete and pos < line:len() then
				redraw(" ")
				line = line:sub(1, pos) .. line:sub(pos + 2, -1)
				redraw()
				local a = sendLiveUpdates("delete")
				if a then return a end
			elseif but == keys["end"] then
				pos = line:len()
				redraw()
			elseif properties.exitOnKey then 
				if but == properties.exitOnKey or (properties.exitOnKey == "control" and 
						(but == 29 or but == 157)) then 
					term.setCursorBlink(false)
					return nil
				end
			end
		end
		local a = sendLiveUpdates(e, but, x, y, p4, p5)
		if a then return a end
	end

	term.setCursorBlink(false)
	if line ~= nil then line = line:gsub("^%s*(.-)%s*$", "%1") end
	return line
end


--  -------- Themes

local defaultTheme = {
	background = "gray",
	backgroundHighlight = "lightGray",
	prompt = "cyan",
	promptHighlight = "lightBlue",
	err = "red",
	errHighlight = "pink",

	editorBackground = "gray",
	editorLineHightlight = "lightBlue",
	editorLineNumbers = "gray",
	editorLineNumbersHighlight = "lightGray",
	editorError = "pink",
	editorErrorHighlight = "red",

	textColor = "white",
	conditional = "yellow",
	constant = "orange",
	["function"] = "magenta",
	string = "red",
	comment = "lime"
}

local normalTheme = {
	background = "black",
	backgroundHighlight = "black",
	prompt = "black",
	promptHighlight = "black",
	err = "black",
	errHighlight = "black",

	editorBackground = "black",
	editorLineHightlight = "black",
	editorLineNumbers = "black",
	editorLineNumbersHighlight = "white",
	editorError = "black",
	editorErrorHighlight = "black",

	textColor = "white",
	conditional = "white",
	constant = "white",
	["function"] = "white",
	string = "white",
	comment = "white"
}

--[[
local availableThemes = {
	{"Water (Default)", "https://raw.github.com/GravityScore/LuaIDE/master/themes/default.txt"},
	{"Fire", "https://raw.github.com/GravityScore/LuaIDE/master/themes/fire.txt"},
	{"Sublime Text 2", "https://raw.github.com/GravityScore/LuaIDE/master/themes/st2.txt"},
	{"Midnight", "https://raw.github.com/GravityScore/LuaIDE/master/themes/midnight.txt"},
	{"TheOriginalBIT", "https://raw.github.com/GravityScore/LuaIDE/master/themes/bit.txt"},
	{"Superaxander", "https://raw.github.com/GravityScore/LuaIDE/master/themes/superaxander.txt"},
	{"Forest", "https://raw.github.com/GravityScore/LuaIDE/master/themes/forest.txt"},
	{"Night", "https://raw.github.com/GravityScore/LuaIDE/master/themes/night.txt"},
	{"Original", "https://raw.github.com/GravityScore/LuaIDE/master/themes/original.txt"},
}
]]--

local function loadTheme(path)
	local f = io.open(path)
	local l = f:read("*l")
	local config = {}
	while l ~= nil do
		local k, v = string.match(l, "^(%a+)=(%a+)")
		if k and v then config[k] = v end
		l = f:read("*l")
	end
	f:close()
	return config
end

-- Load Theme
if isAdvanced() then theme = defaultTheme
else theme = normalTheme end


--  -------- Drawing

local function centerPrint(text, ny)
	if type(text) == "table" then for _, v in pairs(text) do centerPrint(v) end
	else
		local x, y = term.getCursorPos()
		local w, h = term.getSize()
		term.setCursorPos(w/2 - text:len()/2 + (#text % 2 == 0 and 1 or 0), ny or y)
		print(text)
	end
end

local function title(t)
	term.setTextColor(colors[theme.textColor])
	term.setBackgroundColor(colors[theme.background])
	term.clear()

	term.setBackgroundColor(colors[theme.backgroundHighlight])
	for i = 2, 4 do term.setCursorPos(1, i) term.clearLine() end
	term.setCursorPos(3, 3)
	term.write(t)
end

local function centerRead(wid, begt)
	local function liveUpdate(line, e, but, x, y, p4, p5)
		if isAdvanced() and e == "mouse_click" and x >= w/2 - wid/2 and x <= w/2 - wid/2 + 10 
				and y >= 13 and y <= 15 then
			return true, ""
		end
	end

	if not begt then begt = "" end
	term.setTextColor(colors[theme.textColor])
	term.setBackgroundColor(colors[theme.promptHighlight])
	for i = 8, 10 do
		term.setCursorPos(w/2 - wid/2, i)
		term.write(string.rep(" ", wid))
	end

	if isAdvanced() then
		term.setBackgroundColor(colors[theme.errHighlight])
		for i = 13, 15 do
			term.setCursorPos(w/2 - wid/2 + 1, i)
			term.write(string.rep(" ", 10))
		end
		term.setCursorPos(w/2 - wid/2 + 2, 14)
		term.write("> Cancel")
	end

	term.setBackgroundColor(colors[theme.promptHighlight])
	term.setCursorPos(w/2 - wid/2 + 1, 9)
	term.write("> " .. begt)
	return modRead({visibleLength = w/2 + wid/2, liveUpdates = liveUpdate})
end


--  -------- Prompt

local function prompt(list, dir, isGrid)
	local function draw(sel)
		for i, v in ipairs(list) do
			if i == sel then term.setBackgroundColor(v.highlight or colors[theme.promptHighlight])
			else term.setBackgroundColor(v.bg or colors[theme.prompt]) end
			term.setTextColor(v.tc or colors[theme.textColor])
			for i = -1, 1 do
				term.setCursorPos(v[2], v[3] + i)
				term.write(string.rep(" ", v[1]:len() + 4))
			end

			term.setCursorPos(v[2], v[3])
			if i == sel then
				term.setBackgroundColor(v.highlight or colors[theme.promptHighlight])
				term.write(" > ")
			else term.write(" - ") end
			term.write(v[1] .. " ")
		end
	end

	local key1 = dir == "horizontal" and 203 or 200
	local key2 = dir == "horizontal" and 205 or 208
	local sel = 1
	draw(sel)

	while true do
		local e, but, x, y = os.pullEvent()
		if e == "key" and but == 28 then
			return list[sel][1]
		elseif e == "key" and but == key1 and sel > 1 then
			sel = sel - 1
			draw(sel)
		elseif e == "key" and but == key2 and ((err == true and sel < #list - 1) or (sel < #list)) then
			sel = sel + 1
			draw(sel)
		elseif isGrid and e == "key" and but == 203 and sel > 2 and #list == 4 then
			sel = sel - 2
			draw(sel)
		elseif isGrid and e == "key" and but == 205 and sel < 3 and #list == 4 then
			sel = sel + 2
			draw(sel)
		elseif e == "mouse_click" then
			for i, v in ipairs(list) do
				if x >= v[2] - 1 and x <= v[2] + v[1]:len() + 3 and y >= v[3] - 1 and y <= v[3] + 1 then
					return list[i][1]
				end
			end
		end
	end
end

local function scrollingPrompt(list)
	local function draw(items, sel, loc)
		for i, v in ipairs(items) do
			local bg = colors[theme.prompt]
			local bghigh = colors[theme.promptHighlight]
			if v:find("Back") or v:find("Return") then
				bg = colors[theme.err]
				bghigh = colors[theme.errHighlight]
			end

			if i == sel then term.setBackgroundColor(bghigh)
			else term.setBackgroundColor(bg) end
			term.setTextColor(colors[theme.textColor])
			for x = -1, 1 do
				term.setCursorPos(3, (i * 4) + x + 4)
				term.write(string.rep(" ", w - 13))
			end

			term.setCursorPos(3, i * 4 + 4)
			if i == sel then
				term.setBackgroundColor(bghigh)
				term.write(" > ")
			else term.write(" - ") end
			term.write(v .. " ")
		end
	end

	local function updateDisplayList(items, loc, len)
		local ret = {}
		for i = 1, len do
			local item = items[i + loc - 1]
			if item then table.insert(ret, item) end
		end
		return ret
	end

	-- Variables
	local sel = 1
	local loc = 1
	local len = 3
	local disList = updateDisplayList(list, loc, len)
	draw(disList, sel, loc)

	-- Loop
	while true do
		local e, key, x, y = os.pullEvent()

		if e == "mouse_click" then
			for i, v in ipairs(disList) do
				if x >= 3 and x <= w - 11 and y >= i * 4 + 3 and y <= i * 4 + 5 then return v end
			end
		elseif e == "key" and key == 200 then
			if sel > 1 then
				sel = sel - 1
				draw(disList, sel, loc)
			elseif loc > 1 then
				loc = loc - 1
				disList = updateDisplayList(list, loc, len)
				draw(disList, sel, loc)
			end
		elseif e == "key" and key == 208 then
			if sel < len then
				sel = sel + 1
				draw(disList, sel, loc)
			elseif loc + len - 1 < #list then
				loc = loc + 1
				disList = updateDisplayList(list, loc, len)
				draw(disList, sel, loc)
			end
		elseif e == "mouse_scroll" then
			os.queueEvent("key", key == -1 and 200 or 208)
		elseif e == "key" and key == 28 then
			return disList[sel]
		end
	end
end

function monitorKeyboardShortcuts()
	local ta, tb = nil, nil
	local allowChar = false
	local shiftPressed = false
	while true do
		local event, char = os.pullEvent()
		if event == "key" and (char == 42 or char == 52) then
			shiftPressed = true
			tb = os.startTimer(keyboardShortcutTimeout)
		elseif event == "key" and (char == 29 or char == 157 or char == 219 or char == 220) then
			allowEditorEvent = false
			allowChar = true
			ta = os.startTimer(keyboardShortcutTimeout)
		elseif event == "key" and allowChar then
			local name = nil
			for k, v in pairs(keys) do
				if v == char then
					if shiftPressed then os.queueEvent("shortcut", "ctrl shift", k:lower())
					else os.queueEvent("shortcut", "ctrl", k:lower()) end
					sleep(0.005)
					allowEditorEvent = true
				end
			end
			if shiftPressed then os.queueEvent("shortcut", "ctrl shift", char)
			else os.queueEvent("shortcut", "ctrl", char) end
		elseif event == "timer" and char == ta then
			allowEditorEvent = true
			allowChar = false
		elseif event == "timer" and char == tb then
			shiftPressed = false
		end
	end
end


--  -------- Saving and Loading

--[[local function download(url, path)
	for i = 1, 3 do
		local response = http.get(url)
		if response then
			local data = response.readAll()
			response.close()
			if path then
				local f = io.open(path, "w")
				f:write(data)
				f:close()
			end
			return true
		end
	end

	return false
end]]

local function saveFile(path, lines)
	local dir = path:sub(1, path:len() - fs.getName(path):len())
	if not fs.exists(dir) then fs.makeDir(dir) end
	if not fs.isDir(path) and not fs.isReadOnly(path) then
		local a = ""
		for _, v in pairs(lines) do a = a .. v .. "\n" end

		local f = io.open(path, "w")
		f:write(a)
		f:close()
		return true
	else return false end
end

local function loadFile(path)
	if not fs.exists(path) then
		local dir = path:sub(1, path:len() - fs.getName(path):len())
		if not fs.exists(dir) then fs.makeDir(dir) end
		local f = io.open(path, "w")
		f:write("")
		f:close()
	end

	local l = {}
	if fs.exists(path) and not fs.isDir(path) then
		local f = io.open(path, "r")
		if f then
			local a = f:read("*l")
			while a do
				table.insert(l, a)
				a = f:read("*l")
			end
			f:close()
		end
	else return nil end

	if #l < 1 then table.insert(l, "") end
	return l
end


--  -------- Languages

languages.lua = {}
languages.brainfuck = {}
languages.none = {}

--  Lua

languages.lua.helpTips = {
	"A function you tried to call doesn't exist.",
	"You made a typo.",
	"The index of an array is nil.",
	"The wrong variable type was passed.",
	"A function/variable doesn't exist.",
	"You missed an 'end'.",
	"You missed a 'then'.",
	"You declared a variable incorrectly.",
	"One of your variables is mysteriously nil."
}

languages.lua.defaultHelpTips = {
	2, 5
}

languages.lua.errors = {
	["Attempt to call nil."] = {1, 2},
	["Attempt to index nil."] = {3, 2},
	[".+ expected, got .+"] = {4, 2, 9},
	["'end' expected"] = {6, 2},
	["'then' expected"] = {7, 2},
	["'=' expected"] = {8, 2}
}

languages.lua.keywords = {
	["and"] = "conditional",
	["break"] = "conditional",
	["do"] = "conditional",
	["else"] = "conditional",
	["elseif"] = "conditional",
	["end"] = "conditional",
	["for"] = "conditional",
	["function"] = "conditional",
	["if"] = "conditional",
	["in"] = "conditional",
	["local"] = "conditional",
	["not"] = "conditional",
	["or"] = "conditional",
	["repeat"] = "conditional",
	["return"] = "conditional",
	["then"] = "conditional",
	["until"] = "conditional",
	["while"] = "conditional",

	["true"] = "constant",
	["false"] = "constant",
	["nil"] = "constant",

	["print"] = "function",
	["write"] = "function",
	["sleep"] = "function",
	["pairs"] = "function",
	["ipairs"] = "function",
	["load"] = "function",
	["loadfile"] = "function",
	["rawset"] = "function",
	["rawget"] = "function",
}

languages.lua.parseError = function(e)
	local ret = {filename = "unknown", line = -1, display = "Unknown!", err = ""}
	if e and e ~= "" then
		ret.err = e
		if e:find(":") then
			ret.filename = e:sub(1, e:find(":") - 1):gsub("^%s*(.-)%s*$", "%1")
			-- The "" is needed to circumvent a CC bug
			e = (e:sub(e:find(":") + 1) .. ""):gsub("^%s*(.-)%s*$", "%1")
			if e:find(":") then
				ret.line = e:sub(1, e:find(":") - 1)
				e = e:sub(e:find(":") + 2):gsub("^%s*(.-)%s*$", "%1") .. ""
			end
		end
		ret.display = e:sub(1, 1):upper() .. e:sub(2, -1) .. "."
	end

	return ret
end

languages.lua.getCompilerErrors = function(code)
	code = "local function ee65da6af1cb6f63fee9a081246f2fd92b36ef2(...)\n\n" .. code .. "\n\nend"
	local fn, err = load(code)
	if not err then
		local _, e = pcall(fn)
		if e then err = e end
	end

	if err then
		local a = err:find("]", 1, true)
		if a then err = "string" .. err:sub(a + 1, -1) end
		local ret = languages.lua.parseError(err)
		if tonumber(ret.line) then ret.line = tonumber(ret.line) end
		return ret
	else return languages.lua.parseError(nil) end
end

languages.lua.run = function(path, ar)
	local fn, err = loadfile(path, _ENV)
	if not err then
		_, err = pcall(function() fn(table.unpack(ar)) end)
	end
	return err
end


--  Brainfuck

languages.brainfuck.helpTips = {
	"Well idk...",
	"Isn't this the whole point of the language?",
	"Ya know... Not being able to debug it?",
	"You made a typo."
}

languages.brainfuck.defaultHelpTips = {
	1, 2, 3
}

languages.brainfuck.errors = {
	["No matching '['"] = {1, 2, 3, 4}
}

languages.brainfuck.keywords = {}

languages.brainfuck.parseError = function(e)
	local ret = {filename = "unknown", line = -1, display = "Unknown!", err = ""}
	if e and e ~= "" then
		ret.err = e
		ret.line = e:sub(1, e:find(":") - 1)
		e = e:sub(e:find(":") + 2):gsub("^%s*(.-)%s*$", "%1") .. ""
		ret.display = e:sub(1, 1):upper() .. e:sub(2, -1) .. "."
	end

	return ret
end

languages.brainfuck.mapLoops = function(code)
	-- Map loops
	local loopLocations = {}
	local loc = 1
	local line = 1
	for let in string.gmatch(code, ".") do
		if let == "[" then
			loopLocations[loc] = true
		elseif let == "]" then
			local found = false
			for i = loc, 1, -1 do 
				if loopLocations[i] == true then
					loopLocations[i] = loc
					found = true
				end
			end

			if not found then
				return line .. ": No matching '['"
			end
		end

		if let == "\n" then line = line + 1 end
		loc = loc + 1
	end
	return loopLocations
end

languages.brainfuck.getCompilerErrors = function(code)
	local a = languages.brainfuck.mapLoops(code)
	if type(a) == "string" then return languages.brainfuck.parseError(a)
	else return languages.brainfuck.parseError(nil) end
end

languages.brainfuck.run = function(path)
	-- Read from file
	local f = io.open(path, "r")
	local content = f:read("*a")
	f:close()

	-- Define environment
	local dataCells = {}
	local dataPointer = 1
	local instructionPointer = 1

	-- Map loops
	local loopLocations = languages.brainfuck.mapLoops(content)
	if type(loopLocations) == "string" then return loopLocations end

	-- Execute code
	while true do
		local let = content:sub(instructionPointer, instructionPointer)

		if let == ">" then
			dataPointer = dataPointer + 1
			if not dataCells[tostring(dataPointer)] then dataCells[tostring(dataPointer)] = 0 end
		elseif let == "<" then
			if not dataCells[tostring(dataPointer)] then dataCells[tostring(dataPointer)] = 0 end
			dataPointer = dataPointer - 1
			if not dataCells[tostring(dataPointer)] then dataCells[tostring(dataPointer)] = 0 end
		elseif let == "+" then
			if not dataCells[tostring(dataPointer)] then dataCells[tostring(dataPointer)] = 0 end
			dataCells[tostring(dataPointer)] = dataCells[tostring(dataPointer)] + 1
		elseif let == "-" then
			if not dataCells[tostring(dataPointer)] then dataCells[tostring(dataPointer)] = 0 end
			dataCells[tostring(dataPointer)] = dataCells[tostring(dataPointer)] - 1
		elseif let == "." then
			if not dataCells[tostring(dataPointer)] then dataCells[tostring(dataPointer)] = 0 end
			if term.getCursorPos() >= w then print("") end
			write(string.char(math.max(1, dataCells[tostring(dataPointer)])))
		elseif let == "," then
			if not dataCells[tostring(dataPointer)] then dataCells[tostring(dataPointer)] = 0 end
			term.setCursorBlink(true)
			local e, but = os.pullEvent("char")
			term.setCursorBlink(false)
			dataCells[tostring(dataPointer)] = string.byte(but)
			if term.getCursorPos() >= w then print("") end
			write(but)
		elseif let == "/" then
			if not dataCells[tostring(dataPointer)] then dataCells[tostring(dataPointer)] = 0 end
			if term.getCursorPos() >= w then print("") end
			write(dataCells[tostring(dataPointer)])
		elseif let == "[" then
			if dataCells[tostring(dataPointer)] == 0 then
				for k, v in pairs(loopLocations) do
					if k == instructionPointer then instructionPointer = v end
				end
			end
		elseif let == "]" then
			for k, v in pairs(loopLocations) do
				if v == instructionPointer then instructionPointer = k - 1 end
			end
		end

		instructionPointer = instructionPointer + 1
		if instructionPointer > content:len() then print("") break end
	end
end

--  None

languages.none.helpTips = {}
languages.none.defaultHelpTips = {}
languages.none.errors = {}
languages.none.keywords = {}

languages.none.parseError = function(err)
	return {filename = "", line = -1, display = "", err = ""}
end

languages.none.getCompilerErrors = function(code)
	return languages.none.parseError(nil)
end

languages.none.run = function(path) end


-- Load language
curLanguage = languages.lua


--  -------- Run GUI

local function viewErrorHelp(e)
	title("LuaIDE - Error Help")

	local tips = nil
	for k, v in pairs(curLanguage.errors) do
		if e.display:find(k) then tips = v break end
	end

	term.setBackgroundColor(colors[theme.err])
	for i = 6, 8 do
		term.setCursorPos(5, i)
		term.write(string.rep(" ", 35))
	end

	term.setBackgroundColor(colors[theme.prompt])
	for i = 10, 18 do
		term.setCursorPos(5, i)
		term.write(string.rep(" ", 46))
	end

	if tips then
		term.setBackgroundColor(colors[theme.err])
		term.setCursorPos(6, 7)
		term.write("Error Help")

		term.setBackgroundColor(colors[theme.prompt])
		for i, v in ipairs(tips) do
			term.setCursorPos(7, i + 10)
			term.write("- " .. curLanguage.helpTips[v])
		end
	else
		term.setBackgroundColor(colors[theme.err])
		term.setCursorPos(6, 7)
		term.write("No Error Tips Available!")

		term.setBackgroundColor(colors[theme.prompt])
		term.setCursorPos(6, 11)
		term.write("There are no error tips available, but")
		term.setCursorPos(6, 12)
		term.write("you could see if it was any of these:")

		for i, v in ipairs(curLanguage.defaultHelpTips) do
			term.setCursorPos(7, i + 12)
			term.write("- " .. curLanguage.helpTips[v])
		end
	end

	prompt({{"Back", w - 8, 7}}, "horizontal")
end

local function run(path, lines, useArgs)
	local ar = {}
	if useArgs then
		title("LuaIDE - Run " .. fs.getName(path))
		local s = centerRead(w - 13, fs.getName(path) .. " ")
		for m in string.gmatch(s, "[^ \t]+") do ar[#ar + 1] = m:gsub("^%s*(.-)%s*$", "%1") end
	end
	
	saveFile(path, lines)
	term.setCursorBlink(false)
	term.setBackgroundColor(colors.black)
	term.setTextColor(colors.white)
	term.clear()
	term.setCursorPos(1, 1)
	local err = curLanguage.run(path, ar)

	term.setBackgroundColor(colors.black)
	print("\n")
	if err then
		if isAdvanced() then term.setTextColor(colors.red) end
		centerPrint("The program has crashed!")
	end
	term.setTextColor(colors.white)
	centerPrint("Press any key to return to LuaIDE...")
	while true do
		local e = os.pullEvent()
		if e == "key" then break end
	end

	-- To prevent key from showing up in editor
	os.queueEvent(event_distract)
	os.pullEvent()

	if err then
		if curLanguage == languages.lua and err:find("]") then
			err = fs.getName(path) .. err:sub(err:find("]", 1, true) + 1, -1)
		end

		while true do
			title("LuaIDE - Error!")

			term.setBackgroundColor(colors[theme.err])
			for i = 6, 8 do
				term.setCursorPos(3, i)
				term.write(string.rep(" ", w - 5))
			end
			term.setCursorPos(4, 7)
			term.write("The program has crashed!")

			term.setBackgroundColor(colors[theme.prompt])
			for i = 10, 14 do
				term.setCursorPos(3, i)
				term.write(string.rep(" ", w - 5))
			end

			local formattedErr = curLanguage.parseError(err)
			term.setCursorPos(4, 11)
			term.write("Line: " .. formattedErr.line)
			term.setCursorPos(4, 12)
			term.write("Error:")
			term.setCursorPos(5, 13)

			local a = formattedErr.display
			local b = nil
			if a:len() > w - 8 then
				for i = a:len(), 1, -1 do
					if a:sub(i, i) == " " then
						b = a:sub(i + 1, -1)
						a = a:sub(1, i)
						break
					end
				end
			end

			term.write(a)
			if b then
				term.setCursorPos(5, 14)
				term.write(b)
			end
			
			local opt = prompt({{"Error Help", w/2 - 15, 17}, {"Go To Line", w/2 + 2, 17}},
				"horizontal")
			if opt == "Error Help" then
				viewErrorHelp(formattedErr)
			elseif opt == "Go To Line" then
				-- To prevent key from showing up in editor
				os.queueEvent(event_distract)
				os.pullEvent()

				return "go to", tonumber(formattedErr.line)
			end
		end
	end
end


--  -------- Functions

local function goto()
	term.setBackgroundColor(colors[theme.backgroundHighlight])
	term.setCursorPos(2, 1)
	term.clearLine()
	term.write("Line: ")
	local line = modRead({visibleLength = w - 2})

	local num = tonumber(line)
	if num and num > 0 then return num
	else
		term.setCursorPos(2, 1)
		term.clearLine()
		term.write("Not a line number!")
		sleep(1.6)
		return nil
	end
end

local function setsyntax()
	local opts = {
		"[Lua]   Brainfuck    None ",
		" Lua   [Brainfuck]   None ",
		" Lua    Brainfuck   [None]"
	}
	local sel = 1

	term.setCursorBlink(false)
	term.setBackgroundColor(colors[theme.backgroundHighlight])
	term.setCursorPos(2, 1)
	term.clearLine()
	term.write(opts[sel])
	while true do
		local e, but, x, y = os.pullEvent("key")
		if but == 203 then
			sel = math.max(1, sel - 1)
			term.setCursorPos(2, 1)
			term.clearLine()
			term.write(opts[sel])
		elseif but == 205 then
			sel = math.min(#opts, sel + 1)
			term.setCursorPos(2, 1)
			term.clearLine()
			term.write(opts[sel])
		elseif but == 28 then
			if sel == 1 then curLanguage = languages.lua
			elseif sel == 2 then curLanguage = languages.brainfuck
			elseif sel == 3 then curLanguage = languages.none end
			term.setCursorBlink(true)
			return
		end
	end
end


--  -------- Re-Indenting

local tabWidth = 2

local comments = {}
local strings = {}

local increment = {
	"if%s+.+%s+then%s*$",
	"for%s+.+%s+do%s*$",
	"while%s+.+%s+do%s*$",
	"repeat%s*$",
	"function%s+[a-zA-Z_0-9]\(.*\)%s*$"
}

local decrement = {
	"end",
	"until%s+.+"
}

local special = {
	"else%s*$",
	"elseif%s+.+%s+then%s*$"
}

local function check(func)
	for _, v in pairs(func) do
		local cLineStart = v["lineStart"]
		local cLineEnd = v["lineEnd"]
		local cCharStart = v["charStart"]
		local cCharEnd = v["charEnd"]

		if line >= cLineStart and line <= cLineEnd then
			if line == cLineStart then return cCharStart < charNumb
			elseif line == cLineEnd then return cCharEnd > charNumb
			else return true end
		end
	end
end

local function isIn(line, loc)
	if check(comments) then return true end
	if check(strings) then return true end
	return false
end

local function setComment(ls, le, cs, ce)
	comments[#comments + 1] = {}
	comments[#comments].lineStart = ls
	comments[#comments].lineEnd = le
	comments[#comments].charStart = cs
	comments[#comments].charEnd = ce
end

local function setString(ls, le, cs, ce)
	strings[#strings + 1] = {}
	strings[#strings].lineStart = ls
	strings[#strings].lineEnd = le
	strings[#strings].charStart = cs
	strings[#strings].charEnd = ce
end

local function map(contents)
	local inCom = false
	local inStr = false

	for i = 1, #contents do
		if content[i]:find("%-%-%[%[") and not inStr and not inCom then
			local cStart = content[i]:find("%-%-%[%[")
			setComment(i, nil, cStart, nil)
			inCom = true
		elseif content[i]:find("%-%-%[=%[") and not inStr and not inCom then
			local cStart = content[i]:find("%-%-%[=%[")
			setComment(i, nil, cStart, nil)
			inCom = true
		elseif content[i]:find("%[%[") and not inStr and not inCom then
			local cStart = content[i]:find("%[%[")
			setString(i, nil, cStart, nil)
			inStr = true
		elseif content[i]:find("%[=%[") and not inStr and not inCom then
			local cStart = content[i]:find("%[=%[")
			setString(i, nil, cStart, nil)
			inStr = true
		end

		if content[i]:find("%]%]") and inStr and not inCom then
			local cStart, cEnd = content[i]:find("%]%]")
			strings[#strings].lineEnd = i
			strings[#strings].charEnd = cEnd
			inStr = false
		elseif content[i]:find("%]=%]") and inStr and not inCom then
			local cStart, cEnd = content[i]:find("%]=%]")
			strings[#strings].lineEnd = i
			strings[#strings].charEnd = cEnd
			inStr = false
		end

		if content[i]:find("%]%]") and not inStr and inCom then
			local cStart, cEnd = content[i]:find("%]%]")
			comments[#comments].lineEnd = i
			comments[#comments].charEnd = cEnd
			inCom = false
		elseif content[i]:find("%]=%]") and not inStr and inCom then
			local cStart, cEnd = content[i]:find("%]=%]")
			comments[#comments].lineEnd = i
			comments[#comments].charEnd = cEnd
			inCom = false
		end

		if content[i]:find("%-%-") and not inStr and not inCom then
			local cStart = content[i]:find("%-%-")
			setComment(i, i, cStart, -1)
		elseif content[i]:find("'") and not inStr and not inCom then
			local cStart, cEnd = content[i]:find("'")
			local nextChar = content[i]:sub(cEnd + 1, string.len(content[i]))
			local _, cEnd = nextChar:find("'")
			setString(i, i, cStart, cEnd)
		elseif content[i]:find('"') and not inStr and not inCom then
			local cStart, cEnd = content[i]:find('"')
			local nextChar = content[i]:sub(cEnd + 1, string.len(content[i]))
			local _, cEnd = nextChar:find('"')
			setString(i, i, cStart, cEnd)
		end
	end
end

local function reindent(contents)
	local err = nil
	if curLanguage ~= languages.lua then
		err = "Cannot indent languages other than Lua!"
	elseif curLanguage.getCompilerErrors(table.concat(contents, "\n")).line ~= -1 then
		err = "Cannot indent a program with errors!"
	end

	if err then
		term.setCursorBlink(false)
		term.setCursorPos(2, 1)
		term.setBackgroundColor(colors[theme.backgroundHighlight])
		term.clearLine()
		term.write(err)
		sleep(1.6)
		return contents
	end

	local new = {}
	local level = 0
	for k, v in pairs(contents) do
		local incrLevel = false
		local foundIncr = false
		for _, incr in pairs(increment) do
			if v:find(incr) and not isIn(k, v:find(incr)) then
				incrLevel = true
			end
			if v:find(incr:sub(1, -2)) and not isIn(k, v:find(incr)) then
				foundIncr = true
			end
		end

		local decrLevel = false
		if not incrLevel then
			for _, decr in pairs(decrement) do
				if v:find(decr) and not isIn(k, v:find(decr)) and not foundIncr then
					level = math.max(0, level - 1)
					decrLevel = true
				end
			end
		end

		if not decrLevel then
			for _, sp in pairs(special) do
				if v:find(sp) and not isIn(k, v:find(sp)) then
					incrLevel = true
					level = math.max(0, level - 1)
				end
			end
		end

		new[k] = string.rep(" ", level * tabWidth) .. v
		if incrLevel then level = level + 1 end
	end

	return new
end


--  -------- Menu

local menu = {
	[1] = {"File",
--		"About",
--		"Settings",
--		"",
		"New File  ^+N",
		"Open File ^+O",
		"Save File ^+S",
		"Close     ^+W",
		"Print     ^+P",
		"Quit      ^+Q"
	}, [2] = {"Edit",
		"Cut Line   ^+X",
		"Copy Line  ^+C",
		"Paste Line ^+V",
		"Delete Line",
		"Clear Line"
	}, [3] = {"Functions",
		"Go To Line    ^+G",
		"Re-Indent     ^+I",
		"Set Syntax    ^+E",
		"Start of Line ^+<",
		"End of Line   ^+>"
	}, [4] = {"Run",
		"Run Program       ^+R",
		"Run w/ Args ^+Shift+R"
	}
}

local shortcuts = {
	-- File
	["ctrl n"] = "New File  ^+N",
	["ctrl o"] = "Open File ^+O",
	["ctrl s"] = "Save File ^+S",
	["ctrl w"] = "Close     ^+W",
	["ctrl p"] = "Print     ^+P",
	["ctrl q"] = "Quit      ^+Q",

	-- Edit
	["ctrl x"] = "Cut Line   ^+X",
	["ctrl c"] = "Copy Line  ^+C",
	["ctrl v"] = "Paste Line ^+V",

	-- Functions
	["ctrl g"] = "Go To Line    ^+G",
	["ctrl i"] = "Re-Indent     ^+I",
	["ctrl e"] = "Set Syntax    ^+E",
	["ctrl 203"] = "Start of Line ^+<",
	["ctrl 205"] = "End of Line   ^+>",

	-- Run
	["ctrl r"] = "Run Program       ^+R",
	["ctrl shift r"] = "Run w/ Args ^+Shift+R"
}

local menuFunctions = {
	-- File
--	["About"] = function() end,
--	["Settings"] = function() end,
	["New File  ^+N"] = function(path, lines) saveFile(path, lines) return "new" end,
	["Open File ^+O"] = function(path, lines) saveFile(path, lines) return "open" end,
	["Save File ^+S"] = function(path, lines) saveFile(path, lines) end,
	["Close     ^+W"] = function(path, lines) saveFile(path, lines) return "menu" end,
	["Print     ^+P"] = function(path, lines) saveFile(path, lines) return nil end,
	["Quit      ^+Q"] = function(path, lines) saveFile(path, lines) return "exit" end,

	-- Edit
	["Cut Line   ^+X"] = function(path, lines, y)
		clipboard = lines[y] table.remove(lines, y) return nil, lines end,
	["Copy Line  ^+C"] = function(path, lines, y) clipboard = lines[y] end,
	["Paste Line ^+V"] = function(path, lines, y)
		if clipboard then table.insert(lines, y, clipboard) end return nil, lines end,
	["Delete Line"] = function(path, lines, y) table.remove(lines, y) return nil, lines end,
	["Clear Line"] = function(path, lines, y) lines[y] = "" return nil, lines, "cursor" end,

	-- Functions
	["Go To Line    ^+G"] = function() return nil, "go to", goto() end,
	["Re-Indent     ^+I"] = function(path, lines)
		local a = reindent(lines) saveFile(path, lines) return nil, a
	end,
	["Set Syntax    ^+E"] = function(path, lines)
		setsyntax()
		if curLanguage == languages.brainfuck and lines[1] ~= "-- Syntax: Brainfuck" then
			table.insert(lines, 1, "-- Syntax: Brainfuck")
			return nil, lines
		end
	end,
	["Start of Line ^+<"] = function() os.queueEvent("key", 199) end,
	["End of Line   ^+>"] = function() os.queueEvent("key", 207) end,

	-- Run
	["Run Program       ^+R"] = function(path, lines)
		saveFile(path, lines)
		return nil, run(path, lines, false)
	end,
	["Run w/ Args ^+Shift+R"] = function(path, lines)
		saveFile(path, lines)
		return nil, run(path, lines, true)
	end,
}

local function drawMenu(open)
	term.setCursorPos(1, 1)
	term.setTextColor(colors[theme.textColor])
	term.setBackgroundColor(colors[theme.backgroundHighlight])
	term.clearLine()
	local curX = 0
	for _, v in pairs(menu) do
		term.setCursorPos(3 + curX, 1)
		term.write(v[1])
		curX = curX + v[1]:len() + 3
	end

	if open then
		local it = {}
		local x = 1
		for _, v in pairs(menu) do
			if open == v[1] then
				it = v
				break
			end
			x = x + v[1]:len() + 3
		end
		x = x + 1

		local items = {}
		for i = 2, #it do
			table.insert(items, it[i])
		end

		local len = 1
		for _, v in pairs(items) do if v:len() + 2 > len then len = v:len() + 2 end end

		for i, v in ipairs(items) do
			term.setCursorPos(x, i + 1)
			term.write(string.rep(" ", len))
			term.setCursorPos(x + 1, i + 1)
			term.write(v)
		end
		term.setCursorPos(x, #items + 2)
		term.write(string.rep(" ", len))
		return items, len
	end
end

local function triggerMenu(cx, cy)
	-- Determine clicked menu
	local curX = 0
	local open = nil
	for _, v in pairs(menu) do
		if cx >= curX + 3 and cx <= curX + v[1]:len() + 2 then
			open = v[1]
			break
		end
		curX = curX + v[1]:len() + 3
	end
	local menux = curX + 2
	if not open then return false end

	-- Flash menu item
	term.setCursorBlink(false)
	term.setCursorPos(menux, 1)
	term.setBackgroundColor(colors[theme.background])
	term.write(string.rep(" ", open:len() + 2))
	term.setCursorPos(menux + 1, 1)
	term.write(open)
	sleep(0.1)
	local items, len = drawMenu(open)

	local ret = true

	-- Pull events on menu
	local ox, oy = term.getCursorPos()
	while type(ret) ~= "string" do
		local e, but, x, y = os.pullEvent()
		if e == "mouse_click" then
			-- If clicked outside menu
			if x < menux - 1 or x > menux + len - 1 then break
			elseif y > #items + 2 then break
			elseif y == 1 then break end

			for i, v in ipairs(items) do
				if y == i + 1 and x >= menux and x <= menux + len - 2 then
					-- Flash when clicked
					term.setCursorPos(menux, y)
					term.setBackgroundColor(colors[theme.background])
					term.write(string.rep(" ", len))
					term.setCursorPos(menux + 1, y)
					term.write(v)
					sleep(0.1)
					drawMenu(open)

					-- Return item
					ret = v
					break
				end
			end
		end
	end

	term.setCursorPos(ox, oy)
	term.setCursorBlink(true)
	return ret
end


--  -------- Editing

local standardsCompletions = {
	"if%s+.+%s+then%s*$",
	"for%s+.+%s+do%s*$",
	"while%s+.+%s+do%s*$",
	"repeat%s*$",
	"function%s+[a-zA-Z_0-9]?\(.*\)%s*$",
	"=%s*function%s*\(.*\)%s*$",
	"else%s*$",
	"elseif%s+.+%s+then%s*$"
}

local liveCompletions = {
	["("] = ")",
	["{"] = "}",
	["["] = "]",
	["\""] = "\"",
	["'"] = "'",
}

local x, y = 0, 0
local edw, edh = 0, h - 1
local offx, offy = 0, 1
local scrollx, scrolly = 0, 0
local lines = {}
local liveErr = curLanguage.parseError(nil)
local displayCode = true
local lastEventClock = os.clock()

local function attemptToHighlight(line, regex, col)
	local match = string.match(line, regex)
	if match then
		if type(col) == "number" then term.setTextColor(col)
		elseif type(col) == "function" then term.setTextColor(col(match)) end
		term.write(match)
		term.setTextColor(colors[theme.textColor])
		return line:sub(match:len() + 1, -1)
	end
	return nil
end

local function writeHighlighted(line)
	if curLanguage == languages.lua then
		while line:len() > 0 do	
			line = attemptToHighlight(line, "^%-%-%[%[.-%]%]", colors[theme.comment]) or
				attemptToHighlight(line, "^%-%-.*", colors[theme.comment]) or
				attemptToHighlight(line, "^\".*[^\\]\"", colors[theme.string]) or
				attemptToHighlight(line, "^\'.*[^\\]\'", colors[theme.string]) or
				attemptToHighlight(line, "^%[%[.-%]%]", colors[theme.string]) or
				attemptToHighlight(line, "^[%w_]+", function(match)
					if curLanguage.keywords[match] then
						return colors[theme[curLanguage.keywords[match]]]
					end
					return colors[theme.textColor]
				end) or
				attemptToHighlight(line, "^[^%w_]", colors[theme.textColor])
		end
	else term.write(line) end
end

local function draw()
	-- Menu
	term.setTextColor(colors[theme.textColor])
	term.setBackgroundColor(colors[theme.editorBackground])
	term.clear()
	drawMenu()

	-- Line numbers
	offx, offy = tostring(#lines):len() + 1, 1
	edw, edh = w - offx, h - 1

	-- Draw text
	for i = 1, edh do
		local a = lines[scrolly + i]
		if a then
			local ln = string.rep(" ", offx - 1 - tostring(scrolly + i):len()) .. tostring(scrolly + i) 
			local l = a:sub(scrollx + 1, edw + scrollx + 1)
			ln = ln .. ":"

			if liveErr.line == scrolly + i then ln = string.rep(" ", offx - 2) .. "!:" end

			term.setCursorPos(1, i + offy)
			term.setBackgroundColor(colors[theme.editorBackground])
			if scrolly + i == y then
				if scrolly + i == liveErr.line and os.clock() - lastEventClock > 3 then
					term.setBackgroundColor(colors[theme.editorErrorHighlight])
				else term.setBackgroundColor(colors[theme.editorLineHightlight]) end
				term.clearLine()
			elseif scrolly + i == liveErr.line then
				term.setBackgroundColor(colors[theme.editorError])
				term.clearLine()
			end

			term.setCursorPos(1 - scrollx + offx, i + offy)
			if scrolly + i == y then
				if scrolly + i == liveErr.line and os.clock() - lastEventClock > 3 then
					term.setBackgroundColor(colors[theme.editorErrorHighlight])
				else term.setBackgroundColor(colors[theme.editorLineHightlight]) end
			elseif scrolly + i == liveErr.line then term.setBackgroundColor(colors[theme.editorError])
			else term.setBackgroundColor(colors[theme.editorBackground]) end
			if scrolly + i == liveErr.line then
				if displayCode then term.write(a)
				else term.write(liveErr.display) end
			else writeHighlighted(a) end

			term.setCursorPos(1, i + offy)
			if scrolly + i == y then
				if scrolly + i == liveErr.line and os.clock() - lastEventClock > 3 then
					term.setBackgroundColor(colors[theme.editorError])
				else term.setBackgroundColor(colors[theme.editorLineNumbersHighlight]) end
			elseif scrolly + i == liveErr.line then
				term.setBackgroundColor(colors[theme.editorErrorHighlight])
			else term.setBackgroundColor(colors[theme.editorLineNumbers]) end
			term.write(ln)
		end
	end
	term.setCursorPos(x - scrollx + offx, y - scrolly + offy)
end

local function drawLine(...)
	local ls = {...}
	offx = tostring(#lines):len() + 1
	for _, ly in pairs(ls) do
		local a = lines[ly]
		if a then
			local ln = string.rep(" ", offx - 1 - tostring(ly):len()) .. tostring(ly) 
			local l = a:sub(scrollx + 1, edw + scrollx + 1)
			ln = ln .. ":"

			if liveErr.line == ly then ln = string.rep(" ", offx - 2) .. "!:" end

			term.setCursorPos(1, (ly - scrolly) + offy)
			term.setBackgroundColor(colors[theme.editorBackground])
			if ly == y then
				if ly == liveErr.line and os.clock() - lastEventClock > 3 then
					term.setBackgroundColor(colors[theme.editorErrorHighlight])
				else term.setBackgroundColor(colors[theme.editorLineHightlight]) end
			elseif ly == liveErr.line then
				term.setBackgroundColor(colors[theme.editorError])
			end
			term.clearLine()

			term.setCursorPos(1 - scrollx + offx, (ly - scrolly) + offy)
			if ly == y then
				if ly == liveErr.line and os.clock() - lastEventClock > 3 then
					term.setBackgroundColor(colors[theme.editorErrorHighlight])
				else term.setBackgroundColor(colors[theme.editorLineHightlight]) end
			elseif ly == liveErr.line then term.setBackgroundColor(colors[theme.editorError])
			else term.setBackgroundColor(colors[theme.editorBackground]) end
			if ly == liveErr.line then
				if displayCode then term.write(a)
				else term.write(liveErr.display) end
			else writeHighlighted(a) end

			term.setCursorPos(1, (ly - scrolly) + offy)
			if ly == y then
				if ly == liveErr.line and os.clock() - lastEventClock > 3 then
					term.setBackgroundColor(colors[theme.editorError])
				else term.setBackgroundColor(colors[theme.editorLineNumbersHighlight]) end
			elseif ly == liveErr.line then
				term.setBackgroundColor(colors[theme.editorErrorHighlight])
			else term.setBackgroundColor(colors[theme.editorLineNumbers]) end
			term.write(ln)
		end
	end
	term.setCursorPos(x - scrollx + offx, y - scrolly + offy)
end

local function cursorLoc(x, y, force)
	local sx, sy = x - scrollx, y - scrolly
	local redraw = false
	if sx < 1 then
		scrollx = x - 1
		sx = 1
		redraw = true
	elseif sx > edw then
		scrollx = x - edw
		sx = edw
		redraw = true
	end if sy < 1 then
		scrolly = y - 1
		sy = 1
		redraw = true
	elseif sy > edh then
		scrolly = y - edh
		sy = edh
		redraw = true
	end if redraw or force then draw() end
	term.setCursorPos(sx + offx, sy + offy)
end

local function executeMenuItem(a, path)
	if type(a) == "string" and menuFunctions[a] then
		local opt, nl, gtln = menuFunctions[a](path, lines, y)
		if type(opt) == "string" then term.setCursorBlink(false) return opt end
		if type(nl) == "table" then
			if #lines < 1 then table.insert(lines, "") end
			y = math.min(y, #lines)
			x = math.min(x, lines[y]:len() + 1)
			lines = nl
		elseif type(nl) == "string" then
			if nl == "go to" and gtln then
				x, y = 1, math.min(#lines, gtln)
				cursorLoc(x, y)
			end
		end
	end
	term.setCursorBlink(true)
	draw()
	term.setCursorPos(x - scrollx + offx, y - scrolly + offy)
end

local function edit(path)
	-- Variables
	x, y = 1, 1
	offx, offy = 0, 1
	scrollx, scrolly = 0, 0
	lines = loadFile(path)
	if not lines then return "menu" end

	-- Enable brainfuck
	if lines[1] == "-- Syntax: Brainfuck" then
		curLanguage = languages.brainfuck
	end

	-- Clocks
	local autosaveClock = os.clock()
	local scrollClock = os.clock() -- To prevent redraw flicker
	local liveErrorClock = os.clock()
	local hasScrolled = false

	-- Draw
	draw()
	term.setCursorPos(x + offx, y + offy)
	term.setCursorBlink(true)
	
	-- Main loop
	local tid = os.startTimer(3)
	while true do
		local e, key, cx, cy = os.pullEvent()
		if e == "key" and allowEditorEvent then
			if key == 200 and y > 1 then
				-- Up
				x, y = math.min(x, lines[y - 1]:len() + 1), y - 1
				drawLine(y, y + 1)
				cursorLoc(x, y)
			elseif key == 208 and y < #lines then
				-- Down
				x, y = math.min(x, lines[y + 1]:len() + 1), y + 1
				drawLine(y, y - 1)
				cursorLoc(x, y)
			elseif key == 203 and x > 1 then
				-- Left
				x = x - 1
				local force = false
				if y - scrolly + offy < offy + 1 then force = true end
				cursorLoc(x, y, force)
			elseif key == 205 and x < lines[y]:len() + 1 then
				-- Right
				x = x + 1
				local force = false
				if y - scrolly + offy < offy + 1 then force = true end
				cursorLoc(x, y, force)
			elseif (key == 28 or key == 156) and (displayCode and true or y + scrolly - 1 ==
					liveErr.line) then
				-- Enter
				local f = nil
				for _, v in pairs(standardsCompletions) do
					if lines[y]:find(v) then f = v end
				end

				local _, spaces = lines[y]:find("^[ ]+")
				if not spaces then spaces = 0 end
				if f then
					table.insert(lines, y + 1, string.rep(" ", spaces + 2))
					if not f:find("else", 1, true) and not f:find("elseif", 1, true) then
						table.insert(lines, y + 2, string.rep(" ", spaces) .. 
							(f:find("repeat", 1, true) and "until " or f:find("{", 1, true) and "}" or 
							"end"))
					end
					x, y = spaces + 3, y + 1
					cursorLoc(x, y, true)
				else
					local oldLine = lines[y]

					lines[y] = lines[y]:sub(1, x - 1)
					table.insert(lines, y + 1, string.rep(" ", spaces) .. oldLine:sub(x, -1))

					x, y = spaces + 1, y + 1
					cursorLoc(x, y, true)
				end
			elseif key == 14 and (displayCode and true or y + scrolly - 1 == liveErr.line) then
				-- Backspace
				if x > 1 then
					local f = false
					for k, v in pairs(liveCompletions) do
						if lines[y]:sub(x - 1, x - 1) == k then f = true end
					end

					lines[y] = lines[y]:sub(1, x - 2) .. lines[y]:sub(x + (f and 1 or 0), -1)
					drawLine(y)
					x = x - 1
					cursorLoc(x, y)
				elseif y > 1 then
					local prevLen = lines[y - 1]:len() + 1
					lines[y - 1] = lines[y - 1] .. lines[y]
					table.remove(lines, y)
					x, y = prevLen, y - 1
					cursorLoc(x, y, true)
				end
			elseif key == 199 then
				-- Home
				x = 1
				local force = false
				if y - scrolly + offy < offy + 1 then force = true end
				cursorLoc(x, y, force)
			elseif key == 207 then
				-- End
				x = lines[y]:len() + 1
				local force = false
				if y - scrolly + offy < offy + 1 then force = true end
				cursorLoc(x, y, force)
			elseif key == 211 and (displayCode and true or y + scrolly - 1 == liveErr.line) then
				-- Forward Delete
				if x < lines[y]:len() + 1 then
					lines[y] = lines[y]:sub(1, x - 1) .. lines[y]:sub(x + 1)
					local force = false
					if y - scrolly + offy < offy + 1 then force = true end
					drawLine(y)
					cursorLoc(x, y, force)
				elseif y < #lines then
					lines[y] = lines[y] .. lines[y + 1]
					table.remove(lines, y + 1)
					draw()
					cursorLoc(x, y)
				end
			elseif key == 15 and (displayCode and true or y + scrolly - 1 == liveErr.line) then
				-- Tab
				lines[y] = string.rep(" ", tabWidth) .. lines[y]
				x = x + 2
				local force = false
				if y - scrolly + offy < offy + 1 then force = true end
				drawLine(y)
				cursorLoc(x, y, force)
			elseif key == 201 then
				-- Page up
				y = math.min(math.max(y - edh, 1), #lines)
				x = math.min(lines[y]:len() + 1, x)
				cursorLoc(x, y, true)
			elseif key == 209 then
				-- Page down
				y = math.min(math.max(y + edh, 1), #lines)
				x = math.min(lines[y]:len() + 1, x)
				cursorLoc(x, y, true)
			end
		elseif e == "char" and allowEditorEvent and (displayCode and true or 
				y + scrolly - 1 == liveErr.line) then
			local shouldIgnore = false
			for k, v in pairs(liveCompletions) do
				if key == v and lines[y]:find(k, 1, true) and lines[y]:sub(x, x) == v then
					shouldIgnore = true
				end
			end

			local addOne = false
			if not shouldIgnore then
				for k, v in pairs(liveCompletions) do
					if key == k and lines[y]:sub(x, x) ~= k then key = key .. v addOne = true end
				end
				lines[y] = lines[y]:sub(1, x - 1) .. key .. lines[y]:sub(x, -1)
			end

			x = x + (addOne and 1 or key:len())
			local force = false
			if y - scrolly + offy < offy + 1 then force = true end
			drawLine(y)
			cursorLoc(x, y, force)
		elseif e == "mouse_click" and key == 1 then
			if cy > 1 then
				if cx <= offx and cy - offy == liveErr.line - scrolly then
					displayCode = not displayCode
					drawLine(liveErr.line)
				else
					local oldy = y
					y = math.min(math.max(scrolly + cy - offy, 1), #lines)
					x = math.min(math.max(scrollx + cx - offx, 1), lines[y]:len() + 1)
					if oldy ~= y then drawLine(oldy, y) end
					cursorLoc(x, y)
				end
			else
				local a = triggerMenu(cx, cy)
				if a then
					local opt = executeMenuItem(a, path)
					if opt then return opt end
				end
			end
		elseif e == "shortcut" then
			local a = shortcuts[key .. " " .. cx]
			if a then
				local parent = nil
				local curx = 0
				for i, mv in ipairs(menu) do
					for _, iv in pairs(mv) do
						if iv == a then
							parent = menu[i][1]
							break
						end
					end
					if parent then break end
					curx = curx + mv[1]:len() + 3
				end
				local menux = curx + 2

				-- Flash menu item
				term.setCursorBlink(false)
				term.setCursorPos(menux, 1)
				term.setBackgroundColor(colors[theme.background])
				term.write(string.rep(" ", parent:len() + 2))
				term.setCursorPos(menux + 1, 1)
				term.write(parent)
				sleep(0.1)
				drawMenu()

				-- Execute item
				local opt = executeMenuItem(a, path)
				if opt then return opt end
			end
		elseif e == "mouse_scroll" then
			if key == -1 and scrolly > 0 then
				scrolly = scrolly - 1
				if os.clock() - scrollClock > 0.0005 then
					draw()
					term.setCursorPos(x - scrollx + offx, y - scrolly + offy)
				end
				scrollClock = os.clock()
				hasScrolled = true
			elseif key == 1 and scrolly < #lines - edh then
				scrolly = scrolly + 1
				if os.clock() - scrollClock > 0.0005 then
					draw()
					term.setCursorPos(x - scrollx + offx, y - scrolly + offy)
				end
				scrollClock = os.clock()
				hasScrolled = true
			end
		elseif e == "timer" and key == tid then
			drawLine(y)
			tid = os.startTimer(3)
		end

		-- Draw
		if hasScrolled and os.clock() - scrollClock > 0.1 then
			draw()
			term.setCursorPos(x - scrollx + offx, y - scrolly + offy)
			hasScrolled = false
		end

		-- Autosave
		if os.clock() - autosaveClock > autosaveInterval then
			saveFile(path, lines)
			autosaveClock = os.clock()
		end

		-- Errors
		if os.clock() - liveErrorClock > 1 then
			local prevLiveErr = liveErr
			liveErr = curLanguage.parseError(nil)
			local code = ""
			for _, v in pairs(lines) do code = code .. v .. "\n" end

			liveErr = curLanguage.getCompilerErrors(code)
			liveErr.line = math.min(liveErr.line - 2, #lines)
			if liveErr ~= prevLiveErr then draw() end
			liveErrorClock = os.clock()
		end
	end

	return "menu"
end


--  -------- Open File

local function newFile()
	local wid = w - 13

	-- Get name
	title("Lua IDE - New File")
	local name = centerRead(wid, "/")
	if not name or name == "" then return "menu" end
	name = "/" .. name

	-- Clear
	title("Lua IDE - New File")
	term.setTextColor(colors[theme.textColor])
	term.setBackgroundColor(colors[theme.promptHighlight])
	for i = 8, 10 do
		term.setCursorPos(w/2 - wid/2, i)
		term.write(string.rep(" ", wid))
	end
	term.setCursorPos(1, 9)
	if fs.isDir(name) then
		centerPrint("Cannot Edit a Directory!")
		sleep(1.6)
		return "menu"
	elseif fs.exists(name) then
		centerPrint("File Already Exists!")
		local opt = prompt({{"Open", w/2 - 9, 14}, {"Cancel", w/2 + 2, 14}}, "horizontal")
		if opt == "Open" then return "edit", name
		elseif opt == "Cancel" then return "menu" end
	else return "edit", name end
end

local function openFile()
	local wid = w - 13

	-- Get name
	title("Lua IDE - Open File")
	local name = centerRead(wid, "/")
	if not name or name == "" then return "menu" end
	name = "/" .. name

	-- Clear
	title("Lua IDE - New File")
	term.setTextColor(colors[theme.textColor])
	term.setBackgroundColor(colors[theme.promptHighlight])
	for i = 8, 10 do
		term.setCursorPos(w/2 - wid/2, i)
		term.write(string.rep(" ", wid))
	end
	term.setCursorPos(1, 9)
	if fs.isDir(name) then
		centerPrint("Cannot Open a Directory!")
		sleep(1.6)
		return "menu"
	elseif not fs.exists(name) then
		centerPrint("File Doesn't Exist!")
		local opt = prompt({{"Create", w/2 - 11, 14}, {"Cancel", w/2 + 2, 14}}, "horizontal")
		if opt == "Create" then return "edit", name
		elseif opt == "Cancel" then return "menu" end
	else return "edit", name end
end


--  -------- Settings

local function update()
--[[
	local function draw(status)
		title("LuaIDE - Update")
		term.setBackgroundColor(colors[theme.prompt])
		term.setTextColor(colors[theme.textColor])
		for i = 8, 10 do
			term.setCursorPos(w/2 - (status:len() + 4), i)
			write(string.rep(" ", status:len() + 4))
		end
		term.setCursorPos(w/2 - (status:len() + 4), 9)
		term.write(" - " .. status .. " ")

		term.setBackgroundColor(colors[theme.errHighlight])
		for i = 8, 10 do
			term.setCursorPos(w/2 + 2, i)
			term.write(string.rep(" ", 10))
		end
		term.setCursorPos(w/2 + 2, 9)
		term.write(" > Cancel ")
	end

	if not http then
		draw("HTTP API Disabled!")
		sleep(1.6)
		return "settings"
	end

	draw("Updating...")
	local tID = os.startTimer(10)
	http.request(updateURL)
	while true do
		local e, but, x, y = os.pullEvent()
		if (e == "key" and but == 28) or
				(e == "mouse_click" and x >= w/2 + 2 and x <= w/2 + 12 and y == 9) then
			draw("Cancelled")
			sleep(1.6)
			break
		elseif e == "http_success" and but == updateURL then
			local new = x.readAll()
			local curf = io.open(ideLocation, "r")
			local cur = curf:read("*a")
			curf:close()

			if cur ~= new then
				draw("Update Found")
				sleep(1.6)
				local f = io.open(ideLocation, "w")
				f:write(new)
				f:close()

				draw("Click to Exit")
				while true do
					local e = os.pullEvent()
					if e == "mouse_click" or (not isAdvanced() and e == "key") then break end
				end
				return "exit"
			else
				draw("No Updates Found!")
				sleep(1.6)
				break
			end
		elseif e == "http_failure" or (e == "timer" and but == tID) then
			draw("Update Failed!")
			sleep(1.6)
			break
		end
	end
]]--

	return "settings"
end

local function changeTheme()
	title("LuaIDE - Theme")
	term.setCursorPos(1, 7)
	centerPrint("Themes are not available on the")
	centerPrint("treasure disk version of LuaIDE!")
	centerPrint("Download the full program from the")
	centerPrint("ComputerCraft Forums!")

--[[
	if isAdvanced() then
		local disThemes = {"Back"}
		for _, v in pairs(availableThemes) do table.insert(disThemes, v[1]) end
		local t = scrollingPrompt(disThemes)
		local url = nil
		for _, v in pairs(availableThemes) do if v[1] == t then url = v[2] end end

		if not url then return "settings" end
		if t == "Dawn (Default)" then
			term.setBackgroundColor(colors[theme.backgroundHighlight])
			term.setCursorPos(3, 3)
			term.clearLine()
			term.write("LuaIDE - Loaded Theme!")
			sleep(1.6)

			fs.delete(themeLocation)
			theme = defaultTheme
			return "menu"
		end

		term.setBackgroundColor(colors[theme.backgroundHighlight])
		term.setCursorPos(3, 3)
		term.clearLine()
		term.write("LuaIDE - Downloading...")

		fs.delete("/.LuaIDE_temp_theme_file")
		download(url, "/.LuaIDE_temp_theme_file")
		local a = loadTheme("/.LuaIDE_temp_theme_file")

		term.setCursorPos(3, 3)
		term.clearLine()
		if a then
			term.write("LuaIDE - Loaded Theme!")
			fs.delete(themeLocation)
			fs.move("/.LuaIDE_temp_theme_file", themeLocation)
			theme = a
			sleep(1.6)
			return "menu"
		end
		
		term.write("LuaIDE - Could Not Load Theme!")
		fs.delete("/.LuaIDE_temp_theme_file")
		sleep(1.6)
		return "settings"
	else
		term.setCursorPos(1, 8)
		centerPrint("Themes are not available on")
		centerPrint("normal computers!")
	end
]]--
end

local function settings()
	title("LuaIDE - Settings")

	local opt = prompt({{"Change Theme", w/2 - 17, 8}, {"Return to Menu", w/2 - 19, 13},
		--[[{"Check for Updates", w/2 + 2, 8},]] {"Exit IDE", w/2 + 2, 13, bg = colors[theme.err], 
		highlight = colors[theme.errHighlight]}}, "vertical", true)
	if opt == "Change Theme" then return changeTheme()
--	elseif opt == "Check for Updates" then return update()
	elseif opt == "Return to Menu" then return "menu"
	elseif opt == "Exit IDE" then return "exit" end
end


--  -------- Menu

local function menu()
	title("Welcome to LuaIDE " .. version)

	local opt = prompt({{"New File", w/2 - 13, 8}, {"Open File", w/2 - 14, 13},
		{"Settings", w/2 + 2, 8}, {"Exit IDE", w/2 + 2, 13, bg = colors[theme.err],
		highlight = colors[theme.errHighlight]}}, "vertical", true)
	if opt == "New File" then return "new"
	elseif opt == "Open File" then return "open"
	elseif opt == "Settings" then return "settings"
	elseif opt == "Exit IDE" then return "exit" end
end


--  -------- Main

local function main(arguments)
	local opt, data = "menu", nil

	-- Check arguments
	if type(arguments) == "table" and #arguments > 0 then
		local f = "/" .. shell.resolve(arguments[1])
		if fs.isDir(f) then print("Cannot edit a directory.") end
		opt, data = "edit", f
	end

	-- Main run loop
	while true do
		-- Menu
		if opt == "menu" then opt = menu() end

		-- Other
		if opt == "new" then opt, data = newFile()
		elseif opt == "open" then opt, data = openFile()
		elseif opt == "settings" then opt = settings()
		end if opt == "exit" then break end

		-- Edit
		if opt == "edit" and data then opt = edit(data) end
	end
end

-- Load Theme
if fs.exists(themeLocation) then theme = loadTheme(themeLocation) end
if not theme and isAdvanced() then theme = defaultTheme
elseif not theme then theme = normalTheme end

-- Run
local _, err = pcall(function()
	parallel.waitForAny(function() main(args) end, monitorKeyboardShortcuts)
end)

-- Catch errors
if err and not err:find("Terminated") then
	term.setCursorBlink(false)
	title("LuaIDE - Crash! D:")

	term.setBackgroundColor(colors[theme.err])
	for i = 6, 8 do
		term.setCursorPos(5, i)
		term.write(string.rep(" ", 36))
	end
	term.setCursorPos(6, 7)
	term.write("LuaIDE Has Crashed! D:")

	term.setBackgroundColor(colors[theme.background])
	term.setCursorPos(2, 10)
	print(err)

	term.setBackgroundColor(colors[theme.prompt])
	local _, cy = term.getCursorPos()
	for i = cy + 1, cy + 4 do
		term.setCursorPos(5, i)
		term.write(string.rep(" ", 36))
	end
	term.setCursorPos(6, cy + 2)
	term.write("Please report this error to")
	term.setCursorPos(6, cy + 3)
	term.write("GravityScore! ")
	
	term.setBackgroundColor(colors[theme.background])
	if isAdvanced() then centerPrint("Click to Exit...", h - 1)
	else centerPrint("Press Any Key to Exit...", h - 1) end
	while true do
		local e = os.pullEvent()
		if e == "mouse_click" or (not isAdvanced() and e == "key") then break end
	end

	-- Prevent key from being shown
	os.queueEvent(event_distract)
	os.pullEvent()
end

-- Exit
term.setBackgroundColor(colors.black)
term.setTextColor(colors.white)
term.clear()
term.setCursorPos(1, 1)
centerPrint("Thank You for Using Lua IDE " .. version)
centerPrint("Made by GravityScore")
