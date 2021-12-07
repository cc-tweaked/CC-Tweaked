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
-- @tparam string func The function's name
-- @tparam int    idx  The argument index to this function
-- @tparam string ty   The type this argument should have. May be 'value' for
--                     any non-nil value.
-- @param val     val  The value to check
-- @throws If this value doesn't match the expected type.
local function check(func, idx, ty, val)
    if ty == 'value' then
        if val == nil then
            error(('%s: bad argument #%d (got nil)'):format(func, idx), 3)
        end
    elseif type(val) ~= ty then
        return error(('%s: bad argument #%d (expected %s, got %s)'):format(func, idx, ty, type(val)), 3)
    end
end

--- A stub - wraps a value within a a table,
local stub_mt = {}
stub_mt.__index = stub_mt

--- Revert this stub, restoring the previous value.
--
-- Note, a stub can only be reverted once.
function stub_mt:revert()
    if not self.active then return end

    self.active = false
    rawset(self.stubbed_in, self.key, self.original)
end

local active_stubs = {}

local function default_stub() end

--- Stub a table entry with a new value.
--
-- @tparam table tbl The table whose field should be stubbed.
-- @tparam string key The variable to stub
-- @param[opt] value The value to stub it with. If this is a function, one can
-- use the various stub expectation methods to determine what it was called
-- with. Defaults to an empty function - pass @{nil} in explicitly to set the
-- value to nil.
-- @treturn Stub The resulting stub
local function stub(tbl, key, ...)
    check('stub', 1, 'table', tbl)
    check('stub', 2, 'string', key)

    local stub = setmetatable({
        active = true,
        stubbed_in = tbl,
        key = key,
        original = rawget(tbl, key),
    }, stub_mt)

    local value = ...
    if select('#', ...) == 0 then value = default_stub end
    if type(value) == "function" then
        local arguments, delegate = {}, value
        stub.arguments = arguments
        value = function(...)
            arguments[#arguments + 1] = table.pack(...)
            return delegate(...)
        end
    end

    table.insert(active_stubs, stub)
    rawset(tbl, key, value)
    return stub
end

--- Capture the current global state of the computer
local function push_state()
    local stubs = active_stubs
    active_stubs = {}
    return {
        term = term.current(),
        input = io.input(),
        output = io.output(),
        dir = shell.dir(),
        path = shell.path(),
        aliases = shell.aliases(),
        stubs = stubs,
    }
end

--- Restore the global state of the computer to a previous version
local function pop_state(state)
    for i = #active_stubs, 1, -1 do active_stubs[i]:revert() end

    active_stubs = state.stubs

    term.redirect(state.term)
    io.input(state.input)
    io.output(state.output)
    shell.setDir(state.dir)
    shell.setPath(state.path)

    local aliases = shell.aliases()
    for k in pairs(aliases) do
        if not state.aliases[k] then shell.clearAlias(k) end
    end
    for k, v in pairs(state.aliases) do
        if aliases[k] ~= v then shell.setAlias(k, v) end
    end
end

local error_mt = { __tostring = function(self) return self.message end }

--- Attempt to execute the provided function, gathering a stack trace when it
-- errors.
--
-- @tparam function() fn The function to run
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
        return { message = err, trace = debug.traceback(nil, 2) }
    end)

    -- If we succeeded, propagate it
    if ok then return ok, err end

    -- Error handling failed for some reason - just return a simpler error
    if type(err) ~= "table" then
        return ok, setmetatable({ message = tostring(err) }, error_mt)
    end

    -- Find the common substring the errors' trace and the current one. Then
    -- eliminate it.
    local trace = debug.traceback()
    for i = 1, #trace do
        if trace:sub(-i) ~= err.trace:sub(-i) then
            err.trace = err.trace:sub(1, -i)
            break
        end
    end

    -- If we've received a rethrown error, copy
    if getmetatable(err.message) == error_mt then
        for k, v in pairs(err.message) do err[k] = v end
        return ok, err
    end

    return ok, setmetatable(err, error_mt)
end

--- Fail a test with the given message
--
-- @tparam string message The message to fail with
-- @throws An error with the given message
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

function expect_mt:_fail(message)
    if self._extra then message = self._extra .. "\n" .. message end
    fail(message)
end

--- Assert that this expectation has the provided value
--
-- @param value The value to require this expectation to be equal to
-- @throws If the values are not equal
function expect_mt:equals(value)
    if value ~= self.value then
        self:_fail(("Expected %s\n but got %s"):format(format(value), format(self.value)))
    end

    return self
end
expect_mt.equal = expect_mt.equals
expect_mt.eq = expect_mt.equals

