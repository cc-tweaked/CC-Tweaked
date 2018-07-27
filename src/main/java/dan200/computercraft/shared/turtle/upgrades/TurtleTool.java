/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
import dan200.computercraft.api.turtle.event.TurtleAttackEvent;
import dan200.computercraft.api.turtle.event.TurtleBlockEvent;
import dan200.computercraft.shared.turtle.core.TurtleBrain;
import dan200.computercraft.shared.turtle.core.TurtlePlaceCommand;
import dan200.computercraft.shared.turtle.core.TurtlePlayer;
import dan200.computercraft.shared.util.InventoryUtil;
import dan200.computercraft.shared.util.WorldUtil;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.item.EntityArmorStand;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class TurtleTool implements ITurtleUpgrade
{
    private ResourceLocation m_id;
    private String m_adjective;
    protected ItemStack m_item;

    public TurtleTool( ResourceLocation id, String adjective, Item item )
    {
        m_id = id;
        m_adjective = adjective;
        m_item = new ItemStack( item, 1, 0 );
    }

    @Nonnull
    @Override
    public ResourceLocation getUpgradeID()
    {
        return m_id;
    }

    @Nonnull
    @Override
    public String getUnlocalisedAdjective()
    {
        return m_adjective;
    }

    @Nonnull
    @Override
    public TurtleUpgradeType getType()
    {
        return TurtleUpgradeType.Tool;
    }

    @Nonnull
    @Override
    public ItemStack getCraftingItem()
    {
        return m_item.copy();
    }

    @Override
    public IPeripheral createPeripheral( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        return null;
    }

    @Nonnull
    @Override
    @SideOnly( Side.CLIENT )
    public Pair<IBakedModel, Matrix4f> getModel( ITurtleAccess turtle, @Nonnull TurtleSide side )
    {
        float xOffset = (side == TurtleSide.Left) ? -0.40625f : 0.40625f;
        Matrix4f transform = new Matrix4f(
            0.0f, 0.0f, -1.0f, 1.0f + xOffset,
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, -1.0f, 0.0f, 1.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        );
        Minecraft mc = Minecraft.getMinecraft();
        return Pair.of(
            mc.getRenderItem().getItemModelMesher().getItemModel( m_item ),
            transform
        );
    }

    @Nonnull
    @Override
    public TurtleCommandResult useTool( @Nonnull ITurtleAccess turtle, @Nonnull TurtleSide side, @Nonnull TurtleVerb verb, @Nonnull EnumFacing direction )
    {
        switch( verb )
        {
            case Attack:
            {
                return attack( turtle, direction, side );
            }
            case Dig:
            {
                return dig( turtle, direction, side );
            }
            default:
            {
                return TurtleCommandResult.failure( "Unsupported action" );
            }
        }
    }

    protected boolean canBreakBlock( IBlockState state, World world, BlockPos pos, TurtlePlayer player )
    {
        Block block = state.getBlock();
        return !block.isAir( state, world, pos )
            && block != Blocks.BEDROCK
            && state.getPlayerRelativeBlockHardness( player, world, pos ) > 0
            && block.canEntityDestroy( state, world, pos, player );
    }

    protected float getDamageMultiplier()
    {
        return 3.0f;
    }

    private TurtleCommandResult attack( final ITurtleAccess turtle, EnumFacing direction, TurtleSide side )
    {
        // Create a fake player, and orient it appropriately
        final World world = turtle.getWorld();
        final BlockPos position = turtle.getPosition();
        final TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, position, direction );

        // See if there is an entity present
        Vec3d turtlePos = new Vec3d( turtlePlayer.posX, turtlePlayer.posY, turtlePlayer.posZ );
        Vec3d rayDir = turtlePlayer.getLook( 1.0f );
        Pair<Entity, Vec3d> hit = WorldUtil.rayTraceEntities( world, turtlePos, rayDir, 1.5 );
        if( hit != null )
        {
            // Load up the turtle's inventory
            ItemStack stackCopy = m_item.copy();
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
            List<ItemStack> extra = new ArrayList<>();
            ComputerCraft.setDropConsumer( hitEntity, turtleDropConsumer( turtle, extra ) );

            // Attack the entity
            boolean attacked = false;
            if( !hitEntity.hitByEntity( turtlePlayer ) )
            {
                float damage = (float) turtlePlayer.getEntityAttribute( SharedMonsterAttributes.ATTACK_DAMAGE ).getAttributeValue();
                damage *= getDamageMultiplier();
                if( damage > 0.0f )
                {
                    DamageSource source = DamageSource.causePlayerDamage( turtlePlayer );
                    if( hitEntity instanceof EntityArmorStand )
                    {
                        // Special case for armor stands: attack twice to guarantee destroy
                        hitEntity.attackEntityFrom( source, damage );
                        if( !hitEntity.isDead )
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
            stopConsuming( turtle, extra );

            // Put everything we collected into the turtles inventory, then return
            if( attacked )
            {
                turtlePlayer.unloadInventory( turtle );
                return TurtleCommandResult.success();
            }
        }

        return TurtleCommandResult.failure( "Nothing to attack here" );
    }

    private TurtleCommandResult dig( ITurtleAccess turtle, EnumFacing direction, TurtleSide side )
    {
        // Get ready to dig
        World world = turtle.getWorld();
        BlockPos turtlePosition = turtle.getPosition();
        BlockPos blockPosition = WorldUtil.moveCoords( turtlePosition, direction );

        if( WorldUtil.isBlockInWorld( world, blockPosition ) &&
            !world.isAirBlock( blockPosition ) &&
            !WorldUtil.isLiquidBlock( world, blockPosition ) )
        {
            IBlockState state = world.getBlockState( blockPosition );

            TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, turtlePosition, direction );
            turtlePlayer.loadInventory( m_item.copy() );

            if( ComputerCraft.turtlesObeyBlockProtection )
            {
                // Check spawn protection
                if( MinecraftForge.EVENT_BUS.post( new BlockEvent.BreakEvent( world, blockPosition, state, turtlePlayer ) ) )
                {
                    return TurtleCommandResult.failure( "Cannot break protected block" );
                }

                if( !ComputerCraft.isBlockEditable( world, blockPosition, turtlePlayer ) )
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
            List<ItemStack> extra = new ArrayList<>();
            ComputerCraft.setDropConsumer( world, blockPosition, turtleDropConsumer( turtle, extra ) );

            TileEntity tile = world.getTileEntity( blockPosition );

            // Much of this logic comes from PlayerInteractionManager#tryHarvestBlock, so it's a good idea
            // to consult there before making any changes.

            // Play the destruction sound
            world.playEvent( 2001, blockPosition, Block.getStateId( state ) );

            // Destroy the block
            boolean canHarvest = state.getBlock().canHarvestBlock( world, blockPosition, turtlePlayer );
            boolean canBreak = state.getBlock().removedByPlayer( state, world, blockPosition, turtlePlayer, canHarvest );
            if( canBreak ) state.getBlock().onPlayerDestroy( world, blockPosition, state );
            if( canHarvest )
            {
                state.getBlock().harvestBlock( world, turtlePlayer, blockPosition, state, tile, turtlePlayer.getHeldItemMainhand() );
            }

            stopConsuming( turtle, extra );

            // Remember the previous block
            if( turtle instanceof TurtleBrain )
            {
                TurtleBrain brain = (TurtleBrain) turtle;
                brain.saveBlockChange( blockPosition, state );
            }

            return TurtleCommandResult.success();
        }

        return TurtleCommandResult.failure( "Nothing to dig here" );
    }

    private Consumer<ItemStack> turtleDropConsumer( ITurtleAccess turtle, List<ItemStack> extra )
    {
        return ( drop ) ->
        {
            ItemStack remainder = InventoryUtil.storeItems( drop, turtle.getItemHandler(), turtle.getSelectedSlot() );
            if( !remainder.isEmpty() ) extra.add( remainder );
        };
    }

    private void stopConsuming( ITurtleAccess turtle, List<ItemStack> extra )
    {
        ComputerCraft.clearDropConsumer();
        for( ItemStack remainder : extra )
        {
            WorldUtil.dropItemStack( remainder, turtle.getWorld(), turtle.getPosition(), turtle.getDirection().getOpposite() );
        }
    }
}
