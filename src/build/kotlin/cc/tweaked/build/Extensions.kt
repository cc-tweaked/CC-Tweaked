// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.build

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.Type
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter

/** A descriptor of a class's method or field. */
data class Desc(val owner: String, val name: String, val descriptor: String)

internal fun withMethodTraceVisitor(accept: (TraceMethodVisitor) -> Unit): String {
    val printer = Textifier()

    accept(TraceMethodVisitor(printer))

    val output = StringWriter()
    PrintWriter(output).use { printer.print(it) }
    return output.toString().trim()
}

/** Check if this is a reference type (an object or array). */
val Type.isReference: Boolean
    get() = when (sort) {
        Type.OBJECT, Type.ARRAY -> true
        Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT, Type.FLOAT, Type.DOUBLE, Type.LONG -> false
        else -> throw IllegalArgumentException("$this is not a value type")
    }

fun Int.testMask(mask: Int) = and(mask) == mask

/** Load a constant int, using the smaller instructions where possible. */
fun MethodVisitor.visitLoadInt(i: Int) = when (i) {
    -1 -> visitInsn(ICONST_M1)
    0 -> visitInsn(ICONST_0)
    1 -> visitInsn(ICONST_1)
    2 -> visitInsn(ICONST_2)
    3 -> visitInsn(ICONST_3)
    4 -> visitInsn(ICONST_4)
    5 -> visitInsn(ICONST_5)
    else -> visitLdcInsn(i)
}

val Type.boxedName: String?
    get() = when (sort) {
        Type.BOOLEAN -> "java/lang/Boolean"
        Type.BYTE -> "java/lang/Byte"
        Type.CHAR -> "java/lang/Character"
        Type.SHORT -> "java/lang/Short"
        Type.INT -> "java/lang/Integer"
        Type.FLOAT -> "java/lang/Float"
        Type.DOUBLE -> "java/lang/Double"
        Type.LONG -> "java/lang/Long"
        Type.OBJECT, Type.ARRAY -> null
        else -> throw java.lang.IllegalArgumentException("$this is not a value type")
    }

fun prefixSignature(owner: String, descriptor: String): String = Type.getMethodDescriptor(
    Type.getReturnType(descriptor),
    Type.getObjectType(owner),
    *Type.getArgumentTypes(descriptor),
)

@Suppress("NOTHING_TO_INLINE")
inline fun logger(noinline fn: () -> Unit): Logger =
    LoggerFactory.getLogger(fn.javaClass.toString().substringBefore("Kt$"))
