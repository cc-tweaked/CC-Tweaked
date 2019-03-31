-- Colors
white = 1
orange = 2
magenta = 4
lightBlue = 8
yellow = 16
lime = 32
pink = 64
gray = 128
lightGray = 256
cyan = 512
purple = 1024
blue = 2048
brown = 4096
green = 8192
red = 16384
black = 32768

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

function test( colors, color )
    if type( colors ) ~= "number" then
        error( "bad argument #1 (expected number, got " .. type( colors ) .. ")", 2 )
    end
    if type( color ) ~= "number" then
        error( "bad argument #2 (expected number, got " .. type( color ) .. ")", 2 )
    end
    return bit32.band(colors, color) == color
end

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
