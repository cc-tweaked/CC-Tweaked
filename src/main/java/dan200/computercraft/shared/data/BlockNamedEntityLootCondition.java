/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.data;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.INameable;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameter;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraft.world.storage.loot.conditions.ILootCondition;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * A loot condition which checks if the tile entity has a name.
 */
public final class BlockNamedEntityLootCondition implements ILootCondition
{
    public static final BlockNamedEntityLootCondition INSTANCE = new BlockNamedEntityLootCondition();

    private BlockNamedEntityLootCondition()
    {
    }

    @Override
    public boolean test( LootContext lootContext )
    {
        TileEntity tile = lootContext.get( LootParameters.BLOCK_ENTITY );
        return tile instanceof INameable && ((INameable) tile).hasCustomName();
    }

    @Nonnull
    @Override
    public Set<LootParameter<?>> getRequiredParameters()
    {
        return Collections.singleton( LootParameters.BLOCK_ENTITY );
    }
}
