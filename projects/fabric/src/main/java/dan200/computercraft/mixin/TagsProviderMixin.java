/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Don't throw when using a Minecraft tag inside {@link TagsProvider}.
 * <p>
 * There's cleaner ways of doing this, like Forge's {@code ExistingFileHelper}, but I'm too lazy for that.
 */
@Mixin(TagsProvider.class)
class TagsProviderMixin {
    @Inject(at = @At("HEAD"), method = "method_33130", cancellable = true)
    public void onVerifyPresent(TagEntry tag, CallbackInfoReturnable<Boolean> cir) {
        var element = ((TagEntryAccessor) tag).computercraft$elementOrTag();
        if (element.tag() && element.id().getNamespace().equals("minecraft")) cir.setReturnValue(false);
    }
}
