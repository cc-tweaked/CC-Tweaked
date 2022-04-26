/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http.options;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dan200.computercraft.ComputerCraft;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses, checks and generates {@link Config}s for {@link AddressRule}.
 */
public class AddressRuleConfig
{
    public static UnmodifiableConfig makeRule( String host, Action action )
    {
        CommentedConfig config = InMemoryCommentedFormat.defaultInstance().createConfig( ConcurrentHashMap::new );
        config.add( "host", host );
        config.add( "action", action.name().toLowerCase( Locale.ROOT ) );

        if( host.equals( "*" ) && action == Action.ALLOW )
        {
            config.setComment( "timeout", "The period of time (in milliseconds) to wait before a HTTP request times out. Set to 0 for unlimited." );
            config.add( "timeout", AddressRule.TIMEOUT );

            config.setComment( "max_download", "The maximum size (in bytes) that a computer can download in a single request. Note that responses may receive more data than allowed, but this data will not be returned to the client." );
            config.set( "max_download", AddressRule.MAX_DOWNLOAD );

            config.setComment( "max_upload", "The maximum size (in bytes) that a computer can upload in a single request. This includes headers and POST text." );
            config.set( "max_upload", AddressRule.MAX_UPLOAD );

            config.setComment( "max_websocket_message", "The maximum size (in bytes) that a computer can send or receive in one websocket packet." );
            config.set( "max_websocket_message", AddressRule.WEBSOCKET_MESSAGE );
        }

        return config;
    }

    public static boolean checkRule( UnmodifiableConfig builder )
    {
        String hostObj = get( builder, "host", String.class ).orElse( null );
        Integer port = get( builder, "port", Number.class ).map( Number::intValue ).orElse( null );
        return hostObj != null && checkEnum( builder, "action", Action.class )
            && check( builder, "port", Number.class )
            && check( builder, "timeout", Number.class )
            && check( builder, "max_upload", Number.class )
            && check( builder, "max_download", Number.class )
            && check( builder, "websocket_message", Number.class )
            && AddressRule.parse( hostObj, port, PartialOptions.DEFAULT ) != null;
    }

    @Nullable
    public static AddressRule parseRule( UnmodifiableConfig builder )
    {
        String hostObj = get( builder, "host", String.class ).orElse( null );
        if( hostObj == null ) return null;

        Action action = getEnum( builder, "action", Action.class ).orElse( null );
        Integer port = get( builder, "port", Number.class ).map( Number::intValue ).orElse( null );
        Integer timeout = get( builder, "timeout", Number.class ).map( Number::intValue ).orElse( null );
        Long maxUpload = get( builder, "max_upload", Number.class ).map( Number::longValue ).orElse( null );
        Long maxDownload = get( builder, "max_download", Number.class ).map( Number::longValue ).orElse( null );
        Integer websocketMessage = get( builder, "websocket_message", Number.class ).map( Number::intValue ).orElse( null );

        PartialOptions options = new PartialOptions(
            action,
            maxUpload,
            maxDownload,
            timeout,
            websocketMessage
        );

        return AddressRule.parse( hostObj, port, options );
    }

    private static <T> boolean check( UnmodifiableConfig config, String field, Class<T> klass )
    {
        Object value = config.get( field );
        if( value == null || klass.isInstance( value ) ) return true;

        ComputerCraft.log.warn( "HTTP rule's {} is not a {}.", field, klass.getSimpleName() );
        return false;
    }

    private static <T extends Enum<T>> boolean checkEnum( UnmodifiableConfig config, String field, Class<T> klass )
    {
        Object value = config.get( field );
        if( value == null ) return true;

        if( !(value instanceof String) )
        {
            ComputerCraft.log.warn( "HTTP rule's {} is not a string", field );
            return false;
        }

        if( parseEnum( klass, (String) value ) == null )
        {
            ComputerCraft.log.warn( "HTTP rule's {} is not a known option", field );
            return false;
        }

        return true;
    }

    private static <T> Optional<T> get( UnmodifiableConfig config, String field, Class<T> klass )
    {
        Object value = config.get( field );
        return klass.isInstance( value ) ? Optional.of( klass.cast( value ) ) : Optional.empty();
    }

    private static <T extends Enum<T>> Optional<T> getEnum( UnmodifiableConfig config, String field, Class<T> klass )
    {
        return get( config, field, String.class ).map( x -> parseEnum( klass, x ) );
    }

    @Nullable
    private static <T extends Enum<T>> T parseEnum( Class<T> klass, String x )
    {
        for( T value : klass.getEnumConstants() )
        {
            if( value.name().equalsIgnoreCase( x ) ) return value;
        }
        return null;
    }
}
