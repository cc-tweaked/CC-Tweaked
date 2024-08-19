// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

import com.google.common.net.InetAddresses;

import java.net.Inet4Address;
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
interface AddressPredicate {
    default boolean matches(String domain) {
        return false;
    }

    default boolean matches(InetAddress socketAddress) {
        return false;
    }

    final class HostRange implements AddressPredicate {
        private final byte[] min;
        private final byte[] max;

        HostRange(byte[] min, byte[] max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean matches(InetAddress address) {
            var entry = address.getAddress();
            if (entry.length != min.length) return false;

            for (var i = 0; i < entry.length; i++) {
                var value = 0xFF & entry[i];
                if (value < (0xFF & min[i]) || value > (0xFF & max[i])) return false;
            }

            return true;
        }

        public static HostRange parse(String addressStr, String prefixSizeStr) {
            int prefixSize;
            try {
                prefixSize = Integer.parseInt(prefixSizeStr);
            } catch (NumberFormatException e) {
                throw new InvalidRuleException(String.format(
                    "Invalid host '%s': Cannot extract size of CIDR mask from '%s'.",
                    addressStr + '/' + prefixSizeStr, prefixSizeStr
                ));
            }

            InetAddress address;
            try {
                address = InetAddresses.forString(addressStr);
            } catch (IllegalArgumentException e) {
                throw new InvalidRuleException(String.format(
                    "Invalid host '%s': Cannot extract IP address from '%s'.",
                    addressStr + '/' + prefixSizeStr, addressStr
                ));
            }

            // Mask the bytes of the IP address.
            byte[] minBytes = address.getAddress(), maxBytes = address.getAddress();
            var size = prefixSize;
            for (var i = 0; i < minBytes.length; i++) {
                if (size <= 0) {
                    minBytes[i] = (byte) 0;
                    maxBytes[i] = (byte) 0xFF;
                } else if (size < 8) {
                    minBytes[i] = (byte) (minBytes[i] & 0xFF << (8 - size));
                    maxBytes[i] = (byte) (maxBytes[i] | ~(0xFF << (8 - size)));
                }

                size -= 8;
            }

            return new HostRange(minBytes, maxBytes);
        }
    }

    final class DomainPattern implements AddressPredicate {
        private final Pattern pattern;

        DomainPattern(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(String domain) {
            return pattern.matcher(domain).matches();
        }

        @Override
        public boolean matches(InetAddress socketAddress) {
            return pattern.matcher(socketAddress.getHostAddress()).matches();
        }
    }

    final class PrivatePattern implements AddressPredicate {
        static final PrivatePattern INSTANCE = new PrivatePattern();

        private static final Set<InetAddress> additionalAddresses = Arrays.stream(new String[]{
            // Block various cloud providers internal IPs.
            "192.0.0.192", // Oracle
        }).map(InetAddresses::forString).collect(Collectors.toUnmodifiableSet());

        @Override
        public boolean matches(InetAddress socketAddress) {
            return socketAddress.isAnyLocalAddress()   // 0.0.0.0, ::0
                || socketAddress.isLoopbackAddress()   // 127.0.0.0/8, ::1
                || socketAddress.isLinkLocalAddress()  // 169.254.0.0/16, fe80::/10
                || socketAddress.isSiteLocalAddress()  // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, fec0::/10
                || socketAddress.isMulticastAddress()  // 224.0.0.0/4, ff00::/8
                || isUniqueLocalAddress(socketAddress) // fd00::/8
                || isCarrierGradeNatAddress(socketAddress) // 100.64.0.0/10
                || additionalAddresses.contains(socketAddress);
        }

        /**
         * Determine if an IP address lives inside the ULA address range.
         *
         * @param address The IP address to test.
         * @return Whether this address sits in the ULA address range.
         * @see <a href="https://en.wikipedia.org/wiki/Unique_local_address">Unique local address on Wikipedia</a>
         */
        private boolean isUniqueLocalAddress(InetAddress address) {
            // ULA is actually defined as fc00::/7 (so both fc00::/8 and fd00::/8). However, only the latter is actually
            // defined right now, so let's be conservative.
            return address instanceof Inet6Address && (address.getAddress()[0] & 0xff) == 0xfd;
        }

        /**
         * Determine if an IP address lives within the CGNAT address range (100.64.0.0/10).
         *
         * @param address The IP address to test.
         * @return Whether this address sits in the CGNAT address range.
         * @see <a href="https://en.wikipedia.org/wiki/Carrier-grade_NAT">Carrier-grade NAT on Wikipedia</a>
         */
        private boolean isCarrierGradeNatAddress(InetAddress address) {
            if (!(address instanceof Inet4Address)) return false;
            var bytes = address.getAddress();
            return bytes[0] == 100 && ((bytes[1] & 0xFF) >= 64 && (bytes[1] & 0xFF) <= 127);
        }
    }

}
