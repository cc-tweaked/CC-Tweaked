// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.details;

import dan200.computercraft.api.detail.VanillaDetailRegistries;
import dan200.computercraft.shared.platform.RegistryWrappers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.Map;

/**
 * Data providers for entities.
 *
 * @see VanillaDetailRegistries#ENTITY
 */
public class EntityDetails {
    public static void fillBasic(Map<? super String, Object> data, Entity entity) {
        data.put("name", DetailHelpers.getId(RegistryWrappers.ENTITY_TYPES, entity.getType()));
    }

    public static void fill(Map<? super String, Object> data, Entity entity) {
        data.put("displayName", entity.getName().getString());

        data.put("tags", DetailHelpers.getTags(entity.getType().builtInRegistryHolder()));

        if (entity instanceof LivingEntity living) {
            data.put("health", living.getHealth());
            data.put("maxHealth", living.getMaxHealth());
        }
    }
}
