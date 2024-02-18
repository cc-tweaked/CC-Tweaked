<!--
SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

An exhaustive list of all error states in the parser, and the error messages we
generate for each one. This is _not_ a complete collection of all possible
errors, but is a useful guide for where we might be providing terrible messages.

```lua
do until
-- Line 1: 'end' expected near 'until' (program)
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | do until
   | ^^ Block started here.
   |
 1 | do until
   |    ^^^^^ Expected end of block here.
```


```lua {repl_exprs}
... true
-- Line 1: <eof> expected near 'true' (repl_exprs)
```

```txt
Unexpected true.
   |
 1 | ... true
   |     ^^^^
```


```lua
:: xyz while
-- Line 1: '::' expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | :: xyz while
   | ^^ Label was started here.
   |
 1 | :: xyz while
   |        ^^^^^ Tip: Try adding :: here.
```


```lua
:: while
-- Line 1: <name> expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | :: while
   |    ^^^^^
```


```lua
for xyz , xyz while
-- Line 1: 'in' expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | for xyz , xyz while
   |               ^^^^^
```


```lua
for xyz , while
-- Line 1: <name> expected near 'while' (program)
```

```txt
Unexpected while. Expected a variable name.
   |
 1 | for xyz , while
   |           ^^^^^
```


```lua
for xyz = xyz , xyz , xyz do until
-- Line 1: 'end' expected near 'until' (program)
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | for xyz = xyz , xyz , xyz do until
   | ^^^ Block started here.
   |
 1 | for xyz = xyz , xyz , xyz do until
   |                              ^^^^^ Expected end of block here.
```


```lua
for xyz = xyz , xyz , xyz while
-- Line 1: 'do' expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | for xyz = xyz , xyz , xyz while
   |                           ^^^^^
```


```lua
for xyz = xyz , xyz , while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | for xyz = xyz , xyz , while
   |                       ^^^^^
```


```lua
for xyz = xyz , xyz do until
-- Line 1: 'end' expected near 'until' (program)
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | for xyz = xyz , xyz do until
   | ^^^ Block started here.
   |
 1 | for xyz = xyz , xyz do until
   |                        ^^^^^ Expected end of block here.
```


```lua
for xyz = xyz , xyz while
-- Line 1: 'do' expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | for xyz = xyz , xyz while
   |                     ^^^^^
```


```lua
for xyz = xyz , while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | for xyz = xyz , while
   |                 ^^^^^
```


```lua
for xyz = xyz while
-- Line 1: ',' expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | for xyz = xyz while
   |               ^^^^^
```


```lua
for xyz = while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | for xyz = while
   |           ^^^^^
```


```lua
for xyz in xyz do until
-- Line 1: 'end' expected near 'until' (program)
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | for xyz in xyz do until
   | ^^^ Block started here.
   |
 1 | for xyz in xyz do until
   |                   ^^^^^ Expected end of block here.
```


```lua
for xyz in xyz while
-- Line 1: 'do' expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | for xyz in xyz while
   |                ^^^^^
```


```lua
for xyz in while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | for xyz in while
   |            ^^^^^
```


```lua
for xyz while
-- Line 1: '=' or 'in' expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | for xyz while
   |         ^^^^^
```


```lua
for while
-- Line 1: <name> expected near 'while' (program)
```

```txt
Unexpected while. Expected a variable name.
   |
 1 | for while
   |     ^^^^^
```


```lua
function xyz : while
-- Line 1: <name> expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | function xyz : while
   |                ^^^^^
```


```lua
function xyz . while
-- Line 1: <name> expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | function xyz . while
   |                ^^^^^
```


```lua
function xyz ( ) until
-- Line 1: 'end' expected near 'until' (program)
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | function xyz ( ) until
   | ^^^^^^^^ Block started here.
   |
 1 | function xyz ( ) until
   |                  ^^^^^ Expected end of block here.
```


```lua
function xyz while
-- Line 1: '(' expected near 'while' (program)
```

```txt
Unexpected while. Expected ( to start function arguments.
   |
 1 | function xyz while
   |              ^^^^^
```


```lua {repl_exprs}
function ( ) until
-- Line 1: 'end' expected near 'until' (repl_exprs)
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | function ( ) until
   | ^^^^^^^^ Block started here.
   |
 1 | function ( ) until
   |              ^^^^^ Expected end of block here.
```


