// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.DataFixerBuilder;
import com.mojang.datafixers.schemas.Schema;
import dan200.computercraft.shared.datafix.TurtleUpgradeComponentizationFix;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import net.minecraft.util.datafix.schemas.V3818_5;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DataFixers.class)
abstract class DataFixersMixin {
    /**
     * Register {@link TurtleUpgradeComponentizationFix} alongside {@link ItemStackComponentizationFix}.
     * <p>
     * We use a {@link ModifyArg} to capture the schema passed to {@link ItemStackComponentizationFix}. This is a
     * little gross, but is the easiest way to obtain the schema without hard-coding local ordinals.
     *
     * @param schema  The {@link V3818_5} schema.
     * @param builder The current datafixer builder
     * @return The input schema.
     */
    @ModifyArg(
        method = "addFixers",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/util/datafix/fixes/ItemStackComponentizationFix;<init>(Lcom/mojang/datafixers/schemas/Schema;)V"),
        index = 0,
        allow = 1
    )
    @SuppressWarnings("UnusedMethod")
    private static Schema addComponentizationFixes(Schema schema, @Local DataFixerBuilder builder) {
        assertSchemaVersion(schema, DataFixUtils.makeKey(3818, 5));
        builder.addFixer(new TurtleUpgradeComponentizationFix(schema));
        return schema;
    }

    @Unique
    private static void assertSchemaVersion(Schema schema, int version) {
        if (schema.getVersionKey() != version) {
            throw new IllegalStateException("Unexpected schema version. Expected " + version + ", got " + schema.getVersionKey());
        }
    }
}
