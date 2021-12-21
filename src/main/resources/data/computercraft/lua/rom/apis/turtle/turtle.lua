--[[- Turtles are a robotic device, which can break and place blocks, attack mobs, and move about the world. They have
an internal inventory of 16 slots, allowing them to store blocks they have broken or would like to place.

## Movement
Turtles are capable of moving throug the world. As turtles are blocks themselves, they are confined to Minecraft's grid,
moving a single block at a time.

@{turtle.forward} and @{turtle.back} move the turtle in the direction it is facing, while @{turtle.up} and
@{turtle.down} move it up and down (as one might expect!). In order to move left or right, you first need to turn the
turtle using @{turtle.turnLeft}/@{turtle.turnRight} and then move forward or backwards.

:::info
The name "turtle" comes from [Turtle graphics], which originated from the Logo programming language. Here you'd move
a turtle with various commands like "move 10" and "turn left", much like ComputerCraft's turtles!
:::

Moving a turtle (though not turning it) consumes *fuel*. If a turtle does not have any @{turtle.refuel|fuel}, it won't
move, and the movement functions will return @{false}. If your turtle isn't going anywhere, the first thing to check
is if you've fuelled your turtle.

:::tip Handling errors
Many turtle functions can fail in various ways. For instance, a turtle cannot move forward if there's already a block
there. Instead of erroring, functions which can fail either return @{true} if they succeed, or @{false} and some error
message if they fail.

Unexpected failures can often lead to strange behaviour. It's often a good idea to check the return values of these
functions, or wrap them in @{assert} (for instance, use `assert(turtle.forward())` rather than `turtle.forward()`),
so the program doesn't misbehave.
:::

## Turtle upgrades
While a normal turtle can move about the world and place blocks, its functionality is limited. Thankfully, turtles can
be upgraded with *tools* and @{peripheral|peripherals}. Turtles have two upgrade slots, one on the left and right sides.
Upgrades can be equipped by crafting a turtle with the upgrade, or calling the @{turtle.equipLeft}/@{turtle.equipRight}
functions.

Turtle tools allow you to break blocks (@{turtle.dig}) and attack entities (@{turtle.attack}). Some tools are more
suitable to a task than others. For instance, a diamond pickaxe can break every block, while a sword does more damage.
Other tools have more niche use-cases, for instance hoes can til dirt.

Peripherals (such as the @{modem|wireless modem} or @{speaker}) can also be equipped as upgrades. These are then
accessible by accessing the `"left"` or `"right"` peripheral.

[Turtle Graphics]: https://en.wikipedia.org/wiki/Turtle_graphics "Turtle graphics"

@module turtle
@since 1.3
]]

if not turtle then
    error("Cannot load turtle API on computer", 2)
end

--- The builtin turtle API, without any generated helper functions.
--
-- @deprecated Historically this table behaved differently to the main turtle API, but this is no longer the base. You
-- should not need to use it.
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
