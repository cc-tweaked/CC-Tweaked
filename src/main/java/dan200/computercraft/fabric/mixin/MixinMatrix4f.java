/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.fabric.mixin;

import com.mojang.math.Matrix4f;
import dan200.computercraft.fabric.mixininterface.IMatrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin( Matrix4f.class )
public class MixinMatrix4f implements IMatrix4f
{
    @Shadow protected float a00;
    @Shadow protected float a01;
    @Shadow protected float a02;
    @Shadow protected float a03;
    @Shadow protected float a10;
    @Shadow protected float a11;
    @Shadow protected float a12;
    @Shadow protected float a13;
    @Shadow protected float a20;
    @Shadow protected float a21;
    @Shadow protected float a22;
    @Shadow protected float a23;
    @Shadow protected float a30;
    @Shadow protected float a31;
    @Shadow protected float a32;
    @Shadow protected float a33;

    public void setFloatArray( float[] values )
    {
        a00 = values[0];
        a01 = values[1];
        a02 = values[2];
        a03 = values[3];
        a10 = values[4];
        a11 = values[5];
        a12 = values[6];
        a13 = values[7];
        a20 = values[8];
        a21 = values[9];
        a22 = values[10];
        a23 = values[11];
        a30 = values[12];
        a31 = values[13];
        a32 = values[14];
        a33 = values[15];
    }
}
