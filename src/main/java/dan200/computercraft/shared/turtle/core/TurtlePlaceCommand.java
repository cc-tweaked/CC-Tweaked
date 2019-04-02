/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;

public class TurtlePlaceCommand implements ITurtleCommand
{
    private final InteractDirection m_direction;
    private final Object[] m_extraArguments;

    public TurtlePlaceCommand( InteractDirection direction, Object[] arguments )
    {
        m_direction = direction;
        m_extraArguments = arguments;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Get thing to place
        ItemStack stack = turtle.getInventory().getStackInSlot( turtle.getSelectedSlot() );
        if( stack.isEmpty() )
        {
            return TurtleCommandResult.failure( "No items to place" );
        }

        // Remember old block
        EnumFacing direction = m_direction.toWorldDir( turtle );
        World world = turtle.getWorld();
        BlockPos coordinates = turtle.getPosition().offset( direction );

        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition().offset( direction );
        TurtlePlayer turtlePlayer = createPlayer( turtle, playerPosition, direction );

        TurtleBlockEvent.Place place = new TurtleBlockEvent.Place( turtle, turtlePlayer, turtle.getWorld(), coordinates, stack );
        if( MinecraftForge.EVENT_BUS.post( place ) )
        {
            return TurtleCommandResult.failure( place.getFailureMessage() );
        }

        // Do the deploying
        String[] errorMessage = new String[1];
        ItemStack remainder = deploy( stack, turtle, turtlePlayer, direction, m_extraArguments, errorMessage );
        if( remainder != stack )
        {
            // Put the remaining items back
            turtle.getInventory().setInventorySlotContents( turtle.getSelectedSlot(), remainder );
            turtle.getInventory().markDirty();

            // Animate and return success
            turtle.playAnimation( TurtleAnimation.Wait );
            return TurtleCommandResult.success();
        }
        else
        {
            if( errorMessage[0] != null )
            {
                return TurtleCommandResult.failure( errorMessage[0] );
            }
            else if( stack.getItem() instanceof ItemBlock )
            {
                return TurtleCommandResult.failure( "Cannot place block here" );
            }
            else
            {
                return TurtleCommandResult.failure( "Cannot place item here" );
            }
        }
    }

    public static ItemStack deploy( @Nonnull ItemStack stack, ITurtleAccess turtle, EnumFacing direction, Object[] extraArguments, String[] outErrorMessage )
    {
        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition().offset( direction );
        TurtlePlayer turtlePlayer = createPlayer( turtle, playerPosition, direction );

        return deploy( stack, turtle, turtlePlayer, direction, extraArguments, outErrorMessage );
    }

    public static ItemStack deploy( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, EnumFacing direction, Object[] extraArguments, String[] outErrorMessage )
    {
        // Deploy on an entity
        ItemStack remainder = deployOnEntity( stack, turtle, turtlePlayer, direction, extraArguments, outErrorMessage );
        if( remainder != stack )
        {
            return remainder;
        }

        // Deploy on the block immediately in front
        BlockPos position = turtle.getPosition();
        BlockPos newPosition = position.offset( direction );
        remainder = deployOnBlock( stack, turtle, turtlePlayer, newPosition, direction.getOpposite(), extraArguments, true, outErrorMessage );
        if( remainder != stack )
        {
            return remainder;
        }

        // Deploy on the block one block away
        remainder = deployOnBlock( stack, turtle, turtlePlayer, newPosition.offset( direction ), direction.getOpposite(), extraArguments, false, outErrorMessage );
        if( remainder != stack )
        {
            return remainder;
        }

        if( direction.getAxis() != EnumFacing.Axis.Y )
        {
            // Deploy down on the block in front
            remainder = deployOnBlock( stack, turtle, turtlePlayer, newPosition.down(), EnumFacing.UP, extraArguments, false, outErrorMessage );
            if( remainder != stack )
            {
                return remainder;
            }
        }

        // Deploy back onto the turtle
        remainder = deployOnBlock( stack, turtle, turtlePlayer, position, direction, extraArguments, false, outErrorMessage );
        if( remainder != stack )
        {
            return remainder;
        }

        // If nothing worked, return the original stack unchanged
        return stack;
    }

    public static TurtlePlayer createPlayer( ITurtleAccess turtle, BlockPos position, EnumFacing direction )
    {
        TurtlePlayer turtlePlayer = TurtlePlayer.get( turtle );
        orientPlayer( turtle, turtlePlayer, position, direction );
        return turtlePlayer;
    }

