// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.network.wired;

import dan200.computercraft.api.network.wired.WiredNode;

import javax.annotation.Nullable;
import java.util.Objects;

/**
 * A disjoint-set/union-find of {@link WiredNodeImpl}s.
 * <p>
 * Rather than actually maintaining a list of included nodes, wired nodes store {@linkplain WiredNodeImpl#currentSet the
 * set they're part of}. This means that we can only have one disjoint-set at once, but that is not a problem in
 * practice.
 *
 * @see WiredNodeImpl#currentSet
 * @see WiredNetworkImpl#remove(WiredNode)
 * @see <a href="https://en.wikipedia.org/wiki/Disjoint-set_data_structure">Disjoint-set data structure</a>
 */
class NodeSet {
    private NodeSet parent = this;
    private int size = 1;
    private @Nullable WiredNetworkImpl network;

    private boolean isRoot() {
        return parent == this;
    }

    /**
     * Resolve this union, finding the root {@link NodeSet}.
     *
     * @return The root union.
     */
    NodeSet find() {
        var self = this;
        while (!self.isRoot()) self = self.parent = self.parent.parent;
        return self;
    }

    /**
     * Get the size of this node set.
     *
     * @return The size of the set.
     */
    int size() {
        return find().size;
    }

    /**
     * Add a node to this {@link NodeSet}.
     *
     * @param node The node to add to the set.
     */
    void addNode(WiredNodeImpl node) {
        if (!isRoot()) throw new IllegalStateException("Cannot grow a non-root set.");
        if (node.currentSet != null) throw new IllegalArgumentException("Node is already in a set.");

        node.currentSet = this;
        size++;
    }

    /**
     * Merge two nodes sets together.
     *
     * @param left  The first union.
     * @param right The second union.
     * @return The union which was subsumed.
     */
    public static NodeSet merge(NodeSet left, NodeSet right) {
        if (!left.isRoot() || !right.isRoot()) throw new IllegalArgumentException("Cannot union a non-root set.");
        if (left == right) throw new IllegalArgumentException("Cannot merge a node into itself.");

        return left.size >= right.size ? mergeInto(left, right) : mergeInto(right, left);
    }

    private static NodeSet mergeInto(NodeSet root, NodeSet child) {
        assert root.size > child.size;
        child.parent = root;
        root.size += child.size;
        return child;
    }

    void setNetwork(WiredNetworkImpl network) {
        if (!isRoot()) throw new IllegalStateException("Set is not the root.");
        if (this.network != null) throw new IllegalStateException("Set already has a network.");
        this.network = network;
    }

    /**
     * Get the associated network.
     *
     * @return The associated network.
     */
    WiredNetworkImpl network() {
        return Objects.requireNonNull(find().network);
    }
}
