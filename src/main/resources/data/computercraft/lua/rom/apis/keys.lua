
-- Minecraft key code bindings
-- See http://www.minecraftwiki.net/wiki/Key_codes for more info

local tKeys = {
    nil,	 	"one", 		"two", 		"three", 	"four",			-- 1
    "five", 	"six", 		"seven", 	"eight", 	"nine",			-- 6
    "zero", 	"minus", 	"equals", 	"backspace","tab",			-- 11
    "q", 		"w", 		"e", 		"r",		"t",			-- 16
    "y",		"u",		"i",		"o",		"p",			-- 21
    "leftBracket","rightBracket","enter","leftCtrl","a",			-- 26
    "s",		"d",		"f",		"g",		"h",			-- 31
    "j",		"k",		"l",		"semiColon","apostrophe",	-- 36
    "grave",	"leftShift","backslash","z",		"x",			-- 41
    "c",		"v",		"b",		"n",		"m",			-- 46
    "comma",	"period",	"slash",	"rightShift","multiply",	-- 51
    "leftAlt",	"space",	"capsLock",	"f1",		"f2",			-- 56
    "f3",		"f4",		"f5",		"f6",		"f7",			-- 61
    "f8",		"f9",		"f10",		"numLock",	"scrollLock",	-- 66
    "numPad7",	"numPad8",	"numPad9",	"numPadSubtract","numPad4",	-- 71
    "numPad5",	"numPad6",	"numPadAdd","numPad1",	"numPad2",		-- 76
    "numPad3",	"numPad0",	"numPadDecimal",nil,	nil,			-- 81
    nil,	 	"f11",		"f12",		nil,		nil,			-- 86
    nil,		nil,		nil,		nil,		nil,			-- 91
    nil,		nil,		nil,		nil,		"f13",			-- 96
    "f14",		"f15",		nil,		nil,		nil,			-- 101
    nil,		nil,		nil,		nil,		nil,			-- 106
    nil,		"kana",		nil,		nil,		nil,			-- 111
    nil,		nil,		nil,		nil,		nil,			-- 116
    "convert",	nil,		"noconvert",nil,		"yen",			-- 121
    nil,		nil,		nil,		nil,		nil,			-- 126
    nil,		nil,		nil,		nil,		nil,			-- 131
    nil,		nil,		nil,		nil,		nil,			-- 136
    "numPadEquals",nil,		nil,		"circumflex","at",			-- 141
    "colon",	"underscore","kanji",	"stop",		"ax",			-- 146
    nil,		nil,		nil,		nil,		nil,			-- 151
    "numPadEnter","rightCtrl",nil,      nil,		nil,			-- 156
    nil,		nil,		nil,		nil,		nil,			-- 161
    nil,		nil,		nil,		nil,		nil,			-- 166
    nil,		nil,		nil,		nil,		nil,			-- 171
    nil,		nil,		nil,		"numPadComma",nil,			-- 176
    "numPadDivide",nil,		nil,		"rightAlt",	nil,			-- 181
    nil,		nil,		nil,		nil,		nil,			-- 186
    nil,		nil,		nil,		nil,		nil,			-- 191
    nil,		"pause",	nil,		"home",		"up",			-- 196
    "pageUp",	nil,		"left",		nil,		"right",		-- 201
    nil,		"end",		"down",		"pageDown",	"insert",		-- 206
    "delete"														-- 211
}

local keys = _ENV
for nKey, sKey in pairs( tKeys ) do
    keys[sKey] = nKey
end
keys["return"] = keys.enter
--backwards compatibility to earlier, typo prone, versions
keys.scollLock = keys.scrollLock
keys.cimcumflex = keys.circumflex

function getName( _nKey )
    if type( _nKey ) ~= "number" then
        error( "bad argument #1 (expected number, got " .. type( _nKey ) .. ")", 2 )
    end
    return tKeys[ _nKey ]
end
