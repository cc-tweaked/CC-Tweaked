// SPDX-FileCopyrightText: 2021 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.MethodResult;

import javax.annotation.Nullable;

final class ResultHelpers {
    private ResultHelpers() {
    }

    @Nullable
    static Object[] checkNormalResult(MethodResult result) {
        if (result.getCallback() != null) {
            // Due to how tasks are implemented, we can't currently return a MethodResult. This is an
            // entirely artificial limitation - we can remove it if it ever becomes an issue.
            throw new IllegalStateException("Must return MethodResult.of from mainThread function.");
        }

        return result.getResult();
    }

    static RuntimeException throwUnchecked(Throwable t) {
        return throwUnchecked0(t);
    }

    @SuppressWarnings({ "unchecked", "TypeParameterUnusedInFormals" })
    private static <T extends Throwable> T throwUnchecked0(Throwable t) throws T {
        throw (T) t;
    }
}
