-- SPDX-FileCopyrightText: 2017 Daniel Ratcliffe
--
-- SPDX-License-Identifier: LicenseRef-CCPL

--[[- Constants and functions for colour values, suitable for working with
[`term`] and [`redstone`].

This is useful in conjunction with [Bundled Cables][`redstone.setBundledOutput`]
from mods like Project Red, and [colors on Advanced Computers and Advanced
Monitors][`term.setTextColour`].

For the non-American English version just replace [`colors`] with [`colours`].
This alternative API is exactly the same, except the colours use British English
(e.g. [`colors.gray`] is spelt [`colours.grey`]).

On basic terminals (such as the Computer and Monitor), all the colors are
converted to grayscale. This means you can still use all 16 colors on the
screen, but they will appear as the nearest tint of gray. You can check if a
terminal supports color by using the function [`term.isColor`].

Grayscale colors are calculated by taking the average of the three components,
i.e. `(red + green + blue) / 3`.

<table>
<thead>
    <tr><th colspan="8" align="center">Default Colors</th></tr>
    <tr>
    <th rowspan="2" align="center">Color</th>
    <th colspan="3" align="center">Value</th>
    <th colspan="4" align="center">Default Palette Color</th>
    </tr>
    <tr>
    <th>Dec</th><th>Hex</th><th>Paint/Blit</th>
    <th>Preview</th><th>Hex</th><th>RGB</th><th>Grayscale</th>
    </tr>
</thead>
<tbody>
    <tr>
    <td><code>colors.white</code></td>
    <td align="right">1</td><td align="right">0x1</td><td align="right">0</td>
    <td style="background:#F0F0F0"></td><td>#F0F0F0</td><td>240, 240, 240</td>
    <td style="background:#F0F0F0"></td>
    </tr>
    <tr>
    <td><code>colors.orange</code></td>
    <td align="right">2</td><td align="right">0x2</td><td align="right">1</td>
    <td style="background:#F2B233"></td><td>#F2B233</td><td>242, 178, 51</td>
    <td style="background:#9D9D9D"></td>
    </tr>
    <tr>
    <td><code>colors.magenta</code></td>
    <td align="right">4</td><td align="right">0x4</td><td align="right">2</td>
    <td style="background:#E57FD8"></td><td>#E57FD8</td><td>229, 127, 216</td>
    <td style="background:#BEBEBE"></td>
    </tr>
    <tr>
    <td><code>colors.lightBlue</code></td>
    <td align="right">8</td><td align="right">0x8</td><td align="right">3</td>
    <td style="background:#99B2F2"></td><td>#99B2F2</td><td>153, 178, 242</td>
    <td style="background:#BFBFBF"></td>
    </tr>
    <tr>
    <td><code>colors.yellow</code></td>
    <td align="right">16</td><td align="right">0x10</td><td align="right">4</td>
    <td style="background:#DEDE6C"></td><td>#DEDE6C</td><td>222, 222, 108</td>
    <td style="background:#B8B8B8"></td>
    </tr>
    <tr>
    <td><code>colors.lime</code></td>
    <td align="right">32</td><td align="right">0x20</td><td align="right">5</td>
    <td style="background:#7FCC19"></td><td>#7FCC19</td><td>127, 204, 25</td>
    <td style="background:#767676"></td>
    </tr>
    <tr>
    <td><code>colors.pink</code></td>
    <td align="right">64</td><td align="right">0x40</td><td align="right">6</td>
    <td style="background:#F2B2CC"></td><td>#F2B2CC</td><td>242, 178, 204</td>
    <td style="background:#D0D0D0"></td>
    </tr>
    <tr>
    <td><code>colors.gray</code></td>
    <td align="right">128</td><td align="right">0x80</td><td align="right">7</td>
    <td style="background:#4C4C4C"></td><td>#4C4C4C</td><td>76, 76, 76</td>
    <td style="background:#4C4C4C"></td>
    </tr>
    <tr>
    <td><code>colors.lightGray</code></td>
    <td align="right">256</td><td align="right">0x100</td><td align="right">8</td>
    <td style="background:#999999"></td><td>#999999</td><td>153, 153, 153</td>
    <td style="background:#999999"></td>
    </tr>
    <tr>
    <td><code>colors.cyan</code></td>
    <td align="right">512</td><td align="right">0x200</td><td align="right">9</td>
    <td style="background:#4C99B2"></td><td>#4C99B2</td><td>76, 153, 178</td>
    <td style="background:#878787"></td>
    </tr>
    <tr>
    <td><code>colors.purple</code></td>
    <td align="right">1024</td><td align="right">0x400</td><td align="right">a</td>
    <td style="background:#B266E5"></td><td>#B266E5</td><td>178, 102, 229</td>
    <td style="background:#A9A9A9"></td>
    </tr>
    <tr>
    <td><code>colors.blue</code></td>
    <td align="right">2048</td><td align="right">0x800</td><td align="right">b</td>
    <td style="background:#3366CC"></td><td>#3366CC</td><td>51, 102, 204</td>
    <td style="background:#777777"></td>
    </tr>
    <tr>
    <td><code>colors.brown</code></td>
    <td align="right">4096</td><td align="right">0x1000</td><td align="right">c</td>
    <td style="background:#7F664C"></td><td>#7F664C</td><td>127, 102, 76</td>
    <td style="background:#656565"></td>
    </tr>
    <tr>
    <td><code>colors.green</code></td>
    <td align="right">8192</td><td align="right">0x2000</td><td align="right">d</td>
    <td style="background:#57A64E"></td><td>#57A64E</td><td>87, 166, 78</td>
    <td style="background:#6E6E6E"></td>
    </tr>
    <tr>
    <td><code>colors.red</code></td>
    <td align="right">16384</td><td align="right">0x4000</td><td align="right">e</td>
    <td style="background:#CC4C4C"></td><td>#CC4C4C</td><td>204, 76, 76</td>
    <td style="background:#767676"></td>
    </tr>
    <tr>
    <td><code>colors.black</code></td>
    <td align="right">32768</td><td align="right">0x8000</td><td align="right">f</td>
    <td style="background:#111111"></td><td>#111111</td><td>17, 17, 17</td>
    <td style="background:#111111"></td>
    </tr>
</tbody>
</table>

@see colours
@module colors
]]

