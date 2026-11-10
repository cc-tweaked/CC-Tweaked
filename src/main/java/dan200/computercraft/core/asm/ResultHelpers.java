// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

final class ResultHelpers {
    private ResultHelpers() {
    }

    static RuntimeException throwUnchecked(Throwable t) {
        return throwUnchecked0(t);
    }

    @SuppressWarnings({ "unchecked", "TypeParameterUnusedInFormals" })
    private static <T extends Throwable> T throwUnchecked0(Throwable t) throws T {
        throw (T) t;
    }
}
