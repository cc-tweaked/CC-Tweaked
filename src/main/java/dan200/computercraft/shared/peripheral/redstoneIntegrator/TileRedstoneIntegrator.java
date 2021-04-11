package dan200.computercraft.shared.peripheral.redstoneIntegrator;

import dan200.computercraft.api.lua.ILuaContext;
import dan200.computercraft.api.lua.LuaException;
import dan200.computercraft.api.peripheral.IComputerAccess;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.IPeripheralTile;
import dan200.computercraft.shared.BundledRedstone;
import dan200.computercraft.shared.common.TileGeneric;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.ForgeEventFactory;
import org.squiddev.plethora.gameplay.Plethora;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static dan200.computercraft.api.lua.ArgumentHelper.*;
import static org.squiddev.plethora.api.method.ArgumentHelper.assertBetween;

public class TileRedstoneIntegrator extends TileGeneric implements IPeripheral, IPeripheralTile {
    private final byte[] inputs = new byte[6];
    private final byte[] outputs = new byte[6];
    private final int[] bundledInputs = new int[6];
    private final int[] bundledOutputs = new int[6];

    private boolean outputDirty = false;
    private boolean inputDirty = false;

    private final Set<IComputerAccess> computers = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private void updateInput() {
        World world = getWorld();
        if (world == null || world.isRemote || isInvalid() || !world.isBlockLoaded(pos)) return;

        boolean changed = false;
        for (EnumFacing dir : EnumFacing.VALUES) {
            BlockPos offset = pos.offset(dir);
            EnumFacing offsetSide = dir.getOpposite();
            int dirIdx = dir.ordinal();

            byte newInput = (byte) getRedstoneInput(world, offset, offsetSide);
            if (newInput != inputs[dirIdx]) {
                inputs[dirIdx] = newInput;
                changed = true;
            }

            short newBundled = (short) BundledRedstone.getOutput(world, offset, offsetSide);
            if (bundledInputs[dirIdx] != newBundled) {
                bundledInputs[dirIdx] = newBundled;
                changed = true;
            }
        }

        if (changed) enqueueInputTick();
    }

    private void enqueueInputTick() {
        if (!inputDirty) {
            inputDirty = true;
            BlockRedstoneIntegrator.enqueueTick(this);
        }
    }

    private void enqueueOutputTick() {
        if (!outputDirty) {
            outputDirty = true;
            BlockRedstoneIntegrator.enqueueTick(this);
        }
    }

    void updateOnce() {
        World world = getWorld();
        if (world == null || world.isRemote || isInvalid() || !world.isBlockLoaded(pos)) return;

        if (outputDirty) {
            for (EnumFacing dir : EnumFacing.VALUES) {
                propagateRedstoneOutput(world, pos, dir);
            }
            outputDirty = false;
        }

        if (inputDirty) {
            Iterator<IComputerAccess> computers = this.computers.iterator();
            while (computers.hasNext()) {
                IComputerAccess computer = computers.next();
                try {
                    computer.queueEvent("redstone", new Object[]{ computer.getAttachmentName() });
                } catch (RuntimeException e) {
                    Plethora.LOG.error("Could not queue redstone event", e);
                    computers.remove();
                }
            }
            inputDirty = false;
        }
    }

    /**
     * Gets the redstone input for an adjacent block
     *
     * @param world The world we exist in
     * @param pos   The position of the neighbour
     * @param side  The side we are reading from
     * @return The effective redstone power
     * @see net.minecraft.block.BlockRedstoneDiode#calculateInputStrength(World, BlockPos, IBlockState)
     */

    private static int getRedstoneInput(World world, BlockPos pos, EnumFacing side) {
        int power = world.getRedstonePower(pos, side);
        if (power >= 15) return power;

        IBlockState neighbour = world.getBlockState(pos);
        return neighbour.getBlock() == Blocks.REDSTONE_WIRE
            ? Math.max(power, neighbour.getValue(BlockRedstoneWire.POWER))
            : power;
    }

    /**
     * Propagate ordinary output
     *
     * @param world The world we exist in
     * @param pos   Our position
     * @param side  The side to propagate to
     * @see net.minecraft.block.BlockRedstoneDiode#notifyNeighbors(World, BlockPos, IBlockState)
     */

    private static void propagateRedstoneOutput(World world, BlockPos pos, EnumFacing side) {
        IBlockState block = world.getBlockState(pos);
        if (ForgeEventFactory.onNeighborNotify(world, pos, block, EnumSet.of(side), false).isCanceled()) return;

        BlockPos neighbourPos = pos.offset(side);
        world.neighborChanged(neighbourPos, block.getBlock(), pos);
        world.notifyNeighborsOfStateExcept(neighbourPos, block.getBlock(), side.getOpposite());
    }

