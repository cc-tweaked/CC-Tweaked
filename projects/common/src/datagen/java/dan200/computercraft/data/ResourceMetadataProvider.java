// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dan200.computercraft.client.gui.GuiSprites;
import net.minecraft.client.resources.metadata.gui.GuiMetadataSection;
import net.minecraft.client.resources.metadata.gui.GuiSpriteScaling;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.metadata.MetadataSectionType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Similar to {@link PackMetadataGenerator}, but for individual resources.
 */
final class ResourceMetadataProvider implements DataProvider {
    private final PackOutput output;

    ResourceMetadataProvider(PackOutput output) {
        this.output = output;
    }

    private void register(Builder builder) {
        for (var computerTextures : List.of(
            GuiSprites.COMPUTER_ADVANCED,
            GuiSprites.COMPUTER_COLOUR,
            GuiSprites.COMPUTER_COMMAND,
            GuiSprites.COMPUTER_NORMAL
        )) {
            builder.texture(computerTextures.border()).add(GuiMetadataSection.TYPE, new GuiMetadataSection(
                new GuiSpriteScaling.NineSlice(36, 36, simpleNineSlicedBorder(12))
            ));

            var sidebar = computerTextures.sidebar();
            if (sidebar != null) {
                builder.texture(sidebar).add(GuiMetadataSection.TYPE, new GuiMetadataSection(
                    new GuiSpriteScaling.NineSlice(17, 14, new GuiSpriteScaling.NineSlice.Border(3, 4, 0, 3))
                ));
            }

            var pocketBottom = computerTextures.pocketBottom();
            if (pocketBottom != null) {
                builder.texture(pocketBottom).add(GuiMetadataSection.TYPE, new GuiMetadataSection(
                    new GuiSpriteScaling.NineSlice(36, 20, new GuiSpriteScaling.NineSlice.Border(12, 0, 12, 0))
                ));
            }
        }
    }

    private static GuiSpriteScaling.NineSlice.Border simpleNineSlicedBorder(int size) {
        return new GuiSpriteScaling.NineSlice.Border(size, size, size, size);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cachedOutput) {
        var builder = new Builder();
        register(builder);

        var outputPath = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK);
        return CompletableFuture.allOf(builder.metadata.entrySet().stream().map(entry -> {
            var json = new JsonObject();
            entry.getValue().elements.forEach((name, element) -> json.add(name, element.get()));
            return DataProvider.saveStable(cachedOutput, json, outputPath.resolve(entry.getKey().getNamespace()).resolve(entry.getKey().getPath() + ".mcmeta"));
        }).toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Resource Metadata";
    }

    /**
     * A builder for a set of {@code mcmeta} files.
     */
    private static final class Builder {
        private final Map<ResourceLocation, FileMetadata> metadata = new HashMap<>();

        FileMetadata texture(ResourceLocation texture) {
            return file(texture.withPrefix("textures/").withSuffix(".png"));
        }

        FileMetadata file(ResourceLocation path) {
            return metadata.computeIfAbsent(path, p -> new FileMetadata());
        }
    }

    /**
     * A builder for a given file's {@code mcmeta} file.
     */
    private static final class FileMetadata {
        private final Map<String, Supplier<JsonElement>> elements = new HashMap<>();

        <T> FileMetadata add(MetadataSectionType<T> type, T value) {
            elements.put(type.getMetadataSectionName(), () -> type.toJson(value));
            return this;
        }
    }
}
