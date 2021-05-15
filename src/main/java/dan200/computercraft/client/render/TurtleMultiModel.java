/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */

package dan200.computercraft.client.render;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.annotation.Nonnull;

import dan200.computercraft.api.client.TransformedModel;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.render.model.json.ModelOverrideList;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.AffineTransformation;
import net.minecraft.util.math.Direction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment (EnvType.CLIENT)
public class TurtleMultiModel implements BakedModel {
    private final BakedModel m_baseModel;
    private final BakedModel m_overlayModel;
    private final AffineTransformation m_generalTransform;
    private final TransformedModel m_leftUpgradeModel;
    private final TransformedModel m_rightUpgradeModel;
    private List<BakedQuad> m_generalQuads = null;
    private Map<Direction, List<BakedQuad>> m_faceQuads = new EnumMap<>(Direction.class);

    public TurtleMultiModel(BakedModel baseModel, BakedModel overlayModel, AffineTransformation generalTransform, TransformedModel leftUpgradeModel,
                            TransformedModel rightUpgradeModel) {
        // Get the models
        this.m_baseModel = baseModel;
        this.m_overlayModel = overlayModel;
        this.m_leftUpgradeModel = leftUpgradeModel;
        this.m_rightUpgradeModel = rightUpgradeModel;
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


        ModelTransformer.transformQuadsTo(quads, this.m_baseModel.getQuads(state, side, rand), this.m_generalTransform.getMatrix());
        if (this.m_overlayModel != null) {
            ModelTransformer.transformQuadsTo(quads, this.m_overlayModel.getQuads(state, side, rand), this.m_generalTransform.getMatrix());
        }
        if (this.m_leftUpgradeModel != null) {
            AffineTransformation upgradeTransform = this.m_generalTransform.multiply(this.m_leftUpgradeModel.getMatrix());
            ModelTransformer.transformQuadsTo(quads, this.m_leftUpgradeModel.getModel()
                                                                            .getQuads(state, side, rand),
                                              upgradeTransform.getMatrix());
        }
        if (this.m_rightUpgradeModel != null) {
            AffineTransformation upgradeTransform = this.m_generalTransform.multiply(this.m_rightUpgradeModel.getMatrix());
            ModelTransformer.transformQuadsTo(quads, this.m_rightUpgradeModel.getModel()
                                                                             .getQuads(state, side, rand),
                                              upgradeTransform.getMatrix());
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
    public boolean isSideLit() {
        return this.m_baseModel.isSideLit();
    }

    @Override
    public boolean isBuiltin() {
        return this.m_baseModel.isBuiltin();
    }

    @Nonnull
    @Override
    @Deprecated
    public Sprite getSprite() {
        return this.m_baseModel.getSprite();
    }

    @Nonnull
    @Override
    @Deprecated
    public net.minecraft.client.render.model.json.ModelTransformation getTransformation() {
        return this.m_baseModel.getTransformation();
    }

    @Nonnull
    @Override
    public ModelOverrideList getOverrides() {
        return ModelOverrideList.EMPTY;
    }
}
