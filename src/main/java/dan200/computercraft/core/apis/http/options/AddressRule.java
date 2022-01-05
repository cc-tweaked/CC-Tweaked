/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http.options;

import com.google.common.net.InetAddresses;
import dan200.computercraft.core.apis.http.options.AddressPredicate.DomainPattern;
import dan200.computercraft.core.apis.http.options.AddressPredicate.HostRange;
import dan200.computercraft.core.apis.http.options.AddressPredicate.PrivatePattern;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;

/**
 * A pattern which matches an address, and controls whether it is accessible or not.
 */
public final class AddressRule
{
    public static final long MAX_DOWNLOAD = 16 * 1024 * 1024;
    public static final long MAX_UPLOAD = 4 * 1024 * 1024;
    public static final int TIMEOUT = 30_000;
    public static final int WEBSOCKET_MESSAGE = 128 * 1024;

    private final AddressPredicate predicate;
    private final Integer port;
    private final PartialOptions partial;

    private AddressRule( @Nonnull AddressPredicate predicate, @Nullable Integer port, @Nonnull PartialOptions partial )
    {
        this.predicate = predicate;
        this.partial = partial;
        this.port = port;
    }

    @Nullable
    public static AddressRule parse( String filter, @Nullable Integer port, @Nonnull PartialOptions partial )
    {
        int cidr = filter.indexOf( '/' );
        if( cidr >= 0 )
        {
            String addressStr = filter.substring( 0, cidr );
            String prefixSizeStr = filter.substring( cidr + 1 );
            HostRange range = HostRange.parse( addressStr, prefixSizeStr );
            return range == null ? null : new AddressRule( range, port, partial );
        }
        else if( filter.equalsIgnoreCase( "$private" ) )
        {
            return new AddressRule( PrivatePattern.INSTANCE, port, partial );
        }
        else
        {
            Pattern pattern = Pattern.compile( "^\\Q" + filter.replaceAll( "\\*", "\\\\E.*\\\\Q" ) + "\\E$", Pattern.CASE_INSENSITIVE );
            return new AddressRule( new DomainPattern( pattern ), port, partial );
        }
    }

    /**
     * Determine whether the given address matches a series of patterns.
     *
     * @param domain      The domain to match
     * @param port        The port of the address.
     * @param address     The address to check.
     * @param ipv4Address An ipv4 version of the address, if the original was an ipv6 address.
     * @return Whether it matches any of these patterns.
     */
    private boolean matches( String domain, int port, InetAddress address, Inet4Address ipv4Address )
    {
        if( this.port != null && this.port != port ) return false;
        return predicate.matches( domain )
            || predicate.matches( address )
            || (ipv4Address != null && predicate.matches( ipv4Address ));
    }

    public static Options apply( Iterable<? extends AddressRule> rules, String domain, InetSocketAddress socketAddress )
    {
        PartialOptions options = null;
        boolean hasMany = false;

        int port = socketAddress.getPort();
        InetAddress address = socketAddress.getAddress();
        Inet4Address ipv4Address = address instanceof Inet6Address && InetAddresses.is6to4Address( (Inet6Address) address )
            ? InetAddresses.get6to4IPv4Address( (Inet6Address) address ) : null;

        for( AddressRule rule : rules )
        {
            if( !rule.matches( domain, port, address, ipv4Address ) ) continue;

            if( options == null )
            {
                options = rule.partial;
            }
            else
            {

                if( !hasMany )
                {
                    options = options.copy();
                    hasMany = true;
                }

                options.merge( rule.partial );
            }
        }

        return (options == null ? PartialOptions.DEFAULT : options).toOptions();
    }
}
