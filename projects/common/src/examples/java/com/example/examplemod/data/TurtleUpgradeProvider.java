package com.example.examplemod.data;

import com.example.examplemod.ExampleMod;
import com.example.examplemod.ExampleTurtleUpgrade;
import dan200.computercraft.api.client.turtle.ItemUpgradeModel;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.registries.RegistriesDatapackGenerator;
import net.minecraft.data.registries.RegistryPatchGenerator;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Extends the bootstrap registries with our {@linkplain ExampleTurtleUpgrade example turtle upgrade}.
 */
// @start region=body
public class TurtleUpgradeProvider {
    // Define our upgrade ids.
    private static final Identifier EXAMPLE_TURTLE_UPGRADE = Identifier.fromNamespaceAndPath(ExampleMod.MOD_ID, "example_turtle_upgrade");

    // Define a RegistrySetBuilder containing our new upgrades. In a real mod, this may contain other dynamic
    // registries, such as structures or enchantments.
    private static final RegistrySetBuilder WORLD_REGISTRY_BUILDER = new RegistrySetBuilder()
        .add(ITurtleUpgrade.REGISTRY, TurtleUpgradeProvider::addUpgrades);

    // Register our turtle upgrades to the RegistrySetBuilder.
    private static void addUpgrades(BootstrapContext<ITurtleUpgrade> upgrades) {
        upgrades.register(ResourceKey.create(ITurtleUpgrade.REGISTRY, EXAMPLE_TURTLE_UPGRADE), new ExampleTurtleUpgrade(new ItemStackTemplate(Items.COMPASS)));
    }

    // Finally use our WORLD_REGISTRY_BUILDER to build a set of registries with the upgrades, and then use
    // RegistriesDatapackGenerator to write them to disk.
    public static void addTurtleUpgrades(DataGenerator.PackGenerator pack, CompletableFuture<HolderLookup.Provider> registries) {
        var registryPatch = RegistryPatchGenerator.createWorldLookup(registries, WORLD_REGISTRY_BUILDER);
        var patchedRegistries = registryPatch.thenApply(RegistrySetBuilder.PatchedRegistries::patches);
        pack.addProvider(o -> RegistriesDatapackGenerator.forWorldLayer(o, patchedRegistries));
    }

    // Register our turtle models.
    public static void addUpgradeModels(BiConsumer<Identifier, TurtleUpgradeModel.Unbaked> models) {
        models.accept(EXAMPLE_TURTLE_UPGRADE, ItemUpgradeModel.unbaked());
    }
}
// @end region=body
