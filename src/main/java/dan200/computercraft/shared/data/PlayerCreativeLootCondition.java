/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.data;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * A loot condition which checks if the entity is in creative mode.
 */
public final class PlayerCreativeLootCondition implements LootCondition
{
    public static final PlayerCreativeLootCondition INSTANCE = new PlayerCreativeLootCondition();
    public static final LootConditionType TYPE = ConstantLootConditionSerializer.type( INSTANCE );
    public static final Builder BUILDER = () -> INSTANCE;

    private PlayerCreativeLootCondition()
    {
    }

    @Override
    public boolean test( LootContext lootContext )
    {
        Entity entity = lootContext.get( LootContextParameters.THIS_ENTITY );
        return entity instanceof PlayerEntity && ((PlayerEntity) entity).getAbilities().creativeMode;
    }

    @Nonnull
    @Override
    public Set<LootContextParameter<?>> getRequiredParameters()
    {
        return Collections.singleton( LootContextParameters.THIS_ENTITY );
    }

    @Override
    @Nonnull
    public LootConditionType getType()
    {
        return TYPE;
    }
}
