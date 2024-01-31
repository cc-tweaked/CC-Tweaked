-- SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

local timeout = require "test_helpers".timeout

describe("cc.internal.event", function()
    local event = require "cc.internal.event"
    describe("discard_char", function()

        local function test(events)
            local unique_event = "flush_" .. math.random(2 ^ 30)

            -- Queue and pull to flush the queue once.
            os.queueEvent(unique_event)
            os.pullEvent(unique_event)

            -- Queue our desired events
            for i = 1, #events do os.queueEvent(table.unpack(events[i])) end

            timeout(0.1, function()
                event.discard_char()

                -- Then read the remainder of the event queue, and check there's
                -- no char event.
                os.queueEvent(unique_event)
                while true do
                    local event = os.pullEvent()
                    if event == unique_event then break end
                    expect(event):ne("char")
                end
            end)
        end

        it("discards char events", function()
            test { { "char", "a" } }
        end)

        it("handles an empty event queue", function()
            test {}
        end)
    end)
end)
