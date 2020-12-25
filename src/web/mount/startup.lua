-- Make the startup file invisible, then run the file. We could use
-- shell.run, but this ensures the program is in shell history, etc...
fs.delete("startup.lua")
os.queueEvent("paste", "example.lua")
os.queueEvent("key", keys.enter, false)
