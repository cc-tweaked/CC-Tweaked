// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared;

import com.mojang.serialization.Codec;
import dan200.computercraft.shared.loot.InjectLootTableModifier;
import dan200.computercraft.shared.platform.PlatformHelper;
import dan200.computercraft.shared.platform.RegistrationHelper;
import dan200.computercraft.shared.platform.RegistryEntry;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * {@link ModRegistry} equivalent for Forge-specific content.
 */
public final class ForgeModRegistry {
    private ForgeModRegistry() {
    }

    public static final class Codecs {
        static final RegistrationHelper<Codec<? extends IGlobalLootModifier>> REGISTRY = PlatformHelper.get().createRegistrationHelper(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS);

        public static final RegistryEntry<Codec<InjectLootTableModifier>> INJECT_LOOT_TABLE = REGISTRY.register("inject_loot_table", InjectLootTableModifier::createCodec);
    }

    public static void register() {
        Codecs.REGISTRY.register();
    }
}
