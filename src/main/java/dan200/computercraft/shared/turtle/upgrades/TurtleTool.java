/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.api.turtle.event.TurtleAttackEvent;
import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import dan200.computercraft.shared.ComputerCraftTags;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.TransformationMatrix;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class TurtleTool extends AbstractTurtleUpgrade
{
    protected static final TurtleCommandResult UNBREAKABLE = TurtleCommandResult.failure( "Unbreakable block detected" );
    protected static final TurtleCommandResult INEFFECTIVE = TurtleCommandResult.failure( "Cannot break block with this tool" );

    protected final ItemStack item;

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
        CompoundNBT tag = stack.getTag();
        if( tag == null || tag.isEmpty() ) return true;

        // Check we've not got anything vaguely interesting on the item. We allow other mods to add their
        // own NBT, with the understanding such details will be lost to the mist of time.
        if( stack.isDamaged() || stack.isEnchanted() || stack.hasCustomHoverName() ) return false;
        if( tag.contains( "AttributeModifiers", Constants.NBT.TAG_LIST ) &&
            !tag.getList( "AttributeModifiers", Constants.NBT.TAG_COMPOUND ).isEmpty() )
        {
            return false;
        }

        return true;
    }

    @Nonnull
    @Override
    @OnlyIn( Dist.CLIENT )
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
                return attack( turtle, direction, side );
            case DIG:
                return dig( turtle, direction, side );
            default:
                return TurtleCommandResult.failure( "Unsupported action" );
        }
    }

    protected TurtleCommandResult checkBlockBreakable( BlockState state, World world, BlockPos pos, TurtlePlayer player )
    {
        Block block = state.getBlock();
        return !state.isAir()
            && block != Blocks.BEDROCK
            && state.getDestroyProgress( player, world, pos ) > 0
            && block.canEntityDestroy( state, world, pos, player )
            ? TurtleCommandResult.success() : UNBREAKABLE;
    }

    protected float getDamageMultiplier()
    {
        return 3.0f;
    }

    private TurtleCommandResult attack( ITurtleAccess turtle, Direction direction, TurtleSide side )
    {
        // Create a fake player, and orient it appropriately
        World world = turtle.getWorld();
        BlockPos position = turtle.getPosition();
        TileEntity turtleTile = turtle instanceof TurtleBrain ? ((TurtleBrain) turtle).getOwner() : world.getBlockEntity( position );
        if( turtleTile == null ) return TurtleCommandResult.failure( "Turtle has vanished from existence." );

        final TurtlePlayer turtlePlayer = TurtlePlayer.getWithPosition( turtle, position, direction );

        // See if there is an entity present
        Vector3d turtlePos = turtlePlayer.position();
        Vector3d rayDir = turtlePlayer.getViewVector( 1.0f );
        Pair<Entity, Vector3d> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit != null )
        {
            // Load up the turtle's inventory
            ItemStack stackCopy = item.copy();
            turtlePlayer.loadInventory( stackCopy );

            Entity hitEntity = hit.getKey();

            // Fire several events to ensure we have permissions.
            if( MinecraftForge.EVENT_BUS.post( new AttackEntityEvent( turtlePlayer, hitEntity ) ) || !hitEntity.isAttackable() )
            {
                return TurtleCommandResult.failure( "Nothing to attack here" );
            }

            TurtleAttackEvent attackEvent = new TurtleAttackEvent( turtle, turtlePlayer, hitEntity, this, side );
            if( MinecraftForge.EVENT_BUS.post( attackEvent ) )
            {
                return TurtleCommandResult.failure( attackEvent.getFailureMessage() );
            }

            // Start claiming entity drops
            DropConsumer.set( hitEntity, turtleDropConsumer( turtleTile, turtle ) );

            // Attack the entity
            boolean attacked = false;
            if( !hitEntity.skipAttackInteraction( turtlePlayer ) )
            {
                float damage = (float) turtlePlayer.getAttributeValue( Attributes.ATTACK_DAMAGE );
                damage *= getDamageMultiplier();
                ComputerCraft.log.info( "Dealing {} damage", damage );
                if( damage > 0.0f )
                {
                    DamageSource source = DamageSource.playerAttack( turtlePlayer );
                    if( hitEntity instanceof ArmorStandEntity )
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
            stopConsuming( turtleTile, turtle );

            // Put everything we collected into the turtles inventory, then return
            if( attacked )
            {
                turtlePlayer.inventory.clearContent();
                return TurtleCommandResult.success();
            }
        }

        return TurtleCommandResult.failure( "Nothing to attack here" );
    }

    private TurtleCommandResult dig( ITurtleAccess turtle, Direction direction, TurtleSide side )
    {
        // Get ready to dig
        World world = turtle.getWorld();
        BlockPos turtlePosition = turtle.getPosition();
        TileEntity turtleTile = turtle instanceof TurtleBrain ? ((TurtleBrain) turtle).getOwner() : world.getBlockEntity( turtlePosition );
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
            if( MinecraftForge.EVENT_BUS.post( new BlockEvent.BreakEvent( world, blockPosition, state, turtlePlayer ) ) )
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

        // Fire the dig event, checking whether it was cancelled.
        TurtleBlockEvent.Dig digEvent = new TurtleBlockEvent.Dig( turtle, turtlePlayer, world, blockPosition, state, this, side );
        if( MinecraftForge.EVENT_BUS.post( digEvent ) )
        {
            return TurtleCommandResult.failure( digEvent.getFailureMessage() );
        }

        // Consume the items the block drops
        DropConsumer.set( world, blockPosition, turtleDropConsumer( turtleTile, turtle ) );

        TileEntity tile = world.getBlockEntity( blockPosition );

        // Much of this logic comes from PlayerInteractionManager#tryHarvestBlock, so it's a good idea
        // to consult there before making any changes.

        // Play the destruction sound and particles
        world.levelEvent( 2001, blockPosition, Block.getId( state ) );

        // Destroy the block
        boolean canHarvest = state.canHarvestBlock( world, blockPosition, turtlePlayer );
        boolean canBreak = state.removedByPlayer( world, blockPosition, turtlePlayer, canHarvest, fluidState );
        if( canBreak ) state.getBlock().destroy( world, blockPosition, state );
        if( canHarvest && canBreak )
        {
            state.getBlock().playerDestroy( world, turtlePlayer, blockPosition, state, tile, turtlePlayer.getMainHandItem() );
        }

        stopConsuming( turtleTile, turtle );

        return TurtleCommandResult.success();

    }

    private static Function<ItemStack, ItemStack> turtleDropConsumer( TileEntity tile, ITurtleAccess turtle )
    {
        return drop -> tile.isRemoved() ? drop : InventoryUtil.storeItems( drop, turtle.getItemHandler(), turtle.getSelectedSlot() );
    }

    private static void stopConsuming( TileEntity tile, ITurtleAccess turtle )
    {
        Direction direction = tile.isRemoved() ? null : turtle.getDirection().getOpposite();
        DropConsumer.clearAndDrop( turtle.getWorld(), turtle.getPosition(), direction );
    }

    private static class Transforms
    {
        static final TransformationMatrix leftTransform = getMatrixFor( -0.40625f );
        static final TransformationMatrix rightTransform = getMatrixFor( 0.40625f );

        private static TransformationMatrix getMatrixFor( float offset )
        {
            return new TransformationMatrix( new Matrix4f( new float[] {
                0.0f, 0.0f, -1.0f, 1.0f + offset,
                1.0f, 0.0f, 0.0f, 0.0f,
                0.0f, -1.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 0.0f, 1.0f,
            } ) );
        }
    }

    protected boolean isTriviallyBreakable( IWorldReader reader, BlockPos pos, BlockState state )
    {
        return state.is( ComputerCraftTags.Blocks.TURTLE_ALWAYS_BREAKABLE )
            // Allow breaking any "instabreak" block.
            || (state.getDestroySpeed( reader, pos ) == 0 && state.getHarvestLevel() <= 0);
    }
}
