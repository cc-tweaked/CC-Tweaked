/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.loot.ILootSerializer;
import net.minecraft.loot.LootConditionType;
import net.minecraft.loot.conditions.ILootCondition;

import javax.annotation.Nonnull;

public final class ConstantLootConditionSerializer<T extends ILootCondition> implements ILootSerializer<T>
{
    private final T instance;

    public ConstantLootConditionSerializer( T instance )
    {
        this.instance = instance;
    }

    public static <T extends ILootCondition> LootConditionType type( T condition )
    {
        return new LootConditionType( new ConstantLootConditionSerializer<>( condition ) );
    }

    @Override
    public void serialize( @Nonnull JsonObject json, @Nonnull T object, @Nonnull JsonSerializationContext context )
    {
    }

    @Nonnull
    @Override
    public T deserialize( @Nonnull JsonObject json, @Nonnull JsonDeserializationContext context )
    {
        return instance;
    }
}
