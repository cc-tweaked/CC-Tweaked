// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch.framework.transform;

import cc.tweaked.patch.framework.AnnotationHelper;
import org.objectweb.asm.*;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.objectweb.asm.Opcodes.INVOKESPECIAL;


/**
 * Merge two classes together.
 */
public class MergeVisitor extends ClassVisitor {
    private final static String SHADOW = Type.getDescriptor(Shadow.class);
    private final static String REWRITE = Type.getDescriptor(Rewrite.class);

    private final ClassNode node;

    private final Set<String> visited = new HashSet<>();
    private final Remapper remapper;

    private boolean writingOverride = false;
    private String superClass = null;

    /**
     * Merge two classes together.
     *
     * @param cv       The visitor to write to
     * @param node     The node that holds override methods
     * @param remapper Mapper for override classes to new ones
     */
    public MergeVisitor(ClassVisitor cv, ClassNode node, Remapper remapper) {
        super(Opcodes.ASM4);
        this.cv = new RemapClass(cv, remapper);
        this.node = node;
        this.remapper = remapper;
    }

    /**
     * Merge two classes together.
     *
     * @param cv       The visitor to write to
     * @param node     The class reader that holds override properties
     * @param remapper Mapper for override classes to new ones
     */
    public MergeVisitor(ClassVisitor cv, ClassReader node, Remapper remapper) {
        this(cv, makeNode(node), remapper);
    }

    /**
     * Helper method to make a {@link ClassNode}
     *
     * @param reader The class reader to make a node
     * @return The created node
     */
    private static ClassNode makeNode(ClassReader reader) {
        ClassNode node = new ClassNode();
        reader.accept(node, ClassReader.EXPAND_FRAMES);
        return node;
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        if (AnnotationHelper.hasAnnotation(node, SHADOW)) {
            // If we are a stub, visit normally
            super.visit(version, access, name, signature, superName, interfaces);
        } else if (AnnotationHelper.hasAnnotation(node, REWRITE)) {
            // If we are a total rewrite, then visit the overriding class
            node.accept(cv);

            // And prevent writing the normal one
            cv = null;
        } else {
            // Merge both interfaces
            Set<String> overrideInterfaces = new HashSet<>();
            for (String inter : (List<String>) node.interfaces) {
                overrideInterfaces.add(remapper.mapType(inter));
            }
            Collections.addAll(overrideInterfaces, interfaces);

            writingOverride = true;
            superClass = superName;

            super.visit(node.version, access, name, node.signature, superName, overrideInterfaces.toArray(new String[0]));

            // Visit fields
            for (FieldNode field : (List<FieldNode>) node.fields) {
                if (!AnnotationHelper.hasAnnotation(field.invisibleAnnotations, SHADOW)) field.accept(this);
            }

            // Visit methods
            for (MethodNode method : (List<MethodNode>) node.methods) {
                if (!method.name.equals("<init>") && !method.name.equals("<clinit>")) {
                    if (!AnnotationHelper.hasAnnotation(method.invisibleAnnotations, SHADOW)) method.accept(this);
                }
            }

            writingOverride = false;
        }
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        if (!visited.add(name)) return null;
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        String description = "(" + remapper.mapMethodDesc(desc) + ")";

        if (!visited.add(name + description)) return null;

        MethodVisitor visitor = super.visitMethod(access, name, desc, signature, exceptions);

        // We remap super methods if the method is not static and we are writing the override methods
        return visitor != null && !Modifier.isStatic(access) && writingOverride && superClass != null
            ? new SuperMethodVisitor(api, visitor)
            : visitor;
    }

    /**
     * Visitor that remaps super calls
     */
    public class SuperMethodVisitor extends MethodVisitor {
        public SuperMethodVisitor(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String desc) {
            // If it is a constructor, or it is in the current class (private method)
            // we shouldn't remap to the base class
            // Reference: http://stackoverflow.com/questions/20382652/detect-super-word-in-java-code-using-bytecode
            if (opcode == INVOKESPECIAL && !name.equals("<init>") && owner.equals(node.superName)) {
                owner = superClass;
            }
            super.visitMethodInsn(opcode, owner, name, desc);
        }
    }

    /**
     * Mark this node as a stub, it will not be injected into the target class.
     */
    @Target({ ElementType.METHOD, ElementType.FIELD, ElementType.TYPE, ElementType.CONSTRUCTOR })
    @Retention(RetentionPolicy.CLASS)
    public @interface Shadow {
    }

    /**
     * Rewrite the original class instead of merging
     */
    @Target({ ElementType.TYPE, ElementType.FIELD })
    @Retention(RetentionPolicy.CLASS)
    public @interface Rewrite {
    }
}
