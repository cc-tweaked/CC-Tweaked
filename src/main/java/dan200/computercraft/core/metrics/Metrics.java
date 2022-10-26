/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.metrics;

/**
 * Built-in metrics that CC produces.
 */
public final class Metrics
{
    private Metrics()
    {
    }

    public static final Metric.Event COMPUTER_TASKS = new Metric.Event( "computer_tasks", "ms", Metric::formatTime );
    public static final Metric.Event SERVER_TASKS = new Metric.Event( "server_tasks", "ms", Metric::formatTime );

    public static final Metric.Counter PERIPHERAL_OPS = new Metric.Counter( "peripheral" );
    public static final Metric.Counter FS_OPS = new Metric.Counter( "fs" );

    public static final Metric.Counter HTTP_REQUESTS = new Metric.Counter( "http_requests" );
    public static final Metric.Event HTTP_UPLOAD = new Metric.Event( "http_upload", "bytes", Metric::formatBytes );
    public static final Metric.Event HTTP_DOWNLOAD = new Metric.Event( "http_download", "bytes", Metric::formatBytes );

    public static final Metric.Event WEBSOCKET_INCOMING = new Metric.Event( "websocket_incoming", "bytes", Metric::formatBytes );
    public static final Metric.Event WEBSOCKET_OUTGOING = new Metric.Event( "websocket_outgoing", "bytes", Metric::formatBytes );

    public static final Metric.Counter COROUTINES_CREATED = new Metric.Counter( "coroutines_created" );
    public static final Metric.Counter COROUTINES_DISPOSED = new Metric.Counter( "coroutines_dead" );

    public static final Metric.Counter TURTLE_OPS = new Metric.Counter( "turtle_ops" );

    /**
     * Ensures metrics are registered.
     */
    public static void init()
    {
    }
}
