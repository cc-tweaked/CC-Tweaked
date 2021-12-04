/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import com.mojang.math.Matrix4f;
import com.mojang.math.Transformation;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.ComputerCraftTags;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.function.Function;

import static net.minecraft.nbt.Tag.TAG_COMPOUND;
import static net.minecraft.nbt.Tag.TAG_LIST;

public class TurtleTool extends AbstractTurtleUpgrade
{
    protected static final TurtleCommandResult UNBREAKABLE = TurtleCommandResult.failure( "Cannot break unbreakable block" );
    protected static final TurtleCommandResult INEFFECTIVE = TurtleCommandResult.failure( "Cannot break block with this tool" );

    final ItemStack item;
    final float damageMulitiplier;

    public TurtleTool( ResourceLocation id, Item item, float damageMulitiplier )
    {
        super( id, TurtleUpgradeType.TOOL, new ItemStack( item ) );
        this.item = new ItemStack( item );
        this.damageMulitiplier = damageMulitiplier;
    }

    //    public TurtleTool( ResourceLocation id, String adjective, Item craftItem, ItemStack toolItem, float damageMulitiplier )
    //    {
    //        super( id, TurtleUpgradeType.TOOL, adjective, new ItemStack( craftItem ) );
    //        item = toolItem;
    //        this.damageMulitiplier = damageMulitiplier;
    //    }

    @Override
    public boolean isItemSuitable( @Nonnull ItemStack stack )
    {
        CompoundTag tag = stack.getTag();
        if( tag == null || tag.isEmpty() ) return true;

        // Check we've not got anything vaguely interesting on the item. We allow other mods to add their
        // own NBT, with the understanding such details will be lost to the mist of time.
        if( stack.isDamaged() || stack.isEnchanted() || stack.hasCustomHoverName() ) return false;
        if( tag.contains( "AttributeModifiers", TAG_LIST ) &&
            !tag.getList( "AttributeModifiers", TAG_COMPOUND ).isEmpty() )
        {
            return false;
        }

        return true;
    }

