/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.platform.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.DataProvider;
import net.minecraft.data.models.BlockModelGenerators;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.blockstates.BlockStateGenerator;
import net.minecraft.data.models.model.DelegatedModel;
import net.minecraft.data.models.model.ModelLocationUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private final DataGenerator.PathProvider blockStatePath;
    private final DataGenerator.PathProvider modelPath;

    private final Consumer<BlockModelGenerators> blocks;
    private final Consumer<ItemModelGenerators> items;

    public ModelProvider(DataGenerator generator, Consumer<BlockModelGenerators> blocks, Consumer<ItemModelGenerators> items) {
        blockStatePath = generator.createPathProvider(DataGenerator.Target.RESOURCE_PACK, "blockstates");
        modelPath = generator.createPathProvider(DataGenerator.Target.RESOURCE_PACK, "models");

        this.blocks = blocks;
        this.items = items;
    }

    @Override
    public void run(@Nonnull CachedOutput output) {
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

        for (var block : Registries.BLOCKS) {
            if (!blockStates.containsKey(block)) continue;

            var item = Item.BY_BLOCK.get(block);
            if (item == null || explicitItems.contains(item)) continue;

            var model = ModelLocationUtils.getModelLocation(item);
            if (!models.containsKey(model)) {
                models.put(model, new DelegatedModel(ModelLocationUtils.getModelLocation(block)));
            }
        }

        saveCollection(output, blockStates, x -> blockStatePath.json(Registries.BLOCKS.getKey(x)));
        saveCollection(output, models, modelPath::json);
    }

    private <T> void saveCollection(CachedOutput output, Map<T, ? extends Supplier<JsonElement>> items, Function<T, Path> getLocation) {
        for (Map.Entry<T, ? extends Supplier<JsonElement>> entry : items.entrySet()) {
            var path = getLocation.apply(entry.getKey());
            try {
                DataProvider.saveStable(output, entry.getValue().get(), path);
            } catch (Exception exception) {
                ComputerCraft.log.error("Couldn't save {}", path, exception);
            }
        }
    }

    @Nonnull
    @Override
    public String getName() {
        return "Block State Definitions";
    }
}
