// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.request;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;

/**
 * Helper class to make {@link AbstractTrafficShapingHandler#isHandlerActive(ChannelHandlerContext)} accessible to
 * {@link HttpRequestHandler}.
 */
abstract class TrafficShapingHandlerAccess extends AbstractTrafficShapingHandler {
    public static boolean isHandlerActive(ChannelHandlerContext ctx) {
        return AbstractTrafficShapingHandler.isHandlerActive(ctx);
    }
}
