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
        Entity entity = this.entity;
        if( entity == null || m_stack == null || entity.removed ) return null;

        if( entity instanceof EntityPlayer )
        {
            InventoryPlayer inventory = ((EntityPlayer) entity).inventory;
            return inventory.mainInventory.contains( m_stack ) || inventory.offHandInventory.contains( m_stack ) ? entity : null;
        }
        else if( entity instanceof EntityLivingBase )
        {
            EntityLivingBase living = (EntityLivingBase) entity;
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
        NBTTagCompound tag = getUserData();
        return tag.contains( NBT_LIGHT, Constants.NBT.TAG_ANY_NUMERIC ) ? tag.getInt( NBT_LIGHT ) : -1;
    }

    @Override
    public void setLight( int colour )
    {
        NBTTagCompound tag = getUserData();
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
    public NBTTagCompound getUpgradeNBTData()
    {
        return ItemPocketComputer.getUpgradeInfo( m_stack );
    }

    @Override
    public void updateUpgradeNBTData()
    {
        if( entity instanceof EntityPlayer ) ((EntityPlayer) entity).inventory.markDirty();
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
            setPosition( entity.getPosition() );
        }

        // If a new entity has picked it up then rebroadcast the terminal to them
        if( entity != this.entity && entity instanceof EntityPlayerMP ) markTerminalChanged();

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

        if( (hasTerminalChanged() || force) && entity instanceof EntityPlayerMP )
        {
            // Broadcast the state to the current entity if they're not already interacting with it.
            EntityPlayerMP player = (EntityPlayerMP) entity;
            if( player.connection != null && !isInteracting( player ) )
            {
                NetworkHandler.sendToPlayer( player, createTerminalPacket() );
            }
        }
    }
}
