// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.linter

import com.google.common.collect.ImmutableSet
import com.google.common.collect.ImmutableSetMultimap
import com.uber.nullaway.LibraryModels
import com.uber.nullaway.LibraryModels.FieldRef.fieldRef
import com.uber.nullaway.LibraryModels.MethodRef.methodRef

/**
 * Extends NullAway's model of Minecraft's code with a couple of extra annotations.
 */
class MinecraftLibraryModel : LibraryModels {
    override fun failIfNullParameters(): ImmutableSetMultimap<LibraryModels.MethodRef, Int> = ImmutableSetMultimap.of()
    override fun explicitlyNullableParameters(): ImmutableSetMultimap<LibraryModels.MethodRef, Int> =
        ImmutableSetMultimap.of()

    override fun nonNullParameters(): ImmutableSetMultimap<LibraryModels.MethodRef, Int> = ImmutableSetMultimap.of()
    override fun nullImpliesTrueParameters(): ImmutableSetMultimap<LibraryModels.MethodRef, Int> =
        ImmutableSetMultimap.of()

    override fun nullImpliesFalseParameters(): ImmutableSetMultimap<LibraryModels.MethodRef, Int> =
        ImmutableSetMultimap.of()

    override fun nullImpliesNullParameters(): ImmutableSetMultimap<LibraryModels.MethodRef, Int> =
        ImmutableSetMultimap.of()

    override fun castToNonNullMethods(): ImmutableSetMultimap<LibraryModels.MethodRef, Int> = ImmutableSetMultimap.of()
    override fun nullableReturns(): ImmutableSet<LibraryModels.MethodRef> = ImmutableSet.of()

    override fun nonNullReturns(): ImmutableSet<LibraryModels.MethodRef> = ImmutableSet.of(
        // Reasoning about nullability of BlockEntity.getLevel() is awkward. For now, assume it's non-null.
        methodRef("net.minecraft.world.level.block.entity.BlockEntity", "getLevel()"),
    )

    override fun nullableFields(): ImmutableSet<LibraryModels.FieldRef> = ImmutableSet.of(
        // This inherits from Minecraft.hitResult, and so can also be null.
        fieldRef("net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher", "cameraHitResult"),
    )
}
