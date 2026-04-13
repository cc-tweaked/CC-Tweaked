// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.turtle;

import dan200.computercraft.api.client.turtle.BasicUpgradeModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.client.CustomModelManager;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.cuboid.MissingCuboidModel;
import net.minecraft.core.Holder;
import net.minecraft.resources.FileToIdConverter;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * The model manager for {@link TurtleUpgradeModel}s.
 */
public final class TurtleUpgradeModelManager {
    private static final CustomModelManager<TurtleUpgradeModel.Unbaked, TurtleUpgradeModel> loader = new CustomModelManager<>(
        "turtle upgrade", FileToIdConverter.json(TurtleUpgradeModel.SOURCE), TurtleUpgradeModel.CODEC,
        TurtleUpgradeModel.Unbaked::bake,
        BasicUpgradeModel.unbaked(MissingCuboidModel.LOCATION, MissingCuboidModel.LOCATION)
    );

    public static CustomModelManager<TurtleUpgradeModel.Unbaked, TurtleUpgradeModel> loader() {
        return loader;
    }

    /**
     * Find the model for the given turtle upgrade.
     *
     * @param modelManager The model manager.
     * @param upgrade      The turtle upgrade
     * @return The turtle upgrade model.
     */
    @Contract("_, null -> null; _, !null -> !null")
    public static @Nullable TurtleUpgradeModel get(ModelManager modelManager, Holder.@Nullable Reference<ITurtleUpgrade> upgrade) {
        return upgrade == null ? null : loader.get(modelManager, upgrade.key().identifier());
    }
}
