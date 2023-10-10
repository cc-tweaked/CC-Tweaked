// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.client;

import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;

@Mixin(BlockModel.class)
public interface BlockModelAccessor {
    @Accessor("parentLocation")
    @Nullable
    ResourceLocation computercraft$getParentLocation();

    @Accessor("parent")
    @Nullable
    BlockModel computercraft$getParent();
}
