package dan200.computercraft.shared.turtle.upgrades;


import javax.annotation.Nullable;

import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.core.InteractDirection;
import dan200.computercraft.shared.turtle.core.TurtleToolCommand;

public class ToolPeripheral implements IPeripheral{
    private final ITurtleAccess turtle;
    private final TurtleSide side;
    private final String type;

    public ToolPeripheral(ITurtleAccess turtle, TurtleSide side, String type) {
        this.turtle = turtle;
        this.side = side;
        this.type = type;
    }

    @Override
    public String getType() {
        return type;
    }

    @LuaFunction
    public final MethodResult digDown() throws LuaException {
        return turtle.executeCommand(TurtleToolCommand.dig(InteractDirection.DOWN, side));
    }

    @LuaFunction
    public final MethodResult digUp() throws LuaException {
        return turtle.executeCommand(TurtleToolCommand.dig(InteractDirection.UP, side));
    }

    @LuaFunction
    public final MethodResult dig() throws LuaException {
        return turtle.executeCommand(TurtleToolCommand.dig(InteractDirection.FORWARD, side));
    }

    @LuaFunction
    public final MethodResult attackDown() throws LuaException {
        return turtle.executeCommand(TurtleToolCommand.attack(InteractDirection.DOWN, side));
    }

    @LuaFunction
    public final MethodResult attackUp() throws LuaException {
        return turtle.executeCommand(TurtleToolCommand.attack(InteractDirection.UP, side));
    }

    @LuaFunction
    public final MethodResult attack() throws LuaException {
        return turtle.executeCommand(TurtleToolCommand.attack(InteractDirection.FORWARD, side));
    }

    @Override
    public boolean equals(@Nullable IPeripheral other) {
        return other instanceof ToolPeripheral && type.equals(other.getType());
    }

    @Override
    public Object getTarget() {
        return turtle;
    }
}
