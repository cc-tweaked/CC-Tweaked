// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Patches {@link NbtUtils#structureToSnbt(CompoundTag)} to remove air blocks from the structure file. This
 * significantly reduces the size of our generated templates.
 *
 * @see DirectoryTemplateSourceMixin Loading structures
 * @see NbtUtils#structureToSnbt(CompoundTag)
 */
@Mixin(NbtUtils.class)
class NbtUtilsMixin {
    @Inject(method = "structureToSnbt", at = @At("HEAD"))
    @SuppressWarnings("unused")
    private static void structureToSnbt(CompoundTag tag, CallbackInfoReturnable<String> ci) {
        // Load in the structure, strip out air, then save it back again.
        var structure = new StructureTemplate();
        var version = NbtUtils.getDataVersion(tag, 500);
        structure.load(BuiltInRegistries.BLOCK, DataFixTypes.STRUCTURE.updateToCurrentVersion(DataFixers.getDataFixer(), tag, version));

        var palette = ((StructureTemplateAccessor) structure).getPalettes().get(0);
        palette.blocks().removeIf(x -> x.state().isAir());
        var newTag = structure.save(new CompoundTag());

        // Overwrite the existing tag.
        tag.keySet().clear();
        for (var entry : newTag.entrySet()) tag.put(entry.getKey(), entry.getValue());
    }
}
