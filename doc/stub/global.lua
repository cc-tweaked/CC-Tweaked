--[[-
Global functions defined by `bios.lua`. This does not include standard Lua
functions.

@module _G
]]

--[[- Pauses execution for the specified number of seconds.

As it waits for a fixed amount of world ticks, `time` will automatically be
rounded up to the nearest multiple of 0.05 seconds. If you are using coroutines
or the @{parallel|parallel API}, it will only pause execution of the current
thread, not the whole program.

**Note** Because sleep internally uses timers, it is a function that yields.
This means that you can use it to prevent "Too long without yielding" errors,
however, as the minimum sleep time is 0.05 seconds, it will slow your program
down.

**Warning** Internally, this function queues and waits for a timer event (using
@{os.startTimer}), however it does not listen for any other events. This means
that any event that occurs while sleeping will be entirely discarded. If you
need to receive events while sleeping, consider using @{os.startTimer|timers},
or the @{parallel|parallel API}.

@tparam number time The number of seconds to sleep for, rounded up to the
nearest multiple of 0.05.

@see os.startTimer
]]
function sleep(time) end

--- Writes a line of text to the screen without a newline at the end, wrapping
-- text if necessary.
--
-- @tparam string text The text to write to the string
-- @treturn number The number of lines written
-- @see print A wrapper around write that adds a newline and accepts multiple arguments
function write(text) end

--- Prints the specified values to the screen separated by spaces, wrapping if
-- necessary. After printing, the cursor is moved to the next line.
--
-- @tparam any ... The values to print on the screen
-- @treturn number The number of lines written
function print(...) end

--- Prints the specified values to the screen in red, separated by spaces, 
-- wrapping if necessary. After printing, the cursor is moved to the next line.
--
-- @tparam any ... The values to print on the screen
function printError(...) end

--[[- Reads user input from the terminal, automatically handling arrow keys,
pasting, character replacement, history scrollback, auto-completion, and
default values.

@tparam[opt] string replaceChar A character to replace each typed character with.
This can be used for hiding passwords, for example.
@tparam[opt] table history A table holding history items that can be scrolled
back to with the up/down arrow keys. The oldest item is at index 1, while the
newest item is at the highest index.
@tparam[opt] function(partial: string):({ string}|nil) completeFn A function to be
used for completion. This function should take the partial text typed so far, and
returns a list of possible completion options.
@tparam[opt] string default The default value to return if no text is entered.
@treturn string The text typed in, or if nothing was entered and a default value
was specified, the default string.
]]
function read(replaceChar, history, completeFn, default) end

--- The ComputerCraft and Minecraft version of the current computer environment.
--
-- For example, `ComputerCraft 1.93.0 (Minecraft 1.15.2)`.
_HOST = _HOST

--[[- The default computer settings as defined in the ComputerCraft
configuration.

This is a comma-separated list of settings pairs defined by the mod
configuration or server owner. By default, it is empty.

An example value to disable autocompletion:

    shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false

]]
_CC_DEFAULT_SETTINGS = _CC_DEFAULT_SETTINGS
