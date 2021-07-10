--[[- The Colours API allows you to manipulate sets of colours.
This is useful in conjunction with Bundled Cables from the RedPower mod, RedNet
Cables from the MineFactory Reloaded mod, and colours on Advanced Computers and
Advanced Monitors.

For the American English version just replace @{colours} with @{colors} and
it will use the other API, colours which is exactly the same, except in American
English (e.g. @{colours.grey} is spelt @{colors.gray}).

On basic terminals (such as the Computer and Monitor), all the colours are
converted to grayscale. This means you can still use all 16 colours on the
screen, but they will appear as the nearest tint of grey. You can check if a
terminal supports colour by using the function @{term.isColour}.
Greyscale colours are calculated by taking the average of the three components,
i.e. `(red + green + blue) / 3`.
<table class="pretty-table">
<thead>
    <tr><th colspan="8" align="center">Default Colours</th></tr>
    <tr>
    <th rowspan="2" align="center">Colour</th>
    <th colspan="3" align="center">Value</th>
    <th colspan="4" align="center">Default Palette Colour</th>
    </tr>
    <tr>
    <th>Dec</th><th>Hex</th><th>Paint/Blit</th>
    <th>Preview</th><th>Hex</th><th>RGB</th><th>Grayscale</th>
    </tr>
</thead>
<tbody>
    <tr>
    <td><code>colours.white</code></td>
    <td align="right">1</td><td align="right">0x1</td><td align="right">0</td>
    <td style="background:#F0F0F0"></td><td>#F0F0F0</td><td>240, 240, 240</td>
    <td style="background:#F0F0F0"></td>
    </tr>
    <tr>
    <td><code>colours.orange</code></td>
    <td align="right">2</td><td align="right">0x2</td><td align="right">1</td>
    <td style="background:#F2B233"></td><td>#F2B233</td><td>242, 178, 51</td>
    <td style="background:#9D9D9D"></td>
    </tr>
    <tr>
    <td><code>colours.magenta</code></td>
    <td align="right">4</td><td align="right">0x4</td><td align="right">2</td>
    <td style="background:#E57FD8"></td><td>#E57FD8</td><td>229, 127, 216</td>
    <td style="background:#BEBEBE"></td>
    </tr>
    <tr>
    <td><code>colours.lightBlue</code></td>
    <td align="right">8</td><td align="right">0x8</td><td align="right">3</td>
    <td style="background:#99B2F2"></td><td>#99B2F2</td><td>153, 178, 242</td>
    <td style="background:#BFBFBF"></td>
    </tr>
    <tr>
    <td><code>colours.yellow</code></td>
    <td align="right">16</td><td align="right">0x10</td><td align="right">4</td>
    <td style="background:#DEDE6C"></td><td>#DEDE6C</td><td>222, 222, 108</td>
    <td style="background:#B8B8B8"></td>
    </tr>
    <tr>
    <td><code>colours.lime</code></td>
    <td align="right">32</td><td align="right">0x20</td><td align="right">5</td>
    <td style="background:#7FCC19"></td><td>#7FCC19</td><td>127, 204, 25</td>
    <td style="background:#767676"></td>
    </tr>
    <tr>
    <td><code>colours.pink</code></td>
    <td align="right">64</td><td align="right">0x40</td><td align="right">6</td>
    <td style="background:#F2B2CC"></td><td>#F2B2CC</td><td>242, 178, 204</td>
    <td style="background:#D0D0D0"></td>
    </tr>
    <tr>
    <td><code>colours.grey</code></td>
    <td align="right">128</td><td align="right">0x80</td><td align="right">7</td>
    <td style="background:#4C4C4C"></td><td>#4C4C4C</td><td>76, 76, 76</td>
    <td style="background:#4C4C4C"></td>
    </tr>
    <tr>
    <td><code>colours.lightGrey</code></td>
    <td align="right">256</td><td align="right">0x100</td><td align="right">8</td>
    <td style="background:#999999"></td><td>#999999</td><td>153, 153, 153</td>
    <td style="background:#999999"></td>
    </tr>
    <tr>
    <td><code>colours.cyan</code></td>
    <td align="right">512</td><td align="right">0x200</td><td align="right">9</td>
    <td style="background:#4C99B2"></td><td>#4C99B2</td><td>76, 153, 178</td>
    <td style="background:#878787"></td>
    </tr>
    <tr>
    <td><code>colours.purple</code></td>
    <td align="right">1024</td><td align="right">0x400</td><td align="right">a</td>
    <td style="background:#B266E5"></td><td>#B266E5</td><td>178, 102, 229</td>
    <td style="background:#A9A9A9"></td>
    </tr>
    <tr>
    <td><code>colours.blue</code></td>
    <td align="right">2048</td><td align="right">0x800</td><td align="right">b</td>
    <td style="background:#3366CC"></td><td>#3366CC</td><td>51, 102, 204</td>
    <td style="background:#777777"></td>
    </tr>
    <tr>
    <td><code>colours.brown</code></td>
    <td align="right">4096</td><td align="right">0x1000</td><td align="right">c</td>
    <td style="background:#7F664C"></td><td>#7F664C</td><td>127, 102, 76</td>
    <td style="background:#656565"></td>
    </tr>
    <tr>
    <td><code>colours.green</code></td>
    <td align="right">8192</td><td align="right">0x2000</td><td align="right">d</td>
    <td style="background:#57A64E"></td><td>#57A64E</td><td>87, 166, 78</td>
    <td style="background:#6E6E6E"></td>
    </tr>
    <tr>
    <td><code>colours.red</code></td>
    <td align="right">16384</td><td align="right">0x4000</td><td align="right">e</td>
    <td style="background:#CC4C4C"></td><td>#CC4C4C</td><td>204, 76, 76</td>
    <td style="background:#767676"></td>
    </tr>
    <tr>
    <td><code>colours.black</code></td>
    <td align="right">32768</td><td align="right">0x8000</td><td align="right">f</td>
    <td style="background:#111111"></td><td>#111111</td><td>17, 17, 17</td>
    <td style="background:#111111"></td>
    </tr>
</tbody>
</table>
@see colors
@module colours
]]

local colours = _ENV
for k, v in pairs(colors) do
    colours[k] = v
end

--- Grey. Written as `7` in paint files and @{term.blit}, has a default
-- terminal colour of #4C4C4C.
--
-- @see colors.gray
colours.grey = colors.gray
colours.gray = nil --- @local

--- Light grey. Written as `8` in paint files and @{term.blit}, has a
-- default terminal colour of #999999.
--
-- @see colors.lightGray
colours.lightGrey = colors.lightGray
colours.lightGray = nil --- @local
