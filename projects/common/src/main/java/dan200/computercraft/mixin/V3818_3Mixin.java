// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.datafixers.util.Pair;
import dan200.computercraft.impl.UpgradeManager;
import dan200.computercraft.shared.ModRegistry.DataComponents;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.V3818_3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Add our custom data components to the datafixer system.
 *
 * @see UpgradeManager#codec()
 * @see DataComponents#POCKET_UPGRADE
 * @see DataComponents#LEFT_TURTLE_UPGRADE
 * @see DataComponents#RIGHT_TURTLE_UPGRADE
 * @see ItemStackComponentizationFixMixin
 */
@Mixin(V3818_3.class)
class V3818_3Mixin {
    @ModifyReturnValue(
        method = "method_57277",
        at = @At("TAIL")
    )
    @SuppressWarnings("UnusedMethod")
    private static TypeTemplate addExtraTypes(TypeTemplate type, Schema schema) {
        // Create a codec for UpgradeData
        var upgradeData = DSL.optionalFields("components", References.DATA_COMPONENTS.in(schema));

        return extraOptionalFields(type,
            Pair.of("computercraft:pocket_upgrade", upgradeData),
            Pair.of("computercraft:left_turtle_upgrade", upgradeData),
            Pair.of("computercraft:right_turtle_upgrade", upgradeData)
        );
    }

    @SafeVarargs
    @SuppressWarnings("varargs")
    private static TypeTemplate extraOptionalFields(TypeTemplate base, Pair<String, TypeTemplate>... fields) {
        return DSL.and(Stream.concat(
            Arrays.stream(fields).map(entry -> DSL.optional(DSL.field(entry.getFirst(), entry.getSecond()))),
            Stream.of(base)
        ).toList());
    }
}
