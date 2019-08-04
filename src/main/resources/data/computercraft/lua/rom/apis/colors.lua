local expect = dofile("rom/modules/main/cc/expect.lua").expect

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
    for i = 1, select('#', ...) do
        local c = select(i, ...)
        expect(i, c, "number")
        r = bit32.bor(r,c)
    end
    return r
end

function subtract( colors, ... )
    expect(1, colors, "number")
    local r = colors
    for i = 1, select('#', ...) do
        local c = select(i, ...)
        expect(i + 1, c, "number")
        r = bit32.band(r, bit32.bnot(c))
    end
    return r
end

function test( colors, color )
    expect(1, colors, "number")
    expect(2, color, "number")
    return bit32.band(colors, color) == color
end

function packRGB( r, g, b )
    expect(1, r, "number")
    expect(2, g, "number")
    expect(3, b, "number")
    return
        bit32.band( r * 255, 0xFF ) * 2^16 +
        bit32.band( g * 255, 0xFF ) * 2^8 +
        bit32.band( b * 255, 0xFF )
end

function unpackRGB( rgb )
    expect(1, rgb, "number")
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
