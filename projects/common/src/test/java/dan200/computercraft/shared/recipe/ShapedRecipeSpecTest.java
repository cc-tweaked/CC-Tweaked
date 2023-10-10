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
public class ShapedRecipeSpecTest {
    static {
        WithMinecraft.Setup.bootstrap(); // @Property doesn't run test lifecycle methods.
    }

    @Property
    public void testRoundTrip(@ForAll("recipe") ShapedRecipeSpec spec) {
        var converted = NetworkSupport.roundTrip(spec, ShapedRecipeSpec::toNetwork, ShapedRecipeSpec::fromNetwork);
        assertThat("Recipes are equal", converted, RecipeEqualities.shapedRecipeSpec.asMatcher(ShapedRecipeSpec.class, spec));
    }

    @Provide
    Arbitrary<ShapedRecipeSpec> recipe() {
        return RecipeArbitraries.shapedRecipeSpec();
    }
}
