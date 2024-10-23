// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import dan200.computercraft.shared.util.ComponentizationFixers;
import net.minecraft.util.datafix.schemas.V3818_3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.SequencedMap;
import java.util.function.Supplier;

/**
 * Add our custom data components to the datafixer system.
 * <p>
 * This mixin is identical between Fabric and NeoForge aside from using a different method name.
 */
@Mixin(V3818_3.class)
class V3818_3Mixin {
    @ModifyReturnValue(method = "components", at = @At("TAIL"))
    @SuppressWarnings("UnusedMethod")
    private static SequencedMap<String, Supplier<TypeTemplate>> components(SequencedMap<String, Supplier<TypeTemplate>> types, Schema schema) {
        ComponentizationFixers.addExtraTypes(types, schema);
        return types;
    }
}
