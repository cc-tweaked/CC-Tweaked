// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

import com.google.errorprone.annotations.Immutable;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

@Immutable
public final class PartialOptions {
    public static final PartialOptions DEFAULT = new PartialOptions(
        null, OptionalLong.empty(), OptionalLong.empty(), OptionalInt.empty(), Optional.empty()
    );

    private final @Nullable Action action;
    private final OptionalLong maxUpload;
    private final OptionalLong maxDownload;
    private final OptionalInt websocketMessage;
    private final Optional<Boolean> useProxy;

    @SuppressWarnings("Immutable") // Lazily initialised, so this mutation is invisible in the public API
    private @Nullable Options options;

    public PartialOptions(@Nullable Action action, OptionalLong maxUpload, OptionalLong maxDownload, OptionalInt websocketMessage, Optional<Boolean> useProxy) {
        this.action = action;
        this.maxUpload = maxUpload;
        this.maxDownload = maxDownload;
        this.websocketMessage = websocketMessage;
        this.useProxy = useProxy;
    }

    Options toOptions() {
        if (options != null) return options;

        return options = new Options(
            action == null ? Action.DENY : action,
            maxUpload.orElse(AddressRule.MAX_UPLOAD),
            maxDownload.orElse(AddressRule.MAX_DOWNLOAD),
            websocketMessage.orElse(AddressRule.WEBSOCKET_MESSAGE),
            useProxy.orElse(false)
        );
    }

    /**
     * Perform a left-biased union of two {@link PartialOptions}.
     *
     * @param other The other partial options to combine with.
     * @return The merged options map.
     */
    PartialOptions merge(PartialOptions other) {
        // Short circuit for DEFAULT. This has no effect on the outcome, but avoids an allocation.
        if (this == DEFAULT) return other;

        return new PartialOptions(
            action == null && other.action != null ? other.action : action,
            maxUpload.isPresent() ? maxUpload : other.maxUpload,
            maxDownload.isPresent() ? maxDownload : other.maxDownload,
            websocketMessage.isPresent() ? websocketMessage : other.websocketMessage,
            useProxy.isPresent() ? useProxy : other.useProxy
        );
    }
}
