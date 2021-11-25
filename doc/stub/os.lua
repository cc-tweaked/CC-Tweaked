-- Defined in bios.lua

--[[- Loads the given API into the global environment.

This function loads and executes the file at the given path, and all global
variables and functions exported by it will by available through the use of
`myAPI.<function name>`, where `myAPI` is the base name of the API file.

@tparam string path The path of the API to load.
@treturn boolean Whether or not the API was successfully loaded.
@since 1.2

@deprecated When possible it's best to avoid using this function. It pollutes
the global table and can mask errors.

@{require} should be used to load libraries instead.
]]
function loadAPI(path) end

--- Unloads an API which was loaded by @{os.loadAPI}.
--
-- This effectively removes the specified table from `_G`.
--
-- @tparam string name The name of the API to unload.
-- @since 1.2
-- @deprecated See @{os.loadAPI} for why.
function unloadAPI(name) end

--[[- Pause execution of the current thread and waits for any events matching
`filter`.

This function @{coroutine.yield|yields} the current process and waits for it
to be resumed with a vararg list where the first element matches `filter`.
If no `filter` is supplied, this will match all events.

Unlike @{os.pullEventRaw}, it will stop the application upon a "terminate"
event, printing the error "Terminated".

@tparam[opt] string filter Event to filter for.
@treturn string event The name of the event that fired.
@treturn any param... Optional additional parameters of the event.
@usage Listen for `mouse_click` events.

    while true do
        local event, button, x, y = os.pullEvent("mouse_click")
        print("Button", button, "was clicked at", x, ",", y)
    end

@usage Listen for multiple events.

    while true do
        local eventData = {os.pullEvent()}
        local event = eventData[1]

        if event == "mouse_click" then
            print("Button", eventData[2], "was clicked at", eventData[3], ",", eventData[4])
        elseif event == "key" then
            print("Key code", eventData[2], "was pressed")
        end
    end

@see os.pullEventRaw To pull the terminate event.
@changed 1.3 Added filter argument.
]]
function pullEvent(filter) end

--[[- Pause execution of the current thread and waits for events, including the
`terminate` event.

This behaves almost the same as @{os.pullEvent}, except it allows you to handle
the `terminate` event yourself - the program will not stop execution when
<kbd>Ctrl+T</kbd> is pressed.

@tparam[opt] string filter Event to filter for.
@treturn string event The name of the event that fired.
@treturn any param... Optional additional parameters of the event.
@usage Listen for `terminate` events.

    while true do
        local event = os.pullEventRaw()
        if event == "terminate" then
            print("Caught terminate event!")
        end
    end

@see os.pullEvent To pull events normally.
]]
function pullEventRaw(filter) end

--- Pauses execution for the specified number of seconds, alias of @{_G.sleep}.
--
-- @tparam number time The number of seconds to sleep for, rounded up to the
-- nearest multiple of 0.05.
function sleep(time) end

--- Get the current CraftOS version (for example, `CraftOS 1.8`).
--
-- This is defined by `bios.lua`. For the current version of CC:Tweaked, this
-- should return `CraftOS 1.8`.
--
-- @treturn string The current CraftOS version.
-- @usage os.version()
function version() end

--[[- Run the program at the given path with the specified environment and
arguments.

This function does not resolve program names like the shell does. This means
that, for example, `os.run("edit")` will not work. As well as this, it does not
provide access to the @{shell} API in the environment. For this behaviour, use
@{shell.run} instead.

If the program cannot be found, or failed to run, it will print the error and
return `false`. If you want to handle this more gracefully, use an alternative
such as @{loadfile}.

@tparam table env The environment to run the program with.
@tparam string path The exact path of the program to run.
@param ... The arguments to pass to the program.
@treturn boolean Whether or not the program ran successfully.
@usage Run the default shell from within your program:

    os.run({}, "/rom/programs/shell.lua")

@see shell.run
@see loadfile
]]
function run(env, path, ...) end
