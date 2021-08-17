/*
 * This file is part of ComputerCraft - http://www.computercraft.info
 * Copyright Daniel Ratcliffe, 2011-2021. Do not distribute without permission.
 * Send enquiries to dratcliffe@gmail.com
 */
package dan200.computercraft.ingame.mixin;

import net.minecraft.test.TestFunctionInfo;
import net.minecraft.test.TestTrackerHolder;
import net.minecraft.util.Rotation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Consumer;

/**
 * Mixin to replace final fields and some getters with non-final versions.
 *
 * Due to (I assume) the magic of proguard, some getters are replaced with constant
 * implementations. Thus we need to replace them with a sensible version.
 */
@Mixin( TestFunctionInfo.class )
public class MixinTestFunctionInfo
{
    @Shadow
    @Mutable
    private String batchName;

    @Shadow
    @Mutable
    private String testName;

    @Shadow
    @Mutable
    private String structureName;

    @Shadow
    @Mutable
    private boolean required;

    @Shadow
    @Mutable
    private Consumer<TestTrackerHolder> function;

    @Shadow
    @Mutable
    private int maxTicks;

    @Shadow
    @Mutable
    private long setupTicks;

    @Shadow
    @Mutable
    private Rotation rotation;

    @Overwrite
    public int getMaxTicks()
    {
        return this.maxTicks;
    }

    @Overwrite
    public long getSetupTicks()
    {
        return setupTicks;
    }

    @Overwrite
    public boolean isRequired()
    {
        return required;
    }
}
