// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.apis.http.options;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;

public enum Action {
    ALLOW,
    DENY;

    private final PartialOptions partial = new PartialOptions(
        this, OptionalLong.empty(), OptionalLong.empty(), OptionalInt.empty(), Optional.empty()
    );

    public PartialOptions toPartial() {
        return partial;
    }
}
