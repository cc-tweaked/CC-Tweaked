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
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.LootTables;
import net.minecraft.world.level.storage.loot.ValidationContext;
import net.minecraft.world.level.storage.loot.entries.DynamicLoot;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSet;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.AlternativeLootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

class LootTableGenerator extends LootTableProvider {
    LootTableGenerator(DataGenerator generator) {
        super(generator);
    }

    @Nonnull
    @Override
    public List<Pair<Supplier<Consumer<BiConsumer<ResourceLocation, LootTable.Builder>>>, LootContextParamSet>> getTables() {
        return List.of(
            Pair.of(() -> LootTableGenerator::registerBlocks, LootContextParamSets.BLOCK),
            Pair.of(() -> LootTableGenerator::registerGeneric, LootContextParamSets.ALL_PARAMS)
        );
    }

    @Override
    protected void validate(Map<ResourceLocation, LootTable> map, @Nonnull ValidationContext validationtracker) {
        map.forEach((id, table) -> LootTables.validate(validationtracker, id, table));
    }

    private static void registerBlocks(BiConsumer<ResourceLocation, LootTable.Builder> add) {
        namedBlockDrop(add, Registry.ModBlocks.DISK_DRIVE);
        selfDrop(add, Registry.ModBlocks.MONITOR_NORMAL);
        selfDrop(add, Registry.ModBlocks.MONITOR_ADVANCED);
        namedBlockDrop(add, Registry.ModBlocks.PRINTER);
        selfDrop(add, Registry.ModBlocks.SPEAKER);
        selfDrop(add, Registry.ModBlocks.WIRED_MODEM_FULL);
        selfDrop(add, Registry.ModBlocks.WIRELESS_MODEM_NORMAL);
        selfDrop(add, Registry.ModBlocks.WIRELESS_MODEM_ADVANCED);

        computerDrop(add, Registry.ModBlocks.COMPUTER_NORMAL);
        computerDrop(add, Registry.ModBlocks.COMPUTER_ADVANCED);
        computerDrop(add, Registry.ModBlocks.COMPUTER_COMMAND);
        computerDrop(add, Registry.ModBlocks.TURTLE_NORMAL);
        computerDrop(add, Registry.ModBlocks.TURTLE_ADVANCED);

        add.accept(Registry.ModBlocks.CABLE.get().getLootTable(), LootTable
            .lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(Registry.ModItems.CABLE.get()))
                .when(ExplosionCondition.survivesExplosion())
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(Registry.ModBlocks.CABLE.get())
                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(BlockCable.CABLE, true))
                )
            )
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(Registry.ModItems.WIRED_MODEM.get()))
                .when(ExplosionCondition.survivesExplosion())
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(Registry.ModBlocks.CABLE.get())
                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(BlockCable.MODEM, CableModemVariant.None))
                    .invert()
                )
            ));
    }

    private static void registerGeneric(BiConsumer<ResourceLocation, LootTable.Builder> add) {
        add.accept(CommonHooks.LOOT_TREASURE_DISK, LootTable.lootTable());
    }

    private static void selfDrop(BiConsumer<ResourceLocation, LootTable.Builder> add, Supplier<? extends Block> wrapper) {
        blockDrop(add, wrapper, LootItem.lootTableItem(wrapper.get()), ExplosionCondition.survivesExplosion());
    }

    private static void namedBlockDrop(BiConsumer<ResourceLocation, LootTable.Builder> add, Supplier<? extends Block> wrapper) {
        blockDrop(
            add, wrapper,
            LootItem.lootTableItem(wrapper.get()).apply(CopyNameFunction.copyName(CopyNameFunction.NameSource.BLOCK_ENTITY)),
            ExplosionCondition.survivesExplosion()
        );
    }

    private static void computerDrop(BiConsumer<ResourceLocation, LootTable.Builder> add, Supplier<? extends Block> block) {
        blockDrop(
            add, block,
            DynamicLoot.dynamicEntry(new ResourceLocation(ComputerCraft.MOD_ID, "computer")),
            AlternativeLootItemCondition.alternative(
                BlockNamedEntityLootCondition.BUILDER,
                HasComputerIdLootCondition.BUILDER,
                PlayerCreativeLootCondition.BUILDER.invert()
            )
        );
    }

    private static void blockDrop(
        BiConsumer<ResourceLocation, LootTable.Builder> add, Supplier<? extends Block> wrapper,
        LootPoolEntryContainer.Builder<?> drop,
        LootItemCondition.Builder condition
    ) {
        var block = wrapper.get();
        add.accept(block.getLootTable(), LootTable
            .lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(drop)
                .when(condition)
            )
        );
    }
}
