/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

import javax.annotation.Nonnull;

public final class ConstantLootConditionSerializer<T extends LootItemCondition> implements Serializer<T>
{
    private final T instance;

    public ConstantLootConditionSerializer( T instance )
    {
        this.instance = instance;
    }

    public static <T extends LootItemCondition> LootItemConditionType type( T condition )
    {
        return new LootItemConditionType( new ConstantLootConditionSerializer<>( condition ) );
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
