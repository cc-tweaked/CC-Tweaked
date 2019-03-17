--- Colours (for lovers of British spelling)
--
-- @see colors
-- @module colours

local colours = _ENV
for k,v in pairs(colors) do
	colours[k] = v
end

colours.grey = colors.gray
colours.gray = nil

colours.lightGrey = colors.lightGray
colours.lightGray = nil
