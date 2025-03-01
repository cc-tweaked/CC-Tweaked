// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.datafix;

import com.mojang.datafixers.DataFix;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import net.minecraft.util.datafix.fixes.References;

/**
 * Rewrites turtle block entities to store upgrades as components.
 *
 * @see ComponentizationFixers#makeBlockEntityRewrites(Schema, Schema)
 */
public class TurtleUpgradeComponentizationFix extends DataFix {
    public TurtleUpgradeComponentizationFix(Schema outputSchema) {
        super(outputSchema, true);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return fixTypeEverywhereTyped(
            "Turtle upgrade componentization",
            getInputSchema().getType(References.BLOCK_ENTITY),
            getOutputSchema().getType(References.BLOCK_ENTITY),
            ComponentizationFixers.makeBlockEntityRewrites(getInputSchema(), getOutputSchema())
        );
    }
}
