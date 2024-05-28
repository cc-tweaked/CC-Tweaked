// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
abstract class ItemEntityMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    @SuppressWarnings("unused")
    private void onTick(CallbackInfo ci) {
        var stack = getItem();
        if (stack.getItem() instanceof PocketComputerItem pocket) {
            pocket.onEntityItemUpdate(stack, (ItemEntity) (Object) this);
        }
    }

    @Shadow
    public abstract ItemStack getItem();
}
