// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.upgrades;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.BiFunction;

/**
 * Simple serialiser which returns a constant upgrade with a custom crafting item.
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 *
 * @param <T> The upgrade that this class can serialise and deserialise.
 */
@ApiStatus.Internal
public abstract class SerialiserWithCraftingIngredient<T extends UpgradeBase> implements UpgradeSerialiser<T> {
    private final BiFunction<ResourceLocation, Ingredient, T> factory;

    protected SerialiserWithCraftingIngredient(BiFunction<ResourceLocation, Ingredient, T> factory) {
        this.factory = factory;
    }

    @Override
    public final T fromJson(ResourceLocation id, JsonObject object) {
        return factory.apply(id, getIngredientOrItem(object, "item"));
    }

    @Override
    public final T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        return factory.apply(id, Ingredient.fromNetwork(buffer));
    }

    @Override
    public final void toNetwork(FriendlyByteBuf buffer, T upgrade) {
        upgrade.getCraftingIngredient().toNetwork(buffer);
    }

    public static Ingredient getIngredientOrItem(JsonObject object, String key) {
        return object.get(key) instanceof JsonPrimitive p && p.isString()
            ? Ingredient.of(GsonHelper.getAsItem(object, key))
            : Ingredient.fromJson(GsonHelper.getAsJsonObject(object, key));
    }
}
