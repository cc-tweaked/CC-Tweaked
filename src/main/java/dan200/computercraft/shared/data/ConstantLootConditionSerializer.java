/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import javax.annotation.Nonnull;
import net.minecraft.loot.condition.LootCondition;
import net.minecraft.loot.condition.LootConditionType;
import net.minecraft.util.JsonSerializer;

public final class ConstantLootConditionSerializer<T extends LootCondition> implements JsonSerializer<T>
{
    private final T instance;

    public ConstantLootConditionSerializer( T instance )
    {
        this.instance = instance;
    }

    public static <T extends LootCondition> LootConditionType type( T condition )
    {
        return new LootConditionType( new ConstantLootConditionSerializer<>( condition ) );
    }

    @Override
    public void func_230424_a_( @Nonnull JsonObject json, @Nonnull T object, @Nonnull JsonSerializationContext context )
    {
    }

    @Nonnull
    @Override
    public T fromJson( @Nonnull JsonObject json, @Nonnull JsonDeserializationContext context )
    {
        return instance;
    }
}
