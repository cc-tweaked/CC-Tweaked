// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dan200.computercraft.client.platform.ClientPlatformHelper;
import dan200.computercraft.client.platform.ModelKey;
import dan200.computercraft.shared.util.ResourceUtils;
import net.minecraft.client.resources.model.MissingBlockModel;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.function.BiFunction;

/**
 * A manager for loading custom models. This is responsible for {@linkplain #load(ResourceManager, Executor) loading
 * models from resource packs}, {@linkplain #register(ClientRegistry.RegisterExtraModels, Map) registering them as
 * extra models}, and then {@linkplain #get(ModelManager, ResourceLocation) looking them up}.
 *
 * @param <U> The type of unbaked model.
 * @param <T> The type of baked model.
 */
public class CustomModelManager<U extends ResolvableModel, T> {
    private final String kind;
    private final FileToIdConverter lister;
    private final Codec<U> codec;
    private final BiFunction<U, ModelBaker, T> bake;

    private final ModelKey<T> missingModelKey;
    private final U missingModel;

    private final Map<ResourceLocation, ModelKey<T>> modelKeys = new ConcurrentHashMap<>();

    public CustomModelManager(String kind, FileToIdConverter lister, Codec<U> codec, BiFunction<U, ModelBaker, T> bake, U missingModel) {
        this.kind = kind;
        this.lister = lister;
        this.codec = codec;
        this.bake = bake;

        this.missingModelKey = ClientPlatformHelper.get().createModelKey(MissingBlockModel.LOCATION, () -> "Missing " + kind);
        this.missingModel = missingModel;
    }

    private ModelKey<T> getModelKey(ResourceLocation id) {
        return modelKeys.computeIfAbsent(id, o -> ClientPlatformHelper.get().createModelKey(o, () -> kind + " " + o));
    }

    /**
     * Load our models from resources.
     *
     * @param resources The current resource manager.
     * @param executor  The executor to schedule work on.
     * @return The map of unbaked models.
     */
    public CompletableFuture<Map<ResourceLocation, U>> load(ResourceManager resources, Executor executor) {
        return ResourceUtils.load(resources, executor, kind, lister, JsonOps.INSTANCE, codec);
    }

    /**
     * Register our unbaked models.
     *
     * @param register The callback to register models with.
     * @param models   The models to register.
     */
    public void register(ClientRegistry.RegisterExtraModels register, Map<ResourceLocation, U> models) {
        models.forEach((id, model) -> register.register(getModelKey(id), model, bake));
        register.register(missingModelKey, missingModel, bake);
    }

    /**
     * Find the model with the given id. If the model does not exist, then the missing model is returned instead.
     *
     * @param modelManager The model manager.
     * @param id           The model id.
     * @return The loaded model.
     */
    public T get(ModelManager modelManager, ResourceLocation id) {
        var model = getModelKey(id).get(modelManager);
        if (model != null) return model;

        var missing = missingModelKey.get(modelManager);
        if (missing == null) throw new IllegalStateException("Models have not yet been loaded.");
        return missing;
    }
}
