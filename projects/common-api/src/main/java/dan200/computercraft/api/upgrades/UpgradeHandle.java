// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public interface UpgradeHandle<T extends UpgradeBase> {
    T getUpgrade();

    default UpgradeData<T> getUpgradeWithData() {
        return UpgradeData.of(getUpgrade(), getItem(), getData());
    }

    ItemStack getItem();

    void setItem(ItemStack item);

    CompoundTag getData();

    void setComponents(CompoundTag data);

    void setChanged();

    void clear();
}
