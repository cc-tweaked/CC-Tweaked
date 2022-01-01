/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ComputerState;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.computer.inventory.ComputerMenuWithoutInventory;
import dan200.computercraft.shared.util.CapabilityUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class TileComputer extends TileComputerBase
{
    private ComputerProxy proxy;
    private LazyOptional<IPeripheral> peripheral;

    public TileComputer( BlockEntityType<? extends TileComputer> type, BlockPos pos, BlockState state, ComputerFamily family )
    {
        super( type, pos, state, family );
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        ComputerFamily family = getFamily();
        ServerComputer computer = new ServerComputer(
            getLevel(), id, label, instanceID, family,
            ComputerCraft.computerTermWidth,
            ComputerCraft.computerTermHeight
        );
        computer.setPosition( getBlockPos() );
        return computer;
    }

    protected boolean isUsableByPlayer( Player player )
    {
        return isUsable( player, false );
    }

    @Override
    public Direction getDirection()
    {
        return getBlockState().getValue( BlockComputer.FACING );
    }

    @Override
    protected void updateBlockState( ComputerState newState )
    {
        BlockState existing = getBlockState();
        if( existing.getValue( BlockComputer.STATE ) != newState )
        {
            getLevel().setBlock( getBlockPos(), existing.setValue( BlockComputer.STATE, newState ), 3 );
        }
    }

    @Override
    protected ComputerSide remapLocalSide( ComputerSide localSide )
    {
        // For legacy reasons, computers invert the meaning of "left" and "right". A computer's front is facing
        // towards you, but a turtle's front is facing the other way.
        if( localSide == ComputerSide.RIGHT ) return ComputerSide.LEFT;
        if( localSide == ComputerSide.LEFT ) return ComputerSide.RIGHT;
        return localSide;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu( int id, @Nonnull Inventory inventory, @Nonnull Player player )
    {
        return new ComputerMenuWithoutInventory( Registry.ModContainers.COMPUTER.get(), id, inventory, this::isUsableByPlayer, createServerComputer(), getFamily() );
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability( @Nonnull Capability<T> cap, @Nullable Direction side )
    {
        if( cap == CAPABILITY_PERIPHERAL )
        {
            if( peripheral == null )
            {
                peripheral = LazyOptional.of( () -> {
                    if( proxy == null ) proxy = new ComputerProxy( () -> this );
                    return new ComputerPeripheral( "computer", proxy );
                } );
            }
            return peripheral.cast();
        }

        return super.getCapability( cap, side );
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        peripheral = CapabilityUtil.invalidate( peripheral );
    }
}
