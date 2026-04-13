// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentInitializers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ServiceLoader;

/**
 * Bootstrap Minecraft before running these tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(WithMinecraft.Setup.class)
public @interface WithMinecraft {
    class Setup implements Extension, BeforeAllCallback {
        private static boolean setup = false;

        @Override
        public void beforeAll(ExtensionContext context) {
            bootstrap();
        }

        public static synchronized void bootstrap() {
            if (setup) return;
            setup = true;

            var hooks = ServiceLoader.load(SetupHook.class, SetupHook.class.getClassLoader())
                .stream().map(ServiceLoader.Provider::get).toList();

            for (var hook : hooks) hook.beforeBootstrap();
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
            for (var hook : hooks) hook.afterBootstrap();
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
                .build(VanillaRegistries.createLookup())
                .forEach(DataComponentInitializers.PendingComponents::apply);
        }
    }

    /**
     * Additional hooks to run as part of bootstrap.
     */
    interface SetupHook {
        void beforeBootstrap();
        void afterBootstrap();
    }
}
