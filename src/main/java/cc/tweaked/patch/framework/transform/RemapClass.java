// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch.framework.transform;

import org.objectweb.asm.*;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingClassAdapter;

/**
 * An alternative to {@link RemappingClassAdapter}, which doesn't require {@link ClassReader#EXPAND_FRAMES}.
 */
public class RemapClass extends ClassVisitor {
    private final Remapper remapper;

    public RemapClass(ClassVisitor classVisitor, Remapper remapper) {
        super(Opcodes.ASM4, classVisitor);
        this.remapper = remapper;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return super.visitField(access, name, remapper.mapDesc(desc), signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new RemapMethod(super.visitMethod(access, name, remapper.mapMethodDesc(desc), signature, exceptions), remapper);
    }
}
