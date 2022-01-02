/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http.options;

import com.google.common.net.InetAddresses;
import dan200.computercraft.ComputerCraft;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;

/**
 * A predicate on an address. Matches against a domain and an ip address.
 *
 * @see AddressRule#apply(Iterable, String, InetSocketAddress) for the actual handling of this rule.
 */
interface AddressPredicate
{
    default boolean matches( String domain )
    {
        return false;
    }

    default boolean matches( InetAddress socketAddress )
    {
        return false;
    }

    final class HostRange implements AddressPredicate
    {
        private final byte[] min;
        private final byte[] max;

        HostRange( byte[] min, byte[] max )
        {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean matches( InetAddress address )
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

        public static HostRange parse( String addressStr, String prefixSizeStr )
        {
            int prefixSize;
            try
            {
                prefixSize = Integer.parseInt( prefixSizeStr );
            }
            catch( NumberFormatException e )
            {
                ComputerCraft.log.error(
                    "Malformed http whitelist/blacklist entry '{}': Cannot extract size of CIDR mask from '{}'.",
                    addressStr + '/' + prefixSizeStr, prefixSizeStr
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
                    addressStr + '/' + prefixSizeStr, prefixSizeStr
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

            return new HostRange( minBytes, maxBytes );
        }
    }

    final class DomainPattern implements AddressPredicate
    {
        private final Pattern pattern;

        DomainPattern( Pattern pattern )
        {
            this.pattern = pattern;
        }

        @Override
        public boolean matches( String domain )
        {
            return pattern.matcher( domain ).matches();
        }

        @Override
        public boolean matches( InetAddress socketAddress )
        {
            return pattern.matcher( socketAddress.getHostAddress() ).matches();
        }
    }


    final class PrivatePattern implements AddressPredicate
    {
        static final PrivatePattern INSTANCE = new PrivatePattern();

        @Override
        public boolean matches( InetAddress socketAddress )
        {
            return socketAddress.isAnyLocalAddress()
                || socketAddress.isLoopbackAddress()
                || socketAddress.isLinkLocalAddress()
                || socketAddress.isSiteLocalAddress();
        }
    }

}
