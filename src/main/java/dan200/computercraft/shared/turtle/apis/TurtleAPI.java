/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.lua.MethodResult;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import dan200.computercraft.api.turtle.event.TurtleInspectItemEvent;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.turtle.core.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static dan200.computercraft.api.lua.ArgumentHelper.*;

public class TurtleAPI implements ILuaAPI
{
    private final IAPIEnvironment environment;
    private final ITurtleAccess turtle;

    public TurtleAPI( IAPIEnvironment environment, ITurtleAccess turtle )
    {
        this.environment = environment;
        this.turtle = turtle;
    }

    @Override
    public String[] getNames()
    {
        return new String[] { "turtle" };
    }

    private MethodResult trackCommand( ITurtleCommand command )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return turtle.executeCommand( command );
    }

    private int parseSlotNumber( Object[] arguments, int index ) throws LuaException
    {
        int slot = getInt( arguments, index );
        if( slot < 1 || slot > 16 ) throw new LuaException( "Slot number " + slot + " out of range" );
        return slot - 1;
    }

    private int parseOptionalSlotNumber( Object[] arguments, int index, int fallback ) throws LuaException
    {
        if( index >= arguments.length || arguments[index] == null ) return fallback;
        return parseSlotNumber( arguments, index );
    }

    private static int parseCount( Object[] arguments, int index ) throws LuaException
    {
        int count = optInt( arguments, index, 64 );
        if( count < 0 || count > 64 ) throw new LuaException( "Item count " + count + " out of range" );
        return count;
    }

    @Nullable
    private static TurtleSide parseSide( Object[] arguments, int index ) throws LuaException
    {
        String side = optString( arguments, index, null );
        if( side == null ) return null;
        if( side.equalsIgnoreCase( "left" ) ) return TurtleSide.LEFT;
        if( side.equalsIgnoreCase( "right" ) ) return TurtleSide.RIGHT;
        throw new LuaException( "Invalid side" );
    }

    @LuaFunction
    public final MethodResult forward()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.FORWARD ) );
    }

    @LuaFunction
    public final MethodResult back()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.BACK ) );
    }

    @LuaFunction
    public final MethodResult up()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.UP ) );
    }

    @LuaFunction
    public final MethodResult down()
    {
        return trackCommand( new TurtleMoveCommand( MoveDirection.DOWN ) );
    }

    @LuaFunction
    public final MethodResult turnLeft()
    {
        return trackCommand( new TurtleTurnCommand( TurnDirection.LEFT ) );
    }

    @LuaFunction
    public final MethodResult turnRight()
    {
        return trackCommand( new TurtleTurnCommand( TurnDirection.RIGHT ) );
    }

    @LuaFunction
    public final MethodResult dig( Object[] args ) throws LuaException
    {
        TurtleSide side = parseSide( args, 0 );
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.FORWARD, side ) );
    }

    @LuaFunction
    public final MethodResult digUp( Object[] args ) throws LuaException
    {
        TurtleSide side = parseSide( args, 0 );
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.UP, side ) );
    }

    @LuaFunction
    public final MethodResult digDown( Object[] args ) throws LuaException
    {
        TurtleSide side = parseSide( args, 0 );
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.DOWN, side ) );
    }

    @LuaFunction
    public final MethodResult place( Object[] args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.FORWARD, args ) );
    }

    @LuaFunction
    public final MethodResult placeUp( Object[] args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.UP, args ) );
    }

    @LuaFunction
    public final MethodResult placeDown( Object[] args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.DOWN, args ) );
    }

    @LuaFunction
    public final MethodResult drop( Object[] args ) throws LuaException
    {
        int count = parseCount( args, 0 );
        return trackCommand( new TurtleDropCommand( InteractDirection.FORWARD, count ) );
    }

    @LuaFunction
    public final MethodResult dropUp( Object[] args ) throws LuaException
    {
        int count = parseCount( args, 0 );
        return trackCommand( new TurtleDropCommand( InteractDirection.UP, count ) );
    }

    @LuaFunction
    public final MethodResult dropDown( Object[] args ) throws LuaException
    {
        int count = parseCount( args, 0 );
        return trackCommand( new TurtleDropCommand( InteractDirection.DOWN, count ) );
    }

    @LuaFunction
    public final MethodResult select( Object[] args ) throws LuaException
    {
        int slot = parseSlotNumber( args, 0 );
        return turtle.executeCommand( turtle -> {
            turtle.setSelectedSlot( slot );
            return TurtleCommandResult.success();
        } );
    }

    @LuaFunction
    public final int getItemCount( Object[] args ) throws LuaException
    {
        int slot = parseOptionalSlotNumber( args, 0, turtle.getSelectedSlot() );
        return turtle.getInventory().getStackInSlot( slot ).getCount();
    }

    @LuaFunction
    public final int getItemSpace( Object[] args ) throws LuaException
    {
        int slot = parseOptionalSlotNumber( args, 0, turtle.getSelectedSlot() );
        ItemStack stack = turtle.getInventory().getStackInSlot( slot );
        return stack.isEmpty() ? 64 : Math.min( stack.getMaxStackSize(), 64 ) - stack.getCount();
    }

    @LuaFunction
    public final MethodResult detect()
    {
        return trackCommand( new TurtleDetectCommand( InteractDirection.FORWARD ) );
    }

    @LuaFunction
    public final MethodResult detectUp()
    {
        return trackCommand( new TurtleDetectCommand( InteractDirection.UP ) );
    }

    @LuaFunction
    public final MethodResult detectDown()
    {
        return trackCommand( new TurtleDetectCommand( InteractDirection.DOWN ) );
    }

    @LuaFunction
    public final MethodResult compare()
    {
        return trackCommand( new TurtleCompareCommand( InteractDirection.FORWARD ) );
    }

    @LuaFunction
    public final MethodResult compareUp()
    {
        return trackCommand( new TurtleCompareCommand( InteractDirection.UP ) );
    }

    @LuaFunction
    public final MethodResult compareDown()
    {
        return trackCommand( new TurtleCompareCommand( InteractDirection.DOWN ) );
    }

    @LuaFunction
    public final MethodResult attack( Object[] args ) throws LuaException
    {
        TurtleSide side = parseSide( args, 0 );
        return trackCommand( TurtleToolCommand.attack( InteractDirection.FORWARD, side ) );
    }

    @LuaFunction
    public final MethodResult attackUp( Object[] args ) throws LuaException
    {
        TurtleSide side = parseSide( args, 0 );
        return trackCommand( TurtleToolCommand.attack( InteractDirection.UP, side ) );
    }

    @LuaFunction
    public final MethodResult attackDown( Object[] args ) throws LuaException
    {
        TurtleSide side = parseSide( args, 0 );
        return trackCommand( TurtleToolCommand.attack( InteractDirection.DOWN, side ) );
    }

    @LuaFunction
    public final MethodResult suck( Object[] args ) throws LuaException
    {
        int count = parseCount( args, 0 );
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( new TurtleSuckCommand( InteractDirection.FORWARD, count ) );
    }

    @LuaFunction
    public final MethodResult suckUp( Object[] args ) throws LuaException
    {
        int count = parseCount( args, 0 );
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( new TurtleSuckCommand( InteractDirection.UP, count ) );
    }

    @LuaFunction
    public final MethodResult suckDown( Object[] args ) throws LuaException
    {
        int count = parseCount( args, 0 );
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( new TurtleSuckCommand( InteractDirection.DOWN, count ) );
    }

    @LuaFunction
    public final Object getFuelLevel()
    {
        return turtle.isFuelNeeded() ? turtle.getFuelLevel() : "unlimited";
    }

    @LuaFunction
    public final MethodResult refuel( Object[] args ) throws LuaException
    {
        int count = optInt( args, 0, Integer.MAX_VALUE );
        if( count < 0 ) throw new LuaException( "Refuel count " + count + " out of range" );
        return trackCommand( new TurtleRefuelCommand( count ) );
    }

    @LuaFunction
    public final MethodResult compareTo( Object[] args ) throws LuaException
    {
        int slot = parseSlotNumber( args, 0 );
        return trackCommand( new TurtleCompareToCommand( slot ) );
    }

    @LuaFunction
    public final MethodResult transferTo( Object[] args ) throws LuaException
    {
        int slot = parseSlotNumber( args, 0 );
        int count = parseCount( args, 1 );
        return trackCommand( new TurtleTransferToCommand( slot, count ) );
    }

    @LuaFunction
    public final int getSelectedSlot()
    {
        return turtle.getSelectedSlot() + 1;
    }

    @LuaFunction
    public final Object getFuelLimit()
    {
        return turtle.isFuelNeeded() ? turtle.getFuelLimit() : "unlimited";
    }

    @LuaFunction
    public final MethodResult equipLeft()
    {
        return trackCommand( new TurtleEquipCommand( TurtleSide.LEFT ) );
    }

    @LuaFunction
    public final MethodResult equipRight()
    {
        return trackCommand( new TurtleEquipCommand( TurtleSide.RIGHT ) );
    }

    @LuaFunction
    public final MethodResult inspect()
    {
        return trackCommand( new TurtleInspectCommand( InteractDirection.FORWARD ) );
    }

    @LuaFunction
    public final MethodResult inspectUp()
    {
        return trackCommand( new TurtleInspectCommand( InteractDirection.UP ) );
    }

    @LuaFunction
    public final MethodResult inspectDown()
    {
        return trackCommand( new TurtleInspectCommand( InteractDirection.DOWN ) );
    }

    @LuaFunction
    public final Object[] getItemDetail( Object[] args ) throws LuaException
    {
        // FIXME: There's a race condition here if the stack is being modified (mutating NBT, etc...)
        //  on another thread. The obvious solution is to move this into a command, but some programs rely
        //  on this having a 0-tick delay.
        int slot = parseOptionalSlotNumber( args, 0, turtle.getSelectedSlot() );
        ItemStack stack = turtle.getInventory().getStackInSlot( slot );
        if( stack.isEmpty() ) return new Object[] { null };

        Item item = stack.getItem();
        String name = ForgeRegistries.ITEMS.getKey( item ).toString();
        int count = stack.getCount();

        Map<String, Object> table = new HashMap<>();
        table.put( "name", name );
        table.put( "count", count );

        TurtleActionEvent event = new TurtleInspectItemEvent( turtle, stack, table );
        if( MinecraftForge.EVENT_BUS.post( event ) ) return new Object[] { false, event.getFailureMessage() };

        return new Object[] { table };
    }
}
