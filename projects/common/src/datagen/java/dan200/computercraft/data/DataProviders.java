// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.mojang.serialization.Codec;
import dan200.computercraft.client.gui.GuiSprites;
import dan200.computercraft.client.model.LecternPocketModel;
import dan200.computercraft.client.model.LecternPrintoutModel;
import dan200.computercraft.shared.turtle.inventory.UpgradeSlot;
import net.minecraft.client.renderer.texture.atlas.SpriteSource;
import net.minecraft.client.renderer.texture.atlas.SpriteSources;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

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
    }
}
