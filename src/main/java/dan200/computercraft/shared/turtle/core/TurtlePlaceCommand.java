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
import dan200.computercraft.api.turtle.event.TurtleEvent;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.util.DirectionUtil;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;

public class TurtlePlaceCommand implements ITurtleCommand
{
    private final InteractDirection direction;
    private final Object[] extraArguments;

    public TurtlePlaceCommand( InteractDirection direction, Object[] arguments )
    {
        this.direction = direction;
        extraArguments = arguments;
    }

    public static ItemStack deploy( @Nonnull ItemStack stack, ITurtleAccess turtle, Direction direction, Object[] extraArguments, String[] outErrorMessage )
    {
        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition()
            .relative( direction );
        TurtlePlayer turtlePlayer = createPlayer( turtle, playerPosition, direction );

        return deploy( stack, turtle, turtlePlayer, direction, extraArguments, outErrorMessage );
    }

    @Nonnull
    @Override
    public TurtleCommandResult execute( @Nonnull ITurtleAccess turtle )
    {
        // Get thing to place
        ItemStack stack = turtle.getInventory()
            .getItem( turtle.getSelectedSlot() );
        if( stack.isEmpty() )
        {
            return TurtleCommandResult.failure( "No items to place" );
        }

        // Remember old block
        Direction direction = this.direction.toWorldDir( turtle );
        BlockPos coordinates = turtle.getPosition()
            .relative( direction );

        // Create a fake player, and orient it appropriately
        BlockPos playerPosition = turtle.getPosition()
            .relative( direction );
        TurtlePlayer turtlePlayer = createPlayer( turtle, playerPosition, direction );

        TurtleBlockEvent.Place place = new TurtleBlockEvent.Place( turtle, turtlePlayer, turtle.getWorld(), coordinates, stack );
        if( TurtleEvent.post( place ) )
        {
            return TurtleCommandResult.failure( place.getFailureMessage() );
        }

        // Do the deploying
        String[] errorMessage = new String[1];
        ItemStack remainder = deploy( stack, turtle, turtlePlayer, direction, extraArguments, errorMessage );
        if( remainder != stack )
        {
            // Put the remaining items back
            turtle.getInventory()
                .setItem( turtle.getSelectedSlot(), remainder );
            turtle.getInventory()
                .setChanged();

            // Animate and return success
            turtle.playAnimation( TurtleAnimation.WAIT );
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

    public static TurtlePlayer createPlayer( ITurtleAccess turtle, BlockPos position, Direction direction )
    {
        TurtlePlayer turtlePlayer = TurtlePlayer.get( turtle );
        orientPlayer( turtle, turtlePlayer, position, direction );
        return turtlePlayer;
    }

    public static ItemStack deploy( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, Direction direction,
                                    Object[] extraArguments, String[] outErrorMessage )
    {
        // Deploy on an entity
        ItemStack remainder = deployOnEntity( stack, turtle, turtlePlayer, direction, extraArguments, outErrorMessage );
        if( remainder != stack )
        {
            return remainder;
        }

        // Deploy on the block immediately in front
        BlockPos position = turtle.getPosition();
        BlockPos newPosition = position.relative( direction );
        remainder = deployOnBlock( stack, turtle, turtlePlayer, newPosition, direction.getOpposite(), extraArguments, true, outErrorMessage );
        if( remainder != stack )
        {
            return remainder;
        }

        // Deploy on the block one block away
        remainder = deployOnBlock( stack,
            turtle,
            turtlePlayer,
            newPosition.relative( direction ),
            direction.getOpposite(),
            extraArguments,
            false,
            outErrorMessage );
        if( remainder != stack )
        {
            return remainder;
        }

        if( direction.getAxis() != Direction.Axis.Y )
        {
            // Deploy down on the block in front
            remainder = deployOnBlock( stack, turtle, turtlePlayer, newPosition.below(), Direction.UP, extraArguments, false, outErrorMessage );
            if( remainder != stack )
            {
                return remainder;
            }
        }

        // Deploy back onto the turtle
        remainder = deployOnBlock( stack, turtle, turtlePlayer, position, direction, extraArguments, false, outErrorMessage );
        return remainder;

        // If nothing worked, return the original stack unchanged
    }

    private static void orientPlayer( ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, Direction direction )
    {
        double posX = position.getX() + 0.5;
        double posY = position.getY() + 0.5;
        double posZ = position.getZ() + 0.5;

        // Stop intersection with the turtle itself
        if( turtle.getPosition()
            .equals( position ) )
        {
            posX += 0.48 * direction.getStepX();
            posY += 0.48 * direction.getStepY();
            posZ += 0.48 * direction.getStepZ();
        }

        if( direction.getAxis() != Direction.Axis.Y )
        {
            turtlePlayer.setYRot( direction.toYRot() );
            turtlePlayer.setXRot( 0.0f );
        }
        else
        {
            turtlePlayer.setYRot( turtle.getDirection()
                .toYRot() );
            turtlePlayer.setXRot( DirectionUtil.toPitchAngle( direction ) );
        }

        turtlePlayer.setPosRaw( posX, posY, posZ );
        turtlePlayer.xo = posX;
        turtlePlayer.yo = posY;
        turtlePlayer.zo = posZ;
        turtlePlayer.xRotO = turtlePlayer.getXRot();
        turtlePlayer.yRotO = turtlePlayer.getYRot();

        turtlePlayer.yHeadRot = turtlePlayer.getYRot();
        turtlePlayer.yHeadRotO = turtlePlayer.yHeadRot;
    }

    @Nonnull
    private static ItemStack deployOnEntity( @Nonnull ItemStack stack, final ITurtleAccess turtle, TurtlePlayer turtlePlayer, Direction direction,
                                             Object[] extraArguments, String[] outErrorMessage )
    {
        // See if there is an entity present
        final Level world = turtle.getWorld();
        final BlockPos position = turtle.getPosition();
        Vec3 turtlePos = turtlePlayer.position();
        Vec3 rayDir = turtlePlayer.getViewVector( 1.0f );
        Pair<Entity, Vec3> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit == null )
        {
            return stack;
        }

        // Load up the turtle's inventory
        ItemStack stackCopy = stack.copy();
        turtlePlayer.loadInventory( stackCopy );

        // Start claiming entity drops
        Entity hitEntity = hit.getKey();
        Vec3 hitPos = hit.getValue();
        DropConsumer.set( hitEntity, drop -> InventoryUtil.storeItems( drop, turtle.getItemHandler(), turtle.getSelectedSlot() ) );

        // Place on the entity
        boolean placed = false;
        InteractionResult cancelResult = hitEntity.interactAt( turtlePlayer, hitPos, InteractionHand.MAIN_HAND );

        if( cancelResult != null && cancelResult.consumesAction() )
        {
            placed = true;
        }
        else
        {
            cancelResult = hitEntity.interact( turtlePlayer, InteractionHand.MAIN_HAND );
            if( cancelResult != null && cancelResult.consumesAction() )
            {
                placed = true;
            }
            else if( hitEntity instanceof LivingEntity )
            {
                placed = stackCopy.interactLivingEntity( turtlePlayer, (LivingEntity) hitEntity, InteractionHand.MAIN_HAND ).consumesAction();
                if( placed ) turtlePlayer.loadInventory( stackCopy );
            }
        }

        // Stop claiming drops
        List<ItemStack> remainingDrops = DropConsumer.clear();
        for( ItemStack remaining : remainingDrops )
        {
            WorldUtil.dropItemStack( remaining,
                world,
                position,
                turtle.getDirection()
                    .getOpposite() );
        }

        // Put everything we collected into the turtles inventory, then return
        ItemStack remainder = turtlePlayer.unloadInventory( turtle );
        if( !placed && ItemStack.matches( stack, remainder ) )
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

    @Nonnull
    private static ItemStack deployOnBlock( @Nonnull ItemStack stack, ITurtleAccess turtle, TurtlePlayer turtlePlayer, BlockPos position, Direction side,
                                            Object[] extraArguments, boolean allowReplace, String[] outErrorMessage )
    {
        // Re-orient the fake player
        Direction playerDir = side.getOpposite();
        BlockPos playerPosition = position.relative( side );
        orientPlayer( turtle, turtlePlayer, playerPosition, playerDir );

        ItemStack stackCopy = stack.copy();
        turtlePlayer.loadInventory( stackCopy );

        // Calculate where the turtle would hit the block
        float hitX = 0.5f + side.getStepX() * 0.5f;
        float hitY = 0.5f + side.getStepY() * 0.5f;
        float hitZ = 0.5f + side.getStepZ() * 0.5f;
        if( Math.abs( hitY - 0.5f ) < 0.01f )
        {
            hitY = 0.45f;
        }

        // Check if there's something suitable to place onto
        BlockHitResult hit = new BlockHitResult( new Vec3( hitX, hitY, hitZ ), side, position, false );
        UseOnContext context = new UseOnContext( turtlePlayer, InteractionHand.MAIN_HAND, hit );
        BlockPlaceContext placementContext = new BlockPlaceContext( context );
        if( !canDeployOnBlock( new BlockPlaceContext( context ), turtle, turtlePlayer, position, side, allowReplace, outErrorMessage ) )
        {
            return stack;
        }

        // Load up the turtle's inventory
        Item item = stack.getItem();

        // Do the deploying (put everything in the players inventory)
        boolean placed = false;
        BlockEntity existingTile = turtle.getWorld()
            .getBlockEntity( position );

        if( stackCopy.useOn( context ).consumesAction() )
        {
            placed = true;
            turtlePlayer.loadInventory( stackCopy );
        }

        if( !placed && (item instanceof BucketItem || item instanceof BoatItem || item instanceof WaterLilyBlockItem || item instanceof BottleItem) )
        {
            InteractionResultHolder<ItemStack> result = stackCopy.use( turtle.getWorld(), turtlePlayer, InteractionHand.MAIN_HAND );
            if( result.getResult()
                .consumesAction() && !ItemStack.matches( stack, result.getObject() ) )
            {
                placed = true;
                turtlePlayer.loadInventory( result.getObject() );
            }
        }

        // Set text on signs
        if( placed && item instanceof SignItem )
        {
            if( extraArguments != null && extraArguments.length >= 1 && extraArguments[0] instanceof String )
            {
                Level world = turtle.getWorld();
                BlockEntity tile = world.getBlockEntity( position );
                if( tile == null || tile == existingTile )
                {
                    tile = world.getBlockEntity( position.relative( side ) );
                }
                if( tile instanceof SignBlockEntity )
                {
                    SignBlockEntity signTile = (SignBlockEntity) tile;
                    String s = (String) extraArguments[0];
                    String[] split = s.split( "\n" );
                    int firstLine = split.length <= 2 ? 1 : 0;
                    for( int i = 0; i < 4; i++ )
                    {
                        if( i >= firstLine && i < firstLine + split.length )
                        {
                            if( split[i - firstLine].length() > 15 )
                            {
                                signTile.setMessage( i, new TextComponent( split[i - firstLine].substring( 0, 15 ) ) );
                            }
                            else
                            {
                                signTile.setMessage( i, new TextComponent( split[i - firstLine] ) );
                            }
                        }
                        else
                        {
                            signTile.setMessage( i, new TextComponent( "" ) );
                        }
                    }
                    signTile.setChanged();
                    world.sendBlockUpdated( tile.getBlockPos(), tile.getBlockState(), tile.getBlockState(), 3 );
                }
            }
        }

        // Put everything we collected into the turtles inventory, then return
        ItemStack remainder = turtlePlayer.unloadInventory( turtle );
        if( !placed && ItemStack.matches( stack, remainder ) )
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

    private static boolean canDeployOnBlock( @Nonnull BlockPlaceContext context, ITurtleAccess turtle, TurtlePlayer player, BlockPos position,
                                             Direction side, boolean allowReplaceable, String[] outErrorMessage )
    {
        Level world = turtle.getWorld();
        if( !world.isInWorldBounds( position ) || world.isEmptyBlock( position ) || (context.getItemInHand()
            .getItem() instanceof BlockItem && WorldUtil.isLiquidBlock( world,
            position )) )
        {
            return false;
        }

        BlockState state = world.getBlockState( position );

        boolean replaceable = state.canBeReplaced( context );
        if( !allowReplaceable && replaceable )
        {
            return false;
        }

        if( ComputerCraft.turtlesObeyBlockProtection )
        {
            // Check spawn protection
            boolean editable = replaceable ? TurtlePermissions.isBlockEditable( world, position, player ) : TurtlePermissions.isBlockEditable( world,
                position.relative(
                    side ),
                player );
            if( !editable )
            {
                if( outErrorMessage != null )
                {
                    outErrorMessage[0] = "Cannot place in protected area";
                }
                return false;
            }
        }

        return true;
    }
}
