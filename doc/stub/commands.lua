--- Execute a specific command.
--
-- @tparam string command The command to execute.
-- @treturn boolean Whether the command executed successfully.
-- @treturn { string... } The output of this command, as a list of lines.
-- @treturn number|nil The number of "affected" objects, or `nil` if the command
-- failed. The definition of this varies from command to command.
-- @usage Set the block above the command computer to stone.
--
--     commands.exec("setblock ~ ~1 ~ minecraft:stone")
function exec(command) end

--- Asynchronously execute a command.
--
-- Unlike @{exec}, this will immediately return, instead of waiting for the
-- command to execute. This allows you to run multiple commands at the same
-- time.
--
-- When this command has finished executing, it will queue a `task_complete`
-- event containing the result of executing this command (what @{exec} would
-- return).
--
-- @tparam string command The command to execute.
-- @treturn number The "task id". When this command has been executed, it will
-- queue a `task_complete` event with a matching id.
-- @usage Asynchronously sets the block above the computer to stone.
--
--     commands.execAsync("~ ~1 ~ minecraft:stone")
-- @see parallel One may also use the parallel API to run multiple commands at
-- once.
function execAsync(commad) end

--- List all available commands which the computer has permission to execute.
--
-- @treturn { string... } A list of all available commands
function list() end

--- Get the position of the current command computer.
--
-- @treturn number This computer's x position.
-- @treturn number This computer's y position.
-- @treturn number This computer's z position.
-- @see gps.locate To get the position of a non-command computer.
function getBlockPosition() end

--- Get some basic information about a block.
--
-- The returned table contains the current name, metadata and block state (as
-- with @{turtle.inspect}). If there is a tile entity for that block, its NBT
-- will also be returned.
--
-- @tparam number x The x position of the block to query.
-- @tparam number y The y position of the block to query.
-- @tparam number z The z position of the block to query.
-- @treturn table The given block's information.
-- @throws If the coordinates are not within the world, or are not currently
-- loaded.
function getBlockInfo(x, y, z) end

--- Get information about a range of blocks.
--
-- This returns the same information as @{getBlockInfo}, just for multiple
-- blocks at once.
--
-- Blocks are traversed by ascending y level, followed by z and x - the returned
-- table may be indexed using `x + z*width + y*depth*depth`.
--
-- @tparam number min_x The start x coordinate of the range to query.
-- @tparam number min_y The start y coordinate of the range to query.
-- @tparam number min_z The start z coordinate of the range to query.
-- @tparam number max_x The end x coordinate of the range to query.
-- @tparam number max_y The end y coordinate of the range to query.
-- @tparam number max_z The end z coordinate of the range to query.
-- @treturn { table... } A list of information about each block.
-- @throws If the coordinates are not within the world.
-- @throws If trying to get information about more than 4096 blocks.
function getBlockInfos(min_x, min_y, min_z, max_x, max_y, max_z) end
