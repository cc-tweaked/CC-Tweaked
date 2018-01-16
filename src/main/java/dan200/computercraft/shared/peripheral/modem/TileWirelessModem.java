/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.common.BlockPeripheral;
import dan200.computercraft.shared.peripheral.common.BlockPeripheralVariant;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;

public class TileWirelessModem extends TileModemBase
{
    // Statics

    private static class Peripheral extends WirelessModemPeripheral
    {
        private TileModemBase m_entity;

        public Peripheral( TileModemBase entity )
        {
            super( false );
            m_entity = entity;
        }

        @Nonnull
        @Override
        public World getWorld()
        {
            return m_entity.getWorld();
        }

        @Nonnull
        @Override
        public Vec3d getPosition()
        {
            BlockPos pos = m_entity.getPos().offset( m_entity.getCachedDirection() );
            return new Vec3d( pos.getX(), pos.getY(), pos.getZ() );
        }

        @Override
        public boolean equals( IPeripheral other )
        {
            if( other instanceof Peripheral )
            {
                Peripheral otherModem = (Peripheral) other;
                return otherModem.m_entity == m_entity;
            }
            return false;
        }
    }

    // Members

    private boolean m_hasDirection = false;

    public TileWirelessModem()
    {
        m_dir = EnumFacing.DOWN;
    }

    @Override
    public void onLoad()
    {
        super.onLoad();
        updateDirection();
    }

    @Override
    public void updateContainingBlockInfo()
    {
        m_hasDirection = false;
    }

    @Override
    public void update()
    {
        super.update();
        updateDirection();
    }

    private void updateDirection()
    {
        if( !m_hasDirection )
        {
            m_hasDirection = true;
            m_dir = getDirection();
        }
    }

    @Override
    public EnumFacing getDirection()
    {
        // Wireless Modem
        IBlockState state = getBlockState();
        switch( state.getValue( BlockPeripheral.Properties.VARIANT ) )
        {
            case WirelessModemDownOff:
            case WirelessModemDownOn:
            {
                return EnumFacing.DOWN;
            }
            case WirelessModemUpOff:
            case WirelessModemUpOn:
            {
                return EnumFacing.UP;
            }
            default:
            {
                return state.getValue( BlockPeripheral.Properties.FACING );
            }
        }
    }

    @Override
    public void setDirection( EnumFacing dir )
    {
        // Wireless Modem
        if( dir == EnumFacing.UP )
        {
            setBlockState( getBlockState()
                .withProperty( BlockPeripheral.Properties.VARIANT, BlockPeripheralVariant.WirelessModemUpOff )
                .withProperty( BlockPeripheral.Properties.FACING, EnumFacing.NORTH )
            );
        }
        else if( dir == EnumFacing.DOWN )
        {
            setBlockState( getBlockState()
                .withProperty( BlockPeripheral.Properties.VARIANT, BlockPeripheralVariant.WirelessModemDownOff )
                .withProperty( BlockPeripheral.Properties.FACING, EnumFacing.NORTH )
            );
        }
        else
        {
            setBlockState( getBlockState()
                .withProperty( BlockPeripheral.Properties.VARIANT, BlockPeripheralVariant.WirelessModemOff )
                .withProperty( BlockPeripheral.Properties.FACING, dir )
            );
        }
    }

    @Override
    protected ModemPeripheral createPeripheral()
    {
        return new Peripheral( this );
    }

    @Override
    public boolean shouldRefresh( World world, BlockPos pos, @Nonnull IBlockState oldState, @Nonnull IBlockState newState )
    {
        return super.shouldRefresh( world, pos, oldState, newState ) || ComputerCraft.Blocks.peripheral.getPeripheralType( newState ) != PeripheralType.WirelessModem;
    }
}
