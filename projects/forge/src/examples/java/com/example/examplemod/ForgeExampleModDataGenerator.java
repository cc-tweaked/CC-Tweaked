package com.example.examplemod;

import com.example.examplemod.data.TurtleUpgradeProvider;
import dan200.computercraft.api.client.turtle.TurtleUpgradeModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.JsonCodecProvider;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import java.util.concurrent.CompletableFuture;

/**
 * Data generators for the Forge version of our example mod.
 */
@EventBusSubscriber
public class ForgeExampleModDataGenerator {
    @SubscribeEvent
    public static void gather(GatherDataEvent.Client event) {
        var pack = event.getGenerator().getVanillaPack(true);
        addTurtleUpgrades(pack, event.getWorldLookupProvider());
    }

    // @start region=turtle_upgrades
    private static void addTurtleUpgrades(DataGenerator.PackGenerator pack, CompletableFuture<HolderLookup.Provider> registries) {
        TurtleUpgradeProvider.addTurtleUpgrades(pack, registries);
        pack.addProvider(o -> new JsonCodecProvider<>(o, PackOutput.Target.RESOURCE_PACK, TurtleUpgradeModel.SOURCE, TurtleUpgradeModel.CODEC, registries, ExampleMod.MOD_ID) {
            @Override
            protected void gather() {
                TurtleUpgradeProvider.addUpgradeModels(this::unconditional);
            }
        });
    }
    // @end region=turtle_upgrades
}
