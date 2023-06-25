package dan200.computercraft.api.upgrades;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public record UpgradeData<T extends UpgradeBase>(T upgrade, CompoundTag data) {
    /** Utility function, that provide ability to build default version of upgrade data
     * based only on upgrade object.
     * @param upgrade any upgrade
     * @param <T> Upgrade class
     * @return default instance of upgrade data
     */
    public static <T extends UpgradeBase> UpgradeData<T> wrap(T upgrade) {
        return new UpgradeData<>(upgrade, upgrade.getUpgradeData(upgrade.getCraftingItem()));
    }

    /** Transform UpgradeData to it persistent variant that should be stored, when turtle or pocket items stop functioning.
     * @see UpgradeBase#getPersistedData(CompoundTag)
     * @param upgrade UpgradeData that should be persisted or null
     * @return UpgradeData instance with only persistent upgrade information or null
     */
    public static <T extends UpgradeBase> @Nullable UpgradeData<T> persist(@Nullable UpgradeData<T> upgrade) {
        if (upgrade == null) {
            return null;
        }
        return new UpgradeData<>(
            upgrade.upgrade, upgrade.upgrade.getPersistedData(upgrade.data)
        );
    }

    /** Utility functions, that allows to compare two upgrade data and check if they have same upgrades.
     *
     * @param first first upgrade data
     * @param second second upgrade data
     * @param <T> UpgradeBase class
     * @return true if upgrades data have same upgrades or both null
     */
    public static <T extends UpgradeBase> boolean isSame(@Nullable UpgradeData<T> first, @Nullable UpgradeData<T> second) {
        if (first == null) {
            return second == null;
        }
        if (second == null) {
            return false;
        }
        return first.upgrade == second.upgrade;
    }

    /** Build ItemStack from upgrade and data pair.
     * @return ItemStack that correspond current upgrade + data pair
     */
    public ItemStack getUpgradeItem() {
        return upgrade.getUpgradeItem(data);
    }

    /**
     * Proxy method to upgrade.
     * @return default crafting item for upgrade
     */
    public ItemStack getCraftingItem() {
        return upgrade.getCraftingItem();
    }

    /**
     * Proxy method to upgrade.
     * @return The localisation key for this upgrade's adjective.
     */
    public String getUnlocalisedAdjective() {
        return upgrade.getUnlocalisedAdjective();
    }

    /** Wrapper that just pass upgrade ID.
     * @return upgrade id
     */
    public ResourceLocation getUpgradeID() {
        return upgrade.getUpgradeID();
    }
}
