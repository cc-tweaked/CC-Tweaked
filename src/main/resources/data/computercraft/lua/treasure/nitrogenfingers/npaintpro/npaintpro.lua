--[[
		NPaintPro
		By NitrogenFingers
]]--

--The screen size
local w,h = term.getSize()
--Whether or not the program is currently waiting on user input
local inMenu = false
--Whether or not a drop down menu is active
local inDropDown = false
--Whether or not animation tools are enabled (use -a to turn them on)
local animated = false
--Whether or not the text tools are enabled (use -t to turn them on)
local textual = false
--Whether or not "blueprint" display mode is on
local blueprint = false
--Whether or not the "layer" display is on
local layerDisplay = false
--Whether or not the "direction" display is on
local printDirection = false
--The tool/mode npaintpro is currently in. Default is "paint"
--For a list of modes, check out the help file
local state = "paint"
--Whether or not the program is presently running
local isRunning = true
--The rednet address of the 3D printer, if one has been attached
local printer = nil

--The list of every frame, containing every image in the picture/animation
--Note: nfp files always have the picture at frame 1
local frames = { }
--How many frames are currently in the given animation.
local frameCount = 1
--The Colour Picker column
local column = {}
--The currently selected left and right colours
local lSel,rSel = colours.white,nil
--The amount of scrolling on the X and Y axis
local sx,sy = 0,0
--The alpha channel colour
--Change this to change default canvas colour
local alphaC = colours.black
--The currently selected frame. Default is 1
local sFrame = 1
--The contents of the image buffer- contains contents, width and height
local buffer = nil
--The position, width and height of the selection rectangle
local selectrect = nil

--Whether or not text tools are enabled for this document
local textEnabled = false
--The X and Y positions of the text cursor
local textCurX, textCurY = 1,1

--The currently calculated required materials
local requiredMaterials = {}
--Whether or not required materials are being displayed in the pallette
local requirementsDisplayed = false
--A list of the rednet ID's all in-range printers located
local printerList = { }
--A list of the names of all in-range printers located. Same as the printerList in reference
local printerNames = { }
--The selected printer
local selectedPrinter = 1
--The X,Y,Z and facing of the printer
local px,py,pz,pfx,pfz = 0,0,0,0,0
--The form of layering used
local layering = "up"

--The animation state of the selection rectangle and image buffer 
local rectblink = 0
--The ID for the timer
local recttimer = nil
--The radius of the brush tool
local brushsize = 3
--Whether or not "record" mode is activated (animation mode only)
local record = false
--The time between each frame when in play mode (animation mode only)
local animtime = 0.3

--The current "cursor position" in text mode
local cursorTexX,cursorTexY = 1,1

