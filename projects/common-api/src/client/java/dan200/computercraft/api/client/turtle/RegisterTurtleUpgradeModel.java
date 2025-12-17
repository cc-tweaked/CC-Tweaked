// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.api.client.turtle;

import com.mojang.serialization.MapCodec;
import net.minecraft.resources.Identifier;

/**
 * A functional interface to register a {@link TurtleUpgradeModel}.
 * <p>
 * This interface is largely intended to be used from multi-loader code, to allow sharing registration code between
 * multiple loaders.
 */
@FunctionalInterface
public interface RegisterTurtleUpgradeModel {
    /**
     * Register a {@link TurtleUpgradeModel}.
     *
     * @param id    The id used for this type of upgrade model.
     * @param model The codec used to read/decode an upgrade model.
     */
    void register(Identifier id, MapCodec<? extends TurtleUpgradeModel.Unbaked> model);
}
