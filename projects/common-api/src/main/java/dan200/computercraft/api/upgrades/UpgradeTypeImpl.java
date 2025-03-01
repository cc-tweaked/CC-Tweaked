// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.upgrades;

import com.mojang.serialization.MapCodec;

/**
 * Simple implementation of {@link UpgradeType}.
 *
 * @param codec The codec to read/write upgrades with.
 * @param <T>   The upgrade subclass that this upgrade type represents.
 */
record UpgradeTypeImpl<T extends UpgradeBase>(MapCodec<T> codec) implements UpgradeType<T> {
}
