/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2020. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.client.render;

import com.google.common.base.Objects;
import com.mojang.blaze3d.matrix.MatrixStack;
import dan200.computercraft.api.client.TransformedModel;
import dan200.computercraft.api.turtle.ITurtleUpgrade;
import dan200.computercraft.api.turtle.TurtleSide;
import dan200.computercraft.shared.turtle.items.ItemTurtle;
import dan200.computercraft.shared.util.Holiday;
import dan200.computercraft.shared.util.HolidayUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.TransformationMatrix;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class TurtleSmartItemModel implements IBakedModel
{
    private static final TransformationMatrix identity, flip;

    static
    {
        MatrixStack stack = new MatrixStack();
        stack.scale( 0, -1, 0 );
        stack.translate( 0, 0, 1 );

        identity = TransformationMatrix.identity();
        flip = new TransformationMatrix( stack.getLast().getPositionMatrix() );
    }

    private static class TurtleModelCombination
    {
        final boolean m_colour;
        final ITurtleUpgrade m_leftUpgrade;
        final ITurtleUpgrade m_rightUpgrade;
        final ResourceLocation m_overlay;
        final boolean m_christmas;
        final boolean m_flip;

        TurtleModelCombination( boolean colour, ITurtleUpgrade leftUpgrade, ITurtleUpgrade rightUpgrade, ResourceLocation overlay, boolean christmas, boolean flip )
        {
            m_colour = colour;
            m_leftUpgrade = leftUpgrade;
            m_rightUpgrade = rightUpgrade;
            m_overlay = overlay;
            m_christmas = christmas;
            m_flip = flip;
        }

        @Override
        public boolean equals( Object other )
        {
            if( other == this ) return true;
            if( !(other instanceof TurtleModelCombination) ) return false;

            TurtleModelCombination otherCombo = (TurtleModelCombination) other;
            return otherCombo.m_colour == m_colour &&
                otherCombo.m_leftUpgrade == m_leftUpgrade &&
                otherCombo.m_rightUpgrade == m_rightUpgrade &&
                Objects.equal( otherCombo.m_overlay, m_overlay ) &&
                otherCombo.m_christmas == m_christmas &&
                otherCombo.m_flip == m_flip;
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 0;
            result = prime * result + (m_colour ? 1 : 0);
            result = prime * result + (m_leftUpgrade != null ? m_leftUpgrade.hashCode() : 0);
            result = prime * result + (m_rightUpgrade != null ? m_rightUpgrade.hashCode() : 0);
            result = prime * result + (m_overlay != null ? m_overlay.hashCode() : 0);
            result = prime * result + (m_christmas ? 1 : 0);
            result = prime * result + (m_flip ? 1 : 0);
            return result;
        }
    }

    private final IBakedModel familyModel;
    private final IBakedModel colourModel;

    private HashMap<TurtleModelCombination, IBakedModel> m_cachedModels;
    private ItemOverrideList m_overrides;

    public TurtleSmartItemModel( IBakedModel familyModel, IBakedModel colourModel )
    {
        this.familyModel = familyModel;
        this.colourModel = colourModel;

        m_cachedModels = new HashMap<>();
        m_overrides = new ItemOverrideList()
        {
            @Nonnull
            @Override
            public IBakedModel getModelWithOverrides( @Nonnull IBakedModel originalModel, @Nonnull ItemStack stack, @Nullable World world, @Nullable LivingEntity entity )
            {
                ItemTurtle turtle = (ItemTurtle) stack.getItem();
                int colour = turtle.getColour( stack );
                ITurtleUpgrade leftUpgrade = turtle.getUpgrade( stack, TurtleSide.Left );
                ITurtleUpgrade rightUpgrade = turtle.getUpgrade( stack, TurtleSide.Right );
                ResourceLocation overlay = turtle.getOverlay( stack );
                boolean christmas = HolidayUtil.getCurrentHoliday() == Holiday.Christmas;
                String label = turtle.getLabel( stack );
                boolean flip = label != null && (label.equals( "Dinnerbone" ) || label.equals( "Grumm" ));
                TurtleModelCombination combo = new TurtleModelCombination( colour != -1, leftUpgrade, rightUpgrade, overlay, christmas, flip );

                IBakedModel model = m_cachedModels.get( combo );
                if( model == null ) m_cachedModels.put( combo, model = buildModel( combo ) );
                return model;
            }
        };
    }

    @Nonnull
    @Override
    public ItemOverrideList getOverrides()
    {
        return m_overrides;
    }

    private IBakedModel buildModel( TurtleModelCombination combo )
    {
        Minecraft mc = Minecraft.getInstance();
        ModelManager modelManager = mc.getItemRenderer().getItemModelMesher().getModelManager();
        ModelResourceLocation overlayModelLocation = TileEntityTurtleRenderer.getTurtleOverlayModel( combo.m_overlay, combo.m_christmas );

        IBakedModel baseModel = combo.m_colour ? colourModel : familyModel;
        IBakedModel overlayModel = overlayModelLocation != null ? modelManager.getModel( overlayModelLocation ) : null;
        TransformationMatrix transform = combo.m_flip ? flip : identity;
        TransformedModel leftModel = combo.m_leftUpgrade != null ? combo.m_leftUpgrade.getModel( null, TurtleSide.Left ) : null;
        TransformedModel rightModel = combo.m_rightUpgrade != null ? combo.m_rightUpgrade.getModel( null, TurtleSide.Right ) : null;
        return new TurtleMultiModel( baseModel, overlayModel, transform, leftModel, rightModel );
    }

    @Nonnull
    @Override
    @Deprecated
    public List<BakedQuad> getQuads( BlockState state, Direction facing, @Nonnull Random rand )
    {
        return familyModel.getQuads( state, facing, rand );
    }

    @Nonnull
    @Override
    @Deprecated
    public List<BakedQuad> getQuads( BlockState state, Direction facing, @Nonnull Random rand, @Nonnull IModelData data )
    {
        return familyModel.getQuads( state, facing, rand, data );
    }

    @Override
    public boolean isAmbientOcclusion()
    {
        return familyModel.isAmbientOcclusion();
    }

    @Override
    public boolean isGui3d()
    {
        return familyModel.isGui3d();
    }

    @Override
    public boolean isBuiltInRenderer()
    {
        return familyModel.isBuiltInRenderer();
    }

    @Override
    public boolean func_230044_c_()
    {
        return familyModel.func_230044_c_();
    }

    @Nonnull
    @Override
    @Deprecated
    public TextureAtlasSprite getParticleTexture()
    {
        return familyModel.getParticleTexture();
    }

    @Nonnull
    @Override
    @Deprecated
    public ItemCameraTransforms getItemCameraTransforms()
    {
        return familyModel.getItemCameraTransforms();
    }

}
