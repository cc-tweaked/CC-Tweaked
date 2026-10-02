// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.serialization.Codec;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.turtle.TurtleUpgradeDataProvider;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import dan200.computercraft.client.gui.GuiSprites;
import dan200.computercraft.client.model.LecternPocketModel;
import dan200.computercraft.client.model.LecternPrintoutModel;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.turtle.inventory.UpgradeSlot;
import dan200.computercraft.shared.turtle.upgrades.TurtleStorage;
import net.minecraft.DetectedVersion;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.metadata.PackMetadataGenerator;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ShulkerBoxBlock;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * All data providers for ComputerCraft. We require a mod-loader abstraction {@link GeneratorSink} (instead of
 * {@link PackOutput})to handle the slight differences between how Forge and Fabric expose Minecraft's data providers.
 */
public final class DataProviders {
    private DataProviders() {
    }

    public static void add(GeneratorSink generator) {
        generator.add(out -> new PackMetadataGenerator(out)
            .add(PackMetadataSection.TYPE, new PackMetadataSection(
                Component.literal("CC: Tweaked"),
                DetectedVersion.BUILT_IN.getPackVersion(PackType.CLIENT_RESOURCES)
            ))
        );

        var turtleUpgrades = generator.add(TurtleUpgradeProvider::new);
        var pocketUpgrades = generator.add(PocketUpgradeProvider::new);
        generator.add(out -> new RecipeProvider(out, turtleUpgrades, pocketUpgrades));

        var blockTags = generator.blockTags(TagProvider::blockTags);
        generator.itemTags(TagProvider::itemTags, blockTags);

        generator.add(out -> new net.minecraft.data.loot.LootTableProvider(out, Set.of(), LootTableProvider.getTables()));

        generator.add(out -> new ModelProvider(out, BlockModelProvider::addBlockModels, ItemModelProvider::addItemModels));

        generator.add(out -> new LanguageProvider(out, turtleUpgrades, pocketUpgrades));

        generator.addFromCodec("Block atlases", PackType.CLIENT_RESOURCES, "atlases", SpriteSources.FILE_CODEC, out -> {
            out.accept(new ResourceLocation("blocks"), makeSprites(Stream.of(
                UpgradeSlot.LEFT_UPGRADE,
                UpgradeSlot.RIGHT_UPGRADE,
                LecternPrintoutModel.TEXTURE,
                LecternPocketModel.TEXTURE_NORMAL, LecternPocketModel.TEXTURE_ADVANCED,
                LecternPocketModel.TEXTURE_COLOUR, LecternPocketModel.TEXTURE_FRAME, LecternPocketModel.TEXTURE_LIGHT
            )));
            out.accept(GuiSprites.SPRITE_SHEET, makeSprites(
                Stream.of(GuiSprites.TURTLE_NORMAL_SELECTED_SLOT, GuiSprites.TURTLE_ADVANCED_SELECTED_SLOT),
                // Buttons
                GuiSprites.TURNED_OFF.textures(),
                GuiSprites.TURNED_ON.textures(),
                GuiSprites.TERMINATE.textures(),
                // Computers
                GuiSprites.COMPUTER_NORMAL.textures(),
                GuiSprites.COMPUTER_ADVANCED.textures(),
                GuiSprites.COMPUTER_COMMAND.textures(),
                GuiSprites.COMPUTER_COLOUR.textures()
            ));
        });

        var shulkerPack = generator.nestedDatapack(TurtleStorage.DATA_PACK_NAME);
        shulkerPack.add(out -> PackMetadataGenerator.forFeaturePack(out, Component.translatable(TurtleStorage.DATA_PACK_TRANSLATION)));
        shulkerPack.add(out -> new TurtleUpgradeDataProvider(out) {
            @Override
            protected void addUpgrades(Consumer<Upgrade<TurtleUpgradeSerialiser<?>>> addUpgrade) {
                for (var item : BuiltInRegistries.ITEM) {
                    if (item instanceof BlockItem blockItem && blockItem.getBlock() instanceof ShulkerBoxBlock) {
                        var name = PlatformHelper.get().getRegistryKey(Registries.ITEM, item).getPath();
                        simpleWithCustomItem(new ResourceLocation(ComputerCraftAPI.MOD_ID, name), ModRegistry.TurtleSerialisers.STORAGE.get(), item).add(addUpgrade);
                    }
                }
            }
        });
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    private static List<SpriteSource> makeSprites(final Stream<ResourceLocation>... files) {
        return Arrays.stream(files).flatMap(Function.identity()).<SpriteSource>map(x -> new SingleFile(x, Optional.empty())).toList();
    }

    public interface GeneratorSink {
        <T extends DataProvider> T add(DataProvider.Factory<T> factory);

        <T> void addFromCodec(String name, PackType type, String directory, Codec<T> codec, Consumer<BiConsumer<ResourceLocation, T>> output);

        TagsProvider<Block> blockTags(Consumer<TagProvider.TagConsumer<Block>> tags);

        TagsProvider<Item> itemTags(Consumer<TagProvider.ItemTagConsumer> tags, TagsProvider<Block> blocks);

        GeneratorSink nestedDatapack(String name);
    }
}
