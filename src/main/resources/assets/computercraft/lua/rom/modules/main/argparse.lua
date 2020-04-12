local function simpleParser( tArgs )
    local parameters = {}
    local arguments = {}
    local options = {}
    local currentArgument = nil
    for k, v in ipairs( tArgs ) do
        if currentArgument ~= nil then
            arguments[currentArgument] = v
            currentArgument = nil
        elseif v:find("--",1,true) == 1 then
            v = v:sub(3,-1)
            currentArgument = v
        elseif v:find("-") == 1 then
            v = v:sub(2,-1)
            for c in v:gmatch"." do
                options[c] = true
            end
        else
            table.insert( parameters, v )
        end
    end
    return parameters, options, arguments
end

local function advancedParser()
    parserTable = setmetatable({options={},arguments={}},{})
    function parserTable.addOption(parserTable, char, description)
        parserTable.options[char] = {description=description}
    end
    function parserTable.addArgument(parserTable, argument, description)
        parserTable.arguments[argument] = {description=description}
    end
    function parserTable.parseArgs( parserTable, tArgs )
        local parameters = {}
        local arguments = {}
        local options = {}
        local currentArgument = nil
        for k, v in ipairs( tArgs ) do
            if currentArgument ~= nil then
                arguments[currentArgument] = v
                currentArgument = nil
            elseif v:find("--",1,true) == 1 then
                v = v:sub(3,-1)
                if v == "help" then
                    parserTable:displayHelp()
                    error()
                elseif type( parserTable.arguments[v] ) == "table" then
                    currentArgument = v
                else
                    printError('Unknown Argument "' .. v .. '". Run --help to see all options.')
                    error()
                end
            elseif v:find("-") == 1 then
                v = v:sub(2,-1)
                for c in v:gmatch"." do
                    if type( parserTable.options[c] ) == "table" then
                        options[v] = true
                    else
                        printError('Unknown Option "' .. c .. '". Run --help to see all options.')
                        error()
                    end
                end
            else
                table.insert( parameters, v )
            end
        end
        return parameters, options, arguments
    end
    function parserTable.displayHelp(parserTable)
        tPrint = {{"Argument","Description"}}
        for k, v in pairs(parserTable.arguments) do
            table.insert( tPrint, { "--" .. k,v.description} )
        end
        for k, v in pairs(parserTable.options) do
            table.insert( tPrint, { "-" .. k,v.description} )
        end
        textutils.pagedTabulate( table.unpack( tPrint ) )
    end
    return parserTable
end

return {
    simpleParser = simpleParser,
    advancedParser = advancedParser
}
