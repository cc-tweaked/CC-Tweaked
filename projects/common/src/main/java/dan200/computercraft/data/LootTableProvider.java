// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.CommonHooks;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.data.BlockNamedEntityLootCondition;
import dan200.computercraft.shared.data.HasComputerIdLootCondition;
import dan200.computercraft.shared.data.PlayerCreativeLootCondition;
import dan200.computercraft.shared.peripheral.modem.wired.CableBlock;
import dan200.computercraft.shared.peripheral.modem.wired.CableModemVariant;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.data.loot.LootTableProvider.SubProviderEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.DynamicLoot;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.CopyNameFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

class LootTableProvider {
    public static List<SubProviderEntry> getTables() {
        return List.of(
            new SubProviderEntry(() -> LootTableProvider::registerBlocks, LootContextParamSets.BLOCK),
            new SubProviderEntry(() -> LootTableProvider::registerGeneric, LootContextParamSets.ALL_PARAMS)
        );
    }

    private static void registerBlocks(BiConsumer<ResourceLocation, LootTable.Builder> add) {
        namedBlockDrop(add, ModRegistry.Blocks.DISK_DRIVE);
        selfDrop(add, ModRegistry.Blocks.MONITOR_NORMAL);
        selfDrop(add, ModRegistry.Blocks.MONITOR_ADVANCED);
        namedBlockDrop(add, ModRegistry.Blocks.PRINTER);
        selfDrop(add, ModRegistry.Blocks.SPEAKER);
        selfDrop(add, ModRegistry.Blocks.WIRED_MODEM_FULL);
        selfDrop(add, ModRegistry.Blocks.WIRELESS_MODEM_NORMAL);
        selfDrop(add, ModRegistry.Blocks.WIRELESS_MODEM_ADVANCED);
        selfDrop(add, ModRegistry.Blocks.REDSTONE_RELAY);

        computerDrop(add, ModRegistry.Blocks.COMPUTER_NORMAL);
        computerDrop(add, ModRegistry.Blocks.COMPUTER_ADVANCED);
        computerDrop(add, ModRegistry.Blocks.COMPUTER_COMMAND);
        computerDrop(add, ModRegistry.Blocks.TURTLE_NORMAL);
        computerDrop(add, ModRegistry.Blocks.TURTLE_ADVANCED);

        blockDrop(add, ModRegistry.Blocks.LECTERN, LootItem.lootTableItem(Items.LECTERN), ExplosionCondition.survivesExplosion());

        add.accept(ModRegistry.Blocks.CABLE.get().getLootTable(), LootTable
            .lootTable()
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ModRegistry.Items.CABLE.get()))
                .when(ExplosionCondition.survivesExplosion())
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModRegistry.Blocks.CABLE.get())
                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CableBlock.CABLE, true))
                )
            )
            .withPool(LootPool.lootPool()
                .setRolls(ConstantValue.exactly(1))
                .add(LootItem.lootTableItem(ModRegistry.Items.WIRED_MODEM.get()))
                .when(ExplosionCondition.survivesExplosion())
                .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(ModRegistry.Blocks.CABLE.get())
                    .setProperties(StatePropertiesPredicate.Builder.properties().hasProperty(CableBlock.MODEM, CableModemVariant.None))
                    .invert()
                )
            ));
    }

    private static void registerGeneric(BiConsumer<ResourceLocation, LootTable.Builder> add) {
        add.accept(CommonHooks.TREASURE_DISK_LOOT, LootTable.lootTable());
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
            DynamicLoot.dynamicEntry(new ResourceLocation(ComputerCraftAPI.MOD_ID, "computer")),
            AnyOfCondition.anyOf(
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
