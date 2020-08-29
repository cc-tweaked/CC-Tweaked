/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2019. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nonnull;
import javax.vecmath.Matrix4f;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.render.model.json.ModelTransformation;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Direction;

public class TurtleMultiModel implements BakedModel {
    private final BakedModel m_baseModel;
    private final BakedModel m_overlayModel;
    private final Matrix4f m_generalTransform;
    private final BakedModel m_leftUpgradeModel;
    private final Matrix4f m_leftUpgradeTransform;
    private final BakedModel m_rightUpgradeModel;
    private final Matrix4f m_rightUpgradeTransform;
    private List<BakedQuad> m_generalQuads = null;
    private Map<Direction, List<BakedQuad>> m_faceQuads = new EnumMap<>(Direction.class);

    public TurtleMultiModel(BakedModel baseModel, BakedModel overlayModel, Matrix4f generalTransform, BakedModel leftUpgradeModel,
                            Matrix4f leftUpgradeTransform, BakedModel rightUpgradeModel, Matrix4f rightUpgradeTransform) {
        // Get the models
        this.m_baseModel = baseModel;
        this.m_overlayModel = overlayModel;
        this.m_leftUpgradeModel = leftUpgradeModel;
        this.m_leftUpgradeTransform = leftUpgradeTransform;
        this.m_rightUpgradeModel = rightUpgradeModel;
        this.m_rightUpgradeTransform = rightUpgradeTransform;
        this.m_generalTransform = generalTransform;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, @Nonnull Random rand) {
        if (side != null) {
            if (!this.m_faceQuads.containsKey(side)) {
                this.m_faceQuads.put(side, this.buildQuads(state, side, rand));
            }
            return this.m_faceQuads.get(side);
        } else {
            if (this.m_generalQuads == null) {
                this.m_generalQuads = this.buildQuads(state, side, rand);
            }
            return this.m_generalQuads;
        }
    }

    private List<BakedQuad> buildQuads(BlockState state, Direction side, Random rand) {
        ArrayList<BakedQuad> quads = new ArrayList<>();
        ModelTransformer.transformQuadsTo(quads, this.m_baseModel.getQuads(state, side, rand), this.m_generalTransform);
        if (this.m_overlayModel != null) {
            ModelTransformer.transformQuadsTo(quads, this.m_overlayModel.getQuads(state, side, rand), this.m_generalTransform);
        }
        if (this.m_leftUpgradeModel != null) {
            Matrix4f upgradeTransform = this.m_generalTransform;
            if (this.m_leftUpgradeTransform != null) {
                upgradeTransform = new Matrix4f(this.m_generalTransform);
                upgradeTransform.mul(this.m_leftUpgradeTransform);
            }
            ModelTransformer.transformQuadsTo(quads, this.m_leftUpgradeModel.getQuads(state, side, rand), upgradeTransform);
        }
        if (this.m_rightUpgradeModel != null) {
            Matrix4f upgradeTransform = this.m_generalTransform;
            if (this.m_rightUpgradeTransform != null) {
                upgradeTransform = new Matrix4f(this.m_generalTransform);
                upgradeTransform.mul(this.m_rightUpgradeTransform);
            }
            ModelTransformer.transformQuadsTo(quads, this.m_rightUpgradeModel.getQuads(state, side, rand), upgradeTransform);
        }
        quads.trimToSize();
        return quads;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.m_baseModel.useAmbientOcclusion();
    }

    @Override
    public boolean hasDepth() {
        return this.m_baseModel.hasDepth();
    }

    @Override
    public boolean isBuiltin() {
        return this.m_baseModel.isBuiltin();
    }

    @Override
    public Sprite getSprite() {
        return this.m_baseModel.getSprite();
    }

    @Override
    public ModelTransformation getTransformation() {
        return this.m_baseModel.getTransformation();
    }

    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }
}
