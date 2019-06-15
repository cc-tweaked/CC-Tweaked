/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.shared.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.conditions.ILootCondition;

import javax.annotation.Nonnull;

public final class ConstantLootConditionSerializer<T extends ILootCondition> extends ILootCondition.AbstractSerializer<T>
{
    private final T instance;

    private ConstantLootConditionSerializer( ResourceLocation id, Class<T> klass, T instance )
    {
        super( id, klass );
        this.instance = instance;
    }

    public static <T extends ILootCondition> ILootCondition.AbstractSerializer<T> of( ResourceLocation id, Class<T> klass, T instance )
    {
        return new ConstantLootConditionSerializer<>( id, klass, instance );
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
