// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.pocket;

import dan200.computercraft.api.upgrades.UpgradeDataProvider;
import dan200.computercraft.api.upgrades.UpgradeType;
import dan200.computercraft.impl.ComputerCraftAPIService;
import dan200.computercraft.impl.RegistryHelper;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

import java.util.function.Consumer;

/**
 * A data provider to generate pocket computer upgrades.
 * <p>
 * This should be subclassed and registered to a {@link DataGenerator.PackGenerator}. Override the
 * {@link #addUpgrades(Consumer)} function, construct each upgrade, and pass them off to the provided consumer to
 * generate them.
 *
 * @see IPocketUpgrade
 * @see UpgradeType
 */
public abstract class PocketUpgradeDataProvider extends UpgradeDataProvider<IPocketUpgrade> {
    public PocketUpgradeDataProvider(PackOutput output) {
        super(output, "Pocket Computer Upgrades", RegistryHelper.POCKET_UPGRADE, ComputerCraftAPIService.get().pocketUpgradeCodec());
    }
}
