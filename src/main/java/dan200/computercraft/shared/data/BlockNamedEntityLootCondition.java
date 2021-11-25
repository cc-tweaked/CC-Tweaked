/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.data;

import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameter;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.conditions.ILootCondition;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.INameable;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * A loot condition which checks if the tile entity has a name.
 */
public final class BlockNamedEntityLootCondition implements ILootCondition
{
    public static final BlockNamedEntityLootCondition INSTANCE = new BlockNamedEntityLootCondition();
    public static final LootConditionType TYPE = ConstantLootConditionSerializer.type( INSTANCE );
    public static final IBuilder BUILDER = () -> INSTANCE;

    private BlockNamedEntityLootCondition()
    {
    }

    @Override
    public boolean test( LootContext lootContext )
    {
        TileEntity tile = lootContext.getParamOrNull( LootParameters.BLOCK_ENTITY );
        return tile instanceof INameable && ((INameable) tile).hasCustomName();
    }

    @Nonnull
    @Override
    public Set<LootParameter<?>> getReferencedContextParams()
    {
        return Collections.singleton( LootParameters.BLOCK_ENTITY );
    }

    @Override
    @Nonnull
    public LootConditionType getType()
    {
        return TYPE;
    }
}
