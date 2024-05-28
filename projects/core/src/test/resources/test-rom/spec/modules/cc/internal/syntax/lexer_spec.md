<!--
SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers

SPDX-License-Identifier: MPL-2.0
-->

We provide a lexer for Lua source code. Here we test that the lexer returns the
correct tokens and positions, and that it can report sensible error messages.

# Comments

## Single-line comments
We can lex some basic comments:

```lua
-- A basic singleline comment comment
--[ Not a multiline comment
--[= Also not a multiline comment!
```

```txt
1:1-1:37 COMMENT -- A basic singleline comment comment
2:1-2:27 COMMENT --[ Not a multiline comment
3:1-3:34 COMMENT --[= Also not a multiline comment!
```

It's also useful to test empty comments (including no trailing newline) separately:

```lua
--
```

```txt
1:1-1:2 COMMENT --
```

## Multi-line comments
Multiline/long-string-style comments are also supported:

```lua
--[[
  A
  multiline
  comment
]]

--[=[  ]==] ]] ]=]

--[[ ]=]]
```

```txt
1:1-5:2 COMMENT --[[<NL>  A<NL>  multiline<NL>  comment<NL>]]
7:1-7:18 COMMENT --[=[  ]==] ]] ]=]
9:1-9:9 COMMENT --[[ ]=]]
```

We also fail on unfinished comments:

```lua
--[=[
```

```txt
This comment was never finished.
   |
 1 | --[=[
   | ^^^^^ Comment was started here.
We expected a closing delimiter (]=]) somewhere after this comment was started.
1:1-1:5 ERROR --[=[
```

Nested comments are rejected, just as Lua 5.1 does:

```lua
--[[ [[ ]]
```

```txt
[[ cannot be nested inside another [[ ... ]]
   |
 1 | --[[ [[ ]]
   |      ^^
1:1-1:10 COMMENT --[[ [[ ]]
```

# Strings

We can lex basic strings:

```lua
return "abc", "abc\"", 'abc', 'abc\z

', "abc\
continued"
```

```txt
1:1-1:6 RETURN return
1:8-1:12 STRING "abc"
1:13-1:13 COMMA ,
1:15-1:21 STRING "abc\""
1:22-1:22 COMMA ,
1:24-1:28 STRING 'abc'
1:29-1:29 COMMA ,
1:31-3:1 STRING 'abc\z<NL><NL>'
3:2-3:2 COMMA ,
3:4-4:10 STRING "abc\<NL>continued"
```

We also can lex unterminated strings, including those where there's no closing
quote:

```lua
return "abc
```

```txt
1:1-1:6 RETURN return
This string is not finished. Are you missing a closing quote (")?
   |
 1 | return "abc
   |        ^ String started here.
   |
 1 | return "abc
   |            ^ Expected a closing quote here.
1:8-1:11 STRING "abc
```

And those where the zap is malformed:

```lua
return "abc\z

```

```txt
1:1-1:6 RETURN return
This string is not finished. Are you missing a closing quote (")?
   |
 1 | return "abc\z
   |        ^ String started here.
   |
 1 | return "abc\z
   |              ^ Expected a closing quote here.
1:8-1:14 STRING "abc\z<NL>
```

Finally, strings where the escape is entirely missing:

```lua
return "abc\
```

```txt
1:1-1:6 RETURN return
This string is not finished.
   |
 1 | return "abc\
   |        ^ String started here.
   |
 1 | return "abc\
   |            ^ An escape sequence was started here, but with nothing following it.
1:8-1:12 STRING "abc\
```

## Multi-line/long strings
We can also handle long strings fine

```lua
return [[a b c]], [=[a b c ]=]
```

```txt
1:1-1:6 RETURN return
1:8-1:16 STRING [[a b c]]
1:17-1:17 COMMA ,
1:19-1:30 STRING [=[a b c ]=]
```

Unfinished long strings are correctly reported:

```lua
return [[
```

```txt
1:1-1:6 RETURN return
This string was never finished.
   |
 1 | return [[
   |        ^^ String was started here.
We expected a closing delimiter (]]) somewhere after this string was started.
1:8-1:9 ERROR [[
```

We also handle malformed opening strings:

```lua
return [=
```

```txt
1:1-1:6 RETURN return
Incorrect start of a long string.
   |
 1 | return [=
   |        ^^^
Tip: If you wanted to start a long string here, add an extra [ here.
1:8-1:10 ERROR [=
```

# Numbers

```lua
return 0, 0.0, 0e1, .23, 0x23, 23e-2, 23e+2
```

```txt
1:1-1:6 RETURN return
1:8-1:8 NUMBER 0
1:9-1:9 COMMA ,
1:11-1:13 NUMBER 0.0
1:14-1:14 COMMA ,
1:16-1:18 NUMBER 0e1
1:19-1:19 COMMA ,
1:21-1:23 NUMBER .23
1:24-1:24 COMMA ,
1:26-1:29 NUMBER 0x23
1:30-1:30 COMMA ,
1:32-1:36 NUMBER 23e-2
1:37-1:37 COMMA ,
1:39-1:43 NUMBER 23e+2
```

We also handle malformed numbers:

```lua
return 2..3, 2eee2
```

```txt
1:1-1:6 RETURN return
This isn't a valid number.
   |
 1 | return 2..3, 2eee2
   |        ^^^^
Numbers must be in one of the following formats: 123, 3.14, 23e35, 0x01AF.
1:8-1:11 NUMBER 2..3
1:12-1:12 COMMA ,
This isn't a valid number.
   |
 1 | return 2..3, 2eee2
   |              ^^^^^
Numbers must be in one of the following formats: 123, 3.14, 23e35, 0x01AF.
1:14-1:18 NUMBER 2eee2
```

# Unknown tokens

We can suggest alternatives for possible errors:

```lua
if a != b then end
if a ~= b then end
if a && b then end
if a || b then end
if ! a then end
```

```txt
1:1-1:2 IF if
1:4-1:4 IDENT a
Unexpected character.
   |
 1 | if a != b then end
   |      ^^
Tip: Replace this with ~= to check if two values are not equal.
1:6-1:7 NE !=
1:9-1:9 IDENT b
1:11-1:14 THEN then
1:16-1:18 END end
2:1-2:2 IF if
2:4-2:4 IDENT a
2:6-2:7 NE ~=
2:9-2:9 IDENT b
2:11-2:14 THEN then
2:16-2:18 END end
3:1-3:2 IF if
3:4-3:4 IDENT a
Unexpected character.
   |
 3 | if a && b then end
   |      ^^
Tip: Replace this with and to check if both values are true.
3:6-3:7 AND &&
3:9-3:9 IDENT b
3:11-3:14 THEN then
3:16-3:18 END end
4:1-4:2 IF if
4:4-4:4 IDENT a
Unexpected character.
   |
 4 | if a || b then end
   |      ^^
Tip: Replace this with or to check if either value is true.
4:6-4:7 OR ||
4:9-4:9 IDENT b
4:11-4:14 THEN then
4:16-4:18 END end
5:1-5:2 IF if
Unexpected character.
   |
 5 | if ! a then end
   |    ^
Tip: Replace this with not to negate a boolean.
5:4-5:4 NOT !
5:6-5:6 IDENT a
5:8-5:11 THEN then
5:13-5:15 END end
```

For entirely unknown glyphs we should just give up and return an `ERROR` token.

```lua
return $*&(*)xyz
```

```txt
1:1-1:6 RETURN return
Unexpected character.
   |
 1 | return $*&(*)xyz
   |        ^ This character isn't usable in Lua code.
1:8-1:10 ERROR $*&
```
