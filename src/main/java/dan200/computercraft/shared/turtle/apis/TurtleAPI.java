/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.apis;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.event.TurtleAction;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.turtle.core.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

import static dan200.computercraft.core.apis.ArgumentHelper.*;

public class TurtleAPI implements ILuaAPI
{
    private IAPIEnvironment m_environment;
    private ITurtleAccess m_turtle;

    public TurtleAPI( IAPIEnvironment environment, ITurtleAccess turtle )
    {
        m_environment = environment;
        m_turtle = turtle;
    }

    // ILuaAPI implementation

    @Override
    public String[] getNames()
    {
        return new String[] {
            "turtle"
        };
    }

    @Nonnull
    @Override
    public String[] getMethodNames()
    {
        return new String[] {
            "forward",
            "back",
            "up",
            "down",
            "turnLeft",
            "turnRight",
            "dig",
            "digUp",
            "digDown",
            "place",
            "placeUp",
            "placeDown",
            "drop",
            "select",
            "getItemCount",
            "getItemSpace",
            "detect",
            "detectUp",
            "detectDown",
            "compare",
            "compareUp",
            "compareDown",
            "attack",
            "attackUp",
            "attackDown",
            "dropUp",
            "dropDown",
            "suck",
            "suckUp",
            "suckDown",
            "getFuelLevel",
            "refuel",
            "compareTo",
            "transferTo",
            "getSelectedSlot",
            "getFuelLimit",
            "equipLeft",
            "equipRight",
            "inspect",
            "inspectUp",
            "inspectDown",
            "getItemDetail",
        };
    }

