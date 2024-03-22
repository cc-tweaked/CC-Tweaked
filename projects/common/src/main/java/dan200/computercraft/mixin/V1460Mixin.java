// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.mixin;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.peripheral.diskdrive.DiskDriveBlockEntity;
import dan200.computercraft.shared.peripheral.printer.PrinterBlockEntity;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.util.datafix.schemas.V1460;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Register our item-contianing block entities.
 *
 * @see DiskDriveBlockEntity
 * @see PrinterBlockEntity
 * @see TurtleBlockEntity
 */
@Mixin(V1460.class)
class V1460Mixin {
    @Inject(at = @At("RETURN"), method = "registerBlockEntities")
    @SuppressWarnings("UnusedMethod")
    private void registerBlockEntities(Schema schema, CallbackInfoReturnable<Map<String, Supplier<TypeTemplate>>> ci) {
        var map = ci.getReturnValue();

        // Basic inventories
        registerInventory(schema, map, ModRegistry.BlockEntities.TURTLE_NORMAL.id().toString());
        registerInventory(schema, map, ModRegistry.BlockEntities.TURTLE_ADVANCED.id().toString());
        registerInventory(schema, map, ModRegistry.BlockEntities.PRINTER.id().toString());

        // Disk drives contain a single item
        schema.register(map, ModRegistry.BlockEntities.DISK_DRIVE.id().toString(), () -> DSL.optionalFields(
            "Item", References.ITEM_STACK.in(schema)
        ));
    }

    @Shadow
    protected static void registerInventory(Schema schema, Map<String, Supplier<TypeTemplate>> map, String name) {
    }
}
