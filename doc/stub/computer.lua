--- A computer or turtle wrapped as a peripheral.
--
-- This allows for basic interaction with adjacent computers. Computers wrapped
-- as peripherals will have the type `computer` while turtles will be `turtle`.
--
-- @module[kind=peripheral] computer

function turnOn() end --- Turn the other computer on.
function shutdown() end --- Shutdown the other computer.
function reboot() end --- Reboot or turn on the other computer.

--- Get the other computer's ID.
--
-- @treturn number The computer's ID.
-- @see os.getComputerID To get your computer ID.
function getID() end

--- Determine if the other computer is on.
--
-- @treturn boolean If the computer is on.
function isOn() end

--- Get the other computer's label.
--
-- @treturn string|nil The computer's label.
-- @see os.getComputerLabel To get your label.
function getLabel() end
