// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.data.models.ItemModelGenerators;
import net.minecraft.data.models.model.ModelTemplate;
import net.minecraft.data.models.model.ModelTemplates;
import net.minecraft.data.models.model.TextureMapping;
import net.minecraft.data.models.model.TextureSlot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.Optional;

import static net.minecraft.data.models.model.ModelLocationUtils.getModelLocation;

public final class ItemModelProvider {
    private ItemModelProvider() {
    }

    public static void addItemModels(ItemModelGenerators generators) {
        registerDisk(generators, ModRegistry.Items.DISK.get());
        registerDisk(generators, ModRegistry.Items.TREASURE_DISK.get());

        registerPocketComputer(generators, getModelLocation(ModRegistry.Items.POCKET_COMPUTER_NORMAL.get()), false);
        registerPocketComputer(generators, getModelLocation(ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get()), false);
        registerPocketComputer(generators, new ResourceLocation(ComputerCraftAPI.MOD_ID, "item/pocket_computer_colour"), true);

        generators.generateFlatItem(ModRegistry.Items.PRINTED_BOOK.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModRegistry.Items.PRINTED_PAGE.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModRegistry.Items.PRINTED_PAGES.get(), ModelTemplates.FLAT_ITEM);
    }

    private static void registerPocketComputer(ItemModelGenerators generators, ResourceLocation id, boolean off) {
        createFlatItem(generators, id.withSuffix("_blinking"),
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "item/pocket_computer_blink"),
            id,
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "item/pocket_computer_light")
        );

        createFlatItem(generators, id.withSuffix("_on"),
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "item/pocket_computer_on"),
            id,
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "item/pocket_computer_light")
        );

        // Don't emit the default/off state for advanced/normal pocket computers, as they have item overrides.
        if (off) {
            createFlatItem(generators, id,
                new ResourceLocation(ComputerCraftAPI.MOD_ID, "item/pocket_computer_frame"),
                id
            );
        }
    }

    private static void registerDisk(ItemModelGenerators generators, Item item) {
        createFlatItem(generators, item,
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "item/disk_frame"),
            new ResourceLocation(ComputerCraftAPI.MOD_ID, "item/disk_colour")
        );
    }

    private static void createFlatItem(ItemModelGenerators generators, Item item, ResourceLocation... ids) {
        createFlatItem(generators, getModelLocation(item), ids);
    }

    /**
     * Generate a flat item from an arbitrary number of layers.
     *
     * @param generators The current item generator helper.
     * @param model      The model we're writing to.
     * @param textures   The textures which make up this model.
     * @see net.minecraft.client.renderer.block.model.ItemModelGenerator The parser for this file format.
     */
    private static void createFlatItem(ItemModelGenerators generators, ResourceLocation model, ResourceLocation... textures) {
        if (textures.length > 5) throw new IndexOutOfBoundsException("Too many layers");
        if (textures.length == 0) throw new IndexOutOfBoundsException("Must have at least one texture");
        if (textures.length == 1) {
            ModelTemplates.FLAT_ITEM.create(model, TextureMapping.layer0(textures[0]), generators.output);
            return;
        }

        var slots = new TextureSlot[textures.length];
        var mapping = new TextureMapping();
        for (var i = 0; i < textures.length; i++) {
            var slot = slots[i] = TextureSlot.create("layer" + i);
            mapping.put(slot, textures[i]);
        }

        new ModelTemplate(Optional.of(new ResourceLocation("item/generated")), Optional.empty(), slots)
            .create(model, mapping, generators.output);
    }
}
