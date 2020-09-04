/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ContainerComputer;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.Direction;

public class TileComputer extends TileComputerBase {
    private ComputerProxy proxy;

    public TileComputer(ComputerFamily family, BlockEntityType<? extends TileComputer> type) {
        super(type, family);
    }

    public boolean isUsableByPlayer(PlayerEntity player) {
        return this.isUsable(player, false);
    }

    @Override
    protected void updateBlockState(ComputerState newState) {
        BlockState existing = this.getCachedState();
        if (existing.get(BlockComputer.STATE) != newState) {
            this.getWorld().setBlockState(this.getPos(), existing.with(BlockComputer.STATE, newState), 3);
        }
    }

    @Override
    public Direction getDirection() {
        return this.getCachedState().get(BlockComputer.FACING);
    }

    @Override
    protected ComputerSide remapLocalSide(ComputerSide localSide) {
        // For legacy reasons, computers invert the meaning of "left" and "right". A computer's front is facing
        // towards you, but a turtle's front is facing the other way.
        if (localSide == ComputerSide.RIGHT) {
            return ComputerSide.LEFT;
        }
        if (localSide == ComputerSide.LEFT) {
            return ComputerSide.RIGHT;
        }
        return localSide;
    }

    @Override
    protected ServerComputer createComputer(int instanceID, int id) {
        ComputerFamily family = this.getFamily();
        ServerComputer computer = new ServerComputer(this.getWorld(),
                                                     id, this.label,
                                                     instanceID,
                                                     family,
                                                     ComputerCraft.terminalWidth_computer,
                                                     ComputerCraft.terminalHeight_computer);
        computer.setPosition(this.getPos());
        return computer;
    }

    @Override
    public ComputerProxy createProxy() {
        if (this.proxy == null) {
            this.proxy = new ComputerProxy(() -> this) {
                @Override
                protected TileComputerBase getTile() {
                    return TileComputer.this;
                }
            };
        }
        return this.proxy;
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player) {
        return new ContainerComputer(id, this);
    }

}
