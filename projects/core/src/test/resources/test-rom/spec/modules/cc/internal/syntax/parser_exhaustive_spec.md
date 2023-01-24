An exhaustive list of all error states in the parser, and the error messages we
generate for each one. This is _not_ a complete collection of all possible
errors, but is a useful guide for where we might be providing terrible messages.

```lua{repl_exprs}
break until
```

```txt
Unexpected until.
   |
 1 | break until
   |       ^^^^^
```


```lua
break while
```

```txt
Expected a statement near while.
   |
 1 | break while
   |       ^^^^^
```


```lua
do end true
```

```txt
Expected a statement near true.
   |
 1 | do end true
   |        ^^^^
```


```lua
do until
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


```lua{repl_exprs}
... true
```

```txt
Unexpected true.
   |
 1 | ... true
   |     ^^^^
```


```lua
for xyz , xyz while
```

```txt
Unexpected while.
   |
 1 | for xyz , xyz while
   |               ^^^^^
```


```lua
for xyz , while
```

```txt
Unexpected while.
   |
 1 | for xyz , while
   |           ^^^^^
```


```lua
for xyz = xyz , xyz , xyz while
```

```txt
Unexpected while.
   |
 1 | for xyz = xyz , xyz , xyz while
   |                           ^^^^^
```


```lua
for xyz = xyz , xyz , while
```

```txt
Expected an expression near while.
   |
 1 | for xyz = xyz , xyz , while
   |                       ^^^^^
```


```lua
for xyz = xyz , xyz do until
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
```

```txt
Unexpected while.
   |
 1 | for xyz = xyz , xyz while
   |                     ^^^^^
```


```lua
for xyz = xyz , while
```

```txt
Expected an expression near while.
   |
 1 | for xyz = xyz , while
   |                 ^^^^^
```


```lua
for xyz = xyz while
```

```txt
Unexpected while.
   |
 1 | for xyz = xyz while
   |               ^^^^^
```


```lua
for xyz = while
```

```txt
Expected an expression near while.
   |
 1 | for xyz = while
   |           ^^^^^
```


```lua
for xyz in xyz do until
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
```

```txt
Unexpected while.
   |
 1 | for xyz in xyz while
   |                ^^^^^
```


```lua
for xyz in while
```

```txt
Unexpected while.
   |
 1 | for xyz in while
   |            ^^^^^
```


```lua
for xyz while
```

```txt
Unexpected while.
   |
 1 | for xyz while
   |         ^^^^^
```


```lua
for while
```

```txt
Unexpected while.
   |
 1 | for while
   |     ^^^^^
```


```lua
function xyz : while
```

```txt
Unexpected while.
   |
 1 | function xyz : while
   |                ^^^^^
```


```lua
function xyz . while
```

```txt
Unexpected while.
   |
 1 | function xyz . while
   |                ^^^^^
```


```lua
function xyz ( ) until
```

```txt
Expected a statement near until.
   |
 1 | function xyz ( ) until
   |                  ^^^^^
```


```lua
function xyz while
```

```txt
Unexpected while.
   |
 1 | function xyz while
   |              ^^^^^
```


```lua{repl_exprs}
function ( ) until
```

```txt
Expected a statement near until.
   |
 1 | function ( ) until
   |              ^^^^^
