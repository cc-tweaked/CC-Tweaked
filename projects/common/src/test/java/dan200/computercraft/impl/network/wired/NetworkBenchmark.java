// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.network.wired;

import dan200.computercraft.api.network.wired.WiredNetwork;
import dan200.computercraft.impl.network.wired.NetworkTest.NetworkElement;
import dan200.computercraft.shared.util.DirectionUtil;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class NetworkBenchmark {
    private static final int BRUTE_SIZE = 16;

    public static void main(String[] args) throws RunnerException {
        var opts = new OptionsBuilder()
            .include(NetworkBenchmark.class.getName() + "\\..*")
            .warmupIterations(2)
            .measurementIterations(5)
            .forks(1)
            .build();
        new Runner(opts).run();
    }

    @Benchmark
    @Warmup(time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(time = 2, timeUnit = TimeUnit.SECONDS)
    public void removeEveryNode(ConnectedGrid grid) {
        grid.grid.forEach((node, pos) -> node.remove());
    }

    @Benchmark
    @Warmup(time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(time = 2, timeUnit = TimeUnit.SECONDS)
    public void connectAndDisconnect(SplitGrid connectedGrid) {
        WiredNodeImpl left = connectedGrid.left, right = connectedGrid.right;

        assertNotEquals(left.getNetwork(), right.getNetwork());
        left.connectTo(right);
        assertEquals(left.getNetwork(), right.getNetwork());
        left.disconnectFrom(right);
        assertNotEquals(left.getNetwork(), right.getNetwork());
    }

    @Benchmark
    @Warmup(time = 1, timeUnit = TimeUnit.SECONDS)
    @Measurement(time = 2, timeUnit = TimeUnit.SECONDS)
    public void connectAndRemove(SplitGrid connectedGrid) {
        WiredNodeImpl left = connectedGrid.left, right = connectedGrid.right, centre = connectedGrid.centre;

        assertNotEquals(left.getNetwork(), right.getNetwork());
        centre.connectTo(left);
        centre.connectTo(right);
        assertEquals(left.getNetwork(), right.getNetwork());
        centre.remove();
        assertNotEquals(left.getNetwork(), right.getNetwork());
    }

    /**
     * Create a grid where all nodes are connected to their neighbours.
     */
    @State(Scope.Thread)
    public static class ConnectedGrid {
        Grid<WiredNodeImpl> grid;

        @Setup(Level.Invocation)
        public void setup() {
            var grid = this.grid = new Grid<>(BRUTE_SIZE);
            grid.map((existing, pos) -> new NetworkElement("n_" + pos, pos.getX() == pos.getY() && pos.getY() == pos.getZ()).getNode());

            // Connect every node
            grid.forEach((node, pos) -> {
                for (var facing : DirectionUtil.FACINGS) {
                    var other = grid.get(pos.relative(facing));
                    if (other != null) node.connectTo(other);
                }
            });

            var networks = countNetworks(grid);
            if (networks.size() != 1) throw new AssertionError("Expected exactly one network.");
        }
    }

    /**
     * Create a grid where the nodes at {@code x < BRUTE_SIZE/2} and {@code x >= BRUTE_SIZE/2} are in separate networks,
     * but otherwise connected to their neighbours.
     */
    @State(Scope.Thread)
    public static class SplitGrid {
        Grid<WiredNodeImpl> grid;
        WiredNodeImpl left, right, centre;

        @Setup
        public void setup() {
            var grid = this.grid = new Grid<>(BRUTE_SIZE);
            grid.map((existing, pos) -> new NetworkElement("n_" + pos, pos.getX() == pos.getY() && pos.getY() == pos.getZ()).getNode());

            // Connect every node
            grid.forEach((node, pos) -> {
                for (var facing : DirectionUtil.FACINGS) {
                    var offset = pos.relative(facing);
                    if (offset.getX() >= BRUTE_SIZE / 2 == pos.getX() >= BRUTE_SIZE / 2) {
                        var other = grid.get(offset);
                        if (other != null) node.connectTo(other);
                    }
                }
            });

            var networks = countNetworks(grid);
            if (networks.size() != 2) throw new AssertionError("Expected exactly two networks.");
            for (var network : networks.object2IntEntrySet()) {
                if (network.getIntValue() != BRUTE_SIZE * BRUTE_SIZE * (BRUTE_SIZE / 2)) {
                    throw new AssertionError("Network is the wrong size");
                }
            }

            left = Objects.requireNonNull(grid.get(new BlockPos(BRUTE_SIZE / 2 - 1, 0, 0)));
            right = Objects.requireNonNull(grid.get(new BlockPos(BRUTE_SIZE / 2, 0, 0)));
            centre = new NetworkElement("c", false).getNode();
        }
    }

    private static Object2IntMap<WiredNetwork> countNetworks(Grid<WiredNodeImpl> grid) {
        Object2IntMap<WiredNetwork> networks = new Object2IntOpenHashMap<>();
        grid.forEach((node, pos) -> networks.put(node.network, networks.getOrDefault(node.network, 0) + 1));
        return networks;
    }

    private static class Grid<T> {
        private final int size;
        private final T[] box;

        @SuppressWarnings("unchecked")
        Grid(int size) {
            this.size = size;
            this.box = (T[]) new Object[size * size * size];
        }

        public T get(BlockPos pos) {
            int x = pos.getX(), y = pos.getY(), z = pos.getZ();

            return x >= 0 && x < size && y >= 0 && y < size && z >= 0 && z < size
                ? box[x * size * size + y * size + z]
                : null;
        }

        public void forEach(BiConsumer<T, BlockPos> transform) {
            for (var x = 0; x < size; x++) {
                for (var y = 0; y < size; y++) {
                    for (var z = 0; z < size; z++) {
                        transform.accept(box[x * size * size + y * size + z], new BlockPos(x, y, z));
                    }
                }
            }
        }

        public void map(BiFunction<T, BlockPos, T> transform) {
            for (var x = 0; x < size; x++) {
                for (var y = 0; y < size; y++) {
                    for (var z = 0; z < size; z++) {
                        box[x * size * size + y * size + z] = transform.apply(box[x * size * size + y * size + z], new BlockPos(x, y, z));
                    }
                }
            }
        }
    }
}
