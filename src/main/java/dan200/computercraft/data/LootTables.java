/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.data;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.*;
import net.minecraft.world.storage.loot.conditions.Alternative;
import net.minecraft.world.storage.loot.conditions.SurvivesExplosion;

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
        basicDrop( add, ComputerCraft.Blocks.diskDrive );
        basicDrop( add, ComputerCraft.Blocks.monitorNormal );
        basicDrop( add, ComputerCraft.Blocks.monitorAdvanced );
        basicDrop( add, ComputerCraft.Blocks.printer );
        basicDrop( add, ComputerCraft.Blocks.speaker );
        basicDrop( add, ComputerCraft.Blocks.wiredModemFull );
        basicDrop( add, ComputerCraft.Blocks.wirelessModemNormal );
        basicDrop( add, ComputerCraft.Blocks.wirelessModemAdvanced );

        computerDrop( add, ComputerCraft.Blocks.computerNormal );
        computerDrop( add, ComputerCraft.Blocks.computerAdvanced );
        computerDrop( add, ComputerCraft.Blocks.turtleNormal );
        computerDrop( add, ComputerCraft.Blocks.turtleAdvanced );
    }

    private static void basicDrop( BiConsumer<ResourceLocation, LootTable> add, Block block )
    {
        add.accept( block.getRegistryName(), LootTable
            .builder()
            .addLootPool( LootPool.builder()
                .name( "main" )
                .rolls( ConstantRange.of( 1 ) )
                .addEntry( ItemLootEntry.builder( block ) )
                .acceptCondition( SurvivesExplosion.builder() )
            ).build() );
    }

    private static void computerDrop( BiConsumer<ResourceLocation, LootTable> add, Block block )
    {
        add.accept( block.getRegistryName(), LootTable
            .builder()
            .addLootPool( LootPool.builder()
                .name( "main" )
                .rolls( ConstantRange.of( 1 ) )
                .addEntry( DynamicLootEntry.func_216162_a( new ResourceLocation( ComputerCraft.MOD_ID, "computer" ) ) )
                .acceptCondition( Alternative.builder(
                    BlockNamedEntityLootCondition.builder(),
                    PlayerCreativeLootCondition.builder().inverted()
                ) )
            ).build() );
    }
}
