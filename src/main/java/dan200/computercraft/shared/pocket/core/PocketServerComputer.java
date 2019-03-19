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
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.NetworkHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

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
    @Deprecated
    public Entity getEntity()
    {
        return m_entity;
    }

    @Nullable
    @Override
    public Entity getValidEntity()
    {
        Entity entity = m_entity;
        if( entity == null || m_stack == null || entity.isDead ) return null;

        if( m_entity instanceof EntityPlayer )
        {
            InventoryPlayer inventory = ((EntityPlayer) m_entity).inventory;
            return inventory.mainInventory.contains( m_stack ) || inventory.offHandInventory.contains( m_stack ) ? entity : null;
        }
        else if( m_entity instanceof EntityLivingBase )
        {
            EntityLivingBase living = (EntityLivingBase) m_entity;
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
        return ComputerCraft.Items.pocketComputer.getColour( m_stack );
    }

    @Override
    public void setColour( int colour )
    {
        ComputerCraft.Items.pocketComputer.setColourDirect( m_stack, colour );
        updateUpgradeNBTData();
    }

    @Override
    public int getLight()
    {
        NBTTagCompound tag = getUserData();
        if( tag.hasKey( "modemLight", Constants.NBT.TAG_ANY_NUMERIC ) )
        {
            return tag.getInteger( "modemLight" );
        }
        else
        {
            return -1;
        }
    }

    @Override
    public void setLight( int colour )
    {
        NBTTagCompound tag = getUserData();
        if( colour >= 0 && colour <= 0xFFFFFF )
        {
            if( !tag.hasKey( "modemLight", Constants.NBT.TAG_ANY_NUMERIC ) || tag.getInteger( "modemLight" ) != colour )
            {
                tag.setInteger( "modemLight", colour );
                updateUserData();
            }
        }
        else if( tag.hasKey( "modemLight", Constants.NBT.TAG_ANY_NUMERIC ) )
        {
            tag.removeTag( "modemLight" );
            updateUserData();
        }
    }

    @Nonnull
    @Override
    public NBTTagCompound getUpgradeNBTData()
    {
        return ComputerCraft.Items.pocketComputer.getUpgradeInfo( m_stack );
    }

    @Override
    public void updateUpgradeNBTData()
    {
        InventoryPlayer inventory = m_entity instanceof EntityPlayer ? ((EntityPlayer) m_entity).inventory : null;
        if( inventory != null )
        {
            inventory.markDirty();
        }
    }

    @Override
    public void invalidatePeripheral()
    {
        IPeripheral peripheral = m_upgrade == null ? null : m_upgrade.createPeripheral( this );
        setPeripheral( 2, peripheral );
    }

    @Nonnull
    @Override
    public Map<ResourceLocation, IPeripheral> getUpgrades()
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
            ComputerCraft.Items.pocketComputer.setUpgrade( m_stack, upgrade );
            if( m_entity instanceof EntityPlayer ) ((EntityPlayer) m_entity).inventory.markDirty();

            this.m_upgrade = upgrade;
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
        if( entity != m_entity && entity instanceof EntityPlayerMP ) markTerminalChanged();

        m_entity = entity;
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

        if( (hasTerminalChanged() || force) && m_entity instanceof EntityPlayerMP )
        {
            // Broadcast the state to the current entity if they're not already interacting with it.
            EntityPlayerMP player = (EntityPlayerMP) m_entity;
            if( player.connection != null && !isInteracting( player ) )
            {
                NetworkHandler.sendToPlayer( player, createTerminalPacket() );
            }
        }
    }
}
