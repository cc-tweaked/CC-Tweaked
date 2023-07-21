package dan200.computercraft.core.terminal;

import dan200.computercraft.core.util.StringUtil;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class VariableWidthTextBuffer extends TextBuffer{

    private final int[] text;
    private final int[] cumulativeWidth;

    public VariableWidthTextBuffer(char c, int length){
        super("");
        text = new int[length];
        cumulativeWidth = new int[length];
        fill(c);
    }

    public VariableWidthTextBuffer(String text) {
        super("");
        this.text = text.codePoints().toArray();
        cumulativeWidth = new int[this.text.length];
        for (int i = 0; i < this.text.length; i++) {
            this.cumulativeWidth[i] = (i == 0 ? 0 : cumulativeWidth[i - 1]) + StringUtil.getCodepointWidth(this.text[i]);
        }
    }

    @Override
    public int length() {
        return this.text.length;
    }

    @Override
    public void write(String text, int start) {
        var pos = start;
        start = Math.max(start, 0);
        var actualTextLen = text.codePointCount(0, text.length());
        var end = Math.min(start + actualTextLen, pos + actualTextLen);
        end = Math.min(end, this.text.length);
        int oldEnd = cumulativeWidth[end - 1];
        int surrogateOffset = 0;
        for (var i = start; i < end; i++) {
            var c = text.codePointAt(i - pos + surrogateOffset);
            if(c > 0xffff) surrogateOffset++;
            this.text[i] = c;
            var codepointWidth = StringUtil.getCodepointWidth(c);
            this.cumulativeWidth[i] = (i == 0 ? 0 : this.cumulativeWidth[i - 1]) + codepointWidth;
        }
        int deltaEnd = cumulativeWidth[end - 1] - oldEnd;
        if(deltaEnd != 0){
            for (int i = end; i < cumulativeWidth.length; i++) {
                cumulativeWidth[i] += deltaEnd;
            }
        }
    }

    public void write(ByteBuffer text, int start) {
        var pos = start;
        var bufferPos = text.position();

        start = Math.max(start, 0);
        var length = text.remaining();
        var end = Math.min(start + length, pos + length);
        end = Math.min(end, this.text.length);
        int oldEnd = cumulativeWidth[end - 1];
        for (var i = start; i < end; i++) {
            var c = (char) (text.get(bufferPos + i - pos) & 0xFF);
            this.text[i] = c;
            this.cumulativeWidth[i] = (i == 0 ? 0 : this.cumulativeWidth[i - 1]) + StringUtil.getCodepointWidth(c);
        }
        int deltaEnd = cumulativeWidth[end - 1] - oldEnd;
        if(deltaEnd != 0){
            for (int i = end; i < cumulativeWidth.length; i++) {
                cumulativeWidth[i] += deltaEnd;
            }
        }
    }

    @Override
    public void write(TextBuffer text) {
        var end = Math.min(text.length(), this.text.length);
        var oldEnd = cumulativeWidth[end - 1];
        for (var i = 0; i < end; i++) {
            var c = text instanceof VariableWidthTextBuffer vtext ? vtext.codepointAt(i) : text.charAt(i);
            this.text[i] = c;
            this.cumulativeWidth[i] = (i == 0 ? 0 : this.cumulativeWidth[i - 1]) + StringUtil.getCodepointWidth(c);
        }
        if(end < this.text.length){
            var deltaEnd = cumulativeWidth[end - 1] - oldEnd;
            if(deltaEnd != 0){
                for (int i = end; i < cumulativeWidth.length; i++) {
                    cumulativeWidth[i] += deltaEnd;
                }
            }
        }
    }

    @Override
    public void fill(char c) {
        fill(c, 0, text.length);
    }

    @Override
    public void fill(char c, int start, int end) {
        start = Math.max(start, 0);
        end = Math.min(end, text.length);
        int w = StringUtil.getCodepointWidth(c);
        int oldEnd = cumulativeWidth[end - 1];
        for (var i = start; i < end; i++) {
            text[i] = c;
            cumulativeWidth[i] = (i == 0 ? 0 : cumulativeWidth[i - 1]) + w;
        }
        int deltaEnd = cumulativeWidth[end - 1] - oldEnd;
        if(deltaEnd != 0){
            for (int i = end; i < cumulativeWidth.length; i++) {
                cumulativeWidth[i] += deltaEnd;
            }
        }
    }

    /**
     * @deprecated use {@code codepointAt} instead.
     */
    @Deprecated
    public char charAt(int i) {
        var i1 = this.text[i % this.text.length];
        if (Character.isSupplementaryCodePoint(i1)) {
            if (i >= this.text.length) {
                return Character.lowSurrogate(i1);
            }
            else{
                return Character.highSurrogate(i1);
            }
        }
        return (char) i1;
    }

    public int codepointAt(int i){
        return this.text[i];
    }

    public int getCumulativeWidth(int i){
        if(i < 0) return 0;
        i = Math.min(i, cumulativeWidth.length - 1);
        return cumulativeWidth[i];
    }

    /**
     * Given an expected width, return the index of last character that fits within the width
     * @param w
     * @return
     */
    public int getIndexFromWidth(int w){
        var i = Arrays.binarySearch(this.cumulativeWidth, w);
        if(i < 0) return -i - 2;
        return i;
    }

    public void setChar(int i, char c) {
        this.setChar(i, ((int) c));
    }

    public void setChar(int i, int c) {
        if (i >= 0 && i < text.length) {
            var deltaW = StringUtil.getCodepointWidth(c) - cumulativeWidth[i] + (i == 0 ? 0 : cumulativeWidth[i - 1]);
            text[i] = c;
            if(deltaW != 0){
                for (int j = i; j < cumulativeWidth.length; j++) {
                    cumulativeWidth[j] += deltaW;
                }
            }
        }
    }

    @Override
    public String toString() {
        return new String(text, 0, text.length);
    }
}
