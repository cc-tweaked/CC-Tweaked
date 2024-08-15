// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dan200.computercraft.api.ComputerCraftAPI;
import dan200.computercraft.client.render.CustomLecternRenderer;
import dan200.computercraft.shared.media.items.PrintoutItem;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import java.util.List;

/**
 * A model for {@linkplain PrintoutItem printouts} placed on a lectern.
 * <p>
 * This provides two models, {@linkplain #renderPages(PoseStack, VertexConsumer, int, int, int) one for a variable
 * number of pages}, and {@linkplain #renderBook(PoseStack, VertexConsumer, int, int) one for books}.
 *
 * @see CustomLecternRenderer
 */
public class LecternPrintoutModel {
    public static final ResourceLocation TEXTURE = new ResourceLocation(ComputerCraftAPI.MOD_ID, "entity/printout");
    public static final Material MATERIAL = new Material(InventoryMenu.BLOCK_ATLAS, TEXTURE);

    private static final int TEXTURE_WIDTH = 32;
    private static final int TEXTURE_HEIGHT = 32;

    private static final String PAGE_1 = "page_1";
    private static final String PAGE_2 = "page_2";
    private static final String PAGE_3 = "page_3";
    private static final List<String> PAGES = List.of(PAGE_1, PAGE_2, PAGE_3);

    private final ModelPart pagesRoot;
    private final ModelPart bookRoot;
    private final ModelPart[] pages;

    public LecternPrintoutModel() {
        pagesRoot = buildPages();
        bookRoot = buildBook();
        pages = PAGES.stream().map(pagesRoot::getChild).toArray(ModelPart[]::new);
    }

    private static ModelPart buildPages() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();
        parts.addOrReplaceChild(
            PAGE_1,
            CubeListBuilder.create().texOffs(0, 0).addBox(-0.005f, -4.0f, -2.5f, 1f, 8.0f, 5.0f),
            PartPose.ZERO
        );

        parts.addOrReplaceChild(
            PAGE_2,
            CubeListBuilder.create().texOffs(12, 0).addBox(-0.005f, -4.0f, -2.5f, 1f, 8.0f, 5.0f),
            PartPose.offsetAndRotation(-0.125f, 0, 1.5f, (float) Math.PI * (1f / 16), 0, 0)
        );
        parts.addOrReplaceChild(
            PAGE_3,
            CubeListBuilder.create().texOffs(12, 0).addBox(-0.005f, -4.0f, -2.5f, 1f, 8.0f, 5.0f),
            PartPose.offsetAndRotation(-0.25f, 0, -1.5f, (float) -Math.PI * (2f / 16), 0, 0)
        );

        return mesh.getRoot().bake(TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    private static ModelPart buildBook() {
        var mesh = new MeshDefinition();
        var parts = mesh.getRoot();

        parts.addOrReplaceChild(
            "spine",
            CubeListBuilder.create().texOffs(12, 15).addBox(-0.005f, -5.0f, -0.5f, 0, 10, 1.0f),
            PartPose.ZERO
        );

        var angle = (float) Math.toRadians(5);
        parts.addOrReplaceChild(
            "left",
            CubeListBuilder.create()
                .texOffs(0, 10).addBox(0, -5.0f, -6.0f, 0, 10, 6.0f)
                .texOffs(0, 0).addBox(0.005f, -4.0f, -5.0f, 1.0f, 8.0f, 5.0f),
            PartPose.offsetAndRotation(-0.005f, 0, -0.5f, 0, -angle, 0)
        );

        parts.addOrReplaceChild(
            "right",
            CubeListBuilder.create()
                .texOffs(14, 10).addBox(0, -5.0f, 0, 0, 10, 6.0f)
                .texOffs(0, 0).addBox(0.005f, -4.0f, 0, 1.0f, 8.0f, 5.0f),
            PartPose.offsetAndRotation(-0.005f, 0, 0.5f, 0, angle, 0)
        );

        return mesh.getRoot().bake(TEXTURE_WIDTH, TEXTURE_HEIGHT);
    }

    public void renderBook(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        bookRoot.render(poseStack, buffer, packedLight, packedOverlay, 1, 1, 1, 1);
    }

    public void renderPages(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int pageCount) {
        if (pageCount > pages.length) pageCount = pages.length;
        var i = 0;
        for (; i < pageCount; i++) pages[i].visible = true;
        for (; i < pages.length; i++) pages[i].visible = false;

        pagesRoot.render(poseStack, buffer, packedLight, packedOverlay, 1, 1, 1, 1);
    }
}
