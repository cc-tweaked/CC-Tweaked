-- Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
--
-- SPDX-License-Identifier: LicenseRef-CCPL

-- Regular input
print("Redstone inputs: ")

local count = 0
local bundledCount = 0
for n,sSide in ipairs(redstone.getSides()) do
    if redstone.getBundledInput(sSide) > 0 then
        bundledCount = bundledCount + 1
    end
    if redstone.getInput(sSide) then
        if count > 0 then
            io.write(", ")
        end
        io.write(sSide)
        count = count + 1
    end
end

if count > 0 then
    print(".")
else
    print("None.")
end

-- Bundled input
if bundledCount > 0 then
    print()
    print("Bundled inputs:")
    for i,sSide in ipairs(redstone.getSides()) do
        if redstone.getBundledInput(sSide) > 0 then
            write(sSide.." = "..redstone.getBundledInput(sSide).." (")

            local count = 0
            for sColour,nColour in pairs(colors) do
                if type(nColour) == "number" and
                   redstone.testBundledInput(sSide, nColour) then
                    if count > 0 then
                        write(" + ")
                    end
                    if term.isColour() then
                        term.setTextColour(nColour)
                    end
                    write(sColour)
                    if term.isColour() then
                        term.setTextColour(colours.white)
                    end
                    count = count + 1
                end
            end
            print(")")
        end
    end
end
