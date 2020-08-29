/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.Containers;
import dan200.computercraft.shared.util.NamedBlockEntityType;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;

public class TileComputer extends TileComputerBase {
    public static final NamedBlockEntityType<TileComputer> FACTORY_NORMAL = NamedBlockEntityType.create(new Identifier(ComputerCraft.MOD_ID,
                                                                                                                       "computer_normal"),
                                                                                                        f -> new TileComputer(ComputerFamily.Normal, f));

    public static final NamedBlockEntityType<TileComputer> FACTORY_ADVANCED = NamedBlockEntityType.create(new Identifier(ComputerCraft.MOD_ID,
                                                                                                                         "computer_advanced"),
                                                                                                          f -> new TileComputer(ComputerFamily.Advanced,
                                                                                                                                f));

    private ComputerProxy m_proxy;

    public TileComputer(ComputerFamily family, BlockEntityType<? extends TileComputer> type) {
        super(type, family);
    }

    @Override
    public void openGUI(PlayerEntity player) {
        Containers.openComputerGUI(player, this);
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
                                                     id, this.m_label,
                                                     instanceID,
                                                     family,
                                                     ComputerCraft.terminalWidth_computer,
                                                     ComputerCraft.terminalHeight_computer);
        computer.setPosition(this.getPos());
        return computer;
    }

    @Override
    public ComputerProxy createProxy() {
        if (this.m_proxy == null) {
            this.m_proxy = new ComputerProxy() {
                @Override
                protected TileComputerBase getTile() {
                    return TileComputer.this;
                }
            };
        }
        return this.m_proxy;
    }

    public boolean isUsableByPlayer(PlayerEntity player) {
        return this.isUsable(player, false);
    }
}
