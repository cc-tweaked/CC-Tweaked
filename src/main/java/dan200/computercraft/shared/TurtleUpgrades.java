/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.util.InventoryUtil;

import net.minecraft.item.ItemStack;

public final class TurtleUpgrades {
    private static final Map<String, ITurtleUpgrade> upgrades = new HashMap<>();
    private static final IdentityHashMap<ITurtleUpgrade, Wrapper> wrappers = new IdentityHashMap<>();
    private static ITurtleUpgrade[] vanilla;
    private static boolean needsRebuild;
    private TurtleUpgrades() {}

    public static void register(@Nonnull ITurtleUpgrade upgrade) {
        Objects.requireNonNull(upgrade, "upgrade cannot be null");
        rebuild();

        Wrapper wrapper = new Wrapper(upgrade);
        String id = wrapper.id;
        ITurtleUpgrade existing = upgrades.get(id);
        if (existing != null) {
            throw new IllegalStateException("Error registering '" + upgrade.getUnlocalisedAdjective() + " Turtle'. Upgrade ID '" + id + "' is already " +
                                            "registered by '" + existing.getUnlocalisedAdjective() + " Turtle'");
        }

        upgrades.put(id, upgrade);
        wrappers.put(upgrade, wrapper);
    }

    /**
     * Rebuild the cache of turtle upgrades. This is done before querying the cache or registering new upgrades.
     */
    private static void rebuild() {
        if (!needsRebuild) {
            return;
        }

        upgrades.clear();
        for (Wrapper wrapper : wrappers.values()) {
            if (!wrapper.enabled) {
                continue;
            }

            ITurtleUpgrade existing = upgrades.get(wrapper.id);
            if (existing != null) {
                ComputerCraft.log.error("Error registering '" + wrapper.upgrade.getUnlocalisedAdjective() + " Turtle'." + " Upgrade ID '" + wrapper.id +
                                        "' is already registered by '" + existing.getUnlocalisedAdjective() + " Turtle'");
                continue;
            }

            upgrades.put(wrapper.id, wrapper.upgrade);
        }

        needsRebuild = false;
    }

    @Nullable
    public static ITurtleUpgrade get(String id) {
        rebuild();
        return upgrades.get(id);
    }

    @Nullable
    public static String getOwner(@Nonnull ITurtleUpgrade upgrade) {
        Wrapper wrapper = wrappers.get(upgrade);
        return wrapper != null ? wrapper.modId : null;
    }

    public static ITurtleUpgrade get(@Nonnull ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }

        for (Wrapper wrapper : wrappers.values()) {
            if (!wrapper.enabled) {
                continue;
            }

            ItemStack craftingStack = wrapper.upgrade.getCraftingItem();
            if (!craftingStack.isEmpty() && InventoryUtil.areItemsSimilar(stack, craftingStack)) {
                return wrapper.upgrade;
            }
        }

        return null;
    }

    public static Stream<ITurtleUpgrade> getVanillaUpgrades() {
        if (vanilla == null) {
            vanilla = new ITurtleUpgrade[] {
                // ComputerCraft upgrades
                ComputerCraft.TurtleUpgrades.wirelessModemNormal,
                ComputerCraft.TurtleUpgrades.wirelessModemAdvanced,
                ComputerCraft.TurtleUpgrades.speaker,

                // Vanilla Minecraft upgrades
                ComputerCraft.TurtleUpgrades.diamondPickaxe,
                ComputerCraft.TurtleUpgrades.diamondAxe,
                ComputerCraft.TurtleUpgrades.diamondSword,
                ComputerCraft.TurtleUpgrades.diamondShovel,
                ComputerCraft.TurtleUpgrades.diamondHoe,
                ComputerCraft.TurtleUpgrades.craftingTable,
                };
        }

        return Arrays.stream(vanilla)
                     .filter(x -> x != null && wrappers.get(x).enabled);
    }

    public static Stream<ITurtleUpgrade> getUpgrades() {
        return wrappers.values()
                       .stream()
                       .filter(x -> x.enabled)
                       .map(x -> x.upgrade);
    }

    public static boolean suitableForFamily(ComputerFamily family, ITurtleUpgrade upgrade) {
        return true;
    }

    public static void enable(ITurtleUpgrade upgrade) {
        Wrapper wrapper = wrappers.get(upgrade);
        if (wrapper.enabled) {
            return;
        }

        wrapper.enabled = true;
        needsRebuild = true;
    }

    public static void disable(ITurtleUpgrade upgrade) {
        Wrapper wrapper = wrappers.get(upgrade);
        if (!wrapper.enabled) {
            return;
        }

        wrapper.enabled = false;
        upgrades.remove(wrapper.id);
    }

    public static void remove(ITurtleUpgrade upgrade) {
        wrappers.remove(upgrade);
        needsRebuild = true;
    }

    private static class Wrapper {
        final ITurtleUpgrade upgrade;
        final String id;
        final String modId;
        boolean enabled;

        Wrapper(ITurtleUpgrade upgrade) {
            this.upgrade = upgrade;
            this.id = upgrade.getUpgradeID()
                             .toString();
            // TODO This should be the mod id of the mod the peripheral comes from
            this.modId = ComputerCraft.MOD_ID;
            this.enabled = true;
        }
    }
}
