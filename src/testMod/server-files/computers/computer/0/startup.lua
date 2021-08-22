local label = os.getComputerLabel()
if label == nil then return test.fail("Label a computer to use it.") end

local fn, err = loadfile("tests/" .. label .. ".lua", nil, _ENV)
if not fn then return test.fail(err) end

local ok, err = pcall(fn)
if not ok then return test.fail(err) end

print("Run " .. label)
test.ok()
