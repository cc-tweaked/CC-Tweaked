<!--
SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

We provide a parser for Lua source code. Here we test that the parser reports
sensible syntax errors in specific cases.

# Expressions

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

and also

```lua
return { x + 1 = 1 }
```

```txt
Unexpected = in expression.
   |
 1 | return { x + 1 = 1 }
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

## Unclosed parenthesis
We warn on unclosed parenthesis in expressions:

```lua
return (2
```

```txt
Unexpected end of file. Are you missing a closing bracket?
   |
 1 | return (2
   |        ^ Brackets were opened here.
   |
 1 | return (2
   |          ^ Unexpected end of file here.
```

Function calls:

```lua
return f(2
```

```txt
Unexpected end of file. Are you missing a closing bracket?
   |
 1 | return f(2
   |         ^ Brackets were opened here.
   |
 1 | return f(2
   |           ^ Unexpected end of file here.
```

and function definitions:

```lua
local function f(a
```

```txt
Unexpected end of file. Are you missing a closing bracket?
   |
 1 | local function f(a
   |                 ^ Brackets were opened here.
   |
 1 | local function f(a
   |                   ^ Unexpected end of file here.
```

## Missing commas in tables
We try to detect missing commas in tables, and print an appropriate error message.

```lua
return { 1 2 }
```

```txt
Unexpected number in table.
   |
 1 | return { 1 2 }
   |            ^
   |
 1 | return { 1 2 }
   |           ^ Are you missing a comma here?
```
```lua
return { 1, 2 3 }
```

```txt
Unexpected number in table.
   |
 1 | return { 1, 2 3 }
   |               ^
   |
 1 | return { 1, 2 3 }
   |              ^ Are you missing a comma here?
```

This also works with table keys.

```lua
print({ x = 1 y = 2 })
```

```txt
Unexpected identifier in table.
   |
 1 | print({ x = 1 y = 2 })
   |               ^
   |
 1 | print({ x = 1 y = 2 })
   |              ^ Are you missing a comma here?
```

```lua
print({ ["x"] = 1 ["y"] = 2 })
```

```txt
Unexpected [ in table.
   |
 1 | print({ ["x"] = 1 ["y"] = 2 })
   |                   ^
   |
 1 | print({ ["x"] = 1 ["y"] = 2 })
   |                  ^ Are you missing a comma here?
```

We gracefully handle the case where we are actually missing a closing brace.

```lua
print({ 1, )
```

```txt
Unexpected ). Are you missing a closing bracket?
   |
 1 | print({ 1, )
   |       ^ Brackets were opened here.
   |
 1 | print({ 1, )
   |            ^ Unexpected ) here.
```

# Statements

## Local functions with table identifiers
We provide a custom error for using `.` inside a `local function` name.

```lua
local function x.f() end
```

```txt
Cannot use local function with a table key.
   |
 1 | local function x.f() end
   |                 ^ . appears here.
   |
 1 | local function x.f() end
   | ^^^^^ Tip: Try removing this local keyword.
```

## Standalone identifiers
A common error is a user forgetting to use `()` to call a function. We provide
a custom error for this case:

```lua
term.clear
local _ = 1
```

```txt
Unexpected local after name.
   |
 1 | term.clear
   |           ^ Expected something before the end of the line.
Tip: Use () to call with no arguments.
```

If the next symbol is on the same line we provide a slightly different error:

```lua
x 1
```

```txt
Unexpected number after name.
   |
 1 | x 1
   |   ^
Did you mean to assign this or call it as a function?
```

An EOF token is treated as a new line.

```lua
term.clear
```

```txt
Unexpected end of file after name.
   |
 1 | term.clear
   |           ^ Expected something before the end of the line.
Tip: Use () to call with no arguments.
```

When we've got a list of variables, we only suggest assigning it.

```lua
term.clear, foo
```

```txt
Unexpected end of file after name.
   |
 1 | term.clear, foo
   |                ^
Did you mean to assign this?
```

And when we've got a partial expression, we only suggest calling it.

```lua
(a + b)
```

```txt
Unexpected end of file after name.
   |
 1 | (a + b)
   |        ^ Expected something before the end of the line.
Tip: Use () to call with no arguments.
```

## If statements
For if statements, we say when we expected the `then` keyword.

```lua
if 0
```

```txt
Expected then after if condition.
   |
 1 | if 0
   | ^^ If statement started here.
   |
 1 | if 0
   |     ^ Expected then before here.
```

```lua
if 0 then
elseif 0
```

```txt
Expected then after if condition.
   |
 2 | elseif 0
   | ^^^^^^ If statement started here.
   |
 2 | elseif 0
   |         ^ Expected then before here.
```

## Expecting `end`
We provide errors for missing `end`s.

```lua
if true then
  print("Hello")
```

```txt
Unexpected end of file. Expected end or another statement.
   |
 1 | if true then
   | ^^ Block started here.
   |
 2 |   print("Hello")
   |                 ^ Expected end of block here.
```

```lua
if true then
else
  print("Hello")
```

```txt
Unexpected end of file. Expected end or another statement.
   |
 2 | else
   | ^^^^ Block started here.
   |
 3 |   print("Hello")
   |                 ^ Expected end of block here.
```

```lua
if true then
elseif true then
  print("Hello")
```

```txt
Unexpected end of file. Expected end or another statement.
   |
 2 | elseif true then
   | ^^^^^^ Block started here.
   |
 3 |   print("Hello")
   |                 ^ Expected end of block here.
```

```lua
while true do
  print("Hello")
```

```txt
Unexpected end of file. Expected end or another statement.
   |
 1 | while true do
   | ^^^^^ Block started here.
   |
 2 |   print("Hello")
   |                 ^ Expected end of block here.
```

```lua
local function f()
```

```txt
Unexpected end of file. Expected end or another statement.
   |
 1 | local function f()
   | ^^^^^^^^^^^^^^ Block started here.
   |
 1 | local function f()
   |                   ^ Expected end of block here.
```

```lua
function f()
```

```txt
Unexpected end of file. Expected end or another statement.
   |
 1 | function f()
   | ^^^^^^^^ Block started here.
   |
 1 | function f()
   |             ^ Expected end of block here.
```

```lua
return function()
```

```txt
Unexpected end of file. Expected end or another statement.
   |
 1 | return function()
   |        ^^^^^^^^ Block started here.
   |
 1 | return function()
   |                  ^ Expected end of block here.
```

While we typically see these errors at the end of the file, there are some cases
where it may occur before then:

```lua
return (function()
  if true then
)()
```

```txt
Unexpected ). Expected end or another statement.
   |
 2 |   if true then
   |   ^^ Block started here.
   |
 3 | )()
   | ^ Expected end of block here.
```

Note we do not currently attempt to identify mismatched `end`s. This might be
something to do in the future.

```lua
if true then
  while true do
end
```

```txt
Unexpected end of file. Expected end or another statement.
   |
 1 | if true then
   | ^^ Block started here.
   |
 3 | end
   |    ^ Expected end of block here.
```

## Unexpected `end`
We also print when there's more `end`s than expected.

```lua
if true then
end
end
```

```txt
Unexpected end.
   |
 3 | end
   | ^^^
Your program contains more ends than needed. Check each block (if, for, function, ...) only has one end.
```

```lua
repeat
  if true then
  end
  end
until true
```

```txt
Unexpected end.
   |
 4 |   end
   |   ^^^
Your program contains more ends than needed. Check each block (if, for, function, ...) only has one end.
```

## `goto` and labels
We `goto` the same as normal identifiers.

```lua
goto 2
```

```txt
Unexpected number after name.
   |
 1 | goto 2
   |      ^
Did you mean to assign this or call it as a function?
```

Labels have a basic closing check:
```lua
::foo
```

```txt
Unexpected end of file.
   |
 1 | ::foo
   | ^^ Label was started here.
   |
 1 | ::foo
   |      ^ Tip: Try adding :: here.
```

But we do nothing fancy for just a `::`

```lua
::
```

```txt
Unexpected end of file.
   |
 1 | ::
   |   ^
```

## Missing function arguments
We provide an error message for missing arguments in function definitions:

```lua
function f
```

```txt
Unexpected end of file. Expected ( to start function arguments.
   |
 1 | function f
   |           ^
```

```lua
return function
```

```txt
Unexpected end of file. Expected ( to start function arguments.
   |
 1 | return function
   |                ^
```

# Function calls

## Additional commas
We suggest the user removes additional trailing commas on function calls:

```lua
f(2, )
```

```txt
Unexpected ) in function call.
   |
 1 | f(2, )
   |      ^
   |
 1 | f(2, )
   |    ^ Tip: Try removing this ,.
```

```lua
f(2, 3, )
```

```txt
Unexpected ) in function call.
   |
 1 | f(2, 3, )
   |         ^
   |
 1 | f(2, 3, )
   |       ^ Tip: Try removing this ,.
```

```lua
x:f(2, 3, )
```

```txt
Unexpected ) in function call.
   |
 1 | x:f(2, 3, )
   |           ^
   |
 1 | x:f(2, 3, )
   |         ^ Tip: Try removing this ,.
```
