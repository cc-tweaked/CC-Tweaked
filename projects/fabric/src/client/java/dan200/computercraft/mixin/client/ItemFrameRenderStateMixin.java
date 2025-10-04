// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import dan200.computercraft.client.ExtendedItemFrameRenderStateHolder;
import dan200.computercraft.client.render.ExtendedItemFrameRenderState;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ItemFrameRenderState.class)
class ItemFrameRenderStateMixin implements ExtendedItemFrameRenderStateHolder {
    private @Nullable ExtendedItemFrameRenderState computercraft$state;

    @Override
    public ExtendedItemFrameRenderState computercraft$state() {
        if (computercraft$state == null) computercraft$state = new ExtendedItemFrameRenderState();
        return computercraft$state;
    }
}