    private static void orientPlayer( ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, EnumFacing direction )
    {
        turtlePlayer.posX = position.getX() + 0.5;
        turtlePlayer.posY = position.getY() + 0.5;
        turtlePlayer.posZ = position.getZ() + 0.5;

        // Stop intersection with the turtle itself
        if( turtle.getPosition().equals( position ) )
        {
            turtlePlayer.posX += 0.48 * direction.getXOffset();
            turtlePlayer.posY += 0.48 * direction.getYOffset();
            turtlePlayer.posZ += 0.48 * direction.getZOffset();
        }

        if( direction.getAxis() != EnumFacing.Axis.Y )
        {
            turtlePlayer.rotationYaw = direction.getHorizontalAngle();
            turtlePlayer.rotationPitch = 0.0f;
        }
        else
        {
            turtlePlayer.rotationYaw = turtle.getDirection().getHorizontalAngle();
            turtlePlayer.rotationPitch = DirectionUtil.toPitchAngle( direction );
        }

        turtlePlayer.prevPosX = turtlePlayer.posX;
        turtlePlayer.prevPosY = turtlePlayer.posY;
        turtlePlayer.prevPosZ = turtlePlayer.posZ;
        turtlePlayer.prevRotationPitch = turtlePlayer.rotationPitch;
        turtlePlayer.prevRotationYaw = turtlePlayer.rotationYaw;

        turtlePlayer.rotationYawHead = turtlePlayer.rotationYaw;
        turtlePlayer.prevRotationYawHead = turtlePlayer.rotationYawHead;
    }

    @Nonnull
    private static ItemStack deployOnEntity( @Nonnull ItemStack stack, final ITurtleAccess turtle, TurtlePlayer turtlePlayer, EnumFacing direction, Object[] extraArguments, String[] outErrorMessage )
    {
        // See if there is an entity present
        final World world = turtle.getWorld();
        final BlockPos position = turtle.getPosition();
        Vec3d turtlePos = new Vec3d( turtlePlayer.posX, turtlePlayer.posY, turtlePlayer.posZ );
        Vec3d rayDir = turtlePlayer.getLook( 1.0f );
        Pair<Entity, Vec3d> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit == null )
        {
            return stack;
        }

        // Load up the turtle's inventory
        ItemStack stackCopy = stack.copy();
        turtlePlayer.loadInventory( stackCopy );

        // Start claiming entity drops
        Entity hitEntity = hit.getKey();
        Vec3d hitPos = hit.getValue();
        DropConsumer.set(
            hitEntity,
            drop -> InventoryUtil.storeItems( drop, turtle.getItemHandler(), turtle.getSelectedSlot() )
        );

        // Place on the entity
        boolean placed = false;
        EnumActionResult cancelResult = ForgeHooks.onInteractEntityAt( turtlePlayer, hitEntity, hitPos, EnumHand.MAIN_HAND );
        if( cancelResult == null )
        {
            cancelResult = hitEntity.applyPlayerInteraction( turtlePlayer, hitPos, EnumHand.MAIN_HAND );
        }

        if( cancelResult == EnumActionResult.SUCCESS )
        {
            placed = true;
        }
        else
        {
            // See EntityPlayer.interactOn
            cancelResult = ForgeHooks.onInteractEntity( turtlePlayer, hitEntity, EnumHand.MAIN_HAND );
            if( cancelResult == EnumActionResult.SUCCESS )
            {
                placed = true;
            }
            else if( cancelResult == null )
            {
                if( hitEntity.processInitialInteract( turtlePlayer, EnumHand.MAIN_HAND ) )
                {
                    placed = true;
                }
                else if( hitEntity instanceof EntityLivingBase )
                {
                    placed = stackCopy.interactWithEntity( turtlePlayer, (EntityLivingBase) hitEntity, EnumHand.MAIN_HAND );
                    if( placed ) turtlePlayer.loadInventory( stackCopy );
                }
            }
        }

        // Stop claiming drops
        List<ItemStack> remainingDrops = DropConsumer.clear();
        for( ItemStack remaining : remainingDrops )
        {
            WorldUtil.dropItemStack( remaining, world, position, turtle.getDirection().getOpposite() );
        }

