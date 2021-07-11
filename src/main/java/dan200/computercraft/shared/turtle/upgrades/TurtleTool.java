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
import dan200.computercraft.api.turtle.event.TurtleEvent;
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
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3f;
import net.minecraft.world.World;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

public class TurtleTool extends AbstractTurtleUpgrade
{
    protected final ItemStack item;

    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;

    public TurtleTool( Identifier id, String adjective, Item item )
    {
        super( id, TurtleUpgradeType.TOOL, adjective, item );
        this.item = new ItemStack( item );
    }

    public TurtleTool( Identifier id, Item item )
    {
        super( id, TurtleUpgradeType.TOOL, item );
        this.item = new ItemStack( item );
    }

    public TurtleTool( Identifier id, ItemStack craftItem, ItemStack toolItem )
    {
        super( id, TurtleUpgradeType.TOOL, craftItem );
        item = toolItem;
    }

    @Override
    public boolean isItemSuitable( @Nonnull ItemStack stack )
    {
        NbtCompound tag = stack.getNbt();
        if( tag == null || tag.isEmpty() ) return true;

        // Check we've not got anything vaguely interesting on the item. We allow other mods to add their
        // own NBT, with the understanding such details will be lost to the mist of time.
        if( stack.isDamaged() || stack.hasEnchantments() || stack.hasCustomName() ) return false;
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
        float xOffset = side == TurtleSide.LEFT ? -0.40625f : 0.40625f;
        return TransformedModel.of( getCraftingItem(), new AffineTransformation( new Vec3f( xOffset + 1, 0, 1 ), Vec3f.POSITIVE_Y.getDegreesQuaternion( 270 ), new Vec3f( 1, 1, 1 ), Vec3f.POSITIVE_Z.getDegreesQuaternion( 90 ) ) );
    }

    private TurtleCommandResult attack( ITurtleAccess turtle, Direction direction, TurtleSide side )
    {
        // Create a fake player, and orient it appropriately
        World world = turtle.getWorld();
        BlockPos position = turtle.getPosition();
        BlockEntity turtleBlock = turtle instanceof TurtleBrain ? ((TurtleBrain) turtle).getOwner() : world.getBlockEntity( position );
        if( turtleBlock == null ) return TurtleCommandResult.failure( "Turtle has vanished from existence." );

        final TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, position, direction );

        // See if there is an entity present
        Vec3d turtlePos = turtlePlayer.getPos();
        Vec3d rayDir = turtlePlayer.getRotationVec( 1.0f );
        Pair<Entity, Vec3d> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
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
                    Hand.MAIN_HAND,
                    hitEntity,
                    null ) == ActionResult.FAIL || !hitEntity.isAttackable() )
            {
                return TurtleCommandResult.failure( "Nothing to attack here" );
            }

            TurtleAttackEvent attackEvent = new TurtleAttackEvent( turtle, turtlePlayer, hitEntity, this, side );
            if( TurtleEvent.post( attackEvent ) )
            {
                return TurtleCommandResult.failure( attackEvent.getFailureMessage() );
            }

            // Start claiming entity drops
            DropConsumer.set( hitEntity, turtleDropConsumer( turtleBlock, turtle ) );

            // Attack the entity
            boolean attacked = false;
            if( !hitEntity.handleAttack( turtlePlayer ) )
            {
                float damage = (float) turtlePlayer.getAttributeValue( EntityAttributes.GENERIC_ATTACK_DAMAGE );
                damage *= getDamageMultiplier();
                if( damage > 0.0f )
                {
                    DamageSource source = DamageSource.player( turtlePlayer );
                    if( hitEntity instanceof ArmorStandEntity )
                    {
                        // Special case for armor stands: attack twice to guarantee destroy
                        hitEntity.damage( source, damage );
                        if( hitEntity.isAlive() )
                        {
                            hitEntity.damage( source, damage );
                        }
                        attacked = true;
                    }
                    else
                    {
                        if( hitEntity.damage( source, damage ) )
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
        World world = turtle.getWorld();
        BlockPos turtlePosition = turtle.getPosition();
        BlockEntity turtleBlock = turtle instanceof TurtleBrain ? ((TurtleBrain) turtle).getOwner() : world.getBlockEntity( turtlePosition );
        if( turtleBlock == null ) return TurtleCommandResult.failure( "Turtle has vanished from existence." );


        BlockPos blockPosition = turtlePosition.offset( direction );

        if( world.isAir( blockPosition ) || WorldUtil.isLiquidBlock( world, blockPosition ) )
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

        // Fire the dig event, checking whether it was cancelled.
        TurtleBlockEvent.Dig digEvent = new TurtleBlockEvent.Dig( turtle, turtlePlayer, world, blockPosition, state, this, side );
        if( TurtleEvent.post( digEvent ) )
        {
            return TurtleCommandResult.failure( digEvent.getFailureMessage() );
        }

        // Consume the items the block drops
        DropConsumer.set( world, blockPosition, turtleDropConsumer( turtleBlock, turtle ) );

        BlockEntity tile = world.getBlockEntity( blockPosition );

        // Much of this logic comes from PlayerInteractionManager#tryHarvestBlock, so it's a good idea
        // to consult there before making any changes.

        // Play the destruction sound and particles
        world.syncWorldEvent( 2001, blockPosition, Block.getRawIdFromState( state ) );

        // Destroy the block
        state.getBlock()
            .onBreak( world, blockPosition, state, turtlePlayer );
        if( world.removeBlock( blockPosition, false ) )
        {
            state.getBlock()
                .onBroken( world, blockPosition, state );
            if( turtlePlayer.canHarvest( state ) )
            {
                state.getBlock()
                    .afterBreak( world, turtlePlayer, blockPosition, state, tile, turtlePlayer.getMainHandStack() );
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
                turtle.getWorld(),
                turtle.getPosition(),
                direction );
        }
    }

    protected boolean canBreakBlock( BlockState state, World world, BlockPos pos, TurtlePlayer player )
    {
        Block block = state.getBlock();
        return !state.isAir() && block != Blocks.BEDROCK && state.calcBlockBreakingDelta( player, world, pos ) > 0;
    }
}
