--[[- Internal tools for working with errors.

:::warning
This is an internal module and SHOULD NOT be used in your own code. It may
be removed or changed at any time.
:::

@local
]]

local expect = require "cc.expect".expect
local exception = require "cc.exception"
local error_printer = require "cc.internal.error_printer"

local function find_frame(thread, file, line)
    -- Scan the first 16 frames for something interesting.
    for offset = 0, 15 do
        local frame = debug.getinfo(thread, offset, "Sl")
        if not frame then break end

        if frame.short_src == file and frame.what ~= "C" and frame.currentline == line then
            return frame
        end
    end
end

--[[- Report additional context about an error.

@param err The error to report, possibly a @{cc.exception.Exception}.
@tparam[opt] { [string] = string } source_map Map of chunk names to their contents.
]]
local function report(err, source_map)
    expect(2, source_map, "table", "nil")
    if not exception.is_exception(err) then return end

    local file, line = err.message:match("^([^:]+):(%d+):")
    if not file then return end
    line = tonumber(line)

    local frame = find_frame(err.root_cause.thread, file, line)
    if not frame or not frame.currentcolumn then return end

    local column = frame.currentcolumn
    local line_contents
    if source_map and source_map[frame.source] then
        -- File exists in the source map.
        local pos, contents = 1, source_map[frame.source]
        for _ = 1, line - 1 do
            local next_pos = contents:find("\n", pos)
            if not next_pos then return end
            pos = next_pos + 1
        end

        local end_pos = contents:find("\n", pos)
        line_contents = contents:sub(pos, end_pos and end_pos - 1 or #contents)

    elseif frame.source:sub(1, 2) == "@/" then
        -- Read the file from disk.
        local handle = fs.open(frame.source:sub(3), "r")
        if not handle then return end
        for _ = 1, line - 1 do handle.readLine() end

        line_contents = handle.readLine()
    end

    -- Could not determine the line. Bail.
    if not line_contents or #line_contents == "" then return end

    error_printer({
        get_pos = function() return line, column end,
        get_line = function() return line_contents end,
    }, {
        { tag = "annotate", start_pos = column, end_pos = column, msg = "" },
    })
end


return {
    report = report,
}
