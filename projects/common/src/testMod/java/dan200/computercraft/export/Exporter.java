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
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

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
        for (var item : RegistryWrappers.ITEMS) {
            if (RegistryWrappers.ITEMS.getKey(item).getNamespace().equals(ComputerCraftAPI.MOD_ID)) items.add(item);
        }

        // Now find all CC recipes.
        var level = Objects.requireNonNull(Minecraft.getInstance().level);
        for (var recipe : level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)) {
            var result = recipe.getResultItem(level.registryAccess());
            if (!RegistryWrappers.ITEMS.getKey(result.getItem()).getNamespace().equals(ComputerCraftAPI.MOD_ID)) {
                continue;
            }
            if (result.hasTag()) {
                TestHooks.LOG.warn("Skipping recipe {} as it has NBT", recipe.getId());
                continue;
            }

            if (recipe instanceof ShapedRecipe shaped) {
                var converted = new JsonDump.Recipe(result);

                for (var x = 0; x < shaped.getWidth(); x++) {
                    for (var y = 0; y < shaped.getHeight(); y++) {
                        var ingredient = shaped.getIngredients().get(x + y * shaped.getWidth());
                        if (ingredient.isEmpty()) continue;

                        converted.setInput(x + y * 3, ingredient, items);
                    }
                }

                dump.recipes.put(recipe.getId().toString(), converted);
            } else if (recipe instanceof ShapelessRecipe shapeless) {
                var converted = new JsonDump.Recipe(result);

                var ingredients = shapeless.getIngredients();
                for (var i = 0; i < ingredients.size(); i++) {
                    converted.setInput(i, ingredients.get(i), items);
                }

                dump.recipes.put(recipe.getId().toString(), converted);
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
            var location = RegistryWrappers.ITEMS.getKey(item);

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
