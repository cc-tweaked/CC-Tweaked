/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.peripheral.common;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.peripheral.PeripheralType;
import dan200.computercraft.shared.peripheral.monitor.TileMonitor;
import dan200.computercraft.shared.peripheral.printer.TilePrinter;
import dan200.computercraft.shared.util.DirectionUtil;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockFaceShape;
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
            .withProperty( VARIANT, BlockPeripheralVariant.AdvancedMonitor )
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
        if( meta == 10 )
        {
            state = state.withProperty( VARIANT, BlockPeripheralVariant.Monitor );
        }
        else if( meta == 11 )
        {
            state = state.withProperty( VARIANT, BlockPeripheralVariant.PrinterEmpty );
        }
        else if( meta == 12 )
        {
            state = state.withProperty( VARIANT, BlockPeripheralVariant.AdvancedMonitor );
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
            case Monitor:
            {
                meta = 10;
                break;
            }
            case Printer:
            {
                meta = 11;
                break;
            }
            case AdvancedMonitor:
            {
                meta = 12;
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
            case Monitor:
            case AdvancedMonitor:
            {
                EnumFacing front;
                int xIndex, yIndex, width, height;
                if( tile instanceof TileMonitor )
                {
                    TileMonitor monitor = (TileMonitor) tile;
                    dir = monitor.getDirection();
                    front = monitor.getFront();
                    xIndex = monitor.getXIndex();
                    yIndex = monitor.getYIndex();
                    width = monitor.getWidth();
                    height = monitor.getHeight();
                }
                else
                {
                    dir = EnumFacing.NORTH;
                    front = EnumFacing.NORTH;
                    xIndex = 0;
                    yIndex = 0;
                    width = 1;
                    height = 1;
                }

                BlockPeripheralVariant baseVariant;
                if( front == EnumFacing.UP )
                {
                    baseVariant = (type == PeripheralType.AdvancedMonitor) ?
                        BlockPeripheralVariant.AdvancedMonitorUp :
                        BlockPeripheralVariant.MonitorUp;
                }
                else if( front == EnumFacing.DOWN )
                {
                    baseVariant = (type == PeripheralType.AdvancedMonitor) ?
                        BlockPeripheralVariant.AdvancedMonitorDown :
                        BlockPeripheralVariant.MonitorDown;
                }
                else
                {
                    baseVariant = (type == PeripheralType.AdvancedMonitor) ?
                        BlockPeripheralVariant.AdvancedMonitor :
                        BlockPeripheralVariant.Monitor;
                }

                int subType;
                if( width == 1 && height == 1 )
                {
                    subType = 0;
                }
                else if( height == 1 )
                {
                    if( xIndex == 0 )
                    {
                        subType = 1;
                    }
                    else if( xIndex == width - 1 )
                    {
                        subType = 3;
                    }
                    else
                    {
                        subType = 2;
                    }
                }
                else if( width == 1 )
                {
                    if( yIndex == 0 )
                    {
                        subType = 6;
                    }
                    else if( yIndex == height - 1 )
                    {
                        subType = 4;
                    }
                    else
                    {
                        subType = 5;
                    }
                }
                else
                {
                    if( xIndex == 0 )
                    {
                        subType = 7;
                    }
                    else if( xIndex == width - 1 )
                    {
                        subType = 9;
                    }
                    else
                    {
                        subType = 8;
                    }
                    if( yIndex == 0 )
                    {
                        subType += 6;
                    }
                    else if( yIndex < height - 1 )
                    {
                        subType += 3;
                    }
                }

                state = state.withProperty( FACING, dir );
                state = state.withProperty( VARIANT,
                    BlockPeripheralVariant.values()[baseVariant.ordinal() + subType]
                );
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
            case Monitor:
            {
                return getDefaultState().withProperty( VARIANT, BlockPeripheralVariant.Monitor );
            }
            case Printer:
            {
                return getDefaultState().withProperty( VARIANT, BlockPeripheralVariant.PrinterEmpty );
            }
            case AdvancedMonitor:
            {
                return getDefaultState().withProperty( VARIANT, BlockPeripheralVariant.AdvancedMonitor );
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
            case Monitor:
            case AdvancedMonitor:
                return new TileMonitor();
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
            case Monitor:
            case AdvancedMonitor:
            {
                if( tile instanceof TileMonitor )
                {
                    int direction = DirectionUtil.fromEntityRot( player ).getIndex();
                    if( player.rotationPitch > 66.5F )
                    {
                        direction += 12;
                    }
                    else if( player.rotationPitch < -66.5F )
                    {
                        direction += 6;
                    }

                    TileMonitor monitor = (TileMonitor) tile;
                    if( world.isRemote )
                    {
                        monitor.setDir( direction );
                    }
                    else
                    {
                        monitor.contractNeighbours();
                        monitor.setDir( direction );
                        monitor.contract();
                        monitor.expand();
                    }
                }
                break;
            }
        }
    }

    @Override
    @Deprecated
    public final boolean isOpaqueCube( IBlockState state )
    {
        PeripheralType type = getPeripheralType( state );
        return type == PeripheralType.Monitor || type == PeripheralType.AdvancedMonitor;
    }

    @Override
    @Deprecated
    public final boolean isFullCube( IBlockState state )
    {
        return isOpaqueCube( state );
    }

    @Override
    @Deprecated
    public boolean isFullBlock( IBlockState state )
    {
        return isOpaqueCube( state );
    }

    @Nonnull
    @Override
    @Deprecated
    public BlockFaceShape getBlockFaceShape( IBlockAccess world, IBlockState state, BlockPos pos, EnumFacing side )
    {
        return isOpaqueCube( state ) ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @Override
    @Deprecated
    public boolean causesSuffocation( IBlockState state )
    {
        // This normally uses the default state 
        return material.blocksMovement() && state.isOpaqueCube();
    }

    @Override
    @Deprecated
    public int getLightOpacity( IBlockState state )
    {
        // This normally uses the default state
        return isOpaqueCube( state ) ? 255 : 0;
    }
}
