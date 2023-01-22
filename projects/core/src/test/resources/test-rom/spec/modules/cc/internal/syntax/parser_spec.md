We provide a parser for Lua source code. Here we test that the parser reports
sensible syntax errors in specific cases.

## Invalid equals
We correct the user if they type `=` instead of `==`.

```lua
if a = b then end
```

```txt
Unexpected = in expression.
   |
 1 | if a = b then end
   |      ^
Tip: Replace this with == to check if two values are equal.
```

We apply a slightly different error when this occurs in tables:

```lua
return { "abc" = "def" }
```

```txt
Unexpected = in expression.
   |
 1 | return { "abc" = "def" }
   |                ^
Tip: Wrap the preceding expression in [ and ] to use it as a table key.
```

Note this doesn't occur if this there's already a table key here:

```lua
return { x = "abc" = }
```

```txt
Unexpected = in expression.
   |
 1 | return { x = "abc" = }
   |                    ^
Tip: Replace this with == to check if two values are equal.
```

## Local functions with table identifiers

```lua
local function x.f() end
```

```txt
Cannot use local function with tables.
   |
 1 | local function x.f() end
   |                 ^ . appears here.
   |
 1 | local function x.f() end
   | ^^^^^ Tip: Try removing this local keyword.
```

## Unclosed parenthesis
We warn on unclosed parenthesis in expressions:

```lua
return (2
```

```txt
Brackets were not closed.
   |
 1 | return (2
   |        ^ Brackets were opened here.
   |
 1 | return (2
   |          ^ Expected to be closed before here.
```

Function calls:

```lua
return f(2
```

```txt
Brackets were not closed.
   |
 1 | return f(2
   |         ^ Brackets were opened here.
   |
 1 | return f(2
   |           ^ Expected to be closed before here.
```

and function definitions:

```lua
local function f(a
```

```txt
Brackets were not closed.
   |
 1 | local function f(a
   |                 ^ Brackets were opened here.
   |
 1 | local function f(a
   |                   ^ Expected to be closed before here.
```

## Standalone identifiers

We suggest function calls:

```lua
term.clear
local _ = 1
```

```txt
Unexpected symbol after variable.
   |
 1 | term.clear
   |           ^ Expected something before the end of the line.
Tip: Use () to call with no arguments.
```

And assigns/calls:

```lua
x 1
```

```txt
Unexpected symbol after name.
   |
 1 | x 1
   |   ^
Did you mean to assign this or call it as a function?
```
