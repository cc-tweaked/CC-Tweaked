-- Get file to edit.
local tArgs = { ... }
if #tArgs == 0 then
	print( "Usage: edit <path> [line number]" )
	return
end

-- Error checking.
local sPath = shell.resolve( tArgs[1] )
local bReadOnly = fs.isReadOnly( sPath )
if fs.exists( sPath ) and fs.isDir( sPath ) then
	print( sPath.." cannot be opened because it is a directory." )
	return
end
if not fs.exists( sPath ) and bReadOnly then
	print( sPath.." cannot be opened because the path is read only and the file does not exist." )
	return
end

-- Set up some variables
local showLineNumberSidebar = true
local x, y = 1, 1
local shiftX = 0

if type(tonumber(tArgs[2])) == "number" then
	y = math.floor(math.max(tArgs[2], 1))
end
local w, h = term.getSize()
local scrollX, scrollY = 0, y - 1

local tLines = {}
local bRunning = true

-- Code Highlighting Colors.
local highlightColor, keywordColor, commentColor, textColor, bgColor, menuBgColor
if term.isColor() then
	menuBgColor = colors.gray
	bgColor = colors.black
	textColor = colors.white
	highlightColor = colors.yellow
	keywordColor = colors.blue
	commentColor = colors.green
	stringColor = colors.red
else
	menuBgColor = colors.black
	bgColor = colors.black
	textColor = colors.white
	highlightColor = colors.white
	keywordColor = colors.white
	commentColor = colors.white
	stringColor = colors.white
end

-- Menus.
local bMenu = false
local nMenuItem = 1
local tMenuItems = { "Save As...", "Exit", "Go To", "Test", "Find", "Replace", "Add Bookmark", "Bookmarks" }
if not bReadOnly then
	table.insert( tMenuItems, 1, "Save" )
end
if peripheral.find( "printer" ) then
	table.insert( tMenuItems, "Print" )
end

local tSplashStatus = {
	"Press Ctrl to access menu.",
	"Press Ctrl to access the extended menu, with more options than the default Editor included with ComputerCraft.",
	"One of the most important things when creating scripts is to test them. Use the Test function to test your script.",
	"If a script runs into an unprotected error the status window will tell you what error it is.",
	"To add a bookmark, use the Add Bookmark function.",
	"Adding an Editor Bookmark will cause the built in Autocomplete feature to automatically include that line in the list of lines to show.",
	"Bookmarks can be accessed by clicking on the Bookmark button.",
	"To quickly jump to the next line with a certain string in it, use the Find function.",
	"To quickly jump to any line in the script, use the Go To function.",
	"When attempting to edit a read-only file, use the Save As... function to start editing a new file.",
	"The status bar shows the results of an action you made.",
}

