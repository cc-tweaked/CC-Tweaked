/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.support;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bootstrap Minecraft before running these tests.
 */
@Target( ElementType.TYPE )
@Retention( RetentionPolicy.RUNTIME )
@ExtendWith( WithMinecraft.Setup.class )
public @interface WithMinecraft
{
    class Setup implements Extension, BeforeAllCallback
    {
        @Override
        public void beforeAll( ExtensionContext context )
        {
            bootstrap();
        }

        public static void bootstrap()
        {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
        }
    }
}
