--[[- A parser for Lua programs and expressions.

:::warning
This is an internal module and SHOULD NOT be used in your own code. It may
be removed or changed at any time.
:::

Most of the code in this module is automatically generated from the Lua grammar,
hence being mostly unreadable!

## The parser
The main parser is defined by a state machine (the `transitions` and
`productions` tables) and the function `parse`, which runs the state machine.
It's a fairly standard LR(1) parser - we maintain a stack of current states (and
their positions), then push and pop values on that stack.

Most LR(1) parsers will encode the transition table in a compact binary format,
optimised for space and fast lookups. However, without access to built-in
bitwise operations, this is harder to justify in Lua. Instead, the transition
table is a 2D lookup table of `action = transitions[state][value]`, where
`action` can be one of the following:

 - `action = false`: This transition is undefined, and thus a parse error. We
   use this (rather than nil) to ensure our tables are dense, and thus stored as
   arrays rather than maps.

 - `action > 0`: Shift this terminal or non-terminal onto the stack, then
   transition to `state = action`.

 - `action < 0`: Apply production `productions[-action]`. This production is a
   tuple composed of the next state and the number of values to pop from the
   stack.

## Error reporting
Error messages are defined as a series of regular expressions on the current
parser stack. These are then compiled to a DFA, resulting in the state machine
encoded in `error_transitions` and `error_states`.

Unlike the parser, the error handling code does not need to be fast, and so we
use sparse tables to encode our transitions.

@local
]]

-- Lazily load our map of errors
local errors = setmetatable({}, {
    __index = function(self, key)
        setmetatable(self, nil)
        for k, v in pairs(require "cc.internal.syntax.errors") do self[k] = v end

        return self[key]
    end,
})

-- Everything below this line is auto-generated. DO NOT EDIT.

--- A lookup table of valid Lua tokens
local tokens = (function() return {} end)() -- Make tokens opaque to illuaminate. Nasty!
for i, token in ipairs({
    "WHILE", "UNTIL", "TRUE", "THEN", "SUB", "STRING", "SEMICOLON", "RETURN",
    "REPEAT", "POW", "OSQUARE", "OR", "OPAREN", "OBRACE", "NUMBER", "NOT",
    "NIL", "NE", "MUL", "MOD", "LT", "LOCAL", "LEN", "LE", "IN", "IF",
    "IDENT", "GT", "GE", "FUNCTION", "FOR", "FALSE", "EQUALS", "EQ", "EOF",
    "END", "ELSEIF", "ELSE", "DOTS", "DOT", "DO", "DIV", "CSQUARE", "CPAREN",
    "CONCAT", "COMMA", "COLON", "CBRACE", "BREAK", "AND", "ADD", "COMMENT",
    "ERROR",
}) do tokens[token] = i end
setmetatable(tokens, { __index = function(_, name) error("No such token " .. tostring(name), 2) end })

-- Error handling code. This is composed of a list of error handling functions (error_messages), and a DFA which matches
-- the current LR(1) stack (error_transitions, error_states).
local function line_end_position(context, previous, token)
    local prev_line = context.get_pos(previous)
    local tok_line = context.get_pos(token.s)
    if tok_line == prev_line then
        return token.s
    else
        return previous + 1
    end
end

