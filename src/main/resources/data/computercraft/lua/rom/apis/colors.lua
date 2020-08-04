--- The Colors API allows you to manipulate sets of colors.
--
-- This is useful in conjunction with Bundled Cables from the RedPower mod,
-- RedNet Cables from the MineFactory Reloaded mod, and colors on Advanced
-- Computers and Advanced Monitors.
--
-- For the non-American English version just replace @{colors} with @{colours}
-- and it will use the other API, colours which is exactly the same, except in
-- British English (e.g. @{colors.gray} is spelt @{colours.grey}).
--
-- @see colours
-- @module colors

local expect = dofile("rom/modules/main/cc/expect.lua").expect

--- White: Written as `0` in paint files and @{term.blit}, has a default
-- terminal colour of #F0F0F0.
white = 0x1

--- Orange: Written as `1` in paint files and @{term.blit}, has a
-- default terminal colour of #F2B233.
orange = 0x2

--- Magenta: Written as `2` in paint files and @{term.blit}, has a
-- default terminal colour of #E57FD8.
magenta = 0x4

--- Light blue: Written as `3` in paint files and @{term.blit}, has a
-- default terminal colour of #99B2F2.
lightBlue = 0x8

--- Yellow: Written as `4` in paint files and @{term.blit}, has a
-- default terminal colour of #DEDE6C.
yellow = 0x10

--- Lime: Written as `5` in paint files and @{term.blit}, has a default
-- terminal colour of #7FCC19.
lime = 0x20

--- Pink. Written as `6` in paint files and @{term.blit}, has a default
-- terminal colour of #F2B2CC.
pink = 0x40

--- Gray: Written as `7` in paint files and @{term.blit}, has a default
-- terminal colour of #4C4C4C.
gray = 0x80

--- Light gray: Written as `8` in paint files and @{term.blit}, has a
-- default terminal colour of #999999.
lightGray = 0x100

--- Cyan: Written as `9` in paint files and @{term.blit}, has a default
-- terminal colour of #4C99B2.
cyan = 0x200

--- Purple: Written as `a` in paint files and @{term.blit}, has a
-- default terminal colour of #B266E5.
purple = 0x400

--- Blue: Written as `b` in paint files and @{term.blit}, has a default
-- terminal colour of #3366CC.
blue = 0x800

--- Brown: Written as `c` in paint files and @{term.blit}, has a default
-- terminal colour of #7F664C.
brown = 0x1000

--- Green: Written as `d` in paint files and @{term.blit}, has a default
-- terminal colour of #57A64E.
green = 0x2000

--- Red: Written as `e` in paint files and @{term.blit}, has a default
-- terminal colour of #CC4C4C.
red = 0x4000

--- Black: Written as `f` in paint files and @{term.blit}, has a default
-- terminal colour of #191919.
black = 0x8000

--- Combines a set of colors (or sets of colors) into a larger set.
--
-- @tparam number ... The colors to combine.
-- @treturn number The union of the color sets given in `...`
-- @usage
-- ```lua
-- colors.combine(colors.white, colors.magenta, colours.lightBlue)
-- -- => 13
-- ```
function combine(...)
    local r = 0
    for i = 1, select('#', ...) do
        local c = select(i, ...)
        expect(i, c, "number")
        r = bit32.bor(r, c)
    end
    return r
end

--- Removes one or more colors (or sets of colors) from an initial set.
--
-- Each parameter beyond the first may be a single color or may be a set of
-- colors (in the latter case, all colors in the set are removed from the
-- original set).
--
-- @tparam number colors The color from which to subtract.
-- @tparam number ... The colors to subtract.
-- @treturn number The resulting color.
-- @usage
-- ```lua
-- colours.subtract(colours.lime, colours.orange, colours.white)
-- -- => 32
-- ```
function subtract(colors, ...)
    expect(1, colors, "number")
    local r = colors
    for i = 1, select('#', ...) do
        local c = select(i, ...)
        expect(i + 1, c, "number")
        r = bit32.band(r, bit32.bnot(c))
    end
    return r
end

--- Tests whether `color` is contained within `colors`.
--
-- @tparam number colors A color, or color set
-- @tparam number color A color or set of colors that `colors` should contain.
-- @treturn boolean If `colors` contains all colors within `color`.
-- @usage
-- ```lua
-- colors.test(colors.combine(colors.white, colors.magenta, colours.lightBlue), colors.lightBlue)
-- -- => true
-- ```
function test(colors, color)
    expect(1, colors, "number")
    expect(2, color, "number")
    return bit32.band(colors, color) == color
end

--- Combine a three-colour RGB value into one hexadecimal representation.
--
-- @tparam number r The red channel, should be between 0 and 1.
-- @tparam number g The red channel, should be between 0 and 1.
-- @tparam number b The blue channel, should be between 0 and 1.
-- @treturn number The combined hexadecimal colour.
-- @usage
-- ```lua
-- colors.rgb(0.7, 0.2, 0.6)
-- -- => 0xb23399
-- ```
function packRGB(r, g, b)
    expect(1, r, "number")
    expect(2, g, "number")
    expect(3, b, "number")
    return
        bit32.band(r * 255, 0xFF) * 2 ^ 16 +
        bit32.band(g * 255, 0xFF) * 2 ^ 8 +
        bit32.band(b * 255, 0xFF)
end

--- Separate a hexadecimal RGB colour into its three constituent channels.
--
-- @tparam number rgb The combined hexadecimal colour.
-- @treturn number The red channel, will be between 0 and 1.
-- @treturn number The red channel, will be between 0 and 1.
-- @treturn number The blue channel, will be between 0 and 1.
-- @usage
-- ```lua
-- colors.rgb(0xb23399)
-- -- => 0.7, 0.2, 0.6
-- ```
-- @see colors.packRGB
function unpackRGB(rgb)
    expect(1, rgb, "number")
    return
        bit32.band(bit32.rshift(rgb, 16), 0xFF) / 255,
        bit32.band(bit32.rshift(rgb, 8), 0xFF) / 255,
        bit32.band(rgb, 0xFF) / 255
end

--- Either calls @{colors.packRGB} or @{colors.unpackRGB}, depending on how many
-- arguments it receives.
--
-- @tparam[1] number r The red channel, as an argument to @{colors.packRGB}.
-- @tparam[1] number g The green channel, as an argument to @{colors.packRGB}.
-- @tparam[1] number b The blue channel, as an argument to @{colors.packRGB}.
-- @tparam[2] number rgb The combined hexadecimal color, as an argument to @{colors.unpackRGB}.
-- @treturn[1] number The combined hexadecimal colour, as returned by @{colors.packRGB}.
-- @treturn[2] number The red channel, as returned by @{colors.unpackRGB}
-- @treturn[2] number The green channel, as returned by @{colors.unpackRGB}
-- @treturn[2] number The blue channel, as returned by @{colors.unpackRGB}
-- @deprecated Use @{packRGB} or @{unpackRGB} directly.
-- @usage
-- ```lua
-- colors.rgb(0xb23399)
-- -- => 0.7, 0.2, 0.6
-- ```
-- @usage
-- ```lua
-- colors.rgb(0.7, 0.2, 0.6)
-- -- => 0xb23399
-- ```
function rgb8(r, g, b)
    if g == nil and b == nil then
        return unpackRGB(r)
    else
        return packRGB(r, g, b)
    end
end
