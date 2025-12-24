// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.details;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.test.core.CustomMatchers;
import dan200.computercraft.test.shared.WithMinecraft;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;

@WithMinecraft
class ItemDetailsTest {
    @BeforeAll
    public static void setup() {
        VanillaDetailRegistries.ITEM_STACK.addProvider(ItemDetails::fill);
    }

    /**
     * Test that all potion-imbued items (potions, throwables and arrows) have the correct duration.
     */
    @Test
    public void testPotionDurations() {
        var registries = VanillaRegistries.createLookup();
        assertThat(
            VanillaDetailRegistries.ITEM_STACK.getDetails(registries, PotionContents.createItemStack(Items.POTION, Potions.LONG_NIGHT_VISION)),
            containsEntryWith("potionEffects", contains(allOf(containsEntry("name", "minecraft:night_vision"), containsEntry("duration", 480.0))))
        );

        assertThat(
            VanillaDetailRegistries.ITEM_STACK.getDetails(registries, PotionContents.createItemStack(Items.LINGERING_POTION, Potions.LONG_NIGHT_VISION)),
            containsEntryWith("potionEffects", contains(allOf(containsEntry("name", "minecraft:night_vision"), containsEntry("duration", 120.0))))
        );

        assertThat(
            VanillaDetailRegistries.ITEM_STACK.getDetails(registries, PotionContents.createItemStack(Items.SPLASH_POTION, Potions.LONG_NIGHT_VISION)),
            containsEntryWith("potionEffects", contains(allOf(containsEntry("name", "minecraft:night_vision"), containsEntry("duration", 480.0))))
        );

        assertThat(
            VanillaDetailRegistries.ITEM_STACK.getDetails(registries, PotionContents.createItemStack(Items.TIPPED_ARROW, Potions.LONG_NIGHT_VISION)),
            containsEntryWith("potionEffects", contains(allOf(containsEntry("name", "minecraft:night_vision"), containsEntry("duration", 60.0))))
        );
    }

    private static Matcher<Map<? extends String, ?>> containsEntry(String key, Object value) {
        return CustomMatchers.containsEntry(key, value);
    }

    @SuppressWarnings("unchecked")
    private static Matcher<Map<? extends String, ?>> containsEntryWith(String key, Matcher<?> value) {
        return CustomMatchers.containsEntryWith(key, (Matcher<? super Object>) value);
    }
}
