/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleAccess;
import dan200.computercraft.api.turtle.ITurtleCommand;
import dan200.computercraft.api.turtle.TurtleAnimation;
import dan200.computercraft.api.turtle.TurtleCommandResult;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

public class TurtlePlaceCommand implements ITurtleCommand
{
    private final InteractDirection direction;
    private final Object[] extraArguments;

    public TurtlePlaceCommand( InteractDirection direction, Object[] arguments )
    {
        this.direction = direction;
        extraArguments = arguments;
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Get thing to place
        ItemStack stack = turtle.getInventory().getItem( turtle.getSelectedSlot() );
        if( stack.isEmpty() ) return TurtleCommandResult.failure( "No items to place" );

        // Remember old block
        Direction direction = this.direction.toWorldDir( turtle );
        BlockPos coordinates = turtle.getPosition().relative( direction );

        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition().relative( direction );
        TurtlePlayer turtlePlayer = TurtlePlayer.getWithPosition( turtle, playerPosition, direction );

        // Do the deploying
        turtlePlayer.loadInventory( turtle );
        ErrorMessage message = new ErrorMessage();
        boolean result = deploy( stack, turtle, turtlePlayer, direction, extraArguments, message );
        turtlePlayer.unloadInventory( turtle );
        if( result )
        {
            // Animate and return success
            turtle.playAnimation( TurtleAnimation.WAIT );
            return TurtleCommandResult.success();
        }
        else if( message.message != null )
        {
            return TurtleCommandResult.failure( message.message );
        }
        else
        {
            return TurtleCommandResult.failure( stack.getItem() instanceof BlockItem ? "Cannot place block here" : "Cannot place item here" );
        }
    }

    public static boolean deployCopiedItem( @Nonnull ItemStack stack, ITurtleAccess turtle, Direction direction, Object[] extraArguments, ErrorMessage outErrorMessage )
    {
        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition().relative( direction );
        TurtlePlayer turtlePlayer = TurtlePlayer.getWithPosition( turtle, playerPosition, direction );
        turtlePlayer.loadInventory( stack );
        boolean result = deploy( stack, turtle, turtlePlayer, direction, extraArguments, outErrorMessage );
        turtlePlayer.getInventory().clearContent();
        return result;
    }

    private static boolean deploy( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, Direction direction, Object[] extraArguments, ErrorMessage outErrorMessage )
    {
        // Deploy on an entity
        if( deployOnEntity( stack, turtle, turtlePlayer ) ) return true;

        BlockPos position = turtle.getPosition();
        BlockPos newPosition = position.relative( direction );

        // Try to deploy against a block. Tries the following options:
        //     Deploy on the block immediately in front
        return deployOnBlock( stack, turtle, turtlePlayer, newPosition, direction.getOpposite(), extraArguments, true, outErrorMessage )
            // Deploy on the block one block away
            || deployOnBlock( stack, turtle, turtlePlayer, newPosition.relative( direction ), direction.getOpposite(), extraArguments, false, outErrorMessage )
            // Deploy down on the block in front
            || (direction.getAxis() != Direction.Axis.Y && deployOnBlock( stack, turtle, turtlePlayer, newPosition.below(), Direction.UP, extraArguments, false, outErrorMessage ))
            // Deploy back onto the turtle
            || deployOnBlock( stack, turtle, turtlePlayer, position, direction, extraArguments, false, outErrorMessage );
    }

    private static boolean deployOnEntity( @Nonnull ItemStack stack, final ITurtleAccess turtle, TurtlePlayer turtlePlayer )
    {
        // See if there is an entity present
        final Level world = turtle.getLevel();
        final BlockPos position = turtle.getPosition();
        Vec3 turtlePos = turtlePlayer.position();
        Vec3 rayDir = turtlePlayer.getViewVector( 1.0f );
        Pair<Entity, Vec3> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit == null ) return false;

        // Start claiming entity drops
        Entity hitEntity = hit.getKey();
        Vec3 hitPos = hit.getValue();

