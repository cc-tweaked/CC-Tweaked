// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.web.builder;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nullable;

import static org.objectweb.asm.Opcodes.*;

/**
 * Patch Cobalt's {@code LuaState} and CC's {@code CobaltLuaMachine} to self-interrupt.
 * <p>
 * In normal CC:T, computers are paused/interrupted asynchronously from the {@code ComputerThread}. However, as
 * Javascript doesn't (easily) support multi-threaded code, we must find another option.
 * <p>
 * Instead, we patch {@code LuaState.isInterrupted()} to periodically return true (every 1024 instructions), and then
 * patch the interruption callback to refresh the timeout state. This means that the machine runs in very small time
 * slices which, while quite slow, ensures we never block the UI thread for very long.
 */
public final class PatchCobalt {
    private static final String LUA_STATE = "org/squiddev/cobalt/LuaState";
    private static final String COBALT_MACHINE = "dan200/computercraft/core/lua/CobaltLuaMachine";

    private PatchCobalt() {
    }

    public static ClassVisitor patch(String name, ClassVisitor visitor) {
        return switch (name) {
            case LUA_STATE -> patchLuaState(visitor);
            case COBALT_MACHINE -> patchCobaltMachine(visitor);
            default -> visitor;
        };
    }

    /**
     * Patch Cobalt's {@code LuaState.isInterrupted()} to periodically return true.
     *
     * @param cv The original class visitor.
     * @return The transforming class visitor.
     */
    private static ClassVisitor patchLuaState(ClassVisitor cv) {
        return new ClassVisitor(Opcodes.ASM9, cv) {
            @Override
            public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                super.visit(version, access, name, signature, superName, interfaces);
                super.visitField(ACC_PRIVATE, "$count", "I", null, null).visitEnd();
            }

            @Override
            public @Nullable MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (mv == null) return null;

                if (name.equals("isInterrupted")) {
                    mv.visitCode();

                    // int x = $count + 1;
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, LUA_STATE, "$count", "I");
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(IADD);
                    mv.visitLdcInsn(1023);
                    mv.visitInsn(IAND);
                    mv.visitVarInsn(ISTORE, 1);
                    // $count = x;
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitFieldInsn(PUTFIELD, LUA_STATE, "$count", "I");
                    // return x == 0;
                    var label = new Label();
                    mv.visitVarInsn(ILOAD, 1);
                    mv.visitJumpInsn(IFNE, label);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(IRETURN);
                    mv.visitLabel(label);
                    mv.visitInsn(ICONST_0);
                    mv.visitInsn(IRETURN);

                    mv.visitMaxs(2, 2);
                    mv.visitEnd();

                    return null;
                }
                return mv;
            }
        };
    }

    /**
     * Patch {@code CobaltLuaMachine} to call {@code TimeoutState.refresh()} at the head of the function.
     *
     * @param cv The original class visitor.
     * @return The wrapped visitor.
     */
    private static ClassVisitor patchCobaltMachine(ClassVisitor cv) {
        return new ClassVisitor(ASM9, cv) {
            private boolean visited = false;

            @Override
            public @Nullable MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                var mv = super.visitMethod(access, name, descriptor, signature, exceptions);
                if (mv == null) return null;

                if (name.startsWith("lambda$") && descriptor.equals("()Lorg/squiddev/cobalt/interrupt/InterruptAction;")) {
                    visited = true;
                    return new MethodVisitor(api, mv) {
                        @Override
                        public void visitCode() {
                            super.visitCode();
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitFieldInsn(GETFIELD, COBALT_MACHINE, "timeout", "Ldan200/computercraft/core/computer/TimeoutState;");
                            mv.visitMethodInsn(INVOKEVIRTUAL, "dan200/computercraft/core/computer/TimeoutState", "refresh", "()V", false);
                        }
                    };
                }

                return mv;
            }

            @Override
            public void visitEnd() {
                if (!visited) throw new IllegalStateException("Did not inject .refresh() into CobaltLuaMachine");
                super.visitEnd();
            }
        };
    }
}
