---
module: [kind=guide] using_require
---

# Reusing code with require
A library is a collection of useful functions and other definitions which is stored separately to your main program. You
might want to create a library because you have some functions which are used in multiple programs, or just to split
your program into multiple more modular files.

Let's say we want to create a small library to make working with the @{term|terminal} a little easier. We'll provide two
functions: `reset`, which clears the terminal and sets the cursor to (1, 1), and `write_center`, which prints some text
in the middle of the screen.

Start off by creating a file called `more_term.lua`:

```lua {data-snippet=more_term}
local function reset()
  term.clear()
  term.setCursorPos(1, 1)
end

local function write_center(text)
  local x, y = term.getCursorPos()
  local width, height = term.getSize()
  term.setCursorPos(math.floor((width - #text) / 2) + 1, y)
  term.write(text)
end

return { reset = reset, write_center = write_center }
```

Now, what's going on here? We define our two functions as one might expect, and then at the bottom return a table with
the two functions. When we require this library, this table is what is returned. With that, we can then call the
original functions. Now create a new file, with the following:

```lua {data-mount=more_term:more_term.lua}
local more_term = require("more_term")
more_term.reset()
more_term.write_center("Hello, world!")
```

When run, this'll clear the screen and print some text in the middle of the first line.

## require in depth
While the previous section is a good introduction to how @{require} operates, there are a couple of remaining points
which are worth mentioning for more advanced usage.

### Libraries can return anything
In our above example, we return a table containing the functions we want to expose. However, it's worth pointing out
that you can return ''anything'' from your library - a table, a function or even just a string! @{require} treats them
all the same, and just returns whatever your library provides.

### Module resolution and the package path
In the above examples, we defined our library in a file, and @{require} read from it. While this is what you'll do most
of the time, it is possible to make @{require} look elsewhere for your library, such as downloading from a website or
loading from an in-memory library store.

As a result, the *module name* you pass to @{require} doesn't correspond to a file path. One common mistake is to load
code from a sub-directory using `require("folder/library")` or even `require("folder/library.lua")`, neither of which
will do quite what you expect.

When loading libraries (also referred to as *modules*) from files, @{require} searches along the *@{package.path|module
path}*. By default, this looks something like:

* `?.lua`
* `?/init.lua`
* `/rom/modules/main/?.lua`
* etc...

When you call `require("my_library")`, @{require} replaces the `?` in each element of the path with your module name, and
checks if the file exists. In this case, we'd look for `my_library.lua`, `my_library/init.lua`,
`/rom/modules/main/my_library.lua` and so on. Note that this works *relative to the current program*, so if your
program is actually called `folder/program`, then we'll look for `folder/my_library.lua`, etc...

One other caveat is loading libraries from sub-directories. For instance, say we have a file
`my/fancy/library.lua`. This can be loaded by using `require("my.fancy.library")` - the '.'s are replaced with '/'
before we start looking for the library.

## External links
There are several external resources which go into require in a little more detail:

 - The [Lua Module tutorial](http://lua-users.org/wiki/ModulesTutorial) on the Lua wiki.
 - [Lua's manual section on @{require}](https://www.lua.org/manual/5.1/manual.html#pdf-require).
