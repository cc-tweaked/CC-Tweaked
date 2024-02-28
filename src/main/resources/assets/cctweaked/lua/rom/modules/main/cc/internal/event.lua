-- SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
--
-- SPDX-License-Identifier: MPL-2.0

--[[- Utilities for working with events.

> [!DANGER]
> This is an internal module and SHOULD NOT be used in your own code. It may
> be removed or changed at any time.

@local
]]

--[[-
Attempt to discard a [`event!char`] event that may follow a [`event!key`] event.

This attempts to flush the event queue via a timer, stopping early if we observe
another key or char event.

We flush the event queue by waiting a single tick. It is technically possible
the key and char events will be delivered in different ticks, but it should be
very rare, and not worth adding extra delay for.
]]
local function discard_char()
    local timer = os.startTimer(0)
    while true do
        local event, id = os.pullEvent()
        if event == "timer" and id == timer then break
        elseif event == "char" or event == "key" or event == "key_up" then
            os.cancelTimer(timer)
            break
        end
    end
end


return { discard_char = discard_char }
