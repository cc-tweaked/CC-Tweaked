local expect = require "cc.expect".expect

function parse(image)
    expect(1, image, "string")
    local tImage = {}
    local nLine = 1
    local sForeground = "0"
    local sBackground = "F"
    local i, len = 1, #image
    while i <= len do
        local c = image:sub(i, i)
        if c == "\31" and i < len then
            i = i + 1
            sForeground = image:sub(i, i)
        elseif c == "\30" and i < len then
            i = i + 1
            sBackground = image:sub(i, i)
        elseif c == "\n" then
            nLine = nLine + 1
        else
            if tImage[nLine] == nil then
                tImage[nLine] = {}
                tImage[nLine]["foreground"] = ""
                tImage[nLine]["background"] = ""
                tImage[nLine]["text"] = ""
            end
            tImage[nLine]["foreground"] = tImage[nLine]["foreground"]..sForeground
            tImage[nLine]["background"] = tImage[nLine]["background"]..sBackground
            tImage[nLine]["text"] = tImage[nLine]["text"]..c
        end
        i = i + 1
    end
    return tImage
end

function load(path)
    expect(1, path, "string")
    local file = io.open(path, "r")
    if file then
        local sContent = file:read("*a")
        file:close()
        return parse(sContent)
    end
    return nil
end

function draw(image, xPos, yPos)
    expect(1, image, "table")
    expect(2, xPos, "number")
    expect(3, yPos, "number")
    for count,i in ipairs(image) do
        term.setCursorPos(xPos,yPos-1+count)
        term.blit(i["text"],i["foreground"],i["background"])
    end
end

return {
    parse = parse,
    load = load,
    draw = draw
}