local expect = dofile("rom/modules/main/cc/expect.lua").expect

--- White: Written as `0` in paint files and [`term.blit`], has a default
-- terminal colour of #F0F0F0.
white = 0x1

--- Orange: Written as `1` in paint files and [`term.blit`], has a
-- default terminal colour of #F2B233.
orange = 0x2

--- Magenta: Written as `2` in paint files and [`term.blit`], has a
-- default terminal colour of #E57FD8.
magenta = 0x4

--- Light blue: Written as `3` in paint files and [`term.blit`], has a
-- default terminal colour of #99B2F2.
lightBlue = 0x8

--- Yellow: Written as `4` in paint files and [`term.blit`], has a
-- default terminal colour of #DEDE6C.
yellow = 0x10

--- Lime: Written as `5` in paint files and [`term.blit`], has a default
-- terminal colour of #7FCC19.
lime = 0x20

--- Pink: Written as `6` in paint files and [`term.blit`], has a default
-- terminal colour of #F2B2CC.
pink = 0x40

--- Gray: Written as `7` in paint files and [`term.blit`], has a default
-- terminal colour of #4C4C4C.
gray = 0x80

--- Light gray: Written as `8` in paint files and [`term.blit`], has a
-- default terminal colour of #999999.
lightGray = 0x100

--- Cyan: Written as `9` in paint files and [`term.blit`], has a default
-- terminal colour of #4C99B2.
cyan = 0x200

--- Purple: Written as `a` in paint files and [`term.blit`], has a
-- default terminal colour of #B266E5.
purple = 0x400

--- Blue: Written as `b` in paint files and [`term.blit`], has a default
-- terminal colour of #3366CC.
blue = 0x800

--- Brown: Written as `c` in paint files and [`term.blit`], has a default
-- terminal colour of #7F664C.
brown = 0x1000

--- Green: Written as `d` in paint files and [`term.blit`], has a default
-- terminal colour of #57A64E.
green = 0x2000

--- Red: Written as `e` in paint files and [`term.blit`], has a default
-- terminal colour of #CC4C4C.
red = 0x4000

--- Black: Written as `f` in paint files and [`term.blit`], has a default
-- terminal colour of #111111.
black = 0x8000

--- Combines a set of colors (or sets of colors) into a larger set. Useful for
-- Bundled Cables.
--
-- @tparam number ... The colors to combine.
-- @treturn number The union of the color sets given in `...`
-- @since 1.2
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

