// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.upgrades;

import com.google.gson.JsonObject;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.ItemStack;
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
public abstract class SerialiserWithCraftingItem<T extends UpgradeBase> implements UpgradeSerialiser<T> {
    private final BiFunction<ResourceLocation, ItemStack, T> factory;

    protected SerialiserWithCraftingItem(BiFunction<ResourceLocation, ItemStack, T> factory) {
        this.factory = factory;
    }

    @Override
    public final T fromJson(ResourceLocation id, JsonObject object) {
        var item = GsonHelper.getAsItem(object, "item");
        return factory.apply(id, new ItemStack(item));
    }

    @Override
    public final T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        var item = buffer.readItem();
        return factory.apply(id, item);
    }

    @Override
    public final void toNetwork(FriendlyByteBuf buffer, T upgrade) {
        buffer.writeItem(upgrade.getCraftingItem());
    }
}
