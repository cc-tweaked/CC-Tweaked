/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.data;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameter;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.ILootCondition;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * A loot condition which checks if the entity is in creative mode.
 */
public final class PlayerCreativeLootCondition implements ILootCondition
{
    public static final PlayerCreativeLootCondition INSTANCE = new PlayerCreativeLootCondition();
    public static final LootConditionType TYPE = ConstantLootConditionSerializer.type( INSTANCE );
    public static final IBuilder BUILDER = () -> INSTANCE;

    private PlayerCreativeLootCondition()
    {
    }

    @Override
    public boolean test( LootContext lootContext )
    {
        Entity entity = lootContext.getParamOrNull( LootParameters.THIS_ENTITY );
        return entity instanceof PlayerEntity && ((PlayerEntity) entity).abilities.instabuild;
    }

    @Nonnull
    @Override
    public Set<LootParameter<?>> getReferencedContextParams()
    {
        return Collections.singleton( LootParameters.THIS_ENTITY );
    }

    @Override
    @Nonnull
    public LootConditionType getType()
    {
        return TYPE;
    }
}
