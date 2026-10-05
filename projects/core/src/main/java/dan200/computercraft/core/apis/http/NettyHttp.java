// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http;

import dan200.computercraft.core.apis.IAPIEnvironment;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;
import dan200.computercraft.core.apis.http.options.Options;
import dan200.computercraft.core.apis.http.options.ProxyType;
import dan200.computercraft.core.apis.http.request.HttpRequest;
import dan200.computercraft.core.apis.http.websocket.Websocket;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import org.jspecify.annotations.Nullable;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.OptionalInt;

/**
 * Provides HTTP support for computers using Netty.
 *
 * @see HttpHandler
 */
public class NettyHttp implements HttpHandler.Factory {
    /**
     * Configuration for {@link NettyHttp}.
     *
     * @param enabled           Whether the HTTP API is enabled.
     * @param addressRules      Rules to apply to hosts/ports to determine whether a HTTP request is allowed, along with
     *                          other {@link Options}.
     * @param maxRequests       The maximum number of HTTP requests that can be in-flight per computer.
     * @param maxWebsockets     The maximum number of websockets that can be in-flight per computer.
     * @param downloadBandwidth The maximum download bandwidth (in bytes) across all computers.
     * @param uploadBandwidth   The maximum upload bandwidth (in bytes) across all computers.
     * @param proxy             Configuration for our HTTP proxy.
     */
    public record Config(
        boolean enabled,
        List<AddressRule> addressRules,
        int maxRequests,
        int maxWebsockets,
        int downloadBandwidth,
        int uploadBandwidth,
        @Nullable ProxyConfig proxy
    ) {
        public Config withAddressRules(List<AddressRule> addressRules) {
            return new Config(enabled(), addressRules, maxRequests(), maxWebsockets(), downloadBandwidth(), uploadBandwidth(), proxy());
        }
    }

    /**
     * Configuration for a HTTP proxy.
     *
     * @param type     The type of proxy.
     * @param host     The host name of the proxy.
     * @param port     The port of the proxy.
     * @param username An optional username for the proxy. Can be an empty string.
     * @param password An optional password for the proxy. Can be an empty string.
     */
    public record ProxyConfig(ProxyType type, String host, int port, String username, String password) {
    }

    /**
     * The default config options.
     */
    public static final Config DEFAULT_CONFIG = new Config(
        true,
        List.of(
            AddressRule.parse("$private", OptionalInt.empty(), Action.DENY.toPartial()),
            AddressRule.parse("*", OptionalInt.empty(), Action.ALLOW.toPartial())
        ),
        HttpHandler.DEFAULT_MAX_REQUESTS,
        HttpHandler.DEFAULT_MAX_WEBSOCKETS,
        32 * 1024 * 1024,
        32 * 1024 * 1024,
        null
    );

    /**
     * A version of {@link #DEFAULT_CONFIG} that allows access to the local network.
     */
    public static final Config LOCAL_NETWORK_CONFIG = DEFAULT_CONFIG.withAddressRules(
        List.of(AddressRule.parse("*", OptionalInt.empty(), Action.ALLOW.toPartial()))
    );

    private Config config;
    private final NetworkUtils network;

    /**
     * Create a new {@link NettyHttp} using the default config.
     */
    public NettyHttp() {
        this(DEFAULT_CONFIG);
    }

    /**
     * Create a new {@link NettyHttp} using the given config.
     *
     * @param config The config to create this instance with.
     */
    public NettyHttp(Config config) {
        this.config = config;
        this.network = new NetworkUtils(this);
    }

    @Override
    public @Nullable HttpHandler create(IAPIEnvironment environment) {
        return new NettyHttpHandler(environment);
    }

    Config getConfig() {
        return config;
    }

    /**
     * Update the config for our HTTP requests.
     *
     * @param config The new config definition.
     */
    public void setConfig(Config config) {
        this.config = config;
        network.reloadConfig();
    }

    private final class NettyHttpHandler implements HttpHandler {
        private final IAPIEnvironment environment;
        private final ResourceGroup<CheckUrl> checkUrls = new ResourceGroup<>(() -> ResourceGroup.DEFAULT_LIMIT);
        private final ResourceGroup<HttpRequest> requests = new ResourceQueue<>(() -> config.maxRequests());
        private final ResourceGroup<Websocket> websockets = new ResourceGroup<>(() -> config.maxWebsockets());

        private NettyHttpHandler(IAPIEnvironment environment) {
            this.environment = environment;
        }

        @Override
        public void startup() {
            checkUrls.startup();
            requests.startup();
            websockets.startup();
        }

        @Override
        public void shutdown() {
            checkUrls.shutdown();
            requests.shutdown();
            websockets.shutdown();
        }

        @Override
        public boolean queueCheckUrl(String address, URI uri) {
            return new CheckUrl(checkUrls, environment, network, address, uri).queue(CheckUrl::run);
        }

        @Override
        public boolean queueRequest(String address, URI uri, HttpMethod method, @Nullable ByteBuffer postBody, HttpHeaders headers, boolean binary, boolean followRedirects, int timeout) {
            return new HttpRequest(requests, environment, network, address, postBody, headers, binary, followRedirects, timeout).queue(r -> r.request(uri, method));
        }

        @Override
        public boolean queueWebsocket(String address, URI uri, HttpHeaders headers, int timeout) {
            return new Websocket(websockets, environment, network, address, uri, headers, timeout).queue(Websocket::connect);
        }
    }
}
