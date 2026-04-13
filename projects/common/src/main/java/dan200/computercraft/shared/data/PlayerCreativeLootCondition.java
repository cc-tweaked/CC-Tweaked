// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.data;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Set;

/**
 * A loot condition which checks if the entity is in creative mode.
 */
public final class PlayerCreativeLootCondition implements LootItemCondition {
    public static final PlayerCreativeLootCondition INSTANCE = new PlayerCreativeLootCondition();
    public static final Builder BUILDER = () -> INSTANCE;
    public static final MapCodec<PlayerCreativeLootCondition> CODEC = MapCodec.unit(INSTANCE);

    private PlayerCreativeLootCondition() {
    }

    @Override
    public boolean test(LootContext lootContext) {
        var entity = lootContext.getOptionalParameter(LootContextParams.THIS_ENTITY);
        return entity instanceof Player player && player.isCreative();
    }

    @Override
    public Set<ContextKey<?>> getReferencedContextParams() {
        return Set.of(LootContextParams.THIS_ENTITY);
    }

    @Override
    public MapCodec<PlayerCreativeLootCondition> codec() {
        return CODEC;
    }
}
