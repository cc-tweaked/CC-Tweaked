/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.data;

import javax.annotation.Nonnull;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;
import java.util.Collections;
import java.util.Set;

/**
 * A loot condition which checks if the entity is in creative mode.
 */
public final class PlayerCreativeLootCondition implements LootItemCondition
{
    public static final PlayerCreativeLootCondition INSTANCE = new PlayerCreativeLootCondition();
    public static final LootItemConditionType TYPE = ConstantLootConditionSerializer.type( INSTANCE );
    public static final Builder BUILDER = () -> INSTANCE;

    private PlayerCreativeLootCondition()
    {
    }

    @Override
    public boolean test( LootContext lootContext )
    {
        Entity entity = lootContext.getParamOrNull( LootContextParams.THIS_ENTITY );
        return entity instanceof Player && ((Player) entity).getAbilities().instabuild;
    }

    @Nonnull
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams()
    {
        return Collections.singleton( LootContextParams.THIS_ENTITY );
    }

    @Override
    @Nonnull
    public LootItemConditionType getType()
    {
        return TYPE;
    }
}
