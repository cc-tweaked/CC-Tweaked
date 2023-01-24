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
local function is_same_line(context, previous, token)
    local prev_line = context.get_pos(previous)
    local tok_line = context.get_pos(token.s)
    return prev_line == tok_line and token.v ~= tokens.EOF
end

local function line_end_position(context, previous, token)
    if is_same_line(context, previous, token) then
        return token.s
    else
        return previous + 1
    end
end

local error_messages = {
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 21
        if token.v == tokens.EQUALS then
        return errors.table_key_equals(token.s, token.e)
    end
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 29
        if token.v == tokens.EQUALS then
        return errors.use_double_equals(token.s, token.e)
    end
    end,
    function(context, stack, stack_n, regs, token)
        local lp = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 37
        return (errors.unclosed_brackets(lp.s, lp.e, token.s))
    end,
    function(context, stack, stack_n, regs, token)
        local lp = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 39
        return (errors.unclosed_brackets(lp.s, lp.e, token.s))
    end,
    function(context, stack, stack_n, regs, token)
        local lp = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 41
        return (errors.unclosed_brackets(lp.s, lp.e, token.s))
    end,
    function(context, stack, stack_n, regs, token)
        local loc = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 46
        if token.v == tokens.DOT then
        return errors.local_function_dot(loc.s, loc.e, token.s, token.e)
    end
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 54
        local end_pos = stack[stack_n + 2] -- Hack to get the last position
    if is_same_line(context, end_pos, token) then
        return errors.standalone_name(token.s)
    else
        return errors.standalone_name_call(end_pos)
    end
    end,
    function(context, stack, stack_n, regs, token)
        local start = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 65
        return errors.expected_then(start.s, start.e, line_end_position(context, stack[stack_n + 2], token))
    end,
    function(context, stack, stack_n, regs, token)
        local start = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 69
        return errors.expected_end(start.s, start.e, token.v, token.s, token.e)
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 73
        if token ~= tokens.EOF then
        return errors.expected_statement(token.v, token.s, token.e)
    end
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 81
        return errors.expected_expression(token.v, token.s, token.e)
    end,
}
local error_transitions = {
    { [1] = 48, [4] = 49, [5] = 53, [6] = 28, [7] = 53, [8] = 28, [9] = 53, [10] = 46, [11] = 53, [12] = 28, [13] = 53, [14] = 28, [15] = 53, [16] = 41, [18] = 45, [19] = 2, [20] = 2, [21] = 3, [23] = 4, [24] = 54, [25] = 13, [26] = 3, [27] = 72, [28] = 60, [29] = 29, [30] = 28, [31] = 28, [32] = 59, [33] = 28, [34] = 30, [35] = 31, [36] = 53, [37] = 30, [38] = 32, [39] = 53, [40] = 33, [41] = 30, [42] = 34, [43] = 75, [44] = 53, [45] = 67, [46] = 53, [47] = 68, [48] = 53, [49] = 68, [50] = 53, [51] = 68, [52] = 53, [53] = 69, [54] = 53, [55] = 70, [56] = 53, [57] = 67, [58] = 53, [59] = 67, [60] = 53, [61] = 70, [62] = 53, [63] = 70, [64] = 53, [65] = 70, [66] = 53, [67] = 70, [68] = 53, [69] = 70, [70] = 53, [71] = 71, [72] = 42, [73] = 45, [74] = 27, [75] = 53, [76] = 65, [77] = 54, [78] = 35, [79] = 66, [81] = 43, [84] = 31, [85] = 36, [86] = 37, [87] = 38, [88] = 38, [89] = 75, [91] = 53, [92] = 62, [93] = 44, [94] = 53, [95] = 63, [96] = 5, [97] = 6, [98] = 6, [99] = 7, [100] = 8, [101] = 73, [102] = 55, [103] = 39, [104] = 74, [105] = 40, [106] = 38, [107] = 61, [108] = 72, [109] = 60, [110] = 14, [111] = 15, [112] = 16, [113] = 17, [114] = 48, [116] = 53, [117] = 64, [120] = 56, [121] = 72, [122] = 60, [123] = 18, [124] = 57, [125] = 19, [127] = 20, [129] = 58, [130] = 21, [131] = 53, [132] = 61, [133] = 72, [134] = 60, [135] = 60, [136] = 53, [137] = 61, [138] = 50, [139] = 9, [140] = 51, [141] = 10, [142] = 60, [143] = 22, [145] = 56, [151] = 72, [152] = 60, [153] = 14, [155] = 56, [156] = 53, [157] = 61, [158] = 53, [159] = 61, [160] = 53, [161] = 61, [163] = 72, [164] = 60, [165] = 23, [169] = 72, [170] = 60, [171] = 22, [172] = 76, [173] = 60, [174] = 21, [175] = 15, [176] = 16, [177] = 24, [178] = 25, [180] = 47, [182] = 21, [184] = 11, [185] = 12, [186] = 52, [187] = 26, [189] = 48 },
    { [18] = 45, [22] = 77 }, {}, {}, { [10] = 46, [99] = 7 }, {}, {}, {},
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    { [73] = 45, [111] = 15 },
    { [5] = 86, [7] = 28, [9] = 100, [10] = 102, [11] = 101, [13] = 28, [15] = 28, [36] = 101, [39] = 38, [44] = 94, [46] = 95, [48] = 95, [50] = 95, [52] = 96, [54] = 97, [56] = 94, [58] = 94, [60] = 97, [62] = 97, [64] = 97, [66] = 97, [68] = 97, [70] = 98, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [189] = 86 },
    {},
    { [5] = 86, [7] = 30, [9] = 100, [10] = 102, [11] = 101, [13] = 30, [15] = 30, [36] = 101, [39] = 38, [44] = 94, [46] = 95, [48] = 95, [50] = 95, [52] = 96, [54] = 97, [56] = 94, [58] = 94, [60] = 97, [62] = 97, [64] = 97, [66] = 97, [68] = 97, [70] = 98, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [189] = 86 },
    { [34] = 105, [83] = 104, [179] = 105 },
    { [5] = 86, [9] = 100, [10] = 102, [11] = 101, [36] = 101, [44] = 94, [46] = 95, [48] = 95, [50] = 95, [52] = 96, [54] = 97, [56] = 94, [58] = 94, [60] = 97, [62] = 97, [64] = 97, [66] = 97, [68] = 97, [70] = 98, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [189] = 86 },
    {},
    { [5] = 86, [7] = 34, [9] = 100, [10] = 102, [11] = 101, [13] = 34, [15] = 34, [36] = 101, [39] = 38, [44] = 94, [46] = 95, [48] = 95, [50] = 95, [52] = 96, [54] = 97, [56] = 94, [58] = 94, [60] = 97, [62] = 97, [64] = 97, [66] = 97, [68] = 97, [70] = 98, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [189] = 86 },
    {}, {}, {}, {}, {}, {},
    { [4] = 47, [5] = 86, [7] = 30, [9] = 100, [11] = 101, [13] = 30, [15] = 30, [18] = 45, [22] = 77, [36] = 101, [39] = 38, [44] = 94, [46] = 95, [48] = 95, [50] = 95, [52] = 96, [54] = 97, [56] = 94, [58] = 94, [60] = 97, [62] = 97, [64] = 97, [66] = 97, [68] = 97, [70] = 98, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [111] = 89, [116] = 90, [118] = 26, [126] = 91, [128] = 109, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [183] = 80, [189] = 86 },
    {}, {}, { [10] = 102, [99] = 99 }, {}, {}, {}, {},
    { [27] = 60, [108] = 60, [121] = 60, [133] = 60, [138] = 78, [140] = 79, [151] = 60, [163] = 60, [169] = 60, [172] = 111 },
    {}, {}, {}, {}, {}, {}, { [17] = 114, [119] = 114, [144] = 114 }, {}, {},
    { [4] = 47, [5] = 86, [7] = 30, [9] = 100, [10] = 102, [11] = 101, [13] = 30, [15] = 30, [36] = 101, [39] = 38, [44] = 94, [46] = 95, [48] = 95, [50] = 95, [52] = 96, [54] = 97, [56] = 94, [58] = 94, [60] = 97, [62] = 97, [64] = 97, [66] = 97, [68] = 97, [70] = 98, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [183] = 80, [189] = 86 },
    { [1] = 60, [2] = 60, [3] = 60, [4] = 60, [5] = 111, [6] = 60, [7] = 60, [8] = 60, [9] = 60, [10] = 60, [11] = 60, [12] = 60, [13] = 60, [14] = 60, [15] = 60, [16] = 60, [17] = 60, [18] = 60, [19] = 60, [20] = 60, [21] = 60, [22] = 60, [23] = 60, [24] = 60, [25] = 60, [26] = 60, [27] = 60, [28] = 60, [29] = 60, [30] = 60, [31] = 60, [32] = 60, [33] = 60, [34] = 60, [35] = 60, [36] = 60, [37] = 60, [38] = 60, [39] = 60, [40] = 60, [41] = 60, [42] = 60, [43] = 60, [44] = 60, [45] = 60, [46] = 60, [47] = 60, [48] = 60, [49] = 60, [50] = 60, [51] = 60, [52] = 60, [53] = 60, [54] = 60, [55] = 60, [56] = 60, [57] = 60, [58] = 60, [59] = 60, [60] = 60, [61] = 60, [62] = 60, [63] = 60, [64] = 60, [65] = 60, [66] = 60, [67] = 60, [68] = 60, [69] = 60, [70] = 60, [71] = 60, [72] = 60, [73] = 60, [74] = 60, [75] = 60, [76] = 60, [77] = 60, [78] = 60, [79] = 60, [80] = 60, [81] = 60, [82] = 60, [83] = 60, [84] = 60, [85] = 60, [86] = 60, [87] = 60, [88] = 60, [89] = 60, [90] = 60, [91] = 60, [92] = 60, [93] = 60, [94] = 60, [95] = 60, [96] = 60, [97] = 60, [98] = 60, [99] = 60, [100] = 60, [101] = 60, [102] = 60, [103] = 60, [104] = 60, [105] = 60, [106] = 60, [107] = 60, [108] = 60, [109] = 60, [110] = 60, [111] = 60, [112] = 60, [113] = 60, [114] = 60, [115] = 60, [116] = 60, [117] = 60, [118] = 60, [119] = 60, [120] = 60, [121] = 60, [122] = 60, [123] = 60, [124] = 60, [125] = 60, [126] = 60, [127] = 60, [128] = 60, [129] = 60, [130] = 60, [131] = 111, [132] = 60, [133] = 60, [134] = 60, [135] = 60, [136] = 60, [137] = 60, [138] = 60, [139] = 60, [140] = 60, [141] = 60, [142] = 60, [143] = 60, [144] = 60, [145] = 60, [146] = 60, [147] = 60, [148] = 60, [149] = 60, [150] = 60, [151] = 60, [152] = 60, [153] = 60, [154] = 111, [155] = 60, [156] = 60, [157] = 60, [158] = 60, [159] = 60, [160] = 60, [161] = 60, [162] = 60, [163] = 60, [164] = 60, [165] = 60, [166] = 60, [167] = 60, [168] = 60, [169] = 60, [170] = 60, [171] = 60, [172] = 111, [173] = 60, [174] = 60, [175] = 60, [176] = 60, [177] = 60, [178] = 60, [179] = 60, [180] = 60, [181] = 60, [182] = 60, [183] = 60, [184] = 60, [185] = 60, [186] = 60, [187] = 60, [188] = 60, [189] = 60, [190] = 60, [191] = 60, [192] = 60, [193] = 60, [194] = 60 },
    { [131] = 115, [136] = 115 }, {}, {}, {}, {},
    { [73] = 45, [111] = 15, [126] = 83, [181] = 19 }, {}, {}, {}, {}, {},
    { [1] = 60, [2] = 60, [3] = 60, [4] = 60, [5] = 111, [6] = 60, [7] = 60, [8] = 60, [9] = 60, [10] = 60, [11] = 60, [12] = 60, [13] = 60, [14] = 60, [15] = 60, [16] = 60, [17] = 60, [18] = 60, [19] = 60, [20] = 60, [21] = 60, [22] = 60, [23] = 60, [24] = 60, [25] = 60, [26] = 60, [27] = 60, [28] = 60, [29] = 60, [30] = 60, [31] = 60, [32] = 60, [33] = 60, [34] = 60, [35] = 60, [36] = 60, [37] = 60, [38] = 60, [39] = 60, [40] = 60, [41] = 60, [42] = 60, [43] = 60, [44] = 60, [45] = 60, [46] = 60, [47] = 60, [48] = 60, [49] = 60, [50] = 60, [51] = 60, [52] = 60, [53] = 60, [54] = 60, [55] = 60, [56] = 60, [57] = 60, [58] = 60, [59] = 60, [60] = 60, [61] = 60, [62] = 60, [63] = 60, [64] = 60, [65] = 60, [66] = 60, [67] = 60, [68] = 60, [69] = 60, [70] = 60, [71] = 60, [72] = 60, [73] = 60, [74] = 60, [75] = 60, [76] = 60, [77] = 60, [78] = 60, [79] = 60, [80] = 60, [81] = 60, [82] = 60, [83] = 60, [84] = 60, [85] = 60, [86] = 60, [87] = 60, [88] = 60, [89] = 60, [90] = 60, [91] = 60, [92] = 60, [93] = 60, [94] = 60, [95] = 60, [96] = 60, [97] = 60, [98] = 60, [99] = 60, [100] = 60, [101] = 60, [102] = 60, [103] = 60, [104] = 60, [105] = 60, [106] = 60, [107] = 60, [108] = 60, [109] = 60, [110] = 60, [111] = 60, [112] = 60, [113] = 60, [114] = 60, [115] = 60, [116] = 60, [117] = 60, [118] = 60, [119] = 60, [120] = 60, [121] = 60, [122] = 60, [123] = 60, [124] = 60, [125] = 60, [126] = 60, [127] = 60, [128] = 60, [129] = 60, [130] = 60, [131] = 111, [132] = 60, [133] = 60, [134] = 60, [135] = 60, [136] = 60, [137] = 60, [138] = 60, [139] = 60, [140] = 60, [141] = 60, [142] = 60, [143] = 60, [144] = 60, [145] = 60, [146] = 60, [147] = 60, [148] = 60, [149] = 60, [150] = 60, [151] = 60, [152] = 60, [153] = 60, [154] = 111, [155] = 60, [156] = 60, [157] = 60, [158] = 60, [159] = 60, [160] = 60, [161] = 60, [162] = 60, [163] = 60, [164] = 60, [165] = 60, [166] = 60, [167] = 60, [168] = 60, [169] = 60, [170] = 60, [171] = 60, [172] = 111, [173] = 60, [174] = 60, [175] = 60, [176] = 60, [177] = 60, [178] = 60, [179] = 60, [180] = 60, [181] = 60, [182] = 60, [183] = 60, [184] = 60, [185] = 60, [186] = 60, [187] = 60, [188] = 60, [189] = 60, [190] = 60, [191] = 60, [192] = 60, [193] = 60, [194] = 60 },
    { [10] = 46, [99] = 7 }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    { [1] = 48, [27] = 72, [108] = 72, [114] = 48, [121] = 72, [133] = 72, [138] = 50, [140] = 51, [151] = 72, [163] = 72, [169] = 72, [172] = 76, [189] = 48 },
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    {},
    { [4] = 85, [5] = 86, [7] = 30, [9] = 100, [10] = 102, [11] = 101, [13] = 30, [15] = 30, [36] = 101, [39] = 38, [44] = 94, [46] = 95, [48] = 95, [50] = 95, [52] = 96, [54] = 97, [56] = 94, [58] = 94, [60] = 97, [62] = 97, [64] = 97, [66] = 97, [68] = 97, [70] = 98, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [189] = 86 },
    {}, {}, {}, {}, {}, {}, {},
    { [27] = 60, [108] = 60, [121] = 60, [133] = 60, [138] = 78, [140] = 79, [151] = 60, [163] = 60, [169] = 60, [172] = 111 },
    { [118] = 135 }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    { [73] = 45, [111] = 15, [126] = 83, [181] = 19 },
    { [5] = 86, [9] = 100, [10] = 102, [11] = 101, [36] = 101, [52] = 96, [54] = 97, [56] = 94, [60] = 97, [62] = 97, [64] = 97, [66] = 97, [68] = 97, [70] = 98, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [189] = 86 },
    { [5] = 86, [9] = 100, [10] = 102, [11] = 101, [36] = 101, [44] = 94, [52] = 96, [54] = 97, [56] = 94, [58] = 94, [60] = 97, [62] = 97, [64] = 97, [66] = 97, [68] = 97, [70] = 98, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [189] = 86 },
    { [5] = 86, [9] = 100, [10] = 102, [11] = 101, [36] = 101, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [189] = 86 },
    { [5] = 86, [9] = 100, [10] = 102, [11] = 101, [36] = 101, [52] = 96, [70] = 98, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [189] = 86 },
    { [5] = 86, [9] = 100, [10] = 102, [11] = 101, [36] = 101, [52] = 96, [73] = 100, [75] = 93, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [181] = 92, [189] = 86 },
    {},
    { [5] = 86, [7] = 28, [9] = 100, [10] = 102, [11] = 101, [13] = 28, [15] = 28, [34] = 105, [36] = 101, [39] = 38, [44] = 94, [46] = 95, [48] = 95, [50] = 95, [52] = 96, [54] = 97, [56] = 94, [58] = 94, [60] = 97, [62] = 97, [64] = 97, [66] = 97, [68] = 97, [70] = 98, [73] = 100, [75] = 93, [83] = 104, [91] = 87, [94] = 88, [99] = 99, [111] = 89, [116] = 90, [126] = 91, [131] = 86, [136] = 86, [156] = 86, [158] = 86, [160] = 86, [167] = 86, [179] = 105, [181] = 92, [189] = 86 },
    {}, {}, {},
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
        if top == 10 or top == 18 or top == 73 or top == 172 then
            regs[1] = idx
        end
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
        return error_states[45](stack, idx - 3, regs, out)
    end,
    error_jump(77), -- State 4
    function(stack, idx, regs, out) -- State 5
        local top = stack[idx]
        if top == 10 then regs[1] = idx end
        local next = error_transitions[5][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(5), -- State 6
    error_jump(5), -- State 7
    error_jump(7), -- State 8
    error_jump(78), -- State 9
    error_jump(79), -- State 10
    error_jump(80), -- State 11
    error_jump(47), -- State 12
    error_jump(81), -- State 13
    error_jump(82), -- State 14
    error_jump(49), -- State 15
    error_jump(15), -- State 16
    error_jump(16), -- State 17
    error_jump(14), -- State 18
    error_jump(26), -- State 19
    error_jump(83), -- State 20
    error_jump(19), -- State 21
    error_jump(18), -- State 22
    error_jump(84), -- State 23
    error_jump(85), -- State 24
    error_jump(24), -- State 25
    error_jump(85), -- State 26
    function(stack, idx, regs, out) -- State 27
        local top = stack[idx]
        if top == 73 then regs[1] = idx end
        local next = error_transitions[27][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 28
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[28][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(103), -- State 29
    function(stack, idx, regs, out) -- State 30
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[30][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 31
        local top = stack[idx]
        local next = error_transitions[31][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 32
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[32][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(38), -- State 33
    function(stack, idx, regs, out) -- State 34
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[34][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(106), -- State 35
    error_jump(104), -- State 36
    error_jump(105), -- State 37
    error_jump(34), -- State 38
    error_jump(107), -- State 39
    error_jump(108), -- State 40
    function(stack, idx, regs, out) -- State 41
        local top = stack[idx]
        if top == 9 or top == 11 or top == 18 or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[41][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(43), -- State 42
    error_jump(110), -- State 43
    function(stack, idx, regs, out) -- State 44
        local top = stack[idx]
        if top == 10 then regs[1] = idx end
        local next = error_transitions[44][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 45
        out[#out + 1] = { 3, regs[1] }
    end,
    function(stack, idx, regs, out) -- State 46
        out[#out + 1] = { 4, regs[1] }
    end,
    function(stack, idx, regs, out) -- State 47
        out[#out + 1] = { 7 }
    end,
    function(stack, idx, regs, out) -- State 48
        out[#out + 1] = { 10 }
    end,
    function(stack, idx, regs, out) -- State 49
        out[#out + 1] = { 10 }
        local top = stack[idx]
        if top == 172 then regs[1] = idx end
        local next = error_transitions[49][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 50
        out[#out + 1] = { 10 }
        return error_states[112](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 51
        out[#out + 1] = { 10 }
        return error_states[60](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 52
        out[#out + 1] = { 10 }
        return error_states[113](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 53
        out[#out + 1] = { 11 }
    end,
    function(stack, idx, regs, out) -- State 54
        regs[1] = idx
        return error_states[45](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 55
        regs[1] = idx
        return error_states[46](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 56
        local top = stack[idx]
        local next = error_transitions[56][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(26), -- State 57
    error_jump(109), -- State 58
    function(stack, idx, regs, out) -- State 59
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[59][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 60
        local top = stack[idx]
        if top == 5 or top == 131 or top == 154 or top == 172 then
            regs[1] = idx
        end
        local next = error_transitions[60][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 61
        out[#out + 1] = { 2 }
        local top = stack[idx]
        if top == 131 or top == 136 then regs[1] = idx end
        local next = error_transitions[61][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 62
        out[#out + 1] = { 2 }
        return error_states[116](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 63
        out[#out + 1] = { 2 }
        return error_states[7](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 64
        out[#out + 1] = { 2 }
        return error_states[21](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 65
        out[#out + 1] = { 2 }
        return error_states[117](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 66
        out[#out + 1] = { 2 }
        local top = stack[idx]
        if top == 73 then regs[1] = idx end
        local next = error_transitions[66][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 67
        out[#out + 1] = { 2 }
        return error_states[118](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 68
        out[#out + 1] = { 2 }
        return error_states[119](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 69
        out[#out + 1] = { 2 }
        return error_states[120](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 70
        out[#out + 1] = { 2 }
        return error_states[121](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 71
        out[#out + 1] = { 2 }
        return error_states[122](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 72
        out[#out + 1] = { 10 }
        local top = stack[idx]
        if top == 5 or top == 131 or top == 154 or top == 172 then
            regs[1] = idx
        end
        local next = error_transitions[72][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 73
        out[#out + 1] = { 2 }
        out[#out + 1] = { 1 }
        local top = stack[idx]
        if top == 10 then regs[1] = idx end
        local next = error_transitions[73][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 74
        out[#out + 1] = { 2 }
        regs[1] = idx
        return error_states[45](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 75
        out[#out + 1] = { 2 }
        regs[1] = idx
        return error_states[123](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 76
        out[#out + 1] = { 10 }
        out[#out + 1] = { 9, regs[1] }
        return error_states[60](stack, idx - 3, regs, out)
    end,
    error_jump(3), -- State 77
    error_jump(112), -- State 78
    error_jump(60), -- State 79
    error_jump(12), -- State 80
    error_jump(124), -- State 81
    error_jump(21), -- State 82
    error_jump(19), -- State 83
    error_jump(125), -- State 84
    function(stack, idx, regs, out) -- State 85
        local top = stack[idx]
        if top == 172 then regs[1] = idx end
        local next = error_transitions[85][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 86
        out[#out + 1] = { 2 }
    end,
    function(stack, idx, regs, out) -- State 87
        out[#out + 1] = { 2 }
        return error_states[8](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 88
        out[#out + 1] = { 2 }
        return error_states[5](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 89
        out[#out + 1] = { 2 }
        return error_states[49](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 90
        out[#out + 1] = { 2 }
        return error_states[19](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 91
        out[#out + 1] = { 2 }
        return error_states[19](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 92
        out[#out + 1] = { 2 }
        return error_states[26](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 93
        out[#out + 1] = { 2 }
        return error_states[126](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 94
        out[#out + 1] = { 2 }
        return error_states[127](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 95
        out[#out + 1] = { 2 }
        return error_states[128](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 96
        out[#out + 1] = { 2 }
        return error_states[129](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 97
        out[#out + 1] = { 2 }
        return error_states[130](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 98
        out[#out + 1] = { 2 }
        return error_states[131](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 99
        out[#out + 1] = { 2 }
        out[#out + 1] = { 1 }
        return error_states[5](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 100
        out[#out + 1] = { 3, regs[1] }
        out[#out + 1] = { 2 }
    end,
    function(stack, idx, regs, out) -- State 101
        out[#out + 1] = { 5, regs[1] }
        out[#out + 1] = { 2 }
    end,
    function(stack, idx, regs, out) -- State 102
        out[#out + 1] = { 4, regs[1] }
        out[#out + 1] = { 2 }
        out[#out + 1] = { 1 }
    end,
    error_jump(132), -- State 103
    error_jump(37), -- State 104
    function(stack, idx, regs, out) -- State 105
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[105][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(31), -- State 106
    error_jump(133), -- State 107
    error_jump(30), -- State 108
    error_jump(57), -- State 109
    error_jump(59), -- State 110
    function(stack, idx, regs, out) -- State 111
        out[#out + 1] = { 9, regs[1] }
        return error_states[60](stack, idx - 3, regs, out)
    end,
    error_jump(134), -- State 112
    function(stack, idx, regs, out) -- State 113
        local top = stack[idx]
        if top == 172 then regs[1] = idx end
        local next = error_transitions[113][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 114
        local top = stack[idx]
        if top == 118 then regs[1] = idx end
        local next = error_transitions[114][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 115
        out[#out + 1] = { 8, regs[1] }
    end,
    error_jump(8), -- State 116
    error_jump(126), -- State 117
    error_jump(127), -- State 118
    error_jump(128), -- State 119
    error_jump(129), -- State 120
    error_jump(130), -- State 121
    error_jump(131), -- State 122
    function(stack, idx, regs, out) -- State 123
        out[#out + 1] = { 5, regs[1] }
    end,
    error_jump(72), -- State 124
    error_jump(22), -- State 125
    function(stack, idx, regs, out) -- State 126
        local top = stack[idx]
        if top == 73 then regs[1] = idx end
        local next = error_transitions[126][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 127
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[127][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 128
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[128][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 129
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[129][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 130
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[130][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 131
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[131][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(28), -- State 132
    function(stack, idx, regs, out) -- State 133
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[133][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(136), -- State 134
    function(stack, idx, regs, out) -- State 135
        out[#out + 1] = { 6, regs[1] }
    end,
    error_jump(60), -- State 136
}

--- The list of productions in our grammar. Each is a tuple of `terminal * production size`.
local productions = {
    { 53 --[[ program' ]], 1 }, { 52 --[[ repl_exprs' ]], 1 },
    { 84 --[[ arg ]], 1 }, { 84 --[[ arg ]], 1 }, { 83 --[[ args ]], 3 },
    { 82 --[[ atom ]], 1 }, { 82 --[[ atom ]], 1 }, { 82 --[[ atom ]], 1 },
    { 82 --[[ atom ]], 1 }, { 82 --[[ atom ]], 1 }, { 82 --[[ atom ]], 1 },
    { 82 --[[ atom ]], 1 }, { 82 --[[ atom ]], 1 }, { 82 --[[ atom ]], 4 },
    { 81 --[[ call ]], 2 }, { 81 --[[ call ]], 4 },
    { 80 --[[ call_args ]], 3 }, { 80 --[[ call_args ]], 1 },
    { 80 --[[ call_args ]], 1 }, { 79 --[[ expr ]], 1 },
    { 79 --[[ expr ]], 3 }, { 79 --[[ expr ]], 3 }, { 79 --[[ expr ]], 3 },
    { 79 --[[ expr ]], 3 }, { 79 --[[ expr ]], 3 }, { 79 --[[ expr ]], 3 },
    { 79 --[[ expr ]], 3 }, { 79 --[[ expr ]], 3 }, { 79 --[[ expr ]], 3 },
    { 79 --[[ expr ]], 3 }, { 79 --[[ expr ]], 3 }, { 79 --[[ expr ]], 3 },
    { 79 --[[ expr ]], 3 }, { 79 --[[ expr ]], 3 },
    { 78 --[[ expr_pow ]], 1 }, { 78 --[[ expr_pow ]], 3 },
    { 78 --[[ expr_pow ]], 2 }, { 78 --[[ expr_pow ]], 2 },
    { 78 --[[ expr_pow ]], 2 }, { 77 --[[ function_name ]], 1 },
    { 77 --[[ function_name ]], 3 }, { 77 --[[ function_name ]], 3 },
    { 76 --[[ last_stat ]], 0 }, { 76 --[[ last_stat ]], 2 },
    { 76 --[[ last_stat ]], 3 }, { 76 --[[ last_stat ]], 1 },
    { 76 --[[ last_stat ]], 2 }, { 75 --[[ name ]], 1 },
    { 75 --[[ name ]], 3 }, { 75 --[[ name ]], 4 },
    { 74 --[[ option(__anonymous_1) ]], 0 },
    { 74 --[[ option(__anonymous_1) ]], 2 },
    { 73 --[[ option(__anonymous_2) ]], 0 },
    { 73 --[[ option(__anonymous_2) ]], 2 },
    { 72 --[[ option(__anonymous_3) ]], 0 },
    { 72 --[[ option(__anonymous_3) ]], 2 }, { 71 --[[ program ]], 2 },
    { 70 --[[ repl_exprs ]], 2 }, { 70 --[[ repl_exprs ]], 2 },
    { 69 --[[ rlist(__anonymous_0) ]], 0 },
    { 69 --[[ rlist(__anonymous_0) ]], 2 },
    { 69 --[[ rlist(__anonymous_0) ]], 3 },
    { 68 --[[ rlist(if_clause(ELSEIF)) ]], 0 },
    { 68 --[[ rlist(if_clause(ELSEIF)) ]], 5 },
    { 67 --[[ sep_list0(COMMA,arg) ]], 0 },
    { 67 --[[ sep_list0(COMMA,arg) ]], 1 },
    { 66 --[[ sep_list0(COMMA,expr) ]], 0 },
    { 66 --[[ sep_list0(COMMA,expr) ]], 1 },
    { 65 --[[ sep_list1(COMMA,arg) ]], 1 },
    { 65 --[[ sep_list1(COMMA,arg) ]], 3 },
    { 64 --[[ sep_list1(COMMA,expr) ]], 1 },
    { 64 --[[ sep_list1(COMMA,expr) ]], 3 },
    { 63 --[[ sep_list1(COMMA,name) ]], 1 },
    { 63 --[[ sep_list1(COMMA,name) ]], 3 },
    { 62 --[[ sep_list1(COMMA,var) ]], 1 },
    { 62 --[[ sep_list1(COMMA,var) ]], 3 }, { 61 --[[ simple_expr ]], 1 },
    { 61 --[[ simple_expr ]], 3 }, { 61 --[[ simple_expr ]], 1 },
    { 60 --[[ stmt ]], 3 }, { 60 --[[ stmt ]], 3 }, { 60 --[[ stmt ]], 5 },
    { 60 --[[ stmt ]], 4 }, { 60 --[[ stmt ]], 7 }, { 60 --[[ stmt ]], 10 },
    { 60 --[[ stmt ]], 7 }, { 60 --[[ stmt ]], 3 }, { 60 --[[ stmt ]], 6 },
    { 60 --[[ stmt ]], 5 }, { 60 --[[ stmt ]], 1 }, { 59 --[[ stmts ]], 2 },
    { 58 --[[ table ]], 3 }, { 57 --[[ table_body ]], 0 },
    { 57 --[[ table_body ]], 1 }, { 57 --[[ table_body ]], 3 },
    { 56 --[[ table_entry ]], 1 }, { 56 --[[ table_entry ]], 3 },
    { 56 --[[ table_entry ]], 5 }, { 55 --[[ table_sep ]], 1 },
    { 55 --[[ table_sep ]], 1 }, { 54 --[[ var ]], 1 },
}

--- The state machine used for our grammar, indexed as `transitions[state][lookahead]`. We don't bother with making this
-- a sparse table, as there's not much point micro-optimising things in Lua! `false` indicates an error, a positive
-- number a shift, and a negative number a reduction.
local f = false
local transitions = {
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 2, f, f, f, f, f, f, f, f, f, 4, f, 188 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 3 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -57 },
    { 5, -43, f, f, f, f, f, 111, 114, f, f, f, 9, f, f, f, f, f, f, f, f, 118, f, f, f, 131, 16, f, f, 144, 154, f, f, f, -43, -43, -43, -43, f, f, 172, f, f, f, f, f, f, f, 175, f, f, f, f, 32, f, f, f, f, f, 177, 179, f, 180, f, f, f, f, f, f, f, f, f, f, f, 185, 186, f, f, f, f, 187 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 107, f, 41, 42 },
    { -9, -9, f, -9, -9, f, -9, -9, -9, -9, f, -9, -9, f, f, f, f, -9, -9, -9, -9, -9, f, -9, f, -9, -9, -9, -9, -9, -9, f, f, -9, -9, -9, -9, -9, f, f, -9, -9, -9, -9, -9, -9, f, -9, -9, -9, -9 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 106, f, f, 41, 42 },
    { -13, -13, f, -13, -13, f, -13, -13, -13, -13, f, -13, -13, f, f, f, f, -13, -13, -13, -13, -13, f, -13, f, -13, -13, -13, -13, -13, -13, f, f, -13, -13, -13, -13, -13, f, f, -13, -13, -13, -13, -13, -13, f, -13, -13, -13, -13 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 104, f, 41, 42 },
    { f, f, 6, f, 7, 8, f, f, f, f, 11, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 93, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, -93, f, f, f, f, f, 32, f, 96, 102, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 101, f, 41, 42 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 89, f, 41, 42 },
    { -12, -12, f, -12, -12, f, -12, -12, -12, -12, f, -12, -12, f, f, f, f, -12, -12, -12, -12, -12, f, -12, f, -12, -12, -12, -12, -12, -12, f, f, -12, -12, -12, -12, -12, f, f, -12, -12, -12, -12, -12, -12, f, -12, -12, -12, -12 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 88, f, f, 41, 42 },
    { -8, -8, f, -8, -8, f, -8, -8, -8, -8, f, -8, -8, f, f, f, f, -8, -8, -8, -8, -8, f, -8, f, -8, -8, -8, -8, -8, -8, f, f, -8, -8, -8, -8, -8, f, f, -8, -8, -8, -8, -8, -8, f, -8, -8, -8, -8 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 87, f, f, 41, 42 },
    { -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101 },
    { f, f, f, f, f, f, f, f, f, f, f, f, 18, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 27 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, 19, f, f, f, f, -65, f, f, f, f, f, f, f, f, f, 20, f, f, f, f, f, f, f, f, f, f, 21, f, 24, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 26 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -4, f, -4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -3, f, -3 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -66, f, 22 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, 19, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 20, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 23 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -70, f, -70 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 25 },
    { -5, f, f, f, f, f, f, -5, -5, f, f, f, -5, f, f, f, f, f, f, f, f, -5, f, f, f, -5, -5, f, f, -5, -5, f, f, f, f, -5, f, f, f, f, -5, f, f, f, f, f, f, f, -5 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -69, f, -69 },
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, -60, f, f, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 28, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 29 },
    { -14, -14, f, -14, -14, f, -14, -14, -14, -14, f, -14, -14, f, f, f, f, -14, -14, -14, -14, -14, f, -14, f, -14, -14, -14, -14, -14, -14, f, f, -14, -14, -14, -14, -14, f, f, -14, -14, -14, -14, -14, -14, f, -14, -14, -14, -14 },
    { -10, -10, f, -10, -10, f, -10, -10, -10, -10, f, -10, -10, f, f, f, f, -10, -10, -10, -10, -10, f, -10, f, -10, -10, -10, -10, -10, -10, f, f, -10, -10, -10, -10, -10, f, f, -10, -10, -10, -10, -10, -10, f, -10, -10, -10, -10 },
    { -11, -11, f, -11, -11, f, -11, -11, -11, -11, f, -11, -11, f, f, f, f, -11, -11, -11, -11, -11, f, -11, f, -11, -11, -11, -11, -11, -11, f, f, -11, -11, -11, -11, -11, f, f, -11, -11, -11, -11, -11, -11, f, -11, -11, -11, -11 },
    { -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48, -48 },
    { -7, -7, f, -7, -7, f, -7, -7, -7, -7, f, -7, -7, f, f, f, f, -7, -7, -7, -7, -7, f, -7, f, -7, -7, -7, -7, -7, -7, f, f, -7, -7, -7, -7, -7, f, f, -7, -7, -7, -7, -7, -7, f, -7, -7, -7, -7 },
    { -6, -6, f, -6, -6, 35, -6, -6, -6, -6, 36, -6, 73, 10, f, f, f, -6, -6, -6, -6, -6, f, -6, f, -6, -6, -6, -6, -6, -6, f, f, -6, -6, -6, -6, -6, f, 80, -6, -6, -6, -6, -6, -6, 82, -6, -6, -6, -6, f, f, f, f, f, f, 84, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 86 },
    { -18, -18, f, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18, f, f, f, -18, -18, -18, -18, -18, f, -18, f, -18, -18, -18, -18, -18, -18, f, f, -18, -18, -18, -18, -18, f, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18, -18 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 43, f, 41, 42 },
    { -77, -77, f, -77, -77, -77, -77, -77, -77, -77, -77, -77, -77, -77, f, f, f, -77, -77, -77, -77, -77, f, -77, f, -77, -77, -77, -77, -77, -77, f, f, -77, -77, -77, -77, -77, f, -77, -77, -77, -77, -77, -77, -77, -77, -77, -77, -77, -77 },
    { -20, -20, f, -20, -20, f, -20, -20, -20, 39, f, -20, -20, f, f, f, f, -20, -20, -20, -20, -20, f, -20, f, -20, -20, -20, -20, -20, -20, f, f, -20, -20, -20, -20, -20, f, f, -20, -20, -20, -20, -20, -20, f, -20, -20, -20, -20 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 40, f, f, 41, 42 },
    { -36, -36, -36, -36, -36, -36, -36, -36, -36, 39, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36, -36 },
    { -79, -79, f, -79, -79, -79, -79, -79, -79, -79, -79, -79, -79, -79, f, f, f, -79, -79, -79, -79, -79, f, -79, f, -79, -79, -79, -79, -79, -79, f, f, -79, -79, -79, -79, -79, f, -79, -79, -79, -79, -79, -79, -79, -79, -79, -79, -79, -79 },
    { -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35, -35 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, 72, f, 56, f, f, f, f, 70, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 45, f, 41, 42 },
    { -24, -24, f, -24, -24, f, -24, -24, -24, f, f, -24, -24, f, f, f, f, -24, 46, 48, -24, -24, f, -24, f, -24, -24, -24, -24, -24, -24, f, f, -24, -24, -24, -24, -24, f, f, -24, 50, -24, -24, -24, -24, f, -24, -24, -24, -24 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 47, f, 41, 42 },
    { -25, -25, f, -25, -25, f, -25, -25, -25, f, f, -25, -25, f, f, f, f, -25, -25, -25, -25, -25, f, -25, f, -25, -25, -25, -25, -25, -25, f, f, -25, -25, -25, -25, -25, f, f, -25, -25, -25, -25, -25, -25, f, -25, -25, -25, -25 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 49, f, 41, 42 },
    { -27, -27, f, -27, -27, f, -27, -27, -27, f, f, -27, -27, f, f, f, f, -27, -27, -27, -27, -27, f, -27, f, -27, -27, -27, -27, -27, -27, f, f, -27, -27, -27, -27, -27, f, f, -27, -27, -27, -27, -27, -27, f, -27, -27, -27, -27 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 51, f, 41, 42 },
    { -26, -26, f, -26, -26, f, -26, -26, -26, f, f, -26, -26, f, f, f, f, -26, -26, -26, -26, -26, f, -26, f, -26, -26, -26, -26, -26, -26, f, f, -26, -26, -26, -26, -26, f, f, -26, -26, -26, -26, -26, -26, f, -26, -26, -26, -26 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 53, f, 41, 42 },
    { -22, -22, f, -22, 44, f, -22, -22, -22, f, f, -22, -22, f, f, f, f, 54, 46, 48, 60, -22, f, 62, f, -22, -22, 64, 66, -22, -22, f, f, 68, -22, -22, -22, -22, f, f, -22, 50, -22, -22, 56, -22, f, -22, -22, 70, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 55, f, 41, 42 },
    { -30, -30, f, -30, 44, f, -30, -30, -30, f, f, -30, -30, f, f, f, f, -30, 46, 48, -30, -30, f, -30, f, -30, -30, -30, -30, -30, -30, f, f, -30, -30, -30, -30, -30, f, f, -30, 50, -30, -30, 56, -30, f, -30, -30, -30, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 57, f, 41, 42 },
    { -28, -28, f, -28, 44, f, -28, -28, -28, f, f, -28, -28, f, f, f, f, -28, 46, 48, -28, -28, f, -28, f, -28, -28, -28, -28, -28, -28, f, f, -28, -28, -28, -28, -28, f, f, -28, 50, -28, -28, 56, -28, f, -28, -28, -28, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 59, f, 41, 42 },
    { -23, -23, f, -23, -23, f, -23, -23, -23, f, f, -23, -23, f, f, f, f, -23, 46, 48, -23, -23, f, -23, f, -23, -23, -23, -23, -23, -23, f, f, -23, -23, -23, -23, -23, f, f, -23, 50, -23, -23, -23, -23, f, -23, -23, -23, -23 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 61, f, 41, 42 },
    { -31, -31, f, -31, 44, f, -31, -31, -31, f, f, -31, -31, f, f, f, f, -31, 46, 48, -31, -31, f, -31, f, -31, -31, -31, -31, -31, -31, f, f, -31, -31, -31, -31, -31, f, f, -31, 50, -31, -31, 56, -31, f, -31, -31, -31, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 63, f, 41, 42 },
    { -32, -32, f, -32, 44, f, -32, -32, -32, f, f, -32, -32, f, f, f, f, -32, 46, 48, -32, -32, f, -32, f, -32, -32, -32, -32, -32, -32, f, f, -32, -32, -32, -32, -32, f, f, -32, 50, -32, -32, 56, -32, f, -32, -32, -32, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 65, f, 41, 42 },
    { -33, -33, f, -33, 44, f, -33, -33, -33, f, f, -33, -33, f, f, f, f, -33, 46, 48, -33, -33, f, -33, f, -33, -33, -33, -33, -33, -33, f, f, -33, -33, -33, -33, -33, f, f, -33, 50, -33, -33, 56, -33, f, -33, -33, -33, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 67, f, 41, 42 },
    { -34, -34, f, -34, 44, f, -34, -34, -34, f, f, -34, -34, f, f, f, f, -34, 46, 48, -34, -34, f, -34, f, -34, -34, -34, -34, -34, -34, f, f, -34, -34, -34, -34, -34, f, f, -34, 50, -34, -34, 56, -34, f, -34, -34, -34, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 69, f, 41, 42 },
    { -29, -29, f, -29, 44, f, -29, -29, -29, f, f, -29, -29, f, f, f, f, -29, 46, 48, -29, -29, f, -29, f, -29, -29, -29, -29, -29, -29, f, f, -29, -29, -29, -29, -29, f, f, -29, 50, -29, -29, 56, -29, f, -29, -29, -29, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 71, f, 41, 42 },
    { -21, -21, f, -21, 44, f, -21, -21, -21, f, f, -21, -21, f, f, f, f, 54, 46, 48, 60, -21, f, 62, f, -21, -21, 64, 66, -21, -21, f, f, 68, -21, -21, -21, -21, f, f, -21, 50, -21, -21, 56, -21, f, -21, -21, -21, 58 },
    { -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50, -50 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, -67, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, 74, f, 77, f, f, f, f, f, f, f, f, 37, f, f, 38, 79, f, 41, 42 },
    { f, -68, f, f, f, f, -68, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -68, -68, -68, -68, f, f, f, f, f, -68, f, 75 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 76, f, 41, 42 },
    { -72, -72, f, f, 44, f, -72, -72, -72, f, f, 52, -72, f, f, f, f, 54, 46, 48, 60, -72, f, 62, f, -72, -72, 64, 66, -72, -72, f, f, 68, -72, -72, -72, -72, f, f, -72, 50, f, -72, 56, -72, f, f, -72, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 78 },
    { -17, -17, f, -17, -17, -17, -17, -17, -17, -17, -17, -17, -17, -17, f, f, f, -17, -17, -17, -17, -17, f, -17, f, -17, -17, -17, -17, -17, -17, f, f, -17, -17, -17, -17, -17, f, -17, -17, -17, -17, -17, -17, -17, -17, -17, -17, -17, -17 },
    { -71, -71, f, f, 44, f, -71, -71, -71, f, f, 52, -71, f, f, f, f, 54, 46, 48, 60, -71, f, 62, f, -71, -71, 64, 66, -71, -71, f, f, 68, -71, -71, -71, -71, f, f, -71, 50, f, -71, 56, -71, f, f, -71, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 81 },
    { -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49, -49 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 83 },
    { f, f, f, f, f, 35, f, f, f, f, f, f, 73, 10, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 84, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 85 },
    { -19, -19, f, -19, -19, -19, -19, -19, -19, -19, -19, -19, -19, -19, f, f, f, -19, -19, -19, -19, -19, f, -19, f, -19, -19, -19, -19, -19, -19, f, f, -19, -19, -19, -19, -19, f, -19, -19, -19, -19, -19, -19, -19, -19, -19, -19, -19, -19 },
    { -16, -16, f, -16, -16, -16, -16, -16, -16, -16, -16, -16, -16, -16, f, f, f, -16, -16, -16, -16, -16, f, -16, f, -16, -16, -16, -16, -16, -16, f, f, -16, -16, -16, -16, -16, f, -16, -16, -16, -16, -16, -16, -16, -16, -16, -16, -16, -16 },
    { -15, -15, f, -15, -15, -15, -15, -15, -15, -15, -15, -15, -15, -15, f, f, f, -15, -15, -15, -15, -15, f, -15, f, -15, -15, -15, -15, -15, -15, f, f, -15, -15, -15, -15, -15, f, -15, -15, -15, -15, -15, -15, -15, -15, -15, -15, -15, -15 },
    { -38, -38, -38, -38, -38, -38, -38, -38, -38, 39, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38, -38 },
    { -39, -39, -39, -39, -39, -39, -39, -39, -39, 39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39, -39 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, 90, f, 56, f, f, f, f, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 91 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 92, f, 41, 42 },
    { f, f, f, f, 44, f, -98, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, -98, f, -98, f, 70, 58 },
    { -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, 94, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101, -101 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 95, f, 41, 42 },
    { f, f, f, f, 44, f, -97, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, -97, f, -97, f, 70, 58 },
    { f, f, f, f, f, f, 97, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 98, f, -94, f, f, f, f, f, f, 99 },
    { f, f, -99, f, -99, -99, f, f, f, f, -99, f, -99, -99, -99, -99, -99, f, f, f, f, f, -99, f, f, f, -99, f, f, -99, f, -99, f, f, f, f, f, f, -99, f, f, f, f, f, f, f, f, -99 },
    { f, f, -100, f, -100, -100, f, f, f, f, -100, f, -100, -100, -100, -100, -100, f, f, f, f, f, -100, f, f, f, -100, f, f, -100, f, -100, f, f, f, f, f, f, -100, f, f, f, f, f, f, f, f, -100 },
    { f, f, 6, f, 7, 8, f, f, f, f, 11, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 93, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, -93, f, f, f, f, f, 32, f, 96, 100, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 101, f, 41, 42 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -95 },
    { f, f, f, f, 44, f, -96, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, -96, f, -96, f, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 103 },
    { -92, -92, f, -92, -92, -92, -92, -92, -92, -92, -92, -92, -92, -92, f, f, f, -92, -92, -92, -92, -92, f, -92, f, -92, -92, -92, -92, -92, -92, f, f, -92, -92, -92, -92, -92, f, -92, -92, -92, -92, -92, -92, -92, -92, -92, -92, -92, -92 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, 105, 56, f, f, f, f, 70, 58 },
    { -78, -78, f, -78, -78, -78, -78, -78, -78, -78, -78, -78, -78, -78, f, f, f, -78, -78, -78, -78, -78, f, -78, f, -78, -78, -78, -78, -78, -78, f, f, -78, -78, -78, -78, -78, f, -78, -78, -78, -78, -78, -78, -78, -78, -78, -78, -78, -78 },
    { -37, -37, -37, -37, -37, -37, -37, -37, -37, 39, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37, -37 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, 108, 50, f, f, 56, f, f, f, f, 70, 58 },
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, -60, f, f, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 109, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 110 },
    { -82, -82, f, f, f, f, -82, -82, -82, f, f, f, -82, f, f, f, f, f, f, f, f, -82, f, f, f, -82, -82, f, f, -82, -82, f, f, f, -82, -82, -82, -82, f, f, -82, f, f, f, f, f, f, f, -82 },
    { f, -67, 6, f, 7, 8, -67, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, -67, -67, -67, -67, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, 74, f, 112, f, f, f, f, f, f, f, f, 37, f, f, 38, 79, f, 41, 42 },
    { f, -44, f, f, f, f, 113, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -44, -44, -44, -44 },
    { f, -45, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -45, -45, -45, -45 },
    { -60, -60, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 115, f, f, f, f, f, f, f, f, f, 4 },
    { f, 116 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 117, f, 41, 42 },
    { -83, -83, f, f, 44, f, -83, -83, -83, f, f, 52, -83, f, f, f, f, 54, 46, 48, 60, -83, f, 62, f, -83, -83, 64, 66, -83, -83, f, f, 68, -83, -83, -83, -83, f, f, -83, 50, f, f, 56, f, f, f, -83, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, 119, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 124, f, f, f, f, f, f, f, 125 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 120 },
    { f, f, f, f, f, f, f, f, f, f, f, f, 18, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 121 },
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, -60, f, f, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 122, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 123 },
    { -88, -88, f, f, f, f, -88, -88, -88, f, f, f, -88, f, f, f, f, f, f, f, f, -88, f, f, f, -88, -88, f, f, -88, -88, f, f, f, -88, -88, -88, -88, f, f, -88, f, f, f, f, f, f, f, -88 },
    { -75, -75, f, f, f, f, -75, -75, -75, f, f, f, -75, f, f, f, f, f, f, f, f, -75, f, f, f, -75, -75, f, f, -75, -75, f, -75, f, -75, -75, -75, -75, f, f, -75, f, f, f, f, -75, f, f, -75 },
    { -55, -55, f, f, f, f, -55, -55, -55, f, f, f, -55, f, f, f, f, f, f, f, f, -55, f, f, f, -55, -55, f, f, -55, -55, f, 126, f, -55, -55, -55, -55, f, f, -55, f, f, f, f, 128, f, f, -55, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 130 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, 127, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 79, f, 41, 42 },
    { -56, -56, f, f, f, f, -56, -56, -56, f, f, f, -56, f, f, f, f, f, f, f, f, -56, f, f, f, -56, -56, f, f, -56, -56, f, f, f, -56, -56, -56, -56, f, f, -56, f, f, f, f, 75, f, f, -56 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 129 },
    { -76, -76, f, f, f, f, -76, -76, -76, f, f, f, -76, f, f, f, f, f, f, f, f, -76, f, f, -76, -76, -76, f, f, -76, -76, f, -76, f, -76, -76, -76, -76, f, f, -76, f, f, f, f, -76, f, f, -76 },
    { -87, -87, f, f, f, f, -87, -87, -87, f, f, f, -87, f, f, f, f, f, f, f, f, -87, f, f, f, -87, -87, f, f, -87, -87, f, f, f, -87, -87, -87, -87, f, f, -87, f, f, f, f, f, f, f, -87 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 132, f, 41, 42 },
    { f, f, f, 133, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, f, f, f, f, 70, 58 },
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, -60, -60, -60, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 134, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -63, -63, -63, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 135 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -51, 136, 140, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 142 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 137, f, 41, 42 },
    { f, f, f, 138, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, f, f, f, f, 70, 58 },
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, -60, -60, -60, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 139, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -64, -64, -64 },
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, -60, f, f, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 141, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -52 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 143 },
    { -84, -84, f, f, f, f, -84, -84, -84, f, f, f, -84, f, f, f, f, f, f, f, f, -84, f, f, f, -84, -84, f, f, -84, -84, f, f, f, -84, -84, -84, -84, f, f, -84, f, f, f, f, f, f, f, -84 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 145, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 146 },
    { f, f, f, f, f, f, f, f, f, f, f, f, -40, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -40, f, f, f, f, f, f, -40 },
    { f, f, f, f, f, f, f, f, f, f, f, f, 18, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 147, f, f, f, f, f, f, 149, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 151 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 148 },
    { f, f, f, f, f, f, f, f, f, f, f, f, -41, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -41, f, f, f, f, f, f, -41 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 150 },
    { f, f, f, f, f, f, f, f, f, f, f, f, -42, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -42, f, f, f, f, f, f, -42 },
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, -60, f, f, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 152, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 153 },
    { -89, -89, f, f, f, f, -89, -89, -89, f, f, f, -89, f, f, f, f, f, f, f, f, -89, f, f, f, -89, -89, f, f, -89, -89, f, f, f, -89, -89, -89, -89, f, f, -89, f, f, f, f, f, f, f, -89 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 155, f, f, f, f, f, f, f, 166 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -75, f, f, f, f, f, f, f, 156, f, f, f, f, f, f, f, f, f, f, f, f, -75 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 157, f, 41, 42 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, f, 50, f, f, 56, 158, f, f, f, 70, 58 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 159, f, 41, 42 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, -53, 50, f, f, 56, 160, f, f, f, 70, 58, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 162 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, f, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 161, f, 41, 42 },
    { f, f, f, f, 44, f, f, f, f, f, f, 52, f, f, f, f, f, 54, 46, 48, 60, f, f, 62, f, f, f, 64, 66, f, f, f, f, 68, f, f, f, f, f, f, -54, 50, f, f, 56, f, f, f, f, 70, 58 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 163 },
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, -60, f, f, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 164, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 165 },
    { -85, -85, f, f, f, f, -85, -85, -85, f, f, f, -85, f, f, f, f, f, f, f, f, -85, f, f, f, -85, -85, f, f, -85, -85, f, f, f, -85, -85, -85, -85, f, f, -85, f, f, f, f, f, f, f, -85 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 167, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 128 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, 168, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 79, f, 41, 42 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 169, f, f, f, f, 75 },
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, -60, f, f, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 170, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 171 },
    { -86, -86, f, f, f, f, -86, -86, -86, f, f, f, -86, f, f, f, f, f, f, f, f, -86, f, f, f, -86, -86, f, f, -86, -86, f, f, f, -86, -86, -86, -86, f, f, -86, f, f, f, f, f, f, f, -86 },
    { -60, f, f, f, f, f, f, -60, -60, f, f, f, -60, f, f, f, f, f, f, f, f, -60, f, f, f, -60, -60, f, f, -60, -60, f, f, f, f, -60, f, f, f, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, f, f, f, f, f, 173, f, f, f, f, f, f, f, f, f, 4 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 174 },
    { -80, -80, f, f, f, f, -80, -80, -80, f, f, f, -80, f, f, f, f, f, f, f, f, -80, f, f, f, -80, -80, f, f, -80, -80, f, f, f, -80, -80, -80, -80, f, f, -80, f, f, f, f, f, f, f, -80 },
    { f, -46, f, f, f, f, 176, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -46, -46, -46, -46 },
    { f, -47, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -47, -47, -47, -47 },
    { -61, -61, f, f, f, f, 178, -61, -61, f, f, f, -61, f, f, f, f, f, f, f, f, -61, f, f, f, -61, -61, f, f, -61, -61, f, f, f, -61, -61, -61, -61, f, f, -61, f, f, f, f, f, f, f, -61 },
    { -62, -62, f, f, f, f, f, -62, -62, f, f, f, -62, f, f, f, f, f, f, f, f, -62, f, f, f, -62, -62, f, f, -62, -62, f, f, f, -62, -62, -62, -62, f, f, -62, f, f, f, f, f, f, f, -62 },
    { f, f, f, f, f, 35, f, f, f, f, 36, f, 73, 10, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 80, f, f, f, f, f, f, 82, f, f, f, f, f, f, f, f, f, f, 84, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 86 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 181, f, f, f, f, f, f, f, f, f, f, f, f, 183 },
    { f, f, 6, f, 7, 8, f, f, f, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, f, 15, f, f, f, 16, f, f, 17, f, 30, f, f, f, f, f, f, 31, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, 33, f, f, 34, f, f, 182, f, f, f, f, f, f, f, f, f, f, 37, f, f, 38, 79, f, 41, 42 },
    { -81, -81, f, f, f, f, -81, -81, -81, f, f, f, -81, f, f, f, f, f, f, f, f, -81, f, f, f, -81, -81, f, f, -81, -81, f, f, f, -81, -81, -81, -81, f, f, -81, f, f, f, f, 75, f, f, -81 },
    { f, f, f, f, f, f, f, f, f, f, f, f, 9, f, f, f, f, f, f, f, f, f, f, f, f, f, 16, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 32, f, f, f, f, f, f, 179, f, f, f, f, f, f, f, f, f, f, f, f, f, 184, f, f, f, f, f, 41 },
    { f, f, f, f, f, -77, f, f, f, f, -77, f, -77, -77, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -74, f, f, f, f, f, f, -77, f, f, f, f, f, -74, -77 },
    { f, f, f, f, f, -77, f, f, f, f, -77, f, -77, -77, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -73, f, f, f, f, f, f, -77, f, f, f, f, f, -73, -77 },
    { -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91, -91 },
    { -90, -90, f, f, f, -79, -90, -90, -90, f, -79, f, -79, -79, f, f, f, f, f, f, f, -90, f, f, f, -90, -90, f, f, -90, -90, f, f, f, -90, -90, -90, -90, f, -79, -90, f, f, f, f, f, -79, f, -90 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -1 },
    { -60, f, 6, f, 7, 8, f, -60, -60, f, f, f, 9, 10, 12, 13, 14, f, f, f, f, -60, 15, f, f, -60, 16, f, f, 17, -60, 30, f, f, -60, f, f, f, 31, f, -60, f, f, f, f, f, f, f, -60, f, f, f, f, 32, f, f, f, 33, 190, f, 34, f, f, 192, f, f, f, f, 4, 194, f, f, f, f, 37, f, f, 38, 79, f, 41, 42 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 191 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -59 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, 193, f, f, f, f, f, f, f, f, f, f, 75 },
    { f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, f, -58 },
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
            elseif n == 0 then
                local end_pos = stack[stack_n + 2]
                state = stack[stack_n]
                action = transitions[state][next_state]
                stack_n = stack_n + 3
                stack[stack_n], stack[stack_n + 1], stack[stack_n + 2] = action, end_pos, end_pos
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
    repl_exprs = 189, --[[- The repl_exprs starting state. ]]
    program = 1, --[[- The program starting state. ]]
}
