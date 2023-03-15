// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.google.gson.JsonElement;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.Util;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A copy of {@link net.minecraft.data.models.ModelProvider} which accepts a custom generator.
 * <p>
 * Please don't sue me Mojang. Or at least make these changes to vanilla before doing so!
 */
public class ModelProvider implements DataProvider {
    private final PackOutput.PathProvider blockStatePath;
    private final PackOutput.PathProvider modelPath;

    private final Consumer<BlockModelGenerators> blocks;
    private final Consumer<ItemModelGenerators> items;

    public ModelProvider(PackOutput output, Consumer<BlockModelGenerators> blocks, Consumer<ItemModelGenerators> items) {
        blockStatePath = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "blockstates");
        modelPath = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "models");

        this.blocks = blocks;
        this.items = items;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        Map<Block, BlockStateGenerator> blockStates = new HashMap<>();
        Consumer<BlockStateGenerator> addBlockState = generator -> {
            var block = generator.getBlock();
            if (blockStates.containsKey(block)) {
                throw new IllegalStateException("Duplicate blockstate definition for " + block);
            }
            blockStates.put(block, generator);
        };

        Map<ResourceLocation, Supplier<JsonElement>> models = new HashMap<>();
        BiConsumer<ResourceLocation, Supplier<JsonElement>> addModel = (id, contents) -> {
            if (models.containsKey(id)) throw new IllegalStateException("Duplicate model definition for " + id);
            models.put(id, contents);
        };
        Set<Item> explicitItems = new HashSet<>();
        blocks.accept(new BlockModelGenerators(addBlockState, addModel, explicitItems::add));
        items.accept(new ItemModelGenerators(addModel));

        for (var block : RegistryWrappers.BLOCKS) {
            if (!blockStates.containsKey(block)) continue;

            var item = Item.BY_BLOCK.get(block);
            if (item == null || explicitItems.contains(item)) continue;

            var model = ModelLocationUtils.getModelLocation(item);
            if (!models.containsKey(model)) {
                models.put(model, new DelegatedModel(ModelLocationUtils.getModelLocation(block)));
            }
        }

        List<CompletableFuture<?>> futures = new ArrayList<>();
        saveCollection(output, futures, blockStates, x -> blockStatePath.json(RegistryWrappers.BLOCKS.getKey(x)));
        saveCollection(output, futures, models, modelPath::json);
        return Util.sequenceFailFast(futures);
    }

    private <T> void saveCollection(CachedOutput output, List<CompletableFuture<?>> futures, Map<T, ? extends Supplier<JsonElement>> items, Function<T, Path> getLocation) {
        for (Map.Entry<T, ? extends Supplier<JsonElement>> entry : items.entrySet()) {
            var path = getLocation.apply(entry.getKey());

            futures.add(DataProvider.saveStable(output, entry.getValue().get(), path));
        }
    }

    @Override
    public String getName() {
        return "Block State Definitions";
    }
}
