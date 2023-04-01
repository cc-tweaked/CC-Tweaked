// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.config;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.InMemoryCommentedFormat;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import dan200.computercraft.core.apis.http.options.Action;
import dan200.computercraft.core.apis.http.options.AddressRule;
import dan200.computercraft.core.apis.http.options.PartialOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Parses, checks and generates {@link Config}s for {@link AddressRule}.
 */
class AddressRuleConfig {
    private static final Logger LOG = LoggerFactory.getLogger(AddressRuleConfig.class);

    public static UnmodifiableConfig makeRule(String host, Action action) {
        var config = InMemoryCommentedFormat.defaultInstance().createConfig(ConcurrentHashMap::new);
        config.add("host", host);
        config.add("action", action.name().toLowerCase(Locale.ROOT));

        if (host.equals("*") && action == Action.ALLOW) {
            config.setComment("timeout", "The period of time (in milliseconds) to wait before a HTTP request times out. Set to 0 for unlimited.");
            config.add("timeout", AddressRule.TIMEOUT);

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
        }

        return config;
    }

    public static boolean checkRule(UnmodifiableConfig builder) {
        var hostObj = get(builder, "host", String.class).orElse(null);
        var port = unboxOptInt(get(builder, "port", Number.class));
        return hostObj != null && checkEnum(builder, "action", Action.class)
            && check(builder, "port", Number.class)
            && check(builder, "timeout", Number.class)
            && check(builder, "max_upload", Number.class)
            && check(builder, "max_download", Number.class)
            && check(builder, "websocket_message", Number.class)
            && AddressRule.parse(hostObj, port, PartialOptions.DEFAULT) != null;
    }

    @Nullable
    public static AddressRule parseRule(UnmodifiableConfig builder) {
        var hostObj = get(builder, "host", String.class).orElse(null);
        if (hostObj == null) return null;

        var action = getEnum(builder, "action", Action.class).orElse(null);
        var port = unboxOptInt(get(builder, "port", Number.class));
        var timeout = unboxOptInt(get(builder, "timeout", Number.class));
        var maxUpload = unboxOptLong(get(builder, "max_upload", Number.class).map(Number::longValue));
        var maxDownload = unboxOptLong(get(builder, "max_download", Number.class).map(Number::longValue));
        var websocketMessage = unboxOptInt(get(builder, "websocket_message", Number.class).map(Number::intValue));

        var options = new PartialOptions(
            action,
            maxUpload,
            maxDownload,
            timeout,
            websocketMessage
        );

        return AddressRule.parse(hostObj, port, options);
    }

    private static <T> boolean check(UnmodifiableConfig config, String field, Class<T> klass) {
        var value = config.get(field);
        if (value == null || klass.isInstance(value)) return true;

        LOG.warn("HTTP rule's {} is not a {}.", field, klass.getSimpleName());
        return false;
    }

    private static <T extends Enum<T>> boolean checkEnum(UnmodifiableConfig config, String field, Class<T> klass) {
        var value = config.get(field);
        if (value == null) return true;

        if (!(value instanceof String)) {
            LOG.warn("HTTP rule's {} is not a string", field);
            return false;
        }

        if (parseEnum(klass, (String) value) == null) {
            LOG.warn("HTTP rule's {} is not a known option", field);
            return false;
        }

        return true;
    }

    private static <T> Optional<T> get(UnmodifiableConfig config, String field, Class<T> klass) {
        var value = config.get(field);
        return klass.isInstance(value) ? Optional.of(klass.cast(value)) : Optional.empty();
    }

    private static <T extends Enum<T>> Optional<T> getEnum(UnmodifiableConfig config, String field, Class<T> klass) {
        return get(config, field, String.class).map(x -> parseEnum(klass, x));
    }

    private static OptionalLong unboxOptLong(Optional<? extends Number> value) {
        return value.map(Number::intValue).map(OptionalLong::of).orElse(OptionalLong.empty());
    }

    private static OptionalInt unboxOptInt(Optional<? extends Number> value) {
        return value.map(Number::intValue).map(OptionalInt::of).orElse(OptionalInt.empty());
    }

    @Nullable
    private static <T extends Enum<T>> T parseEnum(Class<T> klass, String x) {
        for (var value : klass.getEnumConstants()) {
            if (value.name().equalsIgnoreCase(x)) return value;
        }
        return null;
    }
}