local sStatus = tSplashStatus[math.random(#tSplashStatus)]
local nStatusScroll = -1

-- Clears the terminal screen.
local function resetTerm()
	term.setBackgroundColor(colors.black)
	term.setTextColor(colors.white)
	term.setCursorPos(1, 1)
	term.clear()
end

local errors = {
	["	"] = "   ",
	["—"] = "-",
}
-- Gets rid of errors in the editing window.
local function repairString(myString)
	local curString = myString
	pcall(function()
		local replaceStart, replaceEnd
		for incorrectText, correctCharacter in pairs(errors) do
			while curString:find(incorrectText, 1, true) do
				replaceStart, replaceEnd = curString:find(incorrectText, 1, true)
				curString = curString:sub(1, replaceStart - 1)..correctCharacter..curString:sub(replaceEnd + 1)
			end
			if curString:sub(-#incorrectText) == incorrectText then
				curString = curString:sub(1, -#incorrectText)
			end
		end
	end)
	return curString
end

-- Updates the sidebar's width.
local function updateSidebar()
	-- If line numbers are enabled then fix the shiftX variable
	if showLineNumberSidebar then
		local curPower = #tLines
		local power = 1
		while curPower >= 10 do
			curPower = curPower/10
			power = power + 1
		end
		shiftX = power + 2
	else
		shiftX = 0
	end
end

-- Attempts to load the selected file.
local function load( _sPath )
	tLines = {}
	if fs.exists( _sPath ) then
		local file = io.open( _sPath, "r" )
		local sLine = file:read()
		while sLine do
			table.insert( tLines, repairString(sLine) )
			sLine = file:read()
		end
		file:close()
	end

	if #tLines == 0 then
		table.insert( tLines, "" )
	end
end

-- Attempts to save to the selected path.
local function save( _sPath )
	-- Create intervening folder
	local sDir = sPath:sub(1, sPath:len() - fs.getName(sPath):len() )
	if not fs.exists( sDir ) then
		fs.makeDir( sDir )
	end

	-- Save
	local file = nil
	local function innerSave()
		file = fs.open( _sPath, "w" )
		if file then
			for n, sLine in ipairs( tLines ) do
				file.write( sLine .. "\n" )
			end
		else
			error( "Failed to open ".._sPath )
		end
	end

	local ok = pcall( innerSave )
	if file then
		file.close()
	end
	return ok
end

-- Lua keywords
local tKeywords = {
	["and"] = true,
	["break"] = true,
	["do"] = true,
	["else"] = true,
	["elseif"] = true,
	["end"] = true,
	["false"] = true,
	["for"] = true,
	["function"] = true,
	["if"] = true,
	["in"] = true,
	["local"] = true,
	["nil"] = true,
	["not"] = true,
	["or"] = true,
	["repeat"] = true,
	["return"] = true,
	["then"] = true,
	["true"] = true,
	["until"] = true,
	["while"] = true,
}

local function tryWrite( sLine, regex, color )
	local match = string.match( sLine, regex )
	if match then
		if type(color) == "number" then
			term.setTextColor( color )
		else
			term.setTextColor( color(match) )
		end
		term.write( match )
		term.setTextColor( textColor )
		return string.sub( sLine, string.len(match) + 1 )
	end
	return nil
end

local function writeHighlighted( sLine )
	while string.len(sLine) > 0 do
		sLine =
			tryWrite( sLine, "^%-%-%[%[.-%]%]", commentColor ) or
			tryWrite( sLine, "^%-%-.*", commentColor ) or
			tryWrite( sLine, "^\".-[^\\]\"", stringColor ) or
			tryWrite( sLine, "^\'.-[^\\]\'", stringColor ) or
			tryWrite( sLine, "^%[%[.-%]%]", stringColor ) or
			tryWrite( sLine, "^[%w_]+", function( match )
				if tKeywords[ match ] then
					return keywordColor
				end
				return textColor
			end ) or
			tryWrite( sLine, "^[^%w_]", textColor )
	end
end

local tCompletions
local nCompletion

local tCompleteEnv = _ENV
local function complete( sLine )
	local nStartPos = string.find( sLine, "[a-zA-Z0-9_%.]+$" )
	if nStartPos then
		sLine = string.sub( sLine, nStartPos )
	end
	if #sLine > 0 then
		return textutils.complete( sLine, tCompleteEnv )
	end
	return nil
end

local function recomplete()
	local sLine = tLines[y]
	if not bMenu and not bReadOnly and x == string.len(sLine) + 1 then
		tCompletions = complete( sLine )
		if tCompletions and #tCompletions > 0 then
			nCompletion = 1
		else
			nCompletion = nil
		end
	else
		tCompletions = nil
		nCompletion = nil
	end
end

local function eliminatePoint(num)
	local str = tostring(num)
	if str:find(".") then
		return str:sub(1, -1)
	end
	return str
end

local function writeCompletion( sLine )
	if nCompletion then
		local sCompletion = tCompletions[ nCompletion ]
		term.setTextColor( colors.white )
		term.setBackgroundColor( colors.gray )
		term.write( sCompletion )
		term.setTextColor( textColor )
		term.setBackgroundColor( bgColor )
	end
end

local function redrawText()
	updateSidebar()
	local cursorX, cursorY = x, y
	local sLine
	for y=1,h-1 do
		term.setCursorPos( 1 + (shiftX) - scrollX, y )
		term.clearLine()

		sLine = tLines[ y + scrollY ]
		if sLine ~= nil then
			writeHighlighted( sLine )
			if cursorY == y and cursorX == #sLine + 1 then
				writeCompletion()
			end
		end
		term.setCursorPos( 1, y )
		term.setBackgroundColor( menuBgColor )
		term.write( string.rep(" ", shiftX - 1) )
		term.setCursorPos( shiftX - (1 + #eliminatePoint(y + scrollY)), y )
		if y + scrollY <= #tLines then
			term.write( eliminatePoint(y + scrollY ))
		end
		term.setBackgroundColor( bgColor )
	end
	term.setCursorPos( x + (shiftX) - scrollX, y - scrollY )
end

local function redrawLine(_nY)
	updateSidebar()
	local sLine = tLines[_nY]
	if sLine then
		local y = _nY - scrollY
		term.setCursorPos( 1 + (shiftX) - scrollX, y )
		if shiftX <= 0 then
			term.clearLine()
		else
			term.write( string.rep(" ", w) )
			term.setCursorPos( 1 + (shiftX) - scrollX, y )
		end
		writeHighlighted( sLine )
		if _nY == y and x == #sLine + 1 then
			writeCompletion()
		end

		-- Attempt to write to the sidebar.
		term.setCursorPos( 1, y )
		term.setBackgroundColor( menuBgColor )
		term.write( string.rep( " ", shiftX - 1 ) )
		term.setCursorPos( shiftX - 1 - #eliminatePoint(y + scrollY), y )
		if y + scrollY <= #tLines then
			term.write( eliminatePoint(y + scrollY) )
		end

		-- Write the completion.
		term.setCursorPos( 1 + (shiftX + #sLine) - scrollX, y )
		writeCompletion()
		term.setBackgroundColor( bgColor )
		term.setCursorPos( x + (shiftX) - scrollX, _nY - scrollY )
	end
end

local function getScroll(value)
	if value%4 < 1 then
		return value%4
	elseif value%4 >= 1 and value%4 <= 2 then
		return 1
	elseif value%4 <= 3 then
		return math.max(3-(value%4), 0)
	elseif value%4 > 3 then
		return 0.01
	end
end

-- Redraws the menu.
local function redrawMenu()
	updateSidebar()

	-- Clear line
	term.setCursorPos( 1, h )
	term.setBackgroundColor( menuBgColor )
	term.clearLine()

	term.setCursorPos( 1, h )
	if bMenu then
		local startingXPos = 1
		local totalWidthOfOptions = 0
		for nItem, sItem in pairs( tMenuItems ) do
			if nItem >= nMenuItem - ((w > 33 and 1) or 0) then
				if startingXPos < 1 then
					startingXPos = startingXPos + 1
				end
				break
			end
			startingXPos = startingXPos - (#sItem + 2)
		end
		term.setCursorPos( startingXPos, h )
		-- Draw menu
		term.setTextColor( textColor )
		for nItem, sItem in pairs( tMenuItems ) do
			if nItem == nMenuItem then
				term.setTextColor( highlightColor )
				term.write( "[" )
				term.setTextColor( textColor )
				term.write( sItem )
				term.setTextColor( highlightColor )
				term.write( "]" )
				term.setTextColor( textColor )
			else
				term.write( " "..sItem.." " )
			end
			totalWidthOfOptions = totalWidthOfOptions + (#sItem + 2)
		end

		-- Draw indicators that there are more options available.
		if startingXPos < 1 then
			term.setCursorPos( 1, h )
			term.write("<")
		end

		w, h = term.getSize()
		if -startingXPos + w < totalWidthOfOptions then
			term.setCursorPos( w, h )
			term.write(">")
		end
	else
		local scrollWidth = math.max((#sStatus - (w - 2)) + string.len( "Ln "..y ), 0)
		nStatusScroll = nStatusScroll + math.max(.02, (w/math.max(scrollWidth, 1))/50)
		local statusScroll = getScroll(nStatusScroll)
		term.setCursorPos( 1, h )
		-- Draw status
		term.setTextColor( highlightColor )
		term.write( sStatus:sub( statusScroll*scrollWidth ) )
		term.setTextColor( textColor )
		-- Draw line numbers
		term.setCursorPos( w - string.len( "Ln "..y ), h )
		term.setTextColor( highlightColor )
		term.write( " Ln " )
		term.setTextColor( textColor )
		term.write( y )
	end

	term.setBackgroundColor( bgColor )
	-- Reset cursor
	term.setCursorPos( x + (shiftX) - scrollX, y - scrollY )
end

-- Gets a response from the user.
local function getUserInput(message, ...)
	term.setCursorPos(1, h)
	term.setTextColor(textColor)
	term.setBackgroundColor(menuBgColor)
	term.clearLine()

	term.write(message)
	local input = read(...)

	term.setBackgroundColor(bgColor)
	redrawText()
	redrawMenu()
	return input
end

-- A list of menu functions.
local tMenuFuncs = {
	Save=function()
		if bReadOnly then
			sStatus = "That location is read only."
		else
			local ok, err = save( sPath )
			if ok then
				sStatus = "Saved to "..sPath.."."
			else
				sStatus = "Error saving to "..sPath.."."
			end
		end
		redrawMenu()
	end,
	Test=function()
		-- Convert the table into a string that we can load
		local scriptCode = tLines[1] or ""
		for k, v in ipairs(tLines) do
			if k > 1 then
				scriptCode = scriptCode.."\n"..v
			end
		end
		-- Run the script
		local fScript, err = loadstring(scriptCode, sPath or "Script")
		if fScript then
			resetTerm()
			local args = {pcall(fScript)}
			resetTerm()
			local ok, msg = args[1], args[2]
			table.remove(args, 1)
			if ok then
				if #args > 0 then
					for key, value in ipairs(args) do
						args[key] = tostring(value)
					end
					sStatus = "Script returned "..table.concat(args, ", ").."."
				else
					sStatus = "Script ran without errors."
				end
			else
				-- Print the error
				local c1 = (msg:find(']:', 0, true) or 0) + 1
				sStatus = "Error running script: "..msg:sub(c1)
			end
		else
			if err then
				local c1 = err:find(']:', 0, true) + 1
				sStatus = "Error loading script: "..(sPath or "Script")..err:sub(c1)
			else
				sStatus = "Error loading script."
			end
		end
		redrawText()
	end,
	Find=function()
		-- Ask for user input
		local sSearch = getUserInput("Find what>")

		sStatus = "Searching..."
		redrawMenu()

		-- Search for the matches
		local matchx, matchy
		local firstmatchx, firstmatchy

		-- Find a match.
		for linenumber, v in ipairs(tLines) do
			if linenumber > y then
				if string.find(v, sSearch) then
					matchx, matchy = string.find(v, sSearch), linenumber
					break
				end
			elseif not firstmatchx then
				if string.find(v, sSearch) then
					firstmatchx, firstmatchy = string.find(v, sSearch), linenumber
				end
			end
		end

		-- Move the cursor if we found a match and update the status text.
		if matchx or firstmatchx then
			x, y = matchx or firstmatchx, matchy or firstmatchy
			scrollX, scrollY = math.max(x - 16, 0), y - 1
			sStatus = "Found match at line "..(matchy or firstmatchy).."."
		else
			sStatus = "No matches found for '"..sSearch.."'."
		end

		redrawText()
	end,
	Replace=function()
		-- Ask for user input
		local sSearch = getUserInput("Find what>")
		if sSearch == "" then return false end
		local sReplace = getUserInput("Replace with>")

		sStatus = "Searching..."
		redrawMenu()

		-- Replace all instances of sSearch with sReplace.
		local matchx, matchy
		local matches = 0
		for lineNumber, v in ipairs(tLines) do
			while string.find(tLines[lineNumber], sSearch) do
				matches = matches + 1
				matchx, matchy = string.find(tLines[lineNumber], sSearch)
				tLines[lineNumber] = string.sub(tLines[lineNumber], 1, matchx - 1)..sReplace..string.sub(tLines[lineNumber], matchy + 1)
			end
		end

		-- Update the status text.
		if matchx then
			sStatus = "Replaced "..matches..((matches == 1 and " instance") or " instances").." with "..sReplace.."."
		else
			sStatus = "No matches found for '"..sSearch.."'."
		end

		redrawText()
	end,
	Print=function()
		local printer = peripheral.find( "printer" )
		if not printer then
			sStatus = "No printer attached."
			return
		end

		local nPage = 0
		local sName = fs.getName( sPath )
		if printer.getInkLevel() < 1 then
			sStatus = "Printer out of ink."
			return
		elseif printer.getPaperLevel() < 1 then
			sStatus = "Printer out of paper."
			return
		end

		local screenTerminal = term.current()
		local printerTerminal = {
			getCursorPos = printer.getCursorPos,
			setCursorPos = printer.setCursorPos,
			getSize = printer.getPageSize,
			write = printer.write,
		}
		printerTerminal.scroll = function()
			if nPage == 1 then
				printer.setPageTitle( sName.." (page "..nPage..")" )
			end

			local timer

			while not printer.newPage()	do
				if printer.getInkLevel() < 1 then
					sStatus = "Printer out of ink, please refill."
				elseif printer.getPaperLevel() < 1 then
					sStatus = "Printer out of paper, please refill."
				else
					sStatus = "Printer output tray full, please empty."
				end

				term.redirect( screenTerminal )
				redrawMenu()
				term.redirect( printerTerminal )

				timer = os.startTimer(0.5)
				sleep(0.5)
			end

			nPage = nPage + 1
			if nPage == 1 then
				printer.setPageTitle( sName )
			else
				printer.setPageTitle( sName.." (page "..nPage..")" )
			end
		end

		bMenu = false
		term.redirect( printerTerminal )
		local ok, error = pcall( function()
			term.scroll()
			for n, sLine in ipairs( tLines ) do
				print( sLine )
			end
		end )
		term.redirect( screenTerminal )
		if not ok then
			print( error )
		end

		while not printer.endPage() do
			sStatus = "Printer output tray full, please empty."
			redrawMenu()
			sleep( 0.5 )
		end
		bMenu = true

		if nPage ~= 1 then
			sStatus = "Printed "..nPage.." pages."
		else
			sStatus = "Printed 1 page."
		end
		redrawMenu()
	end,
	Exit=function()
		bRunning = false
	end
}

tMenuFuncs["Save As..."] = function()
	sPath = getUserInput("Save to>")
	bReadOnly = fs.isReadOnly(sPath)
	if bReadOnly then
		sStatus = "That location is read only."
	else
		local ok, err = save( sPath )
		if ok then
			sStatus = "Saved to "..sPath.."."
		else
			sStatus = "Error saving to "..sPath.."."
		end
	end
	term.setBackgroundColor(bgColor)
end

tMenuFuncs["Go To"] = function()
	-- Ask for user input
	y = math.max(math.min(tonumber(getUserInput("Go to line>")) or y, #tLines), 1)
	redrawText()

	-- Jump to the correct line.
	sStatus = "Moved to line "..y.."."
	scrollY = math.max(y - 6, 0)
	term.setBackgroundColor(bgColor)

	redrawText()
end

tMenuFuncs["Add Bookmark"] = function()
	local y = y
	tLines[y] = tLines[y].." ".."--Editor Bookmark--"
	redrawText()
end

tMenuFuncs["Bookmarks"] = function()
	local bookmarks = {}
	for line, lineText in ipairs(tLines) do
		if string.find(lineText, "--Editor Bookmark--") then
			table.insert(bookmarks, line)
		end
	end
	local nextBookmark = bookmarks[1]
	if #bookmarks > 0 then
		for bookmarkKey, line in ipairs(bookmarks) do
			if y < line then
				nextBookmark = line
				break
			end
		end
		sStatus = "Bookmark found at line "..nextBookmark.."."
		y = nextBookmark
	else
		sStatus = "No bookmarks found."
	end
end

local function doMenuItem( _n )
	if tMenuFuncs[tMenuItems[_n]] then
		tMenuFuncs[tMenuItems[_n]]()
	else
		sStatus = "This function is not implemented."
	end
	if bMenu then
		bMenu = false
		term.setCursorBlink( true )
	end
	redrawMenu()
end

local function setCursor( newX, newY )
	local oldX, oldY = x, y
	x, y = newX, newY
	local screenX = x - scrollX
	local screenY = y - scrollY

	local bRedraw = false
	if screenX < 1 then
		scrollX = x - 1
		screenX = 1
		bRedraw = true
	elseif screenX > w - shiftX then
		scrollX = x - (w - shiftX)
		screenX = w
		bRedraw = true
	end

	if screenY < 1 then
		scrollY = y - 1
		screenY = 1
		bRedraw = true
	elseif screenY > h - 1 then
		scrollY = y - (h - 1)
		screenY = h - 1
		bRedraw = true
	end

	recomplete()
	if bRedraw then
		redrawText()
	elseif y ~= oldY then
	    redrawLine( oldY )
	    redrawLine( y )
	else
	    redrawLine( y )
	end
	term.setCursorPos( screenX, screenY )

	-- Statusbar now pertains to menu, it would probably be safe to redraw the menu on every key event.
	redrawMenu()
end

local function acceptCompletion()
	if nCompletion then
		-- Find the common prefix of all the other suggestions which start with the same letter as the current one
		local sCompletion = tCompletions[ nCompletion ]
		local sFirstLetter = string.sub( sCompletion, 1, 1 )
		local sCommonPrefix = sCompletion
		local sResult
		for n=1,#tCompletions do
			sResult = tCompletions[n]
			if n ~= nCompletion and string.find( sResult, sFirstLetter, 1, true ) == 1 then
				while #sCommonPrefix > 1 do
					if string.find( sResult, sCommonPrefix, 1, true ) == 1 then
						break
					else
						sCommonPrefix = string.sub( sCommonPrefix, 1, #sCommonPrefix - 1 )
					end
				end
			end
		end

		-- Append this string
		tLines[y] = tLines[y] .. sCommonPrefix
		setCursor( x + string.len( sCommonPrefix ), y )
	end
end

-- Actual program functionality begins. --
load(sPath)

-- Stop it from erroring if you attempt to go to a non-existant line at launch-time.
if #tLines < y then
	y = #tLines
	scrollY = y - 1
end

-- Reset the sidebar.
updateSidebar()

-- Clear the screen.
term.setBackgroundColor( bgColor )
term.clear()
term.setCursorPos( x, y )

-- Draw the menu.
recomplete()
redrawText()
redrawMenu()

-- Handle input.
local ok, err = pcall(function()
	while bRunning do
		term.setCursorBlink( true )
		local sEvent, param, param2, param3 = os.pullEvent()
		term.setCursorBlink( false )
		if sEvent == "key" then
			local oldX, oldY = x, y
			if param == keys.up then
				-- Up
				if not bMenu then
					if nCompletion then
						-- Cycle completions
						nCompletion = nCompletion - 1
						if nCompletion < 1 then
							nCompletion = #tCompletions
						end
						redrawLine(y)

					elseif y > 1 then
						-- Move cursor up
						y = y - 1
						x = math.min( x, string.len( tLines[y] ) + 1 )
						setCursor( x, y )
					end
				end
			elseif param == keys.down then
				-- Down
				if not bMenu then
					-- Move cursor down
					if nCompletion then
						-- Cycle completions
						nCompletion = nCompletion + 1
						if nCompletion > #tCompletions then
							nCompletion = 1
						end
						redrawLine(y)

					elseif y < #tLines then
						-- Move cursor down
						y = y + 1
						x = math.min( x, string.len( tLines[y] ) + 1 )
						setCursor( x, y )
					end
				end
			elseif param == keys.tab then
				-- Tab key.
				if not bMenu and not bReadOnly then
					if nCompletion and x == string.len(tLines[y]) + 1 then
						-- Accept autocomplete
						acceptCompletion()
					else
						-- Indent line.
						local sLine = tLines[y]
						tLines[y] = string.sub(sLine,1,x-1) .. "   " .. string.sub(sLine,x)
						setCursor( x + 3, y )
					end
				end
			elseif param == keys.pageUp then
				-- Page Up
				if not bMenu then
					-- Move up a page
					if y - (h - 1) >= 1 then
						y = y - (h - 1)
					else
						y = 1
					end
					x = math.min( x, string.len( tLines[y] ) + 1 )
					setCursor( x, y )
				end
			elseif param == keys.pageDown then
				-- Page Down
				if not bMenu then
					-- Move down a page
					if y + (h - 1) <= #tLines then
						y = y + (h - 1)
					else
						y = #tLines
					end
					x = math.min( x, string.len( tLines[y] ) + 1 )
					setCursor( x, y )
				end
			elseif param == keys.home then
				-- Home
				if not bMenu then
					-- Move cursor to the beginning
					x=1
					setCursor(x,y)
				end
			elseif param == keys["end"] then
				-- End
				if not bMenu then
					-- Move cursor to the end
					x = string.len( tLines[y] ) + 1
					setCursor(x,y)
				end
			elseif param == keys.left then
				-- Left
				if not bMenu then
					if x > 1 then
						-- Move cursor left
						x = x - 1
					elseif x==1 and y>1 then
						x = string.len( tLines[y-1] ) + 1
						y = y - 1
					end
					setCursor( x, y )
				else
					-- Move menu left
					nMenuItem = nMenuItem - 1
					if nMenuItem < 1 then
						nMenuItem = #tMenuItems
					end
					redrawMenu()
				end
			elseif param == keys.right then
				-- Right
				if not bMenu then
					if x < string.len( tLines[y] ) + 1 then
						-- Move cursor right
						x = x + 1
					elseif x==string.len( tLines[y] ) + 1 and y<#tLines then
						x = 1
						y = y + 1
					end
					setCursor( x, y )
				else
					-- Move menu right
					nMenuItem = nMenuItem + 1
					if nMenuItem > #tMenuItems then
						nMenuItem = 1
					end
					redrawMenu()
				end
			elseif param == keys.delete then
				-- Delete
				if not bMenu and not bReadOnly then
					if  x < string.len( tLines[y] ) + 1 then
						local sLine = tLines[y]
						tLines[y] = string.sub(sLine,1,x-1) .. string.sub(sLine,x+1)
						redrawLine(y)
					elseif y<#tLines then
						tLines[y] = tLines[y] .. tLines[y+1]
						table.remove( tLines, y+1 )
						redrawText()
						redrawMenu()
					end
				end
			elseif param == keys.backspace then
				-- Backspace
				if not bMenu and not bReadOnly then
					if x > 1 then
						-- Remove character
						local sLine = tLines[y]
						tLines[y] = string.sub(sLine,1,x-2) .. string.sub(sLine,x)
						redrawLine(y)

						x = x - 1
						setCursor( x, y )
					elseif y > 1 then
						-- Remove newline
						local sPrevLen = string.len( tLines[y-1] )
						tLines[y-1] = tLines[y-1] .. tLines[y]
						table.remove( tLines, y )
						redrawText()

						x = sPrevLen + 1
						y = y - 1
						setCursor( x, y )
					end
				end
			elseif param == keys.enter then
				-- Enter
				if not bMenu and not bReadOnly then
					-- Newline
					local sLine = tLines[y]
					local _,spaces = string.find(sLine,"^[ ]+")
					if not spaces then
						spaces=0
					end
					tLines[y] = string.sub(sLine,1,x-1)
					table.insert( tLines, y+1, string.rep(' ',spaces)..string.sub(sLine,x) )

					setCursor( spaces + 1, y + 1 )
					redrawText()
				elseif bMenu then
					-- Menu selection
					doMenuItem( nMenuItem )
				end
			elseif param == keys.leftCtrl or param == keys.rightCtrl then
				-- Menu toggle
				bMenu = not bMenu
				if bMenu then
					term.setCursorBlink( false )
				else
					term.setCursorBlink( true )
				end
				redrawMenu()
			end

		elseif sEvent == "char" then
			if not bMenu and not bReadOnly then
				-- Input text
				local sLine = tLines[y]
				tLines[y] = string.sub(sLine,1,x-1) .. param .. string.sub(sLine,x)
				redrawLine(y)

				x = x + 1
				setCursor( x, y )
			elseif bMenu then
				-- Select menu items
				for n,sMenuItem in ipairs( tMenuItems ) do
					if string.lower(string.sub(sMenuItem,1,1)) == string.lower(param) then
						doMenuItem( n )
						break
					end
				end
			end

		elseif sEvent == "paste" then
			if not bMenu and not bReadOnly then
				-- Input text
				local sLine = tLines[y]
				tLines[y] = string.sub(sLine,1,x-1) .. param .. string.sub(sLine,x)
				redrawLine(y)

				x = x + string.len( param )
				setCursor( x, y )
			end

		elseif sEvent == "mouse_click" then
			if not bMenu then
				if param == 1 then
					-- Left click
					local cx, cy = param2, param3
					if cy < h then
						y = math.min( math.max( scrollY + cy, 1 ), #tLines )
						x = math.max(math.min( math.max( scrollX + cx, 1 ), string.len( tLines[y] ) + 1 + shiftX ) - shiftX, 1)
						setCursor( x, y )
					end
				end
			end

		elseif sEvent == "mouse_scroll" then
			if not bMenu then
				if param == -1 then
					-- Scroll up
					if scrollY > 0 then
						-- Move cursor up
						scrollY = scrollY - 1
						redrawText()
					end

				elseif param == 1 then
					-- Scroll down
					local nMaxScroll = #tLines - (h-1)
					if scrollY < nMaxScroll then
						-- Move cursor down
						scrollY = scrollY + 1
						redrawText()
					end

				end
			end

		elseif sEvent == "term_resize" then
			w,h = term.getSize()
			setCursor( x, y )
			redrawMenu()
			redrawText()
		end
	end
end)

-- Cleanup after the program ends.
resetTerm()
term.setCursorBlink( false )

term.setCursorPos( 1, 1 )
if not ok then
	printError(err)
end
