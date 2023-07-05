package dan200.computercraft.client.render.text;

import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.font.SheetGlyphInfo;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import dan200.computercraft.api.ComputerCraftAPI;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.SpecialGlyphs;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.ARBCopyImage;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MathUtil;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;

public class DynamicFontTexture extends AbstractTexture {
    public static final ResourceLocation DEFAULT_NAME = new ResourceLocation(ComputerCraftAPI.MOD_ID, "dyn_term_font");
    private static final int MINIMUM_SIZE = 512;

    private int currentSize;
    private TextureSlotNode rootNode;

    private Int2ObjectMap<RegisteredGlyph> glyphs = new Int2ObjectOpenHashMap<>();

    private RegisteredGlyph whiteGlyph;


    public DynamicFontTexture(int size){
        if(size < MINIMUM_SIZE) currentSize = MINIMUM_SIZE;
        else {
            currentSize = MathUtil.mathRoundPoT(size);
        }
        TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGBA, getId(), currentSize, currentSize);
        rootNode = new TextureSlotNode(null, 0, 0, currentSize, currentSize);
        var whiteImage = new NativeImage(NativeImage.Format.RGBA, 6, 9, false);
        whiteImage.fillRect(0, 0, 6, 9, -1);
        whiteImage.untrack();
        whiteGlyph = Objects.requireNonNull(registerGlyph(new GlyphInfo() {
            @Override
            public float getAdvance() {
                return 0;
            }
            @Override
            public BakedGlyph bake(Function<SheetGlyphInfo, BakedGlyph> function) {
                return function.apply(new SheetGlyphInfo() {
                    @Override
                    public int getPixelWidth() {
                        return whiteImage.getWidth();
                    }
                    @Override
                    public int getPixelHeight() {
                        return whiteImage.getHeight();
                    }
                    @Override
                    public void upload(int xOffset, int yOffset) {
                        whiteImage.upload(0, xOffset, yOffset, false);
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
        }));
        whiteImage.close();
    }
    @Override
    public void load(ResourceManager resourceManager) throws IOException {

    }

    public RegisteredGlyph getWhiteGlyph() {
        return whiteGlyph;
    }

    public int getCurrentSize() {
        return currentSize;
    }

    public RegisteredGlyph getGlyph(int codepoint){
        return glyphs.computeIfAbsent(codepoint, cp -> {
            var fontSet = Minecraft.getInstance().font.getFontSet(Minecraft.UNIFORM_FONT);
            var glyphInfo = fontSet.getGlyphInfo(cp, false);
            if(glyphInfo != SpecialGlyphs.MISSING){
                var glyph = registerGlyph(glyphInfo);
                return glyph != null ? glyph : whiteGlyph;
            }
            return whiteGlyph;
        });
    }

    void registeredGlyph(int codepoint, GlyphInfo glyphInfo){
        var glyph = registerGlyph(glyphInfo);
        if(glyph != null){
            glyphs.put(codepoint, glyph);
        }
    }

    @Nullable
    private RegisteredGlyph registerGlyph(GlyphInfo glyphInfo) {
        var regGlyphWrapper = new RegisteredGlyph[]{ null };
        glyphInfo.bake(sheetGlyphInfo -> {
            var assignedNode = rootNode.insert(sheetGlyphInfo);
            if (assignedNode == null) {
                resize(currentSize * 2);
                assignedNode = rootNode.insert(sheetGlyphInfo);
            }
            if (assignedNode == null) {
                return null;
            }
            bind();
            sheetGlyphInfo.upload(assignedNode.x, assignedNode.y);
            regGlyphWrapper[0] = new RegisteredGlyph(assignedNode.x, assignedNode.y, assignedNode.x + assignedNode.width, assignedNode.y + assignedNode.height);
            return null;
        });
        return regGlyphWrapper[0];
    }

    public void resize(int desiredSize){
        RenderSystem.assertOnRenderThreadOrInit();
        if(desiredSize > currentSize){
            desiredSize = MathUtil.mathRoundPoT(desiredSize);
            if(GL.getCapabilities().GL_ARB_copy_image || GL.getCapabilities().OpenGL43){
                var newId = TextureUtil.generateTextureId();
                TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGBA, newId, desiredSize, desiredSize);
                ARBCopyImage.glCopyImageSubData(id, GL11.GL_TEXTURE_2D, 0, 0, 0, 0,
                    newId, GL11.GL_TEXTURE_2D, 0, 0, 0, 0,
                    currentSize, currentSize, 0);
                TextureUtil.releaseTextureId(id);
                this.id = newId;
            }
            else{
                bind();
                var buf = BufferUtils.createByteBuffer(currentSize * currentSize * 4);
                GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, NativeImage.InternalGlFormat.RGBA.glFormat(), GL11.GL_UNSIGNED_BYTE, buf);
                TextureUtil.prepareImage(NativeImage.InternalGlFormat.RGBA, getId(), desiredSize, desiredSize);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, currentSize, currentSize, NativeImage.InternalGlFormat.RGBA.glFormat(), GL11.GL_UNSIGNED_BYTE, buf);
            }
            currentSize = desiredSize;
            rootNode.width = currentSize;
            rootNode.height = currentSize;
            rootNode.recomputeNodeDimension();
        }
    }

