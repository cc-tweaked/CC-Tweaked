--[[
	GameUtil
	An API for drawing sprites and animations made in NPaintPro
	By NitrogenFingers
]]--


--The back buffer. Initialized as nil
local backbuffer = nil
--The bounds of the terminal the back buffer displays to
local tw,th = nil, nil

--[[Constructs a new buffer. This must be done before the buffer can written to.
	Params: terminal:?table = The function table to draw to a screen. By default (nil) this refers
			to the native terminal, but monitor displays can be passed through as well:
			local leftMonitor = peripherals.wrap("left")
			initializeBuffer(leftMonitor)
	Returns:boolean = True if the buffer was successfully initialized; false otherwise
]]--
function initializeBuffer(terminal)
	if not terminal then terminal = term end
	if not terminal.getSize then
		error("Parameter cannot be used to initialize the backbuffer.")
	end
	if not terminal.isColour() then
		error("Parameter does not represent an advanced computer.")
	end
	
	tw,th = terminal.getSize()
	backbuffer = { }
	for y=1,th do
		backbuffer[y] = { }
	end
	return true
end

--[[Will clear the buffer and reset to nil, or to a colour if provided
	Params: colour:?number = The colour to set the back buffer to
	Returns:nil
]]--
function clearBuffer(colour)
	if not backbuffer then
		error("Back buffer not yet initialized!")
	end	
	
	for y=1,#backbuffer do
		backbuffer[y] = { }
		if colour then
			for x=1,tw do
				backbuffer[y][x] = colour
			end
		end
	end
end

--[[Draws the given entity to the back buffer
	Params: entity:table = the entity to draw to the buffer
	Returns:nil
]]--
function writeToBuffer(entity)
	if not backbuffer then
		error("Back buffer not yet initialized!")
	end	
	
	local image = nil
	if entity.type == "animation" then
		image = entity.frames[entity.currentFrame]
	else
		image = entity.image
	end
	
	for y=1,image.dimensions.height do
		for x=1,image.dimensions.width do
			if image[y][x] then
				local xpos,ypos = x,y
				if entity.mirror.x then xpos = image.dimensions.width - x + 1 end
				if entity.mirror.y then ypos = image.dimensions.height - y + 1 end
				
				--If the YPos doesn't exist, no need to loop through the rest of X!
				--Don't you love optimization?
				if not backbuffer[entity.y + ypos - 1] then break end
				
				backbuffer[entity.y + ypos - 1][entity.x + xpos - 1] = image[y][x]
			end
		end
	end
end

