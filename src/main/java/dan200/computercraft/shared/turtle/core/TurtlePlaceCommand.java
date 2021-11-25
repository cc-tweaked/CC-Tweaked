/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
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
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.network.play.client.CUseEntityPacket;
import net.minecraft.tileentity.SignTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.InvWrapper;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;

import static net.minecraftforge.eventbus.api.Event.Result;

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

        TurtleBlockEvent.Place place = new TurtleBlockEvent.Place( turtle, turtlePlayer, turtle.getWorld(), coordinates, stack );
        if( MinecraftForge.EVENT_BUS.post( place ) ) return TurtleCommandResult.failure( place.getFailureMessage() );

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
        turtlePlayer.inventory.clearContent();
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
        final World world = turtle.getWorld();
        final BlockPos position = turtle.getPosition();
        Vector3d turtlePos = turtlePlayer.position();
        Vector3d rayDir = turtlePlayer.getViewVector( 1.0f );
        Pair<Entity, Vector3d> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit == null ) return false;

        // Start claiming entity drops
        Entity hitEntity = hit.getKey();
        Vector3d hitPos = hit.getValue();

        IItemHandler itemHandler = new InvWrapper( turtlePlayer.inventory );
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
     * @see net.minecraft.network.play.ServerPlayNetHandler#handleInteract(CUseEntityPacket)
     * @see net.minecraft.entity.player.PlayerEntity#interactOn(Entity, Hand)
     */
    private static boolean doDeployOnEntity( @Nonnull ItemStack stack, TurtlePlayer turtlePlayer, @Nonnull Entity hitEntity, @Nonnull Vector3d hitPos )
    {
        // Placing "onto" a block follows two flows. First we try to interactAt. If that doesn't succeed, then we try to
        // call the normal interact path. Cancelling an interactAt *does not* cancel a normal interact path.

        ActionResultType interactAt = ForgeHooks.onInteractEntityAt( turtlePlayer, hitEntity, hitPos, Hand.MAIN_HAND );
        if( interactAt == null ) interactAt = hitEntity.interactAt( turtlePlayer, hitPos, Hand.MAIN_HAND );
        if( interactAt.consumesAction() ) return true;

        ActionResultType interact = ForgeHooks.onInteractEntity( turtlePlayer, hitEntity, Hand.MAIN_HAND );
        if( interact != null ) return interact.consumesAction();

        if( hitEntity.interact( turtlePlayer, Hand.MAIN_HAND ).consumesAction() ) return true;
        if( hitEntity instanceof LivingEntity )
        {
            return stack.interactLivingEntity( turtlePlayer, (LivingEntity) hitEntity, Hand.MAIN_HAND ).consumesAction();
        }

        return false;
    }

    private static boolean canDeployOnBlock(
        @Nonnull BlockItemUseContext context, ITurtleAccess turtle, TurtlePlayer player, BlockPos position,
        Direction side, boolean allowReplaceable, ErrorMessage outErrorMessage
    )
    {
        World world = turtle.getWorld();
        if( !World.isInWorldBounds( position ) || world.isEmptyBlock( position ) ||
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
        BlockRayTraceResult hit = new BlockRayTraceResult( new Vector3d( hitX, hitY, hitZ ), side, position, false );
        ItemUseContext context = new ItemUseContext( turtlePlayer, Hand.MAIN_HAND, hit );
        if( !canDeployOnBlock( new BlockItemUseContext( context ), turtle, turtlePlayer, position, side, allowReplace, outErrorMessage ) )
        {
            return false;
        }

        Item item = stack.getItem();
        TileEntity existingTile = turtle.getWorld().getBlockEntity( position );

        boolean placed = doDeployOnBlock( stack, turtlePlayer, position, context, hit ).consumesAction();

        // Set text on signs
        if( placed && item instanceof SignItem && extraArguments != null && extraArguments.length >= 1 && extraArguments[0] instanceof String )
        {
            World world = turtle.getWorld();
            TileEntity tile = world.getBlockEntity( position );
            if( tile == null || tile == existingTile )
            {
                tile = world.getBlockEntity( position.relative( side ) );
            }

            if( tile instanceof SignTileEntity ) setSignText( world, tile, (String) extraArguments[0] );
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
     * @see net.minecraft.server.management.PlayerInteractionManager#useItemOn For the original implementation.
     */
    private static ActionResultType doDeployOnBlock(
        @Nonnull ItemStack stack, TurtlePlayer turtlePlayer, BlockPos position, ItemUseContext context, BlockRayTraceResult hit
    )
    {
        PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock( turtlePlayer, Hand.MAIN_HAND, position, hit );
        if( event.isCanceled() ) return event.getCancellationResult();

        if( event.getUseItem() != Result.DENY )
        {
            ActionResultType result = stack.onItemUseFirst( context );
            if( result != ActionResultType.PASS ) return result;
        }

        if( event.getUseItem() != Result.DENY )
        {
            ActionResultType result = stack.useOn( context );
            if( result != ActionResultType.PASS ) return result;
        }

        Item item = stack.getItem();
        if( item instanceof BucketItem || item instanceof BoatItem || item instanceof LilyPadItem || item instanceof GlassBottleItem )
        {
            ActionResultType actionResult = ForgeHooks.onItemRightClick( turtlePlayer, Hand.MAIN_HAND );
            if( actionResult != null && actionResult != ActionResultType.PASS ) return actionResult;

            ActionResult<ItemStack> result = stack.use( context.getLevel(), turtlePlayer, Hand.MAIN_HAND );
            if( result.getResult().consumesAction() && !ItemStack.matches( stack, result.getObject() ) )
            {
                turtlePlayer.setItemInHand( Hand.MAIN_HAND, result.getObject() );
                return result.getResult();
            }
        }

        return ActionResultType.PASS;
    }

    private static void setSignText( World world, TileEntity tile, String message )
    {
        SignTileEntity signTile = (SignTileEntity) tile;
        String[] split = message.split( "\n" );
        int firstLine = split.length <= 2 ? 1 : 0;
        for( int i = 0; i < 4; i++ )
        {
            if( i >= firstLine && i < firstLine + split.length )
            {
                String line = split[i - firstLine];
                signTile.setMessage( i, line.length() > 15
                    ? new StringTextComponent( line.substring( 0, 15 ) )
                    : new StringTextComponent( line )
                );
            }
            else
            {
                signTile.setMessage( i, new StringTextComponent( "" ) );
            }
        }
        signTile.setChanged();
        world.sendBlockUpdated( tile.getBlockPos(), tile.getBlockState(), tile.getBlockState(), Constants.BlockFlags.DEFAULT );
    }

    private static class ErrorMessage
    {
        String message;
    }
}
