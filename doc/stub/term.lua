function write(text) end
function scroll(lines) end
function setCursorPos(x, y) end
function setCursorBlink(blink) end
function getCursorPos() end
function getSize() end
function clear() end
function clearLine() end
function setTextColour(colour) end
setTextColor = setTextColour
function setBackgroundColour(colour) end
setBackgroundColor = setBackgroundColour
function isColour() end
isColor = isColour
function getTextColour() end
getTextColor = getTextColor
function getBackgroundColour() end
getBackgroundColor = getBackgroundColour
function blit(text, text_colours, background_colours) end
function setPaletteColour(colour, ...) end
setPaletteColor = setPaletteColour
function getPaletteColour(colour, ...) end
getPaletteColor = getPaletteColour
function nativePaletteColour(colour) end
nativePaletteColor = nativePaletteColour

--- @type Redirect
local Redirect = {}

Redirect.write = write
Redirect.scroll = scroll
Redirect.setCursorPos = setCursorPos
Redirect.setCursorBlink = setCursorBlink
Redirect.getCursorPos = getCursorPos
Redirect.getSize = getSize
Redirect.clear = clear
Redirect.clearLine = clearLine
Redirect.setTextColour = setTextColour
Redirect.setTextColor = setTextColor
Redirect.setBackgroundColour = setBackgroundColour
Redirect.setBackgroundColor = setBackgroundColor
Redirect.isColour = isColour
Redirect.isColor = isColor
Redirect.getTextColour = getTextColour
Redirect.getTextColor = getTextColor
Redirect.getBackgroundColour = getBackgroundColour
Redirect.getBackgroundColor = getBackgroundColor
Redirect.blit = blit
Redirect.setPaletteColour = setPaletteColour
Redirect.setPaletteColor = setPaletteColor
Redirect.getPaletteColour = getPaletteColour
Redirect.getPaletteColor = getPaletteColor
