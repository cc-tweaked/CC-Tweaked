// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;
import dan200.computercraft.core.apis.http.options.InvalidRuleException;
import dan200.computercraft.core.apis.http.options.PartialOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * Parses, checks and generates {@link Config}s for {@link AddressRule}.
 */
class AddressRuleConfig {
    private static final Logger LOG = LoggerFactory.getLogger(AddressRuleConfig.class);

    private static final AddressRule REJECT_ALL = AddressRule.parse("*", OptionalInt.empty(), Action.DENY.toPartial());

    private static final Set<String> knownKeys = Set.of(
        "host", "action", "max_download", "max_upload", "max_websocket_message", "use_proxy"
    );

    public static List<UnmodifiableConfig> defaultRules() {
        return List.of(
            makeRule(config -> {
                config.setComment("host", """
                    The magic "$private" host matches all private address ranges, such as localhost and 192.168.0.0/16.
                    This rule prevents computers accessing internal services, and is strongly recommended.""");
                config.add("host", "$private");

                config.setComment("action", "Deny all requests to private IP addresses.");
                config.add("action", Action.DENY.name().toLowerCase(Locale.ROOT));
            }),
            makeRule(config -> {
                config.setComment("host", """
                    The wildcard "*" rule matches all remaining hosts.""");
                config.add("host", "*");

                config.setComment("action", "Allow all non-denied hosts.");
                config.add("action", Action.ALLOW.name().toLowerCase(Locale.ROOT));

                config.setComment("max_download", """
                    The maximum size (in bytes) that a computer can download in a single request.
                    Note that responses may receive more data than allowed, but this data will not
                    be returned to the client.""");
                config.set("max_download", AddressRule.MAX_DOWNLOAD);

                config.setComment("max_upload", """
                    The maximum size (in bytes) that a computer can upload in a single request. This
                    includes headers and POST text.""");
                config.set("max_upload", AddressRule.MAX_UPLOAD);

                config.setComment("max_websocket_message", "The maximum size (in bytes) that a computer can send or receive in one websocket packet.");
                config.set("max_websocket_message", AddressRule.WEBSOCKET_MESSAGE);

                config.setComment("use_proxy", "Enable use of the HTTP/SOCKS proxy if it is configured.");
                config.set("use_proxy", false);
            })
        );
    }

    private static UnmodifiableConfig makeRule(Consumer<CommentedConfig> setup) {
        var config = InMemoryCommentedFormat.defaultInstance().createConfig(LinkedHashMap::new);
        setup.accept(config);
        return config;
    }

    public static AddressRule parseRule(UnmodifiableConfig builder) {
        try {
            return doParseRule(builder);
        } catch (InvalidRuleException e) {
            LOG.error("Malformed HTTP rule: {} HTTP will NOT work until this is fixed.", e.getMessage());
            return REJECT_ALL;
        }
    }

    public static AddressRule doParseRule(UnmodifiableConfig builder) {
        var hostObj = get(builder, "host", String.class).orElse(null);
        if (hostObj == null) throw new InvalidRuleException("No 'host' specified");

        var action = getEnum(builder, "action", Action.class).orElse(null);
        var port = unboxOptInt(get(builder, "port", Number.class));
        var maxUpload = unboxOptLong(get(builder, "max_upload", Number.class).map(Number::longValue));
        var maxDownload = unboxOptLong(get(builder, "max_download", Number.class).map(Number::longValue));
        var websocketMessage = unboxOptInt(
            get(builder, "max_websocket_message", Number.class)
                // Fallback to (incorrect) websocket_message option.
                .or(() -> get(builder, "websocket_message", Number.class))
                .map(Number::intValue)
        );
        var useProxy = get(builder, "use_proxy", Boolean.class);

        // Find unknown keys and warn about them.
        var unknownKeys = builder.entrySet().stream().map(UnmodifiableConfig.Entry::getKey).filter(x -> !knownKeys.contains(x)).toList();
        if (!unknownKeys.isEmpty()) {
            LOG.warn("Unknown config {} {} in address rule.", unknownKeys.size() == 1 ? "option" : "options", String.join(", ", unknownKeys));
        }

        var options = new PartialOptions(
            action,
            maxUpload,
            maxDownload,
            websocketMessage,
            useProxy
        );

        return AddressRule.parse(hostObj, port, options);
    }

    private static <T> Optional<T> get(UnmodifiableConfig config, String field, Class<T> klass) {
        var value = config.get(field);
        if (value == null) return Optional.empty();
        if (klass.isInstance(value)) return Optional.of(klass.cast(value));

        throw new InvalidRuleException(String.format(
            "Field '%s' should be a '%s' but is a %s.",
            field, klass.getSimpleName(), value.getClass().getSimpleName()
        ));
    }

    private static <T extends Enum<T>> Optional<T> getEnum(UnmodifiableConfig config, String field, Class<T> klass) {
        return get(config, field, String.class).map(x -> parseEnum(field, klass, x));
    }

    private static OptionalLong unboxOptLong(Optional<? extends Number> value) {
        return value.map(Number::intValue).map(OptionalLong::of).orElse(OptionalLong.empty());
    }

    private static OptionalInt unboxOptInt(Optional<? extends Number> value) {
        return value.map(Number::intValue).map(OptionalInt::of).orElse(OptionalInt.empty());
    }

    private static <T extends Enum<T>> T parseEnum(String field, Class<T> klass, String x) {
        for (var value : klass.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(x)) return value;
        }

        throw new InvalidRuleException(String.format(
            "Field '%s' should be one of %s, but is '%s'.",
            field, Arrays.stream(klass.getEnumConstants()).map(Enum::name).toList(), x
        ));
    }
}
