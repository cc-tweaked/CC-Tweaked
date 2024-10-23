// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.templates.TypeTemplate;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import dan200.computercraft.api.upgrades.UpgradeData;
import dan200.computercraft.impl.UpgradeManager;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.media.items.PrintoutData;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import net.minecraft.util.datafix.fixes.References;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Additional fixers to convert CC's item NBT to use components.
 *
 * @see ItemStackComponentizationFix
 */
public class ComponentizationFixers {
    private static final String TURTLE_NORMAL = "computercraft:turtle_normal";
    private static final String TURTLE_ADVANCED = "computercraft:turtle_advanced";

    private static final Set<String> COMPUTER = Set.of("computercraft:computer_normal", "computercraft:computer_advanced");
    private static final Set<String> TURTLES = Set.of(TURTLE_NORMAL, TURTLE_ADVANCED);
    private static final Set<String> POCKET_COMPUTERS = Set.of("computercraft:pocket_computer_normal", "computercraft:pocket_computer_advanced");

    private static final Set<String> ALL_COMPUTERS = Stream.of(COMPUTER, TURTLES, POCKET_COMPUTERS).flatMap(Set::stream).collect(Collectors.toUnmodifiableSet());

    private static final Set<String> PRINTOUTS = Set.of("computercraft:printed_page", "computercraft:printed_pages", "computercraft:printed_book");

    private static final String DISK = "computercraft:disk";
    private static final String TREASURE_DISK = "computercraft:treasure_disk";

    private static final Set<String> DYEABLE = Stream.concat(
        Stream.of(TURTLES, POCKET_COMPUTERS).flatMap(Set::stream),
        Stream.of(DISK)
    ).collect(Collectors.toUnmodifiableSet());

    /**
     * Fix components in item stacks.
     *
     * @param item The current item data.
     * @param ops  A {@link Dynamic} instance, used to create new {@link Dynamic} values.
     */
    public static void fixItemComponents(ItemStackComponentizationFix.ItemStackData item, Dynamic<?> ops) {
        // Set computer ID
        if (item.is(ALL_COMPUTERS)) item.moveTagToComponent("ComputerId", "computercraft:computer_id");

        // Set dyed colour
        if (item.is(DYEABLE)) moveColourToComponent(item, ops, "Color");

        if (item.is(POCKET_COMPUTERS)) {
            item.moveTagToComponent("On", "computercraft:on");

            moveUpgradeToComponent(item, ops, "Upgrade", "UpgradeInfo", "computercraft:pocket_upgrade");

            // Remove instance/session, so they don't end up in minecraft:custom_data.
            item.removeTag("InstanceId");
            item.removeTag("SessionId");
        }

        if (item.is(TURTLES)) {
            item.moveTagToComponent("Fuel", "computercraft:fuel");

            item.removeTag("Overlay").result().ifPresent(x -> item.setComponent("computercraft:overlay", fixOverlay(x)));

            moveUpgradeToComponent(item, ops, "LeftUpgrade", "LeftUpgradeNbt", "computercraft:left_turtle_upgrade");
            moveUpgradeToComponent(item, ops, "RightUpgrade", "RightUpgradeNbt", "computercraft:right_turtle_upgrade");
        }

        if (item.is(DISK)) item.moveTagToComponent("DiskId", "computercraft:disk_id");

        if (item.is(TREASURE_DISK)) {
            var name = item.removeTag("Title").asString().result();
            var path = item.removeTag("SubPath").asString().result();
            if (name.isPresent() && path.isPresent()) {
                item.setComponent("computercraft:treasure_disk", ops.emptyMap()
                    .set("name", ops.createString(name.get()))
                    .set("path", ops.createString(path.get())));
            }

            moveColourToComponent(item, ops, "Colour");
        }

        if (item.is(PRINTOUTS)) movePrintoutToComponent(item, ops);
    }

    private static void moveUpgradeToComponent(ItemStackComponentizationFix.ItemStackData data, Dynamic<?> ops, String key, String dataKey, String component) {
        // Rewrite {Upgrade:"foo",UpgradeData:...} to {cc:upgrade:{id:"foo", components:...}}
        var upgrade = data.removeTag(key).asString(null);
        if (upgrade == null) return;
        data.setComponent(component, createUpgradeData(ops, upgrade, data.removeTag(dataKey)));
    }

    private static void moveColourToComponent(ItemStackComponentizationFix.ItemStackData item, Dynamic<?> ops, String key) {
        item.removeTag(key).asNumber().result().map(Number::intValue).ifPresent(col ->
            item.setComponent("minecraft:dyed_color", ops.emptyMap()
                .set("rgb", ops.createInt(col))
                .set("show_in_tooltip", ops.createBoolean(false))
            ));
    }

