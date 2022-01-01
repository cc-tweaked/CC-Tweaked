/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.shared.Registry;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber( bus = Mod.EventBusSubscriber.Bus.MOD )
public class Generators
{
    @SubscribeEvent
    public static void gather( GatherDataEvent event )
    {
        Registry.registerLoot();

        DataGenerator generator = event.getGenerator();
        ExistingFileHelper existingFiles = event.getExistingFileHelper();

        var turtleUpgrades = new TurtleUpgradeGenerator( generator );
        var pocketUpgrades = new PocketUpgradeGenerator( generator );
        generator.addProvider( turtleUpgrades );
        generator.addProvider( pocketUpgrades );

        generator.addProvider( new RecipeGenerator( generator, turtleUpgrades, pocketUpgrades ) );
        generator.addProvider( new LootTableGenerator( generator ) );
        generator.addProvider( new BlockModelProvider( generator, existingFiles ) );

        BlockTagsGenerator blockTags = new BlockTagsGenerator( generator, existingFiles );
        generator.addProvider( blockTags );
        generator.addProvider( new ItemTagsGenerator( generator, blockTags, existingFiles ) );
    }
}