--[[Draws the contents of the buffer to the screen. This will not clear the screen or the buffer.
	Params: terminal:table = the terminal to draw to
	Returns:nil
]]--
function drawBuffer(terminal)
	if not backbuffer then
		error("Back buffer not yet initialized!")
	end	
	if not terminal then terminal = term end
	if not terminal.setCursorPos or not terminal.setBackgroundColour or not terminal.write then
		error("Parameter cannot be used to initialize the backbuffer.")
	end
	if not terminal.isColour() then
		error("Parameter does not represent an advanced computer.")
	end
	
	for y=1,math.min(#backbuffer, th) do
		for x=1,tw do
			if backbuffer[y][x] then
				terminal.setCursorPos(x,y)
				terminal.setBackgroundColour(backbuffer[y][x])
				terminal.write(" ")
			end
		end
	end
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

--[[Converts every pixel of one colour in a given sprite to another colour
	Use for "reskinning". Uses OO function.
	Params: self:sprite = the sprite to reskin
			oldcol:number = the colour to replace
			newcol:number = the new colour
	Returns:nil
]]--
local function repaintS(self, oldcol, newcol)
	for y=1,self.image.bounds.height do
		for x=1, self.image.bounds.width do
			if self.image[y][x] == oldcol then
				self.image[y][x] = newcol
			end
		end
	end
end

--[[Converts every pixel of one colour in a given animation to another colour
	Use for "reskinning". Uses OO function.
	Params: self:animation = the animation to reskin
			oldcol:number = the colour to replace
			newcol:number = the new colour
	Returns:nil
]]--
local function repaintA(self, oldcol, newcol)
	for f=1,#self.frames do
		print(self.frames[f].bounds)
		for y=1,self.frames[f].bounds.height do
			for x=1, self.frames[f].bounds.width do
				if self.frames[f][y][x] == oldcol then
					self.frames[f][y][x] = newcol
				end
			end
		end
	end
end

--[[Prints the sprite on the screen
	Params: self:sprite = the sprite to draw
	Returns:nil
]]--
local function drawS(self)
	local image = self.image
	
	for y=1,image.dimensions.height do
		for x=1,image.dimensions.width do
			if image[y][x] then
				local xpos,ypos = x,y
				if self.mirror.x then xpos = image.dimensions.width - x + 1 end
				if self.mirror.y then ypos = image.dimensions.height - y + 1 end
				
				term.setBackgroundColour(image[y][x])
				term.setCursorPos(self.x + xpos - 1, self.y + ypos - 1)
				term.write(" ")
			end
		end
	end
end

--[[Prints the current frame of the animation on screen
	Params: self:anim = the animation to draw
			frame:?number = the specific frame to draw (default self.currentFrame)
	Returns:nil
]]--
local function drawA(self, frame)
	if not frame then frame = self.currentFrame end
	local image = self.frames[frame]

	for y=1,image.dimensions.height do
		for x=1,image.dimensions.width do
			if image[y][x] then
				local xpos,ypos = x,y
				if self.mirror.x then xpos = image.dimensions.width - x + 1 end
				if self.mirror.y then ypos = image.dimensions.height - y + 1 end
				
				term.setBackgroundColour(image[y][x])
				term.setCursorPos(self.x + xpos - 1, self.y + ypos - 1)
				term.write(" ")
			end
		end
	end
end

--[[Checks the animation timer provided to see whether or not the animation needs to be updated.
	If so, it makes the necessary change.
	Params: self:animation = the animation to be updated
			timerID:number = the ID of the most recent timer event
	Returns:bool = true if the animation was update; false otherwise
]]--
local function updateA(self, timerID)
	if self.timerID and timerID and self.timerID == timerID then
		self.currentFrame = self.currentFrame + 1
		if self.currentFrame > self.upperBound then
			self.currentFrame = self.lowerBound
		end
		return true
	else
		return false
	end
end

--[[Moves immediately to the next frame in the sequence, as though an update had been called.
	Params: self:animation = the animation to update
	Returns:nil
]]--
local function nextA(self)
	self.currentFrame = self.currentFrame + 1
	if self.currentFrame > self.upperBound then
		self.currentFrame = self.lowerBound
	end
end

--[[Moves immediately to the previous frame in the sequence
	Params: self:animation = the animation to update
	Returns:nil
]]--
local function previousA(self)
	self.currentFrame = self.currentFrame - 1
	if self.currentFrame < self.lowerBound then
		self.currentFrame = self.upperBound
	end
end

--[[A simple debug function that displays the outline of the bounds
	on a given shape. Useful when testing collision detection or other game
	features.
	Params: entity:table = the bounded entity to represent
			colour:?number = the colour to draw the rectangle (default red)
	Returns:nil
]]--
local function drawBounds(entity, colour)
	if not colour then colour = colours.red end
	local image = nil
	if entity.type == "animation" then image = entity.frames[entity.currentFrame]
	else image = entity.image end
	
	term.setBackgroundColour(colour)
	
	corners = {
		topleft = { x = entity.x + image.bounds.x - 1, y = entity.y + image.bounds.y - 1 };
		topright = { x = entity.x + image.bounds.x + image.bounds.width - 2, y = entity.y + image.bounds.y - 1 };
		botleft = { x = entity.x + image.bounds.x - 1, y = entity.y + image.bounds.y + image.bounds.height - 2 };
		botright = { x = entity.x + image.bounds.x + image.bounds.width - 2, y = entity.y + image.bounds.y + image.bounds.height - 2 };
	}
	
	term.setCursorPos(corners.topleft.x, corners.topleft.y)
	term.write(" ")
	term.setCursorPos(corners.topright.x, corners.topright.y)
	term.write(" ")
	term.setCursorPos(corners.botleft.x, corners.botleft.y)
	term.write(" ")
	term.setCursorPos(corners.botright.x, corners.botright.y)
	term.write(" ")
end

--[[Creates a bounding rectangle object. Used in drawing the bounds and the rCollidesWith methods
	Params: self:table = the entity to create the rectangle
	Returns:table = the left, right, top and bottom edges of the rectangle
]]--
local function createRectangle(entity)
	local image = nil
	if entity.type == "animation" then
		image = entity.frames[entity.currentFrame]
	else
		image = entity.image
	end
	--Note that the origin is always 1, so we subtract 1 for every absolute coordinate we have to test.
	return {
		left = entity.x + image.bounds.x - 1;
		right = entity.x + image.bounds.x + image.bounds.width - 2;
		top = entity.y + image.bounds.y - 1;
		bottom = entity.y + image.bounds.y + image.bounds.height - 2;
	}
end

--[[Performs a rectangle collision with another given entity. Entity can be of sprite or animation
	type (also true of the self). Bases collision using a least squared approach (rectangle precision).
	Params: self:sprite,animation = the object in question of the testing
			other:sprite,animation = the other object tested for collision
	Returns:bool = true if bounding rectangle intersect is true; false otherwse
]]--
local function rCollidesWith(self, other)
	--First we construct the rectangles
	local img1C, img2C = createRectangle(self), createRectangle(other)
	
	--We then determine the "relative position" , in terms of which is farther left or right
	leftmost,rightmost,topmost,botmost = nil,nil,nil,nil
	if img1C.left < img2C.left then
		leftmost = img1C
		rightmost = img2C
	else
		leftmost = img2C
		rightmost = img1C
	end
	if img1C.top < img2C.top then
		topmost = img1C
		botmost = img2C
	else
		topmost = img2C
		botmost = img1C
	end
	
	--Then we determine the distance between the "extreme" edges-
		--distance between leftmost/right edge and rightmost/left edge
		--distance between topmost/bottom edge and bottommost/top edge
	local xdist = rightmost.left - leftmost.right
	local ydist = botmost.top - topmost.bottom
	
	--If both are negative, our rectangles intersect!
	return xdist <= 0 and ydist <= 0
end

--[[Performs a pixel collision test on another given entity. Either entity can be of sprite or animation
	type. This is done coarsegrain-finegrain, we first find the intersection between the rectangles
	(if there is one), and then test the space within that intersection for any intersecting pixels.
	Params: self:sprite,animation = the object in question of the testing
			other:sprite,animation = the other object being tested for collision
	Returns:?number,?number: The X and Y position in which the collision occurred.
]]--
local function pCollidesWith(self, other)
	--Identically to rCollidesWith, we create our rectangles...
	local img1C, img2C = createRectangle(self), createRectangle(other)
	--We'll also need the images to compare pixels later
	local img1, img2 = nil,nil
	if self.type == "animation" then img1 = self.frames[self.currentFrame]
	else img1 = self.image end
	if other.type == "animation" then img2 = other.frames[other.currentFrame]
	else img2 = other.image end
	
	--...then we position them...
	leftmost,rightmost,topmost,botmost = nil,nil,nil,nil
	--We also keep track of which is left and which is right- it doesn't matter in a rectangle
	--collision but it does in a pixel collision.
	img1T,img2T = {},{}
	
	if img1C.left < img2C.left then
		leftmost = img1C
		rightmost = img2C
		img1T.left = true
	else
		leftmost = img2C
		rightmost = img1C
		img2T.left = true
	end
	if img1C.top < img2C.top then
		topmost = img1C
		botmost = img2C
		img1T.top = true
	else
		topmost = img2C
		botmost = img1C
		img2T.top = true
	end
	
	--...and we again find the distances between the extreme edges.
	local xdist = rightmost.left - leftmost.right
	local ydist = botmost.top - topmost.bottom
	
	--If these distances are > 0 then we stop- no need to go any farther.
	if xdist > 0 or ydist > 0 then return false end
	
	
	for x = rightmost.left, rightmost.left + math.abs(xdist) do
		for y = botmost.top, botmost.top + math.abs(ydist) do
			--We know a collision has occurred if a pixel is occupied by both images. We do this by
			--first transforming the coordinates based on which rectangle is which, then testing if a
			--pixel is at that point
				-- The leftmost and topmost takes the distance on x and y and removes the upper component
				-- The rightmost and bottommost, being the farther extremes, compare from 1 upwards
			local testX,testY = 1,1
			if img1T.left then testX = x - img1C.left + 1
			else testX = x - img1C.left + 1 end
			if img1T.top then testY = y - img1C.top + 1
			else testY = y - img1C.top + 1 end
			
			local occupy1 = img1[testY + img1.bounds.y-1][testX + img1.bounds.x-1] ~= nil
			
			if img2T.left then testX = x - img2C.left + 1
			else testX = x - img2C.left + 1 end
			if img2T.top then testY = y - img2C.top + 1
			else testY = y - img2C.top + 1 end
			
			local occupy2 = img2[testY + img2.bounds.y-1][testX + img2.bounds.x-1] ~= nil
			
			if occupy1 and occupy2 then return true end
		end
	end
	--If the looop terminates without returning, then no pixels overlap
	return false
end

--[[Moves the sprite or animation to the specified coordinates. This performs the auto-centering, so
	the user doesn't have to worry about adjusting for the bounds of the shape. Recommended for absolute
	positioning operations (as relative direct access to the X will have unexpected results!)
	Params: self:table = the animation or sprite to move
	x:number = the new x position
	y:number = the new y position
]]--
local function moveTo(self, x, y)
	local image = nil
	if self.type == "animation" then
		image = self.frames[self.currentFrame]
	else
		image = self.image
	end
	
	self.x = x - image.bounds.x + 1
	self.y = y - image.bounds.y + 1
end

--[[
	Sprites Fields:
x:number = the x position of the sprite in the world
y:number = the y position of the sprite in the world
image:table = a table of the image. Indexed by height, a series of sub-tables, each entry being a pixel
		at [y][x]. It also contains:
	bounds:table =
		x:number = the relative x position of the bounding rectangle
		y:number = the relative y position of the bounding rectangle
		width:number = the width of the bounding rectangle
		height:number = the height of the bounding rectangle
	dimensions:table =
		width = the width of the entire image in pixels
		height = the height of the entire image in pixels
		
mirror:table =
	x:bool = whether or not the image is mirrored on the X axis
	y:bool = whether or not the image is mirrored on the Y axis
repaint:function = see repaintS (above)
rCollidesWith:function = see rCollidesWith (above)
pCollidesWith:function = see pCollidesWith (above)
draw:function = see drawS (above)
]]--

--[[Loads a new sprite into a table, and returns it to the user.
	Params: path:string = the absolute path to the desired sprite
	x:number = the initial X position of the sprite
	y:number = the initial Y position of the sprite
]]--
function loadSprite(path, x, y)
	local sprite = { 
		type = "sprite",
		x = x,
		y = y,
		image = { },
		mirror = { x = false, y = false }
	}
	
	if fs.exists(path) then
		local file = io.open(path, "r" )
		local leftX, rightX = math.huge, 0
		local topY, botY = nil,nil
		
		local lcount = 0
		for line in file:lines() do
			lcount = lcount+1
			table.insert(sprite.image, {})
			for i=1,#line do
				if string.sub(line, i, i) ~= " " then
					leftX = math.min(leftX, i)
					rightX = math.max(rightX, i)
					if not topY then topY = lcount end
					botY = lcount
				end
				sprite.image[#sprite.image][i] = getColourOf(string.sub(line,i,i))
			end
		end
		file:close()
		
		sprite.image.bounds = {
			x = leftX,
			width = rightX - leftX + 1,
			y = topY,
			height = botY - topY + 1
		}
		sprite.image.dimensions = {
			width = rightX,
			height = botY
		}
		
		sprite.x = sprite.x - leftX + 1
		sprite.y = sprite.y - topY + 1
		
		sprite.repaint = repaintS
		sprite.rCollidesWith = rCollidesWith
		sprite.pCollidesWith = pCollidesWith
		sprite.draw = drawS
		sprite.moveTo = moveTo
		return sprite
	else
		error(path.." not found!")
	end
end

--Animations contain
	--Everything a sprite contains, but the image is a series of frames, not just one image
	--An timerID that tracks the last animation
	--An upper and lower bound on the active animation
	--An update method that takes a timer event and updates the animation if necessary

--[[

]]--
function loadAnimation(path, x, y, currentFrame)
	local anim = {
		type = "animation",
		x = x,
		y = y,
		frames = { },
		mirror = { x = false, y = false },
		currentFrame = currentFrame
	}
	
	table.insert(anim.frames, { })
	if fs.exists(path) then
		local file = io.open(path, "r")
		local leftX, rightX = math.huge, 0
		local topY, botY = nil,nil
		
		local lcount = 0
		for line in file:lines() do
			lcount = lcount+1
			local cFrame = #anim.frames
			if line == "~" then
				anim.frames[cFrame].bounds = {
					x = leftX,
					y = topY,
					width = rightX - leftX + 1,
					height = botY - topY + 1
				}
				anim.frames[cFrame].dimensions = {
					width = rightX,
					height = botY
				}
				table.insert(anim.frames, { })
				leftX, rightX = math.huge, 0
				topY, botY = nil,nil
				lcount = 0
			else
				table.insert(anim.frames[cFrame], {})
				for i=1,#line do
					if string.sub(line, i, i) ~= " " then
						leftX = math.min(leftX, i)
						rightX = math.max(rightX, i)
						if not topY then topY = lcount end
						botY = lcount
					end
					anim.frames[cFrame][#anim.frames[cFrame]] [i] = getColourOf(string.sub(line,i,i))
				end
			end
		end
		file:close()
		local cFrame = #anim.frames
		anim.frames[cFrame].bounds = {
			x = leftX,
			y = topY,
			width = rightX - leftX + 1,
			height = botY - topY + 1
		}
		anim.frames[cFrame].dimensions = {
			width = rightX,
			height = botY
		}
		anim.x = anim.x - leftX + 1
		anim.y = anim.y - topY + 1
		
		if not currentFrame or type(currentFrame) ~= "number" or currentFrame < 1 or 
				currentFrame > #anim.frames then 
			anim.currentFrame = 1 
		end
	
		anim.timerID = nil
		anim.lowerBound = 1
		anim.upperBound = #anim.frames
		anim.updating = false
	
		anim.repaint = repaintA
		anim.rCollidesWith = rCollidesWith
		anim.pCollidesWith = pCollidesWith
		anim.draw = drawA
		anim.update = updateA
		anim.next = nextA
		anim.previous = previousA
		anim.moveTo = moveTo
		return anim
	else
		error(path.." not found!")
	end
end