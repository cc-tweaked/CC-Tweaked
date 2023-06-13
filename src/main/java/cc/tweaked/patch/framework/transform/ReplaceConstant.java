// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch.framework.transform;

import cc.tweaked.patch.CorePlugin;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A {@link MethodVisitor} {@linkplain Transform transformer} which rewrites constants
 */
public final class ReplaceConstant implements Transform<MethodVisitor> {
    private final Map<Object, Object> replace;

    private ReplaceConstant(Map<Object, Object> replace) {
        this.replace = replace;
    }

    public static Transform<MethodVisitor> replace(Object from, Object to) {
        return new ReplaceConstant(Collections.singletonMap(from, to));
    }

    @Override
    public MethodVisitor chain(MethodVisitor visitor) {
        return new MethodVisitor(Opcodes.ASM4, visitor) {
            private final Set<Object> unmatched = new HashSet<>(replace.keySet());

            @Override
            public void visitLdcInsn(Object cst) {
                Object replacement = replace.get(cst);
                if (replacement == null) {
                    super.visitLdcInsn(cst);
                } else {
                    unmatched.remove(cst);
                    super.visitLdcInsn(replacement);
                }
            }

            @Override
            public void visitEnd() {
                super.visitEnd();

                if (!unmatched.isEmpty()) {
                    CorePlugin.LOG.warning("Failed to replace the following constants " + unmatched);
                }
            }
        };
    }
}
