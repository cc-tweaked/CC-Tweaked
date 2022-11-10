/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
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
    @SuppressWarnings("UnusedMethod")
    private void onTick(CallbackInfo ci) {
        var stack = getItem();
        if (stack.getItem() instanceof PocketComputerItem pocket) {
            pocket.onEntityItemUpdate(stack, (ItemEntity) (Object) this);
        }
    }

    @Shadow
    public abstract ItemStack getItem();
}
