// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.computer.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;

/**
 * The size of a computer terminal.
 *
 * @param width  The terminal's width.
 * @param height The terminal's height.
 */
public record TerminalSize(int width, int height) {
    public static final int MAX_SIZE = 255;
    public static final Codec<TerminalSize> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ExtraCodecs.intRange(1, MAX_SIZE).fieldOf("width").forGetter(TerminalSize::width),
        ExtraCodecs.intRange(1, MAX_SIZE).fieldOf("height").forGetter(TerminalSize::height)
    ).apply(instance, TerminalSize::new));

    public static final StreamCodec<ByteBuf, TerminalSize> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, TerminalSize::width,
        ByteBufCodecs.VAR_INT, TerminalSize::height,
        TerminalSize::new
    );

    public TerminalSize {
        checkBounds("width", width);
        checkBounds("height", height);
    }

    private static void checkBounds(String name, int value) {
        if (value < 1 || value > MAX_SIZE) {
            throw new IllegalArgumentException(name + " must be between 1 and " + MAX_SIZE);
        }
    }
}
