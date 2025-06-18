// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.test.shared;

import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.function.Consumer;

/***
 * Helpers for serialising and unserialising values.
 */
public final class SerialisationUtils {
    private SerialisationUtils() {
    }

    public static CompoundTag writeNBT(Consumer<ValueOutput> generate) {
        var output = TagValueOutput.createWithoutContext(ProblemReporter.DISCARDING);
        generate.accept(output);
        return output.buildResult();
    }

    public static void readNBT(CompoundTag tag, Consumer<ValueInput> generate) {
        generate.accept(TagValueInput.create(ProblemReporter.DISCARDING, RegistryAccess.EMPTY, tag));
    }
}
