/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2022. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.shared.platform;

import com.google.auto.service.AutoService;
import dan200.computercraft.api.ComputerCraftAPI;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

@AutoService(dan200.computercraft.impl.PlatformHelper.class)
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

    @Override
    public <T> Registries.RegistryWrapper<T> wrap(ResourceKey<Registry<T>> key) {
        return new RegistryWrapperImpl<>(key.location(), RegistryManager.ACTIVE.getRegistry(key));
    }

    @Override
    public <T> RegistrationHelper<T> createRegistrationHelper(ResourceKey<Registry<T>> registry) {
        return new RegistrationHelperImpl<>(DeferredRegister.create(registry, ComputerCraftAPI.MOD_ID));
    }

    @Nullable
    @Override
    public <K> K tryGetRegistryObject(ResourceKey<Registry<K>> registry, ResourceLocation id) {
        return RegistryManager.ACTIVE.getRegistry(registry).getValue(id);
    }

    @Override
    public <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(BiFunction<BlockPos, BlockState, T> factory, Block block) {
        return new BlockEntityType<>(factory::apply, Set.of(block), null);
    }

    @Nullable
    @Override
    public CompoundTag getShareTag(ItemStack item) {
        return item.getShareTag();
    }

    private record RegistryWrapperImpl<T>(
        ResourceLocation name, ForgeRegistry<T> registry
    ) implements Registries.RegistryWrapper<T> {
        @Override
        public int getId(T object) {
            var id = registry.getID(object);
            if (id == -1) throw new IllegalStateException(object + " was not registered in " + name);
            return id;
        }

        @Override
        public ResourceLocation getKey(T object) {
            var key = registry.getKey(object);
            if (key == null) throw new IllegalStateException(object + " was not registered in " + name);
            return key;
        }

        @Override
        public T get(ResourceLocation location) {
            var object = registry.getValue(location);
            if (object == null) throw new IllegalStateException(location + " was not registered in " + name);
            return object;
        }

        @Nullable
        @Override
        public T tryGet(ResourceLocation location) {
            return registry.getValue(location);
        }

        @Override
        public T get(int id) {
            var object = registry.getValue(id);
            if (object == null) throw new IllegalStateException(id + " was not registered in " + name);
            return object;
        }

        @Override
        public Iterator<T> iterator() {
            return registry.iterator();
        }
    }

    private record RegistrationHelperImpl<T>(DeferredRegister<T> registry) implements RegistrationHelper<T> {
        @Override
        public <U extends T> RegistryEntry<U> register(String name, Supplier<U> create) {
            return new RegistryEntryImpl<>(registry.register(name, create));
        }

        @Override
        public void register() {
            registry.register(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }

    private record RegistryEntryImpl<T>(RegistryObject<T> object) implements RegistryEntry<T> {
        @Override
        public ResourceLocation id() {
            return object().getId();
        }

        @Override
        public T get() {
            return object.get();
        }
    }
}
