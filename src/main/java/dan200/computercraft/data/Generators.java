/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.shared.ModRegistry;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class Generators {
    @SubscribeEvent
    public static void gather(GatherDataEvent event) {
        ModRegistry.registerLoot();

        var generator = event.getGenerator();
        var existingFiles = event.getExistingFileHelper();

        var turtleUpgrades = new TurtleUpgradeGenerator(generator);
        var pocketUpgrades = new PocketUpgradeGenerator(generator);
        generator.addProvider(event.includeServer(), turtleUpgrades);
        generator.addProvider(event.includeServer(), pocketUpgrades);

        generator.addProvider(event.includeServer(), new RecipeGenerator(generator, turtleUpgrades, pocketUpgrades));
        generator.addProvider(event.includeServer(), new LootTableGenerator(generator));
        generator.addProvider(event.includeClient(), new ModelProvider(generator, BlockModelGenerator::addBlockModels, ItemModelGenerator::addItemModels));

        var blockTags = new BlockTagsGenerator(generator, existingFiles);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(), new ItemTagsGenerator(generator, blockTags, existingFiles));
    }
}
