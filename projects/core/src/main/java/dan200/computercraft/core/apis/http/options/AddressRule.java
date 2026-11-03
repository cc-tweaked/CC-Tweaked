// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

import com.google.common.net.InetAddresses;
import dan200.computercraft.core.apis.http.options.AddressPredicate.DomainPattern;
import dan200.computercraft.core.apis.http.options.AddressPredicate.HostRange;
import dan200.computercraft.core.apis.http.options.AddressPredicate.PrivatePattern;
import org.jspecify.annotations.Nullable;

import java.net.*;
import java.util.Arrays;
import java.util.OptionalInt;
import java.util.regex.Pattern;

/**
 * A pattern which matches an address, and controls whether it is accessible or not.
 */
public final class AddressRule {
    public static final long MAX_DOWNLOAD = 16 * 1024 * 1024;
    public static final long MAX_UPLOAD = 4 * 1024 * 1024;
    public static final int WEBSOCKET_MESSAGE = 128 * 1024;

    private final AddressPredicate predicate;
    private final OptionalInt port;
    private final PartialOptions partial;

    private AddressRule(AddressPredicate predicate, OptionalInt port, PartialOptions partial) {
        this.predicate = predicate;
        this.partial = partial;
        this.port = port;
    }

    public static AddressRule parse(String filter, OptionalInt port, PartialOptions partial) {
        var cidr = filter.lastIndexOf('/');
        if (cidr >= 0) {
            var addressStr = filter.substring(0, cidr);
            var prefixSizeStr = filter.substring(cidr + 1);
            var range = HostRange.parse(addressStr, prefixSizeStr);
            return new AddressRule(range, port, partial);
        } else if (filter.equalsIgnoreCase("$private")) {
            return new AddressRule(PrivatePattern.INSTANCE, port, partial);
        } else {
            var pattern = Pattern.compile("^\\Q" + filter.replaceAll("\\*", "\\\\E.*\\\\Q") + "\\E$", Pattern.CASE_INSENSITIVE);
            return new AddressRule(new DomainPattern(pattern), port, partial);
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
    private boolean matches(String domain, int port, InetAddress address, @Nullable Inet4Address ipv4Address) {
        if (this.port.isPresent() && this.port.getAsInt() != port) return false;
        return predicate.matches(domain)
            || predicate.matches(address)
            || (ipv4Address != null && predicate.matches(ipv4Address));
    }

    public static Options apply(Iterable<? extends AddressRule> rules, String domain, InetSocketAddress socketAddress) {
        var options = PartialOptions.DEFAULT;

        var port = socketAddress.getPort();
        var address = socketAddress.getAddress();
        if (address == null) throw new IllegalStateException("Socket address must be resolved");

        var ipv4Address = address instanceof Inet6Address inet6 ? getEmbeddedV4Address(inet6) : null;

        for (var rule : rules) {
            if (!rule.matches(domain, port, address, ipv4Address)) continue;
            options = options.merge(rule.partial);
        }

        return options.toOptions();
    }

    /**
     * Extract any IPv4 address embedded inside an IPv6 address. This includes:
     * <ul>
     *     <li>{@linkplain #isIPv4MappedAddress(Inet6Address) IPv4-mapped addresses} ({@code ::ffff:1.2.3.4})</li>
     *     <li>{@linkplain InetAddresses#isCompatIPv4Address(Inet6Address) IPv4-compatibility addresses} ({@code ::1.2.3.4})</li>
     *     <li>{@linkplain InetAddresses#is6to4Address(Inet6Address) 6-to-4 addresses} ({@code 2002:1.2.3.4}</li>
     * </ul>
     * <p>
     * This also extracts {@linkplain InetAddresses#isTeredoAddress(Inet6Address) Teredo addresses}, and does
     * <em>not</em> extract NAT64. However, these are both blocked by {@link AddressPredicate.PrivatePattern} by
     * default.
     *
     * @param address The IPv6 address to extract from.
     * @return The IPv4 address or {@code null}
     */
    private static @Nullable Inet4Address getEmbeddedV4Address(Inet6Address address) {
        if (InetAddresses.hasEmbeddedIPv4ClientAddress(address)) {
            return InetAddresses.getEmbeddedIPv4ClientAddress(address);
        }

        if (isIPv4MappedAddress(address)) {
            try {
                return (Inet4Address) InetAddress.getByAddress(Arrays.copyOfRange(address.getAddress(), 12, 16));
            } catch (UnknownHostException e) {
                throw new IllegalStateException("getByAddress should not fail", e);
            }
        }

        return null;
    }

    /**
     * Check whether an IP address is an IPv4-mapped address (of the form {@code ::ffff:aabb:ccdd}).
     *
     * @param address The IPv6 address.
     * @return Whether this is an IPv4-mapped address.
     * @see InetAddresses#isMappedIPv4Address(String)
     */
    private static boolean isIPv4MappedAddress(Inet6Address address) {
        var bytes = address.getAddress();
        for (var i = 0; i < 10; i++) {
            if (bytes[i] != 0) return false;
        }
        return bytes[10] == (byte) 0xff && bytes[11] == (byte) 0xff;
    }
}
