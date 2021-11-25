local result = table.pack(__expr__
)

if result.n == 0 then return end

local pp = require "cc.pretty"

local line = {}
for i = 1, result.n do
    if i > 1 then line[#line + 1] = pp.text(", ") end
    line[#line + 1] = pp.pretty(result[i])
end

pp.print(pp.concat(table.unpack(line)))
