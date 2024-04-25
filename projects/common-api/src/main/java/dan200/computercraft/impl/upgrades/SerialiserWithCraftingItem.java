// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.upgrades;

import com.google.gson.JsonObject;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
public final class SerialiserWithCraftingItem<T extends UpgradeBase> implements UpgradeSerialiser<T> {
    private final BiFunction<ResourceLocation, ItemStack, T> factory;

    public SerialiserWithCraftingItem(BiFunction<ResourceLocation, ItemStack, T> factory) {
        this.factory = factory;
    }

    @Override
    public T fromJson(ResourceLocation id, JsonObject object) {
        var item = GsonHelper.getAsItem(object, "item");
        return factory.apply(id, new ItemStack(item));
    }

    @Override
    public T fromNetwork(ResourceLocation id, RegistryFriendlyByteBuf buffer) {
        var item = ItemStack.STREAM_CODEC.decode(buffer);
        return factory.apply(id, item);
    }

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buffer, T upgrade) {
        ItemStack.STREAM_CODEC.encode(buffer, upgrade.getCraftingItem());
    }
}
