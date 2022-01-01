/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.data;

import dan200.computercraft.shared.computer.blocks.IComputerTile;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParam;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * A loot condition which checks if the tile entity has has a non-0 ID.
 */
public final class HasComputerIdLootCondition implements LootItemCondition
{
    public static final HasComputerIdLootCondition INSTANCE = new HasComputerIdLootCondition();
    public static final LootItemConditionType TYPE = ConstantLootConditionSerializer.type( INSTANCE );
    public static final Builder BUILDER = () -> INSTANCE;

    private HasComputerIdLootCondition()
    {
    }

    @Override
    public boolean test( LootContext lootContext )
    {
        BlockEntity tile = lootContext.getParamOrNull( LootContextParams.BLOCK_ENTITY );
        return tile instanceof IComputerTile computer && computer.getComputerID() >= 0;
    }

    @Nonnull
    @Override
    public Set<LootContextParam<?>> getReferencedContextParams()
    {
        return Collections.singleton( LootContextParams.BLOCK_ENTITY );
    }

    @Override
    @Nonnull
    public LootItemConditionType getType()
    {
        return TYPE;
    }
}
