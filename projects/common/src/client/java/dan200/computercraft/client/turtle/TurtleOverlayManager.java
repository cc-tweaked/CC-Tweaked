// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.turtle;

import dan200.computercraft.client.CustomModelManager;
import net.minecraft.client.resources.model.MissingBlockModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * The model manager for {@link TurtleOverlay}s.
 */
public class TurtleOverlayManager {
    private static final CustomModelManager<TurtleOverlay.Unbaked, TurtleOverlay> loader = new CustomModelManager<>(
        "turtle overlay", FileToIdConverter.json(TurtleOverlay.SOURCE), TurtleOverlay.CODEC,
        TurtleOverlay.Unbaked::bake,
        new TurtleOverlay.Unbaked(MissingBlockModel.LOCATION, false)
    );


    public static CustomModelManager<TurtleOverlay.Unbaked, TurtleOverlay> loader() {
        return loader;
    }

    /**
     * Find the turtle overlay with the given id. If the overlay does not exist, then the "missing model" overlay is
     * returned instead.
     *
     * @param modelManager The model manager.
     * @param id           The overlay id.
     * @return The turtle overlay.
     */
    @Contract("_, null -> null; _, !null -> !null")
    public static @Nullable TurtleOverlay get(ModelManager modelManager, @Nullable Identifier id) {
        return id == null ? null : loader.get(modelManager, id);
    }
}
