package org.squiddev.cc_prometheus;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.tracking.ComputerTracker;
import dan200.computercraft.core.tracking.Tracking;
import dan200.computercraft.core.tracking.TrackingContext;
import dan200.computercraft.core.tracking.TrackingField;
import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;
import io.prometheus.client.exporter.common.TextFormat;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

public class PrometheusController extends AbstractHandler
{
    private static final UUID ID = UUID.fromString( "5a4bac41-d081-4584-87e1-f8af7b6aaf90" );
    private static final String NAMESPACE = ComputerCraft.MOD_ID;

    private static Server server;

    public static void startServer()
    {
        stopServer();

        server = new Server( 1234 );
        server.setHandler( new PrometheusController() );

        try
        {
            server.start();
        }
        catch( Exception e )
        {
            ComputerCraft.log.error( "Cannot start PrometheusController server", e );
        }
    }

    public static void stopServer()
    {
        if( server == null ) return;

        try
        {
            server.stop();
        }
        catch( Exception e )
        {
            ComputerCraft.log.error( "Cannot stop PrometheusController server", e );
        }
        finally
        {
            server = null;
        }
    }

    private final CollectorRegistry registry = new CollectorRegistry();

//    private final Gauge totalComputers = Gauge.build()
//        .namespace( NAMESPACE )
//        .name( "computers_total" )
//        .help( "Total number of loaded computers" )
//        .register( registry );
//
//    private final Gauge onComputers = Gauge.build()
//        .name( NAMESPACE )
//        .name( "computers_on" )
//        .help( "Total number of computers which are running" )
//        .register( registry );

    private final TrackingField[] fields;
    private final Gauge[] gauges;

    private final TrackingContext context = Tracking.getContext( ID );

    public PrometheusController()
    {
        fields = TrackingField.fields().keySet().toArray( new TrackingField[TrackingField.fields().size()] );
        gauges = new Gauge[fields.length];

        for( int i = 0; i < fields.length; i++ )
        {
            gauges[i] = Gauge.build()
                .namespace( NAMESPACE )
                .name( fields[i].id() )
                .help( fields[i].displayName() )
                .register( registry );
        }
    }
    
    @Override
    public void handle( String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException
    {
        if( !target.equals( "/metrics" ) )
        {
            response.sendError( HttpServletResponse.SC_NOT_FOUND );
            return;
        }

        for( ComputerTracker context : context.getTimings() )
        {
            for( int i = 0; i < fields.length; i++ )
            {
                gauges[i].labels( "computer_" + context.getComputerId() ).set( context.get( fields[i] ) );
            }
        }
        
        try
        {
            response.setStatus( HttpServletResponse.SC_OK );
            response.setContentType( TextFormat.CONTENT_TYPE_004 );

            TextFormat.write004( response.getWriter(), CollectorRegistry.defaultRegistry.metricFamilySamples() );

            baseRequest.setHandled( true );
        }
        catch( IOException e )
        {
            ComputerCraft.log.error( "Failed to read PrometheusController statistics", e );
            response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
        }
    }
}
