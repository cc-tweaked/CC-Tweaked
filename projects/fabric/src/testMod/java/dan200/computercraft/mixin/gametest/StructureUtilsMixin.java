/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.mixin.gametest;

import net.minecraft.gametest.framework.StructureUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Undo Fabric's mixin which ignores {@link StructureUtils#testStructuresDir}.
 */
@Mixin(value = StructureUtils.class, priority = 0)
public class StructureUtilsMixin {
    // TODO: Replace with https://github.com/FabricMC/fabric/pull/2555 if merged.

    @Inject(method = "getStructureTemplate", at = @At("HEAD"), cancellable = true)
    @SuppressWarnings("UnusedMethod")
    private static void getStructureTemplate(String structureName, ServerLevel serverLevel, CallbackInfoReturnable<StructureTemplate> result) {
        result.setReturnValue(getStructureTemplateImpl(structureName, serverLevel));
    }

    @Unique
    private static StructureTemplate getStructureTemplateImpl(String structureName, ServerLevel serverLevel) {
        var structureTemplateManager = serverLevel.getStructureManager();

        var structureId = new ResourceLocation(structureName);
        var resourceStructure = structureTemplateManager.get(structureId);
        if (resourceStructure.isPresent()) {
            return resourceStructure.get();
        } else {
            var path = Paths.get(StructureUtils.testStructuresDir, structureId.getPath() + ".snbt");
            var structureInfo = tryLoadStructure(path);
            if (structureInfo == null) {
                throw new RuntimeException("Could not find structure file " + path + ", and the structure is not available in the world structures either.");
            }

            return structureTemplateManager.readStructure(structureInfo);
        }
    }

    @Shadow
    private static CompoundTag tryLoadStructure(Path pathToStructure) {
        throw new IllegalArgumentException("Uncallable");
    }
}
