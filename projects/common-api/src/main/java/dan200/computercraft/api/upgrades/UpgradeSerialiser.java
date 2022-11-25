/*
 * This file is part of the public ComputerCraft API - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. This API may be redistributed unmodified and in full only.
 * For help using the API, and posting your mods, visit the forums at computercraft.info.
 */
package dan200.computercraft.api.upgrades;

import com.google.gson.JsonObject;
import dan200.computercraft.api.pocket.PocketUpgradeSerialiser;
import dan200.computercraft.api.turtle.TurtleUpgradeSerialiser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;


/**
 * Base interface for upgrade serialisers. This should generally not be implemented directly, instead implementing one
 * of {@link TurtleUpgradeSerialiser} or {@link PocketUpgradeSerialiser}.
 * <p>
 * However, it may sometimes be useful to implement this if you have some shared logic between upgrade types.
 *
 * @param <T> The upgrade that this class can serialise and deserialise.
 * @see TurtleUpgradeSerialiser
 * @see PocketUpgradeSerialiser
 */
public interface UpgradeSerialiser<T extends UpgradeBase> {
    /**
     * Read this upgrade from a JSON file in a datapack.
     *
     * @param id     The ID of this upgrade.
     * @param object The JSON object to load this upgrade from.
     * @return The constructed upgrade, with a {@link UpgradeBase#getUpgradeID()} equal to {@code id}.
     * @see net.minecraft.util.GsonHelper For additional JSON helper methods.
     */
    T fromJson(ResourceLocation id, JsonObject object);

    /**
     * Read this upgrade from a network packet, sent from the server.
     *
     * @param id     The ID of this upgrade.
     * @param buffer The buffer object to read this upgrade from.
     * @return The constructed upgrade, with a {@link UpgradeBase#getUpgradeID()} equal to {@code id}.
     */
    T fromNetwork(ResourceLocation id, FriendlyByteBuf buffer);

    /**
     * Write this upgrade to a network packet, to be sent to the client.
     *
     * @param buffer  The buffer object to write this upgrade to
     * @param upgrade The upgrade to write.
     */
    void toNetwork(FriendlyByteBuf buffer, T upgrade);

}
