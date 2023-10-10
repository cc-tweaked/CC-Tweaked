// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

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
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(WithMinecraft.Setup.class)
public @interface WithMinecraft {
    class Setup implements Extension, BeforeAllCallback {
        @Override
        public void beforeAll(ExtensionContext context) {
            bootstrap();
        }

        public static void bootstrap() {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
        }
    }
}
