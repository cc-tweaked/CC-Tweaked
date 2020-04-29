--- The turtle API allows you to control your turtle.
--
-- @module turtle

if not turtle then
    error("Cannot load turtle API on computer", 2)
end

--- The builtin turtle API, without any generated helper functions.
--
-- Generally you should not need to use this table - it only exists for
-- backwards compatibility reasons.
native = turtle.native or turtle

local function addCraftMethod(object)
    if peripheral.getType("left") == "workbench" then
        object.craft = function(...)
            return peripheral.call("left", "craft", ...)
        end
    elseif peripheral.getType("right") == "workbench" then
        object.craft = function(...)
            return peripheral.call("right", "craft", ...)
        end
    else
        object.craft = nil
    end
end

-- Put commands into environment table
local env = _ENV
for k, v in pairs(native) do
    if k == "equipLeft" or k == "equipRight" then
        env[k] = function(...)
            local result, err = v(...)
            addCraftMethod(turtle)
            return result, err
        end
    else
        env[k] = v
    end
end
addCraftMethod(env)
