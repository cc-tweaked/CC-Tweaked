package dan200.computercraft.shared.util;

import net.minecraft.util.math.Matrix4f;

public class FillableMatrix4f extends Matrix4f {
    public FillableMatrix4f(float[] input) {
        this.a00 = input[0];
        this.a01 = input[1];
        this.a02 = input[2];
        this.a03 = input[3];
        this.a10 = input[4];
        this.a11 = input[5];
        this.a12 = input[6];
        this.a13 = input[7];
        this.a20 = input[8];
        this.a21 = input[9];
        this.a22 = input[10];
        this.a23 = input[11];
        this.a30 = input[12];
        this.a31 = input[13];
        this.a32 = input[14];
        this.a33 = input[15];
    }
}
