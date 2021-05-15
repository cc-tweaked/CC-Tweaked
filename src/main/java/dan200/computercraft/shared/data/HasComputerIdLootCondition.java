/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.data;

import java.util.Collections;
import java.util.Set;

import javax.annotation.Nonnull;

import dan200.computercraft.shared.computer.blocks.IComputerTile;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.loot.context.LootContext;
import net.minecraft.loot.context.LootContextParameter;
import net.minecraft.loot.context.LootContextParameters;

/**
 * A loot condition which checks if the tile entity has has a non-0 ID.
 */
public final class HasComputerIdLootCondition implements LootCondition {
    public static final HasComputerIdLootCondition INSTANCE = new HasComputerIdLootCondition();
    public static final LootConditionType TYPE = ConstantLootConditionSerializer.type(INSTANCE);
    public static final Builder BUILDER = () -> INSTANCE;

    private HasComputerIdLootCondition() {
    }

    @Override
    public boolean test(LootContext lootContext) {
        BlockEntity tile = lootContext.get(LootContextParameters.BLOCK_ENTITY);
        return tile instanceof IComputerTile && ((IComputerTile) tile).getComputerID() >= 0;
    }

    @Nonnull
    @Override
    public Set<LootContextParameter<?>> getRequiredParameters() {
        return Collections.singleton(LootContextParameters.BLOCK_ENTITY);
    }

    @Override
    @Nonnull
    public LootConditionType getType() {
        return TYPE;
    }
}
