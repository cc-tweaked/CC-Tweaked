--- A very basic test framework for ComputerCraft
--
-- Like Busted (http://olivinelabs.com/busted/), but more memorable.
--
-- @usage
-- describe("something to test", function()
--   it("some property", function()
--     expect(some_function()):equals("What it should equal")
--   end)
-- end)

--- Assert an argument to the given function has the specified type.
--
-- @tparam string The function's name
-- @tparam int    The argument index to this function
-- @tparam string The type this argument should have. May be 'value' for any
--                non-nil value.
-- @param val     The value to check
-- @raise If this value doesn't match the expected type.
local function check(func, arg, ty, val)
    if ty == 'value' then
        if val == nil then
            error(('%s: bad argument #%d (got nil)'):format(func, arg), 3)
        end
    elseif type(val) ~= ty then
        return error(('%s: bad argument #%d (expected %s, got %s)'):format(func, arg, ty, type(val)), 3)
    end
end

local error_mt = { __tostring = function(self) return self.message end }

--- Attempt to execute the provided function, gathering a stack trace when it
-- errors.
--
-- @tparam
-- @return[1] true
-- @return[2] false
-- @return[2] The error object
local function try(fn)
    if not debug or not debug.traceback then
        local ok, err = pcall(fn)
        if ok or getmetatable(err) == error_mt then
            return ok, err
        else
            return ok, setmetatable({ message = tostring(err) }, error_mt)
        end
    end

    local ok, err = xpcall(fn, function(err)
        return { message = err, trace = debug.traceback() }
    end)

    -- Restore a whole bunch of state
    io.input(io.stdin)
    io.output(io.stdout)

    -- If we're an existing error, or we succeded then propagate it.
    if ok then return ok, err end
    if type(err) ~= "table" then
        return setmetatable({ message = tostring(err) }, error_mt)
    end

    if getmetatable(err.message) == error_mt then return ok, err.message end

    -- Find the common substring between the two traces. Yes, this is horrible.
    local trace = debug.traceback()
    for i = 1, #trace do
        if trace:sub(-i) ~= err.trace:sub(-i) then
            err.trace = err.trace:sub(1, -i)
            break
        end
    end

    return ok, setmetatable(err, error_mt)
end

--- Fail a test with the given message
--
-- @tparam string message The message to fail with
-- @raises An error with the given message
local function fail(message)
    check('fail', 1, 'string', message)
    error(setmetatable({ message = message, fail = true }, error_mt))
end

--- Format an object in order to make it more readable
--
-- @param value The value to format
-- @treturn string The formatted value
local function format(value)
    -- TODO: Look into something like mbs's pretty printer.
    local ok, res = pcall(textutils.serialise, value)
    if ok then return res else return tostring(value) end
end

local expect_mt = {}
expect_mt.__index = expect_mt

--- Assert that this expectation has the provided value
--
-- @param value The value to require this expectation to be equal to
-- @raises If the values are not equal
function expect_mt:equals(value)
    if value ~= self.value then
        fail(("Expected %s\n but got %s"):format(format(value), format(self.value)))
    end

    return self
end
expect_mt.equal = expect_mt.equals
expect_mt.eq = expect_mt.equals

--- Assert that this expectation does not equal the provided value
--
-- @param value The value to require this expectation to not be equal to
-- @raises If the values are equal
function expect_mt:not_equals(value)
    if value == self.value then
        fail(("Expected any value but %s"):format(format(value)))
    end

    return self
end
expect_mt.not_equal = expect_mt.not_equals
expect_mt.ne = expect_mt.not_equals

--- Assert that this expectation has something of the provided type
--
-- @tparam string exp_type The type to require this expectation to have
-- @raises If it does not have that thpe
function expect_mt:type(exp_type)
    local actual_type = type(self.value)
    if exp_type ~= actual_type then
        fail(("Expected value of type %s\n                   got %s"):format(exp_type, actual_type))
    end

    return self
end

local function are_same(eq, left, right)
    if left == right then return true end

    local ty = type(left)
    if ty ~= type(right) or ty ~= "table" then return false end

    -- If we've already explored/are exploring the left and right then return
    if eq[left] and eq[left][right] then return true end
    if not eq[left]  then eq[left] = {[right] = true} else eq[left][right] = true end
    if not eq[right] then eq[right] = {[left] = true} else eq[right][left] = true end

    -- Verify all pairs in left are equal to those in right
    for k, v in pairs(left) do
        if not are_same(eq, v, right[k]) then return false end
    end

    -- And verify all pairs in right are present in left
    for k in pairs(right) do
        if left[k] == nil then return false end
    end

    return true
end

--- Assert that this expectation is structurally equivalent to
-- the provided object.
--
-- @param value The value to check for structural equivalence
-- @raises If they are not equivalent
function expect_mt:same(value)
    if not are_same({}, self.value, value) then
        fail(("Expected %s\n but got %s"):format(format(value), format(self.value)))
    end

    return self
end

--- Construct a new expectation from the provided value
--
-- @param value The value to apply assertions to
-- @return The new expectation
local function expect(value)
    return setmetatable({ value = value}, expect_mt)
end

--- The stack of "describe"s.
local test_stack = { n = 0 }

--- Whether we're now running tests, and so cannot run any more.
local tests_locked = false

--- The list of tests that we'll run
local test_list, test_map, test_count = { }, { }, 0

--- Add a new test to our queue.
--
-- @param test The descriptor of this test
local function do_test(test)
    -- Set the name if it doesn't already exist
    if not test.name then test.name = table.concat(test_stack, "\0", 1, test_stack.n) end
    test_count = test_count + 1
    test_list[test_count] = test
    test_map[test.name] = test_count
end

--- Get the "friendly" name of this test.
--
-- @treturn string This test's friendly name
local function test_name(test) return (test.name:gsub("\0", " \26 ")) end

--- Describe something which will be tested, such as a function or situation
--
-- @tparam string name   The name of the object to test
-- @tparam function body A function which describes the tests for this object.
local function describe(name, body)
    check('describe', 1, 'string', name)
    check('describe', 2, 'function', body)
    if tests_locked then error("Cannot describe something while running tests", 2) end

    -- Push our name onto the stack, eval and pop it
    local n = test_stack.n + 1
    test_stack[n], test_stack.n = name, n

    local ok, err = try(body)

    -- We count errors as a (failing) test.
    if not ok then do_test { error = err } end

    test_stack.n = n - 1
end

--- Declare a single test within a context
--
-- @tparam string name   What you are testing
-- @tparam function body A function which runs the test, failing if it does
--                       the assertions are not met.
local function it(name, body)
    check('it', 1, 'string', name)
    check('it', 2, 'function', body)
    if tests_locked then error("Cannot create test while running tests", 2) end

    -- Push name onto the stack
    local n = test_stack.n + 1
    test_stack[n], test_stack.n, tests_locked = name, n, true

    do_test { action = body }

    -- Pop the test from the stack
    test_stack.n, tests_locked = n - 1, false
end

--- Declare a single not-yet-implemented test
--
-- @tparam string name   What you really should be testing but aren't
local function pending(name)
    check('it', 1, 'string', name)
    if tests_locked then error("Cannot create test while running tests", 2) end

    local _, loc = pcall(error, "", 3)
    loc = loc:gsub(":%s*$", "")

    local n = test_stack.n + 1
    test_stack[n], test_stack.n = name, n
    do_test { pending = true, trace = loc }
    test_stack.n = n - 1
end

local arg = ...
if arg == "--help" or arg == "-h" then
    io.write("Usage: mcfly [DIR]\n")
    io.write("\n")
    io.write("Run tests in the provided DIRectory, or `spec` if not given.")
    return
end

local root_dir = shell.resolve(arg or "spec")
if not fs.isDir(root_dir) then
    io.stderr:write(("%q is not a directory.\n"):format(root_dir))
    error()
end

do
    -- Load in the tests from all our files
    local env = setmetatable({
        expect = expect, fail = fail,
        describe = describe, it = it, pending = pending
    }, { __index = _ENV })

    local suffix = "_spec.lua"
    local function run_in(sub_dir)
        for _, name in ipairs(fs.list(sub_dir)) do
            local file = fs.combine(sub_dir, name)
            if fs.isDir(file) then
                run_in(file)
            elseif file:sub(-#suffix) == suffix then
                local fun, err = loadfile(file, env)
                if not fun then
                    do_test { name = file:sub(#root_dir + 2), error = { message = err } }
                else
                    local ok, err = try(fun)
                    if not ok then do_test { name = file:sub(#root_dir + 2), error = err } end
                end
            end
        end
    end

    run_in(root_dir)
end

-- Error if we've found no tests
if test_count == 0 then
    io.stderr:write(("Could not find any tests in %q\n"):format(root_dir))
    error()
end

-- The results of each test, as well as how many passed and the count.
local test_results, test_status, tests_run = { n = 0 }, {}, 0

-- All possible test statuses
local statuses = {
    pass    = { desc = "Pass",    col = colours.green,   dot = "\7"   }, -- Circle
    fail    = { desc = "Failed",  col = colours.red,     dot = "\4"   }, -- Diamond
    error   = { desc = "Error",   col = colours.magenta, dot = "\4"   },
    pending = { desc = "Pending", col = colours.yellow,  dot = "\186" }, -- Hollow circle
}

-- Set up each test status count.
for k in pairs(statuses) do test_status[k] = 0 end

--- Do the actual running of our test
local function do_run(test)
    -- If we're a pre-computed test, determine our status message. Otherwise,
    -- skip.
    local status, err
    if test.pending then
        status = "pending"
    elseif test.error then
        err = test.error
        status = "error"
    elseif test.action then
        local ok
        ok, err = try(test.action)
        status = ok and "pass" or (err.fail and "fail" or "error")
    end

    -- If we've a boolean status, then convert it into a string
    if status == true then status = "pass"
    elseif status == false then status = err.fail and "fail" or "error"
    end

    tests_run = tests_run + 1
    test_status[status] = test_status[status] + 1
    test_results[tests_run] = {
        status = status, name = test.name,
        message = test.message or err and err.message,
        trace = test.trace or err and err.trace,
    }

    -- If we're running under howlci, then log some info.
    if howlci then howlci.status(status, test_name(test)) end
    if cct_test then cct_test.submit(test_results[tests_run]) end

    -- Print our progress dot
    local data = statuses[status]
    term.setTextColour(data.col) io.write(data.dot)
    term.setTextColour(colours.white)
end

-- Loop over all our tests, running them as required.
if cct_test then
    -- If we're within a cct_test environment, then submit them and wait on tests
    -- to be run.
    cct_test.start(test_map)
    while true do
        local _, name = os.pullEvent("cct_test_run")
        if not name then break end
        do_run(test_list[test_map[name]])
    end
else
    for _, test in pairs(test_list) do do_run(test) end
end

-- Otherwise, display the results of each failure
io.write("\n\n")
for i = 1, tests_run do
    local test = test_results[i]
    if test.status ~= "pass" then
        local status_data = statuses[test.status]

        term.setTextColour(status_data.col)
        io.write(status_data.desc)
        term.setTextColour(colours.white)
        io.write(" \26 " .. test_name(test) .. "\n")

        if test.message then
            io.write("  " .. test.message:gsub("\n", "\n  ") .. "\n")
        end

        if test.trace then
            term.setTextColour(colours.lightGrey)
            io.write("  " .. test.trace:gsub("\n", "\n  ") .. "\n")
        end

        io.write("\n")
    end
end

-- And some summary statistics
local actual_count = tests_run - test_status.pending
local info = ("Ran %s test(s), of which %s passed (%g%%).")
    :format(actual_count, test_status.pass, (test_status.pass / actual_count) * 100)

if test_status.pending > 0 then
    info = info .. (" Skipped %d pending test(s)."):format(test_status.pending)
end

term.setTextColour(colours.white) io.write(info .. "\n")
if howlci then howlci.log("debug", info) sleep(3) end
