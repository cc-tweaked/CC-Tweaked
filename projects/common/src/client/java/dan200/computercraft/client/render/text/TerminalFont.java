package dan200.computercraft.client.render.text;

import dan200.computercraft.core.terminal.VariableWidthTextBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.joml.Vector4f;

import javax.annotation.Nullable;

public class TerminalFont {
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
        prepareFontTexture();
        ((ReloadableResourceManager) Minecraft.getInstance().getResourceManager()).registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profilerFiller) {
                return null;
            }

            @Override
            protected void apply(Void unused, ResourceManager resourceManager, ProfilerFiller profilerFiller) {
                TerminalFont.this.fullTexture.close();
                prepareFontTexture();
            }
        });
    }

    private void prepareFontTexture() {
        this.fullTexture = new DynamicFontTexture(256);
        Minecraft.getInstance().getTextureManager().register(DynamicFontTexture.DEFAULT_NAME, this.fullTexture);
        // preload codepoint 0x01-0xff
        for (int i = 1; i < 256; i++) {
            this.fullTexture.getGlyph(i);
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

    public void preloadCharacterFont(VariableWidthTextBuffer textBuffer){
        for (int i = 0; i < textBuffer.length(); i++) {
            fullTexture.getGlyph(textBuffer.codepointAt(i));
        }
    }

    public void preloadCharacterFont(VariableWidthTextBuffer[] textBuffers){
        for (var textBuffer : textBuffers) {
            preloadCharacterFont(textBuffer);
        }
    }
}