        // Put everything we collected into the turtles inventory, then return
        ItemStack remainder = turtlePlayer.unloadInventory( turtle );
        if( !placed && ItemStack.areItemStacksEqual( stack, remainder ) )
        {
            return stack;
        }
        else if( !remainder.isEmpty() )
        {
            return remainder;
        }
        else
        {
            return ItemStack.EMPTY;
        }
    }

    private static boolean canDeployOnBlock( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer player, BlockPos position, EnumFacing side, boolean allowReplaceable, String[] outErrorMessage )
    {
        World world = turtle.getWorld();
        if( !world.isValid( position ) || world.isAirBlock( position ) ||
            (stack.getItem() instanceof ItemBlock && WorldUtil.isLiquidBlock( world, position )) )
        {
            return false;
        }

        Block block = world.getBlockState( position ).getBlock();
        IBlockState state = world.getBlockState( position );

        boolean replaceable = block.isReplaceable( world, position );
        if( !allowReplaceable && replaceable ) return false;

        if( ComputerCraft.turtlesObeyBlockProtection )
        {
            // Check spawn protection
            boolean editable = replaceable
                ? TurtlePermissions.isBlockEditable( world, position, player )
                : TurtlePermissions.isBlockEditable( world, position.offset( side ), player );
            if( !editable )
            {
                if( outErrorMessage != null ) outErrorMessage[0] = "Cannot place in protected area";
                return false;
            }
        }

        // Check the block is solid or liquid
        return block.canCollideCheck( state, true );
    }

    @Nonnull
    private static ItemStack deployOnBlock( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, EnumFacing side, Object[] extraArguments, boolean allowReplace, String[] outErrorMessage )
    {
        // Re-orient the fake player
        EnumFacing playerDir = side.getOpposite();
        BlockPos playerPosition = position.offset( side );
        orientPlayer( turtle, turtlePlayer, playerPosition, playerDir );

        // Check if there's something suitable to place onto
        if( !canDeployOnBlock( stack, turtle, turtlePlayer, position, side, allowReplace, outErrorMessage ) )
        {
            return stack;
        }

        // Calculate where the turtle would hit the block
        float hitX = 0.5f + side.getXOffset() * 0.5f;
        float hitY = 0.5f + side.getYOffset() * 0.5f;
        float hitZ = 0.5f + side.getZOffset() * 0.5f;
        if( Math.abs( hitY - 0.5f ) < 0.01f )
        {
            hitY = 0.45f;
        }

        // Load up the turtle's inventory
        Item item = stack.getItem();
        ItemStack stackCopy = stack.copy();
        turtlePlayer.loadInventory( stackCopy );

        // Do the deploying (put everything in the players inventory)
        boolean placed = false;
        TileEntity existingTile = turtle.getWorld().getTileEntity( position );

        // See PlayerInteractionManager.processRightClickBlock
        PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock( turtlePlayer, EnumHand.MAIN_HAND, position, side, new Vec3d( hitX, hitY, hitZ ) );
        if( !event.isCanceled() )
        {
            if( item.onItemUseFirst( turtlePlayer, turtle.getWorld(), position, side, hitX, hitY, hitZ, EnumHand.MAIN_HAND ) == EnumActionResult.SUCCESS )
            {
                placed = true;
                turtlePlayer.loadInventory( stackCopy );
            }
            else if( event.getUseItem() != Event.Result.DENY &&
                stackCopy.onItemUse( turtlePlayer, turtle.getWorld(), position, EnumHand.MAIN_HAND, side, hitX, hitY, hitZ ) == EnumActionResult.SUCCESS )
            {
                placed = true;
                turtlePlayer.loadInventory( stackCopy );
            }
        }

        if( !placed && (item instanceof ItemBucket || item instanceof ItemBoat || item instanceof ItemLilyPad || item instanceof ItemGlassBottle) )
        {
            EnumActionResult actionResult = ForgeHooks.onItemRightClick( turtlePlayer, EnumHand.MAIN_HAND );
            if( actionResult == EnumActionResult.SUCCESS )
            {
                placed = true;
            }
            else if( actionResult == null )
            {
                ActionResult<ItemStack> result = stackCopy.useItemRightClick( turtle.getWorld(), turtlePlayer, EnumHand.MAIN_HAND );
                if( result.getType() == EnumActionResult.SUCCESS && !ItemStack.areItemStacksEqual( stack, result.getResult() ) )
                {
                    placed = true;
                    turtlePlayer.loadInventory( result.getResult() );
                }
            }
        }

        // Set text on signs
        if( placed && item instanceof ItemSign )
        {
            if( extraArguments != null && extraArguments.length >= 1 && extraArguments[0] instanceof String )
            {
                World world = turtle.getWorld();
                TileEntity tile = world.getTileEntity( position );
                if( tile == null || tile == existingTile )
                {
                    tile = world.getTileEntity( position.offset( side ) );
                }
                if( tile instanceof TileEntitySign )
                {
                    TileEntitySign signTile = (TileEntitySign) tile;
                    String s = (String) extraArguments[0];
                    String[] split = s.split( "\n" );
                    int firstLine = split.length <= 2 ? 1 : 0;
                    for( int i = 0; i < signTile.signText.length; i++ )
                    {
                        if( i >= firstLine && i < firstLine + split.length )
                        {
                            if( split[i - firstLine].length() > 15 )
                            {
                                signTile.signText[i] = new TextComponentString( split[i - firstLine].substring( 0, 15 ) );
                            }
                            else
                            {
                                signTile.signText[i] = new TextComponentString( split[i - firstLine] );
                            }
                        }
                        else
                        {
                            signTile.signText[i] = new TextComponentString( "" );
                        }
                    }
                    signTile.markDirty();
                    world.markBlockRangeForRenderUpdate( signTile.getPos(), signTile.getPos() );
                }
            }
        }

        // Put everything we collected into the turtles inventory, then return
        ItemStack remainder = turtlePlayer.unloadInventory( turtle );
        if( !placed && ItemStack.areItemStacksEqual( stack, remainder ) )
        {
            return stack;
        }
        else if( !remainder.isEmpty() )
        {
            return remainder;
        }
        else
        {
            return ItemStack.EMPTY;
        }
    }
}
