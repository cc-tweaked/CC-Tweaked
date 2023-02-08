local tArgs = { ... }
if #tArgs > 0 then
    print("This is an interactive Lua prompt.")
    print("To run a lua program, just type its name.")
    return
end

local pretty = require "cc.pretty"
local exception = require "cc.exception"

local running = true
local tCommandHistory = {}
local tEnv = {
    ["exit"] = setmetatable({}, {
        __tostring = function() return "Call exit() to exit." end,
        __call = function() running = false end,
    }),
    ["_echo"] = function(...)
        return ...
    end,
}
setmetatable(tEnv, { __index = _ENV })

-- Replace our package.path, so that it loads from the current directory, rather
-- than from /rom/programs. This makes it a little more friendly to use and
-- closer to what you'd expect.
do
    local dir = shell.dir()
    if dir:sub(1, 1) ~= "/" then dir = "/" .. dir end
    if dir:sub(-1) ~= "/" then dir = dir .. "/" end

    local strip_path = "?;?.lua;?/init.lua;"
    local path = package.path
    if path:sub(1, #strip_path) == strip_path then
        path = path:sub(#strip_path + 1)
    end

    package.path = dir .. "?;" .. dir .. "?.lua;" .. dir .. "?/init.lua;" .. path
end

if term.isColour() then
    term.setTextColour(colours.yellow)
end
print("Interactive Lua prompt.")
print("Call exit() to exit.")
term.setTextColour(colours.white)

local chunk_idx, chunk_map = 1, {}
while running do
    --if term.isColour() then
    --    term.setTextColour( colours.yellow )
    --end
    write("lua> ")
    --term.setTextColour( colours.white )

    local input = read(nil, tCommandHistory, function(sLine)
        if settings.get("lua.autocomplete") then
            local nStartPos = string.find(sLine, "[a-zA-Z0-9_%.:]+$")
            if nStartPos then
                sLine = string.sub(sLine, nStartPos)
            end
            if #sLine > 0 then
                return textutils.complete(sLine, tEnv)
            end
        end
        return nil
    end)
    if input:match("%S") and tCommandHistory[#tCommandHistory] ~= input then
        table.insert(tCommandHistory, input)
    end
    if settings.get("lua.warn_against_use_of_local") and input:match("^%s*local%s+") then
        if term.isColour() then
            term.setTextColour(colours.yellow)
        end
       print("To access local variables in later inputs, remove the local keyword.")
       term.setTextColour(colours.white)
    end

    local name, offset = "=lua[" .. chunk_idx .. "]", 0

    local force_print = 0
    local func, err = load(input, name, "t", tEnv)

    local expr_func = load("return _echo(" .. input .. ");", name, "t", tEnv)
    if not func then
        if expr_func then
            func = expr_func
            offset = 13
            force_print = 1
        end
    elseif expr_func then
        func = expr_func
        offset = 13
    end

    if func then
        chunk_map[name] = { contents = input, offset = offset }
        chunk_idx = chunk_idx + 1

        local results = table.pack(exception.try(func))
        if results[1] then
            local n = 1
            while n < results.n or n <= force_print do
                local value = results[n + 1]
                local ok, serialised = pcall(pretty.pretty, value, {
                    function_args = settings.get("lua.function_args"),
                    function_source = settings.get("lua.function_source"),
                })
                if ok then
                    pretty.print(serialised)
                else
                    print(tostring(value))
                end
                n = n + 1
            end
        else
            printError(results[2])
            require "cc.internal.exception".report(results[2], chunk_map)
        end
    else
        local parser = require "cc.internal.syntax"
        if parser.parse_repl(input) then printError(err) end
    end

end
