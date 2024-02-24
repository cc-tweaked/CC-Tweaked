// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.network.wired;

import dan200.computercraft.api.network.wired.WiredElement;
import dan200.computercraft.api.network.wired.WiredNetwork;
import dan200.computercraft.api.network.wired.WiredNetworkChange;
import dan200.computercraft.api.network.wired.WiredNode;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class NetworkTest {
    @Test
    public void testConnect() {
        NetworkElement
            aE = new NetworkElement("a"),
            bE = new NetworkElement("b"),
            cE = new NetworkElement("c");

        WiredNodeImpl
            aN = aE.getNode(),
            bN = bE.getNode(),
            cN = cE.getNode();

        assertNotEquals(aN.getNetwork(), bN.getNetwork(), "A's and B's network must be different");
        assertNotEquals(aN.getNetwork(), cN.getNetwork(), "A's and C's network must be different");
        assertNotEquals(bN.getNetwork(), cN.getNetwork(), "B's and C's network must be different");

        assertTrue(aN.connectTo(bN), "Must be able to add connection");
        assertFalse(aN.connectTo(bN), "Cannot add connection twice");

        assertEquals(aN.getNetwork(), bN.getNetwork(), "A's and B's network must be equal");
        assertEquals(Set.of(aN, bN), nodes(aN.getNetwork()), "A's network should be A and B");

        assertEquals(Set.of("a", "b"), aE.allPeripherals().keySet(), "A's peripheral set should be A, B");
        assertEquals(Set.of("a", "b"), bE.allPeripherals().keySet(), "B's peripheral set should be A, B");

        aN.connectTo(cN);

        assertEquals(aN.getNetwork(), bN.getNetwork(), "A's and B's network must be equal");
        assertEquals(aN.getNetwork(), cN.getNetwork(), "A's and C's network must be equal");
        assertEquals(Set.of(aN, bN, cN), nodes(aN.getNetwork()), "A's network should be A, B and C");

        assertEquals(Set.of(bN, cN), neighbours(aN), "A's neighbour set should be B, C");
        assertEquals(Set.of(aN), neighbours(bN), "B's neighbour set should be A");
        assertEquals(Set.of(aN), neighbours(cN), "C's neighbour set should be A");

        assertEquals(Set.of("a", "b", "c"), aE.allPeripherals().keySet(), "A's peripheral set should be A, B, C");
        assertEquals(Set.of("a", "b", "c"), bE.allPeripherals().keySet(), "B's peripheral set should be A, B, C");
        assertEquals(Set.of("a", "b", "c"), cE.allPeripherals().keySet(), "C's peripheral set should be A, B, C");
    }

    @Test
    public void testDisconnectNoChange() {
        NetworkElement
            aE = new NetworkElement("a"),
            bE = new NetworkElement("b"),
            cE = new NetworkElement("c");

        WiredNodeImpl
            aN = aE.getNode(),
            bN = bE.getNode(),
            cN = cE.getNode();

        aN.connectTo(bN);
        aN.connectTo(cN);
        bN.connectTo(cN);

        aN.disconnectFrom(bN);

        assertEquals(aN.getNetwork(), bN.getNetwork(), "A's and B's network must be equal");
        assertEquals(aN.getNetwork(), cN.getNetwork(), "A's and C's network must be equal");
        assertEquals(Set.of(aN, bN, cN), nodes(aN.getNetwork()), "A's network should be A, B and C");

        assertEquals(Set.of("a", "b", "c"), aE.allPeripherals().keySet(), "A's peripheral set should be A, B, C");
        assertEquals(Set.of("a", "b", "c"), bE.allPeripherals().keySet(), "B's peripheral set should be A, B, C");
        assertEquals(Set.of("a", "b", "c"), cE.allPeripherals().keySet(), "C's peripheral set should be A, B, C");
    }

    @Test
    public void testDisconnectLeaf() {
        NetworkElement
            aE = new NetworkElement("a"),
            bE = new NetworkElement("b"),
            cE = new NetworkElement("c");

        WiredNodeImpl
            aN = aE.getNode(),
            bN = bE.getNode(),
            cN = cE.getNode();

        aN.connectTo(bN);
        aN.connectTo(cN);

        aN.disconnectFrom(bN);

        assertNotEquals(aN.getNetwork(), bN.getNetwork(), "A's and B's network must not be equal");
        assertEquals(aN.getNetwork(), cN.getNetwork(), "A's and C's network must be equal");
        assertEquals(Set.of(aN, cN), nodes(aN.getNetwork()), "A's network should be A and C");
        assertEquals(Set.of(bN), nodes(bN.getNetwork()), "B's network should be B");

        assertEquals(Set.of("a", "c"), aE.allPeripherals().keySet(), "A's peripheral set should be A, C");
        assertEquals(Set.of("b"), bE.allPeripherals().keySet(), "B's peripheral set should be B");
        assertEquals(Set.of("a", "c"), cE.allPeripherals().keySet(), "C's peripheral set should be A, C");
    }

    @Test
    public void testDisconnectSplit() {
        NetworkElement
            aE = new NetworkElement("a"),
            aaE = new NetworkElement("a_"),
            bE = new NetworkElement("b"),
            bbE = new NetworkElement("b_");

        WiredNodeImpl
            aN = aE.getNode(),
            aaN = aaE.getNode(),
            bN = bE.getNode(),
            bbN = bbE.getNode();

        aN.connectTo(aaN);
        bN.connectTo(bbN);

        aN.connectTo(bN);

        aN.disconnectFrom(bN);

        assertNotEquals(aN.getNetwork(), bN.getNetwork(), "A's and B's network must not be equal");
        assertEquals(aN.getNetwork(), aaN.getNetwork(), "A's and A_'s network must be equal");
        assertEquals(bN.getNetwork(), bbN.getNetwork(), "B's and B_'s network must be equal");

        assertEquals(Set.of(aN, aaN), nodes(aN.getNetwork()), "A's network should be A and A_");
        assertEquals(Set.of(bN, bbN), nodes(bN.getNetwork()), "B's network should be B and B_");

        assertEquals(Set.of("a", "a_"), aE.allPeripherals().keySet(), "A's peripheral set should be A and A_");
        assertEquals(Set.of("b", "b_"), bE.allPeripherals().keySet(), "B's peripheral set should be B and B_");
    }

    @Test
    public void testRemoveSingle() {
        var aE = new NetworkElement("a");
        var aN = aE.getNode();

        var network = aN.getNetwork();
        assertFalse(aN.remove(), "Cannot remove node from an empty network");
        assertEquals(network, aN.getNetwork(), "Networks are same before and after");
    }

    @Test
    public void testRemoveLeaf() {
        NetworkElement
            aE = new NetworkElement("a"),
            bE = new NetworkElement("b"),
            cE = new NetworkElement("c");

        WiredNodeImpl
            aN = aE.getNode(),
            bN = bE.getNode(),
            cN = cE.getNode();

        aN.connectTo(bN);
        aN.connectTo(cN);

        assertTrue(bN.remove(), "Must be able to remove node");
        assertFalse(bN.remove(), "Cannot remove a second time");

        assertNotEquals(aN.getNetwork(), bN.getNetwork(), "A's and B's network must not be equal");
        assertEquals(aN.getNetwork(), cN.getNetwork(), "A's and C's network must be equal");

        assertEquals(Set.of(aN, cN), nodes(aN.getNetwork()), "A's network should be A and C");
        assertEquals(Set.of(bN), nodes(bN.getNetwork()), "B's network should be B");

        assertEquals(Set.of("a", "c"), aE.allPeripherals().keySet(), "A's peripheral set should be A, C");
        assertEquals(Set.of(), bE.allPeripherals().keySet(), "B's peripheral set should be empty");
        assertEquals(Set.of("a", "c"), cE.allPeripherals().keySet(), "C's peripheral set should be A, C");
    }

    @Test
    public void testRemoveSplit() {
        NetworkElement
            aE = new NetworkElement("a"),
            aaE = new NetworkElement("a_"),
            bE = new NetworkElement("b"),
            bbE = new NetworkElement("b_"),
            cE = new NetworkElement("c");

        WiredNodeImpl
            aN = aE.getNode(),
            aaN = aaE.getNode(),
            bN = bE.getNode(),
            bbN = bbE.getNode(),
            cN = cE.getNode();

        aN.connectTo(aaN);
        bN.connectTo(bbN);

        cN.connectTo(aN);
        cN.connectTo(bN);

        cN.remove();

        assertNotEquals(aN.getNetwork(), bN.getNetwork(), "A's and B's network must not be equal");
        assertEquals(aN.getNetwork(), aaN.getNetwork(), "A's and A_'s network must be equal");
        assertEquals(bN.getNetwork(), bbN.getNetwork(), "B's and B_'s network must be equal");

        assertEquals(Set.of(aN, aaN), nodes(aN.getNetwork()), "A's network should be A and A_");
        assertEquals(Set.of(bN, bbN), nodes(bN.getNetwork()), "B's network should be B and B_");
        assertEquals(Set.of(cN), nodes(cN.getNetwork()), "C's network should be C");

        assertEquals(Set.of("a", "a_"), aE.allPeripherals().keySet(), "A's peripheral set should be A and A_");
        assertEquals(Set.of("b", "b_"), bE.allPeripherals().keySet(), "B's peripheral set should be B and B_");
        assertEquals(Set.of(), cE.allPeripherals().keySet(), "C's peripheral set should be empty");
    }

    static final class NetworkElement implements WiredElement {
        private final String id;
        private final WiredNodeImpl node;
        private final Map<String, IPeripheral> localPeripherals = new HashMap<>();
        private final Map<String, IPeripheral> remotePeripherals = new HashMap<>();

        NetworkElement(String id) {
            this(id, true);
        }

        NetworkElement(String id, boolean peripheral) {
            this.id = id;
            this.node = new WiredNodeImpl(this);
            if (peripheral) addPeripheral(id);
        }

        @Override
        public Level getLevel() {
            throw new IllegalStateException("Unexpected call to getLevel()");
        }

        @Override
        public Vec3 getPosition() {
            throw new IllegalStateException("Unexpected call to getPosition()");
        }

        @Override
        public String getSenderID() {
            return id;
        }

        @Override
        public String toString() {
            return "NetworkElement{" + id + "}";
        }

        @Override
        public WiredNodeImpl getNode() {
            return node;
        }

        @Override
        public void networkChanged(WiredNetworkChange change) {
            remotePeripherals.keySet().removeAll(change.peripheralsRemoved().keySet());
            remotePeripherals.putAll(change.peripheralsAdded());
        }

        public NetworkElement addPeripheral(String name) {
            localPeripherals.put(name, new NetworkPeripheral());
            getNode().updatePeripherals(localPeripherals);
            return this;
        }

        public Map<String, IPeripheral> allPeripherals() {
            return remotePeripherals;
        }
    }

    private static final class NetworkPeripheral implements IPeripheral {
        @Override
        public String getType() {
            return "test";
        }

        @Override
        public boolean equals(@Nullable IPeripheral other) {
            return this == other;
        }
    }

    private static Set<WiredNodeImpl> nodes(WiredNetwork network) {
        return ((WiredNetworkImpl) network).nodes;
    }

    private static Set<WiredNodeImpl> neighbours(WiredNode node) {
        return ((WiredNodeImpl) node).neighbours;
    }
}
