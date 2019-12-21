/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis.http;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.shared.util.ThreadUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;

import javax.net.ssl.SSLException;
import javax.net.ssl.TrustManagerFactory;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Just a shared object for executing simple HTTP related tasks.
 */
public final class NetworkUtils
{
    public static final ExecutorService EXECUTOR = new ThreadPoolExecutor(
        4, Integer.MAX_VALUE,
        60L, TimeUnit.SECONDS,
        new SynchronousQueue<>(),
        ThreadUtils.builder( "Network" )
            .setPriority( Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2 )
            .build()
    );

    public static final EventLoopGroup LOOP_GROUP = new NioEventLoopGroup( 4, ThreadUtils.builder( "Netty" )
        .setPriority( Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2 )
        .build()
    );

    private NetworkUtils()
    {
    }

    private static final Object sslLock = new Object();
    private static TrustManagerFactory trustManager;
    private static SslContext sslContext;
    private static boolean triedSslContext = false;

    private static TrustManagerFactory getTrustManager()
    {
        if( trustManager != null ) return trustManager;
        synchronized( sslLock )
        {
            if( trustManager != null ) return trustManager;

            TrustManagerFactory tmf = null;
            try
            {
                tmf = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
                tmf.init( (KeyStore) null );
            }
            catch( Exception e )
            {
                ComputerCraft.log.error( "Cannot setup trust manager", e );
            }

            return trustManager = tmf;
        }
    }

    public static SslContext getSslContext() throws HTTPRequestException
    {
        if( sslContext != null || triedSslContext ) return sslContext;
        synchronized( sslLock )
        {
            if( sslContext != null || triedSslContext ) return sslContext;
            try
            {
                return sslContext = SslContextBuilder
                    .forClient()
                    .trustManager( getTrustManager() )
                    .build();
            }
            catch( SSLException e )
            {
                ComputerCraft.log.error( "Cannot construct SSL context", e );
                triedSslContext = true;
                sslContext = null;

                throw new HTTPRequestException( "Cannot create a secure connection" );
            }
        }
    }

    /**
     * Checks a host is allowed.
     *
     * @param host The domain to check against
     * @throws HTTPRequestException If the host is not permitted.
     */
    public static void checkHost( String host ) throws HTTPRequestException
    {
        if( !ComputerCraft.http_whitelist.matches( host ) || ComputerCraft.http_blacklist.matches( host ) )
        {
            throw new HTTPRequestException( "Domain not permitted" );
        }
    }

    /**
     * Create a {@link InetSocketAddress} from the resolved {@code host} and port.
     *
     * Note, this may require a DNS lookup, and so should not be executed on the main CC thread.
     *
     * @param host The host to resolve.
     * @param port The port, or -1 if not defined.
     * @param ssl  Whether to connect with SSL. This is used to find the default port if not otherwise specified.
     * @return The resolved address.
     * @throws HTTPRequestException If the host is not permitted.
     */
    public static InetSocketAddress getAddress( String host, int port, boolean ssl ) throws HTTPRequestException
    {
        if( port < 0 ) port = ssl ? 443 : 80;

        InetSocketAddress socketAddress = new InetSocketAddress( host, port );
        if( socketAddress.isUnresolved() ) throw new HTTPRequestException( "Unknown host" );

        InetAddress address = socketAddress.getAddress();
        if( !ComputerCraft.http_whitelist.matches( address ) || ComputerCraft.http_blacklist.matches( address ) )
        {
            throw new HTTPRequestException( "Domain not permitted" );
        }

        return socketAddress;
    }

    /**
     * Read a {@link ByteBuf} into a byte array.
     *
     * @param buffer The buffer to read.
     * @return The resulting bytes.
     */
    public static byte[] toBytes( ByteBuf buffer )
    {
        byte[] bytes = new byte[buffer.readableBytes()];
        buffer.readBytes( bytes );
        return bytes;
    }
}
