local expect = dofile("rom/modules/main/craftos/expect.lua").expect

local function drawPixelInternal( xPos, yPos )
    term.setCursorPos( xPos, yPos )
    term.write(" ")
end

local tColourLookup = {}
for n=1,16 do
    tColourLookup[ string.byte( "0123456789abcdef",n,n ) ] = 2^(n-1)
end

local function parseLine( tImageArg, sLine )
    local tLine = {}
    for x=1,sLine:len() do
        tLine[x] = tColourLookup[ string.byte(sLine,x,x) ] or 0
    end
    table.insert( tImageArg, tLine )
end

function parseImage( sRawData )
    expect(1, sRawData, "string")
    local tImage = {}
    for sLine in ( sRawData .. "\n" ):gmatch( "(.-)\n" ) do -- read each line like original file handling did
        parseLine( tImage, sLine )
    end
    return tImage
end

function loadImage( sPath )
    expect(1, sPath, "string")

    if fs.exists( sPath ) then
        local file = io.open( sPath, "r" )
        local sContent = file:read("*a")
        file:close()
        return parseImage( sContent ) -- delegate image parse to parseImage
    end
    return nil
end

function drawPixel( xPos, yPos, nColour )
    expect(1, xPos, "number")
    expect(2, yPos, "number")
    expect(3, nColour, "number", "nil")
    if nColour then
        term.setBackgroundColor( nColour )
    end
    return drawPixelInternal( xPos, yPos )
end

function drawLine( startX, startY, endX, endY, nColour )
    expect(1, startX, "number")
    expect(2, startY, "number")
    expect(3, endX, "number")
    expect(4, endY, "number")
    expect(5, nColour, "number", "nil")

    startX = math.floor(startX)
    startY = math.floor(startY)
    endX = math.floor(endX)
    endY = math.floor(endY)

    if nColour then
        term.setBackgroundColor( nColour )
    end
    if startX == endX and startY == endY then
        drawPixelInternal( startX, startY )
        return
    end

    local minX = math.min( startX, endX )
    local maxX, minY, maxY
    if minX == startX then
        minY = startY
        maxX = endX
        maxY = endY
    else
        minY = endY
        maxX = startX
        maxY = startY
    end

    -- TODO: clip to screen rectangle?

    local xDiff = maxX - minX
    local yDiff = maxY - minY

    if xDiff > math.abs(yDiff) then
        local y = minY
        local dy = yDiff / xDiff
        for x=minX,maxX do
            drawPixelInternal( x, math.floor( y + 0.5 ) )
            y = y + dy
        end
    else
        local x = minX
        local dx = xDiff / yDiff
        if maxY >= minY then
            for y=minY,maxY do
                drawPixelInternal( math.floor( x + 0.5 ), y )
                x = x + dx
            end
        else
            for y=minY,maxY,-1 do
                drawPixelInternal( math.floor( x + 0.5 ), y )
                x = x - dx
            end
        end
    end
end

function drawBox( startX, startY, endX, endY, nColour )
    expect(1, startX, "number")
    expect(2, startY, "number")
    expect(3, endX, "number")
    expect(4, endY, "number")
    expect(5, nColour, "number", "nil")

    startX = math.floor(startX)
    startY = math.floor(startY)
    endX = math.floor(endX)
    endY = math.floor(endY)

    if nColour then
        term.setBackgroundColor( nColour )
    end
    if startX == endX and startY == endY then
        drawPixelInternal( startX, startY )
        return
    end

    local minX = math.min( startX, endX )
    local maxX, minY, maxY
    if minX == startX then
        minY = startY
        maxX = endX
        maxY = endY
    else
        minY = endY
        maxX = startX
        maxY = startY
    end

    for x=minX,maxX do
        drawPixelInternal( x, minY )
        drawPixelInternal( x, maxY )
    end

    if (maxY - minY) >= 2 then
        for y=(minY+1),(maxY-1) do
            drawPixelInternal( minX, y )
            drawPixelInternal( maxX, y )
        end
    end
end

function drawFilledBox( startX, startY, endX, endY, nColour )
    expect(1, startX, "number")
    expect(2, startY, "number")
    expect(3, endX, "number")
    expect(4, endY, "number")
    expect(5, nColour, "number", "nil")

    startX = math.floor(startX)
    startY = math.floor(startY)
    endX = math.floor(endX)
    endY = math.floor(endY)

    if nColour then
        term.setBackgroundColor( nColour )
    end
    if startX == endX and startY == endY then
        drawPixelInternal( startX, startY )
        return
    end

    local minX = math.min( startX, endX )
    local maxX, minY, maxY
    if minX == startX then
        minY = startY
        maxX = endX
        maxY = endY
    else
        minY = endY
        maxX = startX
        maxY = startY
    end

    for x=minX,maxX do
        for y=minY,maxY do
            drawPixelInternal( x, y )
        end
    end
end

function drawImage( tImage, xPos, yPos )
    expect(1, tImage, "table")
    expect(2, xPos, "number")
    expect(3, yPos, "number")
    for y=1,#tImage do
        local tLine = tImage[y]
        for x=1,#tLine do
            if tLine[x] > 0 then
                term.setBackgroundColor( tLine[x] )
                drawPixelInternal( x + xPos - 1, y + yPos - 1 )
            end
        end
    end
end