    @Override
    public void onLoad() {
        super.onLoad();

        // Update the output to ensure all redstone is turned off.
        enqueueOutputTick();
    }

    @Override
    public void onNeighbourChange(@Nonnull BlockPos pos) {
        updateInput();
    }

    //region Redstone output providers
    @Override
    public boolean getRedstoneConnectivity(EnumFacing side) {
        return true;
    }

    @Override
    public int getRedstoneOutput(EnumFacing side) {
        return outputs[side.ordinal()];
    }

    @Override
    public boolean getBundledRedstoneConnectivity(@Nonnull EnumFacing side) {
        return true;
    }

    @Override
    public int getBundledRedstoneOutput(@Nonnull EnumFacing side) {
        return bundledOutputs[side.ordinal()];
    }
    //endregion

    //region IPeripheral implementation
    @Nonnull
    @Override
    public String getType() {
        return "redstone_integrator";
    }

    @Nonnull
    @Override
    public String[] getMethodNames() {
        return new String[]{
            "getSides",
            "setOutput", "getOutput", "getInput",
            "setBundledOutput", "getBundledOutput", "getBundledInput", "testBundledInput",
            "setAnalogOutput", "setAnalogueOutput", "getAnalogOutput", "getAnalogueOutput", "getAnalogInput", "getAnalogueInput",
        };
    }

    @Override
    public Object[] callMethod(@Nonnull IComputerAccess computer, @Nonnull ILuaContext context, int method, @Nonnull Object[] args) throws LuaException {
        switch (method) {
            case 0: { // getSides
                Map<Integer, String> result = new HashMap<>();

                for (int i = 0; i < EnumFacing.VALUES.length; i++) {
                    result.put(i + 1, EnumFacing.VALUES[i].getName());
                }

                return new Object[]{ result };
            }
            case 1: { // setOutput
                int side = getFacing(args, 0).ordinal();
                byte power = getBoolean(args, 1) ? (byte) 15 : 0;

                outputs[side] = power;

                enqueueOutputTick();
                return null;
            }
            case 2: { // getOutput
                int side = getFacing(args, 0).ordinal();
                return new Object[]{ outputs[side] > 0 };
            }
            case 3: { // getInput
                int side = getFacing(args, 0).ordinal();
                return new Object[]{ inputs[side] > 0 };
            }
            case 4: { // setBundledOutput
                int side = getFacing(args, 0).ordinal();
                int power = getInt(args, 1);

                bundledOutputs[side] = power;
                enqueueOutputTick();
                return null;
            }
            case 5: { // getBundledOutput
                int side = getFacing(args, 0).ordinal();
                return new Object[]{ bundledOutputs[side] };
            }
            case 6: { // getBundledInput
                int side = getFacing(args, 0).ordinal();
                return new Object[]{ bundledInputs[side] };
            }
            case 7: { // testBundledInput
                int side = getFacing(args, 0).ordinal();
                int power = getInt(args, 1);
                return new Object[]{ (bundledInputs[side] & power) == power };
            }
            case 8: // setAnalogueOutput
            case 9: {
                int side = getFacing(args, 0).ordinal();
                int power = getInt(args, 1);

                assertBetween(power, 0, 15, "Power out of range (%s)");

                outputs[side] = (byte) power;
                enqueueOutputTick();
                return null;
            }
            case 10: // getAnalogueOutput
            case 11: {
                int side = getFacing(args, 0).ordinal();
                return new Object[]{ outputs[side] };
            }
            case 12: // getAnalogueInput
            case 13: {
                int side = getFacing(args, 0).ordinal();
                return new Object[]{ inputs[side] };
            }
            default:
                return null;
        }
    }

    @Override
    public void attach(@Nonnull IComputerAccess computer) {
        computers.add(computer);
    }

    @Override
    public void detach(@Nonnull IComputerAccess computer) {
        computers.remove(computer);
    }

    @Override
    public boolean equals(IPeripheral other) {
        return this == other;
    }

    private static EnumFacing getFacing(Object[] args, int index) throws LuaException {
        String value = getString(args, index);
        if (value.equalsIgnoreCase("bottom")) return EnumFacing.DOWN;
        if (value.equalsIgnoreCase("top")) return EnumFacing.UP;

        EnumFacing facing = EnumFacing.byName(value);
        if (facing == null) {
            throw new LuaException("Bad name '" + value.toLowerCase(Locale.ENGLISH) + "' for argument " + (index + 1));
        }

        return facing;
    }

    @Nullable
    @Override
    public IPeripheral getPeripheral(@Nonnull EnumFacing facing) {
        return this;
    }
    //endregion
}