--- Removes one or more colors (or sets of colors) from an initial set. Useful
-- for Bundled Cables.
--
-- Each parameter beyond the first may be a single color or may be a set of
-- colors (in the latter case, all colors in the set are removed from the
-- original set).
--
-- @tparam number colors The color from which to subtract.
-- @tparam number ... The colors to subtract.
-- @treturn number The resulting color.
-- @since 1.2
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

--- Tests whether `color` is contained within `colors`. Useful for Bundled
-- Cables.
--
-- @tparam number colors A color, or color set
-- @tparam number color A color or set of colors that `colors` should contain.
-- @treturn boolean If `colors` contains all colors within `color`.
-- @since 1.2
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
-- @tparam number g The green channel, should be between 0 and 1.
-- @tparam number b The blue channel, should be between 0 and 1.
-- @treturn number The combined hexadecimal colour.
-- @usage
-- ```lua
-- colors.packRGB(0.7, 0.2, 0.6)
-- -- => 0xb23399
-- ```
-- @since 1.81.0
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
-- @treturn number The green channel, will be between 0 and 1.
-- @treturn number The blue channel, will be between 0 and 1.
-- @usage
-- ```lua
-- colors.unpackRGB(0xb23399)
-- -- => 0.7, 0.2, 0.6
-- ```
-- @see colors.packRGB
-- @since 1.81.0
function unpackRGB(rgb)
    expect(1, rgb, "number")
    return
        bit32.band(bit32.rshift(rgb, 16), 0xFF) / 255,
        bit32.band(bit32.rshift(rgb, 8), 0xFF) / 255,
        bit32.band(rgb, 0xFF) / 255
end

--- Either calls [`colors.packRGB`] or [`colors.unpackRGB`], depending on how many
-- arguments it receives.
--
-- @tparam[1] number r The red channel, as an argument to [`colors.packRGB`].
-- @tparam[1] number g The green channel, as an argument to [`colors.packRGB`].
-- @tparam[1] number b The blue channel, as an argument to [`colors.packRGB`].
-- @tparam[2] number rgb The combined hexadecimal color, as an argument to [`colors.unpackRGB`].
-- @treturn[1] number The combined hexadecimal colour, as returned by [`colors.packRGB`].
-- @treturn[2] number The red channel, as returned by [`colors.unpackRGB`]
-- @treturn[2] number The green channel, as returned by [`colors.unpackRGB`]
-- @treturn[2] number The blue channel, as returned by [`colors.unpackRGB`]
-- @deprecated Use [`packRGB`] or [`unpackRGB`] directly.
-- @usage
-- ```lua
-- colors.rgb8(0xb23399)
-- -- => 0.7, 0.2, 0.6
-- ```
-- @usage
-- ```lua
-- colors.rgb8(0.7, 0.2, 0.6)
-- -- => 0xb23399
-- ```
-- @since 1.80pr1
-- @changed 1.81.0 Deprecated in favor of colors.(un)packRGB.
function rgb8(r, g, b)
    if g == nil and b == nil then
        return unpackRGB(r)
    else
        return packRGB(r, g, b)
    end
end

-- Colour to hex lookup table for toBlit
local color_hex_lookup = {}
for i = 0, 15 do
    color_hex_lookup[2 ^ i] = string.format("%x", i)
end

--[[- Converts the given color to a paint/blit hex character (0-9a-f).

This is equivalent to converting `floor(log_2(color))` to hexadecimal. Values
outside the range of a valid colour will error.

@tparam number color The color to convert.
@treturn string The blit hex code of the color.
@usage
```lua
colors.toBlit(colors.red)
-- => "c"
```
@see colors.fromBlit
@since 1.94.0
]]
function toBlit(color)
    expect(1, color, "number")
    local hex = color_hex_lookup[color]
    if hex then return hex end

    if color < 0 or color > 0xffff then error("Colour out of range", 2) end
    return string.format("%x", math.floor(math.log(color, 2)))
end

--[[- Converts the given paint/blit hex character (0-9a-f) to a color.

This is equivalent to converting the hex character to a number and then 2 ^ decimal

@tparam string hex The paint/blit hex character to convert
@treturn number The color
@usage
```lua
colors.fromBlit("e")
-- => 16384
```
@see colors.toBlit
@since 1.105.0
]]
function fromBlit(hex)
    expect(1, hex, "string")

    if #hex ~= 1 then return nil end
    local value = tonumber(hex, 16)
    if not value then return nil end

    return 2 ^ value
end
