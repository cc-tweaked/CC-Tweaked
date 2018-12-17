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
    return ((bit32.band(colors, color)) == color)
end

function rgb8( r, g, b )
    if type( r ) ~= "number" then
        error( "bad argument #1 (expected number, got " .. type( r ) .. ")", 2 )
    elseif type(r) == "number" and g == nil and b == nil then
        return bit32.band( bit32.rshift( r, 16 ), 0xFF ) / 255, bit32.band( bit32.rshift( r, 8 ), 0xFF ) / 255, bit32.band( r, 0xFF ) / 255
    elseif type(r) == "number" and type(g) == "number" and type(b) == "number" then
        return
            bit32.lshift( bit32.band(r * 255, 0xFF), 16 ) +
            bit32.lshift( bit32.band(g * 255, 0xFF), 8 ) +
            bit32.band(b * 255, 0xFF)
    elseif type( g ) ~= "number" then
        error( "bad argument #2 (expected number, got " .. type( g ) .. ")", 2 )
    elseif type( b ) ~= "number" then
        error( "bad argument #3 (expected number, got " .. type( b ) .. ")", 2 )
    end
end
