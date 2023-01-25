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
        return errors.unclosed_brackets(lp.s, lp.e, token.v, token.s, token.e)
    end,
    function(context, stack, stack_n, regs, token)
        local lp = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 39
        return errors.unclosed_brackets(lp.s, lp.e, token.v, token.s, token.e)
    end,
    function(context, stack, stack_n, regs, token)
        local lp = { s = stack[regs[2] + 1], e = stack[regs[2] + 2] }
        -- parse_errors.mlyl, line 41
        return errors.unclosed_brackets(lp.s, lp.e, token.v, token.s, token.e)
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
        return errors.expected_function_args(token.v, token.s, token.e)
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 77
        if token.v == tokens.END then
      return errors.unexpected_end(token.s, token.e)
    elseif token ~= tokens.EOF then
        return errors.expected_statement(token.v, token.s, token.e)
    end
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 87
        return errors.expected_expression(token.v, token.s, token.e)
    end,
    function(context, stack, stack_n, regs, token)
        -- parse_errors.mlyl, line 91
        return errors.expected_var(token.v, token.s, token.e)
    end,
}
local error_transitions = {
    { [1] = 49, [4] = 50, [5] = 54, [6] = 28, [7] = 54, [8] = 28, [9] = 54, [10] = 46, [11] = 54, [12] = 28, [13] = 54, [14] = 28, [15] = 54, [16] = 41, [17] = 48, [18] = 45, [19] = 2, [20] = 2, [21] = 3, [23] = 4, [24] = 56, [25] = 13, [26] = 3, [27] = 75, [28] = 62, [29] = 29, [30] = 28, [31] = 28, [32] = 61, [33] = 28, [34] = 30, [35] = 31, [36] = 54, [37] = 30, [38] = 32, [39] = 54, [40] = 33, [41] = 30, [42] = 34, [43] = 78, [44] = 54, [45] = 69, [46] = 54, [47] = 70, [48] = 54, [49] = 70, [50] = 54, [51] = 70, [52] = 54, [53] = 71, [54] = 54, [55] = 72, [56] = 54, [57] = 69, [58] = 54, [59] = 69, [60] = 54, [61] = 72, [62] = 54, [63] = 72, [64] = 54, [65] = 72, [66] = 54, [67] = 72, [68] = 54, [69] = 72, [70] = 54, [71] = 73, [72] = 42, [73] = 45, [74] = 27, [75] = 54, [76] = 67, [77] = 56, [78] = 35, [79] = 68, [81] = 43, [84] = 31, [85] = 36, [86] = 37, [87] = 38, [88] = 38, [89] = 78, [91] = 54, [92] = 64, [93] = 44, [94] = 54, [95] = 65, [96] = 5, [97] = 6, [98] = 6, [99] = 7, [100] = 8, [101] = 76, [102] = 57, [103] = 39, [104] = 77, [105] = 40, [106] = 38, [107] = 63, [108] = 75, [109] = 62, [110] = 14, [111] = 15, [112] = 16, [113] = 17, [114] = 49, [116] = 54, [117] = 66, [119] = 55, [120] = 74, [121] = 75, [122] = 62, [123] = 18, [124] = 59, [125] = 19, [126] = 54, [127] = 20, [128] = 55, [129] = 60, [130] = 21, [131] = 54, [132] = 63, [133] = 75, [134] = 62, [135] = 62, [136] = 54, [137] = 63, [138] = 51, [139] = 9, [140] = 52, [141] = 10, [142] = 62, [143] = 22, [144] = 55, [145] = 58, [146] = 48, [151] = 75, [152] = 62, [153] = 14, [154] = 55, [155] = 58, [156] = 54, [157] = 63, [158] = 54, [159] = 63, [160] = 54, [161] = 63, [163] = 75, [164] = 62, [165] = 23, [167] = 54, [169] = 75, [170] = 62, [171] = 22, [172] = 79, [173] = 62, [174] = 21, [175] = 15, [176] = 16, [177] = 24, [178] = 25, [180] = 47, [181] = 54, [182] = 21, [183] = 55, [184] = 11, [185] = 12, [186] = 53, [187] = 26, [189] = 49 },
    { [18] = 45, [22] = 80 }, {}, {}, { [10] = 46, [99] = 7 }, {}, {}, {},
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    { [73] = 45, [111] = 15 },
    { [5] = 89, [7] = 28, [9] = 103, [10] = 105, [11] = 104, [13] = 28, [15] = 28, [36] = 104, [39] = 38, [44] = 97, [46] = 98, [48] = 98, [50] = 98, [52] = 99, [54] = 100, [56] = 97, [58] = 97, [60] = 100, [62] = 100, [64] = 100, [66] = 100, [68] = 100, [70] = 101, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [189] = 89 },
    {},
    { [5] = 89, [7] = 30, [9] = 103, [10] = 105, [11] = 104, [13] = 30, [15] = 30, [36] = 104, [39] = 38, [44] = 97, [46] = 98, [48] = 98, [50] = 98, [52] = 99, [54] = 100, [56] = 97, [58] = 97, [60] = 100, [62] = 100, [64] = 100, [66] = 100, [68] = 100, [70] = 101, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [189] = 89 },
    { [34] = 108, [83] = 107, [179] = 108 },
    { [5] = 89, [9] = 103, [10] = 105, [11] = 104, [36] = 104, [44] = 97, [46] = 98, [48] = 98, [50] = 98, [52] = 99, [54] = 100, [56] = 97, [58] = 97, [60] = 100, [62] = 100, [64] = 100, [66] = 100, [68] = 100, [70] = 101, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [189] = 89 },
    {},
    { [5] = 89, [7] = 34, [9] = 103, [10] = 105, [11] = 104, [13] = 34, [15] = 34, [36] = 104, [39] = 38, [44] = 97, [46] = 98, [48] = 98, [50] = 98, [52] = 99, [54] = 100, [56] = 97, [58] = 97, [60] = 100, [62] = 100, [64] = 100, [66] = 100, [68] = 100, [70] = 101, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [189] = 89 },
    {}, {}, {}, {}, {}, {},
    { [4] = 47, [5] = 89, [7] = 30, [9] = 103, [11] = 104, [13] = 30, [15] = 30, [18] = 45, [22] = 80, [36] = 104, [39] = 38, [44] = 97, [46] = 98, [48] = 98, [50] = 98, [52] = 99, [54] = 100, [56] = 97, [58] = 97, [60] = 100, [62] = 100, [64] = 100, [66] = 100, [68] = 100, [70] = 101, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [111] = 92, [116] = 93, [118] = 26, [126] = 94, [128] = 112, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [183] = 83, [189] = 89 },
    {}, {}, { [10] = 105, [99] = 102 }, {}, {}, {}, {}, {},
    { [27] = 62, [108] = 62, [121] = 62, [133] = 62, [138] = 81, [140] = 82, [151] = 62, [163] = 62, [169] = 62, [172] = 114 },
    {}, {}, {}, {}, {}, {}, {}, { [17] = 117, [119] = 117, [144] = 117 }, {},
    {},
    { [4] = 47, [5] = 89, [7] = 30, [9] = 103, [10] = 105, [11] = 104, [13] = 30, [15] = 30, [36] = 104, [39] = 38, [44] = 97, [46] = 98, [48] = 98, [50] = 98, [52] = 99, [54] = 100, [56] = 97, [58] = 97, [60] = 100, [62] = 100, [64] = 100, [66] = 100, [68] = 100, [70] = 101, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [183] = 83, [189] = 89 },
    { [1] = 62, [2] = 62, [3] = 62, [4] = 62, [5] = 114, [6] = 62, [7] = 62, [8] = 62, [9] = 62, [10] = 62, [11] = 62, [12] = 62, [13] = 62, [14] = 62, [15] = 62, [16] = 62, [17] = 62, [18] = 62, [19] = 62, [20] = 62, [21] = 62, [22] = 62, [23] = 62, [24] = 62, [25] = 62, [26] = 62, [27] = 62, [28] = 62, [29] = 62, [30] = 62, [31] = 62, [32] = 62, [33] = 62, [34] = 62, [35] = 62, [36] = 62, [37] = 62, [38] = 62, [39] = 62, [40] = 62, [41] = 62, [42] = 62, [43] = 62, [44] = 62, [45] = 62, [46] = 62, [47] = 62, [48] = 62, [49] = 62, [50] = 62, [51] = 62, [52] = 62, [53] = 62, [54] = 62, [55] = 62, [56] = 62, [57] = 62, [58] = 62, [59] = 62, [60] = 62, [61] = 62, [62] = 62, [63] = 62, [64] = 62, [65] = 62, [66] = 62, [67] = 62, [68] = 62, [69] = 62, [70] = 62, [71] = 62, [72] = 62, [73] = 62, [74] = 62, [75] = 62, [76] = 62, [77] = 62, [78] = 62, [79] = 62, [80] = 62, [81] = 62, [82] = 62, [83] = 62, [84] = 62, [85] = 62, [86] = 62, [87] = 62, [88] = 62, [89] = 62, [90] = 62, [91] = 62, [92] = 62, [93] = 62, [94] = 62, [95] = 62, [96] = 62, [97] = 62, [98] = 62, [99] = 62, [100] = 62, [101] = 62, [102] = 62, [103] = 62, [104] = 62, [105] = 62, [106] = 62, [107] = 62, [108] = 62, [109] = 62, [110] = 62, [111] = 62, [112] = 62, [113] = 62, [114] = 62, [115] = 62, [116] = 62, [117] = 62, [118] = 62, [119] = 62, [120] = 62, [121] = 62, [122] = 62, [123] = 62, [124] = 62, [125] = 62, [126] = 62, [127] = 62, [128] = 62, [129] = 62, [130] = 62, [131] = 114, [132] = 62, [133] = 62, [134] = 62, [135] = 62, [136] = 62, [137] = 62, [138] = 62, [139] = 62, [140] = 62, [141] = 62, [142] = 62, [143] = 62, [144] = 62, [145] = 62, [146] = 62, [147] = 62, [148] = 62, [149] = 62, [150] = 62, [151] = 62, [152] = 62, [153] = 62, [154] = 114, [155] = 62, [156] = 62, [157] = 62, [158] = 62, [159] = 62, [160] = 62, [161] = 62, [162] = 62, [163] = 62, [164] = 62, [165] = 62, [166] = 62, [167] = 62, [168] = 62, [169] = 62, [170] = 62, [171] = 62, [172] = 114, [173] = 62, [174] = 62, [175] = 62, [176] = 62, [177] = 62, [178] = 62, [179] = 62, [180] = 62, [181] = 62, [182] = 62, [183] = 62, [184] = 62, [185] = 62, [186] = 62, [187] = 62, [188] = 62, [189] = 62, [190] = 62, [191] = 62, [192] = 62, [193] = 62, [194] = 62 },
    { [131] = 118, [136] = 118 }, {}, {}, {}, {},
    { [73] = 45, [111] = 15, [126] = 86, [181] = 19 }, {}, {}, {}, {}, {},
    {},
    { [1] = 62, [2] = 62, [3] = 62, [4] = 62, [5] = 114, [6] = 62, [7] = 62, [8] = 62, [9] = 62, [10] = 62, [11] = 62, [12] = 62, [13] = 62, [14] = 62, [15] = 62, [16] = 62, [17] = 62, [18] = 62, [19] = 62, [20] = 62, [21] = 62, [22] = 62, [23] = 62, [24] = 62, [25] = 62, [26] = 62, [27] = 62, [28] = 62, [29] = 62, [30] = 62, [31] = 62, [32] = 62, [33] = 62, [34] = 62, [35] = 62, [36] = 62, [37] = 62, [38] = 62, [39] = 62, [40] = 62, [41] = 62, [42] = 62, [43] = 62, [44] = 62, [45] = 62, [46] = 62, [47] = 62, [48] = 62, [49] = 62, [50] = 62, [51] = 62, [52] = 62, [53] = 62, [54] = 62, [55] = 62, [56] = 62, [57] = 62, [58] = 62, [59] = 62, [60] = 62, [61] = 62, [62] = 62, [63] = 62, [64] = 62, [65] = 62, [66] = 62, [67] = 62, [68] = 62, [69] = 62, [70] = 62, [71] = 62, [72] = 62, [73] = 62, [74] = 62, [75] = 62, [76] = 62, [77] = 62, [78] = 62, [79] = 62, [80] = 62, [81] = 62, [82] = 62, [83] = 62, [84] = 62, [85] = 62, [86] = 62, [87] = 62, [88] = 62, [89] = 62, [90] = 62, [91] = 62, [92] = 62, [93] = 62, [94] = 62, [95] = 62, [96] = 62, [97] = 62, [98] = 62, [99] = 62, [100] = 62, [101] = 62, [102] = 62, [103] = 62, [104] = 62, [105] = 62, [106] = 62, [107] = 62, [108] = 62, [109] = 62, [110] = 62, [111] = 62, [112] = 62, [113] = 62, [114] = 62, [115] = 62, [116] = 62, [117] = 62, [118] = 62, [119] = 62, [120] = 62, [121] = 62, [122] = 62, [123] = 62, [124] = 62, [125] = 62, [126] = 62, [127] = 62, [128] = 62, [129] = 62, [130] = 62, [131] = 114, [132] = 62, [133] = 62, [134] = 62, [135] = 62, [136] = 62, [137] = 62, [138] = 62, [139] = 62, [140] = 62, [141] = 62, [142] = 62, [143] = 62, [144] = 62, [145] = 62, [146] = 62, [147] = 62, [148] = 62, [149] = 62, [150] = 62, [151] = 62, [152] = 62, [153] = 62, [154] = 114, [155] = 62, [156] = 62, [157] = 62, [158] = 62, [159] = 62, [160] = 62, [161] = 62, [162] = 62, [163] = 62, [164] = 62, [165] = 62, [166] = 62, [167] = 62, [168] = 62, [169] = 62, [170] = 62, [171] = 62, [172] = 114, [173] = 62, [174] = 62, [175] = 62, [176] = 62, [177] = 62, [178] = 62, [179] = 62, [180] = 62, [181] = 62, [182] = 62, [183] = 62, [184] = 62, [185] = 62, [186] = 62, [187] = 62, [188] = 62, [189] = 62, [190] = 62, [191] = 62, [192] = 62, [193] = 62, [194] = 62 },
    { [10] = 46, [99] = 7 }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    { [1] = 49, [27] = 75, [108] = 75, [114] = 49, [121] = 75, [133] = 75, [138] = 51, [140] = 52, [151] = 75, [163] = 75, [169] = 75, [172] = 79, [189] = 49 },
    {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    {},
    { [4] = 88, [5] = 89, [7] = 30, [9] = 103, [10] = 105, [11] = 104, [13] = 30, [15] = 30, [36] = 104, [39] = 38, [44] = 97, [46] = 98, [48] = 98, [50] = 98, [52] = 99, [54] = 100, [56] = 97, [58] = 97, [60] = 100, [62] = 100, [64] = 100, [66] = 100, [68] = 100, [70] = 101, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [189] = 89 },
    {}, {}, {}, {}, {}, {}, {},
    { [27] = 62, [108] = 62, [121] = 62, [133] = 62, [138] = 81, [140] = 82, [151] = 62, [163] = 62, [169] = 62, [172] = 114 },
    { [118] = 138 }, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}, {},
    { [73] = 45, [111] = 15, [126] = 86, [181] = 19 },
    { [5] = 89, [9] = 103, [10] = 105, [11] = 104, [36] = 104, [52] = 99, [54] = 100, [56] = 97, [60] = 100, [62] = 100, [64] = 100, [66] = 100, [68] = 100, [70] = 101, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [189] = 89 },
    { [5] = 89, [9] = 103, [10] = 105, [11] = 104, [36] = 104, [44] = 97, [52] = 99, [54] = 100, [56] = 97, [58] = 97, [60] = 100, [62] = 100, [64] = 100, [66] = 100, [68] = 100, [70] = 101, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [189] = 89 },
    { [5] = 89, [9] = 103, [10] = 105, [11] = 104, [36] = 104, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [189] = 89 },
    { [5] = 89, [9] = 103, [10] = 105, [11] = 104, [36] = 104, [52] = 99, [70] = 101, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [189] = 89 },
    { [5] = 89, [9] = 103, [10] = 105, [11] = 104, [36] = 104, [52] = 99, [73] = 103, [75] = 96, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [181] = 95, [189] = 89 },
    {},
    { [5] = 89, [7] = 28, [9] = 103, [10] = 105, [11] = 104, [13] = 28, [15] = 28, [34] = 108, [36] = 104, [39] = 38, [44] = 97, [46] = 98, [48] = 98, [50] = 98, [52] = 99, [54] = 100, [56] = 97, [58] = 97, [60] = 100, [62] = 100, [64] = 100, [66] = 100, [68] = 100, [70] = 101, [73] = 103, [75] = 96, [83] = 107, [91] = 90, [94] = 91, [99] = 102, [111] = 92, [116] = 93, [126] = 94, [131] = 89, [136] = 89, [156] = 89, [158] = 89, [160] = 89, [167] = 89, [179] = 108, [181] = 95, [189] = 89 },
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
    error_jump(80), -- State 4
    function(stack, idx, regs, out) -- State 5
        local top = stack[idx]
        if top == 10 then regs[1] = idx end
        local next = error_transitions[5][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(5), -- State 6
    error_jump(5), -- State 7
    error_jump(7), -- State 8
    error_jump(81), -- State 9
    error_jump(82), -- State 10
    error_jump(83), -- State 11
    error_jump(47), -- State 12
    error_jump(84), -- State 13
    error_jump(85), -- State 14
    error_jump(50), -- State 15
    error_jump(15), -- State 16
    error_jump(16), -- State 17
    error_jump(14), -- State 18
    error_jump(26), -- State 19
    error_jump(86), -- State 20
    error_jump(19), -- State 21
    error_jump(18), -- State 22
    error_jump(87), -- State 23
    error_jump(88), -- State 24
    error_jump(24), -- State 25
    error_jump(88), -- State 26
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
    error_jump(106), -- State 29
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
    error_jump(109), -- State 35
    error_jump(107), -- State 36
    error_jump(108), -- State 37
    error_jump(34), -- State 38
    error_jump(110), -- State 39
    error_jump(111), -- State 40
    function(stack, idx, regs, out) -- State 41
        local top = stack[idx]
        if top == 9 or top == 11 or top == 18 or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[41][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(43), -- State 42
    error_jump(113), -- State 43
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
        out[#out + 1] = { 11 }
    end,
    function(stack, idx, regs, out) -- State 50
        out[#out + 1] = { 11 }
        local top = stack[idx]
        if top == 172 then regs[1] = idx end
        local next = error_transitions[50][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 51
        out[#out + 1] = { 11 }
        return error_states[115](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 52
        out[#out + 1] = { 11 }
        return error_states[62](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 53
        out[#out + 1] = { 11 }
        return error_states[116](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 54
        out[#out + 1] = { 12 }
    end,
    function(stack, idx, regs, out) -- State 55
        out[#out + 1] = { 13 }
    end,
    function(stack, idx, regs, out) -- State 56
        regs[1] = idx
        return error_states[45](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 57
        regs[1] = idx
        return error_states[46](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 58
        local top = stack[idx]
        local next = error_transitions[58][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(26), -- State 59
    error_jump(112), -- State 60
    function(stack, idx, regs, out) -- State 61
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[61][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 62
        local top = stack[idx]
        if top == 5 or top == 131 or top == 154 or top == 172 then
            regs[1] = idx
        end
        local next = error_transitions[62][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 63
        out[#out + 1] = { 2 }
        local top = stack[idx]
        if top == 131 or top == 136 then regs[1] = idx end
        local next = error_transitions[63][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 64
        out[#out + 1] = { 2 }
        return error_states[119](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 65
        out[#out + 1] = { 2 }
        return error_states[7](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 66
        out[#out + 1] = { 2 }
        return error_states[21](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 67
        out[#out + 1] = { 2 }
        return error_states[120](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 68
        out[#out + 1] = { 2 }
        local top = stack[idx]
        if top == 73 then regs[1] = idx end
        local next = error_transitions[68][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 69
        out[#out + 1] = { 2 }
        return error_states[121](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 70
        out[#out + 1] = { 2 }
        return error_states[122](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 71
        out[#out + 1] = { 2 }
        return error_states[123](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 72
        out[#out + 1] = { 2 }
        return error_states[124](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 73
        out[#out + 1] = { 2 }
        return error_states[125](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 74
        out[#out + 1] = { 10 }
        return error_states[117](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 75
        out[#out + 1] = { 11 }
        local top = stack[idx]
        if top == 5 or top == 131 or top == 154 or top == 172 then
            regs[1] = idx
        end
        local next = error_transitions[75][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 76
        out[#out + 1] = { 2 }
        out[#out + 1] = { 1 }
        local top = stack[idx]
        if top == 10 then regs[1] = idx end
        local next = error_transitions[76][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 77
        out[#out + 1] = { 2 }
        regs[1] = idx
        return error_states[45](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 78
        out[#out + 1] = { 2 }
        regs[1] = idx
        return error_states[126](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 79
        out[#out + 1] = { 11 }
        out[#out + 1] = { 9, regs[1] }
        return error_states[62](stack, idx - 3, regs, out)
    end,
    error_jump(3), -- State 80
    error_jump(115), -- State 81
    error_jump(62), -- State 82
    error_jump(12), -- State 83
    error_jump(127), -- State 84
    error_jump(21), -- State 85
    error_jump(19), -- State 86
    error_jump(128), -- State 87
    function(stack, idx, regs, out) -- State 88
        local top = stack[idx]
        if top == 172 then regs[1] = idx end
        local next = error_transitions[88][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 89
        out[#out + 1] = { 2 }
    end,
    function(stack, idx, regs, out) -- State 90
        out[#out + 1] = { 2 }
        return error_states[8](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 91
        out[#out + 1] = { 2 }
        return error_states[5](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 92
        out[#out + 1] = { 2 }
        return error_states[50](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 93
        out[#out + 1] = { 2 }
        return error_states[19](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 94
        out[#out + 1] = { 2 }
        return error_states[19](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 95
        out[#out + 1] = { 2 }
        return error_states[26](stack, idx - 3, regs, out)
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
        return error_states[132](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 100
        out[#out + 1] = { 2 }
        return error_states[133](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 101
        out[#out + 1] = { 2 }
        return error_states[134](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 102
        out[#out + 1] = { 2 }
        out[#out + 1] = { 1 }
        return error_states[5](stack, idx - 3, regs, out)
    end,
    function(stack, idx, regs, out) -- State 103
        out[#out + 1] = { 3, regs[1] }
        out[#out + 1] = { 2 }
    end,
    function(stack, idx, regs, out) -- State 104
        out[#out + 1] = { 5, regs[1] }
        out[#out + 1] = { 2 }
    end,
    function(stack, idx, regs, out) -- State 105
        out[#out + 1] = { 4, regs[1] }
        out[#out + 1] = { 2 }
        out[#out + 1] = { 1 }
    end,
    error_jump(135), -- State 106
    error_jump(37), -- State 107
    function(stack, idx, regs, out) -- State 108
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[108][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(31), -- State 109
    error_jump(136), -- State 110
    error_jump(30), -- State 111
    error_jump(59), -- State 112
    error_jump(61), -- State 113
    function(stack, idx, regs, out) -- State 114
        out[#out + 1] = { 9, regs[1] }
        return error_states[62](stack, idx - 3, regs, out)
    end,
    error_jump(137), -- State 115
    function(stack, idx, regs, out) -- State 116
        local top = stack[idx]
        if top == 172 then regs[1] = idx end
        local next = error_transitions[116][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 117
        local top = stack[idx]
        if top == 118 then regs[1] = idx end
        local next = error_transitions[117][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 118
        out[#out + 1] = { 8, regs[1] }
    end,
    error_jump(8), -- State 119
    error_jump(129), -- State 120
    error_jump(130), -- State 121
    error_jump(131), -- State 122
    error_jump(132), -- State 123
    error_jump(133), -- State 124
    error_jump(134), -- State 125
    function(stack, idx, regs, out) -- State 126
        out[#out + 1] = { 5, regs[1] }
    end,
    error_jump(75), -- State 127
    error_jump(22), -- State 128
    function(stack, idx, regs, out) -- State 129
        local top = stack[idx]
        if top == 73 then regs[1] = idx end
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
    function(stack, idx, regs, out) -- State 132
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[132][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 133
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[133][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    function(stack, idx, regs, out) -- State 134
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[134][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(28), -- State 135
    function(stack, idx, regs, out) -- State 136
        local top = stack[idx]
        if (top >= 9 and top <= 11) or top == 36 or top == 73 then
            regs[1] = idx
        end
        local next = error_transitions[136][top]
        if next then return error_states[next](stack, idx - 3, regs, out) end
    end,
    error_jump(139), -- State 137
    function(stack, idx, regs, out) -- State 138
        out[#out + 1] = { 6, regs[1] }
    end,
    error_jump(62), -- State 139
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
