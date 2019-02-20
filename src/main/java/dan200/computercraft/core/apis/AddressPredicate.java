/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.core.apis;

import com.google.common.net.InetAddresses;
import dan200.computercraft.ComputerCraft;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Used to determine whether a domain or IP address matches a series of patterns.
 */
public class AddressPredicate
{
    private static class HostRange
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

    private final List<Pattern> wildcards;
    private final List<HostRange> ranges;

    public AddressPredicate( String... filters )
    {
        this( Arrays.asList( filters ) );
    }

    public AddressPredicate( Iterable<? extends String> filters )
    {
        List<Pattern> wildcards = this.wildcards = new ArrayList<>();
        List<HostRange> ranges = this.ranges = new ArrayList<>();

        for( String filter : filters )
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
                    ComputerCraft.log.warn( "Cannot parse CIDR size from {} ({})", filter, prefixSizeStr );
                    continue;
                }

                InetAddress address;
                try
                {
                    address = InetAddresses.forString( addressStr );
                }
                catch( IllegalArgumentException e )
                {
                    ComputerCraft.log.warn( "Cannot parse IP address from {} ({})", filter, addressStr );
                    continue;
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

                ranges.add( new HostRange( minBytes, maxBytes ) );
            }
            else
            {
                wildcards.add( Pattern.compile( "^\\Q" + filter.replaceAll( "\\*", "\\\\E.*\\\\Q" ) + "\\E$" ) );
            }
        }
    }

    /**
     * Determine whether a host name matches a series of patterns.
     *
     * This is intended to allow early exiting, before one has to look up the IP address. You should use
     * {@link #matches(InetAddress)} instead of/in addition to this one.
     *
     * @param domain The domain to match.
     * @return Whether the patterns were matched.
     */
    public boolean matches( String domain )
    {
        for( Pattern domainPattern : wildcards )
        {
            if( domainPattern.matcher( domain ).matches() ) return true;
        }

        return false;
    }

    private boolean matchesAddress( InetAddress address )
    {
        String addressString = address.getHostAddress();
        for( Pattern domainPattern : wildcards )
        {
            if( domainPattern.matcher( addressString ).matches() ) return true;
        }

        for( HostRange range : ranges )
        {
            if( range.contains( address ) ) return true;
        }

        return false;
    }

    /**
     * Determine whether the given address matches a series of patterns
     *
     * @param address The address to check.
     * @return Whether it matches any of these patterns.
     */
    public boolean matches( InetAddress address )
    {
        // Match the host name
        String host = address.getHostName();
        if( host != null && matches( host ) ) return true;

        // Match the normal address
        if( matchesAddress( address ) ) return true;

        // If we're an IPv4 address in disguise then let's check that.
        return address instanceof Inet6Address && InetAddresses.is6to4Address( (Inet6Address) address )
            && matchesAddress( InetAddresses.get6to4IPv4Address( (Inet6Address) address ) );

    }
}
