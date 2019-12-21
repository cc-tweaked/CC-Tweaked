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

    local ok = shell.run(program, ...)

    return {
        output = table.concat(output),
        error = table.concat(error),
        combined = table.concat(combined),
        ok = ok,
    }
end

return {
    capture_program = capture_program,
}
