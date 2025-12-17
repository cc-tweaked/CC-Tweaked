// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

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

    /**
     * Add missing {@link Blocks#AIR} to our "reduced" game test structures.
     *
     * @param id The structure to load.
     * @param ci The callback info.
     * @see NbtUtilsMixin Saving structures
     * @see StructureTemplateManagerMixin#loadFromTestStructures(Identifier, CallbackInfoReturnable)
     */
    @Inject(method = "loadFromTestStructures", at = @At(value = "RETURN"))
    @SuppressWarnings("unused")
    private void loadFromTestStructures(Identifier id, CallbackInfoReturnable<Optional<StructureTemplate>> ci) {
        ci.getReturnValue().ifPresent(StructureTemplateManagerMixin::addMissingAir);
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
