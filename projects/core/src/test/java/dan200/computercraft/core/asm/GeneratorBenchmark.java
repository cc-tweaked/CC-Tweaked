// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import dan200.computercraft.api.lua.*;
import dan200.computercraft.core.methods.LuaMethod;
import org.objectweb.asm.*;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import java.lang.constant.ConstantDescs;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

import static org.objectweb.asm.Opcodes.*;

/**
 * Benchmarks for possible implementation strategies for {@link GeneratorBenchmark}.
 */
public class GeneratorBenchmark {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    public static final MethodHandle ADDER;

    static {
        try {
            ADDER = LOOKUP.findVirtual(Adder.class, "add", MethodType.methodType(int.class, int.class, int.class));
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static final MethodType METHOD_TYPE = MethodType.methodType(MethodResult.class, Object.class, ILuaContext.class, IArguments.class);

    @State(Scope.Benchmark)
    public static class ScriptScope {
        final IArguments arguments = new ObjectArguments(1, 5);
        final LuaMethod asmDirect;
        final LuaMethod asmMethodHandle;

        public ScriptScope() {
            try {
                asmDirect = makeAsmDirect();
                asmMethodHandle = makeAsmMethodHandle();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Benchmark
    public MethodResult asmDirect(ScriptScope scope) throws LuaException {
        return scope.asmDirect.apply(Adder.INSTANCE, null, scope.arguments);
    }

    @Benchmark
    public MethodResult asmMethodHandle(ScriptScope scope) throws LuaException {
        return scope.asmMethodHandle.apply(Adder.INSTANCE, null, scope.arguments);
    }

    /**
     * Make a {@link LuaMethod} via a generated class which invokes {@link Adder#add(int, int)} directly.
     *
     * @return The created {@link LuaMethod} instance.
     * @throws ReflectiveOperationException If the class could not be generated.
     */
    private static LuaMethod makeAsmDirect() throws ReflectiveOperationException {
        var bytes = createClass("AsmDirect", mw -> {
            // Receiver
            mw.visitVarInsn(ALOAD, 1);
            mw.visitTypeInsn(CHECKCAST, Type.getInternalName(Adder.class));
            // Arg 1
            mw.visitVarInsn(ALOAD, 3);
            mw.visitInsn(ICONST_0);
            mw.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(IArguments.class), "getInt", "(I)I", true);
            // Arg 2
            mw.visitVarInsn(ALOAD, 3);
            mw.visitInsn(ICONST_1);
            mw.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(IArguments.class), "getInt", "(I)I", true);
            // Invoke
            mw.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Adder.class), "add", "(II)I", false);
            // Wrap and return
            mw.visitMethodInsn(INVOKESTATIC, Type.getInternalName(Integer.class), "valueOf", "(I)" + Type.getDescriptor(Integer.class), false);
            mw.visitMethodInsn(INVOKESTATIC, Type.getInternalName(MethodResult.class), "of", "(Ljava/lang/Object;)" + Type.getDescriptor(MethodResult.class), false);
            mw.visitInsn(ARETURN);
        });

        return LOOKUP.defineHiddenClass(bytes, true)
            .lookupClass().asSubclass(LuaMethod.class).getConstructor().newInstance();
    }

    /**
     * Make a {@link LuaMethod} via a generated class which invokes {@link Adder#add(int, int)} with {@link MethodHandle#invokeExact(Object...)}.
     *
     * @return The created {@link LuaMethod} instance.
     * @throws ReflectiveOperationException If the class could not be generated.
     */
    private static LuaMethod makeAsmMethodHandle() throws ReflectiveOperationException {
        var castingHandle = ADDER.asType(MethodType.methodType(Object.class, Object.class, int.class, int.class));

        var bytes = createClass("AsmMethodHandle", mw -> {
            var classData = new Handle(
                H_INVOKESTATIC, Type.getInternalName(MethodHandles.class), "classData",
                MethodType.methodType(Object.class, MethodHandles.Lookup.class, String.class, Class.class).descriptorString(), false
            );
            mw.visitLdcInsn(new ConstantDynamic(ConstantDescs.DEFAULT_NAME, MethodHandle.class.descriptorString(), classData));

            // Receiver
            mw.visitVarInsn(ALOAD, 1);
            // Arg 1
            mw.visitVarInsn(ALOAD, 3);
            mw.visitInsn(ICONST_0);
            mw.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(IArguments.class), "getInt", "(I)I", true);
            // Arg 2
            mw.visitVarInsn(ALOAD, 3);
            mw.visitInsn(ICONST_1);
            mw.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(IArguments.class), "getInt", "(I)I", true);
            // Invoke
            mw.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(MethodHandle.class), "invokeExact", castingHandle.type().descriptorString(), false);
            // Wrap and return
            mw.visitMethodInsn(INVOKESTATIC, Type.getInternalName(MethodResult.class), "of", "(Ljava/lang/Object;)" + Type.getDescriptor(MethodResult.class), false);
            mw.visitInsn(ARETURN);
        });

        return LOOKUP.defineHiddenClassWithClassData(bytes, castingHandle, true)
            .lookupClass().asSubclass(LuaMethod.class).getConstructor().newInstance();
    }

    private static byte[] createClass(String name, Consumer<MethodVisitor> method) {
        var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cw.visit(
            V17, ACC_PUBLIC | ACC_FINAL, GeneratorBenchmark.class.getPackageName().replace('.', '/') + "/" + name,
            null, "java/lang/Object", new String[]{ Type.getInternalName(LuaMethod.class) }
        );
        cw.visitSource("CC generated method", null);

        { // Constructor just invokes super.
            var mw = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mw.visitCode();
            mw.visitVarInsn(ALOAD, 0);
            mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mw.visitInsn(RETURN);
            mw.visitMaxs(0, 0);
            mw.visitEnd();
        }

        {
            var mw = cw.visitMethod(
                ACC_PUBLIC, "apply", METHOD_TYPE.descriptorString(),
                null, new String[]{ Type.getInternalName(LuaException.class) }
            );
            mw.visitCode();

            method.accept(mw);

            mw.visitMaxs(0, 0);
            mw.visitEnd();
        }

        cw.visitEnd();

        return cw.toByteArray();
    }

    public static class Adder {
        static final Adder INSTANCE = new Adder();

        public int add(int left, int right) {
            return left + right;
        }
    }

    public static void main(String... args) throws RunnerException {
        var opts = new OptionsBuilder()
            .include(GeneratorBenchmark.class.getName() + ".*")
            .warmupIterations(2)
            .measurementIterations(5)
            .measurementTime(TimeValue.milliseconds(5000))
            .jvmArgsPrepend("-server")
            .forks(3)
            .build();
        new Runner(opts).run();
    }
}
