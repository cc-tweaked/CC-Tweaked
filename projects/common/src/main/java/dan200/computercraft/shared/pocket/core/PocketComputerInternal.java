// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.pocket.core;

import dan200.computercraft.api.pocket.IPocketUpgrade;
import dan200.computercraft.api.pocket.PocketComputer;
import dan200.computercraft.api.upgrades.UpgradeData;
import org.jspecify.annotations.Nullable;

/**
 * An internal version of {@link PocketComputer}.
 * <p>
 * This exposes additional functionality we don't want in the public API, but where we don't want access to the full
 * {@link PocketBrain} interface.
 */
public interface PocketComputerInternal extends PocketComputer {
    @Nullable
    UpgradeData<IPocketUpgrade> getUpgrade(PocketSide side);

    void setUpgrade(PocketSide side, @Nullable UpgradeData<IPocketUpgrade> upgrade);
}
