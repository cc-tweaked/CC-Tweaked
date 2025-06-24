// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.datafix;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.TypeRewriteRule;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import net.minecraft.util.datafix.fixes.AbstractUUIDFix;
import net.minecraft.util.datafix.fixes.References;

/**
 * A {@link com.mojang.datafixers.DataFix} that updates the turtle owner's {@link GameProfile} to use one more
 * consistent with the rest of the game.
 */
public final class TurtleOwnerFix extends AbstractUUIDFix {
    public TurtleOwnerFix(Schema outputSchema) {
        super(outputSchema, References.BLOCK_ENTITY);
    }

    @Override
    protected TypeRewriteRule makeRule() {
        return this.fixTypeEverywhereTyped("BlockEntityUUIDFix", getInputSchema().getType(typeReference), typed -> {
            typed = updateNamedChoice(typed, "computercraft:turtle_normal", TurtleOwnerFix::updateTurtle);
            return updateNamedChoice(typed, "computercraft:turtle_advanced", TurtleOwnerFix::updateTurtle);
        });
    }

    private static Dynamic<?> updateTurtle(Dynamic<?> turtle) {
        return turtle.update("Owner", profile -> profile
            .renameField("Name", "name")
            .remove("LowerId").remove("UpperId")
            .setFieldIfPresent("id", createUUIDFromLongs(profile, "UpperId", "LowerId"))
        );
    }
}