    @Override
    public void close() {
        this.releaseId();
    }

    static class TextureSlotNode {
        final int x;
        final int y;

        int width;
        int height;

        @Nullable
        TextureSlotNode parent;

        @Nullable
        TextureSlotNode left;

        @Nullable
        TextureSlotNode right;

        boolean occupied = false;

        TextureSlotNode(@Nullable TextureSlotNode parent, int x, int y, int width, int height){
            this.parent = parent;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Nullable
        TextureSlotNode insert(SheetGlyphInfo info){
            if (occupied) return null;
            else{
                var glyphW = info.getPixelWidth();
                var glyphH = info.getPixelHeight();
                if (glyphW > width || glyphH > height) return null; // cannot fit the glyph
                if(this.left != null && this.right != null){
                    var result = this.left.insert(info);
                    if(result == null) return this.right.insert(info);
                    return result;
                }
                else if (glyphW == width && glyphH == height) {
                    occupied = true;
                    return this;
                } else {
                    int remainW = width - glyphW;
                    int remainH = height - glyphH;
                    // always cut out part with larger dimension, and the left node is always the smaller part
                    if (remainW > remainH) {
                        this.left = new TextureSlotNode(this, this.x, this.y, glyphW, this.height);
                        this.right = new TextureSlotNode(this, this.x + glyphW + 1, this.y, remainW - 1, this.height);
                    } else {
                        this.left = new TextureSlotNode(this, this.x, this.y, this.width, glyphH);
                        this.right = new TextureSlotNode(this, this.x, this.y + glyphH + 1, this.width, remainH - 1);
                    }
                    return this.left.insert(info);
                }
            }
        }

        void recomputeNodeDimension(){
            if(this.left != null && this.right != null){
                boolean updated = false;
                if(this.left.width == this.right.width){
                    this.left.width = width;
                    this.right.width = width;
                    updated = true;
                }else if(this.left.width + this.right.width != this.width - 1){
                    this.right.width = this.width - this.left.width - 1;
                    updated = true;
                }
                if(this.left.height == this.right.height){
                    this.left.height = height;
                    this.right.height = height;
                    updated = true;
                }else if(this.left.height + this.right.height != this.height - 1){
                    this.right.height = this.height - this.left.height - 1;
                    updated = true;
                }
                if(updated){
                    this.left.recomputeNodeDimension();
                    this.right.recomputeNodeDimension();
                }
            }
        }
    }

    /**
     * Glyph record registered in the font texture.
     * Since the texture may resize. UV coordinate stored here is NOT normalized.
     *
     * @param u0
     * @param v0
     * @param u1
     * @param v1
     */
    public record RegisteredGlyph (float u0, float v0, float u1, float v1){

    }
}
