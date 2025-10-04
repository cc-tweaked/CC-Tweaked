// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.turtle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.api.client.StandaloneModel;
import dan200.computercraft.shared.ModRegistry;
import dan200.computercraft.shared.util.Holiday;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.ResourceLocation;

/**
 * A cosmetic overlay on a turtle.
 *
 * @param model          The path to the overlay's model.
 * @param showElfOverlay Whether this overlay is compatible with the {@linkplain #ELF_MODEL Christmas elf model}.
 * @see ModRegistry.DataComponents#OVERLAY
 */
public record TurtleOverlay(StandaloneModel model, boolean showElfOverlay) {
    /**
     * The folder where upgrades are loaded from.
     */
    public static final String SOURCE = ComputerCraftAPI.MOD_ID + "/turtle_overlay";

    /**
     * The codec used to read/write turtle overlay definitions from resource packs.
     */
    public static final Codec<TurtleOverlay.Unbaked> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ResourceLocation.CODEC.fieldOf("model").forGetter(TurtleOverlay.Unbaked::model),
        Codec.BOOL.optionalFieldOf("show_elf_overlay", false).forGetter(TurtleOverlay.Unbaked::showElfOverlay)
    ).apply(instance, TurtleOverlay.Unbaked::new));

    /**
     * An additional overlay that is rendered on all turtles at {@linkplain Holiday#CHRISTMAS Christmas}.
     *
     * @see #showElfOverlay()
     */
    public static final ResourceLocation ELF_MODEL = ResourceLocation.fromNamespaceAndPath(ComputerCraftAPI.MOD_ID, "block/turtle_elf_overlay");

    public record Unbaked(ResourceLocation model, boolean showElfOverlay) implements ResolvableModel {
        @Override
        public void resolveDependencies(Resolver resolver) {
            resolver.markDependency(model());
        }

        public TurtleOverlay bake(ModelBaker baker) {
            return new TurtleOverlay(StandaloneModel.of(model(), baker), showElfOverlay());
        }
    }
}
