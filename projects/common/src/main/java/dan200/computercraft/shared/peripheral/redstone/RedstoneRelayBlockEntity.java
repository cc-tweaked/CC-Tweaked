// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0
package dan200.computercraft.shared.peripheral.redstone;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.redstone.RedstoneState;
import dan200.computercraft.impl.BundledRedstone;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.RedstoneUtil;
import dan200.computercraft.shared.util.TickScheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class RedstoneRelayBlockEntity extends BlockEntity {
    private final TickScheduler.Token tickToken = new TickScheduler.Token(this);

    private final RedstoneState redstoneState = new RedstoneState(() -> TickScheduler.schedule(tickToken));
    private final RedstoneRelayPeripheral peripheral = new RedstoneRelayPeripheral(redstoneState);

    public RedstoneRelayBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModRegistry.BlockEntities.REDSTONE_RELAY.get(), pos, blockState);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        TickScheduler.schedule(tickToken);
    }

    void update() {
        var changes = redstoneState.updateOutput();
        if (changes != 0) {
            for (var direction : DirectionUtil.FACINGS) {
                if ((changes & (1 << mapSide(direction).ordinal())) != 0) updateRedstoneTo(direction);
            }
        }

        if (redstoneState.pollInputChanged()) peripheral.queueRedstoneEvent();
    }

    void neighborChanged(BlockPos neighbour) {
        for (var dir : DirectionUtil.FACINGS) {
            var offset = getBlockPos().relative(dir);
            if (offset.equals(neighbour)) {
                updateRedstoneInput(dir, offset, false);
                return;
            }
        }

        // If the position is not any adjacent one, update all inputs. This is pretty terrible, but some redstone mods
        // handle this incorrectly.
        for (var dir : DirectionUtil.FACINGS) updateRedstoneInput(dir, getBlockPos().relative(dir), false);
    }

    private void updateRedstoneTo(Direction direction) {
        RedstoneUtil.propagateRedstoneOutput(getLevel(), getBlockPos(), direction);
        updateRedstoneInput(direction, getBlockPos().relative(direction), true);
    }

    private void updateRedstoneInput(Direction dir, BlockPos targetPos, boolean ticking) {
        var changed = redstoneState.setInput(mapSide(dir),
            RedstoneUtil.getRedstoneInput(getLevel(), targetPos, dir),
            BundledRedstone.getOutput(getLevel(), targetPos, dir.getOpposite())
        );

        // If the input has changed, and we're not currently in update(), then schedule a new tick so we can queue a
        // redstone event.
        if (changed && !ticking) TickScheduler.schedule(tickToken);
    }

    private ComputerSide mapSide(Direction globalSide) {
        return DirectionUtil.toLocal(getBlockState().getValue(HorizontalDirectionalBlock.FACING), globalSide);
    }

    int getRedstoneOutput(Direction side) {
        return redstoneState.getExternalOutput(mapSide(side));
    }

    int getBundledRedstoneOutput(Direction side) {
        return redstoneState.getExternalBundledOutput(mapSide(side));
    }

    public IPeripheral peripheral() {
        return peripheral;
    }
}
