// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.network.wired;

import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;

/**
 * Verifies certain elements of a network are well-formed.
 * <p>
 * This adds substantial overhead to network modification, and so is only enabled when assertions are enabled.
 */
final class InvariantChecker {
    private static final Logger LOG = LoggerFactory.getLogger(InvariantChecker.class);

    private InvariantChecker() {
    }

    static void checkNode(WiredNodeImpl node) {
        assert checkNodeImpl(node) : "Node invariants failed. See logs.";
    }

    private static boolean checkNodeImpl(WiredNodeImpl node) {
        var okay = true;

        if (node.currentSet != null) {
            okay = false;
            LOG.error("{}: currentSet was not cleared.", node);
        }

        var network = makeNullable(node.network);
        if (network == null) {
            okay = false;
            LOG.error("{}: Node's network is null.", node);
        } else if (makeNullable(network.nodes) == null || !network.nodes.contains(node)) {
            okay = false;
            LOG.error("{}: Node's network does not contain node.", node);
        }

        for (var neighbour : node.neighbours) {
            if (!neighbour.neighbours.contains(node)) {
                okay = false;
                LOG.error("{}: Neighbour {}'s neighbour set does not contain origianl node.", node, neighbour);
            }
        }

        return okay;
    }

    static void checkNetwork(WiredNetworkImpl network) {
        assert checkNetworkImpl(network) : "Network invariants failed. See logs.";
    }

    private static boolean checkNetworkImpl(WiredNetworkImpl network) {
        var okay = true;
        for (var node : network.nodes) okay &= checkNodeImpl(node);
        return okay;
    }

    @Contract("")
    private static <T> @Nullable T makeNullable(T object) {
        return object;
    }
}
