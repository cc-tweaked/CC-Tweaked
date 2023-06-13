// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch.framework;

import cc.tweaked.patch.framework.transform.Transform;
import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The main entrypoint to the transformer interface. This allows registering {@linkplain Transform transforms} over a
 * specific class or method, and then handles applying those rewrites.
 */
public class TransformationChain {
    private final Map<String, ClassTransformer> transforms = new HashMap<>();

    public TransformationChain atClass(String name, Transform<ClassVisitor> transform) {
        transforms.computeIfAbsent(name, x -> new ClassTransformer()).transforms.add(transform);
        return this;
    }

    public TransformationChain atMethod(String owner, String name, String desc, Transform<MethodVisitor> transform) {
        transforms.computeIfAbsent(owner, x -> new ClassTransformer())
            .methodTransforms.computeIfAbsent(new MethodDesc(name, desc), x -> new ArrayList<>())
            .add(transform);
        return this;
    }

    public byte[] transform(String className, byte[] input) {
        ClassTransformer transform = transforms.get(className);
        if (transform == null) return input;

        ClassReader reader = new ClassReader(input);
        ClassWriter writer = new ClassWriter(0);
        reader.accept(transform.transform(writer), 0);
        return writer.toByteArray();
    }

    private static final class MethodDesc {
        final String name;
        final String desc;

        private MethodDesc(String name, String desc) {
            this.name = name;
            this.desc = desc;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodDesc that = (MethodDesc) o;

            if (!name.equals(that.name)) return false;
            return desc.equals(that.desc);
        }

        @Override
        public int hashCode() {
            int result = name.hashCode();
            result = 31 * result + desc.hashCode();
            return result;
        }
    }

    private static class ClassTransformer {
        final Map<MethodDesc, List<Transform<MethodVisitor>>> methodTransforms = new HashMap<>();
        final List<Transform<ClassVisitor>> transforms = new ArrayList<>();

        ClassVisitor transform(ClassVisitor visitor) {
            for (Transform<ClassVisitor> transform : transforms) visitor = transform.chain(visitor);
            if (!methodTransforms.isEmpty()) visitor = new MethodTransformClassVisitor(visitor, methodTransforms);

            return visitor;
        }
    }

    private static class MethodTransformClassVisitor extends ClassVisitor {
        private final Map<MethodDesc, List<Transform<MethodVisitor>>> methodTransforms;

        MethodTransformClassVisitor(ClassVisitor visitor, Map<MethodDesc, List<Transform<MethodVisitor>>> methodTransforms) {
            super(Opcodes.ASM4, visitor);
            this.methodTransforms = methodTransforms;
        }

        @Override
        public MethodVisitor visitMethod(int opcode, String name, String desc, String signature, String[] exceptions) {
            MethodVisitor visitor = super.visitMethod(opcode, name, desc, signature, exceptions);

            List<Transform<MethodVisitor>> transforms = methodTransforms.get(new MethodDesc(name, desc));
            if (transforms != null) {
                for (Transform<MethodVisitor> transform : transforms) visitor = transform.chain(visitor);
            }

            return visitor;
        }
    }
}
