// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.loader.DirectoryTemplateSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Mixin(DirectoryTemplateSource.class)
class DirectoryTemplateSourceMixin {
    /**
     * Add missing {@link Blocks#AIR} to our "reduced" game test structures.
     *
     * @param id The structure to load.
     * @param ci The callback info.
     * @see NbtUtilsMixin Saving structures
     * @see DirectoryTemplateSource#load(Identifier)
     */
    @Inject(method = "load", at = @At(value = "RETURN"))
    @SuppressWarnings("unused")
    private void load(Identifier id, CallbackInfoReturnable<Optional<StructureTemplate>> ci) {
        ci.getReturnValue().ifPresent(DirectoryTemplateSourceMixin::addMissingAir);
    }

    private static void addMissingAir(StructureTemplate template) {
        var size = template.getSize();
        var palette = ((StructureTemplateAccessor) template).getPalettes().getFirst();

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