        IItemHandler itemHandler = new InvWrapper( turtlePlayer.getInventory() );
        DropConsumer.set( hitEntity, drop -> InventoryUtil.storeItems( drop, itemHandler, 1 ) );

        boolean placed = doDeployOnEntity( stack, turtlePlayer, hitEntity, hitPos );

        DropConsumer.clearAndDrop( world, position, turtle.getDirection().getOpposite() );
        return placed;
    }

    /**
     * Place a block onto an entity. For instance, feeding cows.
     *
     * @param stack        The stack we're placing.
     * @param turtlePlayer The player of the turtle we're placing.
     * @param hitEntity    The entity we're interacting with.
     * @param hitPos       The position our ray trace hit the entity.
     * @return If this item was deployed.
     * @see net.minecraft.server.network.ServerGamePacketListenerImpl#handleInteract(ServerboundInteractPacket)
     * @see net.minecraft.world.entity.player.Player#interactOn(Entity, InteractionHand)
     */
    private static boolean doDeployOnEntity( @Nonnull ItemStack stack, TurtlePlayer turtlePlayer, @Nonnull Entity hitEntity, @Nonnull Vec3 hitPos )
    {
        // Placing "onto" a block follows two flows. First we try to interactAt. If that doesn't succeed, then we try to
        // call the normal interact path. Cancelling an interactAt *does not* cancel a normal interact path.

        InteractionResult interactAt = ForgeHooks.onInteractEntityAt( turtlePlayer, hitEntity, hitPos, InteractionHand.MAIN_HAND );
        if( interactAt == null ) interactAt = hitEntity.interactAt( turtlePlayer, hitPos, InteractionHand.MAIN_HAND );
        if( interactAt.consumesAction() ) return true;

        InteractionResult interact = ForgeHooks.onInteractEntity( turtlePlayer, hitEntity, InteractionHand.MAIN_HAND );
        if( interact != null ) return interact.consumesAction();

        if( hitEntity.interact( turtlePlayer, InteractionHand.MAIN_HAND ).consumesAction() ) return true;
        if( hitEntity instanceof LivingEntity hitLiving )
        {
            return stack.interactLivingEntity( turtlePlayer, hitLiving, InteractionHand.MAIN_HAND ).consumesAction();
        }

        return false;
    }

    private static boolean canDeployOnBlock(
        @Nonnull BlockPlaceContext context, ITurtleAccess turtle, TurtlePlayer player, BlockPos position,
        Direction side, boolean allowReplaceable, ErrorMessage outErrorMessage
    )
    {
        Level world = turtle.getLevel();
        if( !world.isInWorldBounds( position ) || world.isEmptyBlock( position ) ||
            (context.getItemInHand().getItem() instanceof BlockItem && WorldUtil.isLiquidBlock( world, position )) )
        {
            return false;
        }

        BlockState state = world.getBlockState( position );

        boolean replaceable = state.canBeReplaced( context );
        if( !allowReplaceable && replaceable ) return false;

        if( ComputerCraft.turtlesObeyBlockProtection )
        {
            // Check spawn protection
            boolean editable = replaceable
                ? TurtlePermissions.isBlockEditable( world, position, player )
                : TurtlePermissions.isBlockEditable( world, position.relative( side ), player );
            if( !editable )
            {
                if( outErrorMessage != null ) outErrorMessage.message = "Cannot place in protected area";
                return false;
            }
        }

        return true;
    }

    private static boolean deployOnBlock(
        @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, Direction side,
        Object[] extraArguments, boolean allowReplace, ErrorMessage outErrorMessage
    )
    {
        // Re-orient the fake player
        Direction playerDir = side.getOpposite();
        BlockPos playerPosition = position.relative( side );
        turtlePlayer.setPosition( turtle, playerPosition, playerDir );

        // Calculate where the turtle would hit the block
        float hitX = 0.5f + side.getStepX() * 0.5f;
        float hitY = 0.5f + side.getStepY() * 0.5f;
        float hitZ = 0.5f + side.getStepZ() * 0.5f;
        if( Math.abs( hitY - 0.5f ) < 0.01f ) hitY = 0.45f;

        // Check if there's something suitable to place onto
        BlockHitResult hit = new BlockHitResult( new Vec3( hitX, hitY, hitZ ), side, position, false );
        UseOnContext context = new UseOnContext( turtlePlayer, InteractionHand.MAIN_HAND, hit );
        if( !canDeployOnBlock( new BlockPlaceContext( context ), turtle, turtlePlayer, position, side, allowReplace, outErrorMessage ) )
        {
            return false;
        }

        Item item = stack.getItem();
        BlockEntity existingTile = turtle.getLevel().getBlockEntity( position );

        boolean placed = doDeployOnBlock( stack, turtlePlayer, position, context, hit ).consumesAction();

        // Set text on signs
        if( placed && item instanceof SignItem && extraArguments != null && extraArguments.length >= 1 && extraArguments[0] instanceof String message )
        {
            Level world = turtle.getLevel();
            BlockEntity tile = world.getBlockEntity( position );
            if( tile == null || tile == existingTile )
            {
                tile = world.getBlockEntity( position.relative( side ) );
            }

            if( tile instanceof SignBlockEntity ) setSignText( world, tile, message );
        }

        return placed;
    }

    /**
     * Attempt to place an item into the world. Returns true/false if an item was placed.
     *
     * @param stack        The stack the player is using.
     * @param turtlePlayer The player which represents the turtle
     * @param position     The block we're deploying against's position.
     * @param context      The context of this place action.
     * @param hit          Where the block we're placing against was clicked.
     * @return If this item was deployed.
     * @see net.minecraft.server.level.ServerPlayerGameMode#useItemOn  For the original implementation.
     */
    private static InteractionResult doDeployOnBlock(
        @Nonnull ItemStack stack, TurtlePlayer turtlePlayer, BlockPos position, UseOnContext context, BlockHitResult hit
    )
    {
        PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock( turtlePlayer, InteractionHand.MAIN_HAND, position, hit );
        if( event.isCanceled() ) return event.getCancellationResult();

        if( event.getUseItem() != Result.DENY )
        {
            InteractionResult result = stack.onItemUseFirst( context );
            if( result != InteractionResult.PASS ) return result;
        }

        if( event.getUseItem() != Result.DENY )
        {
            InteractionResult result = stack.useOn( context );
            if( result != InteractionResult.PASS ) return result;
        }

        Item item = stack.getItem();
        if( item instanceof BucketItem || item instanceof BoatItem || item instanceof WaterLilyBlockItem || item instanceof BottleItem )
        {
            InteractionResult actionResult = ForgeHooks.onItemRightClick( turtlePlayer, InteractionHand.MAIN_HAND );
            if( actionResult != null && actionResult != InteractionResult.PASS ) return actionResult;

            InteractionResultHolder<ItemStack> result = stack.use( context.getLevel(), turtlePlayer, InteractionHand.MAIN_HAND );
            if( result.getResult().consumesAction() && !ItemStack.matches( stack, result.getObject() ) )
            {
                turtlePlayer.setItemInHand( InteractionHand.MAIN_HAND, result.getObject() );
                return result.getResult();
            }
        }

        return InteractionResult.PASS;
    }

    private static void setSignText( Level world, BlockEntity tile, String message )
    {
        SignBlockEntity signTile = (SignBlockEntity) tile;
        String[] split = message.split( "\n" );
        int firstLine = split.length <= 2 ? 1 : 0;
        for( int i = 0; i < 4; i++ )
        {
            if( i >= firstLine && i < firstLine + split.length )
            {
                String line = split[i - firstLine];
                signTile.setMessage( i, line.length() > 15
                    ? new TextComponent( line.substring( 0, 15 ) )
                    : new TextComponent( line )
                );
            }
            else
            {
                signTile.setMessage( i, new TextComponent( "" ) );
            }
        }
        signTile.setChanged();
        world.sendBlockUpdated( tile.getBlockPos(), tile.getBlockState(), tile.getBlockState(), Block.UPDATE_ALL );
    }

    private static class ErrorMessage
    {
        String message;
    }
}
