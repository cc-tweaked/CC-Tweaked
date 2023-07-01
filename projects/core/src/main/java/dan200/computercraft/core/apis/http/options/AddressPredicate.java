// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.regex.Pattern;

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
                    "Invalid host host '%s': Cannot extract size of CIDR mask from '%s'.",
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

        @Override
        public boolean matches(InetAddress socketAddress) {
            return socketAddress.isAnyLocalAddress()
                || socketAddress.isLoopbackAddress()
                || socketAddress.isLinkLocalAddress()
                || socketAddress.isSiteLocalAddress();
        }
    }

}
