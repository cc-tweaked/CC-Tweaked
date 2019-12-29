local expect = require "cc.expect".expect
local type, getmetatable, setmetatable = type, getmetatable, setmetatable

--- A document, suitable for pretty printing.
--
-- Documents effectively represent a sequence of strings in alternative layouts,
-- which we will try to print in the most compact form necessary.
--
-- @type Doc
local Doc = { }

--- An empty document
local empty = setmetatable({ tag = "nil" }, Doc)

--- A document with a single space in it
local space = setmetatable({ tag = "text", text = " " }, Doc)

--- A line break. When collapsed with @{group}, this will be replaced with @{empty}.
local line = setmetatable({ tag = "line", flat = empty }, Doc)

--- A line break. When collapsed with @{group}, this will be replaced with @{space}.
local space_line = setmetatable({ tag = "line", flat = space }, Doc)

local text_cache = { [""] = empty, [" "] = space }

--- Create a new text document
--
-- @tparam      string text   The string to construct a new document with.
-- @tparam[opt] number colour The colour this text should be printed with.
--                            Otherwise we default to the current colour.
-- @treturn Doc The document with the provided text.
local function text(text, colour)
    expect(1, text, "string")
    expect(2, colour, "number", "nil")
    return text_cache[text] or setmetatable({ tag = "text", text = text, colour = colour }, Doc)
end

--- Concatinate several documents together.

-- @tparam Doc|string ... The documents to concatinate.
-- @treturn Doc The concatinated documents.
local function concat(...)
    local args = table.pack(...)
    for i = 1, args.n do
        if type(args[i]) == "string" then args[i] = text(args[i]) end
        if getmetatable(args[i]) ~= Doc then expect(i, args[i], "document") end
    end

    if args.n == 0 then return empty end
    if args.n == 1 then return args[1] end

    args.tag = "concat"
    return setmetatable(args, Doc)
end

--- Render later lines of the given document with a specific indentation level.
--
-- For instance, nesting the document
-- ```txt
-- foo
-- bar
-- ``
-- by two spaces will produce
-- ```txt
-- foo
--   bar
-- ```
--
-- @tparam number depth The number of spaces with which the document should be
--                      indented.
-- @tparam Doc    doc   The document to indent.
-- @treturn Doc The nested document.
local function nest(depth, doc)
    expect(1, depth, "number")
    if getmetatable(doc) ~= Doc then expect(2, doc, "document") end
    if depth <= 0 then error("depth must be a positive number", 2) end

    return setmetatable({ tag = "nest", depth = depth, doc }, Doc)
end

local function flatten(doc)
    if doc.is_flat then return doc end

    local kind = doc.tag
    if kind == "nil" or kind == "text" then
        return doc
    elseif kind == "line" then
        return doc.flat
    elseif kind == "concat" then
        local out = setmetatable({ tag = "concat", is_flat = true, n = doc.n }, Doc)
        for i = 1, doc.n do out[i] = flatten(doc[i]) end
        return out
    elseif kind == "nest" then
        return flatten(doc[1])
    elseif kind == "group" then
        return doc[1]
    else
        error("Unknown doc " .. kind)
    end
end

--- Builds a document which will be displayed on a single line if there is
-- enough room for it.
--
-- @tparam Doc doc The document to group.
-- @treturn Doc The grouped document.
local function group(doc)
    if getmetatable(doc) ~= Doc then expect(1, doc, "document") end

    if doc.tag == "group" then return doc end -- Skip if already grouped.

    local flattened = flatten(doc)
    if flattened == doc then return doc end -- Also skip if flattening does nothing.
    return setmetatable({ tag = "group", flattened, doc }, Doc)
end

local function get_remaining(doc, width)
    local kind = doc.tag
    if kind == "nil" or kind == "line" then
        return width
    elseif kind == "text" then
        return width - #doc.text
    elseif kind == "concat" then
        for i = 1, doc.n do
            width = get_remaining(doc[i], width)
            if width < 0 then break end
        end
        return width
    elseif kind == "group" or kind == "nest" then
        return get_remaining(kind[1])
    else
        error("Unknown doc " .. kind)
    end
end

--- Print a document to the terminal
--
-- @tparam      Doc         doc         The document to render
-- @tparam[opt] term.Target term        The terminal to draw to, defaults to term.current().
-- @tparam[opt] number      width       The width that we should try to fit this document in.
-- @tparam[opt] number      ribbon_frac The maximum fraction of the width that we
--                                      should write in.
local function write(doc, term, width, ribbon_frac)
    if getmetatable(doc) ~= Doc then expect(1, doc, "document") end
    expect(2, term, "table", "nil")
    expect(3, width, "number", "nil")
    expect(4, ribbon_frac, "number", "nil")

    term = term or _ENV.term.current()
    local twidth, theight = term.getSize()
    width = width or twidth
    local ribbon_width = (ribbon_frac or 0.6) * width
    if ribbon_width < 0 then ribbon_width = 0 end
    if ribbon_width > width then ribbon_width = width end

    local def_colour = term.getTextColour()
    local current_colour = def_colour

    local function go(doc, indent, col)
        local kind = doc.tag
        if kind == "nil" then
            return col
        elseif kind == "text" then
            local doc_colour = doc.colour or def_colour
            if doc_colour ~= current_colour then
                term.setTextColour(doc_colour)
                current_colour = doc_colour
            end

            term.write(doc.text)

            return col + #doc.text
        elseif kind == "line" then
            local _, y = term.getCursorPos()
            if y < theight then
                term.setCursorPos(indent + 1, y + 1)
            else
                term.scroll(1)
                term.setCursorPos(indent + 1, theight)
            end

            return indent
        elseif kind == "concat" then
            for i = 1, doc.n do col = go(doc[i], indent, col) end
            return col
        elseif kind == "nest" then
            return go(doc[1], indent + doc.depth, col)
        elseif kind == "group" then
            if get_remaining(doc[1], math.min(width, ribbon_width + indent) - col) >= 0 then
                return go(doc[1], indent, col)
            else
                return go(doc[2], indent, col)
            end
        else
            error("Unknown doc " .. kind)
        end
    end

    go(doc, 0, 0)
    if current_colour ~= def_colour then term.setTextColour(def_colour) end
end

-- Setup some additional metamethods
Doc.__concat = concat

return {
    empty = empty,
    space = space,
    line = line,
    space_line = space_line,
    text = text,
    concat = concat,
    nest = nest,
    group = group,

    write = write,
}
