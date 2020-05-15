--[[- Interact with redstone attached to this computer.

The @{redstone} library exposes three "types" of redstone control:
 - Binary input/output (@{setOutput}/@{getInput}): These simply check if a
   redstone wire has any input or output. A signal strength of 1 and 15 are
   treated the same.
 - Analogue input/output (@{setAnalogueOutput}/@{getAnalogueInput}): These
   work with the actual signal strength of the redstone wired, from 0 to 15.
 - Bundled cables (@{setBundledOutput}/@{getBundledInput}): These interact with
   "bundled" cables, such as those from Project:Red. These allow you to send
   16 separate on/off signals. Each channel corresponds to a colour, with the
   first being @{colors.white} and the last @{colors.black}.

Whenever a redstone input changes, a `redstone` event will be fired. This may
be used in or

This module may also be referred to as `rs`. For example, one may call
`rs.getSides()` instead of @{redstone.getSides}.

@module redstone
@usage Toggle the redstone signal above the computer every 0.5 seconds.

    while true do
        redstone.setOutput("top", not redstone.getOutput("top"))
        sleep(0.5)
    end
@usage Mimic a redstone comparator in [subtraction mode][comparator].

    while true do
      local rear = rs.getAnalogueInput("back")
      local sides = math.max(rs.getAnalogueInput("left"), rs.getAnalogueInput("right"))
      rs.setAnalogueOutput("front", math.max(rear - sides, 0))

      os.pullEvent("redstone") -- Wait for a change to inputs.
    end

[comparator]: https://minecraft.gamepedia.com/Redstone_Comparator#Subtract_signal_strength "Redstone Comparator on the Minecraft wiki."
]]

--- Returns a table containing the six sides of the computer. Namely, "top",
-- "bottom", "left", "right", "front" and "back".
--
-- @treturn { string... } A table of valid sides.
function getSides() end

--- Turn the redstone signal of a specific side on or off.
--
-- @tparam string side The side to set.
-- @tparam boolean on Whether the redstone signal should be on or off. When on,
-- a signal strength of 15 is emitted.
function setOutput(side, on) end

--- Get the current redstone output of a specific side.
--
-- @tparam string side The side to get.
-- @treturn boolean Whether the redstone output is on or off.
-- @see setOutput
function getOutput(side) end

--- Get the current redstone input of a specific side.
--
-- @tparam string side The side to get.
-- @treturn boolean Whether the redstone input is on or off.
function getInput(side) end

--- Set the redstone signal strength for a specific side.
--
-- @tparam string side The side to set.
-- @tparam number value The signal strength, between 0 and 15.
-- @throws If `value` is not between 0 and 15.
function setAnalogOutput(side, value) end
setAnalogueOutput = setAnalogOutput

--- Get the redstone output signal strength for a specific side.
--
-- @tparam string side The side to get.
-- @treturn number The output signal strength, between 0 and 15.
-- @see setAnalogueOutput
function getAnalogOutput(sid) end
getAnalogueOutput = getAnalogOutput

--- Get the redstone input signal strength for a specific side.
--
-- @tparam string side The side to get.
-- @treturn number The input signal strength, between 0 and 15.
function getAnalogInput(side) end
getAnalogueInput = getAnalogInput

--- Set the bundled cable output for a specific side.
--
-- @tparam string side The side to set.
-- @tparam number The colour bitmask to set.
-- @see colors.subtract For removing a colour from the bitmask.
-- @see colors.combine For adding a colour to the bitmask.
function setBundledOutput(side, output) end

--- Get the bundled cable output for a specific side.
--
-- @tparam string side The side to get.
-- @treturn number The bundled cable's output.
function getBundledOutput(side) end

--- Get the bundled cable input for a specific side.
--
-- @tparam string side The side to get.
-- @treturn number The bundled cable's input.
-- @see testBundledInput To determine if a specific colour is set.
function getBundledInput(side) end

--- Determine if a specific combination of colours are on for the given side.
--
-- @tparam string side The side to test.
-- @tparam number mask The mask to test.
-- @see getBundledInput
-- @see colors.combine For adding a colour to the bitmask.
-- @usage Check if @{colors.white} and @{colors.black} are on for above the
-- computer.
--
--     print(redstone.testBundledInput("top", colors.combine(colors.white, colors.black)))
function testBundledInput(side, mask) end
