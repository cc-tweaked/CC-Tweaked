--- Extend the test API with some convenience functions.
--
-- It's much easier to declare these in Lua rather than Java.

function test.assert(ok, ...)
    if ok then return ... end

    test.fail(... and tostring(...) or "Assertion failed")
end

function test.eq(expected, actual, msg)
    if expected == actual then return end

    local message = ("Assertion failed:\nExpected %s,\ngot %s"):format(expected, actual)
    if msg then message = ("%s - %s"):format(msg, message) end
    test.fail(message)
end

function test.neq(expected, actual, msg)
    if expected ~= actual then return end

    local message = ("Assertion failed:\nExpected something different to %s"):format(expected)
    if msg then message = ("%s - %s"):format(msg, message) end
    test.fail(message)
end
