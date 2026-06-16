// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.platform;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.impl.Services;
import dan200.computercraft.shared.peripheral.monitor.ClientMonitor;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.resources.model.ModelDebugName;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public interface ClientPlatformHelper {
    static ClientPlatformHelper get() {
        var instance = Instance.INSTANCE;
        return instance == null ? Services.raise(ClientPlatformHelper.class, Instance.ERROR) : instance;
    }

    /**
     * Create a new unique {@link ModelKey}.
     *
     * @param name The debug name for this model key.
     * @param <T>  The type of baked model.
     * @return The newly created model key.
     */
    @Contract("_ -> new")
    <T> ModelKey<T> createModelKey(ModelDebugName name);

    /**
     * Submit a monitor to be rendered.
     *
     * @param collector The submit node collector.
     * @param poseStack The current translation.
     * @param monitor   The monitor to draw.
     * @param terminal  The terminal contents of the monitor.
     * @param xMargin   The X margin.
     * @param yMargin   The Y margin.
     */
    void submitMonitor(
        OrderedSubmitNodeCollector collector, PoseStack poseStack, ClientMonitor monitor, Terminal terminal, float xMargin, float yMargin
    );

    final class Instance {
        static final @Nullable ClientPlatformHelper INSTANCE;
        static final @Nullable Throwable ERROR;

        static {
            var helper = Services.tryLoad(ClientPlatformHelper.class);
            INSTANCE = helper.instance();
            ERROR = helper.error();
        }

        private Instance() {
        }
    }
}
