/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.data;

import com.mojang.datafixers.util.Pair;
import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.CommonHooks;
import dan200.computercraft.shared.Registry;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.peripheral.modem.wired.BlockCable;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import net.minecraft.advancements.criterion.StatePropertiesPredicate;
import net.minecraft.block.Block;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.LootTableProvider;
import net.minecraft.loot.*;
import net.minecraft.loot.conditions.Alternative;
import net.minecraft.loot.conditions.BlockStateProperty;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.loot.conditions.SurvivesExplosion;
import net.minecraft.loot.functions.CopyName;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class LootTableGenerator extends LootTableProvider
{
    public LootTableGenerator( DataGenerator generator )
    {
        super( generator );
    }

    @Nonnull
    @Override
    protected List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootParameterSet>> getTables()
    {
        return Arrays.asList(
            Pair.of( () -> LootTableGenerator::registerBlocks, LootParameterSets.BLOCK ),
            Pair.of( () -> LootTableGenerator::registerGeneric, LootParameterSets.ALL_PARAMS )
        );
    }

    @Override
    protected void validate( Map<ResourceLocation, LootTable> map, @Nonnull ValidationTracker validationtracker )
    {
        map.forEach( ( id, table ) -> LootTableManager.validate( validationtracker, id, table ) );
    }

    private static void registerBlocks( BiConsumer<ResourceLocation, LootTable.Builder> add )
    {
        namedBlockDrop( add, Registry.ModBlocks.DISK_DRIVE );
        selfDrop( add, Registry.ModBlocks.MONITOR_NORMAL );
        selfDrop( add, Registry.ModBlocks.MONITOR_ADVANCED );
        namedBlockDrop( add, Registry.ModBlocks.PRINTER );
        selfDrop( add, Registry.ModBlocks.SPEAKER );
        selfDrop( add, Registry.ModBlocks.WIRED_MODEM_FULL );
        selfDrop( add, Registry.ModBlocks.WIRELESS_MODEM_NORMAL );
        selfDrop( add, Registry.ModBlocks.WIRELESS_MODEM_ADVANCED );

        computerDrop( add, Registry.ModBlocks.COMPUTER_NORMAL );
        computerDrop( add, Registry.ModBlocks.COMPUTER_ADVANCED );
        computerDrop( add, Registry.ModBlocks.COMPUTER_COMMAND );
        computerDrop( add, Registry.ModBlocks.TURTLE_NORMAL );
        computerDrop( add, Registry.ModBlocks.TURTLE_ADVANCED );

        add.accept( Registry.ModBlocks.CABLE.get().getLootTable(), LootTable
            .lootTable()
            .withPool( LootPool.lootPool()
                .setRolls( ConstantRange.exactly( 1 ) )
                .add( ItemLootEntry.lootTableItem( Registry.ModItems.CABLE.get() ) )
                .when( SurvivesExplosion.survivesExplosion() )
                .when( BlockStateProperty.hasBlockStateProperties( Registry.ModBlocks.CABLE.get() )
                    .setProperties( StatePropertiesPredicate.Builder.properties().hasProperty( BlockCable.CABLE, true ) )
                )
            )
            .withPool( LootPool.lootPool()
                .setRolls( ConstantRange.exactly( 1 ) )
                .add( ItemLootEntry.lootTableItem( Registry.ModItems.WIRED_MODEM.get() ) )
                .when( SurvivesExplosion.survivesExplosion() )
                .when( BlockStateProperty.hasBlockStateProperties( Registry.ModBlocks.CABLE.get() )
                    .setProperties( StatePropertiesPredicate.Builder.properties().hasProperty( BlockCable.MODEM, CableModemVariant.None ) )
                    .invert()
                )
            ) );
    }

    private static void registerGeneric( BiConsumer<ResourceLocation, LootTable.Builder> add )
    {
        add.accept( CommonHooks.LOOT_TREASURE_DISK, LootTable.lootTable() );
    }

    private static void selfDrop( BiConsumer<ResourceLocation, LootTable.Builder> add, Supplier<? extends Block> wrapper )
    {
        blockDrop( add, wrapper, ItemLootEntry.lootTableItem( wrapper.get() ), SurvivesExplosion.survivesExplosion() );
    }

    private static void namedBlockDrop( BiConsumer<ResourceLocation, LootTable.Builder> add, Supplier<? extends Block> wrapper )
    {
        blockDrop(
            add, wrapper,
            ItemLootEntry.lootTableItem( wrapper.get() ).apply( CopyName.copyName( CopyName.Source.BLOCK_ENTITY ) ),
            SurvivesExplosion.survivesExplosion()
        );
    }

    private static void computerDrop( BiConsumer<ResourceLocation, LootTable.Builder> add, Supplier<? extends Block> block )
    {
        blockDrop(
            add, block,
            DynamicLootEntry.dynamicEntry( new ResourceLocation( ComputerCraft.MOD_ID, "computer" ) ),
            Alternative.alternative(
                BlockNamedEntityLootCondition.BUILDER,
                HasComputerIdLootCondition.BUILDER,
                PlayerCreativeLootCondition.BUILDER.invert()
            )
        );
    }

    private static void blockDrop(
        BiConsumer<ResourceLocation, LootTable.Builder> add, Supplier<? extends Block> wrapper,
        LootEntry.Builder<?> drop,
        ILootCondition.IBuilder condition
    )
    {
        Block block = wrapper.get();
        add.accept( block.getLootTable(), LootTable
            .lootTable()
            .withPool( LootPool.lootPool()
                .setRolls( ConstantRange.exactly( 1 ) )
                .add( drop )
                .when( condition )
            )
        );
    }
}