```lua {repl_exprs}
function ( xyz , while
-- Line 1: <name> or '...' expected near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected a variable name.
   |
 1 | function ( xyz , while
   |                  ^^^^^
```


```lua {repl_exprs}
function ( xyz while
-- Line 1: ')' expected near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | function ( xyz while
   |          ^ Brackets were opened here.
   |
 1 | function ( xyz while
   |                ^^^^^ Unexpected while here.
```


```lua {repl_exprs}
function ( while
-- Line 1: <name> or '...' expected near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | function ( while
   |          ^ Brackets were opened here.
   |
 1 | function ( while
   |            ^^^^^ Unexpected while here.
```


```lua
function while
-- Line 1: <name> expected near 'while' (program)
```

```txt
Unexpected while. Expected a variable name.
   |
 1 | function while
   |          ^^^^^
```


```lua {repl_exprs}
function while
-- Line 1: <name> expected near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected ( to start function arguments.
   |
 1 | function while
   |          ^^^^^
```


```lua {repl_exprs}
xyz + while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz + while
   |       ^^^^^
```


```lua {repl_exprs}
xyz and while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz and while
   |         ^^^^^
```


```lua {repl_exprs}
xyz : xyz while
-- Line 1: function arguments expected near 'while' (repl_exprs)
```

```txt
Unexpected while.
   |
 1 | xyz : xyz while
   |           ^^^^^
```


```lua {repl_exprs}
xyz : while
-- Line 1: <name> expected near 'while' (repl_exprs)
```

```txt
Unexpected while.
   |
 1 | xyz : while
   |       ^^^^^
```


```lua {repl_exprs}
xyz , xyz then
-- Line 1: '=' expected near 'then' (repl_exprs)
```

```txt
Unexpected then.
   |
 1 | xyz , xyz then
   |           ^^^^
```


```lua
xyz , xyz while
-- Line 1: '=' expected near 'while' (program)
```

```txt
Unexpected while after name.
   |
 1 | xyz , xyz while
   |           ^
Did you mean to assign this?
```


```lua
xyz , while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected a variable name.
   |
 1 | xyz , while
   |       ^^^^^
```


```lua {repl_exprs}
xyz , while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz , while
   |       ^^^^^
```


```lua {repl_exprs}
xyz .. while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz .. while
   |        ^^^^^
```


```lua {repl_exprs}
xyz / while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz / while
   |       ^^^^^
```


```lua {repl_exprs}
xyz . while
-- Line 1: <name> expected near 'while' (repl_exprs)
```

```txt
Unexpected while.
   |
 1 | xyz . while
   |       ^^^^^
```


```lua {repl_exprs}
xyz == while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz == while
   |        ^^^^^
```


```lua
xyz = xyz )
-- Line 1: unexpected symbol near ')' (program)
```

```txt
Unexpected ). Expected a statement.
   |
 1 | xyz = xyz )
   |           ^
```


```lua
xyz = while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz = while
   |       ^^^^^
```


```lua {repl_exprs}
xyz >= while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz >= while
   |        ^^^^^
```


```lua {repl_exprs}
xyz > while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz > while
   |       ^^^^^
```


```lua {repl_exprs}
xyz <= while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz <= while
   |        ^^^^^
```


```lua {repl_exprs}
xyz < while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz < while
   |       ^^^^^
```


```lua {repl_exprs}
xyz % while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz % while
   |       ^^^^^
```


```lua {repl_exprs}
xyz * while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz * while
   |       ^^^^^
```


```lua {repl_exprs}
xyz ~= while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz ~= while
   |        ^^^^^
```


```lua {repl_exprs}
xyz ( xyz until
-- Line 1: ')' expected near 'until' (repl_exprs)
```

```txt
Unexpected until. Are you missing a closing bracket?
   |
 1 | xyz ( xyz until
   |     ^ Brackets were opened here.
   |
 1 | xyz ( xyz until
   |           ^^^^^ Unexpected until here.
```


```lua {repl_exprs}
xyz ( while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | xyz ( while
   |     ^ Brackets were opened here.
   |
 1 | xyz ( while
   |       ^^^^^ Unexpected while here.
```


```lua {repl_exprs}
xyz or while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz or while
   |        ^^^^^
```


```lua {repl_exprs}
xyz [ xyz while
-- Line 1: ']' expected near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | xyz [ xyz while
   |     ^ Brackets were opened here.
   |
 1 | xyz [ xyz while
   |           ^^^^^ Unexpected while here.
```


