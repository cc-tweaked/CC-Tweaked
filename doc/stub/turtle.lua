--- Move the turtle forward one block.
-- @treturn boolean Whether the turtle could successfully move.
-- @treturn string|nil The reason the turtle could not move.
function forward() end

--- Move the turtle backwards one block.
-- @treturn boolean Whether the turtle could successfully move.
-- @treturn string|nil The reason the turtle could not move.
function back() end

--- Move the turtle up one block.
-- @treturn boolean Whether the turtle could successfully move.
-- @treturn string|nil The reason the turtle could not move.
function up() end

--- Move the turtle down one block.
-- @treturn boolean Whether the turtle could successfully move.
-- @treturn string|nil The reason the turtle could not move.
function down() end

--- Rotate the turtle 90 degress to the left.
function turnLeft() end

--- Rotate the turtle 90 degress to the right.
function turnRight() end

--- Attempt to break the block in front of the turtle.
--
-- This requires a turtle tool capable of breaking the block. Diamond pickaxes
-- (mining turtles) can break any vanilla block, but other tools (such as axes)
-- are more limited.
--
-- @tparam[opt] "left"|"right" side The specific tool to use.
-- @treturn boolean Whether a block was broken.
-- @treturn string|nil The reason no block was broken.
function dig(side) end

--- Attempt to break the block above the turtle. See @{dig} for full details.
--
-- @tparam[opt] "left"|"right" side The specific tool to use.
-- @treturn boolean Whether a block was broken.
-- @treturn string|nil The reason no block was broken.
function digUp(side) end

--- Attempt to break the block below the turtle. See @{dig} for full details.
--
-- @tparam[opt] "left"|"right" side The specific tool to use.
-- @treturn boolean Whether a block was broken.
-- @treturn string|nil The reason no block was broken.
function digDown(side) end

--- Attack the entity in front of the turtle.
--
-- @tparam[opt] "left"|"right" side The specific tool to use.
-- @treturn boolean Whether an entity was attacked.
-- @treturn string|nil The reason nothing was attacked.
function attack(side) end

--- Attack the entity above the turtle.
--
-- @tparam[opt] "left"|"right" side The specific tool to use.
-- @treturn boolean Whether an entity was attacked.
-- @treturn string|nil The reason nothing was attacked.
function attackUp(side) end

--- Attack the entity below the turtle.
--
-- @tparam[opt] "left"|"right" side The specific tool to use.
-- @treturn boolean Whether an entity was attacked.
-- @treturn string|nil The reason nothing was attacked.
function attackDown(side) end

--- Place a block or item into the world in front of the turtle.
--
-- @treturn boolean Whether the block could be placed.
-- @treturn string|nil The reason the block was not placed.
function place() end

--- Place a block or item into the world above the turtle.
--
-- @treturn boolean Whether the block could be placed.
-- @treturn string|nil The reason the block was not placed.
function placeUp() end

--- Place a block or item into the world below the turtle.
--
-- @treturn boolean Whether the block could be placed.
-- @treturn string|nil The reason the block was not placed.
function placeDown() end

--- Drop the currently selected stack into the inventory in front of the turtle,
-- or as an item into the world if there is no inventory.
--
-- @tparam[opt] number count The number of items to drop. If not given, the
-- entire stack will be dropped.
-- @treturn boolean Whether items were dropped.
-- @treturn string|nil The reason the no items were dropped.
-- @see select
function drop(count) end

--- Drop the currently selected stack into the inventory above the turtle, or as
-- an item into the world if there is no inventory.
--
-- @tparam[opt] number count The number of items to drop. If not given, the
-- entire stack will be dropped.
-- @treturn boolean Whether items were dropped.
-- @treturn string|nil The reason the no items were dropped.
-- @see select
function dropUp(count) end

--- Drop the currently selected stack into the inventory below the turtle, or as
-- an item into the world if there is no inventory.
--
-- @tparam[opt] number count The number of items to drop. If not given, the
-- entire stack will be dropped.
-- @treturn boolean Whether items were dropped.
-- @treturn string|nil The reason the no items were dropped.
-- @see select
function dropDown(count) end

--- Suck an item from the inventory in front of the turtle, or from an item
-- floating in the world.
--
-- This will pull items into the first acceptable slot, starting at the
-- @{select|currently selected} one.
--
-- @tparam[opt] number count The number of items to suck. If not given, up to a
-- stack of items will be picked up.
-- @treturn boolean Whether items were picked up.
-- @treturn string|nil The reason the no items were picked up.
function suck(count) end

--- Suck an item from the inventory above the turtle, or from an item floating
-- in the world.
--
-- @tparam[opt] number count The number of items to suck. If not given, up to a
-- stack of items will be picked up.
-- @treturn boolean Whether items were picked up.
-- @treturn string|nil The reason the no items were picked up.
function suckUp(count) end

--- Suck an item from the inventory below the turtle, or from an item floating
-- in the world.
--
-- @tparam[opt] number count The number of items to suck. If not given, up to a
-- stack of items will be picked up.
-- @treturn boolean Whether items were picked up.
-- @treturn string|nil The reason the no items were picked up.
function suckDown(count) end

--- Check if there is a solid block in front of the turtle. In this case, solid
-- refers to any non-air or liquid block.
--
-- @treturn boolean If there is a solid block in front.
function detect() end

--- Check if there is a solid block above the turtle.
--
-- @treturn boolean If there is a solid block above.
function detectUp() end

--- Check if there is a solid block below the turtle.
--
-- @treturn boolean If there is a solid block below.
function detectDown() end

function compare() end
function compareUp() end
function compareDown() end

function inspect() end
function inspectUp() end
function inspectDown() end


--- Change the currently selected slot.
--
-- The selected slot is determines what slot actions like @{drop} or
-- @{getItemCount} act on.
--
-- @tparam number slot The slot to select.
-- @see getSelectedSlot
function select(slot) end

--- Get the currently selected slot.
--
-- @treturn number The current slot.
-- @see select
function getSelectedSlot() end

--- Get the number of items in the given slot.
--
-- @tparam[opt] number slot The slot we wish to check. Defaults to the @{turtle.select|selected slot}.
-- @treturn number The number of items in this slot.
function getItemCount(slot) end

--- Get the remaining number of items which may be stored in this stack.
--
-- For instance, if a slot contains 13 blocks of dirt, it has room for another 51.
--
-- @tparam[opt] number slot The slot we wish to check. Defaults to the @{turtle.select|selected slot}.
-- @treturn number The space left in this slot.
function getItemSpace(slot) end


--- Get detailed information about the items in the given slot.
--
-- @tparam[opt] number slot The slot to get information about. Defaults to the @{turtle.select|selected slot}.
-- @tparam[opt] boolean detailed Whether to include "detailed" information. When @{true} the method will contain
-- much more information about the item at the cost of taking longer to run.
-- @treturn nil|table Information about the given slot, or @{nil} if it is empty.
-- @usage Print the current slot, assuming it contains 13 dirt.
--
--     print(textutils.serialize(turtle.getItemDetail()))
--     -- => {
--     --    name = "minecraft:dirt",
--     --    count = 13,
--     -- }
function getItemDetail(slot, detailed) end

function getFuelLevel() end

function refuel(count) end
function compareTo(slot) end
function transferTo(slot, count) end

function getFuelLimit() end
function equipLeft() end
function equipRight() end

function craft(limit) end
