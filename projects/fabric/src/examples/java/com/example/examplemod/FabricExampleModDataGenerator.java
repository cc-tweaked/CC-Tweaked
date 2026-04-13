package com.example.examplemod;

import com.example.examplemod.data.TurtleUpgradeProvider;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * Data generators for our Fabric example mod.
 */
public class FabricExampleModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        var pack = generator.createPack();
        addTurtleUpgrades(pack, generator.getRegistries());
    }

    // @start region=turtle_upgrades
    private static void addTurtleUpgrades(FabricDataGenerator.Pack pack, CompletableFuture<HolderLookup.Provider> registries) {
        var fullRegistryPatch = TurtleUpgradeProvider.makeUpgradeRegistry(registries);
        pack.addProvider((FabricPackOutput output) -> new AutomaticDynamicRegistryProvider(output, fullRegistryPatch));
        pack.addProvider((FabricPackOutput output) -> new FabricCodecDataProvider<>(output, registries, PackOutput.Target.RESOURCE_PACK, TurtleUpgradeModel.SOURCE, TurtleUpgradeModel.CODEC) {
            @Override
            public String getName() {
                return "Turtle upgrade models";
            }

            @Override
            protected void configure(BiConsumer<Identifier, TurtleUpgradeModel.Unbaked> out, HolderLookup.Provider provider) {
                TurtleUpgradeProvider.addUpgradeModels(out);
            }
        });
    }

    /**
     * A subclass of {@link FabricDynamicRegistryProvider} that writes all new entries.
     */
    private static class AutomaticDynamicRegistryProvider extends FabricDynamicRegistryProvider {
        AutomaticDynamicRegistryProvider(FabricPackOutput output, CompletableFuture<RegistrySetBuilder.PatchedRegistries> registries) {
            super(output, registries.thenApply(RegistrySetBuilder.PatchedRegistries::patches));
        }

        @Override
        protected void configure(HolderLookup.Provider registries, Entries entries) {
            for (var r : DynamicRegistries.getDynamicRegistries()) entries.addAll(registries.lookupOrThrow(r.key()));
        }

        @Override
        public String getName() {
            return "Registries";
        }
    }
    // @end region=turtle_upgrades
}
