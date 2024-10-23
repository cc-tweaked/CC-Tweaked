// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.export;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.data.PrettyJsonWriter;
import dan200.computercraft.gametest.core.TestHooks;
import dan200.computercraft.shared.util.RegistryHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.ShapedCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Provides a {@literal /ccexport <path>} command which exports icons and recipes for all ComputerCraft items.
 */
public class Exporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static <S> void register(CommandDispatcher<S> dispatcher) {
        dispatcher.register(
            LiteralArgumentBuilder.<S>literal("ccexport")
                .then(RequiredArgumentBuilder.<S, String>argument("path", StringArgumentType.string())
                    .executes(c -> {
                        run(c.getArgument("path", String.class));
                        return 0;
                    })));
    }

    private static void run(String path) {
        var output = new File(path).getAbsoluteFile().toPath();
        if (!Files.isDirectory(output)) {
            Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Output path does not exist"));
            return;
        }

        RenderSystem.assertOnRenderThread();
        try (var renderer = new ImageRenderer()) {
            export(output, renderer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Minecraft.getInstance().gui.getChat().addMessage(Component.literal("Export finished!"));
    }

    private static void export(Path root, ImageRenderer renderer) throws IOException {
        var dump = new JsonDump();

        Set<Item> items = new HashSet<>();

        // First find all CC items
        for (var item : BuiltInRegistries.ITEM) {
            if (RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, item).getNamespace().equals(ComputerCraftAPI.MOD_ID)) {
                items.add(item);
            }
        }

        // Now find all CC recipes.
        var server = Objects.requireNonNull(Minecraft.getInstance().getSingleplayerServer());
        for (var recipe : server.getRecipeManager().getRecipes()) {
            if (recipe.value().getType() != RecipeType.CRAFTING) continue;
            if (!recipe.id().location().getNamespace().equals(ComputerCraftAPI.MOD_ID)) continue;

            var displayInfos = recipe.value().display();
            if (displayInfos.isEmpty()) continue;
            var displayInfo = displayInfos.getFirst();

            var result = ((SlotDisplay.ItemStackSlotDisplay) displayInfo.result()).stack();
            if (!result.getComponentsPatch().isEmpty()) {
                TestHooks.LOG.warn("Skipping recipe {} as it has NBT", recipe.id());
                continue;
            }

            if (displayInfo instanceof ShapedCraftingRecipeDisplay shaped) {
                var converted = new JsonDump.Recipe(result);

                for (var x = 0; x < shaped.width(); x++) {
                    for (var y = 0; y < shaped.height(); y++) {
                        var ingredient = shaped.ingredients().get(x + y * shaped.width());
                        converted.setInput(x + y * 3, ingredient, items);
                    }
                }

                dump.recipes.put(recipe.id().toString(), converted);
            } else if (displayInfo instanceof ShapelessCraftingRecipeDisplay shapeless) {
                var converted = new JsonDump.Recipe(result);

                var ingredients = shapeless.ingredients();
                for (var i = 0; i < ingredients.size(); i++) {
                    converted.setInput(i, ingredients.get(i), items);
                }

                dump.recipes.put(recipe.id().toString(), converted);
            } else {
                TestHooks.LOG.info("Don't know how to handle recipe {}", recipe);
            }
        }

        var itemDir = root.resolve("items");
        if (Files.exists(itemDir)) MoreFiles.deleteRecursively(itemDir, RecursiveDeleteOption.ALLOW_INSECURE);

        renderer.setupState();
        var transform = new PoseStack();
        transform.setIdentity();

        for (var item : items) {
            var stack = new ItemStack(item);
            var location = RegistryHelper.getKeyOrThrow(BuiltInRegistries.ITEM, item);

            dump.itemNames.put(location.toString(), stack.getHoverName().getString());
            renderer.captureRender(itemDir.resolve(location.getNamespace()).resolve(location.getPath() + ".png"),
                () -> {
                    // TODO: Minecraft.getInstance().getItemRenderer().ren(transform, stack, 0, 0)
                }
            );
        }
        renderer.clearState();

        try (Writer writer = Files.newBufferedWriter(root.resolve("index.json")); var jsonWriter = new PrettyJsonWriter(writer)) {
            GSON.toJson(dump, JsonDump.class, jsonWriter);
        }
    }
}
