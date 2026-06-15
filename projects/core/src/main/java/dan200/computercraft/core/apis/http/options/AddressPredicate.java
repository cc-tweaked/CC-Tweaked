// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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

        private HostRange(byte[] min, byte[] max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public boolean matches(InetAddress address) {
            return matches(address.getAddress());
        }

        private boolean matches(byte[] address) {
            return address.length == min.length && Arrays.compareUnsigned(min, address) <= 0 && Arrays.compareUnsigned(address, max) <= 0;
        }

        private static HostRange parse(String cidr) {
            var idx = cidr.lastIndexOf('/');
            if (idx < 0) throw new InvalidRuleException(String.format("Invalid host '%s', not in CIDR notation", cidr));
            return HostRange.parse(cidr.substring(0, idx), cidr.substring(idx + 1));
        }

        static HostRange parse(String addressStr, String prefixSizeStr) {
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

    /**
     * Matches any private/reserved IP address.
     *
     * @see <a href="https://datatracker.ietf.org/doc/html/rfc6890">RFC 6890</a>
     * @see <a href="https://www.iana.org/assignments/iana-ipv4-special-registry/iana-ipv4-special-registry.xhtml">IPv4 Special-Purpose Address Space</a>
     * @see <a href="https://www.iana.org/assignments/iana-ipv6-special-registry/iana-ipv6-special-registry.xhtml">IPv6 Special-Purpose Address Space</a>
     */
    final class PrivatePattern implements AddressPredicate {
        static final PrivatePattern INSTANCE = new PrivatePattern();

        private PrivatePattern() {
        }

        @Override
        public boolean matches(InetAddress socketAddress) {
            return socketAddress.isAnyLocalAddress()   // 0.0.0.0, ::0
                || socketAddress.isLoopbackAddress()   // 127.0.0.0/8, ::1
                || socketAddress.isLinkLocalAddress()  // 169.254.0.0/16, fe80::/10
                || socketAddress.isSiteLocalAddress()  // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, fec0::/10
                || socketAddress.isMulticastAddress()  // 224.0.0.0/4, ff00::/8
                || isAnyAdditional(socketAddress);
        }

        /**
         * Additional address ranges reserved by IANA.
         */
        private static final List<HostRange> ADDITIONAL_RANGES = Stream.of(
            // Shared Address Space ([RFC 6598](https://datatracker.ietf.org/doc/html/rfc6598)), used for
            // [Carrier-grade NAT](https://en.wikipedia.org/wiki/Carrier-grade_NAT).
            "100.64.0.0/10",
            // IETF Protocol Assignments.
            "192.0.0.0/24",
            // TEST-NET-1 ([RFC 5737](https://datatracker.ietf.org/doc/html/rfc5737)).
            "192.0.2.0/24",
            // 6to4 Relay Anycast ([RFC 3068](https://datatracker.ietf.org/doc/html/rfc3068)).
            "192.88.99.0/24",
            // Network Interconnect Device Benchmark Testing
            // ([RFC 2544](https://datatracker.ietf.org/doc/html/rfc2544)).
            "198.18.0.0/15",
            // TEST-NET-2 ([RFC 5737](https://datatracker.ietf.org/doc/html/rfc5737)).
            "198.51.100.0/24",
            // TEST-NET-3 ([RFC 5737](https://datatracker.ietf.org/doc/html/rfc5737)).
            "203.0.113.0/24",
            // Reserved ([RFC 1112](https://datatracker.ietf.org/doc/html/rfc1112#section-4)).
            "192.0.2.0/24",

            // IPv4-IPV6 Translation Address ([RFC 6052](https://datatracker.ietf.org/doc/html/rfc6052)).
            // See also [NAT64 on Wikipedia](https://en.wikipedia.org/wiki/NAT64).
            "64:ff9b::/96",
            // The Local-Use IPv4/IPv6 Translation Prefix ([RFC 8215](https://datatracker.ietf.org/doc/html/rfc8215)).
            "64:ff9b:1::/48",
            // IETF Protocol Assignments ([RFC 2928](https://datatracker.ietf.org/doc/html/rfc2928)).
            // This includes various sub-allocations including TEREDO and ORCHID.
            "2001::/23",
            // Unique Local address ([RFC 4193](https://datatracker.ietf.org/doc/html/rfc4193)). See also
            // [Wikipedia](https://en.wikipedia.org/wiki/Unique_local_address).
            "fc00::/7"
        ).map(HostRange::parse).toList();

        private static boolean isAnyAdditional(InetAddress address) {
            var addressBytes = address.getAddress();
            return ADDITIONAL_RANGES.stream().anyMatch(x -> x.matches(addressBytes));
        }
    }
}
