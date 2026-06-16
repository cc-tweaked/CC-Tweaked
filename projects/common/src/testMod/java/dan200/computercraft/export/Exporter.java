// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.export;

import com.google.common.io.MoreFiles;
import com.google.common.io.RecursiveDeleteOption;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.gametest.core.TestHooks;
import dan200.computercraft.shared.util.PrettyJsonWriter;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
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
import java.util.Objects;
import java.util.stream.Collectors;

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
            Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal("Output path does not exist"));
            return;
        }

        RenderSystem.assertOnRenderThread();
        try {
            export(output);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        Minecraft.getInstance().gui.hud.getChat().addClientSystemMessage(Component.literal("Export finished!"));
    }

    private static void export(Path root) throws IOException {
        var dump = new JsonDump();

        // First find all CC items
        var items = BuiltInRegistries.ITEM.stream()
            .filter(x -> BuiltInRegistries.ITEM.getKey(x).getNamespace().equals(ComputerCraftAPI.MOD_ID))
            .collect(Collectors.toSet());

        // Now find all CC recipes.
        var server = Objects.requireNonNull(Minecraft.getInstance().getSingleplayerServer());
        for (var recipe : server.getRecipeManager().getRecipes()) {
            if (recipe.value().getType() != RecipeType.CRAFTING) continue;
            if (!recipe.id().identifier().getNamespace().equals(ComputerCraftAPI.MOD_ID)) continue;

            var displayInfos = recipe.value().display();
            if (displayInfos.isEmpty()) continue;
            var displayInfo = displayInfos.getFirst();

            var result = ((SlotDisplay.ItemStackSlotDisplay) displayInfo.result()).stack();
            if (!result.components().isEmpty()) {
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

        try (Writer writer = Files.newBufferedWriter(root.resolve("index.json")); var jsonWriter = new PrettyJsonWriter(writer)) {
            GSON.toJson(dump, JsonDump.class, jsonWriter);
        }
    }
}
