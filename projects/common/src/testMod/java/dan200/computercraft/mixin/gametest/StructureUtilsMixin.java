// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Set;

/**
 * Add missing {@link Blocks#AIR} to our "reduced" game test structures.
 *
 * @see NbtUtilsMixin Saving structures
 * @see StructureUtils#getStructureTemplate(String, ServerLevel)
 */
@Mixin(StructureUtils.class)
abstract class StructureUtilsMixin {
    @Inject(
        method = "getStructureTemplate",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplateManager;readStructure(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/level/levelgen/structure/templatesystem/StructureTemplate;",
            shift = At.Shift.AFTER
        )
    )
    @SuppressWarnings("unused")
    private static void getStructureTemplate(String structureName, ServerLevel serverLevel, CallbackInfoReturnable<StructureTemplate> ci) {
        var template = ci.getReturnValue();
        var size = template.getSize();
        var palette = ((StructureTemplateAccessor) template).getPalettes().get(0);

        Set<BlockPos> positions = new HashSet<>();
        for (var x = 0; x < size.getX(); x++) {
            for (var y = 0; y < size.getY(); y++) {
                for (var z = 0; z < size.getZ(); z++) positions.add(new BlockPos(x, y, z));
            }
        }

        for (var block : palette.blocks()) positions.remove(block.pos());

        for (var pos : positions) {
            palette.blocks().add(new StructureTemplate.StructureBlockInfo(pos, Blocks.AIR.defaultBlockState(), null));
        }
    }
}
