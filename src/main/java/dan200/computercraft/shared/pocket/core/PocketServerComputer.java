/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.pocket.core;

import static dan200.computercraft.shared.pocket.items.ItemPocketComputer.NBT_LIGHT;

import java.util.Collections;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
import dan200.computercraft.shared.util.NBTUtil;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

public class PocketServerComputer extends ServerComputer implements IPocketAccess {
    private IPocketUpgrade m_upgrade;
    private Entity m_entity;
    private ItemStack m_stack;

    public PocketServerComputer(World world, int computerID, String label, int instanceID, ComputerFamily family) {
        super(world, computerID, label, instanceID, family, ComputerCraft.terminalWidth_pocketComputer, ComputerCraft.terminalHeight_pocketComputer);
    }

    @Nullable
    @Override
    public Entity getEntity() {
        Entity entity = this.m_entity;
        if (entity == null || this.m_stack == null || !entity.isAlive()) {
            return null;
        }

        if (entity instanceof PlayerEntity) {
            PlayerInventory inventory = ((PlayerEntity) entity).inventory;
            return inventory.main.contains(this.m_stack) || inventory.offHand.contains(this.m_stack) ? entity : null;
        } else if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) entity;
            return living.getMainHandStack() == this.m_stack || living.getOffHandStack() == this.m_stack ? entity : null;
        } else {
            return null;
        }
    }

    @Override
    public int getColour() {
        return IColouredItem.getColourBasic(this.m_stack);
    }

    @Override
    public void setColour(int colour) {
        IColouredItem.setColourBasic(this.m_stack, colour);
        this.updateUpgradeNBTData();
    }

    @Override
    public int getLight() {
        CompoundTag tag = this.getUserData();
        return tag.contains(NBT_LIGHT, NBTUtil.TAG_ANY_NUMERIC) ? tag.getInt(NBT_LIGHT) : -1;
    }

    @Override
    public void setLight(int colour) {
        CompoundTag tag = this.getUserData();
        if (colour >= 0 && colour <= 0xFFFFFF) {
            if (!tag.contains(NBT_LIGHT, NBTUtil.TAG_ANY_NUMERIC) || tag.getInt(NBT_LIGHT) != colour) {
                tag.putInt(NBT_LIGHT, colour);
                this.updateUserData();
            }
        } else if (tag.contains(NBT_LIGHT, NBTUtil.TAG_ANY_NUMERIC)) {
            tag.remove(NBT_LIGHT);
            this.updateUserData();
        }
    }

    @Nonnull
    @Override
    public CompoundTag getUpgradeNBTData() {
        return ItemPocketComputer.getUpgradeInfo(this.m_stack);
    }

    @Override
    public void updateUpgradeNBTData() {
        if (this.m_entity instanceof PlayerEntity) {
            ((PlayerEntity) this.m_entity).inventory.markDirty();
        }
    }

    @Override
    public void invalidatePeripheral() {
        IPeripheral peripheral = this.m_upgrade == null ? null : this.m_upgrade.createPeripheral(this);
        this.setPeripheral(ComputerSide.BACK, peripheral);
    }

    @Nonnull
    @Override
    public Map<Identifier, IPeripheral> getUpgrades() {
        return this.m_upgrade == null ? Collections.emptyMap() : Collections.singletonMap(this.m_upgrade.getUpgradeID(), this.getPeripheral(ComputerSide.BACK));
    }

    public IPocketUpgrade getUpgrade() {
        return this.m_upgrade;
    }

    /**
     * Set the upgrade for this pocket computer, also updating the item stack.
     *
     * Note this method is not thread safe - it must be called from the server thread.
     *
     * @param upgrade The new upgrade to set it to, may be {@code null}.
     */
    public void setUpgrade(IPocketUpgrade upgrade) {
        if (this.m_upgrade == upgrade) {
            return;
        }

        synchronized (this) {
            ItemPocketComputer.setUpgrade(this.m_stack, upgrade);
            this.updateUpgradeNBTData();
            this.m_upgrade = upgrade;
            this.invalidatePeripheral();
        }
    }

    public synchronized void updateValues(Entity entity, @Nonnull ItemStack stack, IPocketUpgrade upgrade) {
        if (entity != null) {
            this.setWorld(entity.getEntityWorld());
            this.setPosition(entity.getBlockPos());
        }

        // If a new entity has picked it up then rebroadcast the terminal to them
        if (entity != this.m_entity && entity instanceof ServerPlayerEntity) {
            this.markTerminalChanged();
        }

        this.m_entity = entity;
        this.m_stack = stack;

        if (this.m_upgrade != upgrade) {
            this.m_upgrade = upgrade;
            this.invalidatePeripheral();
        }
    }

    @Override
    public void broadcastState(boolean force) {
        super.broadcastState(force);

        if ((this.hasTerminalChanged() || force) && this.m_entity instanceof ServerPlayerEntity) {
            // Broadcast the state to the current entity if they're not already interacting with it.
            ServerPlayerEntity player = (ServerPlayerEntity) this.m_entity;
            if (player.networkHandler != null && !this.isInteracting(player)) {
                NetworkHandler.sendToPlayer(player, this.createTerminalPacket());
            }
        }
    }
}
