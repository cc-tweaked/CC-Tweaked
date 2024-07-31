// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.data;

import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;

/**
 * A loot condition which checks if the tile entity has a non-0 ID.
 */
public final class HasComputerIdLootCondition implements LootItemCondition {
    public static final HasComputerIdLootCondition INSTANCE = new HasComputerIdLootCondition();
    public static final Builder BUILDER = () -> INSTANCE;

    private HasComputerIdLootCondition() {
    }

    @Override
    public boolean test(LootContext lootContext) {
        var tile = lootContext.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        return tile instanceof AbstractComputerBlockEntity computer && computer.getComputerID() >= 0;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_ENTITY);
    }

    @Override
    public LootItemConditionType getType() {
        return ModRegistry.LootItemConditionTypes.HAS_ID.get();
    }
}
