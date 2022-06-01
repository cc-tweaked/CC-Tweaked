/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
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
import net.minecraft.entity.item.ItemEntity;
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
    private IPocketUpgrade upgrade;
    private Entity entity;
    private ItemStack stack;

    public PocketServerComputer( World world, int computerID, String label, int instanceID, ComputerFamily family )
    {
        super( world, computerID, label, instanceID, family, ComputerCraft.pocketTermWidth, ComputerCraft.pocketTermHeight );
    }

    @Nullable
    @Override
    public Entity getEntity()
    {
        Entity entity = this.entity;
        if( entity == null || stack == null || !entity.isAlive() ) return null;

        if( entity instanceof PlayerEntity )
        {
            PlayerInventory inventory = ((PlayerEntity) entity).inventory;
            return inventory.items.contains( stack ) || inventory.offhand.contains( stack ) ? entity : null;
        }
        else if( entity instanceof LivingEntity )
        {
            LivingEntity living = (LivingEntity) entity;
            return living.getMainHandItem() == stack || living.getOffhandItem() == stack ? entity : null;
        }
        else if( entity instanceof ItemEntity )
        {
            ItemEntity itemEntity = (ItemEntity) entity;
            return itemEntity.getItem() == stack ? entity : null;
        }
        else
        {
            return null;
        }
    }

    @Override
    public int getColour()
    {
        return IColouredItem.getColourBasic( stack );
    }

    @Override
    public void setColour( int colour )
    {
        IColouredItem.setColourBasic( stack, colour );
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
        return ItemPocketComputer.getUpgradeInfo( stack );
    }

    @Override
    public void updateUpgradeNBTData()
    {
        if( entity instanceof PlayerEntity ) ((PlayerEntity) entity).inventory.setChanged();
    }

    @Override
    public void invalidatePeripheral()
    {
        IPeripheral peripheral = upgrade == null ? null : upgrade.createPeripheral( this );
        setPeripheral( ComputerSide.BACK, peripheral );
    }

    @Nonnull
    @Override
    public Map<ResourceLocation, IPeripheral> getUpgrades()
    {
        return upgrade == null ? Collections.emptyMap() : Collections.singletonMap( upgrade.getUpgradeID(), getPeripheral( ComputerSide.BACK ) );
    }

    public IPocketUpgrade getUpgrade()
    {
        return upgrade;
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
        if( this.upgrade == upgrade ) return;

        synchronized( this )
        {
            ItemPocketComputer.setUpgrade( stack, upgrade );
            updateUpgradeNBTData();
            this.upgrade = upgrade;
            invalidatePeripheral();
        }
    }

    public synchronized void updateValues( Entity entity, @Nonnull ItemStack stack, IPocketUpgrade upgrade )
    {
        if( entity != null )
        {
            setWorld( entity.getCommandSenderWorld() );
            setPosition( entity.blockPosition() );
        }

        // If a new entity has picked it up then rebroadcast the terminal to them
        if( entity != this.entity && entity instanceof ServerPlayerEntity ) markTerminalChanged();

        this.entity = entity;
        this.stack = stack;

        if( this.upgrade != upgrade )
        {
            this.upgrade = upgrade;
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
            if( player.connection != null && !isInteracting( player ) )
            {
                NetworkHandler.sendToPlayer( player, createTerminalPacket() );
            }
        }
    }
}
