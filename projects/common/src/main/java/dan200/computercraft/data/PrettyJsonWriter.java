// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonWriter;
import net.minecraft.data.DataProvider;
import net.minecraft.util.GsonHelper;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Alternative version of {@link JsonWriter} which attempts to lay out the JSON in a more compact format.
 *
 * @see PrettyDataProvider
 */
public class PrettyJsonWriter extends JsonWriter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final int MAX_WIDTH = 120;

    private final Writer out;

    /**
     * A stack of objects. This is either a {@link String} (in which case we've received an object key but no value)
     * or a {@link DocList} (which either represents an array or object).
     */
    private final Deque<Object> stack = new ArrayDeque<>();

    public PrettyJsonWriter(Writer out) {
        super(out);
        this.out = out;
    }

    /**
     * Reformat a JSON string with our pretty printer.
     *
     * @param contents The string to reformat.
     * @return The reformatted string.
     */
    public static byte[] reformat(byte[] contents) {
        JsonElement object;
        try (var reader = new InputStreamReader(new ByteArrayInputStream(contents), StandardCharsets.UTF_8)) {
            object = GSON.fromJson(reader, JsonElement.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (JsonSyntaxException e) {
            return contents;
        }

        var out = new ByteArrayOutputStream();
        try (var writer = new PrettyJsonWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            GsonHelper.writeValue(writer, object, DataProvider.KEY_COMPARATOR);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        out.write('\n');

        return out.toByteArray();
    }

    private void pushValue(Object object) throws IOException {
        // We've popped our top object, just write a value.
        if (stack.isEmpty()) {
            write(out, object, MAX_WIDTH, 0);
            return;
        }

        // Otherwise we either need to push to our list or finish a record pair.
        var head = stack.getLast();
        if (head instanceof DocList) {
            ((DocList) head).add(object);
        } else {
            stack.removeLast();
            ((DocList) stack.getLast()).add(new Pair((String) head, object));
        }
    }

    @Override
    public JsonWriter beginArray() {
        stack.add(new DocList("[", "]"));
        return this;
    }

    @Override
    public JsonWriter endArray() throws IOException {
        var list = (DocList) stack.removeLast();
        pushValue(list);
        return this;
    }

    @Override
    public JsonWriter beginObject() {
        stack.add(new DocList("{", "}"));
        return this;
    }

    @Override
    public JsonWriter endObject() throws IOException {
        return endArray();
    }

    @Override
    public JsonWriter name(String name) throws IOException {
        stack.add(escapeString(name));
        return this;
    }

    @Override
    public JsonWriter jsonValue(String value) throws IOException {
        pushValue(value);
        return this;
    }

    @Override
    public JsonWriter value(@Nullable String value) throws IOException {
        return value == null ? nullValue() : jsonValue(escapeString(value));
    }

    @Override
    public JsonWriter nullValue() throws IOException {
        if (!getSerializeNulls() && stack.peekLast() instanceof String) {
            stack.removeLast();
            return this;
        }

        return jsonValue("null");
    }

    @Override
    public JsonWriter value(boolean value) throws IOException {
        return jsonValue(Boolean.toString(value));
    }

    @Override
    public JsonWriter value(@Nullable Boolean value) throws IOException {
        return value == null ? nullValue() : jsonValue(Boolean.toString(value));
    }

    @Override
    public JsonWriter value(double value) throws IOException {
        return jsonValue(Double.toString(value));
    }

    @Override
    public JsonWriter value(long value) throws IOException {
        return jsonValue(Long.toString(value));
    }

    @Override
    public JsonWriter value(@Nullable Number value) throws IOException {
        return value == null ? nullValue() : jsonValue(value.toString());
    }

    @Override
    public void close() throws IOException {
        if (!stack.isEmpty()) throw new IllegalArgumentException("Object is remaining on the stack");
        out.close();
    }

    /**
     * A key/value pair inside a JSON object.
     *
     * @param key   The escaped object key.
     * @param value The object value.
     */
    private record Pair(String key, Object value) {
        int width() {
            return key.length() + 2 + PrettyJsonWriter.width(value);
        }

        int write(Writer out, int space, int indent) throws IOException {
            out.write(key);
            out.write(": ");
            return PrettyJsonWriter.write(out, value, space - key.length() - 2, indent);
        }
    }

    /**
     * A list of terms inside a JSON document. Either an array or a JSON object.
     */
    private static class DocList {
        final String prefix;
        final String suffix;
        final List<Object> contents = new ArrayList<>();
        int width;

        DocList(String prefix, String suffix) {
            this.prefix = prefix;
            this.suffix = suffix;
            width = prefix.length() + suffix.length();
        }

        void add(Object value) {
            contents.add(value);
            width += width(value) + (contents.isEmpty() ? 0 : 2);
        }

        int write(Writer writer, int space, int indent) throws IOException {
            writer.append(prefix);
            if (width <= space) {
                // We've sufficient room on this line, so write everything on one line.

                // Take into account the suffix length here, as we ignore it the case we wrap.
                space -= prefix.length() + suffix.length();

                var comma = false;
                for (var value : contents) {
                    if (comma) {
                        writer.append(", ");
                        space -= 2;
                    }
                    comma = true;

                    space = PrettyJsonWriter.write(writer, value, space, indent);
                }
            } else {
                // We've run out of room, so write each value on separate lines.
                var indentStr = " ".repeat(indent);
                writer.append("\n  ").append(indentStr);

                var comma = false;
                for (var value : contents) {
                    if (comma) {
                        writer.append(",\n  ").append(indentStr);
                    }
                    comma = true;

                    PrettyJsonWriter.write(writer, value, MAX_WIDTH - indent - 2, indent + 2);
                }
                writer.append("\n").append(indentStr);
            }

            writer.append(suffix);
            return space;
        }
    }

    /**
     * Estimate the width of an object.
     *
     * @param object The object to emit.
     * @return The computed width.
     */
    private static int width(Object object) {
        if (object instanceof String string) return string.length();
        if (object instanceof DocList list) return list.width;
        if (object instanceof Pair pair) return pair.width();
        throw new IllegalArgumentException("Not a valid document");
    }

    /**
     * Write a value to the output stream.
     *
     * @param writer The writer to emit to.
     * @param object The object to write.
     * @param space  The amount of space left on this line. Will be no larger than {@link #MAX_WIDTH}, but may be negative.
     * @param indent The current indent.
     * @return The new amount of space left on this line. This is undefined if the writer wraps.
     * @throws IOException If the underlying writer fails.
     */
    private static int write(Writer writer, Object object, int space, int indent) throws IOException {
        if (object instanceof String str) {
            writer.write(str);
            return space - str.length();
        } else if (object instanceof DocList list) {
            return list.write(writer, space, indent);
        } else if (object instanceof Pair pair) {
            return pair.write(writer, space, indent);
        } else {
            throw new IllegalArgumentException("Not a valid document");
        }
    }

    private static String escapeString(String value) {
        var builder = new StringBuilder();
        builder.append('\"');

        var length = value.length();
        for (var i = 0; i < length; i++) {
            var c = value.charAt(i);
            String replacement = null;
            if (c < STRING_REPLACE.length) {
                replacement = STRING_REPLACE[c];
            } else if (c == '\u2028') {
                replacement = "\\u2028";
            } else if (c == '\u2029') {
                replacement = "\\u2029";
            }

            if (replacement == null) {
                builder.append(c);
            } else {
                builder.append(replacement);
            }
        }

        builder.append('\"');
        return builder.toString();
    }

    private static final String[] STRING_REPLACE = new String[128];

    static {
        for (var i = 0; i <= 0x1f; i++) STRING_REPLACE[i] = String.format("\\u%04x", i);
        STRING_REPLACE['"'] = "\\\"";
        STRING_REPLACE['\\'] = "\\\\";
        STRING_REPLACE['\t'] = "\\t";
        STRING_REPLACE['\b'] = "\\b";
        STRING_REPLACE['\n'] = "\\n";
        STRING_REPLACE['\r'] = "\\r";
        STRING_REPLACE['\f'] = "\\f";
    }
}
