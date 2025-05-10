// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.datafix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.datafixers.types.Type;
import dan200.computercraft.shared.ModRegistry;
import net.minecraft.util.datafix.fixes.DataComponentRemainderFix;
import net.minecraft.util.datafix.fixes.FoodToConsumableFix;
import net.minecraft.util.datafix.fixes.References;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.function.Function;

/**
 * Renames {@code "computercraft:pocket_upgrade"} to {@link ModRegistry.DataComponents#BACK_POCKET_UPGRADE
 * "computercraft:back_pocket_upgrade"}.
 *
 * @see ComponentizationFixers#addComponents(Map, Schema)
 */
public final class RenamePocketComputerUpgradeFix extends DataFix {
    public static final int SCHEMA_VERSION = DataFixUtils.makeKey(4314, 0);

    public RenamePocketComputerUpgradeFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    /**
     * Make a rewrite rule to rename a component.
     * <p>
     * We use {@link #writeFixAndRead(String, Type, Type, Function)} rather than
     * {@link #fixTypeEverywhereTyped(String, Type, Function)}, as the types don't neatly line up. This is consistent
     * with what {@link FoodToConsumableFix} does.
     * <p>
     * {@link DataComponentRemainderFix} <em>does</em> use {@code fixTypeEverywhereTyped}. However, none of the
     * components it references are in the component map, so don't cause the type to change!
     *
     * @return The constructed rewrite rule.
     */
    @Override
    protected TypeRewriteRule makeRule() {
        return writeFixAndRead(
            "Pocket upgrade rename",
            getInputSchema().getType(References.DATA_COMPONENTS),
            getOutputSchema().getType(References.DATA_COMPONENTS),
            dynamic -> dynamic.renameField("computercraft:pocket_upgrade", "computercraft:back_pocket_upgrade")
        );
    }

    private static final Logger LOG = LoggerFactory.getLogger(RenamePocketComputerUpgradeFix.class);
}
