---
module: [kind=guide] startup
see: reference!startup
---

<!--
SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

# Running programs on computer startup
It's often useful to automatically start running a program when a computer is turned on, such as when running a [GPS
host][`gps_setup`]. This can be done with a *startup file*.

Create a file called `startup.lua` in the root of your directory with `edit /startup.lua`, and the code you want to run
on startup. Here we'll use [`shell.run`] to run the `hello` program.

```lua {data-snippet=basic data-mount=basic:startup.lua}
shell.run("hello")
```

## Multiple startup files
Startup files may also be used to define [shell autocompletions](`shell.setCompletionFunction`),
[settings][`settings.define`] or other setup. In those cases, it's may be useful to split your startup files into
separate files. In addition to `startup.lua`, computers will also run any file from the `startup/` directory.

Let's create a basic program called `example` which reads a setting, and either prints its value or toggles it:

```lua {data-snippet=example data-no-run=1}
local cmd = ...
if cmd == "get" then
  print(settings.get("example"))
elseif cmd == "toggle" then
  settings.set("example", not settings.get("example"))
  settings.save()
  print("Toggled our setting")
else
  error("Unknown command", 0)
end
```

We can then create a startup file at `startup/example.lua` which adds completion and settings for this program:

```lua {data-snippet=example_startup data-mount=empty:startup.lua,example_startup:startup/example.lua,example:example.lua}
settings.define("example", { type = "boolean", default = true })

local completion = require "cc.shell.completion"
shell.setCompletionFunction("example.lua", completion.build(
  { completion.choice, { "get", "toggle" } }
))
```

After running this startup file, typing `example` in the shell should provide auto-complete for the program. Now, we can
add another startup file at `startup/hello.lua`, which (again) runs `hello`:

```lua {data-snippet=hello_startup data-mount=empty:startup.lua,example_startup:startup/example.lua,example:example.lua,hello_startup:startup/hello.lua}
shell.run("hello")
```

See that both startup files are run!
