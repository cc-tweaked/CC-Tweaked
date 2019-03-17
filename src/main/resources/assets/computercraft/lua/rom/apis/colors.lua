--- The Colors API allows you to manipulate sets of colors.
--
-- This is useful in conjunction with Bundled Cables from the RedPower
-- mod, RedNet Cables from the MineFactory Reloaded mod, and colors on
-- Advanced Computers and Advanced Monitors.
--
-- For the non-American English version just replace `colors` with
-- `colours` and it will use the other API, colours which is exactly the
-- same, except in British English (e.g. `colors.gray` is spelt
-- `colours.grey`).
--
-- @see colours
-- @module colors

--- White. 0x1 in hexadecimal, displayed as `0` in paint files and `term.blit`, has a default terminal colour of `#F0F0F0`.
white = 1

--- Orange. 0x2 in hexadecimal, displayed as `1` in paint files and `term.blit`, has a default terminal colour of `#F2B233`.
orange = 2

--- Magenta. 0x4 in hexadecimal, displayed as `2` in paint files and `term.blit`, has a default terminal colour of `#E57FD8`.
magenta = 4

--- Light blue. 0x8 in hexadecimal, displayed as `3` in paint files and `term.blit`, has a default terminal colour of `#99B2F2`.
lightBlue = 8

--- Yellow. 0x10 in hexadecimal, displayed as `4` in paint files and `term.blit`, has a default terminal colour of `#DEDE6C`.
yellow = 16

--- Lime. 0x20 in hexadecimal, displayed as `5` in paint files and `term.blit`, has a default terminal colour of `#7FCC19`.
lime = 32

--- Pink. 0x40 in hexadecimal, displayed as `6` in paint files and `term.blit`, has a default terminal colour of `#F2B2CC`.
pink = 64

--- Gray. 0x80 in hexadecimal, displayed as `7` in paint files and `term.blit`, has a default terminal colour of `#4C4C4C`.
gray = 128

--- Light gray. 0x100 in hexadecimal, displayed as `8` in paint files and `term.blit`, has a default terminal colour of `#999999`.
lightGray = 256

--- Cyan. 0x200 in hexadecimal, displayed as `9` in paint files and `term.blit`, has a default terminal colour of `#4C99B2`.
cyan = 512

--- Purple. 0x400 in hexadecimal, displayed as `a` in paint files and `term.blit`, has a default terminal colour of `#B266E5`.
purple = 1024

--- Blue. 0x800 in hexadecimal, displayed as `b` in paint files and `term.blit`, has a default terminal colour of `#3366CC`.
blue = 2048

--- Brown. 0x1000 in hexadecimal, displayed as `c` in paint files and `term.blit`, has a default terminal colour of `#7F664C`.
brown = 4096

--- Green. 0x2000 in hexadecimal, displayed as `d` in paint files and `term.blit`, has a default terminal colour of `#57A64E`.
green = 8192

--- Red. 0x4000 in hexadecimal, displayed as `e` in paint files and `term.blit`, has a default terminal colour of `#CC4C4C`.
red = 16384

--- Black. 0x8000 in hexadecimal, displayed as `f` in paint files and `term.blit`, has a default terminal colour of `#191919`.
black = 32768

--- Combines a set of colors (or sets of colors) into a larger set.
--
-- @tparam number ... The colors to combine.
-- @treturn number The union of the color sets given in `...`
-- @usage colors.combine(colors.white, colors.magenta, colours.lightBlue) == 13
function combine( ... )
    local r = 0
    for n,c in ipairs( { ... } ) do
        if type( c ) ~= "number" then
            error( "bad argument #"..n.." (expected number, got " .. type( c ) .. ")", 2 )
        end
        r = bit32.bor(r,c)
    end
    return r
end

--- Removes one or more colors (or sets of colors) from an initial set.
--
-- Each parameter beyond the first may be a single color or may be a set
-- of colors (in the latter case, all colors in the set are removed from
-- the original set).
--
-- @tparam number colors The color from which to subtract.
-- @tparam number ... The colors to subtract.
-- @treturn number The resulting color.
-- @usage colours.subtract(colours.lime, colours.orange, colours.white) == 32
function subtract( colors, ... )
    if type( colors ) ~= "number" then
        error( "bad argument #1 (expected number, got " .. type( colors ) .. ")", 2 )
    end
    local r = colors
    for n,c in ipairs( { ... } ) do
        if type( c ) ~= "number" then
            error( "bad argument #"..tostring( n+1 ).." (expected number, got " .. type( c ) .. ")", 2 )
        end
        r = bit32.band(r, bit32.bnot(c))
    end
    return r
end

--- Tests whether `color` is contained within `colors`.
--
-- @tparam number colors A color, or color set
-- @tparam number color A color or set of colors that `colors` should contain.
-- @treturn boolean If `colors` contains all colors within `color`.
-- @usage colors.test(colors.combine(colors.white, colors.magenta, colours.lightBlue), colors.lightBlue) == true
function test( colors, color )
    if type( colors ) ~= "number" then
        error( "bad argument #1 (expected number, got " .. type( colors ) .. ")", 2 )
    end
    if type( color ) ~= "number" then
        error( "bad argument #2 (expected number, got " .. type( color ) .. ")", 2 )
    end
    return bit32.band(colors, color) == color
end

--- Combine a three-colour RGB value into one hexadecimal representation.
--
-- @tparam number r The red channel, should be between 0 and 1.
-- @tparam number g The red channel, should be between 0 and 1.
-- @tparam number b The blue channel, should be between 0 and 1.
-- @treturn number The combined hexadecimal colour.
-- @usage colors.packRGB(0.7, 0.2, 0.6) == 0xb23399
-- @see colors.unpackRGB
function packRGB( r, g, b )
    if type( r ) ~= "number" then
        error( "bad argument #1 (expected number, got " .. type( r ) .. ")", 2 )
    end
    if type( g ) ~= "number" then
        error( "bad argument #2 (expected number, got " .. type( g ) .. ")", 2 )
    end
    if type( b ) ~= "number" then
        error( "bad argument #3 (expected number, got " .. type( b ) .. ")", 2 )
    end
    return
        bit32.band( r * 255, 0xFF ) * 2^16 +
        bit32.band( g * 255, 0xFF ) * 2^8 +
        bit32.band( b * 255, 0xFF )
end

--- Separate a hexadecimal RGB colour into its three constituent channels.
--
-- @tparam number rgb The combined hexadecimal colour.
-- @treturn[1] number r The red channel, will be between 0 and 1.
-- @treturn[1] number g The red channel, will be between 0 and 1.
-- @treturn[1] number b The blue channel, will be between 0 and 1.
-- @usage colors.unpackRGB(0xb23399) == 0.7, 0.2, 0.6
-- @see colors.packRGB
function unpackRGB( rgb )
    if type( rgb ) ~= "number" then
        error( "bad argument #1 (expected number, got " .. type( rgb ) .. ")", 2 )
    end
    return
        bit32.band( bit32.rshift( rgb, 16 ), 0xFF ) / 255,
        bit32.band( bit32.rshift( rgb, 8 ), 0xFF ) / 255,
        bit32.band( rgb, 0xFF ) / 255
end

function rgb8( r, g, b )
    if g == nil and b == nil then
        return unpackRGB( r )
    else
        return packRGB( r, g, b )
    end
end
