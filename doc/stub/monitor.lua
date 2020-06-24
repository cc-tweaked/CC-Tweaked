--[[- Monitors are a block which act as a terminal, displaying information on
one side. This allows them to be read and interacted with in-world without
opening a GUI.

Monitors act as @{term.Redirect|terminal redirects} and so expose the same
methods, as well as several additional ones, which are documented below.

Like computers, monitors come in both normal (no colour) and advanced (colour)
varieties.

@module[kind=peripheral] monitor
@usage Write "Hello, world!" to an adjacent monitor:

    local monitor = peripheral.find("monitor")
    monitor.setCursorPos(1, 1)
    monitor.write("Hello, world!")
]]


--- Set the scale of this monitor. A larger scale will result in the monitor
-- having a lower resolution, but display text much larger.
--
-- @tparam number scale The monitor's scale. This must be a multiple of 0.5
-- between 0.5 and 5.
-- @throws If the scale is out of range.
-- @see getTextScale
function setTextScale(scale) end

--- Get the monitor's current text scale.
--
-- @treturn number The monitor's current scale.
function getTextScale() end