local error_messages = {
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 17
        if token.v == tokens.EQUALS then
        return errors.table_key_equals(token.s, token.e)
    end
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 25
        if token.v == tokens.EQUALS then
        return errors.use_double_equals(token.s, token.e)
    end
    end,
    function(context, stack, stack_n, regs, token)
        local lp = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 33
        return (errors.unclosed_brackets(lp.s, lp.e, token.s))
    end,
    function(context, stack, stack_n, regs, token)
        local lp = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 35
        return (errors.unclosed_brackets(lp.s, lp.e, token.s))
    end,
    function(context, stack, stack_n, regs, token)
        local lp = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 37
        return (errors.unclosed_brackets(lp.s, lp.e, token.s))
    end,
    function(context, stack, stack_n, regs, token)
        local loc = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 42
        if token.v == tokens.DOT then
        return errors.local_function_dot(loc.s, loc.e, token.s, token.e)
    end
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 50
        local end_pos = stack[stack_n + 2] -- Hack to get the last position
    local end_line = context.get_pos(end_pos)
    local tok_line = context.get_pos(token.s)
    if token ~= tokens.EOF and tok_line == end_line then
      return errors.standalone_name(token.s)
    else
      return errors.standalone_name_call(end_pos)
    end
    end,
    function(context, stack, stack_n, regs, token)
        local start = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 69
        return errors.expected_then(start.s, start.e, line_end_position(context, stack[stack_n + 2], token))
    end,
    function(context, stack, stack_n, regs, token)
        local start = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 71
        return errors.expected_then(start.s, start.e, line_end_position(context, stack[stack_n + 2], token))
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 75
        if token ~= tokens.EOF then
        return errors.expected_statement(token.v, token.s, token.e)
    end
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 83
        return errors.expected_expression(token.v, token.s, token.e)
    end,
}
local error_transitions = {
    { [1] = 47, [4] = 47, [5] = 48, [6] = 27, [7] = 48, [8] = 27, [9] = 48, [10] = 45, [11] = 48, [12] = 27, [13] = 48, [14] = 27, [15] = 48, [16] = 40, [18] = 44, [19] = 2, [20] = 2, [21] = 3, [23] = 4, [24] = 49, [25] = 5, [26] = 3, [27] = 47, [29] = 28, [30] = 27, [31] = 27, [32] = 54, [33] = 27, [34] = 29, [35] = 30, [36] = 48, [37] = 29, [38] = 31, [39] = 48, [40] = 32, [41] = 29, [42] = 33, [43] = 68, [44] = 48, [45] = 61, [46] = 48, [47] = 62, [48] = 48, [49] = 62, [50] = 48, [51] = 62, [52] = 48, [53] = 63, [54] = 48, [55] = 64, [56] = 48, [57] = 61, [58] = 48, [59] = 61, [60] = 48, [61] = 64, [62] = 48, [63] = 64, [64] = 48, [65] = 64, [66] = 48, [67] = 64, [68] = 48, [69] = 64, [70] = 48, [71] = 65, [72] = 41, [73] = 44, [74] = 26, [75] = 48, [76] = 59, [77] = 49, [78] = 34, [79] = 60, [81] = 42, [84] = 30, [85] = 35, [86] = 36, [87] = 37, [88] = 37, [89] = 68, [91] = 48, [92] = 56, [93] = 43, [94] = 48, [95] = 57, [96] = 6, [97] = 7, [98] = 7, [99] = 8, [100] = 9, [101] = 66, [102] = 50, [103] = 38, [104] = 67, [105] = 39, [106] = 37, [107] = 55, [108] = 47, [110] = 10, [111] = 11, [112] = 12, [113] = 13, [114] = 47, [116] = 48, [117] = 58, [120] = 51, [121] = 47, [123] = 14, [124] = 52, [125] = 15, [127] = 16, [129] = 53, [130] = 17, [131] = 48, [132] = 55, [133] = 47, [136] = 51, [142] = 47, [144] = 10, [146] = 51, [147] = 48, [148] = 55, [149] = 48, [150] = 55, [151] = 48, [152] = 55, [154] = 47, [156] = 18, [160] = 47, [162] = 19, [163] = 47, [165] = 17, [166] = 11, [167] = 12, [168] = 20, [169] = 21, [171] = 46, [173] = 17, [175] = 22, [176] = 23, [177] = 47, [180] = 48, [181] = 55, [182] = 47, [184] = 47, [187] = 24, [189] = 25, [191] = 47 },
    { [18] = 44, [22] = 69 }, {}, {}, {}, { [10] = 45, [99] = 8 }, {}, {},
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    { [73] = 44, [111] = 11 },
    { [5] = 75, [7] = 27, [9] = 89, [10] = 92, [11] = 90, [13] = 27, [15] = 27, [36] = 90, [39] = 37, [44] = 83, [46] = 84, [48] = 84, [50] = 84, [52] = 85, [54] = 86, [56] = 83, [58] = 83, [60] = 86, [62] = 86, [64] = 86, [66] = 86, [68] = 86, [70] = 87, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [180] = 91, [191] = 75 },
    {},
    { [5] = 75, [7] = 29, [9] = 89, [10] = 92, [11] = 90, [13] = 29, [15] = 29, [36] = 90, [39] = 37, [44] = 83, [46] = 84, [48] = 84, [50] = 84, [52] = 85, [54] = 86, [56] = 83, [58] = 83, [60] = 86, [62] = 86, [64] = 86, [66] = 86, [68] = 86, [70] = 87, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [180] = 91, [191] = 75 },
    { [34] = 95, [83] = 94, [170] = 95 },
    { [5] = 75, [9] = 89, [10] = 92, [11] = 90, [36] = 90, [44] = 83, [46] = 84, [48] = 84, [50] = 84, [52] = 85, [54] = 86, [56] = 83, [58] = 83, [60] = 86, [62] = 86, [64] = 86, [66] = 86, [68] = 86, [70] = 87, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [180] = 91, [191] = 75 },
    {},
    { [5] = 75, [7] = 33, [9] = 89, [10] = 92, [11] = 90, [13] = 33, [15] = 33, [36] = 90, [39] = 37, [44] = 83, [46] = 84, [48] = 84, [50] = 84, [52] = 85, [54] = 86, [56] = 83, [58] = 83, [60] = 86, [62] = 86, [64] = 86, [66] = 86, [68] = 86, [70] = 87, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [180] = 91, [191] = 75 },
    {}, {}, {}, {}, {}, {},
    { [4] = 46, [5] = 75, [7] = 29, [9] = 89, [11] = 90, [13] = 29, [15] = 29, [18] = 44, [22] = 69, [36] = 90, [39] = 37, [44] = 83, [46] = 84, [48] = 84, [50] = 84, [52] = 85, [54] = 86, [56] = 83, [58] = 83, [60] = 86, [62] = 86, [64] = 86, [66] = 86, [68] = 86, [70] = 87, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [111] = 78, [116] = 79, [118] = 25, [126] = 80, [128] = 99, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [174] = 74, [180] = 91, [191] = 75 },
    {}, {}, { [10] = 92, [99] = 88 }, {}, {}, {}, {}, {}, {}, {},
    { [17] = 101, [119] = 101, [135] = 101 }, {}, {},
    { [4] = 46, [5] = 75, [7] = 29, [9] = 89, [10] = 92, [11] = 90, [13] = 29, [15] = 29, [36] = 90, [39] = 37, [44] = 83, [46] = 84, [48] = 84, [50] = 84, [52] = 85, [54] = 86, [56] = 83, [58] = 83, [60] = 86, [62] = 86, [64] = 86, [66] = 86, [68] = 86, [70] = 87, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [174] = 74, [180] = 91, [191] = 75 },
    { [131] = 102, [180] = 103 }, {}, {}, {}, {},
    { [73] = 44, [111] = 11, [126] = 71, [172] = 15 }, {}, {}, {}, {}, {},
    { [10] = 45, [99] = 8 }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    { [4] = 73, [5] = 75, [7] = 29, [9] = 89, [10] = 92, [11] = 90, [13] = 29, [15] = 29, [36] = 90, [39] = 37, [44] = 83, [46] = 84, [48] = 84, [50] = 84, [52] = 85, [54] = 86, [56] = 83, [58] = 83, [60] = 86, [62] = 86, [64] = 86, [66] = 86, [68] = 86, [70] = 87, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [180] = 91, [191] = 75 },
    {}, {}, {}, {}, {}, { [118] = 122 }, {}, {}, {}, {}, {}, {}, {}, {}, {},
    {}, {}, {}, { [73] = 44, [111] = 11, [126] = 71, [172] = 15 },
    { [5] = 75, [9] = 89, [10] = 92, [11] = 90, [36] = 90, [52] = 85, [54] = 86, [56] = 83, [60] = 86, [62] = 86, [64] = 86, [66] = 86, [68] = 86, [70] = 87, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [180] = 91, [191] = 75 },
    { [5] = 75, [9] = 89, [10] = 92, [11] = 90, [36] = 90, [44] = 83, [52] = 85, [54] = 86, [56] = 83, [58] = 83, [60] = 86, [62] = 86, [64] = 86, [66] = 86, [68] = 86, [70] = 87, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [180] = 91, [191] = 75 },
    { [5] = 75, [9] = 89, [10] = 92, [11] = 90, [36] = 90, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [180] = 91, [191] = 75 },
    { [5] = 75, [9] = 89, [10] = 92, [11] = 90, [36] = 90, [52] = 85, [70] = 87, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [180] = 91, [191] = 75 },
    { [5] = 75, [9] = 89, [10] = 92, [11] = 90, [36] = 90, [52] = 85, [73] = 89, [75] = 82, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [172] = 81, [180] = 91, [191] = 75 },
    {},
    { [5] = 75, [7] = 27, [9] = 89, [10] = 92, [11] = 90, [13] = 27, [15] = 27, [34] = 95, [36] = 90, [39] = 37, [44] = 83, [46] = 84, [48] = 84, [50] = 84, [52] = 85, [54] = 86, [56] = 83, [58] = 83, [60] = 86, [62] = 86, [64] = 86, [66] = 86, [68] = 86, [70] = 87, [73] = 89, [75] = 82, [83] = 94, [91] = 76, [94] = 77, [99] = 88, [111] = 78, [116] = 79, [126] = 80, [131] = 75, [147] = 75, [149] = 75, [151] = 75, [158] = 75, [170] = 95, [172] = 81, [180] = 91, [191] = 75 },
    {},
}
local error_states
local function error_jump(state)
    return function(stack, idx, regs, out)
        return error_states[state](stack, idx - 3, regs, out)
    end
