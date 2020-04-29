function queueEvent(event, ...) end
function startTimer(delay) end
function setAlarm(time) end
function shutdown() end
function reboot() end
function getComputerID() end
computerID = getComputerID
function setComputerLabel(label) end
function getComputerLabel() end
computerLabel = getComputerLabel
function clock() end
function time(timezone) end
function day(timezone) end
function cancelTimer(id) end
function cancelAlarm(id) end
function epoch(timezone) end
function date(format, time) end

-- Defined in bios.lua
function loadAPI(path) end
function pullEvent(filter) end
function pullEventRaw(filter) end
function version() end
function run(env, path, ...) end
