-- TODO: Do we really need to do this twice? What's the best way of approaching
-- this?

function write(text) end
function scroll(lines) end
function setCursorPos(x, y) end
function setCursorBlink(blink) end
function getCursorPos() end
function getSize() end
function clear() end
function clearLine() end
function setTextColour(colour) end
function setTextColor(color) end
function setBackgroundColour(colour) end
function setBackgroundColor(color) end
function isColour() end
function isColor() end
function getTextColour() end
function getTextColor() end
function getBackgroundColour() end
function getBackgroundColor() end
function blit(text, text_colours, background_colours) end
function setPaletteColour(colour, ...) end
function setPaletteColor(colour, ...) end
function getPaletteColour(colour, ...) end
function getPaletteColor(colour) end
function nativePaletteColour(colour) end
function nativePaletteColor(colour) end

--- @type Redirect
local Redirect = {}

function Redirect.write(text) end
function Redirect.scroll(lines) end
function Redirect.setCursorPos(x, y) end
function Redirect.setCursorBlink(blink) end
function Redirect.getCursorPos() end
function Redirect.getSize() end
function Redirect.clear() end
function Redirect.clearLine() end
function Redirect.setTextColour(colour) end
function Redirect.setTextColor(color) end
function Redirect.setBackgroundColour(colour) end
function Redirect.setBackgroundColor(color) end
function Redirect.isColour() end
function Redirect.isColor() end
function Redirect.getTextColour() end
function Redirect.getTextColor() end
function Redirect.getBackgroundColour() end
function Redirect.getBackgroundColor() end
function Redirect.blit(text, text_colours, background_colours) end
function Redirect.setPaletteColour(colour, ...) end
function Redirect.setPaletteColor(colour, ...) end
function Redirect.getPaletteColour(colour, ...) end
function Redirect.getPaletteColor(colour) end
