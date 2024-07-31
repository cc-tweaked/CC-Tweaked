// Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
//
// SPDX-License-Identifier: LicenseRef-CCPL

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.util.ComponentMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

public class ComputerBlockEntity extends AbstractComputerBlockEntity {
    private @Nullable IPeripheral peripheral;

    public ComputerBlockEntity(BlockEntityType<? extends ComputerBlockEntity> type, BlockPos pos, BlockState state, ComputerFamily family) {
        super(type, pos, state, family);
    }

    @Override
    protected ServerComputer createComputer(int id) {
        return new ServerComputer(
            (ServerLevel) getLevel(), getBlockPos(), id, label,
            getFamily(), Config.computerTermWidth, Config.computerTermHeight,
            ComponentMap.empty()
        );
    }

    protected boolean isUsableByPlayer(Player player) {
        return isUsable(player);
    }

    @Override
    public Direction getDirection() {
        return getBlockState().getValue(ComputerBlock.FACING);
    }

    @Override
    protected void updateBlockState(ComputerState newState) {
        var existing = getBlockState();
        if (existing.getValue(ComputerBlock.STATE) != newState) {
            getLevel().setBlock(getBlockPos(), existing.setValue(ComputerBlock.STATE, newState), ComputerBlock.UPDATE_CLIENTS);
        }
    }

    @Override
    protected ComputerSide remapLocalSide(ComputerSide localSide) {
        // For legacy reasons, computers invert the meaning of "left" and "right". A computer's front is facing
        // towards you, but a turtle's front is facing the other way.
        if (localSide == ComputerSide.RIGHT) return ComputerSide.LEFT;
        if (localSide == ComputerSide.LEFT) return ComputerSide.RIGHT;
        return localSide;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ComputerMenuWithoutInventory(ModRegistry.Menus.COMPUTER.get(), id, inventory, this::isUsableByPlayer, createServerComputer());
    }

    public IPeripheral peripheral() {
        if (peripheral != null) return peripheral;
        return peripheral = new ComputerPeripheral("computer", this);
    }
}
