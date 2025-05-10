// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.turtle;

import com.mojang.serialization.JsonOps;
import dan200.computercraft.client.ClientRegistry;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import dan200.computercraft.client.platform.ModelKey;
import dan200.computercraft.shared.util.ResourceUtils;
import net.minecraft.client.resources.model.MissingBlockModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

/**
 * A manager for loading {@link TurtleOverlay}s. This is responsible for {@linkplain #load(ResourceManager, Executor)
 * loading overlays from resource packs}, {@linkplain #register(ClientRegistry.RegisterExtraModels, Map) baking them}, and
 * then {@linkplain #getOverlay(ModelManager, ResourceLocation) looking them up}.
 */
public class TurtleOverlayManager {
    private static final FileToIdConverter ID_CONVERTER = FileToIdConverter.json(TurtleOverlay.SOURCE);

    /**
     * The {@link ModelKey} for the missing turtle overlay. This is used by
     * {@link #getOverlay(ModelManager, ResourceLocation)} when an overlay does not exist.
     */
    private static final ModelKey<TurtleOverlay> MISSING_KEY = ClientPlatformHelper.get().createModelKey(
        MissingBlockModel.LOCATION, () -> "Missing turtle overlay"
    );

    private static final Map<ResourceLocation, ModelKey<TurtleOverlay>> modelKeys = new ConcurrentHashMap<>();

    private static ModelKey<TurtleOverlay> getModelKey(ResourceLocation overlay) {
        return modelKeys.computeIfAbsent(overlay, o -> ClientPlatformHelper.get().createModelKey(o, () -> "Turtle overlay " + o));
    }

    /**
     * Load our overlays from resources.
     *
     * @param resources The current resource manager.
     * @param executor  The executor to schedule work on.
     * @return The map of unbaked overlay.
     */
    public static CompletableFuture<Map<ResourceLocation, TurtleOverlay.Unbaked>> load(ResourceManager resources, Executor executor) {
        return ResourceUtils.load(resources, executor, "turtle overlay", ID_CONVERTER, JsonOps.INSTANCE, TurtleOverlay.CODEC);
    }

    /**
     * Register our unbaked overlay models.
     *
     * @param register The callback to register models with.
     * @param overlays The overlays to register.
     */
    public static void register(ClientRegistry.RegisterExtraModels register, Map<ResourceLocation, TurtleOverlay.Unbaked> overlays) {
        overlays.forEach((id, overlay) -> register.register(getModelKey(id), overlay, TurtleOverlay.Unbaked::bake));
        register.register(MISSING_KEY, new TurtleOverlay.Unbaked(MissingBlockModel.LOCATION, false), TurtleOverlay.Unbaked::bake);
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
    public static @Nullable TurtleOverlay getOverlay(ModelManager modelManager, @Nullable ResourceLocation id) {
        if (id == null) return null;

        var overlay = getModelKey(id).get(modelManager);
        if (overlay != null) return overlay;

        var missing = MISSING_KEY.get(modelManager);
        if (missing == null) throw new IllegalStateException("Rendering turtles before models are baked");
        return missing;
    }
}
