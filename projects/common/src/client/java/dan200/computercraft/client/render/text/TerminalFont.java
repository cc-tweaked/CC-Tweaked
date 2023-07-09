package dan200.computercraft.client.render.text;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import dan200.computercraft.core.terminal.TextBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector4f;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.Function;

public class TerminalFont {

    public static final ResourceLocation ANSI_TERM_FONT = new ResourceLocation("computercraft", "textures/gui/term_font.png");

    public static final int FONT_HEIGHT = 9;
    public static final int FONT_WIDTH = 6;
    static final float WIDTH = 256.0f;
    private DynamicFontTexture fullTexture;

    @Nullable
    private static TerminalFont instance;

    public static TerminalFont getInstance(){
        if(instance == null){
            instance = new TerminalFont();
        }
        return instance;
    }

    private TerminalFont(){
        this.fullTexture = new DynamicFontTexture(512);
        Minecraft.getInstance().getTextureManager().register(DynamicFontTexture.DEFAULT_NAME, this.fullTexture);
        try (var stream = Minecraft.getInstance().getResourceManager().open(ANSI_TERM_FONT)){
            var image = NativeImage.read(stream);
            var scale = image.getWidth() / WIDTH;
            for (int i = 1; i < 256; i++) {
                var column = i % 16;
                var row = i / 16;
                var xStart = 1 + column * (FONT_WIDTH + 2);
                var yStart = 1 + row * (FONT_HEIGHT + 2);
                fullTexture.registeredGlyph(i, new ANSITerminalGlyphInfo(image, (int) (xStart * scale), (int) (yStart * scale), (int) (FONT_WIDTH * scale), (int) (FONT_HEIGHT * scale)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public int getTextureSize(){
        return fullTexture.getCurrentSize();
    }

    public Vector4f getGlyphUv(int codepoint){
        var registeredGlyph = this.fullTexture.getGlyph(codepoint);
        return new Vector4f(registeredGlyph.u0() / this.fullTexture.getCurrentSize(),
            registeredGlyph.v0() / this.fullTexture.getCurrentSize(),
            registeredGlyph.u1() / this.fullTexture.getCurrentSize(),
            registeredGlyph.v1() / this.fullTexture.getCurrentSize());
    }

    public Vector4f getWhiteGlyphUv(){
        var registeredGlyph = this.fullTexture.getWhiteGlyph();
        return new Vector4f(registeredGlyph.u0() / this.fullTexture.getCurrentSize(),
            registeredGlyph.v0() / this.fullTexture.getCurrentSize(),
            registeredGlyph.u1() / this.fullTexture.getCurrentSize(),
            registeredGlyph.v1() / this.fullTexture.getCurrentSize());
    }

    public void preloadCharacterFont(TextBuffer textBuffer){
        for (int i = 0; i < textBuffer.length(); i++) {
            fullTexture.getGlyph(textBuffer.charAt(i));
        }
    }

    public void preloadCharacterFont(TextBuffer[] textBuffers){
        for (TextBuffer textBuffer : textBuffers) {
            preloadCharacterFont(textBuffer);
        }
    }

    record ANSITerminalGlyphInfo(NativeImage image, int offsetX, int offsetY, int width, int height) implements GlyphInfo{

        public float getAdvance() {
            return 0;
        }

        public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> function) {
            return function.apply(new SheetGlyphInfo() {
                @Override
                public int getPixelWidth() {
                    return ANSITerminalGlyphInfo.this.width;
                }
                @Override
                public int getPixelHeight() {
                    return ANSITerminalGlyphInfo.this.height;
                }
                @Override
                public void upload(int xOffset, int yOffset) {
                    ANSITerminalGlyphInfo.this.image.upload(0, xOffset, yOffset, ANSITerminalGlyphInfo.this.offsetX, ANSITerminalGlyphInfo.this.offsetY, width, height, false, false);
                }
                @Override
                public boolean isColored() {
                    return true;
                }
                @Override
                public float getOversample() {
                    return 1;
                }
            });
        }
    }

}