```lua {repl_exprs}
xyz [ while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz [ while
   |       ^^^^^
```


```lua {repl_exprs}
xyz ^ while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz ^ while
   |       ^^^^^
```


```lua
xyz 'abc' true
-- Line 1: unexpected symbol near 'true' (program)
```

```txt
Unexpected true. Expected a statement.
   |
 1 | xyz 'abc' true
   |           ^^^^
```


```lua {repl_exprs}
xyz - while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | xyz - while
   |       ^^^^^
```


```lua {repl_exprs}
xyz then
-- Line 1: syntax error near 'then' (repl_exprs)
```

```txt
Unexpected then.
   |
 1 | xyz then
   |     ^^^^
```


```lua {repl_exprs}
xyz true
-- Line 1: syntax error near 'true' (repl_exprs)
```

```txt
Unexpected true.
   |
 1 | xyz true
   |     ^^^^
```


```lua
xyz while
-- Line 1: syntax error near 'while' (program)
```

```txt
Unexpected while after name.
   |
 1 | xyz while
   |     ^
Did you mean to assign this or call it as a function?
```


```lua {repl_exprs}
xyz while
-- Line 1: syntax error near 'while' (repl_exprs)
```

```txt
Unexpected while.
   |
 1 | xyz while
   |     ^^^^^
```


```lua
xyz while
-- Line 1: syntax error near 'while' (program)
```

```txt
Unexpected while after name.
   |
 1 | xyz while
   |     ^
Did you mean to assign this or call it as a function?
```


```lua
if xyz then else until
-- Line 1: 'end' expected near 'until' (program)
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | if xyz then else until
   |             ^^^^ Block started here.
   |
 1 | if xyz then else until
   |                  ^^^^^ Expected end of block here.
```


```lua
if xyz then elseif xyz while
-- Line 1: 'then' expected near 'while' (program)
```

```txt
Expected then after if condition.
   |
 1 | if xyz then elseif xyz while
   |             ^^^^^^ If statement started here.
   |
 1 | if xyz then elseif xyz while
   |                        ^ Expected then before here.
```


```lua
if xyz then elseif while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | if xyz then elseif while
   |                    ^^^^^
```


```lua
if xyz then until
-- Line 1: 'end' expected near 'until' (program)
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | if xyz then until
   | ^^ Block started here.
   |
 1 | if xyz then until
   |             ^^^^^ Expected end of block here.
```


```lua
if xyz while
-- Line 1: 'then' expected near 'while' (program)
```

```txt
Expected then after if condition.
   |
 1 | if xyz while
   | ^^ If statement started here.
   |
 1 | if xyz while
   |        ^ Expected then before here.
```


```lua
if while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | if while
   |    ^^^^^
```


```lua {repl_exprs}
# while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | # while
   |   ^^^^^
```


```lua
local function xyz ( ) until
-- Line 1: 'end' expected near 'until' (program)
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | local function xyz ( ) until
   | ^^^^^^^^^^^^^^ Block started here.
   |
 1 | local function xyz ( ) until
   |                        ^^^^^ Expected end of block here.
```


```lua
local function xyz while
-- Line 1: '(' expected near 'while' (program)
```

```txt
Unexpected while. Expected ( to start function arguments.
   |
 1 | local function xyz while
   |                    ^^^^^
```


```lua
local function while
-- Line 1: <name> expected near 'while' (program)
```

```txt
Unexpected while. Expected a variable name.
   |
 1 | local function while
   |                ^^^^^
```


```lua
local xyz = xyz )
-- Line 1: unexpected symbol near ')' (program)
```

```txt
Unexpected ). Expected a statement.
   |
 1 | local xyz = xyz )
   |                 ^
```


```lua
local xyz = while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | local xyz = while
   |             ^^^^^
```


```lua
local xyz true
-- Line 1: unexpected symbol near 'true' (program)
```

```txt
Unexpected true. Expected a statement.
   |
 1 | local xyz true
   |           ^^^^
```


```lua
local while
-- Line 1: <name> expected near 'while' (program)
```

```txt
Unexpected while. Expected a variable name.
   |
 1 | local while
   |       ^^^^^
```


```lua {repl_exprs}
not while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | not while
   |     ^^^^^
```


```lua {repl_exprs}
{ xyz = xyz while
-- Line 1: '}' expected near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | { xyz = xyz while
   | ^ Brackets were opened here.
   |
 1 | { xyz = xyz while
   |             ^^^^^ Unexpected while here.
```


