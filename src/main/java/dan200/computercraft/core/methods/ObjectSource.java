// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.methods;

/**
 * A Lua object which exposes additional methods.
 * <p>
 * This can be used to merge multiple objects together into one. Ideally this'd be part of the API, but I'm not entirely
 * happy with the interface - something I'd like to think about first.
 */
public interface ObjectSource {
    Iterable<Object> getExtra();
}
