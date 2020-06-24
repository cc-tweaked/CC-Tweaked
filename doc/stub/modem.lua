--- @module[kind=peripheral] modem

function open(channel) end
function isOpen(channel) end
function close(channel) end

--- Close all open channels.
function closeAll() end

function transmit(channel, replyChannel, payload) end

--- Determine if this is a wired or wireless modem.
--
-- Some methods (namely those dealing with wired networks and remote
-- peripherals) are only available on wired modems.
--
-- @treturn boolean @{true} if this is a wireless modem.
function isWireless() end

-- Wired modem only

--- List all remote peripherals on the wired network.
--
-- If this computer is attached to the network, it _will not_ be included in
-- this list.
--
-- > **Important:** This function only appears on wired modems. Check
-- > @{isWireless} returns false before calling it.
--
-- @treturn { string... } Remote peripheral names on the network.
function getNamesRemote(name) end

--- Determine if a peripheral is available on this wired network.
--
-- > **Important:** This function only appears on wired modems. Check
-- > @{isWireless} returns false before calling it.
--
-- @tparam string name The peripheral's name.
-- @treturn boolean If a peripheral is present with the given name.
-- @see peripheral.isPresent
function isPresentRemote(name) end

--- Get the type of a peripheral is available on this wired network.
--
-- > **Important:** This function only appears on wired modems. Check
-- > @{isWireless} returns false before calling it.
--
-- @tparam string name The peripheral's name.
-- @treturn string|nil The peripheral's type, or `nil` if it is not present.
-- @see peripheral.getType
function getTypeRemote(name) end

--- Call a method on a peripheral on this wired network.
--
-- > **Important:** This function only appears on wired modems. Check
-- > @{isWireless} returns false before calling it.
--
-- @tparam string remoteName The name of the peripheral to invoke the method on.
-- @tparam string method The name of the method
-- @param ... Additional arguments to pass to the method
-- @return The return values of the peripheral method.
-- @see peripheral.call
function callRemote(remoteName, method, ...) end

--- Returns the network name of the current computer, if the modem is on. This
-- may be used by other computers on the network to wrap this computer as a
-- peripheral.
--
-- > **Important:** This function only appears on wired modems. Check
-- > @{isWireless} returns false before calling it.
--
-- @treturn string|nil The current computer's name on the wired network.
function getNameLocal() end
