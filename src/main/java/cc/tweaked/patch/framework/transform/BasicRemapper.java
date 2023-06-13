// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.patch.framework.transform;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.Remapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A basic {@link Remapper} implementation which just applies a constant mapping.
 *
 * @see org.objectweb.asm.commons.SimpleRemapper
 */
public class BasicRemapper extends Remapper {
    private final Map<String, String> types;

    private BasicRemapper(Map<String, String> types) {
        this.types = types;
    }

    public static BasicRemapper remapType(String from, String to) {
        return new BasicRemapper(Collections.singletonMap(from, to));
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String map(String typeName) {
        return types.getOrDefault(typeName, typeName);
    }

    public Transform<MethodVisitor> toMethodTransform() {
        return mv -> new RemapMethod(mv, this);
    }

    public Transform<ClassVisitor> toClassTransform() {
        return cw -> new RemapClass(cw, this);
    }

    @Override
    public String toString() {
        return "Remap[" + types + "]";
    }

    public static class Builder {
        private final Map<String, String> types = new HashMap<>();

        public Builder remapType(String from, String to) {
            types.put(from, to);
            return this;
        }

        public BasicRemapper build() {
            return new BasicRemapper(types);
        }
    }
}
