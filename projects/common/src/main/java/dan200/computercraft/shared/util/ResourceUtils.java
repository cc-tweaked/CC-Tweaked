// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import net.minecraft.Util;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

public class ResourceUtils {
    private static final Logger LOG = LoggerFactory.getLogger(ResourceUtils.class);

    /**
     * A version of {@link SimpleJsonResourceReloadListener#scanDirectory(ResourceManager, FileToIdConverter, DynamicOps, Codec, Map)},
     * that runs in parallel over multiple executors.
     * <p>
     * This shares some similarity with how {@link net.minecraft.client.resources.model.ModelManager#loadBlockModels(ResourceManager, Executor)}
     * loads resources.
     *
     * @param resourceManager The current resource manager.
     * @param executor        The executor to schedule work on.
     * @param kind            The name of the thing we're loading, for use in error messages.
     * @param lister          The file-to-id-converter used to locate files.
     * @param ops             The dynamic ops used when decoding.
     * @param codec           The codec used to decode each file.
     * @param <T>             The type of the entry to load
     * @return The loaded resources.
     */
    public static <T> CompletableFuture<Map<ResourceLocation, T>> load(
        ResourceManager resourceManager, Executor executor, String kind, FileToIdConverter lister, DynamicOps<JsonElement> ops, Codec<T> codec
    ) {
        return CompletableFuture.supplyAsync(() -> lister.listMatchingResources(resourceManager), executor).thenCompose(resources -> {
            List<CompletableFuture<@Nullable Pair<ResourceLocation, T>>> futures = new ArrayList<>(resources.size());
            resources.forEach((path, resource) -> futures.add(CompletableFuture.supplyAsync(() -> {
                var id = lister.fileToId(path);
                try (var reader = resource.openAsReader()) {
                    var result = codec.parse(ops, JsonParser.parseReader(reader))
                        .ifError(e -> LOG.error("Couldn't parse {} '{}' from pack '{}': {}", kind, id, resource.sourcePackId(), e.message()))
                        .result().orElse(null);
                    return result == null ? null : Pair.of(id, result);
                } catch (Exception exception) {
                    LOG.error("Failed to open {} {} from pack '{}'", kind, resource, resource.sourcePackId(), exception);
                    return null;
                }
            }, executor)));

            return Util.sequence(futures).thenApply(result -> result
                .stream().filter(Objects::nonNull).collect(Collectors.toUnmodifiableMap(Pair::getFirst, Pair::getSecond)));
        });
    }
}
