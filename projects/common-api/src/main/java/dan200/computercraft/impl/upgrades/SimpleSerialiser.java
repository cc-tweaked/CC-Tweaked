// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.upgrades;

import com.google.gson.JsonObject;
import dan200.computercraft.api.upgrades.UpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Function;

/**
 * Simple serialiser which returns a constant upgrade.
 * <p>
 * Do <strong>NOT</strong> directly reference this class. It exists for internal use by the API.
 *
 * @param <T> The upgrade that this class can serialise and deserialise.
 */
@ApiStatus.Internal
public final class SimpleSerialiser<T extends UpgradeBase> implements UpgradeSerialiser<T> {
    private final Function<ResourceLocation, T> constructor;

    public SimpleSerialiser(Function<ResourceLocation, T> constructor) {
        this.constructor = constructor;
    }

    @Override
    public T fromJson(ResourceLocation id, JsonObject object) {
        return constructor.apply(id);
    }

    @Override
    public T fromNetwork(ResourceLocation id, RegistryFriendlyByteBuf buffer) {
        return constructor.apply(id);
    }

    @Override
    public void toNetwork(RegistryFriendlyByteBuf buffer, T upgrade) {
    }
}
