/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2017. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.turtle.upgrades;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.turtle.*;
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
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;
import java.util.List;

public class TurtleTool implements ITurtleUpgrade
{
    private ResourceLocation m_id;
    private int m_legacyId;
    private String m_adjective;
    protected ItemStack m_item;

    public TurtleTool( ResourceLocation id, int legacyID, String adjective, Item item )
    {
        m_id = id;
        m_legacyId = legacyID;
        m_adjective = adjective;
        m_item = new ItemStack( item, 1, 0 );
    }

    @Nonnull
    @Override
    public ResourceLocation getUpgradeID()
    {
        return m_id;
    }

    @Override
    public int getLegacyUpgradeID()
    {
        return m_legacyId;
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
                return attack( turtle, direction );
            }
            case Dig:
            {
                return dig( turtle, direction );
            }
            default:
            {
                return TurtleCommandResult.failure( "Unsupported action" );
            }
        }
    }

    protected boolean canBreakBlock( World world, BlockPos pos )
    {
        IBlockState state = world.getBlockState( pos );
        Block block = state.getBlock();
        return !block.isAir( state, world, pos ) && block != Blocks.BEDROCK && state.getBlockHardness( world, pos ) > -1.0F;
    }
    
    protected boolean canHarvestBlock( ITurtleAccess turtleAccess, BlockPos pos )
    {
        World world = turtleAccess.getWorld();
        Block block = world.getBlockState( pos ).getBlock();
        TurtlePlayer turtlePlayer = new TurtlePlayer( turtleAccess );
        turtlePlayer.loadInventory( m_item.copy() );
        return ForgeHooks.canHarvestBlock( block, turtlePlayer, world, pos );
    }
    
    protected float getDamageMultiplier()
    {
        return 3.0f;
    }
    
    private TurtleCommandResult attack( final ITurtleAccess turtle, EnumFacing direction )
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

            // Start claiming entity drops
            Entity hitEntity = hit.getKey();
            ComputerCraft.setEntityDropConsumer( hitEntity, ( entity, drop ) ->
            {
                ItemStack remainder = InventoryUtil.storeItems( drop, turtle.getItemHandler(), turtle.getSelectedSlot() );
                if( !remainder.isEmpty() )
                {
                    WorldUtil.dropItemStack( remainder, world, position, turtle.getDirection().getOpposite() );
                }
            } );

            // Attack the entity
            boolean attacked = false;
            if( hitEntity.canBeAttackedWithItem() && !hitEntity.hitByEntity( turtlePlayer )
                && !MinecraftForge.EVENT_BUS.post( new AttackEntityEvent( turtlePlayer, hitEntity ) ) )
            {
                float damage = (float)turtlePlayer.getEntityAttribute( SharedMonsterAttributes.ATTACK_DAMAGE ).getAttributeValue();
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
            ComputerCraft.clearEntityDropConsumer( hitEntity );

            // Put everything we collected into the turtles inventory, then return
            if( attacked )
            {
                turtlePlayer.unloadInventory( turtle );
                return TurtleCommandResult.success();
            }
        }

        return TurtleCommandResult.failure( "Nothing to attack here" );
    }
    
    private TurtleCommandResult dig( ITurtleAccess turtle, EnumFacing direction )
    {
        // Get ready to dig
        World world = turtle.getWorld();
        BlockPos position = turtle.getPosition();
        BlockPos newPosition = WorldUtil.moveCoords( position, direction );

        if( WorldUtil.isBlockInWorld( world, newPosition ) &&
            !world.isAirBlock( newPosition ) &&
            !WorldUtil.isLiquidBlock( world, newPosition ) )
        {
            TurtlePlayer turtlePlayer = TurtlePlaceCommand.createPlayer( turtle, position, direction );
            if( ComputerCraft.turtlesObeyBlockProtection )
            {
                // Check spawn protection

                if( MinecraftForge.EVENT_BUS.post( new BlockEvent.BreakEvent( world, newPosition, world.getBlockState( newPosition ), turtlePlayer ) ) )
                {
                    return TurtleCommandResult.failure( "Cannot break protected block" );
                }
                
                if( !ComputerCraft.isBlockEditable( world, newPosition, turtlePlayer ) )
                {
                    return TurtleCommandResult.failure( "Cannot break protected block" );
                }
            }

            // Check if we can break the block
            if( !canBreakBlock( world, newPosition ) )
            {
                return TurtleCommandResult.failure( "Unbreakable block detected" );
            }

            // Consume the items the block drops
            if( canHarvestBlock( turtle, newPosition ) )
            {
                List<ItemStack> items = getBlockDropped( world, newPosition, turtlePlayer );
                if( items != null && items.size() > 0 )
                {
                    for( ItemStack stack : items )
                    {
                        ItemStack remainder = InventoryUtil.storeItems( stack, turtle.getItemHandler(), turtle.getSelectedSlot() );
                        if( !remainder.isEmpty() )
                        {
                            // If there's no room for the items, drop them
                            WorldUtil.dropItemStack( remainder, world, position, direction );
                        }
                    }
                }
            }

            // Destroy the block
            IBlockState previousState = world.getBlockState( newPosition );
            world.playEvent(2001, newPosition, Block.getStateId(previousState));
            world.setBlockToAir( newPosition );

            // Remember the previous block
            if( turtle instanceof TurtleBrain )
            {
                TurtleBrain brain = (TurtleBrain)turtle;
                brain.saveBlockChange( newPosition, previousState );
            }

            return TurtleCommandResult.success();
        }

        return TurtleCommandResult.failure( "Nothing to dig here" );
    }

    private List<ItemStack> getBlockDropped( World world, BlockPos pos, EntityPlayer player )
    {
        IBlockState state = world.getBlockState( pos );
        Block block = state.getBlock();
        NonNullList<ItemStack> drops = NonNullList.create();
        block.getDrops( drops, world, pos, world.getBlockState( pos ), 0 );
        double chance = ForgeEventFactory.fireBlockHarvesting( drops, world, pos, state, 0, 1, false, player );

        for( int i = drops.size() - 1; i >= 0; i-- )
        {
            if( world.rand.nextFloat() > chance )
            {
                drops.remove( i );
            }
        }
        return drops;
    }
}
