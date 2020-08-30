/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
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
import net.minecraft.loot.*;
import net.minecraft.loot.condition.AlternativeLootCondition;
import net.minecraft.loot.condition.SurvivesExplosionLootCondition;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.loot.entry.DynamicEntry;
import net.minecraft.loot.entry.ItemEntry;
import net.minecraft.util.Identifier;
import net.minecraftforge.fml.RegistryObject;

import java.util.function.BiConsumer;

public class LootTables extends LootTableProvider
{
    public LootTables( DataGenerator generator )
    {
        super( generator );
    }

    @Override
    protected void registerLoot( BiConsumer<Identifier, LootTable> add )
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
        computerDrop( add, Registry.ModBlocks.TURTLE_NORMAL );
        computerDrop( add, Registry.ModBlocks.TURTLE_ADVANCED );

        add.accept( ComputerCraftProxyCommon.ForgeHandlers.LOOT_TREASURE_DISK, LootTable
            .builder()
            .type( LootContextTypes.GENERIC )
            .build() );
    }

    private static <T extends Block> void basicDrop( BiConsumer<Identifier, LootTable> add, RegistryObject<T> wrapper )
    {
        Block block = wrapper.get();
        add.accept( block.getLootTableId(), LootTable
            .builder()
            .type( LootContextTypes.BLOCK )
            .pool( LootPool.builder()
                .name( "main" )
                .rolls( ConstantLootTableRange.create( 1 ) )
                .with( ItemEntry.builder( block ) )
                .conditionally( SurvivesExplosionLootCondition.builder() )
            ).build() );
    }

    private static <T extends Block> void computerDrop( BiConsumer<Identifier, LootTable> add, RegistryObject<T> wrapper )
    {
        Block block = wrapper.get();
        add.accept( block.getLootTableId(), LootTable
            .builder()
            .type( LootContextTypes.BLOCK )
            .pool( LootPool.builder()
                .name( "main" )
                .rolls( ConstantLootTableRange.create( 1 ) )
                .with( DynamicEntry.builder( new Identifier( ComputerCraft.MOD_ID, "computer" ) ) )
                .conditionally( AlternativeLootCondition.builder(
                    BlockNamedEntityLootCondition.BUILDER,
                    HasComputerIdLootCondition.BUILDER,
                    PlayerCreativeLootCondition.BUILDER.invert()
                ) )
            ).build() );
    }
}
