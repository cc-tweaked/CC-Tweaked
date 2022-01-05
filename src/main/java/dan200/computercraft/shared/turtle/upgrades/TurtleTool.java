/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import com.mojang.math.Matrix4f;
import com.mojang.math.Transformation;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.fabric.mixininterface.IMatrix4f;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.core.TurtlePlaceCommand;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

public class TurtleTool extends AbstractTurtleUpgrade
{
    protected final ItemStack item;

    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;

    public TurtleTool( ResourceLocation id, String adjective, Item item )
    {
        super( id, TurtleUpgradeType.TOOL, adjective, item );
        this.item = new ItemStack( item );
    }

    public TurtleTool( ResourceLocation id, Item item )
    {
        super( id, TurtleUpgradeType.TOOL, item );
        this.item = new ItemStack( item );
    }

    public TurtleTool( ResourceLocation id, ItemStack craftItem, ItemStack toolItem )
    {
        super( id, TurtleUpgradeType.TOOL, craftItem );
        item = toolItem;
    }

    @Override
    public boolean isItemSuitable( @Nonnull ItemStack stack )
    {
        CompoundTag tag = stack.getTag();
        if( tag == null || tag.isEmpty() ) return true;

        // Check we've not got anything vaguely interesting on the item. We allow other mods to add their
        // own NBT, with the understanding such details will be lost to the mist of time.
        if( stack.isDamaged() || stack.isEnchanted() || stack.hasCustomHoverName() ) return false;
        return !tag.contains( "AttributeModifiers", TAG_LIST ) ||
            tag.getList( "AttributeModifiers", TAG_COMPOUND ).isEmpty();
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction direction )
    {
        switch( verb )
        {
            case ATTACK:
                return attack( turtle, direction, side );
            case DIG:
                return dig( turtle, direction, side );
            default:
                return TurtleCommandResult.failure( "Unsupported action" );
        }
    }

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public TransformedModel getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return TransformedModel.of( getCraftingItem(), side == TurtleSide.LEFT ? Transforms.leftTransform : Transforms.rightTransform );
    }

    private TurtleCommandResult attack( ITurtleAccess turtle, Direction direction, TurtleSide side )
    {
        // Create a fake player, and orient it appropriately
        Level world = turtle.getLevel();
        BlockPos position = turtle.getPosition();
        BlockEntity turtleBlock = turtle instanceof TurtleBrain ? ((TurtleBrain) turtle).getOwner() : world.getBlockEntity( position );
        if( turtleBlock == null ) return TurtleCommandResult.failure( "Turtle has vanished from existence." );

        final TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, position, direction );

        // See if there is an entity present
        Vec3 turtlePos = turtlePlayer.position();
        Vec3 rayDir = turtlePlayer.getViewVector( 1.0f );
        Pair<Entity, Vec3> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit != null )
        {
            // Load up the turtle's inventoryf
            ItemStack stackCopy = item.copy();
            turtlePlayer.loadInventory( stackCopy );

            Entity hitEntity = hit.getKey();

            // Fire several events to ensure we have permissions.
            if( AttackEntityCallback.EVENT.invoker()
                .interact( turtlePlayer,
                    world,
                    InteractionHand.MAIN_HAND,
                    hitEntity,
                    null ) == InteractionResult.FAIL || !hitEntity.isAttackable() )
            {
                return TurtleCommandResult.failure( "Nothing to attack here" );
            }

            // Start claiming entity drops
            DropConsumer.set( hitEntity, turtleDropConsumer( turtleBlock, turtle ) );

            // Attack the entity
            boolean attacked = false;
            if( !hitEntity.skipAttackInteraction( turtlePlayer ) )
            {
                float damage = (float) turtlePlayer.getAttributeValue( Attributes.ATTACK_DAMAGE );
                damage *= getDamageMultiplier();
                if( damage > 0.0f )
                {
                    DamageSource source = DamageSource.playerAttack( turtlePlayer );
                    if( hitEntity instanceof ArmorStand )
                    {
                        // Special case for armor stands: attack twice to guarantee destroy
                        hitEntity.hurt( source, damage );
                        if( hitEntity.isAlive() )
                        {
                            hitEntity.hurt( source, damage );
                        }
                        attacked = true;
                    }
                    else
                    {
                        if( hitEntity.hurt( source, damage ) )
                        {
                            attacked = true;
                        }
                    }
                }
            }

            // Stop claiming drops
            stopConsuming( turtleBlock, turtle );

            // Put everything we collected into the turtles inventory, then return
            if( attacked )
            {
                turtlePlayer.unloadInventory( turtle );
                return TurtleCommandResult.success();
            }
        }

        return TurtleCommandResult.failure( "Nothing to attack here" );
    }

    private TurtleCommandResult dig( ITurtleAccess turtle, Direction direction, TurtleSide side )
    {
        // Get ready to dig
        Level world = turtle.getLevel();
        BlockPos turtlePosition = turtle.getPosition();
        BlockEntity turtleBlock = turtle instanceof TurtleBrain ? ((TurtleBrain) turtle).getOwner() : world.getBlockEntity( turtlePosition );
        if( turtleBlock == null ) return TurtleCommandResult.failure( "Turtle has vanished from existence." );


        BlockPos blockPosition = turtlePosition.relative( direction );

        if( world.isEmptyBlock( blockPosition ) || WorldUtil.isLiquidBlock( world, blockPosition ) )
        {
            return TurtleCommandResult.failure( "Nothing to dig here" );
        }

        BlockState state = world.getBlockState( blockPosition );

        TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, turtlePosition, direction );
        turtlePlayer.loadInventory( item.copy() );

        if( ComputerCraft.turtlesObeyBlockProtection )
        {
            if( !TurtlePermissions.isBlockEditable( world, blockPosition, turtlePlayer ) )
            {
                return TurtleCommandResult.failure( "Cannot break protected block" );
            }
        }

        // Check if we can break the block
        if( !canBreakBlock( state, world, blockPosition, turtlePlayer ) )
        {
            return TurtleCommandResult.failure( "Unbreakable block detected" );
        }

        if( !PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak( world, turtlePlayer, blockPosition, state, null ) )
        {
            return TurtleCommandResult.failure( "Break cancelled" );
        }

        // Consume the items the block drops
        DropConsumer.set( world, blockPosition, turtleDropConsumer( turtleBlock, turtle ) );

        BlockEntity tile = world.getBlockEntity( blockPosition );

        // Much of this logic comes from PlayerInteractionManager#tryHarvestBlock, so it's a good idea
        // to consult there before making any changes.

        // Play the destruction sound and particles
        world.levelEvent( 2001, blockPosition, Block.getId( state ) );

        // Destroy the block
        state.getBlock()
            .playerWillDestroy( world, blockPosition, state, turtlePlayer );
        if( world.removeBlock( blockPosition, false ) )
        {
            state.getBlock()
                .destroy( world, blockPosition, state );
            if( turtlePlayer.hasCorrectToolForDrops( state ) )
            {
                state.getBlock()
                    .playerDestroy( world, turtlePlayer, blockPosition, state, tile, turtlePlayer.getMainHandItem() );
            }
        }

        stopConsuming( turtleBlock, turtle );

        return TurtleCommandResult.success();

    }

    private static Function<ItemStack, ItemStack> turtleDropConsumer( BlockEntity turtleBlock, ITurtleAccess turtle )
    {
        return drop -> turtleBlock.isRemoved() ? drop : InventoryUtil.storeItems( drop, turtle.getItemHandler(), turtle.getSelectedSlot() );
    }

    protected float getDamageMultiplier()
    {
        return 3.0f;
    }

    private static void stopConsuming( BlockEntity turtleBlock, ITurtleAccess turtle )
    {
        Direction direction = turtleBlock.isRemoved() ? null : turtle.getDirection().getOpposite();
        List<ItemStack> extra = DropConsumer.clear();
        for( ItemStack remainder : extra )
        {
            WorldUtil.dropItemStack( remainder,
                turtle.getLevel(),
                turtle.getPosition(),
                direction );
        }
    }

    protected boolean canBreakBlock( BlockState state, Level world, BlockPos pos, TurtlePlayer player )
    {
        Block block = state.getBlock();
        return !state.isAir() && block != Blocks.BEDROCK && state.getDestroyProgress( player, world, pos ) > 0;
    }

    private static class Transforms
    {
        static final Transformation leftTransform = getMatrixFor( -0.40625f );
        static final Transformation rightTransform = getMatrixFor( 0.40625f );

        private static Transformation getMatrixFor( float offset )
        {
            Matrix4f matrix = new Matrix4f();
            ((IMatrix4f) (Object) matrix).setFloatArray( new float[] {
                0.0f, 0.0f, -1.0f, 1.0f + offset,
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, -1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 0.0f, 1.0f,
            } );
            return new Transformation( matrix );
        }
    }
}
