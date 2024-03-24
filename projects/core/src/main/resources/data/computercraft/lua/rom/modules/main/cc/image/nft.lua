-- SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--- Read and draw nft ("Nitrogen Fingers Text") images.
--
-- nft ("Nitrogen Fingers Text") is a file format for drawing basic images.
-- Unlike the images that [`paintutils.parseImage`] uses, nft supports coloured
-- text as well as simple coloured pixels.
--
-- @module cc.image.nft
-- @since 1.90.0
-- @usage Load an image from `example.nft` and draw it.
--
--     local nft = require "cc.image.nft"
--     local image = assert(nft.load("data/example.nft"))
--     nft.draw(image, term.getCursorPos())

local expect = require "cc.expect".expect

--- Parse an nft image from a string.
--
-- @tparam string image The image contents.
-- @return table The parsed image.
local function parse(image)
    expect(1, image, "string")

    local result = {}
    local line = 1
    local foreground = "0"
    local background = "f"

    local i, len = 1, #image
    while i <= len do
        local c = image:sub(i, i)
        if c == "\31" and i < len then
            i = i + 1
            foreground = image:sub(i, i)
        elseif c == "\30" and i < len then
            i = i + 1
            background = image:sub(i, i)
        elseif c == "\n" then
            if result[line] == nil then
                result[line] = { text = "", foreground = "", background = "" }
            end

            line = line + 1
            foreground, background = "0", "f"
        else
            local next = image:find("[\n\30\31]", i) or #image + 1
            local seg_len = next - i

            local this_line = result[line]
            if this_line == nil then
                this_line = { foreground = "", background = "", text = "" }
                result[line] = this_line
            end

            this_line.text = this_line.text .. image:sub(i, next - 1)
            this_line.foreground = this_line.foreground .. foreground:rep(seg_len)
            this_line.background = this_line.background .. background:rep(seg_len)

            i = next - 1
        end

        i = i + 1
    end
    return result
end

--- Load an nft image from a file.
--
-- @tparam string path The file to load.
-- @treturn[1] table The parsed image.
-- @treturn[2] nil If the file does not exist or could not be loaded.
-- @treturn[2] string An error message explaining why the file could not be
-- loaded.
local function load(path)
    expect(1, path, "string")
    local file, err = io.open(path, "r")
    if not file then return nil, err end

    local result = file:read("*a")
    file:close()
    return parse(result)
end

--- Draw an nft image to the screen.
--
-- @tparam table image An image, as returned from [`load`] or [`parse`].
-- @tparam number xPos The x position to start drawing at.
-- @tparam number yPos The y position to start drawing at.
-- @tparam[opt] term.Redirect target The terminal redirect to draw to. Defaults to the
-- current terminal.
local function draw(image, xPos, yPos, target)
    expect(1, image, "table")
    expect(2, xPos, "number")
    expect(3, yPos, "number")
    expect(4, target, "table", "nil")

    if not target then target = term end

    for y, line in ipairs(image) do
        target.setCursorPos(xPos, yPos + y - 1)
        target.blit(line.text, line.foreground, line.background)
    end
end

return {
    parse = parse,
    load = load,
    draw = draw,
}
