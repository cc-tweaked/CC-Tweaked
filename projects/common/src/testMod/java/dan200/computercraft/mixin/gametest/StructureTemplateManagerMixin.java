// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(StructureTemplateManager.class)
class StructureTemplateManagerMixin {
    /**
     * Ensure {@link net.minecraft.SharedConstants#IS_RUNNING_IN_IDE} is always true, meaning the test structure loader
     * is always present.
     *
     * @return A constant {@code true}.
     */
    @SuppressWarnings("UnusedMethod")
    @Redirect(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/SharedConstants;IS_RUNNING_IN_IDE:Z"))
    private boolean getRunningInIde() {
        return true;
    }
}
