// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.upgrades;

import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeType;
import org.jetbrains.annotations.ApiStatus;

/**
 * Simple implementation of {@link UpgradeType}.
 *
 * @param codec The codec to read/write upgrades with.
 * @param <T>   The upgrade subclass that this upgrade type represents.
 */
@ApiStatus.Internal
public record UpgradeTypeImpl<T extends UpgradeBase>(MapCodec<T> codec) implements UpgradeType<T> {
}
