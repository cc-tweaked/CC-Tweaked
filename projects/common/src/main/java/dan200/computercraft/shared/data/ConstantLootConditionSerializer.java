// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.data;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import net.minecraft.world.level.storage.loot.Serializer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemConditionType;

public final class ConstantLootConditionSerializer<T extends LootItemCondition> implements Serializer<T> {
    private final T instance;

    public ConstantLootConditionSerializer(T instance) {
        this.instance = instance;
    }

    public static <T extends LootItemCondition> LootItemConditionType type(T condition) {
        return new LootItemConditionType(new ConstantLootConditionSerializer<>(condition));
    }

    @Override
    public void serialize(JsonObject json, T object, JsonSerializationContext context) {
    }

    @Override
    public T deserialize(JsonObject json, JsonDeserializationContext context) {
        return instance;
    }
}
