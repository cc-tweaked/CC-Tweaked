// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.core;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.shared.common.IColouredItem;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.config.Config;
import dan200.computercraft.shared.network.client.PocketComputerDataMessage;
import dan200.computercraft.shared.network.client.PocketComputerDeletedClientMessage;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public class PocketServerComputer extends ServerComputer implements IPocketAccess {
    private @Nullable IPocketUpgrade upgrade;
    private @Nullable Entity entity;
    private ItemStack stack = ItemStack.EMPTY;

    private int lightColour = -1;
    private boolean lightChanged = false;

    private final Set<ServerPlayer> tracking = new HashSet<>();

    public PocketServerComputer(ServerLevel world, BlockPos position, int computerID, @Nullable String label, ComputerFamily family) {
        super(world, position, computerID, label, family, Config.pocketTermWidth, Config.pocketTermHeight);
    }

    @Nullable
    @Override
    public Entity getEntity() {
        var entity = this.entity;
        if (entity == null || stack.isEmpty() || !entity.isAlive()) return null;

        if (entity instanceof Player) {
            var inventory = ((Player) entity).getInventory();
            return inventory.items.contains(stack) || inventory.offhand.contains(stack) ? entity : null;
        } else if (entity instanceof LivingEntity living) {
            return living.getMainHandItem() == stack || living.getOffhandItem() == stack ? entity : null;
        } else if (entity instanceof ItemEntity itemEntity) {
            return itemEntity.getItem() == stack ? entity : null;
        } else {
            return null;
        }
    }

    @Override
    public int getColour() {
        return IColouredItem.getColourBasic(stack);
    }

    @Override
    public void setColour(int colour) {
        IColouredItem.setColourBasic(stack, colour);
        updateUpgradeNBTData();
    }

    @Override
    public int getLight() {
        return lightColour;
    }

    @Override
    public void setLight(int colour) {
        if (colour < 0 || colour > 0xFFFFFF) colour = -1;

        if (lightColour == colour) return;
        lightColour = colour;
        lightChanged = true;
    }

    @Override
    public CompoundTag getUpgradeNBTData() {
        return PocketComputerItem.getUpgradeInfo(stack);
    }

    @Override
    public void updateUpgradeNBTData() {
        if (entity instanceof Player player) player.getInventory().setChanged();
    }

    @Override
    public void invalidatePeripheral() {
        var peripheral = upgrade == null ? null : upgrade.createPeripheral(this);
        setPeripheral(ComputerSide.BACK, peripheral);
    }

    @Override
    @Deprecated(forRemoval = true)
    public Map<ResourceLocation, IPeripheral> getUpgrades() {
        return upgrade == null ? Map.of() : Collections.singletonMap(upgrade.getUpgradeID(), getPeripheral(ComputerSide.BACK));
    }

    public @Nullable UpgradeData<IPocketUpgrade> getUpgrade() {
        return upgrade == null ? null : UpgradeData.of(upgrade, getUpgradeNBTData());
    }

    /**
     * Set the upgrade for this pocket computer, also updating the item stack.
     * <p>
     * Note this method is not thread safe - it must be called from the server thread.
     *
     * @param upgrade The new upgrade to set it to, may be {@code null}.
     */
    public void setUpgrade(@Nullable UpgradeData<IPocketUpgrade> upgrade) {
        synchronized (this) {
            PocketComputerItem.setUpgrade(stack, upgrade);
            updateUpgradeNBTData();
            this.upgrade = upgrade == null ? null : upgrade.upgrade();
            invalidatePeripheral();
        }
    }

    public synchronized void updateValues(@Nullable Entity entity, ItemStack stack, @Nullable IPocketUpgrade upgrade) {
        if (entity != null) {
            setLevel((ServerLevel) entity.getCommandSenderWorld());
            setPosition(entity.blockPosition());
        }

        // If a new entity has picked it up then rebroadcast the terminal to them
        if (entity != this.entity && entity instanceof ServerPlayer) markTerminalChanged();

        this.entity = entity;
        this.stack = stack;

        if (this.upgrade != upgrade) {
            this.upgrade = upgrade;
            invalidatePeripheral();
        }
    }

    @Override
    public void tickServer() {
        super.tickServer();

        // Find any players which have gone missing and remove them from the tracking list.
        tracking.removeIf(player -> !player.isAlive() || player.level() != getLevel());

        // And now find any new players, add them to the tracking list, and broadcast state where appropriate.
        var sendState = hasOutputChanged() || lightChanged;
        lightChanged = false;
        if (sendState) {
            // Broadcast the state to all players
            tracking.addAll(getLevel().players());
            PlatformHelper.get().sendToPlayers(new PocketComputerDataMessage(this, false), tracking);
        } else {
            // Broadcast the state to new players.
            List<ServerPlayer> added = new ArrayList<>();
            for (var player : getLevel().players()) {
                if (tracking.add(player)) added.add(player);
            }
            if (!added.isEmpty()) {
                PlatformHelper.get().sendToPlayers(new PocketComputerDataMessage(this, false), added);
            }
        }
    }

    @Override
    protected void onTerminalChanged() {
        super.onTerminalChanged();

        if (entity instanceof ServerPlayer player && entity.isAlive()) {
            // Broadcast the terminal to the current player.
            PlatformHelper.get().sendToPlayer(new PocketComputerDataMessage(this, true), player);
        }
    }

    @Override
    protected void onRemoved() {
        super.onRemoved();
        PlatformHelper.get().sendToAllPlayers(new PocketComputerDeletedClientMessage(getInstanceID()), getLevel().getServer());
    }
}
