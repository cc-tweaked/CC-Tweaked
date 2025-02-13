// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.pocket.PocketComputerData;
import dan200.computercraft.client.render.CustomLecternRenderer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * A model for {@linkplain PocketComputerItem pocket computers} placed on a lectern.
 *
 * @see CustomLecternRenderer
 */
public class LecternPocketModel {
    public static final ResourceLocation TEXTURE_NORMAL = new ResourceLocation(ComputerCraftAPI.MOD_ID, "entity/pocket_computer_normal");
    public static final ResourceLocation TEXTURE_ADVANCED = new ResourceLocation(ComputerCraftAPI.MOD_ID, "entity/pocket_computer_advanced");
    public static final ResourceLocation TEXTURE_COLOUR = new ResourceLocation(ComputerCraftAPI.MOD_ID, "entity/pocket_computer_colour");
    public static final ResourceLocation TEXTURE_FRAME = new ResourceLocation(ComputerCraftAPI.MOD_ID, "entity/pocket_computer_frame");
    public static final ResourceLocation TEXTURE_LIGHT = new ResourceLocation(ComputerCraftAPI.MOD_ID, "entity/pocket_computer_light");

    private static final Material MATERIAL_NORMAL = new Material(InventoryMenu.BLOCK_ATLAS, TEXTURE_NORMAL);
    private static final Material MATERIAL_ADVANCED = new Material(InventoryMenu.BLOCK_ATLAS, TEXTURE_ADVANCED);
    private static final Material MATERIAL_COLOUR = new Material(InventoryMenu.BLOCK_ATLAS, TEXTURE_COLOUR);
    private static final Material MATERIAL_FRAME = new Material(InventoryMenu.BLOCK_ATLAS, TEXTURE_FRAME);
    private static final Material MATERIAL_LIGHT = new Material(InventoryMenu.BLOCK_ATLAS, TEXTURE_LIGHT);

    // The size of the terminal within the model.
    public static final float TERM_WIDTH = 12.0f / 32.0f;
    public static final float TERM_HEIGHT = 14.0f / 32.0f;

    // The size of the texture. The texture is 36x36, but is at 2x resolution.
    private static final int TEXTURE_WIDTH = 36 / 2;
    private static final int TEXTURE_HEIGHT = 36 / 2;

    private final ModelPart root;

    public LecternPocketModel() {
        root = buildPages();
    }

    private static ModelPart buildPages() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        parts.addOrReplaceChild(
            "root",
            CubeListBuilder.create().texOffs(0, 0).addBox(0f, -5.0f, -4.0f, 1f, 10.0f, 8.0f),
            PartPose.ZERO
        );
        return mesh.getRoot().bake(TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private void render(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int colour) {
        int red = FastColor.ARGB32.red(colour), green = FastColor.ARGB32.green(colour), blue = FastColor.ARGB32.blue(colour), alpha = FastColor.ARGB32.alpha(colour);
        root.render(poseStack, buffer, packedLight, packedOverlay, red / 255.0f, green / 255.0f, blue / 255.0f, alpha / 255.0f);
    }

    /**
     * Render the pocket computer model.
     *
     * @param poseStack     The current pose stack.
     * @param bufferSource  The buffer source to draw to.
     * @param packedLight   The current light level.
     * @param packedOverlay The overlay texture (used for entity hurt animation).
     * @param family        The computer family.
     * @param frameColour   The pocket computer's {@linkplain PocketComputerItem#getColour(ItemStack) colour}.
     * @param lightColour   The pocket computer's {@linkplain PocketComputerData#getLightState() light colour}.
     */
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay, ComputerFamily family, int frameColour, int lightColour) {
        if (frameColour != -1) {
            root.render(poseStack, MATERIAL_FRAME.buffer(bufferSource, RenderType::entityCutout), packedLight, packedOverlay, 1, 1, 1, 1);
            render(poseStack, MATERIAL_COLOUR.buffer(bufferSource, RenderType::entityCutout), packedLight, packedOverlay, frameColour);
        } else {
            var buffer = (family == ComputerFamily.ADVANCED ? MATERIAL_ADVANCED : MATERIAL_NORMAL).buffer(bufferSource, RenderType::entityCutout);
            root.render(poseStack, buffer, packedLight, packedOverlay, 1, 1, 1, 1);
        }

        render(poseStack, MATERIAL_LIGHT.buffer(bufferSource, RenderType::entityCutout), LightTexture.FULL_BRIGHT, packedOverlay, lightColour);
    }
}
