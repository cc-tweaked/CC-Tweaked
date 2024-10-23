// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.model.turtle.UnbakedTurtleModel;
import net.fabricmc.fabric.api.client.model.loading.v1.PreparableModelLoadingPlugin;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.GsonHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Provides custom model loading for various CC models.
 * <p>
 * This is used from a {@link PreparableModelLoadingPlugin}, which {@linkplain #prepare(ResourceManager, Executor) loads
 * data from disk} in parallel with other loader plugins, and then hooks into the model loading pipeline
 * ({@link #loadModel(ResourceLocation)}).
 *
 * @see UnbakedTurtleModel
 */
public final class CustomModelLoader {
    private static final Logger LOG = LoggerFactory.getLogger(CustomModelLoader.class);
    private static final FileToIdConverter converter = FileToIdConverter.json("models");

    private final Map<ResourceLocation, UnbakedModel> models = new HashMap<>();
    private final Collection<ResourceLocation> extraModels;

    private CustomModelLoader(Collection<ResourceLocation> extraModels) {
        this.extraModels = extraModels;
    }

    public static CompletableFuture<CustomModelLoader> prepare(ResourceManager resources, Executor executor) {
        return CompletableFuture.supplyAsync(() -> {
            var loader = new CustomModelLoader(ExtraModels.loadAll(resources));
            for (var resource : resources.listResources("models", x -> x.getNamespace().equals(ComputerCraftAPI.MOD_ID) && x.getPath().endsWith(".json")).entrySet()) {
                loader.loadModel(resource.getKey(), resource.getValue());
            }
            return loader;
        }, executor);
    }

    private void loadModel(ResourceLocation path, Resource resource) {
        var id = converter.fileToId(path);

        try {
            JsonObject model;
            try (Reader reader = resource.openAsReader()) {
                model = GsonHelper.parse(reader).getAsJsonObject();
            }

            var loader = GsonHelper.getAsString(model, "loader", null);
            if (loader != null) {
                var unbaked = switch (loader) {
                    case ComputerCraftAPI.MOD_ID + ":turtle" -> UnbakedTurtleModel.parse(model);
                    default -> throw new JsonParseException("Unknown model loader " + loader);
                };
                models.put(id, unbaked);
            }
        } catch (IllegalArgumentException | IOException | JsonParseException e) {
            LOG.error("Couldn't parse model file {} from {}", id, path, e);
        }
    }

    public Collection<ResourceLocation> getExtraModels() {
        return extraModels;
    }

    /**
     * Load a custom model. This searches for CC models with a custom {@code loader} field.
     *
     * @param path The path of the model to load.
     * @return The unbaked model that has been loaded, or {@code null} if the model should be loaded as a vanilla model.
     */
    public @Nullable UnbakedModel loadModel(ResourceLocation path) {
        return path.getNamespace().equals(ComputerCraftAPI.MOD_ID) ? models.get(path) : null;
    }
}
