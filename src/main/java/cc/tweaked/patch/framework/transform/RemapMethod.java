// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch.framework.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.RemappingMethodAdapter;

/**
 * An alternative to {@link RemappingMethodAdapter}, which doesn't require {@link ClassReader#EXPAND_FRAMES}.
 */
public class RemapMethod extends MethodVisitor {
    private final Remapper remapper;

    public RemapMethod(MethodVisitor methodVisitor, Remapper remapper) {
        super(Opcodes.ASM4, methodVisitor);
        this.remapper = remapper;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        super.visitMethodInsn(opcode, remapper.mapType(owner), name, remapper.mapMethodDesc(desc));
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        super.visitFieldInsn(opcode, remapper.mapType(owner), name, remapper.mapDesc(desc));
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        super.visitTypeInsn(opcode, remapper.mapType(type));
    }
}
