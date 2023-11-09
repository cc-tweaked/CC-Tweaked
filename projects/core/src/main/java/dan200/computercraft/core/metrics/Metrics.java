// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.metrics;

/**
 * Built-in metrics that CC produces.
 */
public final class Metrics {
    private Metrics() {
    }

    public static final Metric.Event COMPUTER_TASKS = new Metric.Event("computer_tasks", "ns", Metric::formatTime);
    public static final Metric.Event SERVER_TASKS = new Metric.Event("server_tasks", "ns", Metric::formatTime);

    public static final Metric.Event JAVA_ALLOCATION = new Metric.Event("java_allocation", "bytes", Metric::formatBytes);

    public static final Metric.Event PERIPHERAL_OPS = new Metric.Event("peripheral", "ns", Metric::formatTime);
    public static final Metric.Event FS_OPS = new Metric.Event("fs", "ns", Metric::formatTime);

    public static final Metric.Counter HTTP_REQUESTS = new Metric.Counter("http_requests");
    public static final Metric.Event HTTP_UPLOAD = new Metric.Event("http_upload", "bytes", Metric::formatBytes);
    public static final Metric.Event HTTP_DOWNLOAD = new Metric.Event("http_download", "bytes", Metric::formatBytes);

    public static final Metric.Event WEBSOCKET_INCOMING = new Metric.Event("websocket_incoming", "bytes", Metric::formatBytes);
    public static final Metric.Event WEBSOCKET_OUTGOING = new Metric.Event("websocket_outgoing", "bytes", Metric::formatBytes);

    public static final Metric.Counter TURTLE_OPS = new Metric.Counter("turtle_ops");

    /**
     * Ensures metrics are registered.
     */
    public static void init() {
    }
}
