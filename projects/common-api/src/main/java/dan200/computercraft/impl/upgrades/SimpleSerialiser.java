/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.impl.upgrades;

import com.google.gson.JsonObject;
import dan200.computercraft.api.upgrades.IUpgradeBase;
import dan200.computercraft.api.upgrades.UpgradeSerialiser;
import net.minecraft.network.FriendlyByteBuf;
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
public abstract class SimpleSerialiser<T extends IUpgradeBase> implements UpgradeSerialiser<T> {
    private final Function<ResourceLocation, T> constructor;

    public SimpleSerialiser(Function<ResourceLocation, T> constructor) {
        this.constructor = constructor;
    }

    @Override
    public final T fromJson(ResourceLocation id, JsonObject object) {
        return constructor.apply(id);
    }

    @Override
    public final T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
        return constructor.apply(id);
    }

    @Override
    public final void toNetwork(FriendlyByteBuf buffer, T upgrade) {
    }
}