```


```lua{repl_exprs}
function ( xyz , while
```

```txt
Unexpected while.
   |
 1 | function ( xyz , while
   |                  ^^^^^
```


```lua{repl_exprs}
function ( xyz while
```

```txt
Brackets were not closed.
   |
 1 | function ( xyz while
   |          ^ Brackets were opened here.
   |
 1 | function ( xyz while
   |                ^ Expected to be closed before here.
```


```lua{repl_exprs}
function ( while
```

```txt
Brackets were not closed.
   |
 1 | function ( while
   |          ^ Brackets were opened here.
   |
 1 | function ( while
   |            ^ Expected to be closed before here.
```


```lua
function while
```

```txt
Unexpected while.
   |
 1 | function while
   |          ^^^^^
```


```lua{repl_exprs}
function while
```

```txt
Unexpected while.
   |
 1 | function while
   |          ^^^^^
```


```lua{repl_exprs}
xyz + while
```

```txt
Expected an expression near while.
   |
 1 | xyz + while
   |       ^^^^^
```


```lua{repl_exprs}
xyz and while
```

```txt
Expected an expression near while.
   |
 1 | xyz and while
   |         ^^^^^
```


```lua{repl_exprs}
xyz : xyz while
```

```txt
Unexpected while.
   |
 1 | xyz : xyz while
   |           ^^^^^
```


```lua{repl_exprs}
xyz : while
```

```txt
Unexpected while.
   |
 1 | xyz : while
   |       ^^^^^
```


```lua{repl_exprs}
xyz , xyz then
```

```txt
Unexpected then.
   |
 1 | xyz , xyz then
   |           ^^^^
```


```lua
xyz , xyz while
```

```txt
Unexpected symbol after name.
   |
 1 | xyz , xyz while
   |           ^
Did you mean to assign this or call it as a function?
```


```lua
xyz , while
```

```txt
Unexpected while.
   |
 1 | xyz , while
   |       ^^^^^
```


```lua{repl_exprs}
xyz , while
```

```txt
Expected an expression near while.
   |
 1 | xyz , while
   |       ^^^^^
```


```lua{repl_exprs}
xyz .. while
```

```txt
Expected an expression near while.
   |
 1 | xyz .. while
   |        ^^^^^
```


```lua{repl_exprs}
xyz / while
```

```txt
Expected an expression near while.
   |
 1 | xyz / while
   |       ^^^^^
```


```lua{repl_exprs}
xyz . while
```

```txt
Unexpected while.
   |
 1 | xyz . while
   |       ^^^^^
```


```lua{repl_exprs}
xyz == while
```

```txt
Expected an expression near while.
   |
 1 | xyz == while
   |        ^^^^^
```


```lua
xyz = xyz )
```

```txt
Expected a statement near ).
   |
 1 | xyz = xyz )
   |           ^
```


```lua
xyz = while
```

```txt
Unexpected while.
   |
 1 | xyz = while
   |       ^^^^^
```


```lua{repl_exprs}
xyz >= while
```

```txt
Expected an expression near while.
   |
 1 | xyz >= while
   |        ^^^^^
```


```lua{repl_exprs}
xyz > while
```

```txt
Expected an expression near while.
   |
 1 | xyz > while
   |       ^^^^^
```


```lua{repl_exprs}
xyz <= while
```

```txt
Expected an expression near while.
   |
 1 | xyz <= while
   |        ^^^^^
```


```lua{repl_exprs}
xyz < while
```

```txt
Expected an expression near while.
   |
 1 | xyz < while
   |       ^^^^^
```


```lua{repl_exprs}
xyz % while
```

```txt
Expected an expression near while.
   |
 1 | xyz % while
   |       ^^^^^
```


```lua{repl_exprs}
xyz * while
```

```txt
Expected an expression near while.
   |
 1 | xyz * while
   |       ^^^^^
```


```lua{repl_exprs}
xyz ~= while
```

```txt
Expected an expression near while.
   |
 1 | xyz ~= while
   |        ^^^^^
```


```lua{repl_exprs}
xyz ( xyz until
```

```txt
Brackets were not closed.
   |
 1 | xyz ( xyz until
   |     ^ Brackets were opened here.
   |
 1 | xyz ( xyz until
   |           ^ Expected to be closed before here.
```


```lua{repl_exprs}
xyz ( while
```

```txt
Brackets were not closed.
   |
 1 | xyz ( while
   |     ^ Brackets were opened here.
   |
 1 | xyz ( while
   |       ^ Expected to be closed before here.
```


```lua{repl_exprs}
xyz or while
```

```txt
Expected an expression near while.
   |
 1 | xyz or while
   |        ^^^^^
```


```lua{repl_exprs}
xyz [ xyz while
```

```txt
Brackets were not closed.
   |
 1 | xyz [ xyz while
   |     ^ Brackets were opened here.
   |
 1 | xyz [ xyz while
   |           ^ Expected to be closed before here.
```


```lua{repl_exprs}
xyz [ while
```

```txt
Expected an expression near while.
   |
 1 | xyz [ while
   |       ^^^^^
```


```lua{repl_exprs}
xyz ^ while
```

```txt
Expected an expression near while.
   |
 1 | xyz ^ while
   |       ^^^^^
```


```lua
xyz 'abc' true
```

```txt
Expected a statement near true.
   |
 1 | xyz 'abc' true
   |           ^^^^
```


```lua{repl_exprs}
xyz - while
```

```txt
Expected an expression near while.
   |
 1 | xyz - while
   |       ^^^^^
```


```lua{repl_exprs}
xyz then
```

```txt
Unexpected then.
   |
 1 | xyz then
   |     ^^^^
```


```lua{repl_exprs}
xyz true
```

```txt
Unexpected true.
   |
 1 | xyz true
   |     ^^^^
```


```lua
xyz while
```

```txt
Unexpected symbol after name.
   |
 1 | xyz while
   |     ^
Did you mean to assign this or call it as a function?
```


```lua{repl_exprs}
xyz while
```

```txt
Unexpected while.
   |
 1 | xyz while
   |     ^^^^^
```


```lua
if xyz then else until
```

```txt
Unexpected until. Expected end or another statement.
   |
 1 | if xyz then else until
   | ^^ Block started here.
   |
 1 | if xyz then else until
   |                  ^^^^^ Expected end of block here.
```


```lua
if xyz then elseif xyz while
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
```

```txt
Expected an expression near while.
   |
 1 | if xyz then elseif while
   |                    ^^^^^
```


```lua
if xyz then until
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
```

```txt
Expected an expression near while.
   |
 1 | if while
   |    ^^^^^
```


```lua{repl_exprs}
# while
```

```txt
Expected an expression near while.
   |
 1 | # while
   |   ^^^^^
```


```lua
local function xyz ( ) until
```

```txt
Expected a statement near until.
   |
 1 | local function xyz ( ) until
   |                        ^^^^^
```


```lua
local function xyz while
```

```txt
Unexpected while.
   |
 1 | local function xyz while
   |                    ^^^^^
```


```lua
local function while
```

```txt
Unexpected while.
   |
 1 | local function while
   |                ^^^^^
```


```lua
local xyz = xyz )
```

```txt
Expected a statement near ).
   |
 1 | local xyz = xyz )
   |                 ^
```


```lua
local xyz = while
```

```txt
Unexpected while.
   |
 1 | local xyz = while
   |             ^^^^^
```


```lua
local xyz true
```

```txt
Expected a statement near true.
   |
 1 | local xyz true
   |           ^^^^
```


```lua
local while
```

```txt
Unexpected while.
   |
 1 | local while
   |       ^^^^^
```


```lua{repl_exprs}
not while
```

```txt
Expected an expression near while.
   |
 1 | not while
   |     ^^^^^
```


```lua{repl_exprs}
{ xyz , while
```

```txt
Brackets were not closed.
   |
 1 | { xyz , while
   | ^ Brackets were opened here.
   |
 1 | { xyz , while
   |         ^ Expected to be closed before here.
```


```lua{repl_exprs}
{ xyz = xyz while
```

```txt
Brackets were not closed.
   |
 1 | { xyz = xyz while
   | ^ Brackets were opened here.
   |
 1 | { xyz = xyz while
   |             ^ Expected to be closed before here.
```


```lua{repl_exprs}
{ xyz = while
```

```txt
Expected an expression near while.
   |
 1 | { xyz = while
   |         ^^^^^
```


```lua{repl_exprs}
{ xyz while
```

```txt
Brackets were not closed.
   |
 1 | { xyz while
   | ^ Brackets were opened here.
   |
 1 | { xyz while
   |       ^ Expected to be closed before here.
```


```lua{repl_exprs}
{ [ xyz ] = xyz while
```

```txt
Brackets were not closed.
   |
 1 | { [ xyz ] = xyz while
   | ^ Brackets were opened here.
   |
 1 | { [ xyz ] = xyz while
   |                 ^ Expected to be closed before here.
```


```lua{repl_exprs}
{ [ xyz ] = while
```

```txt
Expected an expression near while.
   |
 1 | { [ xyz ] = while
   |             ^^^^^
```


```lua{repl_exprs}
{ [ xyz ] while
```

```txt
Unexpected while.
   |
 1 | { [ xyz ] while
   |           ^^^^^
```


```lua{repl_exprs}
{ [ xyz while
```

```txt
Brackets were not closed.
   |
 1 | { [ xyz while
   |   ^ Brackets were opened here.
   |
 1 | { [ xyz while
   |         ^ Expected to be closed before here.
```


```lua{repl_exprs}
{ [ while
```

```txt
Expected an expression near while.
   |
 1 | { [ while
   |     ^^^^^
```


```lua{repl_exprs}
{ while
```

```txt
Brackets were not closed.
   |
 1 | { while
   | ^ Brackets were opened here.
   |
 1 | { while
   |   ^ Expected to be closed before here.
```


```lua
( xyz ) while
```

```txt
Unexpected while.
   |
 1 | ( xyz ) while
   |         ^^^^^
```


```lua{repl_exprs}
( xyz while
```

```txt
Brackets were not closed.
   |
 1 | ( xyz while
   | ^ Brackets were opened here.
   |
 1 | ( xyz while
   |       ^ Expected to be closed before here.
```


```lua{repl_exprs}
( while
```

```txt
Expected an expression near while.
   |
 1 | ( while
   |   ^^^^^
```


```lua
repeat end
```

```txt
Expected a statement near end.
   |
 1 | repeat end
   |        ^^^
```


```lua
repeat until xyz then
```

```txt
Expected a statement near then.
   |
 1 | repeat until xyz then
   |                  ^^^^
```


```lua
repeat until while
```

```txt
Expected an expression near while.
   |
 1 | repeat until while
   |              ^^^^^
```


```lua
return xyz )
```

```txt
Expected a statement near ).
   |
 1 | return xyz )
   |            ^
```


```lua
return xyz while
```

```txt
Expected a statement near while.
   |
 1 | return xyz while
   |            ^^^^^
```


```lua
return while
```

```txt
Expected a statement near while.
   |
 1 | return while
   |        ^^^^^
```


```lua{repl_exprs}
- while
```

```txt
Expected an expression near while.
   |
 1 | - while
   |   ^^^^^
```


```lua
true
```

```txt
Expected a statement near true.
   |
 1 | true
   | ^^^^
```


```lua
until
```

```txt
Expected a statement near until.
   |
 1 | until
   | ^^^^^
```


```lua{repl_exprs}
until
```

```txt
Expected a statement near until.
   |
 1 | until
   | ^^^^^
```


```lua
while xyz do until
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
```

```txt
Unexpected while.
   |
 1 | while xyz while
   |           ^^^^^
```


```lua
while while
```

```txt
Expected an expression near while.
   |
 1 | while while
   |       ^^^^^
```
