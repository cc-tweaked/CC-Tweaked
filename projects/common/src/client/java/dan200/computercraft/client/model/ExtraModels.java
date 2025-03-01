// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.turtle.TurtleOverlay;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * A list of extra models to load on the client.
 * <p>
 * This is largely intended for use with {@linkplain TurtleOverlay turtle overlays}. As overlays are stored in a dynamic
 * registry, they are not available when resources are loaded, and so we need a way to request the overlays' models be
 * loaded.
 *
 * @param models The models to load.
 */
public record ExtraModels(List<ResourceLocation> models) {
    private static final Logger LOG = LoggerFactory.getLogger(ExtraModels.class);
    private static final Gson GSON = new Gson();

    /**
     * The path where the extra models are listed.
     */
    public static final ResourceLocation PATH = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "extra_models.json");

    /**
     * The coded used to store the extra model file.
     */
    public static final Codec<ExtraModels> CODEC = ResourceLocation.CODEC.listOf().xmap(ExtraModels::new, ExtraModels::models);

    /**
     * Get the list of all extra models to load.
     *
     * @param resources The current resource manager.
     * @return A set of all resources to load.
     */
    public static Collection<ResourceLocation> loadAll(ResourceManager resources) {
        Set<ResourceLocation> out = new HashSet<>();

        for (var path : resources.getResourceStack(PATH)) {
            ExtraModels models;
            try (var stream = path.openAsReader()) {
                models = ExtraModels.CODEC.parse(JsonOps.INSTANCE, GSON.fromJson(stream, JsonElement.class)).getOrThrow(JsonParseException::new);
            } catch (IOException | RuntimeException e) {
                LOG.error("Failed to load extra models from {}", path.sourcePackId());
                continue;
            }

            out.addAll(models.models());
        }

        return Collections.unmodifiableCollection(out);
    }
}