    private Object[] tryCommand( ILuaContext context, ITurtleCommand command ) throws LuaException, InterruptedException
    {
        return m_turtle.executeCommand( context, command );
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

    private int parseCount( Object[] arguments, int index ) throws LuaException
    {
        int count = optInt( arguments, index, 64 );
        if( count < 0 || count > 64 ) throw new LuaException( "Item count " + count + " out of range" );
        return count;
    }

    @Nullable
    private TurtleSide parseSide( Object[] arguments, int index ) throws LuaException
    {
        String side = optString( arguments, index, null );
        if( side == null )
        {
            return null;
        }
        else if( side.equalsIgnoreCase( "left" ) )
        {
            return TurtleSide.Left;
        }
        else if( side.equalsIgnoreCase( "right" ) )
        {
            return TurtleSide.Right;
        }
        else
        {
            throw new LuaException( "Invalid side" );
        }
    }

    @Override
    public Object[] callMethod( @Nonnull ILuaContext context, int method, @Nonnull Object[] args ) throws LuaException, InterruptedException
    {
        switch( method )
        {
            case 0:
            {
                // forward
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleMoveCommand( MoveDirection.Forward ) );
            }
            case 1:
            {
                // back
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleMoveCommand( MoveDirection.Back ) );
            }
            case 2:
            {
                // up
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleMoveCommand( MoveDirection.Up ) );
            }
            case 3:
            {
                // down
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleMoveCommand( MoveDirection.Down ) );
            }
            case 4:
            {
                // turnLeft
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleTurnCommand( TurnDirection.Left ) );
            }
            case 5:
            {
                // turnRight
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleTurnCommand( TurnDirection.Right ) );
            }
            case 6:
            {
                // dig
                TurtleSide side = parseSide( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, TurtleToolCommand.dig( InteractDirection.Forward, side ) );
            }
            case 7:
            {
                // digUp
                TurtleSide side = parseSide( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, TurtleToolCommand.dig( InteractDirection.Up, side ) );
            }
            case 8:
            {
                // digDown
                TurtleSide side = parseSide( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, TurtleToolCommand.dig( InteractDirection.Down, side ) );
            }
            case 9:
            {
                // place
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtlePlaceCommand( InteractDirection.Forward, args ) );
            }
            case 10:
            {
                // placeUp
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtlePlaceCommand( InteractDirection.Up, args ) );
            }
            case 11:
            {
                // placeDown
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtlePlaceCommand( InteractDirection.Down, args ) );
            }
            case 12:
            {
                // drop
                int count = parseCount( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleDropCommand( InteractDirection.Forward, count ) );
            }
            case 13:
            {
                // select
                int slot = parseSlotNumber( args, 0 );
                return tryCommand( context, turtle -> {
                    turtle.setSelectedSlot( slot );
                    return TurtleCommandResult.success();
                } );
            }
            case 14:
            {
                // getItemCount
                int slot = parseOptionalSlotNumber( args, 0, m_turtle.getSelectedSlot() );
                ItemStack stack = m_turtle.getInventory().getStackInSlot( slot );
                if( !stack.isEmpty() )
                {
                    return new Object[] { stack.getCount() };
                }
                else
                {
                    return new Object[] { 0 };
                }
            }
            case 15:
            {
                // getItemSpace
                int slot = parseOptionalSlotNumber( args, 0, m_turtle.getSelectedSlot() );
                ItemStack stack = m_turtle.getInventory().getStackInSlot( slot );
                if( !stack.isEmpty() )
                {
                    return new Object[] {
                        Math.min( stack.getMaxStackSize(), 64 ) - stack.getCount()
                    };
                }
                return new Object[] { 64 };
            }
            case 16:
            {
                // detect
                return tryCommand( context, new TurtleDetectCommand( InteractDirection.Forward ) );
            }
            case 17:
            {
                // detectUp
                return tryCommand( context, new TurtleDetectCommand( InteractDirection.Up ) );
            }
            case 18:
            {
                // detectDown
                return tryCommand( context, new TurtleDetectCommand( InteractDirection.Down ) );
            }
            case 19:
            {
                // compare
                return tryCommand( context, new TurtleCompareCommand( InteractDirection.Forward ) );
            }
            case 20:
            {
                // compareUp
                return tryCommand( context, new TurtleCompareCommand( InteractDirection.Up ) );
            }
            case 21:
            {
                // compareDown
                return tryCommand( context, new TurtleCompareCommand( InteractDirection.Down ) );
            }
            case 22:
            {
                // attack
                TurtleSide side = parseSide( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, TurtleToolCommand.attack( InteractDirection.Forward, side ) );
            }
            case 23:
            {
                // attackUp
                TurtleSide side = parseSide( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, TurtleToolCommand.attack( InteractDirection.Up, side ) );
            }
            case 24:
            {
                // attackDown
                TurtleSide side = parseSide( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, TurtleToolCommand.attack( InteractDirection.Down, side ) );
            }
            case 25:
            {
                // dropUp
                int count = parseCount( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleDropCommand( InteractDirection.Up, count ) );
            }
            case 26:
            {
                // dropDown
                int count = parseCount( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleDropCommand( InteractDirection.Down, count ) );
            }
            case 27:
            {
                // suck
                int count = parseCount( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleSuckCommand( InteractDirection.Forward, count ) );
            }
            case 28:
            {
                // suckUp
                int count = parseCount( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleSuckCommand( InteractDirection.Up, count ) );
            }
            case 29:
            {
                // suckDown
                int count = parseCount( args, 0 );
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleSuckCommand( InteractDirection.Down, count ) );
            }
            case 30:
            {
                // getFuelLevel
                if( m_turtle.isFuelNeeded() )
                {
                    return new Object[] { m_turtle.getFuelLevel() };
                }
                else
                {
                    return new Object[] { "unlimited" };
                }
            }
            case 31:
            {
                // refuel
                int count = parseCount( args, 0 );
                return tryCommand( context, new TurtleRefuelCommand( count ) );
            }
            case 32:
            {
                // compareTo
                int slot = parseSlotNumber( args, 0 );
                return tryCommand( context, new TurtleCompareToCommand( slot ) );
            }
            case 33:
            {
                // transferTo
                int slot = parseSlotNumber( args, 0 );
                int count = parseCount( args, 1 );
                return tryCommand( context, new TurtleTransferToCommand( slot, count ) );
            }
            case 34:
            {
                // getSelectedSlot
                return new Object[] { m_turtle.getSelectedSlot() + 1 };
            }
            case 35:
            {
                // getFuelLimit
                if( m_turtle.isFuelNeeded() )
                {
                    return new Object[] { m_turtle.getFuelLimit() };
                }
                else
                {
                    return new Object[] { "unlimited" };
                }
            }
            case 36:
            {
                // equipLeft
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleEquipCommand( TurtleSide.Left ) );
            }
            case 37:
            {
                // equipRight
                m_environment.addTrackingChange( TrackingField.TURTLE_OPS );
                return tryCommand( context, new TurtleEquipCommand( TurtleSide.Right ) );
            }
            case 38:
            {
                // inspect
                return tryCommand( context, new TurtleInspectCommand( InteractDirection.Forward ) );
            }
            case 39:
            {
                // inspectUp
                return tryCommand( context, new TurtleInspectCommand( InteractDirection.Up ) );
            }
            case 40:
            {
                // inspectDown
                return tryCommand( context, new TurtleInspectCommand( InteractDirection.Down ) );
            }
            case 41:
            {
                // getItemDetail
                int slot = parseOptionalSlotNumber( args, 0, m_turtle.getSelectedSlot() );
                ItemStack stack = m_turtle.getInventory().getStackInSlot( slot );
                if( !stack.isEmpty() )
                {
                    Item item = stack.getItem();
                    String name = Item.REGISTRY.getNameForObject( item ).toString();
                    int damage = stack.getItemDamage();
                    int count = stack.getCount();

                    Map<Object, Object> table = new HashMap<>();
                    table.put( "name", name );
                    table.put( "damage", damage );
                    table.put( "count", count );

                    TurtleActionEvent event = new TurtleActionEvent( m_turtle, TurtleAction.INSPECT_ITEM );
                    if( MinecraftForge.EVENT_BUS.post( event ) )
                    {
                        return new Object[] { false, event.getFailureMessage() };
                    }

                    return new Object[] { table };
                }
                else
                {
                    return new Object[] { null };
                }
            }
            default:
            {
                return null;
            }
        }
    }
}
