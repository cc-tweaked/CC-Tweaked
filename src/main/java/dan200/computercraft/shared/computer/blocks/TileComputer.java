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
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static dan200.computercraft.shared.Capabilities.CAPABILITY_PERIPHERAL;

public class TileComputer extends TileComputerBase
{
    private ComputerProxy proxy;
    private LazyOptional<IPeripheral> peripheral;

    public TileComputer( ComputerFamily family, TileEntityType<? extends TileComputer> type )
    {
        super( type, family );
    }

    @Override
    protected ServerComputer createComputer( int id )
    {
        ComputerFamily family = getFamily();
        ServerComputer computer = new ServerComputer(
            (ServerWorld) getLevel(), id, label, family,
            ComputerCraft.computerTermWidth, ComputerCraft.computerTermHeight
        );
        computer.setPosition( getBlockPos() );
        return computer;
    }

    protected boolean isUsableByPlayer( PlayerEntity player )
    {
        return isUsable( player );
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
    public Container createMenu( int id, @Nonnull PlayerInventory inventory, @Nonnull PlayerEntity player )
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
    protected void invalidateCaps()
    {
        super.invalidateCaps();
        peripheral = CapabilityUtil.invalidate( peripheral );
    }
}
