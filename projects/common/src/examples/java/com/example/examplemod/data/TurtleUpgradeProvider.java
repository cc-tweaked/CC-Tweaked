package com.example.examplemod.data;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.ExampleTurtleUpgrade;
import dan200.computercraft.api.client.turtle.ItemUpgradeModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Extends the bootstrap registries with our {@linkplain ExampleTurtleUpgrade example turtle upgrade}.
 */
// @start region=body
public class TurtleUpgradeProvider {
    // Define our upgrade ids.
    private static final ResourceLocation EXAMPLE_TURTLE_UPGRADE = ResourceLocation.fromNamespaceAndPath(ExampleMod.MOD_ID, "example_turtle_upgrade");

    // Register our turtle upgrades.
    public static void addUpgrades(BootstrapContext<ITurtleUpgrade> upgrades) {
        upgrades.register(ITurtleUpgrade.createKey(EXAMPLE_TURTLE_UPGRADE), new ExampleTurtleUpgrade(new ItemStack(Items.COMPASS)));
    }

    // Set up the dynamic registries to contain our turtle upgrades.
    public static CompletableFuture<RegistrySetBuilder.PatchedRegistries> makeUpgradeRegistry(CompletableFuture<HolderLookup.Provider> registries) {
        return RegistryPatchGenerator.createLookup(registries, Util.make(new RegistrySetBuilder(), builder -> {
            builder.add(ITurtleUpgrade.REGISTRY, TurtleUpgradeProvider::addUpgrades);
        }));
    }

    // Register our turtle models.
    public static void addUpgradeModels(BiConsumer<ResourceLocation, TurtleUpgradeModel.Unbaked> models) {
        models.accept(EXAMPLE_TURTLE_UPGRADE, ItemUpgradeModel.unbaked());
    }
}
// @end region=body
