// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.impl;

import dan200.computercraft.api.turtle.TurtleOrientationProvider;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Registry of {@link TurtleOrientationProvider}s.
 */
public final class TurtleOrientationProviders {
    private static final List<TurtleOrientationProvider> providers = new CopyOnWriteArrayList<>();

    private TurtleOrientationProviders() {
    }

    public static synchronized void register(TurtleOrientationProvider provider) {
        Objects.requireNonNull(provider, "provider cannot be null");
        providers.add(provider);
    }

    @Nullable
    public static BlockState transform(BlockState state, TurtleOrientationProvider.OrientationParameters params) {
        for (var provider : providers) {
            var result = provider.transform(state, params);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
