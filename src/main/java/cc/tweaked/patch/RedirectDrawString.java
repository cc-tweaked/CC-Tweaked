// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch;

import cc.tweaked.patch.framework.transform.Transform;
import dan200.computercraft.client.FixedWidthFontRenderer;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.ASM4;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;

/**
 * Redirects a call from {@link FixedWidthFontRenderer#drawString(String, int, int, String, int)} to
 * {@link FixedWidthFontRenderer#drawStringIsColour(String, int, int, String, int, boolean)}, pulling the colour from
 * the environment.
 *
 * @see dan200.computer.client.GuiComputer
 * @see dan200.computer.client.TileEntityMonitorRenderer
 */
class RedirectDrawString implements Transform<ClassVisitor> {
    private final Consumer<MethodVisitor> getTerminal;

    RedirectDrawString(Consumer<MethodVisitor> getTerminal) {
        this.getTerminal = getTerminal;
    }

    @Override
    public ClassVisitor chain(ClassVisitor visitor) {
        return new ClassVisitor(ASM4, visitor) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
                return new MethodVisitor(ASM4, super.visitMethod(access, name, desc, signature, exceptions)) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
                        if (!owner.endsWith("FixedWidthFontRenderer") || !name.endsWith("drawString") || !desc.equals("(Ljava/lang/String;IILjava/lang/String;I)V")) {
                            super.visitMethodInsn(opcode, owner, name, desc);
                            return;
                        }

                        getTerminal.accept(mv);
                        mv.visitMethodInsn(INVOKEINTERFACE, "dan200/computer/shared/ITerminalEntity", "isColour", "()Z");
                        mv.visitMethodInsn(opcode, owner, "drawStringIsColour", "(Ljava/lang/String;IILjava/lang/String;IZ)V");
                    }
                };
            }
        };
    }
}
