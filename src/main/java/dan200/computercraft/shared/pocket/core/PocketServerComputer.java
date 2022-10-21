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
import dan200.computercraft.shared.network.client.PocketComputerDataMessage;
import dan200.computercraft.shared.network.client.PocketComputerDeletedClientMessage;
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
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class PocketServerComputer extends ServerComputer implements IPocketAccess
{
    private IPocketUpgrade upgrade;
    private Entity entity;
    private ItemStack stack;

    private int lightColour = -1;
    private boolean lightChanged = false;

    private final Set<ServerPlayerEntity> tracking = new HashSet<>();

    public PocketServerComputer( ServerWorld world, int computerID, String label, ComputerFamily family )
    {
        super( world, computerID, label, family, ComputerCraft.pocketTermWidth, ComputerCraft.pocketTermHeight );
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
        return lightColour;
    }

    @Override
    public void setLight( int colour )
    {
        if( colour < 0 || colour > 0xFFFFFF ) colour = -1;

        if( lightColour == colour ) return;
        lightColour = colour;
        lightChanged = true;
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
            setWorld( (ServerWorld) entity.getCommandSenderWorld() );
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
    public void tickServer()
    {
        super.tickServer();

        // Find any players which have gone missing and remove them from the tracking list.
        tracking.removeIf( player -> !player.isAlive() || player.level != getWorld() );

        // And now find any new players, add them to the tracking list, and broadcast state where appropriate.
        boolean sendState = hasOutputChanged() || lightChanged;
        lightChanged = false;
        if( sendState )
        {
            // Broadcast the state to all players
            tracking.addAll( getWorld().players() );
            NetworkHandler.sendToPlayers( new PocketComputerDataMessage( this, false ), tracking );
        }
        else
        {
            // Broadcast the state to new players.
            List<ServerPlayerEntity> added = new ArrayList<>();
            for( ServerPlayerEntity player : getWorld().players() )
            {
                if( tracking.add( player ) ) added.add( player );
            }
            if( !added.isEmpty() )
            {
                NetworkHandler.sendToPlayers( new PocketComputerDataMessage( this, false ), added );
            }
        }
    }

    @Override
    protected void onTerminalChanged()
    {
        super.onTerminalChanged();

        if( entity instanceof ServerPlayerEntity && entity.isAlive() )
        {
            // Broadcast the terminal to the current player.
            NetworkHandler.sendToPlayer( (ServerPlayerEntity) entity, new PocketComputerDataMessage( this, true ) );
        }
    }

    @Override
    protected void onRemoved()
    {
        super.onRemoved();
        NetworkHandler.sendToAllPlayers( new PocketComputerDeletedClientMessage( getInstanceID() ) );
    }
}
