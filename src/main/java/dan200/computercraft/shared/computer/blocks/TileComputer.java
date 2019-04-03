/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.computer.blocks;

import dan200.computercraft.ComputerCraft;
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

public class TileComputer extends TileComputerBase
{
    public static final NamedBlockEntityType<TileComputer> FACTORY_NORMAL = NamedBlockEntityType.create(
        new Identifier( ComputerCraft.MOD_ID, "computer_normal" ),
        f -> new TileComputer( ComputerFamily.Normal, f )
    );

    public static final NamedBlockEntityType<TileComputer> FACTORY_ADVANCED = NamedBlockEntityType.create(
        new Identifier( ComputerCraft.MOD_ID, "computer_advanced" ),
        f -> new TileComputer( ComputerFamily.Advanced, f )
    );

    private ComputerProxy m_proxy;

    public TileComputer( ComputerFamily family, BlockEntityType<? extends TileComputer> type )
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

    @Override
    public void openGUI( PlayerEntity player )
    {
        Containers.openComputerGUI( player, this );
    }

    public boolean isUsableByPlayer( PlayerEntity player )
    {
        return isUsable( player, false );
    }

    @Override
    public Direction getDirection()
    {
        return getCachedState().get( BlockComputer.FACING );
    }

    @Override
    protected void updateBlockState( ComputerState newState )
    {
        BlockState existing = getCachedState();
        if( existing.get( BlockComputer.STATE ) != newState )
        {
            getWorld().setBlockState( getPos(), existing.with( BlockComputer.STATE, newState ), 3 );
        }
    }

    @Override
    protected Direction remapLocalSide( Direction localSide )
    {
        return localSide.getAxis() == Direction.Axis.X ? localSide.getOpposite() : localSide;
    }
}
