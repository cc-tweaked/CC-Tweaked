// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import dan200.computercraft.core.util.Nullability;
import dan200.computercraft.gametest.core.TestHooks;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches {@link NbtUtils#structureToSnbt(CompoundTag)} to remove air blocks from the structure file. This
 * significantly reduces the size of our generated templates.
 *
 * @see StructureTemplateManagerMixin Loading structures
 * @see NbtUtils#structureToSnbt(CompoundTag)
 */
@Mixin(NbtUtils.class)
abstract class NbtUtilsMixin {
    @Inject(method = "structureToSnbt", at = @At("HEAD"))
    @SuppressWarnings("unused")
    private static void structureToSnbt(CompoundTag tag, CallbackInfoReturnable<String> ci) {
        // Load in the structure, strip out air, then save it back again.
        var structure = Nullability.assertNonNull(TestHooks.getStructureManager()).readStructure(tag);
        var palette = ((StructureTemplateAccessor) structure).getPalettes().get(0);
        palette.blocks().removeIf(x -> x.state().isAir());
        var newTag = structure.save(new CompoundTag());

        // Overwrite the existing tag.
        tag.getAllKeys().clear();
        for (var key : newTag.getAllKeys()) tag.put(key, newTag.get(key));
    }
}
