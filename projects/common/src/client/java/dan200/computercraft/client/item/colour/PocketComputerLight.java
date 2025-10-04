// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.item.colour;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.pocket.ClientPocketComputers;
import dan200.computercraft.client.pocket.PocketComputerData;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * An {@link ItemTintSource} that returns the pocket computer's {@linkplain PocketComputerData#getLightState() light
 * colour}.
 *
 * @param defaultColour The default colour, if the light is not currently on.
 */
public record PocketComputerLight(int defaultColour) implements ItemTintSource {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "pocket_computer_light");
    public static final MapCodec<PocketComputerLight> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        ExtraCodecs.RGB_COLOR_CODEC.fieldOf("default").forGetter(PocketComputerLight::defaultColour)
    ).apply(instance, PocketComputerLight::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity holder) {
        var computer = ClientPocketComputers.get(stack);
        return computer == null || computer.getLightState() == -1 ? defaultColour : ARGB.opaque(computer.getLightState());
    }

    @Override
    public MapCodec<? extends ItemTintSource> type() {
        return CODEC;
    }
}