--A list of hexidecimal conversions from numbers to hex digits
local hexnums = { [10] = "a", [11] = "b", [12] = "c", [13] = "d", [14] = "e" , [15] = "f" }
--The NPaintPro logo (divine, isn't it?)
local logo = {
"fcc              3   339";
" fcc          9333    33";
"  fcc        933 333  33";
"   fcc       933  33  33";
"    fcc      933   33 33";
"     c88     333   93333";
"     888     333    9333";
"      333 3  333     939";
}
--The Layer Up and Layer Forward printing icons
local layerUpIcon = {
	"0000000";
	"0088880";
	"0888870";
	"07777f0";
	"0ffff00";
	"0000000";
}
local layerForwardIcon = {
	"0000000";
	"000fff0";
	"00777f0";
	"0888700";
	"0888000";
	"0000000";
}
--The available menu options in the ctrl menu
local mChoices = {"Save","Exit"}
--The available modes from the dropdown menu- tables indicate submenus (include a name!)
local ddModes = { { "paint", "brush", "pippette", "flood", "move", "clear", "select", name = "painting" }, { "alpha to left", "alpha to right", name = "display" }, "help", { "print", "save", "exit", name = "file" }, name = "menu" }
--The available modes from the selection right-click menu
local srModes = { "cut", "copy", "paste", "clear", "hide", name = "selection" }
--The list of available help topics for each mode 127
local helpTopics = {
	[1] = {
		name = "Paint Mode",
		key = nil,
		animonly = false,
		textonly = false,
		message = "The default mode for NPaintPro, for painting pixels."
		.." Controls here that are not overridden will apply for all other modes. Leaving a mode by selecting that mode "
		.." again will always send the user back to paint mode.",
		controls = {
			{ "Arrow keys", "Scroll the canvas" },
			{ "Left Click", "Paint/select left colour" },
			{ "Right Click", "Paint/select right colour" },
			{ "Z Key", "Clear image on screen" },
			{ "Tab Key", "Hide selection rectangle if visible" },
			{ "Q Key", "Set alpha mask to left colour" },
			{ "W Key", "Set alpha mask to right colour" },
			{ "Number Keys", "Swich between frames 1-9" },
			{ "</> keys", "Move to the next/last frame" },
			{ "R Key", "Removes every frame after the current frame"}
		}
	},
	[2] = {
		name = "Brush Mode",
		key = "b",
		animonly = false,
		textonly = false,
		message = "Brush mode allows painting a circular area of variable diameter rather than a single pixel, working in "..
		"the exact same way as paint mode in all other regards.",
		controls = {
			{ "Left Click", "Paints a brush blob with the left colour" },
			{ "Right Click", "Paints a brush blob with the right colour" },
			{ "Number Keys", "Changes the radius of the brush blob from 2-9" }
		}
	},
	[3] = {
		name = "Pippette Mode",
		key = "p",
		animonly = false,
		textonly = false,
		message = "Pippette mode allows the user to click the canvas and set the colour clicked to the left or right "..
		"selected colour, for later painting.",
		controls = {
			{ "Left Click", "Sets clicked colour to the left selected colour" },
			{ "Right Click", "Sets clicked colour to the right selected colour" }
		}
	},
	[4] = {
		name = "Move Mode",
		key = "m",
		animonly = false,
		textonly = false,
		message = "Mode mode allows the moving of the entire image on the screen. This is especially useful for justifying"..
		" the image to the top-left for animations or game assets.",
		controls = {
			{ "Left/Right Click", "Moves top-left corner of image to selected square" },
			{ "Arrow keys", "Moves image one pixel in any direction" }
		}
	},
	[5] = {
		name = "Flood Mode",
		key = "f",
		animonly = false,
		textonly = false,
		message = "Flood mode allows the changing of an area of a given colour to that of the selected colour. "..
		"The tool uses a flood4 algorithm and will not fill diagonally. Transparency cannot be flood filled.",
		controls = {
			{ "Left Click", "Flood fills selected area to left colour" },
			{ "Right Click", "Flood fills selected area to right colour" }
		}
	},
	[6] = {
		name = "Select Mode",
		key = "s",
		animonly = false,
		textonly = false,
		message = "Select mode allows the creation and use of the selection rectangle, to highlight specific areas on "..
		"the screen and perform operations on the selected area of the image. The selection rectangle can contain an "..
		"image on the clipboard- if it does, the image will flash inside the rectangle, and the rectangle edges will "..
		"be light grey instead of dark grey.",
		controls = {
			{ "C Key", "Copy: Moves selection into the clipboard" },
			{ "X Key", "Cut: Clears canvas under the rectangle, and moves it into the clipboard" },
			{ "V Key", "Paste: Copys clipboard to the canvas" },
			{ "Z Key", "Clears clipboard" },
			{ "Left Click", "Moves top-left corner of rectangle to selected pixel" },
			{ "Right Click", "Opens selection menu" },
			{ "Arrow Keys", "Moves rectangle one pixel in any direction" }
		}
	},
	[7] = {
		name = "Corner Select Mode",
		key = nil,
		animonly = false,
		textonly = false,
		message = "If a selection rectangle isn't visible, this mode is selected automatically. It allows the "..
		"defining of the corners of the rectangle- one the top-left and bottom-right corners have been defined, "..
		"NPaintPro switches to selection mode. Note rectangle must be at least 2 pixels wide and high.",
		controls = {
			{ "Left/Right Click", "Defines a corner of the selection rectangle" }
		}
	},
	[8] = {
		name = "Play Mode",
		key = "space",
		animonly = true,
		textonly = false,
		message = "Play mode will loop through each frame in your animation at a constant rate. Editing tools are "..
		"locked in this mode, and the coordinate display will turn green to indicate it is on.",
		controls = {
			{ "</> Keys", "Increases/Decreases speed of the animation" },
			{ "Space Bar", "Returns to paint mode" }
		}
	},
	[9] = {
		name = "Record Mode",
		key = "\\",
		animonly = true,
		textonly = false,
		message = "Record mode is not a true mode, but influences how other modes work. Changes made that modify the "..
		"canvas in record mode will affect ALL frames in the animation. The coordinates will turn red to indicate that "..
		"record mode is on.",
		controls = {
			{ "", "Affects:" },
			{ "- Paint Mode", "" },
			{ "- Brush Mode", "" },
			{ "- Cut and Paste in Select Mode", ""},
			{ "- Move Mode", ""}
		}
	},
	[10] = {
		name = "Help Mode",
		key = "h",
		animonly = false,
		textonly = false,
		message = "Displays this help screen. Clicking on options will display help on that topic. Clicking out of the screen"..
		" will leave this mode.",
		controls = {
			{ "Left/Right Click", "Displays a topic/Leaves the mode" }
		}
	},
	[11] = {
		name = "File Mode",
		key = nil,
		animonly = false,
		textonly = false,
		message = "Clicking on the mode display at the bottom of the screen will open the options menu. Here you can"..
		" activate all of the modes in the program with a simple mouse click. Pressing left control will open up the"..
		" file menu automatically.",
		controls = { 
			{ "leftCtrl", "Opens the file menu" },
			{ "leftAlt", "Opens the paint menu" }
		}
	},
	[12] = {
		name = "Text Mode",
		key = "t",
		animonly = false,
		textonly = true,
		message = "In this mode, the user is able to type letters onto the document for display. The left colour "..
		"pallette value determines what colour the text will be, and the right determines what colour the background "..
		"will be (set either to nil to keep the same colours as already there).",
		controls = {
			{ "Backspace", "Deletes the character on the previous line" },
			{ "Arrow Keys", "Moves the cursor in any direction" },
			{ "Left Click", "Moves the cursor to beneath the mouse cursor" }
		}
	},
	[13] = {
		name = "Textpaint Mode",
		key = "y",
		animonly = false,
		textonly = true,
		message = "Allows the user to paint any text on screen to the desired colour with the mouse. If affects the text colour"..
		" values rather than the background values, but operates identically to paint mode in all other regards.",
		controls = {
			{ "Left Click", "Paints the text with the left colour" },
			{ "Right Click", "Paints the text with the right colour" }
		}
	},
	[14] = {
		name = "About NPaintPro",
		keys = nil,
		animonly = false,
		textonly = false,
		message = "NPaintPro: The feature-bloated paint program for ComputerCraft by Nitrogen Fingers.",
		controls = {
			{ "Testers:", " "},
			{ " ", "Faubiguy"},
			{ " ", "TheOriginalBIT"}
		}
	}
}
--The "bounds" of the image- the first/last point on both axes where a pixel appears
local toplim,botlim,leflim,riglim = nil,nil,nil,nil
--The selected path
local sPath = nil

--[[  
			Section:  Helpers  		
]]--

--[[Converts a colour parameter into a single-digit hex coordinate for the colour
    Params: colour:int = The colour to be converted
	Returns:string A string conversion of the colour
]]--
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

--[[Converts a hex digit into a colour value
	Params: hex:?string = the hex digit to be converted
	Returns:string A colour value corresponding to the hex, or nil if the character is invalid
]]--
local function getColourOf(hex)
	local value = tonumber(hex, 16)
	if not value then return nil end
	value = math.pow(2,value)
	return value
end

--[[Finds the biggest and smallest bounds of the image- the outside points beyond which pixels do not appear
	These values are assigned to the "lim" parameters for access by other methods
	Params: forAllFrames:bool = True if all frames should be used to find bounds, otherwise false or nil
	Returns:nil
]]--
local function updateImageLims(forAllFrames)
	local f,l = sFrame,sFrame
	if forAllFrames == true then f,l = 1,framecount end
	
	toplim,botlim,leflim,riglim = nil,nil,nil,nil
	for locf = f,l do
		for y,_ in pairs(frames[locf]) do
			if type(y) == "number" then
				for x,_ in pairs(frames[locf][y]) do
					if frames[locf][y][x] ~= nil then
						if leflim == nil or x < leflim then leflim = x end
						if toplim == nil or y < toplim then toplim = y end
						if riglim == nil or x > riglim then riglim = x end
						if botlim == nil or y > botlim then botlim = y end
					end
				end
			end
		end
	end
	
	--There is just... no easier way to do this. It's horrible, but necessary
	if textEnabled then
		for locf = f,l do
			for y,_ in pairs(frames[locf].text) do
				for x,_ in pairs(frames[locf].text[y]) do
					if frames[locf].text[y][x] ~= nil then
						if leflim == nil or x < leflim then leflim = x end
						if toplim == nil or y < toplim then toplim = y end
						if riglim == nil or x > riglim then riglim = x end
						if botlim == nil or y > botlim then botlim = y end
					end
				end
			end
			for y,_ in pairs(frames[locf].textcol) do
				for x,_ in pairs(frames[locf].textcol[y]) do
					if frames[locf].textcol[y][x] ~= nil then
						if leflim == nil or x < leflim then leflim = x end
						if toplim == nil or y < toplim then toplim = y end
						if riglim == nil or x > riglim then riglim = x end
						if botlim == nil or y > botlim then botlim = y end
					end
				end
			end
		end
	end
end

--[[Determines how much of each material is required for a print. Done each time printing is called.
	Params: none
	Returns:table A complete list of how much of each material is required.
]]--
function calculateMaterials()
	updateImageLims(animated)
	requiredMaterials = {}
	for i=1,16 do 
		requiredMaterials[i] = 0 
	end
	
	if not toplim then return end
	
	for i=1,#frames do
		for y = toplim, botlim do
			for x = leflim, riglim do
				if type(frames[i][y][x]) == "number" then
					requiredMaterials[math.log(frames[i][y][x],10)/math.log(2,10) + 1] =
						requiredMaterials[math.log(frames[i][y][x],10)/math.log(2,10) + 1] + 1
				end	
			end
		end
	end
end


--[[Updates the rectangle blink timer. Should be called anywhere events are captured, along with a timer capture.
	Params: nil
	Returns:nil
]]--
local function updateTimer(id)
	if id == recttimer then
		recttimer = os.startTimer(0.5)
		rectblink = (rectblink % 2) + 1
	end
end

--[[Constructs a message based on the state currently selected
	Params: nil
	Returns:string A message regarding the state of the application
]]--
local function getStateMessage()
	local msg = " "..string.upper(string.sub(state, 1, 1))..string.sub(state, 2, #state).." mode"
	if state == "brush" then msg = msg..", size="..brushsize end
	return msg
end

--[[Calls the rednet_message event, but also looks for timer events to keep then
	system timer ticking.
	Params: timeout:number how long before the event times out
	Returns:number the id of the sender
		   :number the message send
]]--
local function rsTimeReceive(timeout)
	local timerID
	if timeout then timerID = os.startTimer(timeout) end
	
	local id,key,msg = nil,nil
	while true do
		id,key,msg = os.pullEvent()
		
		if id == "timer" then
			if key == timerID then return
			else updateTimer(key) end
		end
		if id == "rednet_message" then 
			return key,msg
		end
	end
end

--[[Draws a picture, in paint table format on the screen
	Params: image:table = the image to display
			xinit:number = the x position of the top-left corner of the image
			yinit:number = the y position of the top-left corner of the image
			alpha:number = the color to display for the alpha channel. Default is white.
	Returns:nil
]]--
local function drawPictureTable(image, xinit, yinit, alpha)
	if not alpha then alpha = 1 end
	for y=1,#image do
		for x=1,#image[y] do
			term.setCursorPos(xinit + x-1, yinit + y-1)
			local col = getColourOf(string.sub(image[y], x, x))
			if not col then col = alpha end
			term.setBackgroundColour(col)
			term.write(" ")
		end
	end
end

--[[  
			Section: Loading  
]]-- 

--[[Loads a non-animted paint file into the program
	Params: path:string = The path in which the file is located
	Returns:nil
]]--
local function loadNFP(path)
	sFrame = 1
	frames[sFrame] = { }
	if fs.exists(path) then
		local file = io.open(path, "r" )
		local sLine = file:read()
		local num = 1
		while sLine do
			table.insert(frames[sFrame], num, {})
			for i=1,#sLine do
				frames[sFrame][num][i] = getColourOf(string.sub(sLine,i,i))
			end
			num = num+1
			sLine = file:read()
		end
		file:close()
	end
end

--[[Loads a text-paint file into the program
	Params: path:string = The path in which the file is located
	Returns:nil
]]--
local function loadNFT(path)
	sFrame = 1
	frames[sFrame] = { }
	frames[sFrame].text = { }
	frames[sFrame].textcol = { }
	
	if fs.exists(path) then
		local file = io.open(path, "r")
		local sLine = file:read()
		local num = 1
		while sLine do
			table.insert(frames[sFrame], num, {})
			table.insert(frames[sFrame].text, num, {})
			table.insert(frames[sFrame].textcol, num, {})
			
			--As we're no longer 1-1, we keep track of what index to write to
			local writeIndex = 1
			--Tells us if we've hit a 30 or 31 (BG and FG respectively)- next char specifies the curr colour
			local bgNext, fgNext = false, false
			--The current background and foreground colours
			local currBG, currFG = nil,nil
			term.setCursorPos(1,1)
			for i=1,#sLine do
				local nextChar = string.sub(sLine, i, i)
				if nextChar:byte() == 30 then
					bgNext = true
				elseif nextChar:byte() == 31 then
					fgNext = true
				elseif bgNext then
					currBG = getColourOf(nextChar)
					bgNext = false
				elseif fgNext then
					currFG = getColourOf(nextChar)
					fgNext = false
				else
					if nextChar ~= " " and currFG == nil then
						currFG = colours.white
					end
					frames[sFrame][num][writeIndex] = currBG
					frames[sFrame].textcol[num][writeIndex] = currFG
					frames[sFrame].text[num][writeIndex] = nextChar
					writeIndex = writeIndex + 1
				end
			end
			num = num+1
			sLine = file:read()
		end
		file:close()
	end
end

--[[Loads an animated paint file into the program
	Params: path:string = The path in which the file is located
	Returns:nil
]]--
local function loadNFA(path)
	frames[sFrame] = { }
	if fs.exists(path) then
		local file = io.open(path, "r" )
		local sLine = file:read()
		local num = 1
		while sLine do
			table.insert(frames[sFrame], num, {})
			if sLine == "~" then
				sFrame = sFrame + 1
				frames[sFrame] = { }
				num = 1
			else
				for i=1,#sLine do
					frames[sFrame][num][i] = getColourOf(string.sub(sLine,i,i))
				end
				num = num+1
			end
			sLine = file:read()
		end
		file:close()
	end
	framecount = sFrame
	sFrame = 1
end

--[[Saves a non-animated paint file to the specified path
	Params: path:string = The path to save the file to
	Returns:nil
]]--
local function saveNFP(path)
	local sDir = string.sub(sPath, 1, #sPath - #fs.getName(sPath))
	if not fs.exists(sDir) then
		fs.makeDir(sDir)
	end

	local file = io.open(path, "w")
	updateImageLims(false)
	if not toplim then 
		file:close()
		return
	end
	for y=1,botlim do
		local line = ""
		if frames[sFrame][y] then 
			for x=1,riglim do
				line = line..getHexOf(frames[sFrame][y][x])
			end
		end
		file:write(line.."\n")
	end
	file:close()
end

--[[Saves a text-paint file to the specified path
	Params: path:string = The path to save the file to
	Returns:nil
]]--
local function saveNFT(path)
	local sDir = string.sub(sPath, 1, #sPath - #fs.getName(sPath))
	if not fs.exists(sDir) then
		fs.makeDir(sDir)
	end
	
	local file = io.open(path, "w")
	updateImageLims(false)
	if not toplim then
		file:close()
		return
	end
	for y=1,botlim do
		local line = ""
		local currBG, currFG = nil,nil
		for x=1,riglim do
			if frames[sFrame][y] and frames[sFrame][y][x] ~= currBG then
				line = line..string.char(30)..getHexOf(frames[sFrame][y][x])
				currBG = frames[sFrame][y][x]
			end
			if frames[sFrame].textcol[y] and frames[sFrame].textcol[y][x] ~= currFG then
				line = line..string.char(31)..getHexOf(frames[sFrame].textcol[y][x])
				currFG = frames[sFrame].textcol[y][x]
			end
			if frames[sFrame].text[y] then
				local char = frames[sFrame].text[y][x]
				if not char then char = " " end
				line = line..char
			end
		end
		file:write(line.."\n")
	end
	file:close()
end

--[[Saves a animated paint file to the specified path
	Params: path:string = The path to save the file to
	Returns:nil
]]--
local function saveNFA(path)
	local sDir = string.sub(sPath, 1, #sPath - #fs.getName(sPath))
	if not fs.exists(sDir) then
		fs.makeDir(sDir)
	end
	
	local file = io.open(path, "w")
	updateImageLims(true)
	if not toplim then 
		file:close()
		return
	end
	for i=1,#frames do
		for y=1,botlim do
			local line = ""
			if frames[i][y] then 
				for x=1,riglim do
					line = line..getHexOf(frames[i][y][x])
				end
			end
			file:write(line.."\n")
		end
		if i < #frames then file:write("~\n") end
	end
	file:close()
end


--[[Initializes the program, by loading in the paint file. Called at the start of each program.
	Params: none
	Returns:nil
]]--
local function init()
	if textEnabled then
		loadNFT(sPath)
		table.insert(ddModes, 2, { "text", "textpaint", name = "text"})
	elseif animated then 
		loadNFA(sPath)
		table.insert(ddModes, #ddModes, { "record", "play", name = "anim" })
		table.insert(ddModes, #ddModes, { "go to", "remove", name = "frames"})
		table.insert(ddModes[2], #ddModes[2], "blueprint on")
		table.insert(ddModes[2], #ddModes[2], "layers on")
	else 
		loadNFP(sPath) 
		table.insert(ddModes[2], #ddModes[2], "blueprint on")
	end

	for i=0,15 do
		table.insert(column, math.pow(2,i))
	end
end

--[[  
			Section: Drawing  
]]--


--[[Draws the rather superflous logo. Takes about 1 second, before user is able to move to the
	actual program.
	Params: none
	Returns:nil
]]--
local function drawLogo()
	term.setBackgroundColour(colours.white)
	term.clear()
	drawPictureTable(logo, w/2 - #logo[1]/2, h/2 - #logo/2, colours.white)
	term.setBackgroundColour(colours.white)
	term.setTextColour(colours.black)
	local msg = "NPaintPro"
	term.setCursorPos(w/2 - #msg/2, h-3)
	term.write(msg)
	msg = "By NitrogenFingers"
	term.setCursorPos(w/2 - #msg/2, h-2)
	term.write(msg)
	
	os.pullEvent()
end

--[[Clears the display to the alpha channel colour, draws the canvas, the image buffer and the selection
	rectanlge if any of these things are present.
	Params: none
	Returns:nil
]]--
local function drawCanvas()
	--We have to readjust the position of the canvas if we're printing
	turtlechar = "@"
	if state == "active print" then
		if layering == "up" then
			if py >= 1 and py <= #frames then
				sFrame = py
			end
			if pz < sy then sy = pz
			elseif pz > sy + h - 1 then sy = pz + h - 1 end
			if px < sx then sx = px
			elseif px > sx + w - 2 then sx = px + w - 2 end
		else
			if pz >= 1 and pz <= #frames then
				sFrame = pz
			end
			
			if py < sy then sy = py
			elseif py > sy + h - 1 then sy = py + h - 1 end
			if px < sx then sx = px
			elseif px > sx + w - 2 then sx = px + w - 2 end
		end
		
		if pfx == 1 then turtlechar = ">"
		elseif pfx == -1 then turtlechar = "<"
		elseif pfz == 1 then turtlechar = "V"
		elseif pfz == -1 then turtlechar = "^"
		end
	end

	--Picture next
	local topLayer, botLayer
	if layerDisplay then
		topLayer = sFrame
		botLayer = 1
	else
		topLayer,botLayer = sFrame,sFrame
	end
	
	for currframe = botLayer,topLayer,1 do
		for y=sy+1,sy+h-1 do
			if frames[currframe][y] then 
				for x=sx+1,sx+w-2 do
					term.setCursorPos(x-sx,y-sy)
					if frames[currframe][y][x] then
						term.setBackgroundColour(frames[currframe][y][x])
						if textEnabled and frames[currframe].textcol[y][x] and frames[currframe].text[y][x] then
							term.setTextColour(frames[currframe].textcol[y][x])
							term.write(frames[currframe].text[y][x])
						else
							term.write(" ")
						end
					else 
						tileExists = false
						for i=currframe-1,botLayer,-1 do
							if frames[i][y][x] then
								tileExists = true
								break
							end
						end
						
						if not tileExists then
							if blueprint then
								term.setBackgroundColour(colours.blue)
								term.setTextColour(colours.white)
								if x == sx+1 and y % 4 == 1 then
									term.write(""..((y/4) % 10))
								elseif y == sy + 1 and x % 4 == 1 then
									term.write(""..((x/4) % 10))
								elseif x % 2 == 1 and y % 2 == 1 then
									term.write("+")
								elseif x % 2 == 1 then
									term.write("|")
								elseif y % 2 == 1 then
									term.write("-")
								else
									term.write(" ")
								end
							else
								term.setBackgroundColour(alphaC) 
								if textEnabled and frames[currframe].textcol[y][x] and frames[currframe].text[y][x] then
									term.setTextColour(frames[currframe].textcol[y][x])
									term.write(frames[currframe].text[y][x])
								else
									term.write(" ")
								end
							end
						end
					end
				end
			else
				for x=sx+1,sx+w-2 do
					term.setCursorPos(x-sx,y-sy)
					
					tileExists = false
					for i=currframe-1,botLayer,-1 do
						if frames[i][y] and frames[i][y][x] then
							tileExists = true
							break
						end
					end
					
					if not tileExists then
						if blueprint then
							term.setBackgroundColour(colours.blue)
							term.setTextColour(colours.white)
							if x == sx+1 and y % 4 == 1 then
								term.write(""..((y/4) % 10))
							elseif y == sy + 1 and x % 4 == 1 then
								term.write(""..((x/4) % 10))
							elseif x % 2 == 1 and y % 2 == 1 then
								term.write("+")
							elseif x % 2 == 1 then
								term.write("|")
							elseif y % 2 == 1 then
								term.write("-")
							else
								term.write(" ")
							end
						else
							term.setBackgroundColour(alphaC) 
							term.write(" ")
						end
					end
				end
			end
		end
	end
	
	--Then the printer, if he's on
	if state == "active print" then
		local bgColour = alphaC
		if layering == "up" then
			term.setCursorPos(px-sx,pz-sy)
			if frames[sFrame] and frames[sFrame][pz-sy] and frames[sFrame][pz-sy][px-sx] then
				bgColour = frames[sFrame][pz-sy][px-sx]
			elseif blueprint then bgColour = colours.blue end
		else
			term.setCursorPos(px-sx,py-sy)
			if frames[sFrame] and frames[sFrame][py-sy] and frames[sFrame][py-sy][px-sx] then
				bgColour = frames[sFrame][py-sy][px-sx]
			elseif blueprint then bgColour = colours.blue end
		end
		
		term.setBackgroundColour(bgColour)
		if bgColour == colours.black then term.setTextColour(colours.white)
		else term.setTextColour(colours.black) end
		
		term.write(turtlechar)
	end
	
	--Then the buffer
	if selectrect then
		if buffer and rectblink == 1 then
		for y=selectrect.y1, math.min(selectrect.y2, selectrect.y1 + buffer.height-1) do
			for x=selectrect.x1, math.min(selectrect.x2, selectrect.x1 + buffer.width-1) do
				if buffer.contents[y-selectrect.y1+1][x-selectrect.x1+1] then
					term.setCursorPos(x+sx,y+sy)
					term.setBackgroundColour(buffer.contents[y-selectrect.y1+1][x-selectrect.x1+1])
					term.write(" ")
				end
			end
		end
		end
	
		--This draws the "selection" box
		local add = nil
		if buffer then
			term.setBackgroundColour(colours.lightGrey)
		else 
			term.setBackgroundColour(colours.grey)
		end
		for i=selectrect.x1, selectrect.x2 do
			add = (i + selectrect.y1 + rectblink) % 2 == 0
			term.setCursorPos(i-sx,selectrect.y1-sy)
			if add then term.write(" ") end
			add = (i + selectrect.y2 + rectblink) % 2 == 0
			term.setCursorPos(i-sx,selectrect.y2-sy)
			if add then term.write(" ") end
		end
		for i=selectrect.y1 + 1, selectrect.y2 - 1 do
			add = (i + selectrect.x1 + rectblink) % 2 == 0
			term.setCursorPos(selectrect.x1-sx,i-sy)
			if add then term.write(" ") end
			add = (i + selectrect.x2 + rectblink) % 2 == 0
			term.setCursorPos(selectrect.x2-sx,i-sy)
			if add then term.write(" ") end
		end
	end
end

--[[Draws the colour picker on the right side of the screen, the colour pallette and the footer with any 
	messages currently being displayed
	Params: none
	Returns:nil
]]--
local function drawInterface()
	--Picker
	for i=1,#column do
		term.setCursorPos(w-1, i)
		term.setBackgroundColour(column[i])
		if state == "print" then
			if i == 16 then
				term.setTextColour(colours.white)
			else
				term.setTextColour(colours.black)
			end
			if requirementsDisplayed then
				if requiredMaterials[i] < 10 then term.write(" ") end
				term.setCursorPos(w-#tostring(requiredMaterials[i])+1, i)
				term.write(requiredMaterials[i])
			else
				if i < 10 then term.write(" ") end
				term.write(i)
			end
		else
			term.write("  ")
		end
	end
	term.setCursorPos(w-1,#column+1)
	term.setBackgroundColour(colours.black)
	term.setTextColour(colours.red)
	term.write("XX")
	--Pallette
	term.setCursorPos(w-1,h-1)
	if not lSel then
		term.setBackgroundColour(colours.black)
		term.setTextColour(colours.red)
		term.write("X")
	else
		term.setBackgroundColour(lSel)
		term.setTextColour(lSel)
		term.write(" ")
	end
	if not rSel then
		term.setBackgroundColour(colours.black)
		term.setTextColour(colours.red)
		term.write("X")
	else
		term.setBackgroundColour(rSel)
		term.setTextColour(rSel)
		term.write(" ")
	end
	--Footer
	if inMenu then return end
	
	term.setCursorPos(1, h)
	term.setBackgroundColour(colours.lightGrey)
	term.setTextColour(colours.grey)
	term.clearLine()
	if inDropDown then
		term.write(string.rep(" ", 6))
	else
		term.setBackgroundColour(colours.grey)
		term.setTextColour(colours.lightGrey)
		term.write("menu  ")
	end
	term.setBackgroundColour(colours.lightGrey)
	term.setTextColour(colours.grey)
	term.write(getStateMessage())
	
	local coords="X:"..sx.." Y:"..sy
	if animated then coords = coords.." Frame:"..sFrame.."/"..framecount.."   " end
	term.setCursorPos(w-#coords+1,h)
	if state == "play" then term.setBackgroundColour(colours.lime)
	elseif record then term.setBackgroundColour(colours.red) end
	term.write(coords)
	
	if animated then
		term.setCursorPos(w-1,h)
		term.setBackgroundColour(colours.grey)
		term.setTextColour(colours.lightGrey)
		term.write("<>")
	end
end

--[[Runs an interface where users can select topics of help. Will return once the user quits the help screen.
	Params: none
	Returns:nil
]]--
local function drawHelpScreen()
	local selectedHelp = nil
	while true do
		term.setBackgroundColour(colours.lightGrey)
		term.clear()
		if not selectedHelp then
			term.setCursorPos(4, 1)
			term.setTextColour(colours.brown)
			term.write("Available modes (click for info):")
			for i=1,#helpTopics do
				term.setCursorPos(2, 2 + i)
				term.setTextColour(colours.black)
				term.write(helpTopics[i].name)
				if helpTopics[i].key then
					term.setTextColour(colours.red)
					term.write(" ("..helpTopics[i].key..")")
				end
			end
			term.setCursorPos(4,h)
			term.setTextColour(colours.black)
			term.write("Press any key to exit")
		else
			term.setCursorPos(4,1)
			term.setTextColour(colours.brown)
			term.write(helpTopics[selectedHelp].name)
			if helpTopics[selectedHelp].key then
				term.setTextColour(colours.red)
				term.write(" ("..helpTopics[selectedHelp].key..")")
			end
			term.setCursorPos(1,3)
			term.setTextColour(colours.black)
			print(helpTopics[selectedHelp].message.."\n")
			for i=1,#helpTopics[selectedHelp].controls do
				term.setTextColour(colours.brown)
				term.write(helpTopics[selectedHelp].controls[i][1].." ")
				term.setTextColour(colours.black)
				print(helpTopics[selectedHelp].controls[i][2])
			end
		end
		
		local id,p1,p2,p3 = os.pullEvent()
		
		if id == "timer" then updateTimer(p1)
		elseif id == "key" then 
			if selectedHelp then selectedHelp = nil
			else break end
		elseif id == "mouse_click" then
			if not selectedHelp then 
				if p3 >=3 and p3 <= 2+#helpTopics then
					selectedHelp = p3-2 
				else break end
			else
				selectedHelp = nil
			end
		end
	end
end

--[[Draws a message in the footer bar. A helper for DrawInterface, but can be called for custom messages, if the
	inMenu paramter is set to true while this is being done (remember to set it back when done!)
	Params: message:string = The message to be drawn
	Returns:nil
]]--
local function drawMessage(message)
	term.setCursorPos(1,h)
	term.setBackgroundColour(colours.lightGrey)
	term.setTextColour(colours.grey)
	term.clearLine()
	term.write(message)
end

--[[
			Section: Generic Interfaces
]]--


--[[One of my generic text printing methods, printing a message at a specified position with width and offset.
	No colour materials included.
	Params: msg:string = The message to print off-center
			height:number = The starting height of the message
			width:number = The limit as to how many characters long each line may be
			offset:number = The starting width offset of the message
	Returns:number the number of lines used in printing the message
]]--
local function wprintOffCenter(msg, height, width, offset)
	local inc = 0
	local ops = 1
	while #msg - ops > width do
		local nextspace = 0
		while string.find(msg, " ", ops + nextspace) and
				string.find(msg, " ", ops + nextspace) - ops < width do
			nextspace = string.find(msg, " ", nextspace + ops) + 1 - ops
		end
		local ox,oy = term.getCursorPos()
		term.setCursorPos(width/2 - (nextspace)/2 + offset, height + inc)
		inc = inc + 1
		term.write(string.sub(msg, ops, nextspace + ops - 1))
		ops = ops + nextspace
	end
	term.setCursorPos(width/2 - #string.sub(msg, ops)/2 + offset, height + inc)
	term.write(string.sub(msg, ops))
	
	return inc + 1
end

--[[Draws a message that must be clicked on or a key struck to be cleared. No options, so used for displaying
	generic information.
	Params: ctitle:string = The title of the confirm dialogue
			msg:string = The message displayed in the dialogue
	Returns:nil
]]--
local function displayConfirmDialogue(ctitle, msg)
	local dialogoffset = 8
	--We actually print twice- once to get the lines, second time to print proper. Easier this way.
	local lines = wprintOffCenter(msg, 5, w - (dialogoffset+2) * 2, dialogoffset + 2)
	
	term.setCursorPos(dialogoffset, 3)
	term.setBackgroundColour(colours.grey)
	term.setTextColour(colours.lightGrey)
	term.write(string.rep(" ", w - dialogoffset * 2))
	term.setCursorPos(dialogoffset + (w - dialogoffset * 2)/2 - #ctitle/2, 3)
	term.write(ctitle)
	term.setTextColour(colours.grey)
	term.setBackgroundColour(colours.lightGrey)
	term.setCursorPos(dialogoffset, 4)
	term.write(string.rep(" ", w - dialogoffset * 2))
	for i=5,5+lines do
		term.setCursorPos(dialogoffset, i) 
		term.write(" "..string.rep(" ", w - (dialogoffset) * 2 - 2).." ")
	end
	wprintOffCenter(msg, 5, w - (dialogoffset+2) * 2, dialogoffset + 2)
	
	--In the event of a message, the player hits anything to continue
	while true do
		local id,key = os.pullEvent()
		if id == "timer" then updateTimer(key);
		elseif id == "key" or id == "mouse_click" or id == "mouse_drag" then break end
	end
end

--[[Produces a nice dropdown menu based on a table of strings. Depending on the position, this will auto-adjust the position
	of the menu drawn, and allows nesting of menus and sub menus. Clicking anywhere outside the menu will cancel and return nothing
	Params: x:int = the x position the menu should be displayed at
			y:int = the y position the menu should be displayed at
			options:table = the list of options available to the user, as strings or submenus (tables of strings, with a name parameter)
	Returns:string the selected menu option.
]]--
local function displayDropDown(x, y, options)
	inDropDown = true
	--Figures out the dimensions of our thing
	local longestX = #options.name
	for i=1,#options do
		local currVal = options[i]
		if type(currVal) == "table" then currVal = currVal.name end
		
		longestX = math.max(longestX, #currVal)
	end
	local xOffset = math.max(0, longestX - ((w-2) - x) + 1)
	local yOffset = math.max(0, #options - ((h-1) - y))
	
	local clickTimes = 0
	local tid = nil
	local selection = nil
	while clickTimes < 2 do
		drawCanvas()
		drawInterface()
		
		term.setCursorPos(x-xOffset,y-yOffset)
		term.setBackgroundColour(colours.grey)
		term.setTextColour(colours.lightGrey)
		term.write(options.name..string.rep(" ", longestX-#options.name + 2))
	
		for i=1,#options do
			term.setCursorPos(x-xOffset, y-yOffset+i)
			if i==selection and clickTimes % 2 == 0 then
				term.setBackgroundColour(colours.grey)
				term.setTextColour(colours.lightGrey)
			else
				term.setBackgroundColour(colours.lightGrey)
				term.setTextColour(colours.grey)
			end
			local currVal = options[i]
			if type(currVal) == "table" then 
				term.write(currVal.name..string.rep(" ", longestX-#currVal.name + 1))
				term.setBackgroundColour(colours.grey)
				term.setTextColour(colours.lightGrey)
				term.write(">")
			else
				term.write(currVal..string.rep(" ", longestX-#currVal + 2))
			end
		end
		
		local id, p1, p2, p3 = os.pullEvent()
		if id == "timer" then
			if p1 == tid then 
				clickTimes = clickTimes + 1
				if clickTimes > 2 then 
					break
				else 
					tid = os.startTimer(0.1) 
				end
			else 
				updateTimer(p1) 
				drawCanvas()
				drawInterface()
			end
		elseif id == "mouse_click" then
			if p2 >=x-xOffset and p2 <= x-xOffset + longestX + 1 and p3 >= y-yOffset+1 and p3 <= y-yOffset+#options then
				selection = p3-(y-yOffset)
				tid = os.startTimer(0.1)
			else
				selection = ""
				break
			end
		end
	end
	
	if type(selection) == "number" then
		selection = options[selection]
	end
	
	if type(selection) == "string" then 
		inDropDown = false
		return selection
	elseif type(selection) == "table" then 
		return displayDropDown(x, y, selection)
	end
end

--[[A custom io.read() function with a few differences- it limits the number of characters being printed,
	waits a 1/100th of a second so any keys still in the event library are removed before input is read and
	the timer for the selectionrectangle is continuously updated during the process.
	Params: lim:int = the number of characters input is allowed
	Returns:string the inputted string, trimmed of leading and tailing whitespace
]]--
local function readInput(lim)
	term.setCursorBlink(true)

	local inputString = ""
	if not lim or type(lim) ~= "number" or lim < 1 then lim = w - ox end
	local ox,oy = term.getCursorPos()
	--We only get input from the footer, so this is safe. Change if recycling
	term.setBackgroundColour(colours.lightGrey)
	term.setTextColour(colours.grey)
	term.write(string.rep(" ", lim))
	term.setCursorPos(ox, oy)
	--As events queue immediately, we may get an unwanted key... this will solve that problem
	local inputTimer = os.startTimer(0.01)
	local keysAllowed = false
	
	while true do
		local id,key = os.pullEvent()
		
		if keysAllowed then
			if id == "key" and key == 14 and #inputString > 0 then
				inputString = string.sub(inputString, 1, #inputString-1)
				term.setCursorPos(ox + #inputString,oy)
				term.write(" ")
			elseif id == "key" and key == 28 and inputString ~= string.rep(" ", #inputString) then 
				break
			elseif id == "key" and key == keys.leftCtrl then
				return ""
			elseif id == "char" and #inputString < lim then
				inputString = inputString..key
			end
		end
		
		if id == "timer" then
			if key == inputTimer then 
				keysAllowed = true
			else
				updateTimer(key)
				drawCanvas()
				drawInterface()
				term.setBackgroundColour(colours.lightGrey)
				term.setTextColour(colours.grey)
			end
		end
		term.setCursorPos(ox,oy)
		term.write(inputString)
		term.setCursorPos(ox + #inputString, oy)
	end
	
	while string.sub(inputString, 1, 1) == " " do
		inputString = string.sub(inputString, 2, #inputString)
	end
	while string.sub(inputString, #inputString, #inputString) == " " do
		inputString = string.sub(inputString, 1, #inputString-1)
	end
	term.setCursorBlink(false)
	
	return inputString
end

--[[  
			Section: Image tools 
]]--


--[[Copies all pixels beneath the selection rectangle into the image buffer. Empty buffers are converted to nil.
	Params: removeImage:bool = true if the image is to be erased after copying, false otherwise
	Returns:nil
]]--
local function copyToBuffer(removeImage)
	buffer = { width = selectrect.x2 - selectrect.x1 + 1, height = selectrect.y2 - selectrect.y1 + 1, contents = { } }
	
	local containsSomething = false
	for y=1,buffer.height do
		buffer.contents[y] = { }
		local f,l = sFrame,sFrame
		if record then f,l = 1, framecount end
		
		for fra = f,l do
			if frames[fra][selectrect.y1 + y - 1] then
				for x=1,buffer.width do
					buffer.contents[y][x] = frames[sFrame][selectrect.y1 + y - 1][selectrect.x1 + x - 1]
					if removeImage then frames[fra][selectrect.y1 + y - 1][selectrect.x1 + x - 1] = nil end
					if buffer.contents[y][x] then containsSomething = true end
				end
			end
		end
	end
	--I don't classify an empty buffer as a real buffer- confusing to the user.
	if not containsSomething then buffer = nil end
end

--[[Replaces all pixels under the selection rectangle with the image buffer (or what can be seen of it). Record-dependent.
	Params: removeBuffer:bool = true if the buffer is to be emptied after copying, false otherwise
	Returns:nil
]]--
local function copyFromBuffer(removeBuffer)
	if not buffer then return end

	for y = 1, math.min(buffer.height,selectrect.y2-selectrect.y1+1) do
		local f,l = sFrame, sFrame
		if record then f,l = 1, framecount end
		
		for fra = f,l do
			if not frames[fra][selectrect.y1+y-1] then frames[fra][selectrect.y1+y-1] = { } end
			for x = 1, math.min(buffer.width,selectrect.x2-selectrect.x1+1) do
				frames[fra][selectrect.y1+y-1][selectrect.x1+x-1] = buffer.contents[y][x]
			end
		end
	end
	
	if removeBuffer then buffer = nil end
end

--[[Moves the entire image (or entire animation) to the specified coordinates. Record-dependent.
	Params: newx:int = the X coordinate to move the image to
			newy:int = the Y coordinate to move the image to
	Returns:nil
]]--
local function moveImage(newx,newy)
	if not leflim or not toplim then return end
	if newx <=0 or newy <=0 then return end
	local f,l = sFrame,sFrame
	if record then f,l = 1,framecount end
	
	for i=f,l do
		local newlines = { }
		for y=toplim,botlim do
			local line = frames[i][y]
			if line then
				newlines[y-toplim+newy] = { }
				for x,char in pairs(line) do
					newlines[y-toplim+newy][x-leflim+newx] = char
				end
			end
		end
		--Exceptions that allow us to move the text as well
		if textEnabled then
			newlines.text = { }
			for y=toplim,botlim do
				local line = frames[i].text[y]
				if line then
					newlines.text[y-toplim+newy] = { }
					for x,char in pairs(line) do
						newlines.text[y-toplim+newy][x-leflim+newx] = char
					end
				end
			end
			
			newlines.textcol = { }
			for y=toplim,botlim do
				local line = frames[i].textcol[y]
				if line then
					newlines.textcol[y-toplim+newy] = { }
					for x,char in pairs(line) do
						newlines.textcol[y-toplim+newy][x-leflim+newx] = char
					end
				end
			end
		end
		
		frames[i] = newlines
	end
end

--[[Prompts the user to clear the current frame or all frames. Record-dependent.,
	Params: none
	Returns:nil
]]--
local function clearImage()
	inMenu = true
	if not animated then
		drawMessage("Clear image? Y/N: ")
	elseif record then
		drawMessage("Clear ALL frames? Y/N: ")
	else
		drawMessage("Clear current frame? Y/N :")
	end
	if string.find(string.upper(readInput(1)), "Y") then
		local f,l = sFrame,sFrame
		if record then f,l = 1,framecount end
		
		for i=f,l do
			frames[i] = { }
		end
	end
	inMenu = false
end

--[[A recursively called method (watch out for big calls!) in which every pixel of a set colour is
	changed to another colour. Does not work on the nil colour, for obvious reasons.
	Params: x:int = The X coordinate of the colour to flood-fill
			y:int = The Y coordinate of the colour to flood-fill
			targetColour:colour = the colour that is being flood-filled
			newColour:colour = the colour with which to replace the target colour
	Returns:nil
]]--
local function floodFill(x, y, targetColour, newColour)
	if not newColour or not targetColour then return end
	local nodeList = { }
	
	table.insert(nodeList, {x = x, y = y})
	
	while #nodeList > 0 do
		local node = nodeList[1]
		if frames[sFrame][node.y] and frames[sFrame][node.y][node.x] == targetColour then
			frames[sFrame][node.y][node.x] = newColour
			table.insert(nodeList, { x = node.x + 1, y = node.y})
			table.insert(nodeList, { x = node.x, y = node.y + 1})
			if x > 1 then table.insert(nodeList, { x = node.x - 1, y = node.y}) end
			if y > 1 then table.insert(nodeList, { x = node.x, y = node.y - 1}) end
		end
		table.remove(nodeList, 1)
	end
end

--[[  
			Section: Animation Tools  
]]--

--[[Enters play mode, allowing the animation to play through. Interface is restricted to allow this,
	and method only leaves once the player leaves play mode.
	Params: none
	Returns:nil
]]--
local function playAnimation()
	state = "play"
	selectedrect = nil
	
	local animt = os.startTimer(animtime)
	repeat
		drawCanvas()
		drawInterface()
		
		local id,key,_,y = os.pullEvent()
		
		if id=="timer" then
			if key == animt then
				animt = os.startTimer(animtime)
				sFrame = (sFrame % framecount) + 1
			else
				updateTimer(key)
			end
		elseif id=="key" then
			if key == keys.comma and animtime > 0.1 then animtime = animtime - 0.05
			elseif key == keys.period and animtime < 0.5 then animtime = animtime + 0.05
			elseif key == keys.space then state = "paint" end
		elseif id=="mouse_click" and y == h then
			state = "paint"
		end
	until state ~= "play"
	os.startTimer(0.5)
end

--[[Changes the selected frame (sFrame) to the chosen frame. If this frame is above the framecount,
	additional frames are created with a copy of the image on the selected frame.
	Params: newframe:int = the new frame to move to
	Returns:nil
]]--
local function changeFrame(newframe)
	inMenu = true
	if not tonumber(newframe) then
		term.setCursorPos(1,h)
		term.setBackgroundColour(colours.lightGrey)
		term.setTextColour(colours.grey)
		term.clearLine()
	
		term.write("Go to frame: ")
		newframe = tonumber(readInput(2))
		if not newframe or newframe <= 0 then
			inMenu = false
			return 
		end
	elseif newframe <= 0 then return end
	
	if newframe > framecount then
		for i=framecount+1,newframe do
			frames[i] = {}
			for y,line in pairs(frames[sFrame]) do
				frames[i][y] = { }
				for x,v in pairs(line) do
					frames[i][y][x] = v
				end
			end
		end
		framecount = newframe
	end
	sFrame = newframe
	inMenu = false
end

--[[Removes every frame leading after the frame passed in
	Params: frame:int the non-inclusive lower bounds of the delete
	Returns:nil
]]--
local function removeFramesAfter(frame)
	inMenu = true
	if frame==framecount then return end
	drawMessage("Remove frames "..(frame+1).."/"..framecount.."? Y/N :")
	local answer = string.upper(readInput(1))
	
	if string.find(answer, string.upper("Y")) ~= 1 then 
		inMenu = false
		return 
	end
	
	for i=frame+1, framecount do
		frames[i] = nil
	end
	framecount = frame
	inMenu = false
end

--[[
			Section: Printing Tools
]]--

--[[Constructs a new facing to the left of the current facing
	Params: curx:number = The facing on the X axis
			curz:number = The facing on the Z axis
			hand:string = The hand of the axis ("right" or "left")
	Returns:number,number = the new facing on the X and Z axis after a left turn
]]--
local function getLeft(curx, curz)
	local hand = "left"
	if layering == "up" then hand = "right" end
	
	if hand == "right" then
		if curx == 1 then return 0,-1 end
		if curx == -1 then return 0,1 end
		if curz == 1 then return 1,0 end
		if curz == -1 then return -1,0 end
	else
		if curx == 1 then return 0,1 end
		if curx == -1 then return 0,-1 end
		if curz == 1 then return -1,0 end
		if curz == -1 then return 1,0 end
	end
end

--[[Constructs a new facing to the right of the current facing
	Params: curx:number = The facing on the X axis
			curz:number = The facing on the Z axis
			hand:string = The hand of the axis ("right" or "left")
	Returns:number,number = the new facing on the X and Z axis after a right turn
]]--
local function getRight(curx, curz)
	local hand = "left"
	if layering == "up" then hand = "right" end
	
	if hand == "right" then
		if curx == 1 then return 0,1 end
		if curx == -1 then return 0,-1 end
		if curz == 1 then return -1,0 end
		if curz == -1 then return 1,0 end
	else
		if curx == 1 then return 0,-1 end
		if curx == -1 then return 0,1 end
		if curz == 1 then return 1,0 end
		if curz == -1 then return -1,0 end
	end
end


--[[Sends out a rednet signal requesting local printers, and will listen for any responses. Printers found are added to the
	printerList (for ID's) and printerNames (for names)
	Params: nil
	Returns:nil
]]--
local function locatePrinters()
	printerList = { }
	printerNames = { name = "Printers" }
	local oldState = state
	state = "Locating printers, please wait...   "
	drawCanvas()
	drawInterface()
	state = oldState
	
	local modemOpened = false
	for k,v in pairs(rs.getSides()) do
		if peripheral.isPresent(v) and peripheral.getType(v) == "modem" then
			rednet.open(v)
			modemOpened = true
			break
		end
	end
	
	if not modemOpened then
		displayConfirmDialogue("Modem not found!", "No modem peripheral. Must have network modem to locate printers.")
		return false
	end
	
	rednet.broadcast("$3DPRINT IDENTIFY")
	
	while true do
		local id, msg = rsTimeReceive(1)
		
		if not id then break end
		if string.find(msg, "$3DPRINT IDACK") == 1 then
			msg = string.gsub(msg, "$3DPRINT IDACK ", "")
			table.insert(printerList, id)
			table.insert(printerNames, msg)
		end
	end
	
	if #printerList == 0 then
		displayConfirmDialogue("Printers not found!", "No active printers found in proximity of this computer.")
		return false
	else
		return true
	end
end

--[[Sends a request to the printer. Waits on a response and updates the state of the application accordingly.
	Params: command:string the command to send
			param:string a parameter to send, if any
	Returns:nil
]]--
local function sendPC(command,param)
	local msg = "$PC "..command
	if param then msg = msg.." "..param end
	rednet.send(printerList[selectedPrinter], msg)
	
	while true do
		local id,key = rsTimeReceive()
		if id == printerList[selectedPrinter] then
			if key == "$3DPRINT ACK" then
				break
			elseif key == "$3DPRINT DEP" then
				displayConfirmDialogue("Printer Empty", "The printer has exhasted a material. Please refill slot "..param..
					", and click this message when ready to continue.")
				rednet.send(printerList[selectedPrinter], msg)
			elseif key == "$3DPRINT OOF" then
				displayConfirmDialogue("Printer Out of Fuel", "The printer has no fuel. Please replace the material "..
					"in slot 1 with a fuel source, then click this message.")
				rednet.send(printerList[selectedPrinter], "$PC SS 1")
				id,key = rsTimeReceive()
				rednet.send(printerList[selectedPrinter], "$PC RF")
				id,key = rsTimeReceive()
				rednet.send(printerList[selectedPrinter], msg)
			end
		end
	end
	
	--Changes to position are handled after the event has been successfully completed
	if command == "FW" then
		px = px + pfx
		pz = pz + pfz
	elseif command == "BK" then
		px = px - pfx
		pz = pz - pfz
	elseif command == "UP" then
		if layering == "up" then
			py = py + 1
		else 
			py = py - 1
		end
	elseif command == "DW" then
		if layering == "up" then
			py = py - 1
		else 	
			py = py + 1
		end
	elseif command == "TL" then
		pfx,pfz = getLeft(pfx,pfz)
	elseif command == "TR" then
		pfx,pfz = getRight(pfx,pfz)
	elseif command == "TU" then
		pfx = -pfx
		pfz = -pfz
	end
	
	drawCanvas()
	drawInterface()
end

--[[A printing function that commands the printer to turn to face the desired direction, if it is not already doing so
	Params: desx:number = the normalized x direction to face
			desz:number = the normalized z direction to face
	Returns:nil
]]--
local function turnToFace(desx,desz)
	if desx ~= 0 then
		if pfx ~= desx then
			local temppfx,_ = getLeft(pfx,pfz)
			if temppfx == desx then
				sendPC("TL")
			elseif temppfx == -desx then
				sendPC("TR")
			else
				sendPC("TU")
			end
		end
	else
		print("on the z axis")
		if pfz ~= desz then
			local _,temppfz = getLeft(pfx,pfz)
			if temppfz == desz then
				sendPC("TL")
			elseif temppfz == -desz then
				sendPC("TR")
			else
				sendPC("TU")
			end
		end
	end
end

--[[Performs the print
	Params: nil
	Returns:nil
]]--
local function performPrint()
	state = "active print"
	if layering == "up" then
		--An up layering starts our builder bot on the bottom left corner of our build
		px,py,pz = leflim, 0, botlim + 1
		pfx,pfz = 0,-1
		
		--We move him forward and up a bit from his original position.
		sendPC("FW")
		sendPC("UP")
		--For each layer that needs to be completed, we go up by one each time
		for layers=1,#frames do
			--We first decide if we're going forwards or back, depending on what side we're on
			local rowbot,rowtop,rowinc = nil,nil,nil
			if pz == botlim then
				rowbot,rowtop,rowinc = botlim,toplim,-1
			else
				rowbot,rowtop,rowinc = toplim,botlim,1
			end
			
			for rows = rowbot,rowtop,rowinc do
				--Then we decide if we're going left or right, depending on what side we're on
				local linebot,linetop,lineinc = nil,nil,nil
				if px == leflim then
					--Facing from the left side has to be easterly- it's changed here
					turnToFace(1,0)
					linebot,linetop,lineinc = leflim,riglim,1
				else
					--Facing from the right side has to be westerly- it's changed here
					turnToFace(-1,0)
					linebot,linetop,lineinc = riglim,leflim,-1
				end
				
				for lines = linebot,linetop,lineinc do
					--We move our turtle forward, placing the right material at each step
					local material = frames[py][pz][px]
					if material then
						material = math.log10(frames[py][pz][px])/math.log10(2) + 1
						sendPC("SS", material)
						sendPC("PD")
					end
					if lines ~= linetop then
						sendPC("FW")
					end
				end
				
				--The printer then has to do a U-turn, depending on which way he's facing and
				--which way he needs to go
				local temppfx,temppfz = getLeft(pfx,pfz)
				if temppfz == rowinc and rows ~= rowtop then
					sendPC("TL")
					sendPC("FW")
					sendPC("TL")
				elseif temppfz == -rowinc and rows ~= rowtop then
					sendPC("TR")
					sendPC("FW")
					sendPC("TR")
				end
			end
			--Now at the end of a run he does a 180 and moves up to begin the next part of the print
			sendPC("TU")
			if layers ~= #frames then
				sendPC("UP")
			end
		end
		--All done- now we head back to where we started.
		if px ~= leflim then
			turnToFace(-1,0)
			while px ~= leflim do
				sendPC("FW")
			end
		end
		if pz ~= botlim then
			turnToFace(0,-1)
			while pz ~= botlim do
				sendPC("BK")
			end
		end
		turnToFace(0,-1)
		sendPC("BK")
		while py > 0 do
			sendPC("DW")
		end
	else
		--The front facing is at the top-left corner, facing south not north
		px,py,pz = leflim, botlim, 1
		pfx,pfz = 0,1
		--We move the printer to the last layer- he prints from the back forwards
		while pz < #frames do
			sendPC("FW")
		end
		
		--For each layer in the frame we build our wall, the move back
		for layers = 1,#frames do
			--We first decide if we're going left or right based on our position
			local rowbot,rowtop,rowinc = nil,nil,nil
			if px == leflim then
				rowbot,rowtop,rowinc = leflim,riglim,1
			else
				rowbot,rowtop,rowinc = riglim,leflim,-1
			end
			
			for rows = rowbot,rowtop,rowinc do
				--Then we decide if we're going up or down, depending on our given altitude
				local linebot,linetop,lineinc = nil,nil,nil
				if py == botlim then
					linebot,linetop,lineinc = botlim,toplim,-1
				else
					linebot,linetop,lineinc = toplim,botlim,1
				end
				
				for lines = linebot,linetop,lineinc do
				--We move our turtle up/down, placing the right material at each step
					local material = frames[pz][py][px]
					if material then
						material = math.log10(frames[pz][py][px])/math.log10(2) + 1
						sendPC("SS", material)
						sendPC("PF")
					end
					if lines ~= linetop then
						if lineinc == 1 then sendPC("DW")
						else sendPC("UP") end
					end
				end
					
				if rows ~= rowtop then
					turnToFace(rowinc,0)
					sendPC("FW")
					turnToFace(0,1)
				end
			end
			
			if layers ~= #frames then
				sendPC("TU")
				sendPC("FW")
				sendPC("TU")
			end
		end
		--He's easy to reset
		while px ~= leflim do
			turnToFace(-1,0)
			sendPC("FW")
		end
		turnToFace(0,1)
	end
	
	sendPC("DE")
	
	displayConfirmDialogue("Print complete", "The 3D print was successful.")
end

--[[  
			Section: Interface  
]]--

--[[Runs the printing interface. Allows users to find/select a printer, the style of printing to perform and to begin the operation
	Params: none
	Returns:boolean true if printing was started, false otherwse
]]--
local function runPrintInterface()
	calculateMaterials()
	--There's nothing on canvas yet!
	if not botlim then
		displayConfirmDialogue("Cannot Print Empty Canvas", "There is nothing on canvas that "..
				"can be printed, and the operation cannot be completed.")
		return false
	end
	--No printers nearby
	if not locatePrinters() then
		return false
	end
	
	layering = "up"
	requirementsDisplayed = false
	selectedPrinter = 1
	while true do
		drawCanvas()
		term.setBackgroundColour(colours.lightGrey)
		for i=1,10 do
			term.setCursorPos(1,i)
			term.clearLine()
		end
		drawInterface()
		term.setBackgroundColour(colours.lightGrey)
		term.setTextColour(colours.black)
		
		local msg = "3D Printing"
		term.setCursorPos(w/2-#msg/2 - 2, 1)
		term.write(msg)
		term.setBackgroundColour(colours.grey)
		term.setTextColour(colours.lightGrey)
		if(requirementsDisplayed) then
			msg = "Count:"
		else
			msg = " Slot:"
		end
		term.setCursorPos(w-3-#msg, 1)
		term.write(msg)
		term.setBackgroundColour(colours.lightGrey)
		term.setTextColour(colours.black)
		
		term.setCursorPos(7, 2)
		term.write("Layering")
		drawPictureTable(layerUpIcon, 3, 3, colours.white)
		drawPictureTable(layerForwardIcon, 12, 3, colours.white)
		if layering == "up" then
			term.setBackgroundColour(colours.red)
		else
			term.setBackgroundColour(colours.lightGrey)
		end
		term.setCursorPos(3, 9)
		term.write("Upwards")
		if layering == "forward" then
			term.setBackgroundColour(colours.red)
		else
			term.setBackgroundColour(colours.lightGrey)
		end
		term.setCursorPos(12, 9)
		term.write("Forward")
		
		term.setBackgroundColour(colours.lightGrey)
		term.setTextColour(colours.black)
		term.setCursorPos(31, 2)
		term.write("Printer ID")
		term.setCursorPos(33, 3)
		if #printerList > 1 then
			term.setBackgroundColour(colours.grey)
			term.setTextColour(colours.lightGrey)
		else
			term.setTextColour(colours.red)
		end
		term.write(" "..printerNames[selectedPrinter].." ")
		
		term.setBackgroundColour(colours.grey)
		term.setTextColour(colours.lightGrey)
		term.setCursorPos(25, 10)
		term.write(" Cancel ")
		term.setCursorPos(40, 10)
		term.write(" Print ")
		
		local id, p1, p2, p3 = os.pullEvent()
		
		if id == "timer" then
			updateTimer(p1)
		elseif id == "mouse_click" then
			--Layering Buttons
			if p2 >= 3 and p2 <= 9 and p3 >= 3 and p3 <= 9 then
				layering = "up"
			elseif p2 >= 12 and p2 <= 18 and p3 >= 3 and p3 <= 9 then
				layering = "forward"
			--Count/Slot
			elseif p2 >= w - #msg - 3 and p2 <= w - 3 and p3 == 1 then
				requirementsDisplayed = not requirementsDisplayed
			--Printer ID
			elseif p2 >= 33 and p2 <= 33 + #printerNames[selectedPrinter] and p3 == 3 and #printerList > 1 then
				local chosenName = displayDropDown(33, 3, printerNames)
				for i=1,#printerNames do
					if printerNames[i] == chosenName then
						selectedPrinter = i
						break;
					end
				end
			--Print and Cancel
			elseif p2 >= 25 and p2 <= 32 and p3 == 10 then
				break
			elseif p2 >= 40 and p2 <= 46 and p3 == 10 then
				rednet.send(printerList[selectedPrinter], "$3DPRINT ACTIVATE")
				ready = false
				while true do
					local id,msg = rsTimeReceive(10)
					
					if id == printerList[selectedPrinter] and msg == "$3DPRINT ACTACK" then
						ready = true
						break
					end
				end
				if ready then
					performPrint()
					break
				else
					displayConfirmDialogue("Printer Didn't Respond", "The printer didn't respond to the activation command. Check to see if it's online")
				end
			end
		end
	end
	state = "paint"
end

--[[This function changes the current paint program to another tool or mode, depending on user input. Handles
	any necessary changes in logic involved in that.
	Params: mode:string = the name of the mode to change to
	Returns:nil
]]--
local function performSelection(mode)
	if not mode or mode == "" then return
	
	elseif mode == "help" then
		drawHelpScreen()
		
	elseif mode == "blueprint on" then
		blueprint = true
		ddModes[2][3] = "blueprint off"
		
	elseif mode == "blueprint off" then
		blueprint = false
		ddModes[2][3] = "blueprint on"
		
	elseif mode == "layers on" then
		layerDisplay = true
		ddModes[2][4] = "layers off"
	
	elseif mode == "layers off" then
		layerDisplay = false
		ddModes[2][4] = "layers on"
	
	elseif mode == "direction on" then
		printDirection = true
		ddModes[2][5] = "direction off"
		
	elseif mode == "direction off" then
		printDirection = false
		ddModes[2][5] = "direction on"
	
	elseif mode == "go to" then
		changeFrame()
	
	elseif mode == "remove" then
		removeFramesAfter(sFrame)
	
	elseif mode == "play" then
		playAnimation()
		
	elseif mode == "copy" then
		if selectrect and selectrect.x1 ~= selectrect.x2 then
			copyToBuffer(false)
		end
	
	elseif mode == "cut" then
		if selectrect and selectrect.x1 ~= selectrect.x2 then 
			copyToBuffer(true)
		end
		
	elseif mode == "paste" then
		if selectrect and selectrect.x1 ~= selectrect.x2 then 
			copyFromBuffer(false)
		end
		
	elseif mode == "hide" then
		selectrect = nil
		if state == "select" then state = "corner select" end
		
	elseif mode == "alpha to left" then
		if lSel then alphaC = lSel end
		
	elseif mode == "alpha to right" then
		if rSel then alphaC = rSel end
		
	elseif mode == "record" then
		record = not record
		
	elseif mode == "clear" then
		if state=="select" then buffer = nil
		else clearImage() end
	
	elseif mode == "select" then
		if state=="corner select" or state=="select" then
			state = "paint"
		elseif selectrect and selectrect.x1 ~= selectrect.x2 then
			state = "select"
		else
			state = "corner select" 
		end
		
	elseif mode == "print" then
		state = "print"
		runPrintInterface()
		state = "paint"
		
	elseif mode == "save" then
		if animated then saveNFA(sPath)
		elseif textEnabled then saveNFT(sPath)
		else saveNFP(sPath) end
		
	elseif mode == "exit" then
		isRunning = false
	
	elseif mode ~= state then state = mode
	else state = "paint"
	
	end
end

--[[The main function of the program, reads and handles all events and updates them accordingly. Mode changes,
	painting to the canvas and general selections are done here.
	Params: none
	Returns:nil
]]--
local function handleEvents()
	recttimer = os.startTimer(0.5)
	while isRunning do
		drawCanvas()
		drawInterface()
		
		if state == "text" then
			term.setCursorPos(textCurX - sx, textCurY - sy)
			term.setCursorBlink(true)
		end
		
		local id,p1,p2,p3 = os.pullEvent()
			term.setCursorBlink(false)
		if id=="timer" then
			updateTimer(p1)
		elseif id=="mouse_click" or id=="mouse_drag" then
			if p2 >=w-1 and p3 < #column+1 then
				if p1==1 then lSel = column[p3]
				else rSel = column[p3] end
			elseif p2 >=w-1 and p3==#column+1 then
				if p1==1 then lSel = nil
				else rSel = nil end
			elseif p2==w-1 and p3==h and animated then
				changeFrame(sFrame-1)
			elseif p2==w and p3==h and animated then
				changeFrame(sFrame+1)
			elseif p2 < w-10 and p3==h then
				local sel = displayDropDown(1, h-1, ddModes)
				performSelection(sel)
			elseif p2 < w-1 and p3 <= h-1 then
				if state=="pippette" then
					if p1==1 then
						if frames[sFrame][p3+sy] and frames[sFrame][p3+sy][p2+sx] then
							lSel = frames[sFrame][p3+sy][p2+sx] 
						end
					elseif p1==2 then
						if frames[sFrame][p3+sy] and frames[sFrame][p3+sy][p2+sx] then
							rSel = frames[sFrame][p3+sy][p2+sx] 
						end
					end
				elseif state=="move" then
					updateImageLims(record)
					moveImage(p2,p3)
				elseif state=="flood" then
					if p1 == 1 and lSel and frames[sFrame][p3+sy]  then 
						floodFill(p2,p3,frames[sFrame][p3+sy][p2+sx],lSel)
					elseif p1 == 2 and rSel and frames[sFrame][p3+sy] then 
						floodFill(p2,p3,frames[sFrame][p3+sy][p2+sx],rSel)
					end
				elseif state=="corner select" then
					if not selectrect then
						selectrect = { x1=p2+sx, x2=p2+sx, y1=p3+sy, y2=p3+sy }
					elseif selectrect.x1 ~= p2+sx and selectrect.y1 ~= p3+sy then
						if p2+sx<selectrect.x1 then selectrect.x1 = p2+sx
						else selectrect.x2 = p2+sx end
						
						if p3+sy<selectrect.y1 then selectrect.y1 = p3+sy
						else selectrect.y2 = p3+sy end
						
						state = "select"
					end
				elseif state=="textpaint" then
					local paintCol = lSel
					if p1 == 2 then paintCol = rSel end
					if frames[sFrame].textcol[p3+sy] then
						frames[sFrame].textcol[p3+sy][p2+sx] = paintCol
					end
				elseif state=="text" then
					textCurX = p2 + sx
					textCurY = p3 + sy
				elseif state=="select" then
					if p1 == 1 then
						local swidth = selectrect.x2 - selectrect.x1
						local sheight = selectrect.y2 - selectrect.y1
					
						selectrect.x1 = p2 + sx
						selectrect.y1 = p3 + sy
						selectrect.x2 = p2 + swidth + sx
						selectrect.y2 = p3 + sheight + sy
					elseif p1 == 2 and p2 < w-2 and p3 < h-1 then
						inMenu = true
						local sel = displayDropDown(p2, p3, srModes) 
						inMenu = false
						performSelection(sel)
					end
				else
					local f,l = sFrame,sFrame
					if record then f,l = 1,framecount end
					local bwidth = 0
					if state == "brush" then bwidth = brushsize-1 end
				
					for i=f,l do
						for x = math.max(1,p2+sx-bwidth),p2+sx+bwidth do
							for y = math.max(1,p3+sy-bwidth), p3+sy+bwidth do
								if math.abs(x - (p2+sx)) + math.abs(y - (p3+sy)) <= bwidth then
									if not frames[i][y] then frames[i][y] = {} end
									if p1==1 then frames[i][y][x] = lSel
									else frames[i][y][x] = rSel end
									
									if textEnabled then
										if not frames[i].text[y] then frames[i].text[y] = { } end
										if not frames[i].textcol[y] then frames[i].textcol[y] = { } end
									end
								end
							end
						end
					end
				end
			end
		elseif id=="char" then
			if state=="text" then
				if not frames[sFrame][textCurY] then frames[sFrame][textCurY] = { } end
				if not frames[sFrame].text[textCurY] then frames[sFrame].text[textCurY] = { } end
				if not frames[sFrame].textcol[textCurY] then frames[sFrame].textcol[textCurY] = { } end
				
				if rSel then frames[sFrame][textCurY][textCurX] = rSel end
				if lSel then 
					frames[sFrame].text[textCurY][textCurX] = p1
					frames[sFrame].textcol[textCurY][textCurX] = lSel
				else
					frames[sFrame].text[textCurY][textCurX] = " "
					frames[sFrame].textcol[textCurY][textCurX] = rSel
				end
				
				textCurX = textCurX+1
				if textCurX > w + sx - 2 then sx = textCurX - w + 2 end
			elseif tonumber(p1) then
				if state=="brush" and tonumber(p1) > 1 then
					brushsize = tonumber(p1)
				elseif animated and tonumber(p1) > 0 then
					changeFrame(tonumber(p1))
				end
			end
		elseif id=="key" then
			--Text needs special handlers (all other keyboard shortcuts are of course reserved for typing)
			if state=="text" then
				if p1==keys.backspace and textCurX > 1 then
					textCurX = textCurX-1
					if frames[sFrame].text[textCurY] then
						frames[sFrame].text[textCurY][textCurX] = nil
						frames[sFrame].textcol[textCurY][textCurX] = nil
					end
					if textCurX < sx then sx = textCurX end
				elseif p1==keys.left and textCurX > 1 then
					textCurX = textCurX-1
					if textCurX-1 < sx then sx = textCurX-1 end
				elseif p1==keys.right then
					textCurX = textCurX+1
					if textCurX > w + sx - 2 then sx = textCurX - w + 2 end
				elseif p1==keys.up and textCurY > 1 then
					textCurY = textCurY-1
					if textCurY-1 < sy then sy = textCurY-1 end
				elseif p1==keys.down then
					textCurY = textCurY+1
					if textCurY > h + sy - 1 then sy = textCurY - h + 1 end
				end
			
			elseif p1==keys.leftCtrl then
				local sel = displayDropDown(1, h-1, ddModes[#ddModes]) 
				performSelection(sel)
			elseif p1==keys.leftAlt then
				local sel = displayDropDown(1, h-1, ddModes[1]) 
				performSelection(sel)
			elseif p1==keys.h then 
				performSelection("help")
			elseif p1==keys.x then 
				performSelection("cut")
			elseif p1==keys.c then
				performSelection("copy")
			elseif p1==keys.v then
				performSelection("paste")
			elseif p1==keys.z then
				performSelection("clear")
			elseif p1==keys.s then
				performSelection("select")
			elseif p1==keys.tab then
				performSelection("hide")
			elseif p1==keys.q then
				performSelection("alpha to left")
			elseif p1==keys.w then
				performSelection("alpha to right")
			elseif p1==keys.f then
				performSelection("flood")
			elseif p1==keys.b then
				performSelection("brush")
			elseif p1==keys.m then
				performSelection("move")
			elseif p1==keys.backslash and animated then
				performSelection("record")
			elseif p1==keys.p then
				performSelection("pippette")
			elseif p1==keys.g and animated then
				performSelection("go to")
			elseif p1==keys.period and animated then
				changeFrame(sFrame+1)
			elseif p1==keys.comma and animated then
				changeFrame(sFrame-1)
			elseif p1==keys.r and animated then
				performSelection("remove")
			elseif p1==keys.space and animated then
				performSelection("play")
			elseif p1==keys.t and textEnabled then
				performSelection("text")
				sleep(0.01)
			elseif p1==keys.y and textEnabled then
				performSelection("textpaint")
			elseif p1==keys.left then
				if state == "move" and toplim then
					updateImageLims(record)
					if toplim and leflim then
						moveImage(leflim-1,toplim)
					end
				elseif state=="select" and selectrect.x1 > 1 then
					selectrect.x1 = selectrect.x1-1
					selectrect.x2 = selectrect.x2-1
				elseif sx > 0 then sx=sx-1 end
			elseif p1==keys.right then
				if state == "move" then
					updateImageLims(record)
					if toplim and leflim then
						moveImage(leflim+1,toplim)
					end
				elseif state=="select" then
					selectrect.x1 = selectrect.x1+1
					selectrect.x2 = selectrect.x2+1
				else sx=sx+1 end
			elseif p1==keys.up then
				if state == "move" then
					updateImageLims(record)
					if toplim and leflim then
						moveImage(leflim,toplim-1)
					end
				elseif state=="select" and selectrect.y1 > 1 then
					selectrect.y1 = selectrect.y1-1
					selectrect.y2 = selectrect.y2-1
				elseif sy > 0 then sy=sy-1 end
			elseif p1==keys.down then 
				if state == "move" then
					updateImageLims(record)
					if toplim and leflim then
						moveImage(leflim,toplim+1)
					end
				elseif state=="select" then
					selectrect.y1 = selectrect.y1+1
					selectrect.y2 = selectrect.y2+1
				else sy=sy+1 end
			end
		end
	end
end

--[[
			Section: Main  
]]--

if not term.isColour() then
	printError("Requires an Advanced Computer")
	return
end

--Taken almost directly from edit (for consistency)
local tArgs = {...}

local ca = 1

if tArgs[ca] == "-a" then
	animated = true
	ca = ca + 1
end

if tArgs[ca] == "-t" then
	textEnabled = true
	ca = ca + 1
end

if #tArgs < ca then
	print("Usage: npaintpro [-a,-t] <path>")
	return
end

--Yeah you can't have animated text files YET... I haven't supported that, maybe later?
if animated and textEnabled then
	print("No support for animated text files- cannot have both -a and -t")
end

sPath = shell.resolve(tArgs[ca])
local bReadOnly = fs.isReadOnly(sPath)
if fs.exists(sPath) then
	if fs.isDir(sPath) then
		print("Cannot edit a directory.")
		return
	elseif string.find(sPath, ".nfp") ~= #sPath-3 and string.find(sPath, ".nfa") ~= #sPath-3 and
			string.find(sPath, ".nft") ~= #sPath-3 then
		print("Can only edit .nfp, .nft and .nfa files:",string.find(sPath, ".nfp"),#sPath-3)
		return
	end
	
	if string.find(sPath, ".nfa") == #sPath-3 then
		animated = true
	end
	
	if string.find(sPath, ".nft") == #sPath-3 then
		textEnabled = true
	end	
	
	if string.find(sPath, ".nfp") == #sPath-3 and animated then
		print("Convert to nfa? Y/N")
		if string.find(string.lower(io.read()), "y") then
			local nsPath = string.sub(sPath, 1, #sPath-1).."a"
			fs.move(sPath, nsPath)
			sPath = nsPath
		else
			animated = false
		end
	end
	
	--Again this is possible, I just haven't done it. Maybe I will?
	if textEnabled and (string.find(sPath, ".nfp") == #sPath-3 or string.find(sPath, ".nfa") == #sPath-3) then
		print("Cannot convert to nft")
	end
else
	if not animated and not textEnabled and string.find(sPath, ".nfp") ~= #sPath-3 then 
		sPath = sPath..".nfp"
	elseif animated and string.find(sPath, ".nfa") ~= #sPath-3 then 
		sPath = sPath..".nfa"
	elseif textEnabled and string.find(sPath, ".nft") ~= #sPath-3 then
		sPath = sPath..".nft"
	end
end 

drawLogo()
init()
handleEvents()

term.setBackgroundColour(colours.black)
shell.run("clear")