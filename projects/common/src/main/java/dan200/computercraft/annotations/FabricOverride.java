// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.annotations;

import java.lang.annotation.*;

/**
 * Equivalent to {@link Override}, but for Fabric-specific methods.
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface FabricOverride {
}