    /**
     * Move printout data to a component.
     *
     * @param item The item data to convert.
     * @param ops  A {@link Dynamic} instance, for creating new dynamic values.
     * @see PrintoutData
     */
    private static void movePrintoutToComponent(ItemStackComponentizationFix.ItemStackData item, Dynamic<?> ops) {
        var title = item.removeTag("Title").asString("");
        var pages = item.removeTag("Pages").asInt(0);
        if (pages <= 0) return;

        List<Dynamic<?>> lines = new ArrayList<>(pages * PrintoutData.LINES_PER_PAGE);
        for (var i = 0; i < pages * PrintoutData.LINES_PER_PAGE; i++) {
            var text = item.removeTag("Text" + i).asString(PrintoutData.Line.EMPTY.text());
            var colour = item.removeTag("Color" + i).asString(PrintoutData.Line.EMPTY.foreground());
            lines.add(ops.emptyMap().set("text", ops.createString(text)).set("foreground", ops.createString(colour)));
        }

        item.setComponent("computercraft:printout", ops.emptyMap()
            .set("title", ops.createString(title))
            .set("lines", ops.createList(lines.stream())));
    }

    /**
     * Make a fixer for {@linkplain References#BLOCK_ENTITY block entities}.
     *
     * @param input  The input schema.
     * @param output The output schema.
     * @return A function that fixes block entities.
     */
    public static Function<Typed<?>, Typed<?>> makeBlockEntityRewrites(Schema input, Schema output) {
        return fixTurtleBlockEntity(input, output, TURTLE_NORMAL).compose(fixTurtleBlockEntity(input, output, TURTLE_ADVANCED));
    }

    private static Function<Typed<?>, Typed<?>> fixTurtleBlockEntity(Schema inputSchema, Schema outputSchema, String name) {
        var input = DSL.namedChoice(name, inputSchema.getChoiceType(References.BLOCK_ENTITY, name));
        var output = outputSchema.getChoiceType(References.BLOCK_ENTITY, name);

        return typed -> typed.updateTyped(input, output, typed2 -> typed2.update(DSL.remainderFinder(), x -> {
            x = moveUpgradeData(x, "LeftUpgrade", "LeftUpgradeNbt");
            x = moveUpgradeData(x, "RightUpgrade", "RightUpgradeNbt");
            x = x.update("Overlay", ComponentizationFixers::fixOverlay);
            return x;
        }));
    }

    private static Dynamic<?> moveUpgradeData(Dynamic<?> ops, String key, String dataKey) {
        // Rewrite {Upgrade:"foo",UpgradeData:...} to {Upgrade:{id:"foo", components:...}}
        var upgradeId = ops.get(key).asString(null);
        if (upgradeId == null) return ops;

        return ops.set(key, createUpgradeData(ops, upgradeId, ops.get(dataKey))).remove(dataKey);
    }

    private static Dynamic<?> fixOverlay(Dynamic<?> overlay) {
        // Rewrite known overlays to their new ids.
        var overlayId = overlay.asString(null);
        if (overlayId == null) return overlay;

        return switch (overlayId) {
            // Map known overlays to their new id.
            case "computercraft:block/turtle_trans_overlay" -> overlay.createString("computercraft:trans_flag");
            case "computercraft:block/turtle_rainbow_overlay" -> overlay.createString("computercraft:rainbow_flag");
            // And unknown overlays to a direct entry.
            default -> overlay.emptyMap().set("model", overlay);
        };
    }

    /**
     * Create a new upgrade data.
     *
     * @param ops         A {@link Dynamic} instance, for creating new dynamic values.
     * @param upgradeId   The upgrade ID
     * @param upgradeData Additional upgrade data
     * @return The newly created {@link UpgradeData}-compatible value.
     */
    private static Dynamic<?> createUpgradeData(Dynamic<?> ops, String upgradeId, OptionalDynamic<?> upgradeData) {
        var newUpgrade = ops.emptyMap().set("id", ops.createString(upgradeId));

        var data = upgradeData.result().orElse(null);
        if (data != null && !data.equals(ops.emptyMap())) {
            // Migrate all existing data to minecraft:custom_data
            newUpgrade = newUpgrade.set("components", ops.emptyMap().set("minecraft:custom_data", data));
        }

        return newUpgrade;
    }

    /**
     * Add our custom data components to the datafixer system.
     *
     * @param types  The component type definition.
     * @param schema The current schema.
     * @see UpgradeManager#upgradeDataCodec()
     * @see ModRegistry.DataComponents#POCKET_UPGRADE
     * @see ModRegistry.DataComponents#LEFT_TURTLE_UPGRADE
     * @see ModRegistry.DataComponents#RIGHT_TURTLE_UPGRADE
     */
    public static void addExtraTypes(Map<String, Supplier<TypeTemplate>> types, Schema schema) {
        // Create a codec for UpgradeData
        Supplier<TypeTemplate> upgradeData = () -> DSL.optionalFields("components", References.DATA_COMPONENTS.in(schema));

        types.put("computercraft:pocket_upgrade", upgradeData);
        types.put("computercraft:left_turtle_upgrade", upgradeData);
        types.put("computercraft:right_turtle_upgrade", upgradeData);
    }
}
