/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http;

import com.google.common.net.InetAddresses;
import dan200.computercraft.ComputerCraft;

import javax.annotation.Nullable;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.regex.Pattern;

/**
 * A pattern which matches an address, and either allows or blocks it depending
 */
public class AddressRule
{
    private static final class HostRange
    {
        private final byte[] min;
        private final byte[] max;

        private HostRange( byte[] min, byte[] max )
        {
            this.min = min;
            this.max = max;
        }

        public boolean contains( InetAddress address )
        {
            byte[] entry = address.getAddress();
            if( entry.length != min.length ) return false;

            for( int i = 0; i < entry.length; i++ )
            {
                int value = 0xFF & entry[i];
                if( value < (0xFF & min[i]) || value > (0xFF & max[i]) ) return false;
            }

            return true;
        }
    }

    public enum Action
    {
        ALLOW,
        BLOCK
    }

    private final HostRange ip;
    private final Pattern domainPattern;
    private final Action action;

    private AddressRule( HostRange ip, Pattern domainPattern, Action action )
    {
        this.ip = ip;
        this.domainPattern = domainPattern;
        this.action = action;
    }

    @Nullable
    public static AddressRule parse( String filter, Action action )
    {
        int cidr = filter.indexOf( '/' );
        if( cidr >= 0 )
        {
            String addressStr = filter.substring( 0, cidr );
            String prefixSizeStr = filter.substring( cidr + 1 );

            int prefixSize;
            try
            {
                prefixSize = Integer.parseInt( prefixSizeStr );
            }
            catch( NumberFormatException e )
            {
                ComputerCraft.log.error(
                    "Malformed http whitelist/blacklist entry '{}': Cannot extract size of CIDR mask from '{}'.",
                    filter, prefixSizeStr
                );
                return null;
            }

            InetAddress address;
            try
            {
                address = InetAddresses.forString( addressStr );
            }
            catch( IllegalArgumentException e )
            {
                ComputerCraft.log.error(
                    "Malformed http whitelist/blacklist entry '{}': Cannot extract IP address from '{}'.",
                    filter, prefixSizeStr
                );
                return null;
            }

            // Mask the bytes of the IP address.
            byte[] minBytes = address.getAddress(), maxBytes = address.getAddress();
            int size = prefixSize;
            for( int i = 0; i < minBytes.length; i++ )
            {
                if( size <= 0 )
                {
                    minBytes[i] &= 0;
                    maxBytes[i] |= 0xFF;
                }
                else if( size < 8 )
                {
                    minBytes[i] &= 0xFF << (8 - size);
                    maxBytes[i] |= ~(0xFF << (8 - size));
                }

                size -= 8;
            }

            return new AddressRule( new HostRange( minBytes, maxBytes ), null, action );
        }
        else
        {
            Pattern pattern = Pattern.compile( "^\\Q" + filter.replaceAll( "\\*", "\\\\E.*\\\\Q" ) + "\\E$" );
            return new AddressRule( null, pattern, action );
        }
    }

    /**
     * Determine whether the given address matches a series of patterns.
     *
     * @param domain  The domain to match
     * @param address The address to check.
     * @return Whether it matches any of these patterns.
     */
    public boolean matches( String domain, InetAddress address )
    {
        if( domainPattern != null )
        {
            if( domainPattern.matcher( domain ).matches() ) return true;
            if( domainPattern.matcher( address.getHostName() ).matches() ) return true;
        }

        // Match the normal address
        if( matchesAddress( address ) ) return true;

        // If we're an IPv4 address in disguise then let's check that.
        return address instanceof Inet6Address && InetAddresses.is6to4Address( (Inet6Address) address )
            && matchesAddress( InetAddresses.get6to4IPv4Address( (Inet6Address) address ) );
    }

    private boolean matchesAddress( InetAddress address )
    {
        if( domainPattern != null && domainPattern.matcher( address.getHostAddress() ).matches() ) return true;
        return ip != null && ip.contains( address );
    }

    public static Action apply( Iterable<? extends AddressRule> rules, String domain, InetAddress address )
    {
        for( AddressRule rule : rules )
        {
            if( rule.matches( domain, address ) ) return rule.action;
        }

        return Action.BLOCK;
    }
}
