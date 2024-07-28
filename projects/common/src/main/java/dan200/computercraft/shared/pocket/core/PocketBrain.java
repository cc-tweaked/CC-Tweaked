// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
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
import dan200.computercraft.shared.network.client.PocketComputerDataMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Map;

/**
 * Holds additional state for a pocket computer. This includes pocket computer upgrade,
 * {@linkplain IPocketAccess#getLight() light colour} and {@linkplain IPocketAccess#getColour() colour}.
 * <p>
 * This state is read when the brain is created, and written back to the holding item stack when the holding entity is
 * ticked (see {@link #updateItem(ItemStack)}).
 */
public final class PocketBrain implements IPocketAccess {
    private final PocketServerComputer computer;

    private PocketHolder holder;
    private Vec3 position;

    private boolean dirty = false;
    private @Nullable UpgradeData<IPocketUpgrade> upgrade;
    private int colour = -1;
    private int lightColour = -1;

    public PocketBrain(PocketHolder holder, int computerID, @Nullable String label, ComputerFamily family, @Nullable UpgradeData<IPocketUpgrade> upgrade) {
        this.computer = new PocketServerComputer(this, holder, computerID, label, family);
        this.holder = holder;
        this.position = holder.pos();
        this.upgrade = UpgradeData.copyOf(upgrade);
        invalidatePeripheral();
    }

    /**
     * Get the corresponding pocket computer for this brain.
     *
     * @return The pocket computer.
     */
    public PocketServerComputer computer() {
        return computer;
    }

    PocketHolder holder() {
        return holder;
    }

    /**
     * Update the position and holder for this computer.
     *
     * @param newHolder The new holder
     */
    public void updateHolder(PocketHolder newHolder) {
        position = newHolder.pos();
        computer.setPosition(newHolder.level(), newHolder.blockPos());

        var oldHolder = this.holder;
        if (holder.equals(newHolder)) return;
        holder = newHolder;

        // If a new player has picked it up then rebroadcast the terminal to them
        var oldPlayer = oldHolder instanceof PocketHolder.PlayerHolder p ? p.entity() : null;
        if (newHolder instanceof PocketHolder.PlayerHolder player && player.entity() != oldPlayer) {
            ServerNetworking.sendToPlayer(new PocketComputerDataMessage(computer, true), player.entity());
        }
    }

    /**
     * Write back properties of the pocket brain to the item.
     *
     * @param stack The pocket computer stack to update.
     * @return Whether the item was changed.
     */
    public boolean updateItem(ItemStack stack) {
        if (!dirty) return false;
        this.dirty = false;

        IColouredItem.setColourBasic(stack, colour);
        PocketComputerItem.setUpgrade(stack, UpgradeData.copyOf(upgrade));
        return true;
    }

    @Override
    public ServerLevel getLevel() {
        return computer.getLevel();
    }

    @Override
    public Vec3 getPosition() {
        // This method can be called from off-thread, and so we must use the cached position rather than rereading
        // from the holder.
        return position;
    }

    @Override
    public @Nullable Entity getEntity() {
        return holder instanceof PocketHolder.EntityHolder entity && holder.isValid(computer) ? entity.entity() : null;
    }

    @Override
    public int getColour() {
        return colour;
    }

    @Override
    public void setColour(int colour) {
        if (this.colour == colour) return;
        dirty = true;
        this.colour = colour;
    }

    @Override
    public int getLight() {
        return lightColour;
    }

    @Override
    public void setLight(int colour) {
        if (colour < 0 || colour > 0xFFFFFF) colour = -1;
        lightColour = colour;
    }

    @Override
    public CompoundTag getUpgradeNBTData() {
        var upgrade = this.upgrade;
        return upgrade == null ? new CompoundTag() : upgrade.data();
    }

    @Override
    public void updateUpgradeNBTData() {
        dirty = true;
    }

    @Override
    public void invalidatePeripheral() {
        var peripheral = upgrade == null ? null : upgrade.upgrade().createPeripheral(this);
        computer.setPeripheral(ComputerSide.BACK, peripheral);
    }

    @Override
    @Deprecated(forRemoval = true)
    public Map<ResourceLocation, IPeripheral> getUpgrades() {
        var upgrade = this.upgrade;
        return upgrade == null ? Map.of() : Collections.singletonMap(upgrade.upgrade().getUpgradeID(), computer.getPeripheral(ComputerSide.BACK));
    }

    @Override
    public @Nullable UpgradeData<IPocketUpgrade> getUpgrade() {
        return upgrade;
    }

    /**
     * Set the upgrade for this pocket computer, also updating the item stack.
     * <p>
     * Note this method is not thread safe - it must be called from the server thread.
     *
     * @param upgrade The new upgrade to set it to, may be {@code null}.
     */
    @Override
    public void setUpgrade(@Nullable UpgradeData<IPocketUpgrade> upgrade) {
        this.upgrade = upgrade;
        dirty = true;
        invalidatePeripheral();
    }
}
