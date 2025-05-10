// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.util;

import com.mojang.serialization.Codec;
import dan200.computercraft.api.ComputerCraftAPI;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.IntFunction;

/**
 * A non-negative integer id, used for computer and disk ids.
 *
 * @see dan200.computercraft.shared.ModRegistry.DataComponents#COMPUTER_ID
 * @see dan200.computercraft.shared.ModRegistry.DataComponents#DISK_ID
 */
public abstract class NonNegativeId implements TooltipProvider {
    private final int id;

    protected NonNegativeId(int id) {
        if (id < 0) throw new IllegalArgumentException("ID must be >= 0");
        this.id = id;
    }

    /**
     * Get the internal id.
     *
     * @return The internal id.
     */
    public int id() {
        return id;
    }

    public static int getId(@Nullable NonNegativeId id) {
        return id == null ? -1 : id.id();
    }

    public static <T extends NonNegativeId> int getOrCreate(MinecraftServer server, ItemStack stack, DataComponentType<T> component, IntFunction<T> create, String type) {
        var id = stack.get(component);
        if (id != null) return id.id();

        var newId = ComputerCraftAPI.createUniqueNumberedSaveDir(server, type);
        stack.set(component, create.apply(newId));
        return newId;
    }

    protected void addToTooltip(String translation, Consumer<Component> out) {
        out.accept(Component.translatable(translation, id()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public String toString() {
        var className = getClass().getName();
        return className.substring(className.lastIndexOf('.') + 1) + "(" + id + ")";
    }

    @Override
    @SuppressWarnings("EqualsGetClass") // We want to distinguish different subclasses.
    public final boolean equals(Object o) {
        return this == o || (o != null && getClass() == o.getClass() && id == ((NonNegativeId) o).id);
    }

    @Override
    public final int hashCode() {
        return id;
    }

    public static final class Computer extends NonNegativeId {
        public static final Codec<Computer> CODEC = ExtraCodecs.NON_NEGATIVE_INT.xmap(Computer::new, NonNegativeId::id);
        public static final StreamCodec<ByteBuf, Computer> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Computer::new, NonNegativeId::id);

        public Computer(int id) {
            super(id);
        }

        @Override
        public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> out, TooltipFlag flags, DataComponentGetter stack) {
            if (flags.isAdvanced() || stack.get(DataComponents.CUSTOM_NAME) == null) {
                addToTooltip("gui.computercraft.tooltip.computer_id", out);
            }
        }
    }

    public static final class Disk extends NonNegativeId {
        public static final Codec<Disk> CODEC = ExtraCodecs.NON_NEGATIVE_INT.xmap(Disk::new, NonNegativeId::id);
        public static final StreamCodec<ByteBuf, Disk> STREAM_CODEC = ByteBufCodecs.VAR_INT.map(Disk::new, NonNegativeId::id);

        public Disk(int id) {
            super(id);
        }

        @Override
        public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> out, TooltipFlag flags, DataComponentGetter stack) {
            if (flags.isAdvanced()) addToTooltip("gui.computercraft.tooltip.disk_id", out);
        }
    }
}
