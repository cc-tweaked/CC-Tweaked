// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import dan200.computercraft.shared.util.ComponentizationFixers;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import net.minecraft.util.datafix.fixes.References;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Migrates CC's item NBT to use components.
 *
 * @see V3818_3Mixin
 * @see ComponentizationFixers
 */
@Mixin(ItemStackComponentizationFix.class)
abstract class ItemStackComponentizationFixMixin extends DataFix {
    @SuppressWarnings("UnusedMethod")
    private ItemStackComponentizationFixMixin(Schema outputSchema, boolean changesType) {
        super(outputSchema, changesType);
    }

    @Inject(method = "fixItemStack", at = @At("TAIL"))
    @SuppressWarnings("unused")
    private static void fixItemStack(ItemStackComponentizationFix.ItemStackData data, Dynamic<?> ops, CallbackInfo ci) {
        ComponentizationFixers.fixItemComponents(data, ops);
    }

    @ModifyReturnValue(method = "makeRule", at = @At("RETURN"), remap = false)
    @SuppressWarnings("UnusedMethod")
    private TypeRewriteRule wrapMakeRule(TypeRewriteRule existing) {
        return TypeRewriteRule.seq(existing, fixTypeEverywhereTyped(
            "Turtle upgrade componentization",
            getInputSchema().getType(References.BLOCK_ENTITY),
            getOutputSchema().getType(References.BLOCK_ENTITY),
            ComponentizationFixers.makeBlockEntityRewrites(getInputSchema(), getOutputSchema())
        ));
    }
}
