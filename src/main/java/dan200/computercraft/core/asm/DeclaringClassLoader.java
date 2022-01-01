/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.core.asm;

import java.security.ProtectionDomain;

final class DeclaringClassLoader extends ClassLoader
{
    static final DeclaringClassLoader INSTANCE = new DeclaringClassLoader();

    private DeclaringClassLoader()
    {
        super( DeclaringClassLoader.class.getClassLoader() );
    }

    Class<?> define( String name, byte[] bytes, ProtectionDomain protectionDomain ) throws ClassFormatError
    {
        return defineClass( name, bytes, 0, bytes.length, protectionDomain );
    }
}
