/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileComputer extends TileComputerBase {
    private IPeripheral peripheral;

    public TileComputer(BlockEntityType<? extends TileComputer> type, BlockPos pos, BlockState state, ComputerFamily family) {
        super(type, pos, state, family);
    }

    @Override
    protected ServerComputer createComputer(int id) {
        var family = getFamily();
        var computer = new ServerComputer(
            (ServerLevel) getLevel(), id, label, family,
            ComputerCraft.computerTermWidth, ComputerCraft.computerTermHeight
        );
        computer.setPosition(getBlockPos());
        return computer;
    }

    protected boolean isUsableByPlayer(Player player) {
        return isUsable(player);
    }

    @Override
    public Direction getDirection() {
        return getBlockState().getValue(BlockComputer.FACING);
    }

    @Override
    protected void updateBlockState(ComputerState newState) {
        var existing = getBlockState();
        if (existing.getValue(BlockComputer.STATE) != newState) {
            getLevel().setBlock(getBlockPos(), existing.setValue(BlockComputer.STATE, newState), 3);
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
    public AbstractContainerMenu createMenu(int id, @Nonnull Inventory inventory, @Nonnull Player player) {
        return new ComputerMenuWithoutInventory(ModRegistry.Menus.COMPUTER.get(), id, inventory, this::isUsableByPlayer, createServerComputer(), getFamily());
    }

    public IPeripheral peripheral() {
        if (peripheral != null) return peripheral;
        return peripheral = new ComputerPeripheral("computer", this);
    }
}
