// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch.framework.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * A transformation over a method or class.
 *
 * @param <V> The type of visitor, either {@link ClassVisitor} or {@link MethodVisitor}.
 */
public interface Transform<V> {
    /**
     * Apply this transformation, chaining together a target {@linkplain V visitor} with your visitor.
     *
     * @param visitor The visitor to wrap.
     * @return The wrapped visitor.
     */
    V chain(V visitor);
}
