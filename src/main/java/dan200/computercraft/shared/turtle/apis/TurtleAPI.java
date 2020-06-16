/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.apis;

import dan200.computercraft.api.lua.*;
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
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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
    public final MethodResult dig( Optional<TurtleSide> side )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.FORWARD, side.orElse( null ) ) );
    }

    @LuaFunction
    public final MethodResult digUp( Optional<TurtleSide> side )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.UP, side.orElse( null ) ) );
    }

    @LuaFunction
    public final MethodResult digDown( Optional<TurtleSide> side )
    {
        environment.addTrackingChange( TrackingField.TURTLE_OPS );
        return trackCommand( TurtleToolCommand.dig( InteractDirection.DOWN, side.orElse( null ) ) );
    }

    @LuaFunction
    public final MethodResult place( IArguments args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.FORWARD, args.getAll() ) );
    }

    @LuaFunction
    public final MethodResult placeUp( IArguments args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.UP, args.getAll() ) );
    }

    @LuaFunction
    public final MethodResult placeDown( IArguments args )
    {
        return trackCommand( new TurtlePlaceCommand( InteractDirection.DOWN, args.getAll() ) );
    }

    @LuaFunction
    public final MethodResult drop( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleDropCommand( InteractDirection.FORWARD, checkCount( count ) ) );
    }

    @LuaFunction
    public final MethodResult dropUp( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleDropCommand( InteractDirection.UP, checkCount( count ) ) );
    }

    @LuaFunction
    public final MethodResult dropDown( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleDropCommand( InteractDirection.DOWN, checkCount( count ) ) );
    }

    @LuaFunction
    public final MethodResult select( int slot ) throws LuaException
    {
        int actualSlot = checkSlot( slot );
        return turtle.executeCommand( turtle -> {
            turtle.setSelectedSlot( actualSlot );
            return TurtleCommandResult.success();
        } );
    }

    @LuaFunction
    public final int getItemCount( Optional<Integer> slot ) throws LuaException
    {
        int actualSlot = checkSlot( slot ).orElse( turtle.getSelectedSlot() );
        return turtle.getInventory().getStackInSlot( actualSlot ).getCount();
    }

    @LuaFunction
    public final int getItemSpace( Optional<Integer> slot ) throws LuaException
    {
        int actualSlot = checkSlot( slot ).orElse( turtle.getSelectedSlot() );
        ItemStack stack = turtle.getInventory().getStackInSlot( actualSlot );
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
    public final MethodResult attack( Optional<TurtleSide> side )
    {
        return trackCommand( TurtleToolCommand.attack( InteractDirection.FORWARD, side.orElse( null ) ) );
    }

    @LuaFunction
    public final MethodResult attackUp( Optional<TurtleSide> side )
    {
        return trackCommand( TurtleToolCommand.attack( InteractDirection.UP, side.orElse( null ) ) );
    }

    @LuaFunction
    public final MethodResult attackDown( Optional<TurtleSide> side )
    {
        return trackCommand( TurtleToolCommand.attack( InteractDirection.DOWN, side.orElse( null ) ) );
    }

    @LuaFunction
    public final MethodResult suck( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleSuckCommand( InteractDirection.FORWARD, checkCount( count ) ) );
    }

    @LuaFunction
    public final MethodResult suckUp( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleSuckCommand( InteractDirection.UP, checkCount( count ) ) );
    }

    @LuaFunction
    public final MethodResult suckDown( Optional<Integer> count ) throws LuaException
    {
        return trackCommand( new TurtleSuckCommand( InteractDirection.DOWN, checkCount( count ) ) );
    }

    @LuaFunction
    public final Object getFuelLevel()
    {
        return turtle.isFuelNeeded() ? turtle.getFuelLevel() : "unlimited";
    }

    @LuaFunction
    public final MethodResult refuel( Optional<Integer> countA ) throws LuaException
    {
        int count = countA.orElse( Integer.MAX_VALUE );
        if( count < 0 ) throw new LuaException( "Refuel count " + count + " out of range" );
        return trackCommand( new TurtleRefuelCommand( count ) );
    }

    @LuaFunction
    public final MethodResult compareTo( int slot ) throws LuaException
    {
        return trackCommand( new TurtleCompareToCommand( checkSlot( slot ) ) );
    }

    @LuaFunction
    public final MethodResult transferTo( int slotArg, Optional<Integer> countArg ) throws LuaException
    {
        int slot = checkSlot( slotArg );
        int count = checkCount( countArg );
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
    public final Object[] getItemDetail( Optional<Integer> slotArg ) throws LuaException
    {
        // FIXME: There's a race condition here if the stack is being modified (mutating NBT, etc...)
        //  on another thread. The obvious solution is to move this into a command, but some programs rely
        //  on this having a 0-tick delay.
        int slot = checkSlot( slotArg ).orElse( turtle.getSelectedSlot() );
        ItemStack stack = turtle.getInventory().getStackInSlot( slot );
        if( stack.isEmpty() ) return new Object[] { null };

        Item item = stack.getItem();
        String name = ForgeRegistries.ITEMS.getKey( item ).toString();
        int count = stack.getCount();

        Map<String, Object> table = new HashMap<>();
        table.put( "name", name );
        table.put( "count", count );

        Map<String, Boolean> tags = new HashMap<>();
        for( ResourceLocation location : item.getTags() ) tags.put( location.toString(), true );
        table.put( "tags", tags );

        TurtleActionEvent event = new TurtleInspectItemEvent( turtle, stack, table );
        if( MinecraftForge.EVENT_BUS.post( event ) ) return new Object[] { false, event.getFailureMessage() };

        return new Object[] { table };
    }


    private static int checkSlot( int slot ) throws LuaException
    {
        if( slot < 1 || slot > 16 ) throw new LuaException( "Slot number " + slot + " out of range" );
        return slot - 1;
    }

    private static Optional<Integer> checkSlot( Optional<Integer> slot ) throws LuaException
    {
        return slot.isPresent() ? Optional.of( checkSlot( slot.get() ) ) : Optional.empty();
    }

    private static int checkCount( Optional<Integer> countArg ) throws LuaException
    {
        int count = countArg.orElse( 64 );
        if( count < 0 || count > 64 ) throw new LuaException( "Item count " + count + " out of range" );
        return count;
    }
}
