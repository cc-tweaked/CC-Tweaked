// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.data;

import dan200.computercraft.shared.ModRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import java.util.Set;

/**
 * A loot condition which checks if the entity is in creative mode.
 */
public final class PlayerCreativeLootCondition implements LootItemCondition {
    public static final PlayerCreativeLootCondition INSTANCE = new PlayerCreativeLootCondition();
    public static final Builder BUILDER = () -> INSTANCE;

    private PlayerCreativeLootCondition() {
    }

    @Override
    public boolean test(LootContext lootContext) {
        var entity = lootContext.getParamOrNull(LootContextParams.THIS_ENTITY);
        return entity instanceof Player player && player.getAbilities().instabuild;
    }

    @Override
    public Set<LootContextParam<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    public LootItemConditionType getType() {
        return ModRegistry.LootItemConditionTypes.PLAYER_CREATIVE.get();
    }
}