    @Nonnull
    @Override
    @Environment( EnvType.CLIENT )
    public TransformedModel getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return TransformedModel.of( getCraftingItem(), side == TurtleSide.LEFT ? Transforms.leftTransform : Transforms.rightTransform );
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull Direction direction )
    {
        switch( verb )
        {
            case ATTACK:
                return attack( turtle, direction );
            case DIG:
                return dig( turtle, direction );
            default:
                return TurtleCommandResult.failure( "Unsupported action" );
        }
    }

    protected TurtleCommandResult checkBlockBreakable( BlockState state, Level world, BlockPos pos, TurtlePlayer player )
    {
        Block block = state.getBlock();
        if( state.isAir() || block == Blocks.BEDROCK
            || state.getDestroyProgress( player, world, pos ) <= 0 )
        {
            return UNBREAKABLE;
        }

        return isTriviallyBreakable( world, pos, state ) ? TurtleCommandResult.success() : INEFFECTIVE;
    }

    private TurtleCommandResult attack( ITurtleAccess turtle, Direction direction )
    {
        // Create a fake player, and orient it appropriately
        Level world = turtle.getLevel();
        BlockPos position = turtle.getPosition();
        BlockEntity turtleTile = turtle instanceof TurtleBrain ? ((TurtleBrain) turtle).getOwner() : world.getBlockEntity( position );
        if( turtleTile == null ) return TurtleCommandResult.failure( "Turtle has vanished from existence." );

        final TurtlePlayer turtlePlayer = TurtlePlayer.getWithPosition( turtle, position, direction );

        // See if there is an entity present
        Vec3 turtlePos = turtlePlayer.position();
        Vec3 rayDir = turtlePlayer.getViewVector( 1.0f );
        Pair<Entity, Vec3> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit != null )
        {
            // Load up the turtle's inventory
            ItemStack stackCopy = item.copy();
            turtlePlayer.loadInventory( stackCopy );

            Entity hitEntity = hit.getKey();

            // Fire several events to ensure we have permissions.
            if( AttackEntityCallback.EVENT.invoker().interact( turtlePlayer, world, InteractionHand.MAIN_HAND, hitEntity, null ) == InteractionResult.FAIL
                || !hitEntity.isAttackable() )
            {
                return TurtleCommandResult.failure( "Nothing to attack here" );
            }

            // Start claiming entity drops
            DropConsumer.set( hitEntity, turtleDropConsumer( turtleTile, turtle ) );

            // Attack the entity
            boolean attacked = false;
            if( !hitEntity.skipAttackInteraction( turtlePlayer ) )
            {
                float damage = (float) turtlePlayer.getAttributeValue( Attributes.ATTACK_DAMAGE ) * damageMulitiplier;
                if( damage > 0.0f )
                {
                    DamageSource source = DamageSource.playerAttack( turtlePlayer );
                    if( hitEntity instanceof ArmorStand )
                    {
                        // Special case for armor stands: attack twice to guarantee destroy
                        hitEntity.hurt( source, damage );
                        if( hitEntity.isAlive() ) hitEntity.hurt( source, damage );
                        attacked = true;
                    }
                    else
                    {
                        if( hitEntity.hurt( source, damage ) ) attacked = true;
                    }
                }
            }

            // Stop claiming drops
            stopConsuming( turtleTile, turtle );

            // Put everything we collected into the turtles inventory, then return
            if( attacked )
            {
                turtlePlayer.getInventory().clearContent();
                return TurtleCommandResult.success();
            }
        }

        return TurtleCommandResult.failure( "Nothing to attack here" );
    }

    private TurtleCommandResult dig( ITurtleAccess turtle, Direction direction )
    {
        // TODO: HOE_TILL really, if it's ever implemented
        if( item.getItem() == Items.DIAMOND_SHOVEL || item.getItem() == Items.DIAMOND_HOE )
        {
            if( TurtlePlaceCommand.deployCopiedItem( item.copy(), turtle, direction, null, null ) )
            {
                return TurtleCommandResult.success();
            }
        }

        // Get ready to dig
        Level world = turtle.getLevel();
        BlockPos turtlePosition = turtle.getPosition();
        BlockEntity turtleTile = turtle instanceof TurtleBrain ? ((TurtleBrain) turtle).getOwner() : world.getBlockEntity( turtlePosition );
        if( turtleTile == null ) return TurtleCommandResult.failure( "Turtle has vanished from existence." );

        BlockPos blockPosition = turtlePosition.relative( direction );
        if( world.isEmptyBlock( blockPosition ) || WorldUtil.isLiquidBlock( world, blockPosition ) )
        {
            return TurtleCommandResult.failure( "Nothing to dig here" );
        }

        BlockState state = world.getBlockState( blockPosition );
        FluidState fluidState = world.getFluidState( blockPosition );

        TurtlePlayer turtlePlayer = TurtlePlayer.getWithPosition( turtle, turtlePosition, direction );
        turtlePlayer.loadInventory( item.copy() );

        if( ComputerCraft.turtlesObeyBlockProtection )
        {
            // Check spawn protection
            if( !PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak( world, turtlePlayer, blockPosition, state, null ) )
            {
                return TurtleCommandResult.failure( "Cannot break protected block" );
            }

            if( !TurtlePermissions.isBlockEditable( world, blockPosition, turtlePlayer ) )
            {
                return TurtleCommandResult.failure( "Cannot break protected block" );
            }
        }

        // Check if we can break the block
        TurtleCommandResult breakable = checkBlockBreakable( state, world, blockPosition, turtlePlayer );
        if( !breakable.isSuccess() ) return breakable;

        // Consume the items the block drops
        DropConsumer.set( world, blockPosition, turtleDropConsumer( turtleTile, turtle ) );

        BlockEntity tile = world.getBlockEntity( blockPosition );

        // Much of this logic comes from PlayerInteractionManager#tryHarvestBlock, so it's a good idea
        // to consult there before making any changes.

        // Play the destruction sound and particles
        world.levelEvent( 2001, blockPosition, Block.getId( state ) );

        // Destroy the block
        state.getBlock().destroy( world, blockPosition, state );
        boolean canHarvest = turtlePlayer.hasCorrectToolForDrops( state );
        if( canHarvest )
        {
            state.getBlock().playerDestroy( world, turtlePlayer, blockPosition, state, tile, turtlePlayer.getMainHandItem() );
        }

        stopConsuming( turtleTile, turtle );

        return TurtleCommandResult.success();

    }

    private static Function<ItemStack, ItemStack> turtleDropConsumer( BlockEntity tile, ITurtleAccess turtle )
    {
        return drop -> tile.isRemoved() ? drop : InventoryUtil.storeItems( drop, turtle.getItemHandler(), turtle.getSelectedSlot() );
    }

    private static void stopConsuming( BlockEntity tile, ITurtleAccess turtle )
    {
        Direction direction = tile.isRemoved() ? null : turtle.getDirection().getOpposite();
        DropConsumer.clearAndDrop( turtle.getLevel(), turtle.getPosition(), direction );
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

    protected boolean isTriviallyBreakable( BlockGetter reader, BlockPos pos, BlockState state )
    {
        return state.is( ComputerCraftTags.Blocks.TURTLE_ALWAYS_BREAKABLE )
            // Allow breaking any "instabreak" block.
            || state.getDestroySpeed( reader, pos ) == 0;
    }
}
