// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * A well-formed URL with which we can make HTTP requests.
 */
public final class HttpUri {
    private final URI uri;
    private final boolean ssl;
    private final String host;

    private HttpUri(URI uri, boolean ssl, String host) {
        this.uri = uri;
        this.ssl = ssl;
        this.host = host;
    }

    /**
     * Parse and validate a URL.
     *
     * @param url              The URL to parse.
     * @param insecureProtocol The non-SSL based protocol (e.g. {@code http}).
     * @param secureProtocol   The non-SSL based protocol (e.g. {@code https}).
     * @param defaultProtocol  An optional default protocol to use if a protocol is not given.
     * @return The validated URL.
     * @throws HTTPRequestException If the URL is not valid.
     */
    public static HttpUri check(String url, String insecureProtocol, String secureProtocol, @Nullable String defaultProtocol) throws HTTPRequestException {
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException ignored) {
            // Fall through to the case below
        }

        if (defaultProtocol != null && (uri == null || uri.getHost() == null || uri.getScheme() == null)) {
            try {
                uri = new URI(defaultProtocol + "://" + url);
            } catch (URISyntaxException ignored) {
                // Fall through to the case below
            }
        }

        if (uri == null) throw new HTTPRequestException("URL malformed");

        return check(uri, insecureProtocol, secureProtocol);
    }

    /**
     * Validate a URL.
     *
     * @param url              The URL to parse.
     * @param insecureProtocol The non-SSL based protocol (e.g. {@code http}).
     * @param secureProtocol   The non-SSL based protocol (e.g. {@code https}).
     * @return The validated URL.
     * @throws HTTPRequestException If the URL is not valid.
     */
    public static HttpUri check(URI url, String insecureProtocol, String secureProtocol) throws HTTPRequestException {
        // Validate the URL
        if (url.getScheme() == null) {
            throw new HTTPRequestException("Must specify " + insecureProtocol + " or " + secureProtocol);
        }
        if (url.getHost() == null) throw new HTTPRequestException("URL malformed");

        boolean ssl;
        var scheme = url.getScheme();
        if (scheme.equalsIgnoreCase(insecureProtocol)) {
            ssl = false;
        } else if (scheme.equalsIgnoreCase(secureProtocol)) {
            ssl = true;
        } else {
            throw new HTTPRequestException("Invalid protocol '" + scheme + "'");
        }

        return new HttpUri(url, ssl, url.getHost());
    }

    /**
     * Get the host of this URI.
     *
     * @return The host of this URI.
     */
    public String getHost() {
        return host;
    }

    /**
     * Get the port of this URI.
     *
     * @return The port of this URI.
     */
    int getPort() {
        var port = uri.getPort();
        return port >= 0 ? port : (ssl ? 443 : 80);
    }

    /**
     * Get the underlying URI.
     *
     * @return The underlying URI.
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Whether requests to this URI should use SSL/TLS.
     *
     * @return If requests to this URL should use SSL.
     */
    public boolean getSsl() {
        return ssl;
    }
}
