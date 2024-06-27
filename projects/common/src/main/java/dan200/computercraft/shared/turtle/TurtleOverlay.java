// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.turtle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.util.Holiday;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.RegistryFileCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nullable;

/**
 * A cosmetic overlay on a turtle.
 *
 * @param model          The path to the overlay's model.
 * @param showElfOverlay Whether this overlay is compatible with the {@linkplain #ELF_MODEL Christmas elf model}.
 * @see ModRegistry.DataComponents#OVERLAY
 */
public record TurtleOverlay(ResourceLocation model, boolean showElfOverlay) {
    /**
     * The registry turtle overlays are stored in.
     */
    public static final ResourceKey<Registry<TurtleOverlay>> REGISTRY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "turtle_overlay"));

    /**
     * The codec used to read/write turtle overlay definitions from datapacks.
     */
    public static final Codec<TurtleOverlay> DIRECT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("model").forGetter(TurtleOverlay::model),
        Codec.BOOL.optionalFieldOf("show_elf_overlay", false).forGetter(TurtleOverlay::showElfOverlay)
    ).apply(instance, TurtleOverlay::new));

    /**
     * The codec used for {@link TurtleOverlay} instances.
     *
     * @see ModRegistry.DataComponents#OVERLAY
     */
    public static final Codec<Holder<TurtleOverlay>> CODEC = RegistryFileCodec.create(REGISTRY, DIRECT_CODEC);

    /**
     * The stream codec used for {@link TurtleOverlay} instances.
     */
    public static final StreamCodec<RegistryFriendlyByteBuf, Holder<TurtleOverlay>> STREAM_CODEC = ByteBufCodecs.holder(REGISTRY, StreamCodec.composite(
        ResourceLocation.STREAM_CODEC, TurtleOverlay::model,
        ByteBufCodecs.BOOL, TurtleOverlay::showElfOverlay,
        TurtleOverlay::new
    ));

    /**
     * An additional overlay that is rendered on all turtles at {@linkplain Holiday#CHRISTMAS Christmas}.
     *
     * @see #showElfOverlay()
     * @see #showElfOverlay(TurtleOverlay, boolean)
     */
    public static final ResourceLocation ELF_MODEL = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_elf_overlay");

    /**
     * Determine whether we should show the {@linkplain #ELF_MODEL elf overlay}.
     *
     * @param overlay   The current {@link TurtleOverlay}.
     * @param christmas Whether it is Christmas.
     * @return Whether we should show the elf overlay.
     */
    public static boolean showElfOverlay(@Nullable TurtleOverlay overlay, boolean christmas) {
        return christmas && (overlay == null || overlay.showElfOverlay());
    }
}
