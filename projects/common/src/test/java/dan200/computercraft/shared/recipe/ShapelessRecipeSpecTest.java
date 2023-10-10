// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.recipe;

import dan200.computercraft.test.shared.NetworkSupport;
import dan200.computercraft.test.shared.WithMinecraft;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

import static org.hamcrest.MatcherAssert.assertThat;

@WithMinecraft
public class ShapelessRecipeSpecTest {
    static {
        WithMinecraft.Setup.bootstrap(); // @Property doesn't run test lifecycle methods.
    }

    @Property
    public void testRoundTrip(@ForAll("recipe") ShapelessRecipeSpec spec) {
        var converted = NetworkSupport.roundTrip(spec, ShapelessRecipeSpec::toNetwork, ShapelessRecipeSpec::fromNetwork);
        assertThat("Recipes are equal", converted, RecipeEqualities.shapelessRecipeSpec.asMatcher(ShapelessRecipeSpec.class, spec));
    }

    @Provide
    Arbitrary<ShapelessRecipeSpec> recipe() {
        return RecipeArbitraries.shapelessRecipeSpec();
    }
}
