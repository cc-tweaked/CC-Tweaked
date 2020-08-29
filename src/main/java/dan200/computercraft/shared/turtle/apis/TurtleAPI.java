/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.apis;

import static dan200.computercraft.core.apis.ArgumentHelper.getInt;
import static dan200.computercraft.core.apis.ArgumentHelper.optInt;
import static dan200.computercraft.core.apis.ArgumentHelper.optString;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.api.lua.ILuaAPI;
import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.api.turtle.event.TurtleActionEvent;
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.api.turtle.event.TurtleInspectItemEvent;
import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.tracking.TrackingField;
import dan200.computercraft.shared.turtle.core.InteractDirection;
import dan200.computercraft.shared.turtle.core.MoveDirection;
import dan200.computercraft.shared.turtle.core.TurnDirection;
import dan200.computercraft.shared.turtle.core.TurtleCompareCommand;
import dan200.computercraft.shared.turtle.core.TurtleCompareToCommand;
import dan200.computercraft.shared.turtle.core.TurtleDetectCommand;
import dan200.computercraft.shared.turtle.core.TurtleDropCommand;
import dan200.computercraft.shared.turtle.core.TurtleEquipCommand;
import dan200.computercraft.shared.turtle.core.TurtleInspectCommand;
import dan200.computercraft.shared.turtle.core.TurtleMoveCommand;
import dan200.computercraft.shared.turtle.core.TurtlePlaceCommand;
import dan200.computercraft.shared.turtle.core.TurtleRefuelCommand;
import dan200.computercraft.shared.turtle.core.TurtleSuckCommand;
import dan200.computercraft.shared.turtle.core.TurtleToolCommand;
import dan200.computercraft.shared.turtle.core.TurtleTransferToCommand;
import dan200.computercraft.shared.turtle.core.TurtleTurnCommand;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.registry.Registry;

public class TurtleAPI implements ILuaAPI {
    private IAPIEnvironment m_environment;
    private ITurtleAccess m_turtle;

    public TurtleAPI(IAPIEnvironment environment, ITurtleAccess turtle) {
        this.m_environment = environment;
        this.m_turtle = turtle;
    }

    // ILuaAPI implementation

    @Override
    public String[] getNames() {
        return new String[] {
            "turtle"
        };
    }

