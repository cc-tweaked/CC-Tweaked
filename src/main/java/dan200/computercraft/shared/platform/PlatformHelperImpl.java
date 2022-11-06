/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import com.google.auto.service.AutoService;
import dan200.computercraft.impl.PlatformHelper;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.RegistryManager;

import javax.annotation.Nullable;

@AutoService(PlatformHelper.class)
public class PlatformHelperImpl implements PlatformHelper {
    @Override
    public <T> ResourceLocation getRegistryKey(ResourceKey<Registry<T>> registry, T object) {
        var key = RegistryManager.ACTIVE.getRegistry(registry).getKey(object);
        if (key == null) throw new IllegalArgumentException(object + " was not registered in " + registry);
        return key;
    }

    @Override
    public <T> T getRegistryObject(ResourceKey<Registry<T>> registry, ResourceLocation id) {
        var value = RegistryManager.ACTIVE.getRegistry(registry).getValue(id);
        if (value == null) throw new IllegalArgumentException(id + " was not registered in " + registry);
        return value;
    }

    @Nullable
    @Override
    public CompoundTag getShareTag(ItemStack item) {
        return item.getShareTag();
    }
}
