// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.core;

import dan200.computercraft.api.pocket.IPocketAccess;
import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.core.computer.ComputerSide;
import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.network.client.PocketComputerDataMessage;
import dan200.computercraft.shared.network.server.ServerNetworking;
import dan200.computercraft.shared.util.DataComponentUtil;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Holds additional state for a pocket computer. This includes pocket computer upgrade,
 * {@linkplain IPocketAccess#getLight() light colour} and {@linkplain IPocketAccess#getColour() colour}.
 * <p>
 * This state is read when the brain is created, and then written back to the item whenever changed.
 */
public final class PocketBrain implements PocketComputerInternal {
    private final PocketServerComputer computer;

    private PocketHolder holder;
    private Vec3 position;

    private final Map<PocketSide, UpgradeAccess> upgrades = new EnumMap<>(PocketSide.class);

    public PocketBrain(PocketHolder holder, ServerComputer.Properties properties) {
        this.computer = new PocketServerComputer(this, holder, properties);
        this.holder = holder;
        this.position = holder.pos();

        upgrades.put(PocketSide.BACK, new UpgradeAccess(ModRegistry.DataComponents.BACK_POCKET_UPGRADE.get(), ComputerSide.BACK));
        upgrades.put(PocketSide.BOTTOM, new UpgradeAccess(ModRegistry.DataComponents.BOTTOM_POCKET_UPGRADE.get(), ComputerSide.BOTTOM));
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

    private void requireMainThread() {
        if (!computer.getLevel().getServer().isSameThread()) {
            throw new IllegalStateException("Must be called from the main thread");
        }
    }

    private ItemStack requireStack() {
        requireMainThread();
        var stack = holder.getStack(computer);
        if (stack.isEmpty()) throw new IllegalStateException("Pocket computer is not active");
        return stack;
    }

    @Override
    public @Nullable Entity getEntity() {
        requireMainThread();
        return holder instanceof PocketHolder.EntityHolder entity && holder.isValid(computer) ? entity.entity() : null;
    }

    @Override
    public boolean isActive() {
        requireMainThread();
        return holder.isValid(computer);
    }

    @Override
    public int getColour() {
        return DyedItemColor.getOrDefault(requireStack(), -1);
    }

    @Override
    public void setColour(int colour) {
        var stack = requireStack();

        if (DyedItemColor.getOrDefault(stack, -1) == colour) return;

        if (colour == -1) {
            stack.remove(DataComponents.DYED_COLOR);
        } else {
            DataComponentUtil.setDyeColour(stack, colour);
        }
        holder.setChanged();
    }

    public int getLight() {
        // Take the average of all upgrade lights. This is very naive, and just works in sRGB, rather than
        // linear colour space.
        int count = 0, totalR = 0, totalG = 0, totalB = 0;
        for (var upgrade : upgrades.values()) {
            var colour = upgrade.lightColour;
            if (colour == -1) continue;

            count++;
            totalR += ARGB.red(colour);
            totalG += ARGB.green(colour);
            totalB += ARGB.blue(colour);
        }

        return count == 0 ? -1 : ARGB.color(totalR / count, totalG / count, totalB / count);
    }

    public void tick() {
        for (var holder : upgrades.values()) {
            if (holder.upgrade == null) continue;
            holder.upgrade.upgrade().update(holder, computer.getPeripheral(holder.side));
        }
    }

    public boolean onRightClick(ServerLevel level) {
        for (var holder : upgrades.values()) {
            if (holder.upgrade == null) continue;
            return holder.upgrade.upgrade().onRightClick(level, holder, computer.getPeripheral(holder.side));
        }

        return false;
    }

    private UpgradeAccess getUpgradeAccess(PocketSide side) {
        return Nullability.assertNonNull(upgrades.get(side));
    }

    @Override
    public @Nullable UpgradeData<IPocketUpgrade> getUpgrade(PocketSide side) {
        return getUpgradeAccess(side).getUpgrade();
    }

    @Override
    public void setUpgrade(PocketSide side, @Nullable UpgradeData<IPocketUpgrade> upgrade) {
        getUpgradeAccess(side).setUpgrade(upgrade);
    }

    public void setUpgrades(@Nullable UpgradeData<IPocketUpgrade> back, @Nullable UpgradeData<IPocketUpgrade> bottom) {
        getUpgradeAccess(PocketSide.BACK).setUpgradeDirect(back);
        getUpgradeAccess(PocketSide.BOTTOM).setUpgradeDirect(bottom);
    }

    private final class UpgradeAccess implements IPocketAccess {
        private final DataComponentType<UpgradeData<IPocketUpgrade>> component;
        private final ComputerSide side;

        private @Nullable UpgradeData<IPocketUpgrade> upgrade;
        private int lightColour = -1;

        private UpgradeAccess(DataComponentType<UpgradeData<IPocketUpgrade>> component, ComputerSide side) {
            this.component = component;
            this.side = side;
        }

        @Override
        public ServerLevel getLevel() {
            return PocketBrain.this.getLevel();
        }

        @Override
        public Vec3 getPosition() {
            return PocketBrain.this.getPosition();
        }

        @Override
        public @Nullable Entity getEntity() {
            return PocketBrain.this.getEntity();
        }

        @Override
        public boolean isActive() {
            return PocketBrain.this.isActive();
        }

        @Override
        public int getColour() {
            return PocketBrain.this.getColour();
        }

        @Override
        public void setColour(int colour) {
            PocketBrain.this.setColour(colour);
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
        public DataComponentPatch getUpgradeData() {
            var upgrade = this.upgrade;
            return upgrade == null ? DataComponentPatch.EMPTY : upgrade.data();
        }

        @Override
        public void setUpgradeData(DataComponentPatch data) {
            var stack = requireStack();

            var upgrade = this.upgrade;
            if (upgrade == null || upgrade.data().equals(data)) return;

            this.upgrade = UpgradeData.of(upgrade.holder(), data);
            stack.set(component, upgrade);
            holder.setChanged();
        }

        @Override
        public void invalidatePeripheral() {
            var peripheral = upgrade == null ? null : upgrade.upgrade().createPeripheral(this);
            computer.setPeripheral(side, peripheral);
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
            var stack = requireStack();

            if (!setUpgradeDirect(upgrade)) return;

            stack.set(component, upgrade);
            holder.setChanged();
        }

        /**
         * Set an upgrade without writing it back to the stack.
         *
         * @param upgrade The upgrade to set.
         * @return Whether the upgrade changed.
         */
        private boolean setUpgradeDirect(@Nullable UpgradeData<IPocketUpgrade> upgrade) {
            if (Objects.equals(this.upgrade, upgrade)) return false;

            this.upgrade = upgrade;
            lightColour = -1;
            invalidatePeripheral();
            return true;
        }
    }
}
