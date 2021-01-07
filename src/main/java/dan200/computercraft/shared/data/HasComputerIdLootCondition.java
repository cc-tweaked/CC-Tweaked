/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.data;

import dan200.computercraft.shared.computer.blocks.IComputerTile;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.LootParameter;
import net.minecraft.world.storage.loot.LootParameters;
import net.minecraft.world.storage.loot.conditions.ILootCondition;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.Set;

/**
 * A loot condition which checks if the tile entity has has a non-0 ID.
 */
public final class HasComputerIdLootCondition implements ILootCondition
{
    public static final HasComputerIdLootCondition INSTANCE = new HasComputerIdLootCondition();

    private HasComputerIdLootCondition()
    {
    }

    @Override
    public boolean test( LootContext lootContext )
    {
        TileEntity tile = lootContext.get( LootParameters.BLOCK_ENTITY );
        return tile instanceof IComputerTile && ((IComputerTile) tile).getComputerID() >= 0;
    }

    @Nonnull
    @Override
    public Set<LootParameter<?>> getRequiredParameters()
    {
        return Collections.singleton( LootParameters.BLOCK_ENTITY );
    }

    public static IBuilder builder()
    {
        return () -> INSTANCE;
    }
}
