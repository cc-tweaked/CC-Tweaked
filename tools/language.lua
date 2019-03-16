--- Rewrites language files in order to be consistent with en_us.
--
-- This will take every given language file and rewrite it to be in the same
-- order as the en_us file. Any keys which appear in the given file, but not
-- in en_us are warned about.
--
-- When passing the `-v` flag, we also print any missing commands.
--
-- Note, this is not intended to be a fool-proof tool, rather a quick way to
-- ensure language files are mostly correct.
--
-- @example
--
--     # Reformat all files
--     > lua tools/language.lua
--     # Reformat German, and warn about missing entries
--     > lua tools/language.lua -v de_de

local primary = "en_us"

local secondary = {
    "de_de",
    "fr_fr",
    "it_it",
    "pt_br",
    "sv_se",
}

local verbose = false

local path = "src/main/resources/assets/computercraft/lang/%s.lang"


local args = {...}
for i = #args, 1, -1 do
    if args[i] == "-v" or args[i] == "--verbose" then
        table.remove(args, i)
        verbose = true
    end
end
if #args > 0 then secondary = args end

-- Read the contents of the primary language file
local primary_contents, n = {}, 1
for line in io.lines(path:format(primary)) do
    local key = line:match("^([^=]+)=.*$")
    if key then
        primary_contents[n], n = key, n + 1
    elseif line == "" or line:sub(1, 1) == "#" then
        primary_contents[n], n = line, n + 1
    else
        io.stderr:write(("Unknown line %q in %s\n"):format(line, primary))
        os.exit(1)
    end
end

for _, language in ipairs(secondary) do
    local keys = {}

    for line in io.lines(path:format(language)) do
        local key, value = line:match("^([^=]+)=(.*)$")
        if key then
            if keys[key] then
                io.stderr:write(("Duplicate keys for %q in %q\n"):format(key, language))
                os.exit(10)
            end

            keys[key] = value
        elseif line ~= "" and line:sub(1, 1) ~= "#" then
            io.stderr:write(("Unknown line %q in %s\n"):format(line, language))
            os.exit(1)
        end
    end

    local h = io.open(path:format(language), "wb")
    local previous_blank = flase
    for _, line in ipairs(primary_contents) do
        if line == "" then
            if not previous_blank then
                previous_blank = true
                h:write("\n")
            end
        elseif line:sub(1, 1) == "#" then
            h:write(line .. "\n")
            previous_blank = false
        else
            local translated = keys[line]
            if translated then
                h:write(("%s=%s\n"):format(line, translated))
                keys[line] = nil
                previous_blank = false
            elseif verbose then
                io.stderr:write(("Missing translation %q for %q\n"):format(line, language))
            end
        end
    end

    if next(keys) ~= nil then
        local extra = {}
        for k, v in pairs(keys) do extra[#extra + 1] = ("%s=%s\n"):format(k, v) end
        table.sort(extra)

        io.stderr:write(("%d additional unknown translation keys or %q\n"):format(#extra, language))

        if not previous_blank then h:write("\n") end
        h:write("#Unknown translations\n")
        for _, line in ipairs(extra) do h:write(line) end
    end

    h:close()
end
