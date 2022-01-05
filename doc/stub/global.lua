--[[-
Functions in the global environment, defined in `bios.lua`. This does not
include standard Lua functions.

@module _G
]]

--[[- Pauses execution for the specified number of seconds.

As it waits for a fixed amount of world ticks, `time` will automatically be
rounded up to the nearest multiple of 0.05 seconds. If you are using coroutines
or the @{parallel|parallel API}, it will only pause execution of the current
thread, not the whole program.

:::tip
Because sleep internally uses timers, it is a function that yields. This means
that you can use it to prevent "Too long without yielding" errors, however, as
the minimum sleep time is 0.05 seconds, it will slow your program down.
:::

:::caution
Internally, this function queues and waits for a timer event (using
@{os.startTimer}), however it does not listen for any other events. This means
that any event that occurs while sleeping will be entirely discarded. If you
need to receive events while sleeping, consider using @{os.startTimer|timers},
or the @{parallel|parallel API}.
:::

@tparam number time The number of seconds to sleep for, rounded up to the
nearest multiple of 0.05.

@see os.startTimer
@usage Sleep for three seconds.

    print("Sleeping for three seconds")
    sleep(3)
    print("Done!")
]]
function sleep(time) end

--- Writes a line of text to the screen without a newline at the end, wrapping
-- text if necessary.
--
-- @tparam string text The text to write to the string
-- @treturn number The number of lines written
-- @see print A wrapper around write that adds a newline and accepts multiple arguments
-- @usage write("Hello, world")
function write(text) end

--- Prints the specified values to the screen separated by spaces, wrapping if
-- necessary. After printing, the cursor is moved to the next line.
--
-- @param ... The values to print on the screen
-- @treturn number The number of lines written
-- @usage print("Hello, world!")
function print(...) end

--- Prints the specified values to the screen in red, separated by spaces,
-- wrapping if necessary. After printing, the cursor is moved to the next line.
--
-- @param ... The values to print on the screen
-- @usage printError("Something went wrong!")
function printError(...) end

--[[- Reads user input from the terminal, automatically handling arrow keys,
pasting, character replacement, history scrollback, auto-completion, and
default values.

@tparam[opt] string replaceChar A character to replace each typed character with.
This can be used for hiding passwords, for example.
@tparam[opt] table history A table holding history items that can be scrolled
back to with the up/down arrow keys. The oldest item is at index 1, while the
newest item is at the highest index.
@tparam[opt] function(partial: string):({ string... }|nil) completeFn A function
to be used for completion. This function should take the partial text typed so
far, and returns a list of possible completion options.
@tparam[opt] string default Default text which should already be entered into
the prompt.

@treturn string The text typed in.

@see cc.completion For functions to help with completion.
@usage Read a string and echo it back to the user

    write("> ")
    local msg = read()
    print(msg)

@usage Prompt a user for a password.

    while true do
      write("Password> ")
      local pwd = read("*")
      if pwd == "let me in" then break end
      print("Incorrect password, try again.")
    end
    print("Logged in!")

@usage A complete example with completion, history and a default value.

    local completion = require "cc.completion"
    local history = { "potato", "orange", "apple" }
    local choices = { "apple", "orange", "banana", "strawberry" }
    write("> ")
    local msg = read(nil, history, function(text) return completion.choice(text, choices) end, "app")
    print(msg)

@changed 1.74 Added `completeFn` parameter.
@changed 1.80pr1 Added `default` parameter.
]]
function read(replaceChar, history, completeFn, default) end

--- The ComputerCraft and Minecraft version of the current computer environment.
--
-- For example, `ComputerCraft 1.93.0 (Minecraft 1.15.2)`.
-- @usage _HOST
-- @since 1.76
_HOST = _HOST

--[[- The default computer settings as defined in the ComputerCraft
configuration.

This is a comma-separated list of settings pairs defined by the mod
configuration or server owner. By default, it is empty.

An example value to disable autocompletion:

    shell.autocomplete=false,lua.autocomplete=false,edit.autocomplete=false

@usage _CC_DEFAULT_SETTINGS
@since 1.77
]]
_CC_DEFAULT_SETTINGS = _CC_DEFAULT_SETTINGS
