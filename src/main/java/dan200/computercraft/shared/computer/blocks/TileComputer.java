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
import dan200.computercraft.shared.computer.inventory.ContainerComputer;
import dan200.computercraft.shared.util.NamedTileEntityType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TileComputer extends TileComputerBase
{
    public static final NamedTileEntityType<TileComputer> FACTORY_NORMAL = NamedTileEntityType.create(
        new ResourceLocation( ComputerCraft.MOD_ID, "computer_normal" ),
        f -> new TileComputer( ComputerFamily.Normal, f )
    );

    public static final NamedTileEntityType<TileComputer> FACTORY_ADVANCED = NamedTileEntityType.create(
        new ResourceLocation( ComputerCraft.MOD_ID, "computer_advanced" ),
        f -> new TileComputer( ComputerFamily.Advanced, f )
    );

    private ComputerProxy m_proxy;

    public TileComputer( ComputerFamily family, TileEntityType<? extends TileComputer> type )
    {
        super( type, family );
    }

    @Override
    protected ServerComputer createComputer( int instanceID, int id )
    {
        ComputerFamily family = getFamily();
        ServerComputer computer = new ServerComputer(
            getWorld(), id, m_label, instanceID, family,
            ComputerCraft.terminalWidth_computer,
            ComputerCraft.terminalHeight_computer
        );
        computer.setPosition( getPos() );
        return computer;
    }

    @Override
    public ComputerProxy createProxy()
    {
        if( m_proxy == null )
        {
            m_proxy = new ComputerProxy()
            {
                @Override
                protected TileComputerBase getTile()
                {
                    return TileComputer.this;
                }
            };
        }
        return m_proxy;
    }

    public boolean isUsableByPlayer( PlayerEntity player )
    {
        return isUsable( player, false );
    }

    @Override
    public Direction getDirection()
    {
        return getBlockState().get( BlockComputer.FACING );
    }

    @Override
    protected void updateBlockState( ComputerState newState )
    {
        BlockState existing = getBlockState();
        if( existing.get( BlockComputer.STATE ) != newState )
        {
            getWorld().setBlockState( getPos(), existing.with( BlockComputer.STATE, newState ), 3 );
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
        return new ContainerComputer( id, this );
    }
}
