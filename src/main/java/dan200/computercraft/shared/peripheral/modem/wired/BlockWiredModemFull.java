/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2018. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.modem.wired;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.common.BlockGeneric;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BlockWiredModemFull extends BlockGeneric
{
    public static final PropertyBool MODEM_ON = PropertyBool.create( "modem" );
    public static final PropertyBool PERIPHERAL_ON = PropertyBool.create( "peripheral" );

    public BlockWiredModemFull()
    {
        super( Material.ROCK );
        setHardness( 1.5f );
        setTranslationKey( "computercraft:wired_modem_full" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( blockState.getBaseState()
            .withProperty( MODEM_ON, false )
            .withProperty( PERIPHERAL_ON, false )
        );
    }

    @Override
    protected IBlockState getDefaultBlockState( int damage, EnumFacing placedSide )
    {
        return getDefaultState();
    }

    @Nullable
    @Override
    public TileEntity createTileEntity( @Nonnull World world, @Nonnull IBlockState state )
    {
        return new TileWiredModemFull();
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this,
            MODEM_ON,
            PERIPHERAL_ON
        );
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        return 0;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        TileEntity te = world.getTileEntity( pos );
        if( te instanceof TileWiredModemFull )
        {
            TileWiredModemFull modem = (TileWiredModemFull) te;
            state = state
                .withProperty( MODEM_ON, modem.isModemOn() )
                .withProperty( PERIPHERAL_ON, modem.isPeripheralOn() );
        }

        return state;
    }
}
