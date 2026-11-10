// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.build

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.util.CheckClassAdapter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private val logger = logger {}

/** Generate additional classes which don't exist in the original source set. */
interface ClassEmitter {
    /** Emit a class if it does not already exist. */
    fun generate(name: String, flags: Int = 0, write: (ClassVisitor) -> Unit)
}

/** An implementation of [ClassEmitter] which writes files to a zip/jar. */
class ZipClassEmitter(private val output: ZipOutputStream) : ClassEmitter {
    override fun generate(name: String, flags: Int, write: (ClassVisitor) -> Unit) {
        logger.debug("Writing {}", name)
        val cw = ClassWriter(flags)
        write(cw)
        val bytes = cw.toByteArray()

        val cr = ClassReader(bytes)
        try {
            cr.accept(CompatibilityChecker(CheckClassAdapter(cw)), 0)
        } catch (e: IllegalArgumentException) {
            logger.error("Failed to verify $name: ${e.message}")
        }

        val newEntry = ZipEntry("$name.class")
        newEntry.time = 0
        output.putNextEntry(newEntry)
        output.write(bytes)
    }
}
