/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.data;

import dan200.computercraft.shared.proxy.ComputerCraftProxyCommon;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

@Mod.EventBusSubscriber( bus = Mod.EventBusSubscriber.Bus.MOD )
public class Generators
{
    @SubscribeEvent
    public static void gather( GatherDataEvent event )
    {
        ComputerCraftProxyCommon.registerLoot();

        DataGenerator generator = event.getGenerator();
        generator.addProvider( new Recipes( generator ) );
        generator.addProvider( new LootTables( generator ) );
        generator.addProvider( new Tags( generator ) );
        generator.addProvider( new BlockModelProvider( generator, event.getExistingFileHelper() ) );
    }
}
