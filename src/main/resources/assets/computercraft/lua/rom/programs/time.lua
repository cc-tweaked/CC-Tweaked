local translate = require("cc.translate").translate

local nTime = os.time()
local nDay = os.day()
print(translate("cc.time.current_time"):format(textutils.formatTime(nTime, false), nDay))
