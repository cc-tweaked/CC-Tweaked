// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.build

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import java.nio.file.Files
import java.nio.file.Path

/** Generate additional classes which don't exist in the original source set. */
interface ClassEmitter {
    /** Emit a class if it does not already exist. */
    fun generate(name: String, classReader: ClassReader? = null, flags: Int = 0, write: (ClassVisitor) -> Unit)
}

/** An implementation of [ClassEmitter] which writes files to a directory. */
class FileClassEmitter(private val outputDir: Path) : ClassEmitter {
    private val emitted = mutableSetOf<String>()
    override fun generate(name: String, classReader: ClassReader?, flags: Int, write: (ClassVisitor) -> Unit) {
        if (!emitted.add(name)) return

        val cw = NonLoadingClassWriter(classReader, flags)
        write(CheckClassAdapter(cw))

        val outputFile = outputDir.resolve("$name.class")
        Files.createDirectories(outputFile.parent)
        Files.write(outputFile, cw.toByteArray())
    }
}

/** A unordered pair, such that (x, y) = (y, x) */
private class UnorderedPair<T>(private val x: T, private val y: T) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is cc.tweaked.build.UnorderedPair<*>) return false
        return (x == other.x && y == other.y) || (x == other.y && y == other.x)
    }

    override fun hashCode(): Int = x.hashCode() xor y.hashCode()
    override fun toString(): String = "UnorderedPair($x, $y)"
}

private val subclassRelations = mapOf<UnorderedPair<String>, String>(
)

/** A [ClassWriter] extension which avoids loading classes when computing frames. */
private class NonLoadingClassWriter(reader: ClassReader?, flags: Int) : ClassWriter(reader, flags) {
    override fun getCommonSuperClass(type1: String, type2: String): String {
        if (type1 == "java/lang/Object" || type2 == "java/lang/Object") return "java/lang/Object"

        val subclass = subclassRelations[UnorderedPair(type1, type2)]
        if (subclass != null) return subclass

        println("[WARN] Guessing the super-class of $type1 and $type2.")
        return "java/lang/Object"
    }
}
