/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.pocket.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

import static dan200.computercraft.shared.pocket.items.ItemPocketComputer.NBT_LIGHT;

public class PocketServerComputer extends ServerComputer implements IPocketAccess
{
    private IPocketUpgrade m_upgrade;
    private Entity m_entity;
    private ItemStack m_stack;

    public PocketServerComputer( World world, int computerID, String label, int instanceID, ComputerFamily family )
    {
        super( world, computerID, label, instanceID, family, ComputerCraft.terminalWidth_pocketComputer, ComputerCraft.terminalHeight_pocketComputer );
    }

    @Nullable
    @Override
    public Entity getEntity()
    {
        Entity entity = m_entity;
        if( entity == null || m_stack == null || !entity.isAlive() ) return null;

        if( entity instanceof PlayerEntity )
        {
            PlayerInventory inventory = ((PlayerEntity) entity).inventory;
            return inventory.mainInventory.contains( m_stack ) || inventory.offHandInventory.contains( m_stack ) ? entity : null;
        }
        else if( entity instanceof LivingEntity )
        {
            LivingEntity living = (LivingEntity) entity;
            return living.getHeldItemMainhand() == m_stack || living.getHeldItemOffhand() == m_stack ? entity : null;
        }
        else
        {
            return null;
        }
    }

    @Override
    public int getColour()
    {
        return IColouredItem.getColourBasic( m_stack );
    }

    @Override
    public void setColour( int colour )
    {
        IColouredItem.setColourBasic( m_stack, colour );
        updateUpgradeNBTData();
    }

    @Override
    public int getLight()
    {
        CompoundNBT tag = getUserData();
        return tag.contains( NBT_LIGHT, Constants.NBT.TAG_ANY_NUMERIC ) ? tag.getInt( NBT_LIGHT ) : -1;
    }

    @Override
    public void setLight( int colour )
    {
        CompoundNBT tag = getUserData();
        if( colour >= 0 && colour <= 0xFFFFFF )
        {
            if( !tag.contains( NBT_LIGHT, Constants.NBT.TAG_ANY_NUMERIC ) || tag.getInt( NBT_LIGHT ) != colour )
            {
                tag.putInt( NBT_LIGHT, colour );
                updateUserData();
            }
        }
        else if( tag.contains( NBT_LIGHT, Constants.NBT.TAG_ANY_NUMERIC ) )
        {
            tag.remove( NBT_LIGHT );
            updateUserData();
        }
    }

    @Nonnull
    @Override
    public CompoundNBT getUpgradeNBTData()
    {
        return ItemPocketComputer.getUpgradeInfo( m_stack );
    }

    @Override
    public void updateUpgradeNBTData()
    {
        if( m_entity instanceof PlayerEntity ) ((PlayerEntity) m_entity).inventory.markDirty();
    }

    @Override
    public void invalidatePeripheral()
    {
        IPeripheral peripheral = m_upgrade == null ? null : m_upgrade.createPeripheral( this );
        setPeripheral( ComputerSide.BACK, peripheral );
    }

    @Nonnull
    @Override
    public Map<ResourceLocation, IPeripheral> getUpgrades()
    {
        return m_upgrade == null ? Collections.emptyMap() : Collections.singletonMap( m_upgrade.getUpgradeID(), getPeripheral( ComputerSide.BACK ) );
    }

    public IPocketUpgrade getUpgrade()
    {
        return m_upgrade;
    }

    /**
     * Set the upgrade for this pocket computer, also updating the item stack.
     *
     * Note this method is not thread safe - it must be called from the server thread.
     *
     * @param upgrade The new upgrade to set it to, may be {@code null}.
     */
    public void setUpgrade( IPocketUpgrade upgrade )
    {
        if( m_upgrade == upgrade ) return;

        synchronized( this )
        {
            ItemPocketComputer.setUpgrade( m_stack, upgrade );
            updateUpgradeNBTData();
            m_upgrade = upgrade;
            invalidatePeripheral();
        }
    }

    public synchronized void updateValues( Entity entity, @Nonnull ItemStack stack, IPocketUpgrade upgrade )
    {
        if( entity != null )
        {
            setWorld( entity.getEntityWorld() );
            setPosition( entity.getPosition() );
        }

        // If a new entity has picked it up then rebroadcast the terminal to them
        if( entity != m_entity && entity instanceof ServerPlayerEntity ) markTerminalChanged();

        m_entity = entity;
        m_stack = stack;

        if( m_upgrade != upgrade )
        {
            m_upgrade = upgrade;
            invalidatePeripheral();
        }
    }

    @Override
    public void broadcastState( boolean force )
    {
        super.broadcastState( force );

        if( (hasTerminalChanged() || force) && m_entity instanceof ServerPlayerEntity )
        {
            // Broadcast the state to the current entity if they're not already interacting with it.
            ServerPlayerEntity player = (ServerPlayerEntity) m_entity;
            if( player.connection != null && !isInteracting( player ) )
            {
                NetworkHandler.sendToPlayer( player, createTerminalPacket() );
            }
        }
    }
}
