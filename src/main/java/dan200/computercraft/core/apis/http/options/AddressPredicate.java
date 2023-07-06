/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.apis.http.options;

import com.google.common.net.InetAddresses;
import dan200.computercraft.ComputerCraft;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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

        private static final Set<InetAddress> additionalAddresses = Arrays.stream( new String[] {
            // Block various cloud providers internal IPs.
            "100.100.100.200", // Alibaba
            "192.0.0.192", // Oracle
        } ).map( InetAddresses::forString ).collect( Collectors.toSet() );

        @Override
        public boolean matches( InetAddress socketAddress )
        {
            return socketAddress.isAnyLocalAddress()   // 0.0.0.0, ::0
                || socketAddress.isLoopbackAddress()   // 127.0.0.0/8, ::1
                || socketAddress.isLinkLocalAddress()  // 169.254.0.0/16, fe80::/10
                || socketAddress.isSiteLocalAddress()  // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, fec0::/10
                || socketAddress.isMulticastAddress()  // 224.0.0.0/4, ff00::/8
                || isUniqueLocalAddress( socketAddress ) // fd00::/8
                || additionalAddresses.contains( socketAddress );
        }

        /**
         * Determine if an IP address lives inside the ULA address range.
         *
         * @param address The IP address to test.
         * @return Whether this address sits in the ULA address range.
         * @see <a href="https://en.wikipedia.org/wiki/Unique_local_address">Unique local address on Wikipedia</a>
         */
        private boolean isUniqueLocalAddress( InetAddress address )
        {
            // ULA is actually defined as fc00::/7 (so both fc00::/8 and fd00::/8). However, only the latter is actually
            // defined right now, so let's be conservative.
            return address instanceof Inet6Address && (address.getAddress()[0] & 0xff) == 0xfd;
        }
    }

}
