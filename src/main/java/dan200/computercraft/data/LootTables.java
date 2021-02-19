/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.proxy.ComputerCraftProxyCommon;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.Alternative;
import net.minecraft.world.storage.loot.conditions.SurvivesExplosion;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.BiConsumer;

public class LootTables extends LootTableProvider
{
    public LootTables( DataGenerator generator )
    {
        super( generator );
    }

    @Override
    protected void registerLoot( BiConsumer<ResourceLocation, LootTable> add )
    {
        basicDrop( add, Registry.ModBlocks.DISK_DRIVE );
        basicDrop( add, Registry.ModBlocks.MONITOR_NORMAL );
        basicDrop( add, Registry.ModBlocks.MONITOR_ADVANCED );
        basicDrop( add, Registry.ModBlocks.PRINTER );
        basicDrop( add, Registry.ModBlocks.SPEAKER );
        basicDrop( add, Registry.ModBlocks.WIRED_MODEM_FULL );
        basicDrop( add, Registry.ModBlocks.WIRELESS_MODEM_NORMAL );
        basicDrop( add, Registry.ModBlocks.WIRELESS_MODEM_ADVANCED );

        computerDrop( add, Registry.ModBlocks.COMPUTER_NORMAL );
        computerDrop( add, Registry.ModBlocks.COMPUTER_ADVANCED );
        computerDrop( add, Registry.ModBlocks.COMPUTER_COMMAND );
        computerDrop( add, Registry.ModBlocks.TURTLE_NORMAL );
        computerDrop( add, Registry.ModBlocks.TURTLE_ADVANCED );

        add.accept( ComputerCraftProxyCommon.ForgeHandlers.LOOT_TREASURE_DISK, LootTable
            .lootTable()
            .setParamSet( LootParameterSets.ALL_PARAMS )
            .build() );
    }

    private static <T extends Block> void basicDrop( BiConsumer<ResourceLocation, LootTable> add, RegistryObject<T> wrapper )
    {
        Block block = wrapper.get();
        add.accept( block.getLootTable(), LootTable
            .lootTable()
            .setParamSet( LootParameterSets.BLOCK )
            .withPool( LootPool.lootPool()
                .name( "main" )
                .setRolls( ConstantRange.exactly( 1 ) )
                .add( ItemLootEntry.lootTableItem( block ) )
                .when( SurvivesExplosion.survivesExplosion() )
            ).build() );
    }

    private static <T extends Block> void computerDrop( BiConsumer<ResourceLocation, LootTable> add, RegistryObject<T> wrapper )
    {
        Block block = wrapper.get();
        add.accept( block.getLootTable(), LootTable
            .lootTable()
            .setParamSet( LootParameterSets.BLOCK )
            .withPool( LootPool.lootPool()
                .name( "main" )
                .setRolls( ConstantRange.exactly( 1 ) )
                .add( DynamicLootEntry.dynamicEntry( new ResourceLocation( ComputerCraft.MOD_ID, "computer" ) ) )
                .when( Alternative.alternative(
                    BlockNamedEntityLootCondition.builder(),
                    HasComputerIdLootCondition.builder(),
                    PlayerCreativeLootCondition.builder().invert()
                ) )
            ).build() );
    }
}
