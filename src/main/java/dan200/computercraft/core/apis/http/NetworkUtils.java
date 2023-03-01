/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http;

import dan200.computercraft.ComputerCraft;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.shared.util.ThreadUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ConnectTimeoutException;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.handler.traffic.AbstractTrafficShapingHandler;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import javax.annotation.Nonnull;
import javax.net.ssl.*;
import java.io.ByteArrayInputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStoreException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Just a shared object for executing simple HTTP related tasks.
 */
public final class NetworkUtils
{
    public static final ScheduledThreadPoolExecutor EXECUTOR = new ScheduledThreadPoolExecutor(
        4,
        ThreadUtils.builder( "Network" )
            .setPriority( Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2 )
            .build()
    );

    public static final EventLoopGroup LOOP_GROUP = new NioEventLoopGroup( 4, ThreadUtils.builder( "Netty" )
        .setPriority( Thread.MIN_PRIORITY + (Thread.NORM_PRIORITY - Thread.MIN_PRIORITY) / 2 )
        .build()
    );

    public static final AbstractTrafficShapingHandler SHAPING_HANDLER = new GlobalTrafficShapingHandler(
        EXECUTOR, ComputerCraft.httpUploadBandwidth, ComputerCraft.httpDownloadBandwidth
    );

    static
    {
        EXECUTOR.setKeepAliveTime( 60, TimeUnit.SECONDS );
    }

    private NetworkUtils()
    {
    }

    private static final Object sslLock = new Object();
    private static TrustManagerFactory trustManager;
    private static SslContext sslContext;
    private static boolean triedSslContext = false;

    private static final String letsEncryptRootCert = "-----BEGIN CERTIFICATE-----\n" +
        "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\n" +
        "-----END CERTIFICATE-----\n";