--- Assert that this expectation does not equal the provided value
--
-- @param value The value to require this expectation to not be equal to
-- @throws If the values are equal
function expect_mt:not_equals(value)
    if value == self.value then
        self:_fail(("Expected any value but %s"):format(format(value)))
    end

    return self
end
expect_mt.not_equal = expect_mt.not_equals
expect_mt.ne = expect_mt.not_equals

--- Assert that this expectation has something of the provided type
--
-- @tparam string exp_type The type to require this expectation to have
-- @throws If it does not have that thpe
function expect_mt:type(exp_type)
    local actual_type = type(self.value)
    if exp_type ~= actual_type then
        self:_fail(("Expected value of type %s\nbut got %s"):format(exp_type, actual_type))
    end

    return self
end

local function matches(eq, exact, left, right)
    if left == right then return true end

    local ty = type(left)
    if ty ~= type(right) or ty ~= "table" then return false end

    -- If we've already explored/are exploring the left and right then return
    if eq[left] and eq[left][right] then return true end
    if not eq[left]  then eq[left] = { [right] = true } else eq[left][right] = true end
    if not eq[right] then eq[right] = { [left] = true } else eq[right][left] = true end

    -- Verify all pairs in left are equal to those in right
    for k, v in pairs(left) do
        if not matches(eq, exact, v, right[k]) then return false end
    end

    if exact then
        -- And verify all pairs in right are present in left
        for k in pairs(right) do
            if left[k] == nil then return false end
        end
    end

    return true
end

local function pairwise_equal(left, right)
    if left.n ~= right.n then return false end

    for i = 1, left.n do
        if left[i] ~= right[i] then return false end
    end

    return true
end

--- Assert that this expectation is structurally equivalent to
-- the provided object.
--
-- @param value The value to check for structural equivalence
-- @throws If they are not equivalent
function expect_mt:same(value)
    if not matches({}, true, self.value, value) then
        self:_fail(("Expected %s\nbut got %s"):format(format(value), format(self.value)))
    end

    return self
end

--- Assert that this expectation contains all fields mentioned
-- in the provided object.
--
-- @param value The value to check against
-- @throws If this does not match the provided value
function expect_mt:matches(value)
    if not matches({}, false, value, self.value) then
        self:_fail(("Expected %s\nto match %s"):format(format(self.value), format(value)))
    end

    return self
end

--- Assert that this stub was called a specific number of times.
--
-- @tparam[opt] number The exact number of times the function must be called.
-- If not given just require the function to be called at least once.
-- @throws If this function was not called the expected number of times.
function expect_mt:called(times)
    if getmetatable(self.value) ~= stub_mt or self.value.arguments == nil then
        self:_fail(("Expected stubbed function, got %s"):format(type(self.value)))
    end

    local called = #self.value.arguments

    if times == nil then
        if called == 0 then
            self:_fail("Expected stub to be called\nbut it was not.")
        end
    else
        check('stub', 1, 'number', times)
        if called ~= times then
            self:_fail(("Expected stub to be called %d times\nbut was called %d times."):format(times, called))
        end
    end

    return self
end

local function called_with_check(eq, self, ...)
    if getmetatable(self.value) ~= stub_mt or self.value.arguments == nil then
        self:_fail(("Expected stubbed function, got %s"):format(type(self.value)))
    end

    local exp_args = table.pack(...)
    local actual_args = self.value.arguments
    for i = 1, #actual_args do
        if eq(actual_args[i], exp_args) then return self end
    end

    local head = ("Expected stub to be called with %s\nbut was"):format(format(exp_args))
    if #actual_args == 0 then
        self:_fail(head .. " not called at all")
    elseif #actual_args == 1 then
        self:_fail(("%s called with %s."):format(head, format(actual_args[1])))
    else
        local lines = { head .. " called with:" }
        for i = 1, #actual_args do lines[i + 1] = " - " .. format(actual_args[i]) end

        self:_fail(table.concat(lines, "\n"))
    end
end

--- Assert that this stub was called with a set of arguments
--
-- Arguments are compared using exact equality.
function expect_mt:called_with(...)
    return called_with_check(pairwise_equal, self, ...)
end

--- Assert that this stub was called with a set of arguments
--
-- Arguments are compared using matching.
function expect_mt:called_with_matching(...)
    return called_with_check(matches, self, ...)
end

