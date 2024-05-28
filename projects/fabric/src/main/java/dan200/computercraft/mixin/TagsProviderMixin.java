// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.TagEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * Don't throw when using a Minecraft tag inside {@link TagsProvider}.
 * <p>
 * There's cleaner ways of doing this, like Forge's {@code ExistingFileHelper}, but I'm too lazy for that.
 */
@Mixin(TagsProvider.class)
class TagsProviderMixin {
    @Inject(at = @At("HEAD"), method = "method_49658", cancellable = true)
    @SuppressWarnings("unused")
    private static void onVerifyPresent(Predicate<?> predicate1, Predicate<?> predicate2, TagEntry tag, CallbackInfoReturnable<Boolean> cir) {
        var element = ((TagEntryAccessor) tag).computercraft$elementOrTag();
        if (element.tag() && element.id().getNamespace().equals("minecraft")) cir.setReturnValue(false);
    }
}
