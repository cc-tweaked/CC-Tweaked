// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data.client;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.item.colour.PocketComputerLight;
import dan200.computercraft.client.item.properties.PocketComputerStateProperty;
import dan200.computercraft.core.util.Colour;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.core.ComputerState;
import net.minecraft.client.color.item.Dye;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.item.BlockModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Optional;

import static net.minecraft.client.data.models.model.ModelLocationUtils.getModelLocation;

public final class ItemModelProvider {
    private static final ResourceLocation POCKET_COMPUTER_COLOUR = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item/pocket_computer_colour");

    private ItemModelProvider() {
    }

    public static void addItemModels(ItemModelGenerators generators) {
        registerDisk(generators, ModRegistry.Items.DISK.get(), Colour.WHITE.getARGB());
        registerDisk(generators, ModRegistry.Items.TREASURE_DISK.get(), Colour.BLACK.getARGB());

        registerPocketComputer(generators, ModRegistry.Items.POCKET_COMPUTER_NORMAL.get());
        registerPocketComputer(generators, ModRegistry.Items.POCKET_COMPUTER_ADVANCED.get());
        registerPocketComputerModels(generators, POCKET_COMPUTER_COLOUR);

        generators.generateFlatItem(ModRegistry.Items.PRINTED_BOOK.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModRegistry.Items.PRINTED_PAGE.get(), ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ModRegistry.Items.PRINTED_PAGES.get(), ModelTemplates.FLAT_ITEM);
    }

    private static void registerPocketComputerModels(ItemModelGenerators generators, ResourceLocation id) {
        createFlatItem(generators, id.withSuffix("_blinking"),
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item/pocket_computer_blink"),
            id,
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item/pocket_computer_light")
        );

        createFlatItem(generators, id.withSuffix("_on"),
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item/pocket_computer_on"),
            id,
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item/pocket_computer_light")
        );

        createFlatItem(generators, id,
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item/pocket_computer_frame"),
            id
        );
    }

    private static void registerPocketComputer(ItemModelGenerators generators, Item item) {
        registerPocketComputerModels(generators, getModelLocation(item));

        generators.itemModelOutput.accept(item, ItemModelUtils.conditional(
            new HasComponent(DataComponents.DYED_COLOR, false),
            createPocketModel(POCKET_COMPUTER_COLOUR),
            createPocketModel(getModelLocation(item))
        ));
    }

    private static ItemModel.Unbaked createPocketModel(ResourceLocation id) {
        var tints = List.of(
            ItemModelUtils.constantTint(-1),
            new Dye(-1),
            new PocketComputerLight(Colour.BLACK.getARGB())
        );

        return ItemModelUtils.select(PocketComputerStateProperty.create(),
            ItemModelUtils.when(ComputerState.OFF, new BlockModelWrapper.Unbaked(id, tints)),
            ItemModelUtils.when(ComputerState.ON, new BlockModelWrapper.Unbaked(id.withSuffix("_on"), tints)),
            ItemModelUtils.when(ComputerState.BLINKING, new BlockModelWrapper.Unbaked(id.withSuffix("_blinking"), tints))
        );
    }

    private static void registerDisk(ItemModelGenerators generators, Item item, int colour) {
        var model = getModelLocation(item);
        createFlatItem(generators, model,
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item/disk_frame"),
            ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "item/disk_colour")
        );

        generators.itemModelOutput.accept(item, new BlockModelWrapper.Unbaked(model, List.of(
            ItemModelUtils.constantTint(-1),
            new Dye(colour)
        )));
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
            ModelTemplates.FLAT_ITEM.create(model, TextureMapping.layer0(textures[0]), generators.modelOutput);
            return;
        }

        var slots = new TextureSlot[textures.length];
        var mapping = new TextureMapping();
        for (var i = 0; i < textures.length; i++) {
            var slot = slots[i] = TextureSlot.create("layer" + i);
            mapping.put(slot, textures[i]);
        }

        new ModelTemplate(Optional.of(ResourceLocation.withDefaultNamespace("item/generated")), Optional.empty(), slots)
            .create(model, mapping, generators.modelOutput);
    }
}