--- Assert that this expectation matches a Lua pattern
--
-- @tparam string pattern The pattern to match against
-- @throws If it does not match this pattern.
function expect_mt:str_match(pattern)
    local actual_type = type(self.value)
    if actual_type ~= "string" then
        self:_fail(("Expected value of type string\nbut got %s"):format(actual_type))
    end
    if not self.value:find(pattern) then
        self:_fail(("Expected %q\n to match pattern %q"):format(self.value, pattern))
    end

    return self
end

--- Add extra information to this error message.
--
-- @tparam string message Additional message to prepend in the case of failures.
-- @return The current
function expect_mt:describe(message)
    self._extra = tostring(message)
    return self
end

local expect = {}
setmetatable(expect, expect)

--- Construct an expectation on the error message calling this function
-- produces
--
-- @tparam fun The function to call
-- @param ... The function arguments
-- @return The new expectation
function expect.error(fun, ...)
    local ok, res = pcall(fun, ...) local _, line = pcall(error, "", 2)
    if ok then fail("expected function to error") end
    if res:sub(1, #line) == line then
        res = res:sub(#line + 1)
    elseif res:sub(1, 7) == "pcall: " then
        res = res:sub(8)
    end
    return setmetatable({ value = res }, expect_mt)
end

--- Construct a new expectation from the provided value
--
-- @param value The value to apply assertions to
-- @return The new expectation
function expect:__call(value)
    return setmetatable({ value = value }, expect_mt)
end

--- The stack of "describe"s.
local test_stack = { n = 0 }

--- Whether we're now running tests, and so cannot run any more.
local tests_locked = false

--- The list of tests that we'll run
local test_list = {}
local test_map, test_count = {}, 0

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

local native_co_create, native_loadfile = coroutine.create, loadfile
local line_counts = {}
if cct_test then
    local string_sub, debug_getinfo = string.sub, debug.getinfo
    local function debug_hook(_, line_nr)
        local name = debug_getinfo(2, "S").source
        if string_sub(name, 1, 1) ~= "@" then return end
        name = string_sub(name, 2)

        local file = line_counts[name]
        if not file then file = {} line_counts[name] = file end
        file[line_nr] = (file[line_nr] or 0) + 1
    end

    coroutine.create = function(...)
        local co = native_co_create(...)
        debug.sethook(co, debug_hook, "l")
        return co
    end

    local expect = require "cc.expect".expect
    _G.native_loadfile = native_loadfile
    _G.loadfile = function(filename, mode, env)
        -- Support the previous `loadfile(filename, env)` form instead.
        if type(mode) == "table" and env == nil then
            mode, env = nil, mode
        end

        expect(1, filename, "string")
        expect(2, mode, "string", "nil")
        expect(3, env, "table", "nil")

        local file = fs.open(filename, "r")
        if not file then return nil, "File not found" end

        local func, err = load(file.readAll(), "@/" .. fs.combine(filename, ""), mode, env)
        file.close()
        return func, err
    end

    debug.sethook(debug_hook, "l")
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

-- Ensure the test folder is also on the package path
package.path = ("/%s/?.lua;/%s/?/init.lua;%s"):format(root_dir, root_dir, package.path)

do
    -- Load in the tests from all our files
    local env = setmetatable({}, { __index = _ENV })

    local function set_env(tbl)
        for k in pairs(env) do env[k] = nil end
        for k, v in pairs(tbl) do env[k] = v end
    end

    -- When declaring tests, you shouldn't be able to use test methods
    set_env { describe = describe, it = it, pending = pending }

    local suffix = "_spec.lua"
    local function run_in(sub_dir)
        for _, name in ipairs(fs.list(sub_dir)) do
            local file = fs.combine(sub_dir, name)
            if fs.isDir(file) then
                run_in(file)
            elseif file:sub(-#suffix) == suffix then
                local fun, err = loadfile(file, nil, env)
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

    -- When running tests, you shouldn't be able to declare new ones.
    set_env { expect = expect, fail = fail, stub = stub }
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
        local state = push_state()

        -- Flush the event queue and ensure we're running with 0 timeout.
        os.queueEvent("start_test") os.pullEvent("start_test")

        local ok
        ok, err = try(test.action)
        status = ok and "pass" or (err.fail and "fail" or "error")

        pop_state(state)
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
    :format(actual_count, test_status.pass, test_status.pass / actual_count * 100)

if test_status.pending > 0 then
    info = info .. (" Skipped %d pending test(s)."):format(test_status.pending)
end

term.setTextColour(colours.white) io.write(info .. "\n")

-- Restore hook stubs
debug.sethook(nil, "l")
coroutine.create = native_co_create
_G.loadfile = native_loadfile

if cct_test then cct_test.finish(line_counts) end
if howlci then howlci.log("debug", info) sleep(3) end
