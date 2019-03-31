function new(_sizeX, _sizeY, _color)
	local redirect = {buffer = {text = {}, textColor = {}, backColor = {}, cursorX = 1, cursorY = 1, cursorBlink = false, curTextColor = "0", curBackColor = "f", sizeX = _sizeX or 51, sizeY = _sizeY or 19, color = _color}}
	redirect.write = function(text)
		text = tostring(text)
		local pos = redirect.buffer.cursorX
		if redirect.buffer.cursorY > redirect.buffer.sizeY or redirect.buffer.cursorY < 1 then
			redirect.buffer.cursorX = pos + #text
			return
		end
		local writeText
		if pos + #text <= 1 then
			--skip entirely.
			redirect.buffer.cursorX = pos + #text
			return
		elseif pos < 1 then
			--adjust text to fit on screen starting at one.
			writeText = string.sub(text, math.abs(redirect.buffer.cursorX) + 2)
			redirect.buffer.cursorX = 1
		elseif pos > redirect.buffer.sizeX then
			--if we're off the edge to the right, skip entirely.
			redirect.buffer.cursorX = pos + #text
			return
		else
			writeText = text
		end
		local lineText = redirect.buffer.text[redirect.buffer.cursorY]
		local lineColor = redirect.buffer.textColor[redirect.buffer.cursorY]
		local lineBack = redirect.buffer.backColor[redirect.buffer.cursorY]
		local preStop = redirect.buffer.cursorX - 1
		local preStart = math.min(1, preStop)
		local postStart = redirect.buffer.cursorX + string.len(writeText)
		local postStop = redirect.buffer.sizeX
		redirect.buffer.text[redirect.buffer.cursorY] = string.sub(lineText, preStart, preStop)..writeText..string.sub(lineText, postStart, postStop)
		redirect.buffer.textColor[redirect.buffer.cursorY] = string.sub(lineColor, preStart, preStop)..string.rep(redirect.buffer.curTextColor, #writeText)..string.sub(lineColor, postStart, postStop)
		redirect.buffer.backColor[redirect.buffer.cursorY] = string.sub(lineBack, preStart, preStop)..string.rep(redirect.buffer.curBackColor, #writeText)..string.sub(lineBack, postStart, postStop)
		redirect.buffer.cursorX = pos + string.len(text)
	end
	redirect.clear = function()
		for i=1, redirect.buffer.sizeY do
			redirect.buffer.text[i] = string.rep(" ", redirect.buffer.sizeX)
			redirect.buffer.textColor[i] = string.rep(redirect.buffer.curTextColor, redirect.buffer.sizeX)
			redirect.buffer.backColor[i] = string.rep(redirect.buffer.curBackColor, redirect.buffer.sizeX)
		end
	end
	redirect.clearLine = function()
		redirect.buffer.text[redirect.buffer.cursorY] = string.rep(" ", redirect.buffer.sizeX)
		redirect.buffer.textColor[redirect.buffer.cursorY] = string.rep(redirect.buffer.curTextColor, redirect.buffer.sizeX)
		redirect.buffer.backColor[redirect.buffer.cursorY] = string.rep(redirect.buffer.curBackColor, redirect.buffer.sizeX)
	end
	redirect.getCursorPos = function()
		return redirect.buffer.cursorX, redirect.buffer.cursorY
	end
	redirect.setCursorPos = function(x, y)
		redirect.buffer.cursorX = math.floor(tonumber(x)) or redirect.buffer.cursorX
		redirect.buffer.cursorY = math.floor(tonumber(y)) or redirect.buffer.cursorY
	end
	redirect.setCursorBlink = function(b)
		redirect.buffer.cursorBlink = b
	end
	redirect.getSize = function()
		return redirect.buffer.sizeX, redirect.buffer.sizeY
	end
	redirect.scroll = function(n)
		n = tonumber(n) or 1
		if n > 0 then
			for i = 1, redirect.buffer.sizeY - n do
				if redirect.buffer.text[i + n] then
					redirect.buffer.text[i] = redirect.buffer.text[i + n]
					redirect.buffer.textColor[i] = redirect.buffer.textColor[i + n]
					redirect.buffer.backColor[i] = redirect.buffer.backColor[i + n]
				end
			end
			for i = redirect.buffer.sizeY, redirect.buffer.sizeY - n + 1, -1 do
				redirect.buffer.text[i] = string.rep(" ", redirect.buffer.sizeX)
				redirect.buffer.textColor[i] = string.rep(redirect.buffer.curTextColor, redirect.buffer.sizeX)
				redirect.buffer.backColor[i] = string.rep(redirect.buffer.curBackColor, redirect.buffer.sizeX)
			end
		elseif n < 0 then
			for i = redirect.buffer.sizeY, math.abs(n) + 1, -1 do
				if redirect.buffer.text[i + n] then
					redirect.buffer.text[i] = redirect.buffer.text[i + n]
					redirect.buffer.textColor[i] = redirect.buffer.textColor[i + n]
					redirect.buffer.backColor[i] = redirect.buffer.backColor[i + n]
				end
			end
			for i = 1, math.abs(n) do
				redirect.buffer.text[i] = string.rep(" ", redirect.buffer.sizeX)
				redirect.buffer.textColor[i] = string.rep(redirect.buffer.curTextColor, redirect.buffer.sizeX)
				redirect.buffer.backColor[i] = string.rep(redirect.buffer.curBackColor, redirect.buffer.sizeX)
			end
		end
	end
	redirect.setTextColor = function(clr)
		if clr and clr <= 32768 and clr >= 1 then
			if redirect.buffer.color then
				redirect.buffer.curTextColor = string.format("%x", math.floor(math.log(clr) / math.log(2)))
			elseif clr == 1 or clr == 32768 then
				redirect.buffer.curTextColor = string.format("%x", math.floor(math.log(clr) / math.log(2)))
			else
				return nil, "Colour not supported"
			end
		end
	end
	redirect.setTextColour = redirect.setTextColor
	redirect.setBackgroundColor = function(clr)
		if clr and clr <= 32768 and clr >= 1 then
			if redirect.buffer.color then
				redirect.buffer.curBackColor = string.format("%x", math.floor(math.log(clr) / math.log(2)))
			elseif clr == 32768 or clr == 1 then
				redirect.buffer.curBackColor = string.format("%x", math.floor(math.log(clr) / math.log(2)))
			else
				return nil, "Colour not supported"
			end
		end
	end
	redirect.setBackgroundColour = redirect.setBackgroundColor
	redirect.isColor = function()
		return redirect.buffer.color == true
	end
	redirect.isColour = redirect.isColor
	redirect.render = function(inputBuffer)
		for i = 1, redirect.buffer.sizeY do
			redirect.buffer.text[i] = inputBuffer.text[i]
			redirect.buffer.textColor[i] = inputBuffer.textColor[i]
			redirect.buffer.backColor[i] = inputBuffer.backColor[i]
		end
	end
	redirect.clear()
	return redirect
end

function draw(buffer, current)
	for i=1, buffer.sizeY do
		term.setCursorPos(1,i)
		if (current and (buffer.text[i] ~= current.text[i] or buffer.textColor[i] ~= current.textColor[i] or buffer.backColor[i] ~= current.backColor[i])) or not current then
			local lineEnd = false
			local offset = 1
			while not lineEnd do
				local textColorString = string.match(string.sub(buffer.textColor[i], offset), string.sub(buffer.textColor[i], offset, offset).."*")
				local backColorString = string.match(string.sub(buffer.backColor[i], offset), string.sub(buffer.backColor[i], offset, offset).."*")
				term.setTextColor(2 ^ tonumber(string.sub(textColorString, 1, 1), 16))
				term.setBackgroundColor(2 ^ tonumber(string.sub(backColorString, 1, 1), 16))
				term.write(string.sub(buffer.text[i], offset, offset + math.min(#textColorString, #backColorString) - 1))
				offset = offset + math.min(#textColorString, #backColorString)
				if offset > buffer.sizeX then lineEnd = true end
			end
			if current then
				current.text[i] = buffer.text[i]
				current.textColor[i] = buffer.textColor[i]
				current.backColor[i] = buffer.backColor[i]
			end
		end
	end
	term.setCursorPos(buffer.cursorX, buffer.cursorY)
	term.setTextColor(2 ^ tonumber(buffer.curTextColor, 16))
	term.setBackgroundColor(2 ^ tonumber(buffer.curBackColor, 16))
	term.setCursorBlink(buffer.cursorBlink)
	return current
end
