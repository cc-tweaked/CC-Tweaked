
local tHex = {
    [ colors.white ] = "0",
    [ colors.orange ] = "1",
    [ colors.magenta ] = "2",
    [ colors.lightBlue ] = "3",
    [ colors.yellow ] = "4",
    [ colors.lime ] = "5",
    [ colors.pink ] = "6",
    [ colors.gray ] = "7",
    [ colors.lightGray ] = "8",
    [ colors.cyan ] = "9",
    [ colors.purple ] = "a",
    [ colors.blue ] = "b",
    [ colors.brown ] = "c",
    [ colors.green ] = "d",
    [ colors.red ] = "e",
    [ colors.black ] = "f",
}

local type = type
local string_rep = string.rep
local string_sub = string.sub
local table_unpack = table.unpack

function create( parent, nX, nY, nWidth, nHeight, bStartVisible )
    expect(1, parent, "table")
    expect(2, nX, "number")
    expect(3, nY, "number")
    expect(4, nWidth, "number")
    expect(5, nHeight, "number")
    expect(6, bStartVisible, "boolean", "nil")

    if parent == term then
        error( "term is not a recommended window parent, try term.current() instead", 2 )
    end

    local sEmptySpaceLine
    local tEmptyColorLines = {}
    local function createEmptyLines( nWidth )
        sEmptySpaceLine = string_rep( " ", nWidth )
        for n=0,15 do
            local nColor = 2^n
            local sHex = tHex[nColor]
            tEmptyColorLines[nColor] = string_rep( sHex, nWidth )
        end
    end

    createEmptyLines( nWidth )

    -- Setup
    local bVisible = (bStartVisible ~= false)
    local nCursorX = 1
    local nCursorY = 1
    local bCursorBlink = false
    local nTextColor = colors.white
    local nBackgroundColor = colors.black
    local tLines = {}
    local tPalette = {}
    do
        local sEmptyText = sEmptySpaceLine
        local sEmptyTextColor = tEmptyColorLines[ nTextColor ]
        local sEmptyBackgroundColor = tEmptyColorLines[ nBackgroundColor ]
        for y=1,nHeight do
            tLines[y] = {
                text = sEmptyText,
                textColor = sEmptyTextColor,
                backgroundColor = sEmptyBackgroundColor,
            }
        end

        for i=0,15 do
            local c = 2 ^ i
            tPalette[c] = { parent.getPaletteColour( c ) }
        end
    end

    -- Helper functions
    local function updateCursorPos()
        if nCursorX >= 1 and nCursorY >= 1 and
           nCursorX <= nWidth and nCursorY <= nHeight then
            parent.setCursorPos( nX + nCursorX - 1, nY + nCursorY - 1 )
        else
            parent.setCursorPos( 0, 0 )
        end
    end

    local function updateCursorBlink()
        parent.setCursorBlink( bCursorBlink )
    end

    local function updateCursorColor()
        parent.setTextColor( nTextColor )
    end

    local function redrawLine( n )
        local tLine = tLines[ n ]
        parent.setCursorPos( nX, nY + n - 1 )
        parent.blit( tLine.text, tLine.textColor, tLine.backgroundColor )
    end

    local function redraw()
        for n=1,nHeight do
            redrawLine( n )
        end
    end

    local function updatePalette()
        for k,v in pairs( tPalette ) do
            parent.setPaletteColour( k, v[1], v[2], v[3] )
        end
    end

    local function internalBlit( sText, sTextColor, sBackgroundColor )
        local nStart = nCursorX
        local nEnd = nStart + #sText - 1
        if nCursorY >= 1 and nCursorY <= nHeight then
            if nStart <= nWidth and nEnd >= 1 then
                -- Modify line
                local tLine = tLines[ nCursorY ]
                if nStart == 1 and nEnd == nWidth then
                    tLine.text = sText
                    tLine.textColor = sTextColor
                    tLine.backgroundColor = sBackgroundColor
                else
                    local sClippedText, sClippedTextColor, sClippedBackgroundColor
                    if nStart < 1 then
                        local nClipStart = 1 - nStart + 1
                        local nClipEnd = nWidth - nStart + 1
                        sClippedText = string_sub( sText, nClipStart, nClipEnd )
                        sClippedTextColor = string_sub( sTextColor, nClipStart, nClipEnd )
                        sClippedBackgroundColor = string_sub( sBackgroundColor, nClipStart, nClipEnd )
                    elseif nEnd > nWidth then
                        local nClipEnd = nWidth - nStart + 1
                        sClippedText = string_sub( sText, 1, nClipEnd )
                        sClippedTextColor = string_sub( sTextColor, 1, nClipEnd )
                        sClippedBackgroundColor = string_sub( sBackgroundColor, 1, nClipEnd )
                    else
                        sClippedText = sText
                        sClippedTextColor = sTextColor
                        sClippedBackgroundColor = sBackgroundColor
                    end

                    local sOldText = tLine.text
                    local sOldTextColor = tLine.textColor
                    local sOldBackgroundColor = tLine.backgroundColor
                    local sNewText, sNewTextColor, sNewBackgroundColor
                    if nStart > 1 then
                        local nOldEnd = nStart - 1
                        sNewText = string_sub( sOldText, 1, nOldEnd ) .. sClippedText
                        sNewTextColor = string_sub( sOldTextColor, 1, nOldEnd ) .. sClippedTextColor
                        sNewBackgroundColor = string_sub( sOldBackgroundColor, 1, nOldEnd ) .. sClippedBackgroundColor
                    else
                        sNewText = sClippedText
                        sNewTextColor = sClippedTextColor
                        sNewBackgroundColor = sClippedBackgroundColor
                    end
                    if nEnd < nWidth then
                        local nOldStart = nEnd + 1
                        sNewText = sNewText .. string_sub( sOldText, nOldStart, nWidth )
                        sNewTextColor = sNewTextColor .. string_sub( sOldTextColor, nOldStart, nWidth )
                        sNewBackgroundColor = sNewBackgroundColor .. string_sub( sOldBackgroundColor, nOldStart, nWidth )
                    end

                    tLine.text = sNewText
                    tLine.textColor = sNewTextColor
                    tLine.backgroundColor = sNewBackgroundColor
                end

                -- Redraw line
                if bVisible then
                    redrawLine( nCursorY )
                end
            end
        end

        -- Move and redraw cursor
        nCursorX = nEnd + 1
        if bVisible then
            updateCursorColor()
            updateCursorPos()
        end
    end

    -- Terminal implementation
    local window = {}

    function window.write( sText )
        sText = tostring( sText )
        internalBlit( sText, string_rep( tHex[ nTextColor ], #sText ), string_rep( tHex[ nBackgroundColor ], #sText ) )
    end

    function window.blit( sText, sTextColor, sBackgroundColor )
        expect(1, sText, "string")
        expect(2, sTextColor, "string")
        expect(3, sBackgroundColor, "string")
        if #sTextColor ~= #sText or #sBackgroundColor ~= #sText then
            error( "Arguments must be the same length", 2 )
        end
        internalBlit( sText, sTextColor, sBackgroundColor )
    end

    function window.clear()
        local sEmptyText = sEmptySpaceLine
        local sEmptyTextColor = tEmptyColorLines[ nTextColor ]
        local sEmptyBackgroundColor = tEmptyColorLines[ nBackgroundColor ]
        for y=1,nHeight do
            tLines[y] = {
                text = sEmptyText,
                textColor = sEmptyTextColor,
                backgroundColor = sEmptyBackgroundColor,
            }
        end
        if bVisible then
            redraw()
            updateCursorColor()
            updateCursorPos()
        end
    end

    function window.clearLine()
        if nCursorY >= 1 and nCursorY <= nHeight then
            local sEmptyText = sEmptySpaceLine
            local sEmptyTextColor = tEmptyColorLines[ nTextColor ]
            local sEmptyBackgroundColor = tEmptyColorLines[ nBackgroundColor ]
            tLines[ nCursorY ] = {
                text = sEmptyText,
                textColor = sEmptyTextColor,
                backgroundColor = sEmptyBackgroundColor,
            }
            if bVisible then
                redrawLine( nCursorY )
                updateCursorColor()
                updateCursorPos()
            end
        end
    end

    function window.getCursorPos()
        return nCursorX, nCursorY
    end

    function window.setCursorPos( x, y )
        expect(1, x, "number")
        expect(2, y, "number")
        nCursorX = math.floor( x )
        nCursorY = math.floor( y )
        if bVisible then
            updateCursorPos()
        end
    end

    function window.setCursorBlink( blink )
        expect(1, blink, "boolean")
        bCursorBlink = blink
        if bVisible then
            updateCursorBlink()
        end
    end

    function window.getCursorBlink()
        return bCursorBlink
    end

    local function isColor()
        return parent.isColor()
    end

    function window.isColor()
        return isColor()
    end

    function window.isColour()
        return isColor()
    end

    local function setTextColor( color )
        expect(1, color, "number")
        if tHex[color] == nil then
            error( "Invalid color (got " .. color .. ")" , 2 )
        end

        nTextColor = color
        if bVisible then
            updateCursorColor()
        end
    end

    window.setTextColor = setTextColor
    window.setTextColour = setTextColor

    function window.setPaletteColour( colour, r, g, b )
        expect(1, colour, "number")

        if tHex[colour] == nil then
            error( "Invalid color (got " .. colour .. ")" , 2 )
        end

        local tCol
        if type(r) == "number" and g == nil and b == nil then
            tCol = { colours.unpackRGB( r ) }
            tPalette[ colour ] = tCol
        else
            expect(2, r, "number")
            expect(3, g, "number")
            expect(4, b, "number")

            tCol = tPalette[ colour ]
            tCol[1] = r
            tCol[2] = g
            tCol[3] = b
        end

        if bVisible then
            return parent.setPaletteColour( colour, tCol[1], tCol[2], tCol[3] )
        end
    end

    window.setPaletteColor = window.setPaletteColour

    function window.getPaletteColour( colour )
        expect(1, colour, "number")
        if tHex[colour] == nil then
            error( "Invalid color (got " .. colour .. ")" , 2 )
        end
        local tCol = tPalette[ colour ]
        return tCol[1], tCol[2], tCol[3]
    end

    window.getPaletteColor = window.getPaletteColour

    local function setBackgroundColor( color )
        expect(1, color, "number")
        if tHex[color] == nil then
            error( "Invalid color (got " .. color .. ")", 2 )
        end
        nBackgroundColor = color
    end

    window.setBackgroundColor = setBackgroundColor
    window.setBackgroundColour = setBackgroundColor

    function window.getSize()
        return nWidth, nHeight
    end

    function window.scroll( n )
        expect(1, n, "number")
        if n ~= 0 then
            local tNewLines = {}
            local sEmptyText = sEmptySpaceLine
            local sEmptyTextColor = tEmptyColorLines[ nTextColor ]
            local sEmptyBackgroundColor = tEmptyColorLines[ nBackgroundColor ]
            for newY=1,nHeight do
                local y = newY + n
                if y >= 1 and y <= nHeight then
                    tNewLines[newY] = tLines[y]
                else
                    tNewLines[newY] = {
                        text = sEmptyText,
                        textColor = sEmptyTextColor,
                        backgroundColor = sEmptyBackgroundColor,
                    }
                end
            end
            tLines = tNewLines
            if bVisible then
                redraw()
                updateCursorColor()
                updateCursorPos()
            end
        end
    end

    function window.getTextColor()
        return nTextColor
    end

    function window.getTextColour()
        return nTextColor
    end

    function window.getBackgroundColor()
        return nBackgroundColor
    end

    function window.getBackgroundColour()
        return nBackgroundColor
    end

    -- Other functions
    function window.setVisible( bVis )
        expect(1, bVis, "boolean")
        if bVisible ~= bVis then
            bVisible = bVis
            if bVisible then
                window.redraw()
            end
        end
    end

    function window.redraw()
        if bVisible then
            redraw()
            updatePalette()
            updateCursorBlink()
            updateCursorColor()
            updateCursorPos()
        end
    end

    function window.restoreCursor()
        if bVisible then
            updateCursorBlink()
            updateCursorColor()
            updateCursorPos()
        end
    end

    function window.getPosition()
        return nX, nY
    end

    function window.reposition( nNewX, nNewY, nNewWidth, nNewHeight )
        expect(1, nNewX, "number")
        expect(2, nNewY, "number")
        expect(3, nNewWidth, "number", "nil")
        expect(4, nNewHeight, "number", "nil")

        nX = nNewX
        nY = nNewY
        if nNewWidth and nNewHeight then
            local tNewLines = {}
            createEmptyLines( nNewWidth )
            local sEmptyText = sEmptySpaceLine
            local sEmptyTextColor = tEmptyColorLines[ nTextColor ]
            local sEmptyBackgroundColor = tEmptyColorLines[ nBackgroundColor ]
            for y=1,nNewHeight do
                if y > nHeight then
                    tNewLines[y] = {
                        text = sEmptyText,
                        textColor = sEmptyTextColor,
                        backgroundColor = sEmptyBackgroundColor
                    }
                else
                    local tOldLine = tLines[y]
                    if nNewWidth == nWidth then
                        tNewLines[y] = tOldLine
                    elseif nNewWidth < nWidth then
                        tNewLines[y] = {
                            text = string_sub( tOldLine.text, 1, nNewWidth ),
                            textColor = string_sub( tOldLine.textColor, 1, nNewWidth ),
                            backgroundColor = string_sub( tOldLine.backgroundColor, 1, nNewWidth ),
                        }
                    else
                        tNewLines[y] = {
                            text = tOldLine.text .. string_sub( sEmptyText, nWidth + 1, nNewWidth ),
                            textColor = tOldLine.textColor .. string_sub( sEmptyTextColor, nWidth + 1, nNewWidth ),
                            backgroundColor = tOldLine.backgroundColor .. string_sub( sEmptyBackgroundColor, nWidth + 1, nNewWidth ),
                        }
                    end
                end
            end
            nWidth = nNewWidth
            nHeight = nNewHeight
            tLines = tNewLines
        end
        if bVisible then
            window.redraw()
        end
    end

    if bVisible then
        window.redraw()
    end
    return window
end
