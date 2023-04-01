// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import dan200.computercraft.data.PrettyJsonWriter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(targets = "net/minecraft/data/HashCache$CacheUpdater")
public class CacheUpdaterMixin {
    @SuppressWarnings("UnusedMethod")
    @ModifyArg(
        method = "writeIfNeeded",
        at = @At(value = "INVOKE", target = "Ljava/nio/file/Files;write(Ljava/nio/file/Path;[B[Ljava/nio/file/OpenOption;)Ljava/nio/file/Path;"),
        require = 0
    )
    private byte[] reformatJson(byte[] contents) {
        // It would be cleaner to do this inside DataProvider.saveStable, but Forge's version of Mixin doesn't allow us
        // to inject into interfaces.
        return PrettyJsonWriter.reformat(contents);
    }
}
