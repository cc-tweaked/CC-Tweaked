/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
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
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
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
        Direction direction = m_direction.toWorldDir( turtle );
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
            else if( stack.getItem() instanceof BlockItem )
            {
                return TurtleCommandResult.failure( "Cannot place block here" );
            }
            else
            {
                return TurtleCommandResult.failure( "Cannot place item here" );
            }
        }
    }

    public static ItemStack deploy( @Nonnull ItemStack stack, ITurtleAccess turtle, Direction direction, Object[] extraArguments, String[] outErrorMessage )
    {
        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition().offset( direction );
        TurtlePlayer turtlePlayer = createPlayer( turtle, playerPosition, direction );

        return deploy( stack, turtle, turtlePlayer, direction, extraArguments, outErrorMessage );
    }

    public static ItemStack deploy( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, Direction direction, Object[] extraArguments, String[] outErrorMessage )
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

        if( direction.getAxis() != Direction.Axis.Y )
        {
            // Deploy down on the block in front
            remainder = deployOnBlock( stack, turtle, turtlePlayer, newPosition.down(), Direction.UP, extraArguments, false, outErrorMessage );
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

    public static TurtlePlayer createPlayer( ITurtleAccess turtle, BlockPos position, Direction direction )
    {
        TurtlePlayer turtlePlayer = TurtlePlayer.get( turtle );
        orientPlayer( turtle, turtlePlayer, position, direction );
        return turtlePlayer;
    }

    private static void orientPlayer( ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, Direction direction )
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

        if( direction.getAxis() != Direction.Axis.Y )
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
    private static ItemStack deployOnEntity( @Nonnull ItemStack stack, final ITurtleAccess turtle, TurtlePlayer turtlePlayer, Direction direction, Object[] extraArguments, String[] outErrorMessage )
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
        ActionResultType cancelResult = ForgeHooks.onInteractEntityAt( turtlePlayer, hitEntity, hitPos, Hand.MAIN_HAND );
        if( cancelResult == null )
        {
            cancelResult = hitEntity.applyPlayerInteraction( turtlePlayer, hitPos, Hand.MAIN_HAND );
        }

        if( cancelResult == ActionResultType.SUCCESS )
        {
            placed = true;
        }
        else
        {
            // See EntityPlayer.interactOn
            cancelResult = ForgeHooks.onInteractEntity( turtlePlayer, hitEntity, Hand.MAIN_HAND );
            if( cancelResult == ActionResultType.SUCCESS )
            {
                placed = true;
            }
            else if( cancelResult == null )
            {
                if( hitEntity.processInitialInteract( turtlePlayer, Hand.MAIN_HAND ) )
                {
                    placed = true;
                }
                else if( hitEntity instanceof LivingEntity )
                {
                    placed = stackCopy.interactWithEntity( turtlePlayer, (LivingEntity) hitEntity, Hand.MAIN_HAND );
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

    private static boolean canDeployOnBlock( @Nonnull BlockItemUseContext context, ITurtleAccess turtle, TurtlePlayer player, BlockPos position, Direction side, boolean allowReplaceable, String[] outErrorMessage )
    {
        World world = turtle.getWorld();
        if( !World.isValid( position ) || world.isAirBlock( position ) ||
            (context.getItem().getItem() instanceof BlockItem && WorldUtil.isLiquidBlock( world, position )) )
        {
            return false;
        }

        BlockState state = world.getBlockState( position );

        boolean replaceable = state.isReplaceable( context );
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

        return true;
    }

    @Nonnull
    private static ItemStack deployOnBlock( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, Direction side, Object[] extraArguments, boolean allowReplace, String[] outErrorMessage )
    {
        // Re-orient the fake player
        Direction playerDir = side.getOpposite();
        BlockPos playerPosition = position.offset( side );
        orientPlayer( turtle, turtlePlayer, playerPosition, playerDir );

        ItemStack stackCopy = stack.copy();
        turtlePlayer.loadInventory( stackCopy );

        // Calculate where the turtle would hit the block
        float hitX = 0.5f + side.getXOffset() * 0.5f;
        float hitY = 0.5f + side.getYOffset() * 0.5f;
        float hitZ = 0.5f + side.getZOffset() * 0.5f;
        if( Math.abs( hitY - 0.5f ) < 0.01f )
        {
            hitY = 0.45f;
        }

        // Check if there's something suitable to place onto
        BlockRayTraceResult hit = new BlockRayTraceResult( new Vec3d( hitX, hitY, hitZ ), side, position, false );
        ItemUseContext context = new ItemUseContext( turtlePlayer, Hand.MAIN_HAND, hit );
        if( !canDeployOnBlock( new BlockItemUseContext( context ), turtle, turtlePlayer, position, side, allowReplace, outErrorMessage ) )
        {
            return stack;
        }

        // Load up the turtle's inventory
        Item item = stack.getItem();

        // Do the deploying (put everything in the players inventory)
        boolean placed = false;
        TileEntity existingTile = turtle.getWorld().getTileEntity( position );

        // See PlayerInteractionManager.processRightClickBlock
        // TODO: ^ Check we're still consistent.
        PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock( turtlePlayer, Hand.MAIN_HAND, position, side );
        if( !event.isCanceled() )
        {
            if( item.onItemUseFirst( stack, context ) == ActionResultType.SUCCESS )
            {
                placed = true;
                turtlePlayer.loadInventory( stackCopy );
            }
            else if( event.getUseItem() != Event.Result.DENY &&
                stackCopy.onItemUse( context ) == ActionResultType.SUCCESS )
            {
                placed = true;
                turtlePlayer.loadInventory( stackCopy );
            }
        }

        if( !placed && (item instanceof BucketItem || item instanceof BoatItem || item instanceof LilyPadItem || item instanceof GlassBottleItem) )
        {
            ActionResultType actionResult = ForgeHooks.onItemRightClick( turtlePlayer, Hand.MAIN_HAND );
            if( actionResult == ActionResultType.SUCCESS )
            {
                placed = true;
            }
            else if( actionResult == null )
            {
                ActionResult<ItemStack> result = stackCopy.useItemRightClick( turtle.getWorld(), turtlePlayer, Hand.MAIN_HAND );
                if( result.getType() == ActionResultType.SUCCESS && !ItemStack.areItemStacksEqual( stack, result.getResult() ) )
                {
                    placed = true;
                    turtlePlayer.loadInventory( result.getResult() );
                }
            }
        }

        // Set text on signs
        if( placed && item instanceof SignItem )
        {
            if( extraArguments != null && extraArguments.length >= 1 && extraArguments[0] instanceof String )
            {
                World world = turtle.getWorld();
                TileEntity tile = world.getTileEntity( position );
                if( tile == null || tile == existingTile )
                {
                    tile = world.getTileEntity( position.offset( side ) );
                }
                if( tile instanceof SignTileEntity )
                {
                    SignTileEntity signTile = (SignTileEntity) tile;
                    String s = (String) extraArguments[0];
                    String[] split = s.split( "\n" );
                    int firstLine = split.length <= 2 ? 1 : 0;
                    for( int i = 0; i < signTile.signText.length; i++ )
                    {
                        if( i >= firstLine && i < firstLine + split.length )
                        {
                            if( split[i - firstLine].length() > 15 )
                            {
                                signTile.signText[i] = new StringTextComponent( split[i - firstLine].substring( 0, 15 ) );
                            }
                            else
                            {
                                signTile.signText[i] = new StringTextComponent( split[i - firstLine] );
                            }
                        }
                        else
                        {
                            signTile.signText[i] = new StringTextComponent( "" );
                        }
                    }
                    signTile.markDirty();
                    world.notifyBlockUpdate( tile.getPos(), tile.getBlockState(), tile.getBlockState(), 3 );
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