end
error_states = {
    function(stack, idx, regs, out) -- State 1
        local top = stack[idx]
        if top == 10 or top == 18 or top == 73 then regs[1] = idx end
        local next = error_transitions[1][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 2
        local top = stack[idx]
        if top == 18 then regs[1] = idx end
        local next = error_transitions[2][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 3
        regs[1] = idx
        return error_states[44](stack, idx - 3, regs, out)
    end,
    error_jump(69), -- State 4
    error_jump(70), -- State 5
    function(stack, idx, regs, out) -- State 6
        local top = stack[idx]
        if top == 10 then regs[1] = idx end
        local next = error_transitions[6][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(6), -- State 7
    error_jump(6), -- State 8
    error_jump(8), -- State 9
    error_jump(24), -- State 10
    error_jump(47), -- State 11
    error_jump(11), -- State 12
    error_jump(12), -- State 13
    error_jump(10), -- State 14
    error_jump(25), -- State 15
    error_jump(71), -- State 16
    error_jump(15), -- State 17
    error_jump(72), -- State 18
    error_jump(14), -- State 19
    error_jump(73), -- State 20
    error_jump(20), -- State 21
    error_jump(74), -- State 22
    error_jump(46), -- State 23
    error_jump(17), -- State 24
    error_jump(73), -- State 25
    function(stack, idx, regs, out) -- State 26
        local top = stack[idx]
        if top == 73 then regs[1] = idx end
        local next = error_transitions[26][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 27
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[27][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(93), -- State 28
    function(stack, idx, regs, out) -- State 29
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[29][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 30
        local top = stack[idx]
        local next = error_transitions[30][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 31
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[31][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(37), -- State 32
    function(stack, idx, regs, out) -- State 33
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[33][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(96), -- State 34
    error_jump(94), -- State 35
    error_jump(95), -- State 36
    error_jump(33), -- State 37
    error_jump(97), -- State 38
    error_jump(98), -- State 39
    function(stack, idx, regs, out) -- State 40
        local top = stack[idx]
        if top == 9 or top == 11 or top == 18 or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[40][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(42), -- State 41
    error_jump(100), -- State 42
    function(stack, idx, regs, out) -- State 43
        local top = stack[idx]
        if top == 10 then regs[1] = idx end
        local next = error_transitions[43][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 44
        out[#out + 1] = { 3, regs[1] }
    end,
    function(stack, idx, regs, out) -- State 45
        out[#out + 1] = { 4, regs[1] }
    end,
    function(stack, idx, regs, out) -- State 46
        out[#out + 1] = { 7 }
    end,
    function(stack, idx, regs, out) -- State 47
        out[#out + 1] = { 10 }
    end,
    function(stack, idx, regs, out) -- State 48
        out[#out + 1] = { 11 }
    end,
    function(stack, idx, regs, out) -- State 49
        regs[1] = idx
        return error_states[44](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 50
        regs[1] = idx
        return error_states[45](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 51
        local top = stack[idx]
        local next = error_transitions[51][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(25), -- State 52
    error_jump(99), -- State 53
    function(stack, idx, regs, out) -- State 54
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[54][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 55
        out[#out + 1] = { 2 }
        local top = stack[idx]
        if top == 131 or top == 180 then regs[1] = idx end
        local next = error_transitions[55][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 56
        out[#out + 1] = { 2 }
        return error_states[104](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 57
        out[#out + 1] = { 2 }
        return error_states[8](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 58
        out[#out + 1] = { 2 }
        return error_states[17](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 59
        out[#out + 1] = { 2 }
        return error_states[105](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 60
        out[#out + 1] = { 2 }
        local top = stack[idx]
        if top == 73 then regs[1] = idx end
        local next = error_transitions[60][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 61
        out[#out + 1] = { 2 }
        return error_states[106](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 62
        out[#out + 1] = { 2 }
        return error_states[107](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 63
        out[#out + 1] = { 2 }
        return error_states[108](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 64
        out[#out + 1] = { 2 }
        return error_states[109](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 65
        out[#out + 1] = { 2 }
        return error_states[110](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 66
        out[#out + 1] = { 2 }
        out[#out + 1] = { 1 }
        local top = stack[idx]
        if top == 10 then regs[1] = idx end
        local next = error_transitions[66][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 67
        out[#out + 1] = { 2 }
        regs[1] = idx
        return error_states[44](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 68
        out[#out + 1] = { 2 }
        regs[1] = idx
        return error_states[111](stack, idx - 3, regs, out)
    end,
    error_jump(3), -- State 69
    error_jump(112), -- State 70
    error_jump(15), -- State 71
    error_jump(113), -- State 72
    error_jump(47), -- State 73
    error_jump(23), -- State 74
    function(stack, idx, regs, out) -- State 75
        out[#out + 1] = { 2 }
    end,
    function(stack, idx, regs, out) -- State 76
        out[#out + 1] = { 2 }
        return error_states[9](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 77
        out[#out + 1] = { 2 }
        return error_states[6](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 78
        out[#out + 1] = { 2 }
        return error_states[47](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 79
        out[#out + 1] = { 2 }
        return error_states[15](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 80
        out[#out + 1] = { 2 }
        return error_states[15](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 81
        out[#out + 1] = { 2 }
        return error_states[25](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 82
        out[#out + 1] = { 2 }
        return error_states[114](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 83
        out[#out + 1] = { 2 }
        return error_states[115](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 84
        out[#out + 1] = { 2 }
        return error_states[116](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 85
        out[#out + 1] = { 2 }
        return error_states[117](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 86
        out[#out + 1] = { 2 }
        return error_states[118](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 87
        out[#out + 1] = { 2 }
        return error_states[119](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 88
        out[#out + 1] = { 2 }
        out[#out + 1] = { 1 }
        return error_states[6](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 89
        out[#out + 1] = { 3, regs[1] }
        out[#out + 1] = { 2 }
    end,
    function(stack, idx, regs, out) -- State 90
        out[#out + 1] = { 5, regs[1] }
        out[#out + 1] = { 2 }
    end,
    function(stack, idx, regs, out) -- State 91
        out[#out + 1] = { 9, regs[1] }
        out[#out + 1] = { 2 }
    end,
    function(stack, idx, regs, out) -- State 92
        out[#out + 1] = { 4, regs[1] }
        out[#out + 1] = { 2 }
        out[#out + 1] = { 1 }
    end,
    error_jump(120), -- State 93
    error_jump(36), -- State 94
    function(stack, idx, regs, out) -- State 95
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[95][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(30), -- State 96
    error_jump(121), -- State 97
    error_jump(29), -- State 98
    error_jump(52), -- State 99
    error_jump(54), -- State 100
    function(stack, idx, regs, out) -- State 101
        local top = stack[idx]
        if top == 118 then regs[1] = idx end
        local next = error_transitions[101][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 102
        out[#out + 1] = { 8, regs[1] }
    end,
    function(stack, idx, regs, out) -- State 103
        out[#out + 1] = { 9, regs[1] }
    end,
    error_jump(9), -- State 104
    error_jump(114), -- State 105
    error_jump(115), -- State 106
    error_jump(116), -- State 107
    error_jump(117), -- State 108
    error_jump(118), -- State 109
    error_jump(119), -- State 110
    function(stack, idx, regs, out) -- State 111
        out[#out + 1] = { 5, regs[1] }
    end,
    error_jump(47), -- State 112
    error_jump(19), -- State 113
    function(stack, idx, regs, out) -- State 114
        local top = stack[idx]
        if top == 73 then regs[1] = idx end
        local next = error_transitions[114][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 115
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[115][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 116
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[116][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 117
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[117][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 118
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[118][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 119
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[119][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(27), -- State 120
    function(stack, idx, regs, out) -- State 121
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 or top == 180 then
            regs[1] = idx
        end
        local next = error_transitions[121][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 122
        out[#out + 1] = { 6, regs[1] }
    end,
}

--- The list of productions in our grammar. Each is a tuple of `terminal * production size`.
local productions = {
    { 53, 1 }, { 52, 1 }, { 86, 1 }, { 86, 1 }, { 85, 3 }, { 84, 1 },
    { 84, 1 }, { 84, 1 }, { 84, 1 }, { 84, 1 }, { 84, 1 }, { 84, 1 },
    { 84, 1 }, { 84, 4 }, { 83, 2 }, { 83, 4 }, { 82, 3 }, { 82, 1 },
    { 82, 1 }, { 81, 1 }, { 81, 3 }, { 81, 3 }, { 81, 3 }, { 81, 3 },
    { 81, 3 }, { 81, 3 }, { 81, 3 }, { 81, 3 }, { 81, 3 }, { 81, 3 },
    { 81, 3 }, { 81, 3 }, { 81, 3 }, { 81, 3 }, { 80, 1 }, { 80, 3 },
    { 80, 2 }, { 80, 2 }, { 80, 2 }, { 79, 1 }, { 79, 3 }, { 79, 3 },
    { 78, 4 }, { 77, 4 }, { 76, 0 }, { 76, 2 }, { 76, 3 }, { 76, 1 },
    { 76, 2 }, { 75, 1 }, { 75, 3 }, { 75, 4 }, { 74, 0 }, { 74, 2 },
    { 73, 0 }, { 73, 2 }, { 72, 0 }, { 72, 2 }, { 71, 2 }, { 70, 2 },
    { 70, 2 }, { 69, 0 }, { 69, 2 }, { 69, 3 }, { 68, 0 }, { 68, 2 },
    { 67, 0 }, { 67, 1 }, { 66, 0 }, { 66, 1 }, { 65, 1 }, { 65, 3 },
    { 64, 1 }, { 64, 3 }, { 63, 1 }, { 63, 3 }, { 62, 1 }, { 62, 3 },
    { 61, 1 }, { 61, 3 }, { 61, 1 }, { 60, 3 }, { 60, 3 }, { 60, 5 },
    { 60, 4 }, { 60, 4 }, { 60, 10 }, { 60, 7 }, { 60, 3 }, { 60, 6 },
    { 60, 5 }, { 60, 1 }, { 59, 2 }, { 58, 3 }, { 57, 0 }, { 57, 1 },
    { 57, 3 }, { 56, 1 }, { 56, 3 }, { 56, 5 }, { 55, 1 }, { 55, 1 },
    { 54, 1 },
}

--- The state machine used for our grammar, indexed as `transitions[state][lookahead]`. We don't bother with making this
-- a sparse table, as there's not much point micro-optimising things in Lua! `false` indicates an error, a positive
-- number a shift, and a negative number a reduction.
local f = false
local transitions = {
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 2, f, f, f, f, f, f, f, f, f, 4, f, 190 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 3 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -59 },
    { 5, -45, f, f, f, f, f, 111, 114, f, f, f, 9, f, f, f, f, f, f, f, f, 118, f, f, f, 131, 16, f, f, 135, 145, f, f, f, -45, -45, -45, -45, f, f, 163, f, f, f, f, f, f, f, 166, f, f, f, f, 32, f, f, f, f, f, 168, 170, f, 171, f, f, f, f, f, f, f, f, f, f, f, 176, 177, 178, f, f, f, f, f, 189 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 107, f, 41, 42 },
    { -9, -9, f, -9, -9, f, -9, -9, -9, -9, f, -9, -9, f, f, f, f, -9, -9, -9, -9, -9, f, -9, f, -9, -9, -9, -9, -9, -9, f, f, -9, -9, -9, -9, -9, f, f, -9, -9, -9, -9, -9, -9, f, -9, -9, -9, -9 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 106, f, f, 41, 42 },
    { -13, -13, f, -13, -13, f, -13, -13, -13, -13, f, -13, -13, f, f, f, f, -13, -13, -13, -13, -13, f, -13, f, -13, -13, -13, -13, -13, -13, f, f, -13, -13, -13, -13, -13, f, f, -13, -13, -13, -13, -13, -13, f, -13, -13, -13, -13 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 104, f, 41, 42 },
    { f, f, 6, f, 7, 8, f, f, f, f, 11, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 93, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, -95, f, f, f, f, f, 32, f, 96, 102, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 101, f, 41, 42 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 89, f, 41, 42 },
    { -12, -12, f, -12, -12, f, -12, -12, -12, -12, f, -12, -12, f, f, f, f, -12, -12, -12, -12, -12, f, -12, f, -12, -12, -12, -12, -12, -12, f, f, -12, -12, -12, -12, -12, f, f, -12, -12, -12, -12, -12, -12, f, -12, -12, -12, -12 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 88, f, f, 41, 42 },
    { -8, -8, f, -8, -8, f, -8, -8, -8, -8, f, -8, -8, f, f, f, f, -8, -8, -8, -8, -8, f, -8, f, -8, -8, -8, -8, -8, -8, f, f, -8, -8, -8, -8, -8, f, f, -8, -8, -8, -8, -8, -8, f, -8, -8, -8, -8 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 87, f, f, 41, 42 },
    { -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103 },
    { f, f, f, f, f, f, f, f, f, f, f, f, 18, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 27 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, 19, f, f, f, f, -67, f, f, f, f, f, f, f, f, f, 20, f, f, f, f, f, f, f, f, f, f, 21, f, 24, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 26 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -4, f, -4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -3, f, -3 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -68, f, 22 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, 19, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 20, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 23 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -72, f, -72 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 25 },
    { -5, f, f, f, f, f, f, -5, -5, f, f, f, -5, f, f, f, f, f, f, f, f, -5, f, f, f, -5, -5, f, f, -5, -5, f, f, f, f, -5, f, f, f, f, -5, f, f, f, f, f, f, f, -5 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -71, f, -71 },
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, -62, f, f, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 28, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 29 },
    { -14, -14, f, -14, -14, f, -14, -14, -14, -14, f, -14, -14, f, f, f, f, -14, -14, -14, -14, -14, f, -14, f, -14, -14, -14, -14, -14, -14, f, f, -14, -14, -14, -14, -14, f, f, -14, -14, -14, -14, -14, -14, f, -14, -14, -14, -14 },
    { -10, -10, f, -10, -10, f, -10, -10, -10, -10, f, -10, -10, f, f, f, f, -10, -10, -10, -10, -10, f, -10, f, -10, -10, -10, -10, -10, -10, f, f, -10, -10, -10, -10, -10, f, f, -10, -10, -10, -10, -10, -10, f, -10, -10, -10, -10 },
    { -11, -11, f, -11, -11, f, -11, -11, -11, -11, f, -11, -11, f, f, f, f, -11, -11, -11, -11, -11, f, -11, f, -11, -11, -11, -11, -11, -11, f, f, -11, -11, -11, -11, -11, f, f, -11, -11, -11, -11, -11, -11, f, -11, -11, -11, -11 },
    { -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50 },
    { -7, -7, f, -7, -7, f, -7, -7, -7, -7, f, -7, -7, f, f, f, f, -7, -7, -7, -7, -7, f, -7, f, -7, -7, -7, -7, -7, -7, f, f, -7, -7, -7, -7, -7, f, f, -7, -7, -7, -7, -7, -7, f, -7, -7, -7, -7 },
    { -6, -6, f, -6, -6, 35, -6, -6, -6, -6, 36, -6, 73, 10, f, f, f, -6, -6, -6, -6, -6, f, -6, f, -6, -6, -6, -6, -6, -6, f, f, -6, -6, -6, -6, -6, f, 80, -6, -6, -6, -6, -6, -6, 82, -6, -6, -6, -6, f, f, f, f, f, f, 84, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 86 },
    { -18, -18, f, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18, f, f, f, -18, -18, -18, -18, -18, f, -18, f, -18, -18, -18, -18, -18, -18, f, f, -18, -18, -18, -18, -18, f, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 43, f, 41, 42 },
    { -79, -79, f, -79, -79, -79, -79, -79, -79, -79, -79, -79, -79, -79, f, f, f, -79, -79, -79, -79, -79, f, -79, f, -79, -79, -79, -79, -79, -79, f, f, -79, -79, -79, -79, -79, f, -79, -79, -79, -79, -79, -79, -79, -79, -79, -79, -79, -79 },
    { -20, -20, f, -20, -20, f, -20, -20, -20, 39, f, -20, -20, f, f, f, f, -20, -20, -20, -20, -20, f, -20, f, -20, -20, -20, -20, -20, -20, f, f, -20, -20, -20, -20, -20, f, f, -20, -20, -20, -20, -20, -20, f, -20, -20, -20, -20 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 40, f, f, 41, 42 },
    { -36, -36, -36, -36, -36, -36, -36, -36, -36, 39, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36 },
    { -81, -81, f, -81, -81, -81, -81, -81, -81, -81, -81, -81, -81, -81, f, f, f, -81, -81, -81, -81, -81, f, -81, f, -81, -81, -81, -81, -81, -81, f, f, -81, -81, -81, -81, -81, f, -81, -81, -81, -81, -81, -81, -81, -81, -81, -81, -81, -81 },
    { -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, 72, f, 56, f, f, f, f, 70, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 45, f, 41, 42 },
    { -24, -24, f, -24, -24, f, -24, -24, -24, f, f, -24, -24, f, f, f, f, -24, 46, 48, -24, -24, f, -24, f, -24, -24, -24, -24, -24, -24, f, f, -24, -24, -24, -24, -24, f, f, -24, 50, -24, -24, -24, -24, f, -24, -24, -24, -24 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 47, f, 41, 42 },
    { -25, -25, f, -25, -25, f, -25, -25, -25, f, f, -25, -25, f, f, f, f, -25, -25, -25, -25, -25, f, -25, f, -25, -25, -25, -25, -25, -25, f, f, -25, -25, -25, -25, -25, f, f, -25, -25, -25, -25, -25, -25, f, -25, -25, -25, -25 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 49, f, 41, 42 },
    { -27, -27, f, -27, -27, f, -27, -27, -27, f, f, -27, -27, f, f, f, f, -27, -27, -27, -27, -27, f, -27, f, -27, -27, -27, -27, -27, -27, f, f, -27, -27, -27, -27, -27, f, f, -27, -27, -27, -27, -27, -27, f, -27, -27, -27, -27 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 51, f, 41, 42 },
    { -26, -26, f, -26, -26, f, -26, -26, -26, f, f, -26, -26, f, f, f, f, -26, -26, -26, -26, -26, f, -26, f, -26, -26, -26, -26, -26, -26, f, f, -26, -26, -26, -26, -26, f, f, -26, -26, -26, -26, -26, -26, f, -26, -26, -26, -26 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 53, f, 41, 42 },
    { -22, -22, f, -22, 44, f, -22, -22, -22, f, f, -22, -22, f, f, f, f, 54, 46, 48, 60, -22, f, 62, f, -22, -22, 64, 66, -22, -22, f, f, 68, -22, -22, -22, -22, f, f, -22, 50, -22, -22, 56, -22, f, -22, -22, 70, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 55, f, 41, 42 },
    { -30, -30, f, -30, 44, f, -30, -30, -30, f, f, -30, -30, f, f, f, f, -30, 46, 48, -30, -30, f, -30, f, -30, -30, -30, -30, -30, -30, f, f, -30, -30, -30, -30, -30, f, f, -30, 50, -30, -30, 56, -30, f, -30, -30, -30, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 57, f, 41, 42 },
    { -28, -28, f, -28, 44, f, -28, -28, -28, f, f, -28, -28, f, f, f, f, -28, 46, 48, -28, -28, f, -28, f, -28, -28, -28, -28, -28, -28, f, f, -28, -28, -28, -28, -28, f, f, -28, 50, -28, -28, 56, -28, f, -28, -28, -28, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 59, f, 41, 42 },
    { -23, -23, f, -23, -23, f, -23, -23, -23, f, f, -23, -23, f, f, f, f, -23, 46, 48, -23, -23, f, -23, f, -23, -23, -23, -23, -23, -23, f, f, -23, -23, -23, -23, -23, f, f, -23, 50, -23, -23, -23, -23, f, -23, -23, -23, -23 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 61, f, 41, 42 },
    { -31, -31, f, -31, 44, f, -31, -31, -31, f, f, -31, -31, f, f, f, f, -31, 46, 48, -31, -31, f, -31, f, -31, -31, -31, -31, -31, -31, f, f, -31, -31, -31, -31, -31, f, f, -31, 50, -31, -31, 56, -31, f, -31, -31, -31, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 63, f, 41, 42 },
    { -32, -32, f, -32, 44, f, -32, -32, -32, f, f, -32, -32, f, f, f, f, -32, 46, 48, -32, -32, f, -32, f, -32, -32, -32, -32, -32, -32, f, f, -32, -32, -32, -32, -32, f, f, -32, 50, -32, -32, 56, -32, f, -32, -32, -32, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 65, f, 41, 42 },
    { -33, -33, f, -33, 44, f, -33, -33, -33, f, f, -33, -33, f, f, f, f, -33, 46, 48, -33, -33, f, -33, f, -33, -33, -33, -33, -33, -33, f, f, -33, -33, -33, -33, -33, f, f, -33, 50, -33, -33, 56, -33, f, -33, -33, -33, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 67, f, 41, 42 },
    { -34, -34, f, -34, 44, f, -34, -34, -34, f, f, -34, -34, f, f, f, f, -34, 46, 48, -34, -34, f, -34, f, -34, -34, -34, -34, -34, -34, f, f, -34, -34, -34, -34, -34, f, f, -34, 50, -34, -34, 56, -34, f, -34, -34, -34, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 69, f, 41, 42 },
    { -29, -29, f, -29, 44, f, -29, -29, -29, f, f, -29, -29, f, f, f, f, -29, 46, 48, -29, -29, f, -29, f, -29, -29, -29, -29, -29, -29, f, f, -29, -29, -29, -29, -29, f, f, -29, 50, -29, -29, 56, -29, f, -29, -29, -29, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 71, f, 41, 42 },
    { -21, -21, f, -21, 44, f, -21, -21, -21, f, f, -21, -21, f, f, f, f, 54, 46, 48, 60, -21, f, 62, f, -21, -21, 64, 66, -21, -21, f, f, 68, -21, -21, -21, -21, f, f, -21, 50, -21, -21, 56, -21, f, -21, -21, -21, 58 },
    { -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52, -52 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, -69, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, 74, f, 77, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 79, f, 41, 42 },
    { f, -70, f, f, f, f, -70, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -70, -70, -70, -70, f, f, f, f, f, -70, f, 75 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 76, f, 41, 42 },
    { -74, -74, f, f, 44, f, -74, -74, -74, f, f, 52, -74, f, f, f, f, 54, 46, 48, 60, -74, f, 62, f, -74, -74, 64, 66, -74, -74, f, f, 68, -74, -74, -74, -74, f, f, -74, 50, f, -74, 56, -74, f, f, -74, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 78 },
    { -17, -17, f, -17, -17, -17, -17, -17, -17, -17, -17, -17, -17, -17, f, f, f, -17, -17, -17, -17, -17, f, -17, f, -17, -17, -17, -17, -17, -17, f, f, -17, -17, -17, -17, -17, f, -17, -17, -17, -17, -17, -17, -17, -17, -17, -17, -17, -17 },
    { -73, -73, f, f, 44, f, -73, -73, -73, f, f, 52, -73, f, f, f, f, 54, 46, 48, 60, -73, f, 62, f, -73, -73, 64, 66, -73, -73, f, f, 68, -73, -73, -73, -73, f, f, -73, 50, f, -73, 56, -73, f, f, -73, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 81 },
    { -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51, -51 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 83 },
    { f, f, f, f, f, 35, f, f, f, f, f, f, 73, 10, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 84, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 85 },
    { -19, -19, f, -19, -19, -19, -19, -19, -19, -19, -19, -19, -19, -19, f, f, f, -19, -19, -19, -19, -19, f, -19, f, -19, -19, -19, -19, -19, -19, f, f, -19, -19, -19, -19, -19, f, -19, -19, -19, -19, -19, -19, -19, -19, -19, -19, -19, -19 },
    { -16, -16, f, -16, -16, -16, -16, -16, -16, -16, -16, -16, -16, -16, f, f, f, -16, -16, -16, -16, -16, f, -16, f, -16, -16, -16, -16, -16, -16, f, f, -16, -16, -16, -16, -16, f, -16, -16, -16, -16, -16, -16, -16, -16, -16, -16, -16, -16 },
    { -15, -15, f, -15, -15, -15, -15, -15, -15, -15, -15, -15, -15, -15, f, f, f, -15, -15, -15, -15, -15, f, -15, f, -15, -15, -15, -15, -15, -15, f, f, -15, -15, -15, -15, -15, f, -15, -15, -15, -15, -15, -15, -15, -15, -15, -15, -15, -15 },
    { -38, -38, -38, -38, -38, -38, -38, -38, -38, 39, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38 },
    { -39, -39, -39, -39, -39, -39, -39, -39, -39, 39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, 90, f, 56, f, f, f, f, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 91 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 92, f, 41, 42 },
    { f, f, f, f, 44, f, -100, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, -100, f, -100, f, 70, 58 },
    { -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, 94, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103, -103 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 95, f, 41, 42 },
    { f, f, f, f, 44, f, -99, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, -99, f, -99, f, 70, 58 },
    { f, f, f, f, f, f, 97, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 98, f, -96, f, f, f, f, f, f, 99 },
    { f, f, -101, f, -101, -101, f, f, f, f, -101, f, -101, -101, -101, -101, -101, f, f, f, f, f, -101, f, f, f, -101, f, f, -101, f, -101, f, f, f, f, f, f, -101, f, f, f, f, f, f, f, f, -101 },
    { f, f, -102, f, -102, -102, f, f, f, f, -102, f, -102, -102, -102, -102, -102, f, f, f, f, f, -102, f, f, f, -102, f, f, -102, f, -102, f, f, f, f, f, f, -102, f, f, f, f, f, f, f, f, -102 },
    { f, f, 6, f, 7, 8, f, f, f, f, 11, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 93, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, -95, f, f, f, f, f, 32, f, 96, 100, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 101, f, 41, 42 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -97 },
    { f, f, f, f, 44, f, -98, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, -98, f, -98, f, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 103 },
    { -94, -94, f, -94, -94, -94, -94, -94, -94, -94, -94, -94, -94, -94, f, f, f, -94, -94, -94, -94, -94, f, -94, f, -94, -94, -94, -94, -94, -94, f, f, -94, -94, -94, -94, -94, f, -94, -94, -94, -94, -94, -94, -94, -94, -94, -94, -94, -94 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, 105, 56, f, f, f, f, 70, 58 },
    { -80, -80, f, -80, -80, -80, -80, -80, -80, -80, -80, -80, -80, -80, f, f, f, -80, -80, -80, -80, -80, f, -80, f, -80, -80, -80, -80, -80, -80, f, f, -80, -80, -80, -80, -80, f, -80, -80, -80, -80, -80, -80, -80, -80, -80, -80, -80, -80 },
    { -37, -37, -37, -37, -37, -37, -37, -37, -37, 39, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, 108, 50, f, f, 56, f, f, f, f, 70, 58 },
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, -62, f, f, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 109, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 110 },
    { -84, -84, f, f, f, f, -84, -84, -84, f, f, f, -84, f, f, f, f, f, f, f, f, -84, f, f, f, -84, -84, f, f, -84, -84, f, f, f, -84, -84, -84, -84, f, f, -84, f, f, f, f, f, f, f, -84 },
    { f, -69, 6, f, 7, 8, -69, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, -69, -69, -69, -69, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, 74, f, 112, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 79, f, 41, 42 },
    { f, -46, f, f, f, f, 113, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -46, -46, -46, -46 },
    { f, -47, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -47, -47, -47, -47 },
    { -62, -62, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 115, f, f, f, f, f, f, f, f, f, 4 },
    { f, 116 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 117, f, 41, 42 },
    { -85, -85, f, f, 44, f, -85, -85, -85, f, f, 52, -85, f, f, f, f, 54, 46, 48, 60, -85, f, 62, f, -85, -85, 64, 66, -85, -85, f, f, 68, -85, -85, -85, -85, f, f, -85, 50, f, f, 56, f, f, f, -85, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, 119, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 124, f, f, f, f, f, f, f, 125 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 120 },
    { f, f, f, f, f, f, f, f, f, f, f, f, 18, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 121 },
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, -62, f, f, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 122, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 123 },
    { -90, -90, f, f, f, f, -90, -90, -90, f, f, f, -90, f, f, f, f, f, f, f, f, -90, f, f, f, -90, -90, f, f, -90, -90, f, f, f, -90, -90, -90, -90, f, f, -90, f, f, f, f, f, f, f, -90 },
    { -77, -77, f, f, f, f, -77, -77, -77, f, f, f, -77, f, f, f, f, f, f, f, f, -77, f, f, f, -77, -77, f, f, -77, -77, f, -77, f, -77, -77, -77, -77, f, f, -77, f, f, f, f, -77, f, f, -77 },
    { -57, -57, f, f, f, f, -57, -57, -57, f, f, f, -57, f, f, f, f, f, f, f, f, -57, f, f, f, -57, -57, f, f, -57, -57, f, 126, f, -57, -57, -57, -57, f, f, -57, f, f, f, f, 128, f, f, -57, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 130 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, 127, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 79, f, 41, 42 },
    { -58, -58, f, f, f, f, -58, -58, -58, f, f, f, -58, f, f, f, f, f, f, f, f, -58, f, f, f, -58, -58, f, f, -58, -58, f, f, f, -58, -58, -58, -58, f, f, -58, f, f, f, f, 75, f, f, -58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 129 },
    { -78, -78, f, f, f, f, -78, -78, -78, f, f, f, -78, f, f, f, f, f, f, f, f, -78, f, f, -78, -78, -78, f, f, -78, -78, f, -78, f, -78, -78, -78, -78, f, f, -78, f, f, f, f, -78, f, f, -78 },
    { -89, -89, f, f, f, f, -89, -89, -89, f, f, f, -89, f, f, f, f, f, f, f, f, -89, f, f, f, -89, -89, f, f, -89, -89, f, f, f, -89, -89, -89, -89, f, f, -89, f, f, f, f, f, f, f, -89 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 132, f, 41, 42 },
    { f, f, f, 133, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, f, f, f, f, 70, 58 },
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, -62, -62, -62, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 134, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -44, -44, -44 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 136, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 137 },
    { f, f, f, f, f, f, f, f, f, f, f, f, -40, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -40, f, f, f, f, f, f, -40 },
    { f, f, f, f, f, f, f, f, f, f, f, f, 18, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 138, f, f, f, f, f, f, 140, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 142 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 139 },
    { f, f, f, f, f, f, f, f, f, f, f, f, -41, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -41, f, f, f, f, f, f, -41 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 141 },
    { f, f, f, f, f, f, f, f, f, f, f, f, -42, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -42, f, f, f, f, f, f, -42 },
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, -62, f, f, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 143, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 144 },
    { -91, -91, f, f, f, f, -91, -91, -91, f, f, f, -91, f, f, f, f, f, f, f, f, -91, f, f, f, -91, -91, f, f, -91, -91, f, f, f, -91, -91, -91, -91, f, f, -91, f, f, f, f, f, f, f, -91 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 146, f, f, f, f, f, f, f, 157 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -77, f, f, f, f, f, f, f, 147, f, f, f, f, f, f, f, f, f, f, f, f, -77 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 148, f, 41, 42 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, 149, f, f, f, 70, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 150, f, 41, 42 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, -55, 50, f, f, 56, 151, f, f, f, 70, 58, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 153 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 152, f, 41, 42 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, -56, 50, f, f, 56, f, f, f, f, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 154 },
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, -62, f, f, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 155, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 156 },
    { -87, -87, f, f, f, f, -87, -87, -87, f, f, f, -87, f, f, f, f, f, f, f, f, -87, f, f, f, -87, -87, f, f, -87, -87, f, f, f, -87, -87, -87, -87, f, f, -87, f, f, f, f, f, f, f, -87 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 158, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 128 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, 159, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 79, f, 41, 42 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 160, f, f, f, f, 75 },
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, -62, f, f, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 161, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 162 },
    { -88, -88, f, f, f, f, -88, -88, -88, f, f, f, -88, f, f, f, f, f, f, f, f, -88, f, f, f, -88, -88, f, f, -88, -88, f, f, f, -88, -88, -88, -88, f, f, -88, f, f, f, f, f, f, f, -88 },
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, -62, f, f, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 164, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 165 },
    { -82, -82, f, f, f, f, -82, -82, -82, f, f, f, -82, f, f, f, f, f, f, f, f, -82, f, f, f, -82, -82, f, f, -82, -82, f, f, f, -82, -82, -82, -82, f, f, -82, f, f, f, f, f, f, f, -82 },
    { f, -48, f, f, f, f, 167, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -48, -48, -48, -48 },
    { f, -49, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -49, -49, -49, -49 },
    { -63, -63, f, f, f, f, 169, -63, -63, f, f, f, -63, f, f, f, f, f, f, f, f, -63, f, f, f, -63, -63, f, f, -63, -63, f, f, f, -63, -63, -63, -63, f, f, -63, f, f, f, f, f, f, f, -63 },
    { -64, -64, f, f, f, f, f, -64, -64, f, f, f, -64, f, f, f, f, f, f, f, f, -64, f, f, f, -64, -64, f, f, -64, -64, f, f, f, -64, -64, -64, -64, f, f, -64, f, f, f, f, f, f, f, -64 },
    { f, f, f, f, f, 35, f, f, f, f, 36, f, 73, 10, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 80, f, f, f, f, f, f, 82, f, f, f, f, f, f, f, f, f, f, 84, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 86 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 172, f, f, f, f, f, f, f, f, f, f, f, f, 174 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, 173, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 79, f, 41, 42 },
    { -83, -83, f, f, f, f, -83, -83, -83, f, f, f, -83, f, f, f, f, f, f, f, f, -83, f, f, f, -83, -83, f, f, -83, -83, f, f, f, -83, -83, -83, -83, f, f, -83, f, f, f, f, 75, f, f, -83 },
    { f, f, f, f, f, f, f, f, f, f, f, f, 9, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, f, f, f, 170, f, f, f, f, f, f, f, f, f, f, f, f, f, 175, f, f, f, f, f, f, f, 41 },
    { f, f, f, f, f, -79, f, f, f, f, -79, f, -79, -79, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -76, f, f, f, f, f, f, -79, f, f, f, f, f, -76, -79 },
    { f, f, f, f, f, -79, f, f, f, f, -79, f, -79, -79, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -75, f, f, f, f, f, f, -79, f, f, f, f, f, -75, -79 },
    { -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93, -93 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -65, -65, -65, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 179 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -53, 180, 184, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 186, f, f, f, 188 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, f, f, 38, 181, f, 41, 42 },
    { f, f, f, 182, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, f, f, f, f, 70, 58 },
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, -62, -62, -62, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 183, f, f, f, f, f, f, f, f, f, 4 },
    { -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43, -43 },
    { -62, f, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, f, -62, f, f, f, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, f, f, f, f, f, 185, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -54 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 187 },
    { -86, -86, f, f, f, f, -86, -86, -86, f, f, f, -86, f, f, f, f, f, f, f, f, -86, f, f, f, -86, -86, f, f, -86, -86, f, f, f, -86, -86, -86, -86, f, f, -86, f, f, f, f, f, f, f, -86 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -66, -66, -66 },
    { -92, -92, f, f, f, -81, -92, -92, -92, f, -81, f, -81, -81, f, f, f, f, f, f, f, -92, f, f, f, -92, -92, f, f, -92, -92, f, f, f, -92, -92, -92, -92, f, -81, -92, f, f, f, f, f, -81, f, -92 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -1 },
    { -62, f, 6, f, 7, 8, f, -62, -62, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, -62, 15, f, f, -62, 16, f, f, 17, -62, 30, f, f, -62, f, f, f, 31, f, -62, f, f, f, f, f, f, f, -62, f, f, f, f, 32, f, f, f, 33, 192, f, 34, f, f, 194, f, f, f, f, 4, 196, f, f, f, f, 37, f, f, f, f, 38, 79, f, 41, 42 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 193 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -61 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 195, f, f, f, f, f, f, f, f, f, f, 75 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -60 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -2 },
}

--- Run the parser across a sequence of tokens.
--
-- @tparam table context The current parser context.
-- @tparam function get_next A stateful function which returns the next token.
-- @treturn boolean Whether the parse succeeded or not.
local function parse(context, get_next, start)
    local stack, stack_n = { start or 1, 1, 1 }, 1

    local token, token_start, token_end = get_next()

    while true do
        local state = stack[stack_n]
        local action = transitions[state][token]

        if not action then break -- Error
        elseif action >= 0 then -- Shift
            stack_n = stack_n + 3
            stack[stack_n], stack[stack_n + 1], stack[stack_n + 2] = action, token_start, token_end

            token, token_start, token_end = get_next()
        elseif action >= -2 then -- Accept
            return true
        else -- Reduce
            local reduce = productions[-action]
            local next_state, n = reduce[1], reduce[2]

            if n == 1 then
                -- If we've a single production, just replace the current state.
                action = transitions[stack[stack_n - 3]][next_state]
                stack[stack_n] = action
            else
                local end_pos = stack[stack_n + 2]

                -- Pop n values from the stack
                stack_n = stack_n - n * 3

                -- And shift to our new state.
                state = stack[stack_n]
                action = transitions[state][next_state]

                stack_n = stack_n + 3
                stack[stack_n], stack[stack_n + 2] = action, end_pos
            end

            if not action or action <= 0 then error("Impossible", 0) end
        end
    end

    -- If an error occurs, run our error state machine to accumulate a list of error message candidates.
    local messages = {}
    error_states[1](stack, stack_n, {}, messages)

    -- Sort the list to ensure earlier patterns are used first.
    table.sort(messages, function(a, b) return a[1] < b[1] end)

    -- Then loop until we find an error message which actually works!
    local t = { v = token, s = token_start, e = token_end }
    for i = 1, #messages do
        local action = messages[i]
        local message = error_messages[action[1]](context, stack, stack_n, action, t)
        if message then
            context.report(message)
            return false
        end
    end

    context.report(errors.unexpected_token(token, token_start, token_end))
    return false
end

return {
    tokens = tokens,
    parse = parse,
    repl_exprs = 191, --[[- The repl_exprs starting state. ]]
    program = 1, --[[- The program starting state. ]]
}
