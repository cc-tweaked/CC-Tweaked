// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import dan200.computercraft.shared.datafix.ComponentizationFixers;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlockEntity;
import dan200.computercraft.shared.peripheral.printer.PrinterBlockEntity;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.NamespacedSchema;
import net.minecraft.util.datafix.schemas.V1460;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Register our item-containing block entities.
 *
 * @see DiskDriveBlockEntity
 * @see PrinterBlockEntity
 * @see TurtleBlockEntity
 */
@Mixin(V1460.class)
abstract class V1460Mixin {
    @Inject(at = @At("RETURN"), method = "registerBlockEntities")
    @SuppressWarnings("UnusedMethod")
    private void registerBlockEntities(Schema schema, CallbackInfoReturnable<Map<String, Supplier<TypeTemplate>>> ci) {
        var map = ci.getReturnValue();

        // Basic inventories
        registerTurtle(schema, map, "computercraft:turtle_normal");
        registerTurtle(schema, map, "computercraft:turtle_advanced");
        registerInventory(schema, map, "computercraft:printer");

        // Disk drives contain a single item
        schema.register(map, "computercraft:disk_drive", () -> DSL.optionalFields(
            "Item", References.ITEM_STACK.in(schema)
        ));
    }

    private static TypeTemplate upgradeData(Schema schema, ComponentizationFixers.UpgradeKeys keys) {
        if (schema.getVersionKey() >= ComponentizationFixers.SCHEMA_VERSION) {
            // In newer versions this is represented in one field.
            return DSL.optional(DSL.field(keys.id(), ComponentizationFixers.upgradeDataType(schema)));
        } else {
            // Pre-1.20.5 we store the upgrade ID and item separately
            return DSL.and(
                DSL.optional(DSL.field(keys.id(), DSL.constType(NamespacedSchema.namespacedString()))),
                DSL.optional(DSL.field(keys.item(), References.ITEM_STACK.in(schema)))
            );
        }
    }

    @Unique
    private static void registerTurtle(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
        schema.register(map, name, () -> DSL.and(
            upgradeData(schema, ComponentizationFixers.TURTLE_UPGRADE_LEFT),
            upgradeData(schema, ComponentizationFixers.TURTLE_UPGRADE_RIGHT),
            DSL.optionalFields(
                "Items", DSL.list(References.ITEM_STACK.in(schema))
            )
        ));
    }

    @Shadow
    protected static void registerInventory(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
    }
}
