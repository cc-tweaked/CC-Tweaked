// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.CommonHooks;
import dan200.computercraft.shared.loot.InjectLootTableModifier;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.predicates.AnyOfCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.data.GlobalLootModifierProvider;
import net.minecraftforge.common.loot.LootTableIdCondition;

final class LootModifierProvider extends GlobalLootModifierProvider {
    LootModifierProvider(PackOutput output) {
        super(output, ComputerCraftAPI.MOD_ID);
    }

    @Override
    protected void start() {
        add("treasure_disk", new InjectLootTableModifier(
            new LootItemCondition[]{
                AnyOfCondition.anyOf(
                    CommonHooks.TREASURE_DISK_LOOT_TABLES.stream().map(LootTableIdCondition::builder).toArray(LootItemCondition.Builder[]::new)
                ).build(),
            },
            CommonHooks.TREASURE_DISK_LOOT
        ));
    }
}
