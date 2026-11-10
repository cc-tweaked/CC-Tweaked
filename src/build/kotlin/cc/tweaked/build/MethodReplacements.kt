// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.build

object MethodReplacements {
    private const val BACKPORTS: String = "dan200/computercraft/util/JavaBackports"

    private fun makeStatic(owner: String, name: String, desc: String, newOwner: String = BACKPORTS, newName: String = name) =
        Desc(owner, name, desc) to Desc(newOwner, newName, desc)

    private fun makeInstance(owner: String, name: String, desc: String, newOwner: String = BACKPORTS, newName: String = name) =
        Desc(owner, name, desc) to Desc(newOwner, newName, prefixSignature(owner, desc))

    private val replacements: Map<Desc, Desc> = mapOf(
        makeStatic("java/lang/Byte", "compareUnsigned", "(BB)I"),
        makeStatic("java/util/Arrays", "compareUnsigned", "([B[B)I"),
        makeStatic("java/util/Arrays", "equals", "([BII[BII)Z"),
        makeStatic("java/util/Arrays", "mismatch", "([BII[BII)I"),
        makeStatic("java/util/List", "of", "()Ljava/util/List;", "java/util/Collections", "emptyList"),
        makeStatic("java/util/List", "of", "(Ljava/lang/Object;)Ljava/util/List;", "java/util/Collections", "singletonList"),
        makeStatic("java/util/List", "of", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/List;", newName = "listOf"),
        makeStatic("java/util/Map", "of", "()Ljava/util/Map;", "java/util/Collections", "emptyMap"),
        makeStatic("java/util/Objects", "requireNonNullElse", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
        makeStatic("java/util/Set", "of", "()Ljava/util/Set;", "java/util/Collections", "emptySet"),
        makeStatic("java/util/stream/Stream", "of", "([Ljava/lang/Object;)Ljava/util/stream/Stream;", "java/util/Arrays", "stream"),
        makeInstance("java/util/List", "toArray", "(Ljava/util/function/IntFunction;)[Ljava/lang/Object;"),
        makeInstance("java/util/stream/Stream", "toList", "()Ljava/util/List;"),
    )

    fun get(owner: String, name: String, desc: String): Desc? = replacements[Desc(owner, name, desc)]
}
