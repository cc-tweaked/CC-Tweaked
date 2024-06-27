// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data.client;

import com.mojang.serialization.JsonOps;
import dan200.computercraft.client.model.ExtraModels;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * A data provider to generate {@link ExtraModels}.
 */
abstract class ExtraModelsProvider implements DataProvider {
    private final Path path;
    private final CompletableFuture<HolderLookup.Provider> registries;

    ExtraModelsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        path = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK).resolve(ExtraModels.PATH.getNamespace()).resolve(ExtraModels.PATH.getPath());
        this.registries = registries;
    }

    /**
     * Return a stream of models to load.
     *
     * @param registries The current registries.
     * @return The collection of extra models to load.
     */
    public abstract Stream<ResourceLocation> getModels(HolderLookup.Provider registries);

    @Override
    public final CompletableFuture<?> run(CachedOutput output) {
        return registries.thenCompose(registries -> {
            var models = new ExtraModels(getModels(registries).sorted().toList());
            var json = ExtraModels.CODEC.encodeStart(JsonOps.INSTANCE, models).getOrThrow(IllegalStateException::new);
            return DataProvider.saveStable(output, json, path);
        });
    }

    @Override
    public final String getName() {
        return "Extra Models";
    }
}
