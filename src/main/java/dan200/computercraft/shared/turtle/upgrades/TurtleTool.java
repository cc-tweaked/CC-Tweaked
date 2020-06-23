/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.api.turtle.event.TurtleAttackEvent;
import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import dan200.computercraft.shared.TurtlePermissions;
import dan200.computercraft.shared.turtle.core.TurtlePlaceCommand;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.DropConsumer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.fluid.IFluidState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;

public class TurtleTool extends AbstractTurtleUpgrade
{
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
        this.item = toolItem;
    }

    @Nonnull
    @Override
    @OnlyIn( Dist.CLIENT )
    public TransformedModel getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        float xOffset = side == TurtleSide.LEFT ? -0.40625f : 0.40625f;
        Matrix4f transform = new Matrix4f( new float[] {
            0.0f, 0.0f, -1.0f, 1.0f + xOffset,
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f,
        } );
        return TransformedModel.of( getCraftingItem(), new TransformationMatrix( transform ) );
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

    protected boolean canBreakBlock( BlockState state, World world, BlockPos pos, TurtlePlayer player )
    {
        Block block = state.getBlock();
        return !state.isAir( world, pos )
            && block != Blocks.BEDROCK
            && state.getPlayerRelativeBlockHardness( player, world, pos ) > 0
            && block.canEntityDestroy( state, world, pos, player );
    }

    protected float getDamageMultiplier()
    {
        return 3.0f;
    }

    private TurtleCommandResult attack( final ITurtleAccess turtle, Direction direction, TurtleSide side )
    {
        // Create a fake player, and orient it appropriately
        final World world = turtle.getWorld();
        final BlockPos position = turtle.getPosition();
        final TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, position, direction );

        // See if there is an entity present
        Vec3d turtlePos = turtlePlayer.getPositionVec();
        Vec3d rayDir = turtlePlayer.getLook( 1.0f );
        Pair<Entity, Vec3d> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit != null )
        {
            // Load up the turtle's inventory
            ItemStack stackCopy = item.copy();
            turtlePlayer.loadInventory( stackCopy );

            Entity hitEntity = hit.getKey();

            // Fire several events to ensure we have permissions.
            if( MinecraftForge.EVENT_BUS.post( new AttackEntityEvent( turtlePlayer, hitEntity ) ) || !hitEntity.canBeAttackedWithItem() )
            {
                return TurtleCommandResult.failure( "Nothing to attack here" );
            }

            TurtleAttackEvent attackEvent = new TurtleAttackEvent( turtle, turtlePlayer, hitEntity, this, side );
            if( MinecraftForge.EVENT_BUS.post( attackEvent ) )
            {
                return TurtleCommandResult.failure( attackEvent.getFailureMessage() );
            }

            // Start claiming entity drops
            DropConsumer.set( hitEntity, turtleDropConsumer( turtle ) );

            // Attack the entity
            boolean attacked = false;
            if( !hitEntity.hitByEntity( turtlePlayer ) )
            {
                float damage = (float) turtlePlayer.getAttribute( SharedMonsterAttributes.ATTACK_DAMAGE ).getValue();
                damage *= getDamageMultiplier();
                if( damage > 0.0f )
                {
                    DamageSource source = DamageSource.causePlayerDamage( turtlePlayer );
                    if( hitEntity instanceof ArmorStandEntity )
                    {
                        // Special case for armor stands: attack twice to guarantee destroy
                        hitEntity.attackEntityFrom( source, damage );
                        if( hitEntity.isAlive() )
                        {
                            hitEntity.attackEntityFrom( source, damage );
                        }
                        attacked = true;
                    }
                    else
                    {
                        if( hitEntity.attackEntityFrom( source, damage ) )
                        {
                            attacked = true;
                        }
                    }
                }
            }

            // Stop claiming drops
            stopConsuming( turtle );

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
        BlockPos blockPosition = turtlePosition.offset( direction );

        if( world.isAirBlock( blockPosition ) || WorldUtil.isLiquidBlock( world, blockPosition ) )
        {
            return TurtleCommandResult.failure( "Nothing to dig here" );
        }

        BlockState state = world.getBlockState( blockPosition );
        IFluidState fluidState = world.getFluidState( blockPosition );

        TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, turtlePosition, direction );
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
        if( !canBreakBlock( state, world, blockPosition, turtlePlayer ) )
        {
            return TurtleCommandResult.failure( "Unbreakable block detected" );
        }

        // Fire the dig event, checking whether it was cancelled.
        TurtleBlockEvent.Dig digEvent = new TurtleBlockEvent.Dig( turtle, turtlePlayer, world, blockPosition, state, this, side );
        if( MinecraftForge.EVENT_BUS.post( digEvent ) )
        {
            return TurtleCommandResult.failure( digEvent.getFailureMessage() );
        }

        // Consume the items the block drops
        DropConsumer.set( world, blockPosition, turtleDropConsumer( turtle ) );

        TileEntity tile = world.getTileEntity( blockPosition );

        // Much of this logic comes from PlayerInteractionManager#tryHarvestBlock, so it's a good idea
        // to consult there before making any changes.

        // Play the destruction sound and particles
        world.playEvent( 2001, blockPosition, Block.getStateId( state ) );

        // Destroy the block
        boolean canHarvest = state.canHarvestBlock( world, blockPosition, turtlePlayer );
        boolean canBreak = state.removedByPlayer( world, blockPosition, turtlePlayer, canHarvest, fluidState );
        if( canBreak ) state.getBlock().onPlayerDestroy( world, blockPosition, state );
        if( canHarvest && canBreak )
        {
            state.getBlock().harvestBlock( world, turtlePlayer, blockPosition, state, tile, turtlePlayer.getHeldItemMainhand() );
        }

        stopConsuming( turtle );

        return TurtleCommandResult.success();

    }

    private static Function<ItemStack, ItemStack> turtleDropConsumer( ITurtleAccess turtle )
    {
        return drop -> InventoryUtil.storeItems( drop, turtle.getItemHandler(), turtle.getSelectedSlot() );
    }

    private static void stopConsuming( ITurtleAccess turtle )
    {
        List<ItemStack> extra = DropConsumer.clear();
        for( ItemStack remainder : extra )
        {
            WorldUtil.dropItemStack( remainder, turtle.getWorld(), turtle.getPosition(), turtle.getDirection().getOpposite() );
        }
    }
}
