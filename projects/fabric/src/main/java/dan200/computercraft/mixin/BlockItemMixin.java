// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import dan200.computercraft.shared.integration.LoadedMods;
import dan200.computercraft.shared.integration.libmultipart.LibMultiPartIntegration;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Adds multipart support to {@link BlockItem}.
 */
@Mixin(BlockItem.class)
class BlockItemMixin extends Item {
    BlockItemMixin(Properties properties) {
        super(properties);
    }

    @Inject(method = "place", at = @At(value = "HEAD"), cancellable = true)
    private void placeMultipart(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        if (!LoadedMods.LIB_MULTI_PART) return;

        // If we have custom handling for this item, and the default logic would not work, then we run our libmultipart
        // hook.
        var factory = LibMultiPartIntegration.getCreatorForItem(this);
        if (factory != null && !context.canPlace()) cir.setReturnValue(factory.placePart(context));
    }
}
