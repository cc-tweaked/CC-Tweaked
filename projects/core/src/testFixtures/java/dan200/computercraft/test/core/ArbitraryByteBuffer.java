// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.core;

import net.jqwik.api.*;
import net.jqwik.api.arbitraries.SizableArbitrary;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Random;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Generate arbitrary byte buffers with irrelevant (but random) contents.
 * <p>
 * This is more efficient than using {@link Arbitraries#bytes()} and {@link Arbitrary#array(Class)}, as it does not
 * try to shrink the contents, only the size.
 */
public final class ArbitraryByteBuffer implements SizableArbitrary<ByteBuffer> {
    private static final ArbitraryByteBuffer DEFAULT = new ArbitraryByteBuffer(0, null, null);

    private int minSize = 0;
    private final @Nullable Integer maxSize;
    private final @Nullable RandomDistribution distribution;

    private ArbitraryByteBuffer(int minSize, @Nullable Integer maxSize, @Nullable RandomDistribution distribution) {
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.distribution = distribution;
    }

    public static ArbitraryByteBuffer bytes() {
        return DEFAULT;
    }

    @Override
    public SizableArbitrary<ByteBuffer> ofMinSize(int minSize) {
        return new ArbitraryByteBuffer(minSize, maxSize, distribution);
    }

    @Override
    public SizableArbitrary<ByteBuffer> ofMaxSize(int maxSize) {
        return new ArbitraryByteBuffer(minSize, maxSize, distribution);
    }

    @Override
    public SizableArbitrary<ByteBuffer> withSizeDistribution(RandomDistribution distribution) {
        return new ArbitraryByteBuffer(minSize, maxSize, distribution);
    }

    @Override
    public RandomGenerator<ByteBuffer> generator(int genSize) {
        var min = BigInteger.valueOf(minSize);
        ToIntFunction<Random> generator;
        if (distribution == null) {
            generator = sizeGeneratorWithCutoff(minSize, getMaxSize(), genSize);
        } else {
            var gen = distribution.createGenerator(genSize, min, BigInteger.valueOf(getMaxSize()), min);
            generator = r -> gen.next(r).intValueExact();
        }
        return r -> {
            var size = generator.applyAsInt(r);
            return new ShrinkableBuffer(allocateRandom(size, r), minSize);
        };
    }

    @Override
    public EdgeCases<ByteBuffer> edgeCases(int maxEdgeCases) {
        return EdgeCases.fromSuppliers(List.of(
            () -> new ShrinkableBuffer(allocateRandom(minSize, new Random()), minSize),
            () -> new ShrinkableBuffer(allocateRandom(getMaxSize(), new Random()), minSize)
        ));
    }

    private int getMaxSize() {
        return maxSize == null ? Math.max(minSize * 2, 255) : maxSize;
    }

    private static ToIntFunction<Random> sizeGeneratorWithCutoff(int minSize, int maxSize, int genSize) {
        // If we've a large range, we either pick between generating small (<10) or large lists.
        var range = maxSize - minSize;
        var offset = (int) Math.max(Math.round(Math.sqrt(genSize)), 10);
        var cutoff = range <= offset ? maxSize : Math.min(offset + minSize, maxSize);

        if (cutoff >= maxSize) return random -> nextInt(random, minSize, maxSize);

        // Choose size below cutoff with probability of 0.1.
        var maxSizeProbability = Math.min(0.02, 1.0 / (genSize / 10.0));
        var cutoffProbability = 0.1;
        return random -> {
            if (random.nextDouble() <= maxSizeProbability) {
                return maxSize;
            } else if (random.nextDouble() <= cutoffProbability + maxSizeProbability) {
                return nextInt(random, cutoff + 1, maxSize);
            } else {
                return nextInt(random, minSize, cutoff);
            }
        };
    }

    private static int nextInt(Random random, int minSize, int maxSize) {
        return random.nextInt(maxSize - minSize + 1) + minSize;
    }

    private static ByteBuffer allocateRandom(int size, Random random) {
        var buffer = ByteBuffer.allocate(size);

        for (var i = 0; i < size; i++) buffer.put(i, (byte) random.nextInt());
        return buffer.asReadOnlyBuffer();
    }

    private static final class ShrinkableBuffer implements Shrinkable<ByteBuffer> {
        private final ByteBuffer value;
        private final int minSize;

        private ShrinkableBuffer(ByteBuffer value, int minSize) {
            this.value = value;
            this.minSize = minSize;
        }

        @Override
        public ByteBuffer value() {
            return value;
        }

        @Override
        public Stream<Shrinkable<ByteBuffer>> shrink() {
            return StreamSupport.stream(new Spliterators.AbstractSpliterator<Shrinkable<ByteBuffer>>(3, 0) {
                int size = value.remaining();

                @Override
                public boolean tryAdvance(Consumer<? super Shrinkable<ByteBuffer>> action) {
                    if (size <= minSize) return false;

                    var half = (size / 2) - (minSize / 2);
                    size = half == 0 ? minSize : size - half;

                    var slice = value.duplicate();
                    slice.limit(size);
                    action.accept(new ShrinkableBuffer(slice.slice(), minSize));
                    return true;
                }
            }, false);
        }

        @Override
        public ShrinkingDistance distance() {
            return ShrinkingDistance.of(value.remaining() - minSize);
        }
    }
}
