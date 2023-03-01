// SPDX-FileCopyrightText: 2018 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl.network.wired;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Verifies certain elements of a network are "well formed".
 * <p>
 * This adds substantial overhead to network modification, and so should only be enabled
 * in a development environment.
 */
public final class InvariantChecker {
    private static final Logger LOG = LoggerFactory.getLogger(InvariantChecker.class);
    private static final boolean ENABLED = false;

    private InvariantChecker() {
    }

    public static void checkNode(WiredNodeImpl node) {
        if (!ENABLED) return;

        var network = node.network;
        if (network == null) {
            LOG.error("Node's network is null", new Exception());
            return;
        }

        if (network.nodes == null || !network.nodes.contains(node)) {
            LOG.error("Node's network does not contain node", new Exception());
        }

        for (var neighbour : node.neighbours) {
            if (!neighbour.neighbours.contains(node)) {
                LOG.error("Neighbour is missing node", new Exception());
            }
        }
    }

    public static void checkNetwork(WiredNetworkImpl network) {
        if (!ENABLED) return;

        for (var node : network.nodes) checkNode(node);
    }
}
