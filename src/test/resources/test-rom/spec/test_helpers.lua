--- Run a program and capture its output
--
-- @tparam function(tbl:table, var:string, value:string) stub The active stub function.
-- @tparam string program The program name.
-- @tparam string ... Arguments to this program.
-- @treturn { ok = boolean, output = string, error = string, combined = string }
-- Whether this program terminated successfully, and the various output streams.
local function capture_program(stub, program, ...)
    local output, error, combined = {}, {}, {}

    local function out(stream, msg)
        table.insert(stream, msg)
        table.insert(combined, msg)
    end

    stub(_G, "print", function(...)
        for i = 1, select('#', ...) do
            if i > 1 then out(output, " ") end
            out(output, tostring(select(i, ...)))
        end
        out(output, "\n")
    end)

    stub(_G, "printError", function(...)
        for i = 1, select('#', ...) do
            if i > 1 then out(error, " ") end
            out(error, tostring(select(i, ...)))
        end
        out(error, "\n")
    end)

    stub(_G, "write", function(msg) out(output, tostring(msg)) end)
    
    local tTextutilsCopy = {}
    for k,v in pairs(textutils) do
        tTextutilsCopy[k] = v
    end

    function tTextutilsCopy.slowPrint( msg )
        out(output, tostring(msg).."\n")
    end
    
    function tTextutilsCopy.pagedTabulate(...)
        local tArgs = table.pack(...)
        for k,v in ipairs(tArgs) do
            if type(v) == "table" then
                for a,b in ipairs(v) do
                    out(output, tostring(b).."\n")
                end
            else
                out(output, tostring(v).."\n")
            end
        end
    end

    stub(_G,"textutils",tTextutilsCopy)

    local ok = shell.run(program, ...)

    return {
        output = table.concat(output),
        error = table.concat(error),
        combined = table.concat(combined),
        ok = ok
    }
end

return {
    capture_program = capture_program,
}
