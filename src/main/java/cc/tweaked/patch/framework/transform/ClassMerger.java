// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch.framework.transform;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

/**
 * Replaces parts of the class with a mixin-like class
 */
public class ClassMerger implements Transform<ClassVisitor> {
    private final String className;
    private final String mixinName;

    public ClassMerger(String className, String mixinName) {
        this.className = className.replace('.', '/');
        this.mixinName = mixinName.replace('.', '/');
    }

    @Override
    public ClassVisitor chain(ClassVisitor delegate) {
        ClassReader reader;
        try (InputStream stream = ClassMerger.class.getResourceAsStream("/" + mixinName + ".class")) {
            if (stream == null) throw new IllegalArgumentException("Failed to find " + mixinName);
            reader = new ClassReader(stream);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + mixinName, e);
        }

        return new MergeVisitor(delegate, reader, BasicRemapper.remapType(mixinName, className));
    }
}
