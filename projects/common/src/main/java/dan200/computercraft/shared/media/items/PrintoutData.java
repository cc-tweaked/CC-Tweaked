// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.shared.media.items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dan200.computercraft.core.terminal.Terminal;
import dan200.computercraft.shared.ModRegistry;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.component.DataComponentHolder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Arrays;
import java.util.List;

/**
 * The contents of a printout.
 *
 * @param title The title of this printout.
 * @param lines A list of lines for this printout.
 * @see PrintoutItem
 * @see dan200.computercraft.shared.ModRegistry.DataComponents#PRINTOUT
 */
public record PrintoutData(String title, List<Line> lines) {
    public static final int LINE_LENGTH = 25;
    public static final int LINES_PER_PAGE = 21;
    public static final int MAX_PAGES = 16;

    /**
     * An empty printout. This has no title, and is a single page of empty lines.
     */
    public static final PrintoutData EMPTY;

    static {
        var lines = new Line[LINES_PER_PAGE];
        Arrays.fill(lines, Line.EMPTY);
        EMPTY = new PrintoutData("", List.of(lines));
    }

    private static final Codec<String> LINE_TEXT = Codec.STRING.validate(x -> x.length() == LINE_LENGTH
        ? DataResult.success(x)
        : DataResult.error(() -> "Expected string of length " + LINE_LENGTH));

    private static final Codec<Line> LINE_CODEC = RecordCodecBuilder.<Line>create(s -> s.group(
        LINE_TEXT.fieldOf("text").forGetter(Line::text),
        LINE_TEXT.fieldOf("foreground").forGetter(Line::foreground)
    ).apply(s, Line::new));

    private static final StreamCodec<ByteBuf, Line> LINE_STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, Line::text,
        ByteBufCodecs.STRING_UTF8, Line::foreground,
        Line::new
    );

    public static final Codec<PrintoutData> CODEC = RecordCodecBuilder.<PrintoutData>create(s -> s.group(
        Codec.STRING.optionalFieldOf("title", "").forGetter(PrintoutData::title),
        LINE_CODEC.listOf(1, MAX_PAGES * LINES_PER_PAGE)
            .validate(PrintoutData::validateLines)
            .fieldOf("lines").forGetter(PrintoutData::lines)
    ).apply(s, PrintoutData::new));

    public static final StreamCodec<ByteBuf, PrintoutData> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, PrintoutData::title,
        LINE_STREAM_CODEC.apply(ByteBufCodecs.list(MAX_PAGES * LINES_PER_PAGE)), PrintoutData::lines,
        PrintoutData::new
    );

    /**
     * A single line on our printed pages.
     *
     * @param text       The text for this line.
     * @param foreground The foreground colour of this line, in a format equivalent to {@link Terminal#getTextColourLine(int)}.
     */
    public record Line(String text, String foreground) {
        public static final Line EMPTY = new Line(" ".repeat(LINE_LENGTH), "f".repeat(LINE_LENGTH));

        public Line {
            if (text.length() != LINE_LENGTH) throw new IllegalArgumentException("text is of wrong length");
            if (foreground.length() != LINE_LENGTH) throw new IllegalArgumentException("foreground is of wrong length");
        }
    }

    public PrintoutData {
        validateLines(lines).getOrThrow(IllegalArgumentException::new);
    }

    private static DataResult<List<Line>> validateLines(List<Line> lines) {
        if (lines.isEmpty()) return DataResult.error(() -> "Expected non-empty list of lines");
        if ((lines.size() % LINES_PER_PAGE) != 0) return DataResult.error(() -> "Not enough lines for a page");
        if (lines.size() > LINES_PER_PAGE * MAX_PAGES) return DataResult.error(() -> "Too many pages");
        return DataResult.success(lines);
    }

    public static PrintoutData getOrEmpty(DataComponentHolder holder) {
        return holder.getOrDefault(ModRegistry.DataComponents.PRINTOUT.get(), EMPTY);
    }

    /**
     * Get the number of pages in this printout.
     *
     * @return The number of pages.
     */
    public int pages() {
        return Math.ceilDiv(lines.size(), LINES_PER_PAGE);
    }
}