    @Nonnull
    @Override
    public String[] getMethodNames() {
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

    @Override
    public Object[] callMethod(@Nonnull ILuaContext context, int method, @Nonnull Object[] args) throws LuaException, InterruptedException {
        switch (method) {
        case 0: // forward
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleMoveCommand(MoveDirection.Forward));
        case 1: // back
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleMoveCommand(MoveDirection.Back));
        case 2: // up
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleMoveCommand(MoveDirection.Up));
        case 3: // down
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleMoveCommand(MoveDirection.Down));
        case 4: // turnLeft
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleTurnCommand(TurnDirection.Left));
        case 5: // turnRight
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleTurnCommand(TurnDirection.Right));
        case 6: {
            // dig
            TurtleSide side = parseSide(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, TurtleToolCommand.dig(InteractDirection.Forward, side));
        }
        case 7: {
            // digUp
            TurtleSide side = parseSide(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, TurtleToolCommand.dig(InteractDirection.Up, side));
        }
        case 8: {
            // digDown
            TurtleSide side = parseSide(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, TurtleToolCommand.dig(InteractDirection.Down, side));
        }
        case 9: // place
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtlePlaceCommand(InteractDirection.Forward, args));
        case 10: // placeUp
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtlePlaceCommand(InteractDirection.Up, args));
        case 11: // placeDown
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtlePlaceCommand(InteractDirection.Down, args));
        case 12: {
            // drop
            int count = parseCount(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleDropCommand(InteractDirection.Forward, count));
        }
        case 13: {
            // select
            int slot = this.parseSlotNumber(args, 0);
            return this.tryCommand(context, turtle -> {
                turtle.setSelectedSlot(slot);
                return TurtleCommandResult.success();
            });
        }
        case 14: {
            // getItemCount
            int slot = this.parseOptionalSlotNumber(args, 0, this.m_turtle.getSelectedSlot());
            ItemStack stack = this.m_turtle.getInventory()
                                           .getStack(slot);
            return new Object[] {stack.getCount()};
        }
        case 15: {
            // getItemSpace
            int slot = this.parseOptionalSlotNumber(args, 0, this.m_turtle.getSelectedSlot());
            ItemStack stack = this.m_turtle.getInventory()
                                           .getStack(slot);
            return new Object[] {stack.isEmpty() ? 64 : Math.min(stack.getMaxCount(), 64) - stack.getCount()};
        }
        case 16: // detect
            return this.tryCommand(context, new TurtleDetectCommand(InteractDirection.Forward));
        case 17: // detectUp
            return this.tryCommand(context, new TurtleDetectCommand(InteractDirection.Up));
        case 18: // detectDown
            return this.tryCommand(context, new TurtleDetectCommand(InteractDirection.Down));
        case 19: // compare
            return this.tryCommand(context, new TurtleCompareCommand(InteractDirection.Forward));
        case 20: // compareUp
            return this.tryCommand(context, new TurtleCompareCommand(InteractDirection.Up));
        case 21: // compareDown
            return this.tryCommand(context, new TurtleCompareCommand(InteractDirection.Down));
        case 22: {
            // attack
            TurtleSide side = parseSide(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, TurtleToolCommand.attack(InteractDirection.Forward, side));
        }
        case 23: {
            // attackUp
            TurtleSide side = parseSide(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, TurtleToolCommand.attack(InteractDirection.Up, side));
        }
        case 24: {
            // attackDown
            TurtleSide side = parseSide(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, TurtleToolCommand.attack(InteractDirection.Down, side));
        }
        case 25: {
            // dropUp
            int count = parseCount(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleDropCommand(InteractDirection.Up, count));
        }
        case 26: {
            // dropDown
            int count = parseCount(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleDropCommand(InteractDirection.Down, count));
        }
        case 27: {
            // suck
            int count = parseCount(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleSuckCommand(InteractDirection.Forward, count));
        }
        case 28: {
            // suckUp
            int count = parseCount(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleSuckCommand(InteractDirection.Up, count));
        }
        case 29: {
            // suckDown
            int count = parseCount(args, 0);
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleSuckCommand(InteractDirection.Down, count));
        }
        case 30: // getFuelLevel
            return new Object[] {this.m_turtle.isFuelNeeded() ? this.m_turtle.getFuelLevel() : "unlimited"};
        case 31: {
            // refuel
            int count = optInt(args, 0, Integer.MAX_VALUE);
            if (count < 0) {
                throw new LuaException("Refuel count " + count + " out of range");
            }
            return this.tryCommand(context, new TurtleRefuelCommand(count));
        }
        case 32: {
            // compareTo
            int slot = this.parseSlotNumber(args, 0);
            return this.tryCommand(context, new TurtleCompareToCommand(slot));
        }
        case 33: {
            // transferTo
            int slot = this.parseSlotNumber(args, 0);
            int count = parseCount(args, 1);
            return this.tryCommand(context, new TurtleTransferToCommand(slot, count));
        }
        case 34: // getSelectedSlot
            return new Object[] {this.m_turtle.getSelectedSlot() + 1};
        case 35: // getFuelLimit
            return new Object[] {this.m_turtle.isFuelNeeded() ? this.m_turtle.getFuelLimit() : "unlimited"};
        case 36: // equipLeft
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleEquipCommand(TurtleSide.Left));
        case 37: // equipRight
            this.m_environment.addTrackingChange(TrackingField.TURTLE_OPS);
            return this.tryCommand(context, new TurtleEquipCommand(TurtleSide.Right));
        case 38: // inspect
            return this.tryCommand(context, new TurtleInspectCommand(InteractDirection.Forward));
        case 39: // inspectUp
            return this.tryCommand(context, new TurtleInspectCommand(InteractDirection.Up));
        case 40: // inspectDown
            return this.tryCommand(context, new TurtleInspectCommand(InteractDirection.Down));
        case 41: {
            // getItemDetail
            int slot = this.parseOptionalSlotNumber(args, 0, this.m_turtle.getSelectedSlot());
            ItemStack stack = this.m_turtle.getInventory()
                                           .getStack(slot);
            if (stack.isEmpty()) {
                return new Object[] {null};
            }

            Item item = stack.getItem();
            String name = Registry.ITEM.getId(item)
                                       .toString();
            int count = stack.getCount();

            Map<String, Object> table = new HashMap<>();
            table.put("name", name);
            table.put("count", count);

            TurtleActionEvent event = new TurtleInspectItemEvent(this.m_turtle, stack, table);
            if (TurtleEvent.post(event)) {
                return new Object[] {
                    false,
                    event.getFailureMessage()
                };
            }

            return new Object[] {table};
        }

        default:
            return null;
        }
    }

    private Object[] tryCommand(ILuaContext context, ITurtleCommand command) throws LuaException, InterruptedException {
        return this.m_turtle.executeCommand(context, command);
    }

    @Nullable
    private static TurtleSide parseSide(Object[] arguments, int index) throws LuaException {
        String side = optString(arguments, index, null);
        if (side == null) {
            return null;
        } else if (side.equalsIgnoreCase("left")) {
            return TurtleSide.Left;
        } else if (side.equalsIgnoreCase("right")) {
            return TurtleSide.Right;
        } else {
            throw new LuaException("Invalid side");
        }
    }

    private static int parseCount(Object[] arguments, int index) throws LuaException {
        int count = optInt(arguments, index, 64);
        if (count < 0 || count > 64) {
            throw new LuaException("Item count " + count + " out of range");
        }
        return count;
    }

    private int parseSlotNumber(Object[] arguments, int index) throws LuaException {
        int slot = getInt(arguments, index);
        if (slot < 1 || slot > 16) {
            throw new LuaException("Slot number " + slot + " out of range");
        }
        return slot - 1;
    }

    private int parseOptionalSlotNumber(Object[] arguments, int index, int fallback) throws LuaException {
        if (index >= arguments.length || arguments[index] == null) {
            return fallback;
        }
        return this.parseSlotNumber(arguments, index);
    }
}
