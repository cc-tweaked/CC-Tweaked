// SPDX-FileCopyrightText: 2020 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.core.asm;

import java.security.ProtectionDomain;

final class DeclaringClassLoader extends ClassLoader {
    static final DeclaringClassLoader INSTANCE = new DeclaringClassLoader();

    private DeclaringClassLoader() {
        super(DeclaringClassLoader.class.getClassLoader());
    }

    Class<?> define(String name, byte[] bytes, ProtectionDomain protectionDomain) throws ClassFormatError {
        return defineClass(name, bytes, 0, bytes.length, protectionDomain);
    }
}
