package dan200.computercraft.shared.util;

import net.minecraft.util.math.Matrix4f;

public class FillableMatrix4f extends Matrix4f {
    public FillableMatrix4f(float[] input) {
        a00 = input[0];
        a01 = input[1];
        a02 = input[2];
        a03 = input[3];
        a10 = input[4];
        a11 = input[5];
        a12 = input[6];
        a13 = input[7];
        a20 = input[8];
        a21 = input[9];
        a22 = input[10];
        a23 = input[11];
        a30 = input[12];
        a31 = input[13];
        a32 = input[14];
        a33 = input[15];
    }
}
