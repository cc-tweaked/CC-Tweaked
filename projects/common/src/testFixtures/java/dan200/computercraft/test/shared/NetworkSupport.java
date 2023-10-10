// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Support methods for working with Minecraft's networking code.
 */
public final class NetworkSupport {
    private NetworkSupport() {
    }

    /**
     * Attempt to serialise and then deserialise a value.
     *
     * @param value The value to serialise.
     * @param write Serialise this value to a buffer.
     * @param read  Deserialise this value from a buffer.
     * @param <T>   The type of the value to round trip.
     * @return The converted value, for checking equivalency.
     */
    public static <T> T roundTrip(T value, BiConsumer<T, FriendlyByteBuf> write, Function<FriendlyByteBuf, T> read) {
        var buffer = new FriendlyByteBuf(Unpooled.directBuffer());
        write.accept(value, buffer);

        var converted = read.apply(buffer);
        assertEquals(buffer.readableBytes(), 0, "Whole packet was read");
        return converted;
    }

    /**
     * Attempt to serialise and then deserialise a value from a {@link RecipeSerializer}-like interface.
     *
     * @param id    The id of this value.
     * @param value The value to serialise.
     * @param write Serialise this value to a buffer.
     * @param read  Deserialise this value from a buffer.
     * @param <T>   The type of the value to round trip.
     * @return The converted value, for checking equivalency.
     */
    public static <T> T roundTripSerialiser(ResourceLocation id, T value, BiConsumer<FriendlyByteBuf, T> write, BiFunction<ResourceLocation, FriendlyByteBuf, T> read) {
        return roundTrip(value, (x, b) -> write.accept(b, x), b -> read.apply(id, b));
    }
}
