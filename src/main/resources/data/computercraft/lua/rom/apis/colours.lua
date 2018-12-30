-- Colours (for lovers of british spelling)
local colours = _ENV
for k,v in pairs(colors) do
	colours[k] = v
end

colours.grey = colors.gray
colours.gray = nil

colours.lightGrey = colors.lightGray
colours.lightGray = nil