    private static TrustManagerFactory getTrustManager()
    {
        if( trustManager != null ) return trustManager;
        synchronized( sslLock )
        {
            if( trustManager != null ) return trustManager;

            TrustManagerFactory tmf = null;
            try
            {
                Certificate ca = CertificateFactory.getInstance( "X.509" )
                    .generateCertificate( new ByteArrayInputStream( letsEncryptRootCert.getBytes() ) );

                KeyStore ks = KeyStore.getInstance( KeyStore.getDefaultType() );
                ks.load( null, null );
                ks.setCertificateEntry( Integer.toString( 1 ), ca );

                TrustManagerFactory additional = TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() );
                additional.init( ks );

                // Get hold of the extension trust manager
                X509TrustManager x509tm = null;
                for ( TrustManager tm : additional.getTrustManagers() )
                {
                    if ( tm instanceof X509TrustManager )
                    {
                        x509tm = (X509TrustManager) tm;
                        break;
                    }
                }

                tmf = new MergedTrustManagerFactory( TrustManagerFactory.getInstance( TrustManagerFactory.getDefaultAlgorithm() ), x509tm );
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

    public static void reloadConfig()
    {
        SHAPING_HANDLER.configure( ComputerCraft.httpUploadBandwidth, ComputerCraft.httpDownloadBandwidth );
    }

    public static void reset()
    {
        SHAPING_HANDLER.trafficCounter().resetCumulativeTime();
    }

    /**
     * Create a {@link InetSocketAddress} from a {@link java.net.URI}.
     * <p>
     * Note, this may require a DNS lookup, and so should not be executed on the main CC thread.
     *
     * @param uri The URI to fetch.
     * @param ssl Whether to connect with SSL. This is used to find the default port if not otherwise specified.
     * @return The resolved address.
     * @throws HTTPRequestException If the host is not malformed.
     */
    public static InetSocketAddress getAddress( URI uri, boolean ssl ) throws HTTPRequestException
    {
        return getAddress( uri.getHost(), uri.getPort(), ssl );
    }

    /**
     * Create a {@link InetSocketAddress} from the resolved {@code host} and port.
     * <p>
     * Note, this may require a DNS lookup, and so should not be executed on the main CC thread.
     *
     * @param host The host to resolve.
     * @param port The port, or -1 if not defined.
     * @param ssl  Whether to connect with SSL. This is used to find the default port if not otherwise specified.
     * @return The resolved address.
     * @throws HTTPRequestException If the host is not malformed.
     */
    public static InetSocketAddress getAddress( String host, int port, boolean ssl ) throws HTTPRequestException
    {
        if( port < 0 ) port = ssl ? 443 : 80;
        InetSocketAddress socketAddress = new InetSocketAddress( host, port );
        if( socketAddress.isUnresolved() ) throw new HTTPRequestException( "Unknown host" );
        return socketAddress;
    }

    /**
     * Get options for a specific domain.
     *
     * @param host    The host to resolve.
     * @param address The address, resolved by {@link #getAddress(String, int, boolean)}.
     * @return The options for this host.
     * @throws HTTPRequestException If the host is not permitted
     */
    public static Options getOptions( String host, InetSocketAddress address ) throws HTTPRequestException
    {
        Options options = AddressRule.apply( ComputerCraft.httpRules, host, address );
        if( options.action == Action.DENY ) throw new HTTPRequestException( "Domain not permitted" );
        return options;
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

    @Nonnull
    public static String toFriendlyError( @Nonnull Throwable cause )
    {
        if( cause instanceof WebSocketHandshakeException || cause instanceof HTTPRequestException )
        {
            return cause.getMessage();
        }
        else if( cause instanceof TooLongFrameException )
        {
            return "Message is too large";
        }
        else if( cause instanceof ReadTimeoutException || cause instanceof ConnectTimeoutException )
        {
            return "Timed out";
        }
        else if( cause instanceof SSLHandshakeException || (cause instanceof DecoderException && cause.getCause() instanceof SSLHandshakeException) )
        {
            return "Could not create a secure connection";
        }
        else
        {
            return "Could not connect";
        }
    }

    private static class MergedX509TrustManager implements X509TrustManager
    {
        private final X509TrustManager base, additional;

        MergedX509TrustManager( X509TrustManager b, X509TrustManager a )
        {
            this.base = b;
            this.additional = a;
        }

        @Override
        public X509Certificate[] getAcceptedIssuers()
        {
            return base.getAcceptedIssuers();
        }

        @Override
        public void checkServerTrusted( X509Certificate[] chain, String authType ) throws CertificateException
        {
            try
            {
                base.checkServerTrusted( chain, authType );
            }
            catch ( CertificateException e )
            {
                additional.checkServerTrusted( chain, authType );
            }
        }

        @Override
        public void checkClientTrusted( X509Certificate[] chain, String authType ) throws CertificateException
        {
            base.checkClientTrusted( chain, authType );
        }
    }

    private static class MergedTrustManagerFactory extends TrustManagerFactory
    {
        private static class Spi extends TrustManagerFactorySpi
        {
            private final TrustManagerFactory base;
            private final X509TrustManager additional;

            Spi( TrustManagerFactory b, X509TrustManager a )
            {
                this.base = b;
                this.additional = a;
            }

            @Override
            protected void engineInit( KeyStore keyStore ) throws KeyStoreException
            {
                base.init( keyStore );
            }

            @Override
            protected void engineInit( ManagerFactoryParameters managerFactoryParameters ) throws InvalidAlgorithmParameterException
            {
                base.init( managerFactoryParameters );
            }

            @Override
            protected TrustManager[] engineGetTrustManagers()
            {
                TrustManager[] managers = base.getTrustManagers();
                for ( int i = 0; i < managers.length; i++ )
                {
                    if ( managers[i] instanceof X509TrustManager )
                    {
                        managers[i] = new MergedX509TrustManager( (X509TrustManager) managers[i], additional );
                    }
                }
                return managers;
            }
        }

        MergedTrustManagerFactory( TrustManagerFactory b, X509TrustManager a )
        {
            super( new Spi( b, a ), b.getProvider(), b.getAlgorithm() );
        }
    }
}
