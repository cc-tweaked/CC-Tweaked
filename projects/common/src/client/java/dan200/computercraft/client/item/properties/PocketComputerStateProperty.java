// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.item.properties;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.shared.computer.core.ComputerState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A {@link SelectItemModelProperty} that returns the pocket computer's current state.
 */
public final class PocketComputerStateProperty implements SelectItemModelProperty<ComputerState> {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_state");
    private static final PocketComputerStateProperty INSTANCE = new PocketComputerStateProperty();
    public static final MapCodec<PocketComputerStateProperty> CODEC = MapCodec.unit(INSTANCE);
    public static final Type<PocketComputerStateProperty, ComputerState> TYPE = Type.create(CODEC, ComputerState.CODEC);

    private PocketComputerStateProperty() {
    }

    public static PocketComputerStateProperty create() {
        return INSTANCE;
    }

    @Override
    public ComputerState get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity holder, int i, ItemDisplayContext context) {
        var computer = ClientPocketComputers.get(stack);
        return computer == null ? ComputerState.OFF : computer.getState();
    }

    @Override
    public Codec<ComputerState> valueCodec() {
        return ComputerState.CODEC;
    }

    @Override
    public Type<? extends SelectItemModelProperty<ComputerState>, ComputerState> type() {
        return TYPE;
    }
}
