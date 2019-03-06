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
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.SignBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.item.block.BlockItem;
import net.minecraft.item.block.LilyPadItem;
import net.minecraft.item.block.SignItem;
import net.minecraft.text.StringTextComponent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
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
        ItemStack stack = turtle.getInventory().getInvStack( turtle.getSelectedSlot() );
        if( stack.isEmpty() )
        {
            return TurtleCommandResult.failure( "No items to place" );
        }

        // Remember old block
        Direction direction = m_direction.toWorldDir( turtle );
        World world = turtle.getWorld();
        BlockPos coordinates = turtle.getPosition().offset( direction );

        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition().offset( direction );
        TurtlePlayer turtlePlayer = createPlayer( turtle, playerPosition, direction );

        TurtleBlockEvent.Place place = new TurtleBlockEvent.Place( turtle, turtlePlayer, turtle.getWorld(), coordinates, stack );
        if( TurtleEvent.post( place ) )
        {
            return TurtleCommandResult.failure( place.getFailureMessage() );
        }

        // Do the deploying
        String[] errorMessage = new String[1];
        ItemStack remainder = deploy( stack, turtle, turtlePlayer, direction, m_extraArguments, errorMessage );
        if( remainder != stack )
        {
            // Put the remaining items back
            turtle.getInventory().setInvStack( turtle.getSelectedSlot(), remainder );
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

    public static ItemStack deploy( @Nonnull ItemStack stack, ITurtleAccess turtle, Direction direction, Object[] extraArguments, String[] o_errorMessage )
    {
        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition().offset( direction );
        TurtlePlayer turtlePlayer = createPlayer( turtle, playerPosition, direction );

        return deploy( stack, turtle, turtlePlayer, direction, extraArguments, o_errorMessage );
    }

    public static ItemStack deploy( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, Direction direction, Object[] extraArguments, String[] o_errorMessage )
    {
        // Deploy on an entity
        ItemStack remainder = deployOnEntity( stack, turtle, turtlePlayer, direction, extraArguments, o_errorMessage );
        if( remainder != stack )
        {
            return remainder;
        }

        // Deploy on the block immediately in front
        BlockPos position = turtle.getPosition();
        BlockPos newPosition = position.offset( direction );
        remainder = deployOnBlock( stack, turtle, turtlePlayer, newPosition, direction.getOpposite(), extraArguments, true, o_errorMessage );
        if( remainder != stack )
        {
            return remainder;
        }

        // Deploy on the block one block away
        remainder = deployOnBlock( stack, turtle, turtlePlayer, newPosition.offset( direction ), direction.getOpposite(), extraArguments, false, o_errorMessage );
        if( remainder != stack )
        {
            return remainder;
        }

        if( direction.getAxis() != Direction.Axis.Y )
        {
            // Deploy down on the block in front
            remainder = deployOnBlock( stack, turtle, turtlePlayer, newPosition.down(), Direction.UP, extraArguments, false, o_errorMessage );
            if( remainder != stack )
            {
                return remainder;
            }
        }

        // Deploy back onto the turtle
        remainder = deployOnBlock( stack, turtle, turtlePlayer, position, direction, extraArguments, false, o_errorMessage );
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
        turtlePlayer.x = position.getX() + 0.5;
        turtlePlayer.y = position.getY() + 0.5;
        turtlePlayer.z = position.getZ() + 0.5;

        // Stop intersection with the turtle itself
        if( turtle.getPosition().equals( position ) )
        {
            turtlePlayer.x += 0.48 * direction.getOffsetX();
            turtlePlayer.y += 0.48 * direction.getOffsetY();
            turtlePlayer.z += 0.48 * direction.getOffsetZ();
        }

        if( direction.getAxis() != Direction.Axis.Y )
        {
            turtlePlayer.yaw = direction.asRotation();
            turtlePlayer.pitch = 0.0f;
        }
        else
        {
            turtlePlayer.yaw = turtle.getDirection().asRotation();
            turtlePlayer.pitch = DirectionUtil.toPitchAngle( direction );
        }

        turtlePlayer.prevX = turtlePlayer.x;
        turtlePlayer.prevY = turtlePlayer.y;
        turtlePlayer.prevZ = turtlePlayer.z;
        turtlePlayer.prevPitch = turtlePlayer.pitch;
        turtlePlayer.prevYaw = turtlePlayer.yaw;

        turtlePlayer.headYaw = turtlePlayer.yaw;
        turtlePlayer.prevHeadYaw = turtlePlayer.yaw;
    }

    @Nonnull
    private static ItemStack deployOnEntity( @Nonnull ItemStack stack, final ITurtleAccess turtle, TurtlePlayer turtlePlayer, Direction direction, Object[] extraArguments, String[] o_errorMessage )
    {
        // See if there is an entity present
        final World world = turtle.getWorld();
        final BlockPos position = turtle.getPosition();
        Vec3d turtlePos = new Vec3d( turtlePlayer.x, turtlePlayer.y, turtlePlayer.z );
        Vec3d rayDir = turtlePlayer.getRotationVec( 1.0f );
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
            drop -> InventoryUtil.storeItems( drop, ItemStorage.wrap( turtle.getInventory() ), turtle.getSelectedSlot() )
        );

        // Place on the entity
        boolean placed = false;
        ActionResult cancelResult = null; // ForgeHooks.onInteractEntityAt( turtlePlayer, hitEntity, hitPos, Hand.MAIN );
        if( cancelResult == null )
        {
            cancelResult = hitEntity.interactAt( turtlePlayer, hitPos, Hand.MAIN );
        }

        if( cancelResult == ActionResult.SUCCESS )
        {
            placed = true;
        }
        else
        {
            // See PlayerEntity.interactOn
            // cancelResult = ForgeHooks.onInteractEntity( turtlePlayer, hitEntity, Hand.MAIN );
            if( cancelResult == ActionResult.SUCCESS )
            {
                placed = true;
            }
            else if( cancelResult == null )
            {
                if( hitEntity.interact( turtlePlayer, Hand.MAIN ) )
                {
                    placed = true;
                }
                else if( hitEntity instanceof LivingEntity )
                {
                    placed = stackCopy.interactWithEntity( turtlePlayer, (LivingEntity) hitEntity, Hand.MAIN );
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
        if( !placed && ItemStack.areEqual( stack, remainder ) )
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

    private static boolean canDeployOnBlock( @Nonnull ItemPlacementContext context, ITurtleAccess turtle, TurtlePlayer player, BlockPos position, Direction side, boolean allowReplaceable, String[] o_errorMessage )
    {
        World world = turtle.getWorld();
        if( World.isValid( position ) &&
            !world.isAir( position ) &&
            !(context.getItemStack().getItem() instanceof BlockItem && WorldUtil.isLiquidBlock( world, position )) )
        {
            BlockState state = world.getBlockState( position );
            Block block = state.getBlock();

            boolean replaceable = state.method_11587( context );
            if( allowReplaceable || !replaceable )
            {
                if( ComputerCraft.turtlesObeyBlockProtection )
                {
                    // Check spawn protection
                    boolean editable = replaceable
                        ? TurtlePermissions.isBlockEditable( world, position, player )
                        : TurtlePermissions.isBlockEditable( world, position.offset( side ), player );
                    if( !editable )
                    {
                        if( o_errorMessage != null )
                        {
                            o_errorMessage[0] = "Cannot place in protected area";
                        }
                        return false;
                    }
                }

                // Check the block is solid or liquid
                if( !state.getCollisionShape( world, position ).isEmpty() )
                {
                    return true;
                }
            }
        }
        return false;
    }

    @Nonnull
    private static ItemStack deployOnBlock( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, Direction side, Object[] extraArguments, boolean allowReplace, String[] o_errorMessage )
    {
        // Re-orient the fake player
        Direction playerDir = side.getOpposite();
        BlockPos playerPosition = position.offset( side );
        orientPlayer( turtle, turtlePlayer, playerPosition, playerDir );

        ItemStack stackCopy = stack.copy();
        turtlePlayer.loadInventory( stackCopy );

        // Calculate where the turtle would hit the block
        float hitX = 0.5f + side.getOffsetX() * 0.5f;
        float hitY = 0.5f + side.getOffsetY() * 0.5f;
        float hitZ = 0.5f + side.getOffsetZ() * 0.5f;
        if( Math.abs( hitY - 0.5f ) < 0.01f )
        {
            hitY = 0.45f;
        }

        // Check if there's something suitable to place onto
        ItemUsageContext context = new ItemUsageContext( turtlePlayer, stackCopy, new BlockHitResult( new Vec3d( hitX, hitY, hitZ ), side, position, false ) );
        if( !canDeployOnBlock( new ItemPlacementContext( context ), turtle, turtlePlayer, position, side, allowReplace, o_errorMessage ) )
        {
            return stack;
        }

        // Load up the turtle's inventory
        Item item = stack.getItem();

        // Do the deploying (put everything in the players inventory)
        boolean placed = false;
        BlockEntity existingTile = turtle.getWorld().getBlockEntity( position );

        // See PlayerInteractionManager.processRightClickBlock
        /*
        PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock( turtlePlayer, Hand.MAIN, position, side, new Vec3d( hitX, hitY, hitZ ) );
        if( !event.isCanceled() ) */
        {
            /* if( item.onItemUseFirst( turtlePlayer, turtle.getWorld(), position, side, hitX, hitY, hitZ, Hand.MAIN ) == ActionResult.SUCCESS )
            {
                placed = true;
                turtlePlayer.loadInventory( stackCopy );
            }
            else*/
            if( /* event.getUseItem() != Event.Result.DENY && */
                stackCopy.useOnBlock( context ) == ActionResult.SUCCESS )
            {
                placed = true;
                turtlePlayer.loadInventory( stackCopy );
            }
        }

        if( !placed && (item instanceof BucketItem || item instanceof BoatItem || item instanceof LilyPadItem || item instanceof GlassBottleItem) )
        {
            ActionResult actionResult = null; // ForgeHooks.onItemRightClick( turtlePlayer, Hand.MAIN );
            if( actionResult == ActionResult.SUCCESS )
            {
                placed = true;
            }
            else if( actionResult == null )
            {
                TypedActionResult<ItemStack> result = stackCopy.use( turtle.getWorld(), turtlePlayer, Hand.MAIN );
                if( result.getResult() == ActionResult.SUCCESS && !ItemStack.areEqual( stack, result.getValue() ) )
                {
                    placed = true;
                    turtlePlayer.loadInventory( result.getValue() );
                }
            }
        }

        // Set text on signs
        if( placed && item instanceof SignItem )
        {
            if( extraArguments != null && extraArguments.length >= 1 && extraArguments[0] instanceof String )
            {
                World world = turtle.getWorld();
                BlockEntity tile = world.getBlockEntity( position );
                if( tile == null || tile == existingTile )
                {
                    tile = world.getBlockEntity( position.offset( side ) );
                }
                if( tile instanceof SignBlockEntity )
                {
                    SignBlockEntity signTile = (SignBlockEntity) tile;
                    String s = (String) extraArguments[0];
                    String[] split = s.split( "\n" );
                    int firstLine = (split.length <= 2) ? 1 : 0;
                    for( int i = 0; i < signTile.text.length; i++ )
                    {
                        if( i >= firstLine && i < firstLine + split.length )
                        {
                            if( split[i - firstLine].length() > 15 )
                            {
                                signTile.text[i] = new StringTextComponent( split[i - firstLine].substring( 0, 15 ) );
                            }
                            else
                            {
                                signTile.text[i] = new StringTextComponent( split[i - firstLine] );
                            }
                        }
                        else
                        {
                            signTile.text[i] = new StringTextComponent( "" );
                        }
                    }
                    signTile.markDirty();
                    world.scheduleBlockRender( signTile.getPos() ); // TODO: This doesn't do anything!
                }
            }
        }

        // Put everything we collected into the turtles inventory, then return
        ItemStack remainder = turtlePlayer.unloadInventory( turtle );
        if( !placed && ItemStack.areEqual( stack, remainder ) )
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
