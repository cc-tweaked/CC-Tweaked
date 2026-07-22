// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.render;

import dan200.computercraft.annotations.ForgeOverride;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/**
 * A {@link BlockEntityRenderer} which has a custom bounding box for frustum culling.
 * <p>
 * This method is used by NeoForge for its built-in frustum culling, and by the Entity Culling mod on Fabric (see
 * <a href="https://github.com/tr7zw/EntityCulling/issues/313">this issue</a>). We explicitly declare this as an
 * interface method, so that the {@code getRenderBoundingBox(BlockEntity)} bridge method is generated on both loaders.
 *
 * @param <T> The type of block entity to render.
 * @param <S> The render state for the block entity.
 */
public interface BoundedBlockEntityRenderer<T extends BlockEntity, S extends BlockEntityRenderState> extends BlockEntityRenderer<T, S> {
    @ForgeOverride
    AABB getRenderBoundingBox(T blockEntity);
}
