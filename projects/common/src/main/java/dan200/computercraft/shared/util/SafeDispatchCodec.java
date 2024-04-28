// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import com.mojang.serialization.*;
import com.mojang.serialization.codecs.KeyDispatchCodec;
import dan200.computercraft.impl.RegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

import java.util.function.Function;
import java.util.stream.Stream;

/**
 * An implementation of {@link Codec#dispatch(Function, Function)}/{@link KeyDispatchCodec} that checks that the
 * result of {@code instance.getType()} matches the supplied type.
 *
 * @param <K> The type of keys.
 * @param <V> The type of values.
 */
public final class SafeDispatchCodec<K, V> extends MapCodec<V> {
    private static final String TYPE_KEY = "type";
    private static final String COMPRESSED_VALUE_KEY = "value";

    private final Codec<K> typeCodec;
    private final Function<V, K> type;
    private final Function<K, MapCodec<? extends V>> instanceCodec;

    private SafeDispatchCodec(Codec<K> typeCodec, Function<V, K> type, Function<K, MapCodec<? extends V>> instanceCodec) {
        this.typeCodec = typeCodec;
        this.type = type;
        this.instanceCodec = instanceCodec;
    }

    /**
     * Create a new safe dispatch codec.
     * <p>
     * This decodes the {@code "type"} field of a map (using {@code typeCodec}), and then uses {@code instanceCodec} to
     * find the codec that should be used for that type.
     *
     * @param typeCodec     The codec to decode the type.
     * @param type          The function to get the type of an instance.
     * @param instanceCodec The codec to decode an instance.
     * @param <K>           The type of keys.
     * @param <V>           The type of values.
     * @return The dispatch codec.
     */
    public static <K, V> Codec<V> codec(Codec<K> typeCodec, Function<V, K> type, Function<K, MapCodec<? extends V>> instanceCodec) {
        return new SafeDispatchCodec<>(typeCodec, type, instanceCodec).codec();
    }

    /**
     * Create a new safe dispatch codec.
     * <p>
     * This decodes the {@code "type"} field of a map (using {@code typeCodec}), and then uses {@code instanceCodec} to
     * find the codec that should be used for that type.
     *
     * @param typeRegistry  The built-in registry of types.
     * @param type          The function to get the type of an instance.
     * @param instanceCodec The codec to decode an instance.
     * @param <K>           The type of keys.
     * @param <V>           The type of values.
     * @return The dispatch codec.
     */
    public static <K, V> Codec<V> ofRegistry(ResourceKey<Registry<K>> typeRegistry, Function<V, K> type, Function<K, MapCodec<? extends V>> instanceCodec) {
        return codec(Codec.lazyInitialized(() -> RegistryHelper.getRegistry(typeRegistry).byNameCodec()), type, instanceCodec);
    }

    @Override
    public <T> DataResult<V> decode(final DynamicOps<T> ops, final MapLike<T> input) {
        var elementName = input.get(TYPE_KEY);
        if (elementName == null) {
            return DataResult.error(() -> "Input does not contain a key [" + TYPE_KEY + "]: " + input);
        }

        return typeCodec.decode(ops, elementName).flatMap(typeResult -> {
            var type = typeResult.getFirst();
            var decoder = instanceCodec.apply(type);

            DataResult<? extends V> result;
            if (ops.compressMaps()) {
                var value = input.get(ops.createString(COMPRESSED_VALUE_KEY));
                if (value == null) return DataResult.error(() -> "Input does not have a \"value\" entry: " + input);
                result = decoder.decoder().parse(ops, value);
            } else {
                result = decoder.decode(ops, input);
            }

            return result.flatMap(x -> {
                var actualType = this.type.apply(x);
                if (actualType != type) {
                    return DataResult.error(() -> x + " has the incorrect type. Expected " + type + ", but was " + actualType + ".");
                }
                return DataResult.success(x);
            });
        });
    }

    @Override
    public <T> RecordBuilder<T> encode(final V input, final DynamicOps<T> ops, final RecordBuilder<T> builder) {
        @SuppressWarnings("unchecked") var encoder = (MapCodec<V>) instanceCodec.apply(type.apply(input));

        if (ops.compressMaps()) {
            return builder
                .add(TYPE_KEY, typeCodec.encodeStart(ops, type.apply(input)))
                .add(COMPRESSED_VALUE_KEY, encoder.encoder().encodeStart(ops, input));
        }

        return encoder.encode(input, ops, builder).add(TYPE_KEY, typeCodec.encodeStart(ops, type.apply(input)));
    }

    @Override
    public <T> Stream<T> keys(final DynamicOps<T> ops) {
        return Stream.of(TYPE_KEY, COMPRESSED_VALUE_KEY).map(ops::createString);
    }
}
