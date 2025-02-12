// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.render.CustomLecternRenderer;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.pocket.items.PocketComputerItem;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
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

    private static final Material MATERIAL_NORMAL = new Material(InventoryMenu.BLOCK_ATLAS, TEXTURE_NORMAL);
    private static final Material MATERIAL_ADVANCED = new Material(InventoryMenu.BLOCK_ATLAS, TEXTURE_ADVANCED);
    private static final Material MATERIAL_COLOUR = new Material(InventoryMenu.BLOCK_ATLAS, TEXTURE_COLOUR);

    public static final float TERM_WIDTH = 12.0f / 32.0f;
    public static final float TERM_HEIGHT = 14.0f / 32.0f;

    private final ModelPart basic;
    private final ModelPart colourFrame;
    private final ModelPart colourBody;

    public LecternPocketModel() {
        basic = buildPages(18, 18, 0, 0);
        colourFrame = buildPages(32, 32, 0, 0);
        colourBody = buildPages(32, 32, 14, 14);
    }

    private static ModelPart buildPages(int width, int height, int textureOffX, int textureOffY) {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        parts.addOrReplaceChild(
            "root",
            CubeListBuilder.create().texOffs(textureOffX, textureOffY).addBox(0f, -5.0f, -4.0f, 1f, 10.0f, 8.0f),
            PartPose.ZERO
        );
        return mesh.getRoot().bake(width, height);
    }

    public void render(PoseStack poseStack, MultiBufferSource buffers, int packedLight, int packedOverlay, ComputerFamily family, int colour) {
        if (colour != -1) {
            var buffer = MATERIAL_COLOUR.buffer(buffers, RenderType::entityCutout);
            int red = FastColor.ARGB32.red(colour), green = FastColor.ARGB32.green(colour), blue = FastColor.ARGB32.blue(colour);
            colourFrame.render(poseStack, buffer, packedLight, packedOverlay, 1, 1, 1, 1);
            colourBody.render(poseStack, buffer, packedLight, packedOverlay, red / 255.0f, green / 255.0f, blue / 255.0f, 1);
        } else {
            var buffer = (family == ComputerFamily.ADVANCED ? MATERIAL_ADVANCED : MATERIAL_NORMAL).buffer(buffers, RenderType::entityCutout);
            basic.render(poseStack, buffer, packedLight, packedOverlay, 1, 1, 1, 1);
        }
    }
}