```lua {repl_exprs}
{ xyz = while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | { xyz = while
   |         ^^^^^
```


```lua {repl_exprs}
{ xyz ; while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | { xyz ; while
   | ^ Brackets were opened here.
   |
 1 | { xyz ; while
   |         ^^^^^ Unexpected while here.
```


```lua {repl_exprs}
{ xyz while
-- Line 1: '}' expected near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | { xyz while
   | ^ Brackets were opened here.
   |
 1 | { xyz while
   |       ^^^^^ Unexpected while here.
```


```lua {repl_exprs}
{ [ xyz ] = xyz while
-- Line 1: '}' expected near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | { [ xyz ] = xyz while
   | ^ Brackets were opened here.
   |
 1 | { [ xyz ] = xyz while
   |                 ^^^^^ Unexpected while here.
```


```lua {repl_exprs}
{ [ xyz ] = while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | { [ xyz ] = while
   |             ^^^^^
```


```lua {repl_exprs}
{ [ xyz ] while
-- Line 1: '=' expected near 'while' (repl_exprs)
```

```txt
Unexpected while.
   |
 1 | { [ xyz ] while
   |           ^^^^^
```


```lua {repl_exprs}
{ [ xyz while
-- Line 1: ']' expected near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | { [ xyz while
   |   ^ Brackets were opened here.
   |
 1 | { [ xyz while
   |         ^^^^^ Unexpected while here.
```


```lua {repl_exprs}
{ [ while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | { [ while
   |     ^^^^^
```


```lua {repl_exprs}
{ while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | { while
   | ^ Brackets were opened here.
   |
 1 | { while
   |   ^^^^^ Unexpected while here.
```


```lua
( xyz ) while
-- Line 1: syntax error near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | ( xyz ) while
   |         ^^^^^
```


```lua {repl_exprs}
( xyz while
-- Line 1: ')' expected near 'while' (repl_exprs)
```

```txt
Unexpected while. Are you missing a closing bracket?
   |
 1 | ( xyz while
   | ^ Brackets were opened here.
   |
 1 | ( xyz while
   |       ^^^^^ Unexpected while here.
```


```lua {repl_exprs}
( while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | ( while
   |   ^^^^^
```


```lua
repeat --[[eof]]
-- Line 1: 'until' expected near <eof> (program)
```

```txt
Unexpected end of file. Expected a variable name.
   |
 2 | -- Line 1: 'until' expected near <eof> (program)
   |                                                 ^
```


```lua
repeat until xyz then
-- Line 1: unexpected symbol near 'then' (program)
```

```txt
Unexpected then. Expected a statement.
   |
 1 | repeat until xyz then
   |                  ^^^^
```


```lua
repeat until while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | repeat until while
   |              ^^^^^
```


```lua
return xyz )
-- Line 1: <eof> expected near ')' (program)
```

```txt
Unexpected ). Expected a statement.
   |
 1 | return xyz )
   |            ^
```


```lua
return xyz while
-- Line 1: <eof> expected near 'while' (program)
```

```txt
Unexpected while. Expected a statement.
   |
 1 | return xyz while
   |            ^^^^^
```


```lua
return while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected a statement.
   |
 1 | return while
   |        ^^^^^
```


```lua {repl_exprs}
- while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | - while
   |   ^^^^^
```


```lua
true
-- Line 1: unexpected symbol near 'true' (program)
```

```txt
Unexpected true. Expected a statement.
   |
 1 | true
   | ^^^^
```


```lua
until
-- Line 1: <eof> expected near 'until' (program)
```

```txt
Unexpected until. Expected a statement.
   |
 1 | until
   | ^^^^^
```


```lua {repl_exprs}
while
-- Line 1: unexpected symbol near 'while' (repl_exprs)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | while
   | ^^^^^
```


```lua
while xyz do until
-- Line 1: 'end' expected near 'until' (program)
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | while xyz do until
   | ^^^^^ Block started here.
   |
 1 | while xyz do until
   |              ^^^^^ Expected end of block here.
```


```lua
while xyz while
-- Line 1: 'do' expected near 'while' (program)
```

```txt
Unexpected while.
   |
 1 | while xyz while
   |           ^^^^^
```


```lua
while while
-- Line 1: unexpected symbol near 'while' (program)
```

```txt
Unexpected while. Expected an expression.
   |
 1 | while while
   |       ^^^^^
```
