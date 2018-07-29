/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;

public class BlockPeripheral extends BlockPeripheralBase
{
    public static final PropertyDirection FACING = BlockHorizontal.FACING;
    public static final PropertyEnum<BlockPeripheralVariant> VARIANT = PropertyEnum.create( "variant", BlockPeripheralVariant.class );

    public BlockPeripheral()
    {
        setHardness( 2.0f );
        setTranslationKey( "computercraft:peripheral" );
        setCreativeTab( ComputerCraft.mainCreativeTab );
        setDefaultState( this.blockState.getBaseState()
            .withProperty( FACING, EnumFacing.NORTH )
            .withProperty( VARIANT, BlockPeripheralVariant.PrinterEmpty )
        );
    }

    @Override
    @Nonnull
    @SideOnly( Side.CLIENT )
    public BlockRenderLayer getRenderLayer()
    {
        return BlockRenderLayer.CUTOUT;
    }

    @Nonnull
    @Override
    protected BlockStateContainer createBlockState()
    {
        return new BlockStateContainer( this, FACING, VARIANT );
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getStateFromMeta( int meta )
    {
        IBlockState state = getDefaultState();
        if( meta == 11 )
        {
            state = state.withProperty( VARIANT, BlockPeripheralVariant.PrinterEmpty );
        }
        return state;
    }

    @Override
    public int getMetaFromState( IBlockState state )
    {
        int meta = 0;
        BlockPeripheralVariant variant = state.getValue( VARIANT );
        switch( variant.getPeripheralType() )
        {
            case Printer:
            {
                meta = 11;
                break;
            }
        }
        return meta;
    }

    @Nonnull
    @Override
    @Deprecated
    public IBlockState getActualState( @Nonnull IBlockState state, IBlockAccess world, BlockPos pos )
    {
        int anim;
        EnumFacing dir;
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TilePeripheralBase )
        {
            TilePeripheralBase peripheral = (TilePeripheralBase) tile;
            anim = peripheral.getAnim();
            dir = peripheral.getDirection();
        }
        else
        {
            anim = 0;
            dir = state.getValue( FACING );
        }

        PeripheralType type = getPeripheralType( state );
        switch( type )
        {
            case Printer:
            {
                state = state.withProperty( FACING, dir );
                switch( anim )
                {
                    case 0:
                    default:
                    {
                        state = state.withProperty( VARIANT, BlockPeripheralVariant.PrinterEmpty );
                        break;
                    }
                    case 1:
                    {
                        state = state.withProperty( VARIANT, BlockPeripheralVariant.PrinterTopFull );
                        break;
                    }
                    case 2:
                    {
                        state = state.withProperty( VARIANT, BlockPeripheralVariant.PrinterBottomFull );
                        break;
                    }
                    case 3:
                    {
                        state = state.withProperty( VARIANT, BlockPeripheralVariant.PrinterBothFull );
                        break;
                    }
                }
                break;
            }
        }
        return state;
    }

    @Override
    public IBlockState getDefaultBlockState( PeripheralType type, EnumFacing placedSide )
    {
        switch( type )
        {
            default:
            case Printer:
            {
                return getDefaultState().withProperty( VARIANT, BlockPeripheralVariant.PrinterEmpty );
            }
        }
    }

    @Override
    public PeripheralType getPeripheralType( int damage )
    {
        return ((ItemPeripheral) Item.getItemFromBlock( this )).getPeripheralType( damage );
    }

    @Override
    public PeripheralType getPeripheralType( IBlockState state )
    {
        return state.getValue( VARIANT ).getPeripheralType();
    }

    @Override
    public TilePeripheralBase createTile( PeripheralType type )
    {
        switch( type )
        {
            default:
            case Printer:
                return new TilePrinter();
        }
    }

    @Override
    public void onBlockPlacedBy( World world, BlockPos pos, IBlockState state, EntityLivingBase player, @Nonnull ItemStack stack )
    {
        // Not sure why this is necessary
        TileEntity tile = world.getTileEntity( pos );
        if( tile instanceof TilePeripheralBase )
        {
            tile.setWorld( world ); // Not sure why this is necessary
            tile.setPos( pos ); // Not sure why this is necessary
        }

        switch( getPeripheralType( state ) )
        {
            case Printer:
            {
                EnumFacing dir = DirectionUtil.fromEntityRot( player );
                setDirection( world, pos, dir );
                if( stack.hasDisplayName() && tile instanceof TilePeripheralBase )
                {
                    TilePeripheralBase peripheral = (TilePeripheralBase) tile;
                    peripheral.setLabel( stack.getDisplayName() );
                }
                break;
            }
        }
    }
}
