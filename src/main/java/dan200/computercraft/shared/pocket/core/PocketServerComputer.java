/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.core;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.NetworkHandler;
import dan200.computercraft.shared.pocket.items.ItemPocketComputer;
import dan200.computercraft.shared.util.NBTUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

import static dan200.computercraft.shared.pocket.items.ItemPocketComputer.NBT_LIGHT;

public class PocketServerComputer extends ServerComputer implements IPocketAccess
{
    private IPocketUpgrade m_upgrade;
    private Entity entity;
    private ItemStack m_stack;

    public PocketServerComputer( World world, int computerID, String label, int instanceID, ComputerFamily family )
    {
        super( world, computerID, label, instanceID, family, ComputerCraft.terminalWidth_pocketComputer, ComputerCraft.terminalHeight_pocketComputer );
    }

    @Nullable
    @Override
    public Entity getEntity()
    {
        return entity;
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
        CompoundTag tag = getUserData();
        return tag.containsKey( NBT_LIGHT, NBTUtil.TAG_ANY_NUMERIC ) ? tag.getInt( NBT_LIGHT ) : -1;
    }

    @Override
    public void setLight( int colour )
    {
        CompoundTag tag = getUserData();
        if( colour >= 0 && colour <= 0xFFFFFF )
        {
            if( !tag.containsKey( NBT_LIGHT, NBTUtil.TAG_ANY_NUMERIC ) || tag.getInt( NBT_LIGHT ) != colour )
            {
                tag.putInt( NBT_LIGHT, colour );
                updateUserData();
            }
        }
        else if( tag.containsKey( NBT_LIGHT, NBTUtil.TAG_ANY_NUMERIC ) )
        {
            tag.remove( NBT_LIGHT );
            updateUserData();
        }
    }

    @Nonnull
    @Override
    public CompoundTag getUpgradeNBTData()
    {
        return ItemPocketComputer.getUpgradeInfo( m_stack );
    }

    @Override
    public void updateUpgradeNBTData()
    {
        if( entity instanceof PlayerEntity ) ((PlayerEntity) entity).inventory.markDirty();
    }

    @Override
    public void invalidatePeripheral()
    {
        IPeripheral peripheral = m_upgrade == null ? null : m_upgrade.createPeripheral( this );
        setPeripheral( 2, peripheral );
    }

    @Nonnull
    @Override
    public Map<Identifier, IPeripheral> getUpgrades()
    {
        if( m_upgrade == null )
        {
            return Collections.emptyMap();
        }
        else
        {
            return Collections.singletonMap( m_upgrade.getUpgradeID(), getPeripheral( 2 ) );
        }
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
        if( this.m_upgrade == upgrade ) return;

        synchronized( this )
        {
            ItemPocketComputer.setUpgrade( m_stack, upgrade );
            updateUpgradeNBTData();
            this.m_upgrade = upgrade;
            invalidatePeripheral();
        }
    }

    public synchronized void updateValues( Entity entity, @Nonnull ItemStack stack, IPocketUpgrade upgrade )
    {
        if( entity != null )
        {
            setWorld( entity.getEntityWorld() );
            setPosition( entity.getPos() );
        }

        // If a new entity has picked it up then rebroadcast the terminal to them
        if( entity != this.entity && entity instanceof ServerPlayerEntity ) markTerminalChanged();

        this.entity = entity;
        m_stack = stack;

        if( this.m_upgrade != upgrade )
        {
            this.m_upgrade = upgrade;
            invalidatePeripheral();
        }
    }

    @Override
    public void broadcastState( boolean force )
    {
        super.broadcastState( force );

        if( (hasTerminalChanged() || force) && entity instanceof ServerPlayerEntity )
        {
            // Broadcast the state to the current entity if they're not already interacting with it.
            ServerPlayerEntity player = (ServerPlayerEntity) entity;
            if( player.networkHandler != null && !isInteracting( player ) )
            {
                NetworkHandler.sendToPlayer( player, createTerminalPacket() );
            }
        }
    }
}
