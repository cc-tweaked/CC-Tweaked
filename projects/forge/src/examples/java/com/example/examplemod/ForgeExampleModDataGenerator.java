package com.example.examplemod;

import com.example.examplemod.data.TurtleUpgradeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Data generators for the Forge version of our example mod.
 */
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class ForgeExampleModDataGenerator {
    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        var pack = event.getGenerator().getVanillaPack(true);
        addTurtleUpgrades(pack, event.getLookupProvider());
    }

    // @start region=turtle_upgrades
    private static void addTurtleUpgrades(DataGenerator.PackGenerator pack, CompletableFuture<HolderLookup.Provider> registries) {
        var fullRegistryPatch = TurtleUpgradeProvider.makeUpgradeRegistry(registries);
        pack.addProvider(o -> new DatapackBuiltinEntriesProvider(o, fullRegistryPatch, Set.of(ExampleMod.MOD_ID)));
    }
    // @end region=turtle_upgrades
}
