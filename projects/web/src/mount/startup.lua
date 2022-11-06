-- Print out license information if needed
if fs.exists("data/example.dfpwm") then
    local h = io.open("data/example.dfpwm.LICENSE")
    local contents = h:read("*a")
    h:close()

    write(contents)
end

-- Make the startup file invisible, then run the file. We could use
-- shell.run, but this ensures the program is in shell history, etc...
fs.delete("startup.lua")
os.queueEvent("paste", "example.lua")
os.queueEvent("key", keys.enter, false)
