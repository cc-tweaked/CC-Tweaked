// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.data;

import dan200.computercraft.shared.ModRegistry;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;

/**
 * A loot condition which checks if the block entity has a name.
 */
public final class BlockNamedEntityLootCondition implements LootItemCondition {
    public static final BlockNamedEntityLootCondition INSTANCE = new BlockNamedEntityLootCondition();
    public static final Builder BUILDER = () -> INSTANCE;

    private BlockNamedEntityLootCondition() {
    }

    @Override
    public boolean test(LootContext lootContext) {
        var tile = lootContext.getParamOrNull(LootContextParams.BLOCK_ENTITY);
        return tile instanceof Nameable nameable && nameable.hasCustomName();
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.BLOCK_ENTITY);
    }

    @Override
    public LootItemConditionType getType() {
        return ModRegistry.LootItemConditionTypes.BLOCK_NAMED.get();
    }
}
