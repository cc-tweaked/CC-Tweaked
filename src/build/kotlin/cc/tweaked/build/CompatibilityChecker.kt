// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.build

import org.objectweb.asm.*
import org.objectweb.asm.commons.ClassRemapper
import org.objectweb.asm.commons.MethodRemapper
import org.objectweb.asm.commons.Remapper
import java.lang.reflect.Executable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull

private val logger = logger { }

private inline fun <T> tryReflection(f: () -> T): T? = try {
    f()
} catch (_: ReflectiveOperationException) {
    null
}

/**
 * Caches existence of methods/classes in the current classpath.
 */
private object ExistenceCache {
    private val typeCache = ConcurrentHashMap<String, Optional<Class<*>>>()
    private val methodCache = ConcurrentHashMap<Desc, Boolean>()

    private fun getClassFromInternal(name: String): Class<*>? {
        val klass = typeCache.computeIfAbsent(name) {
            Optional.ofNullable(tryReflection { Class.forName(it.replace('/', '.')) })
        }
        return klass.getOrNull()
    }

    private fun getClass(ty: Type): Class<*>? {
        return when (ty.sort) {
            Type.VOID -> Void.TYPE
            Type.BOOLEAN -> Boolean::class.java
            Type.CHAR -> Char::class.java
            Type.BYTE -> Byte::class.java
            Type.SHORT -> Short::class.java
            Type.INT -> Int::class.java
            Type.FLOAT -> Float::class.java
            Type.LONG -> Long::class.java
            Type.DOUBLE -> Double::class.java
            Type.OBJECT -> getClassFromInternal(ty.internalName)
            Type.ARRAY -> {
                val klass = getClass(ty.elementType) ?: return null
                java.lang.reflect.Array.newInstance(klass, 0).javaClass
            }

            else -> throw IllegalArgumentException("Unknown type $ty")
        }
    }

    fun classExists(name: String): Boolean = !name.startsWith("java/") || getClassFromInternal(name) != null

    private fun getMethod(className: String, methodName: String, descriptor: String): Executable? {
        val klass = getClassFromInternal(className) ?: return null
        val retTy = getClass(Type.getReturnType(descriptor)) ?: return null

        val argTypes = Type.getArgumentTypes(descriptor)
        val args = arrayOfNulls<Class<*>>(argTypes.size)
        for ((i, arg) in argTypes.iterator().withIndex()) {
            args[i] = getClass(arg) ?: return null
        }

        return if (methodName == "<init>") {
            tryReflection { klass.getDeclaredConstructor(*args) }
        } else {
            val method = tryReflection { klass.getMethod(methodName, *args) }
                ?: tryReflection { klass.getDeclaredMethod(methodName, *args) }
                ?: return null
            if (method.returnType != retTy) return null
            return method
        }
    }

    fun methodExists(className: String, methodName: String, descriptor: String): Boolean =
        !className.startsWith("java/") || methodCache.computeIfAbsent(Desc(className, methodName, descriptor)) {
            getMethod(it.owner, it.name, it.descriptor) != null
        }
}

private class CheckingRemapper : Remapper(Opcodes.ASM9) {
    lateinit var owner: String

    override fun mapType(internalName: String): String {
        if (!ExistenceCache.classExists(internalName)) logger.error("{} references non-existent class {}", owner, internalName)
        return internalName
    }
}

/**
 * Checks if the given class is compatible with the current JVM, and doesn't use classes or methods from a future Java
 * version.
 */
class CompatibilityChecker(cv: ClassVisitor) : ClassRemapper(Opcodes.ASM9, cv, CheckingRemapper()) {
    private var isInterface: Boolean = false

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        if (superName == null) println(name)
        (remapper as CheckingRemapper).owner = name
        isInterface = (access and Opcodes.ACC_INTERFACE) != 0

        super.visit(version, access, name, signature, superName, interfaces)
    }

    override fun visitNestHost(nestHost: String) {
        logger.error("{} is a nest member of {}", className, nestHost)
        super.visitNestHost(nestHost)
    }

    override fun visitNestMember(nestMember: String) {
        logger.error("{} has a nest member {}", className, nestMember)
        super.visitNestMember(nestMember)
    }

    override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        if (isInterface && name != "<clinit>") {
            if ((access and Opcodes.ACC_STATIC) != 0) {
                logger.error("Unsupported static method {}.{} on an interface", className, name)
                return null
            } else if ((access and Opcodes.ACC_ABSTRACT) == 0) {
                logger.error("Unsupported default method {}.{} on an interface", className, name)
                return null
            }
        }

        return super.visitMethod(access, name, descriptor, signature, exceptions)
    }

    override fun visitRecordComponent(name: String, descriptor: String, signature: String?): RecordComponentVisitor {
        logger.error("{} has a record component {}", className, name)
        return super.visitRecordComponent(name, descriptor, signature)
    }

    override fun createMethodRemapper(methodVisitor: MethodVisitor): MethodVisitor =
        CompatibilityMethodVisitor(methodVisitor, remapper)
}

private class CompatibilityMethodVisitor(mv: MethodVisitor, remapper: Remapper) :
    MethodRemapper(Opcodes.ASM9, mv, remapper) {

    private val className: String
        get() = (remapper as CheckingRemapper).owner

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
        if (!(ExistenceCache.methodExists(owner, name, descriptor))) {
            logger.error("{} references non-existent method {}.{}{}", className, owner, name, descriptor)
        }
        if (opcode == Opcodes.INVOKESTATIC && isInterface) {
            logger.error("{} calls static interface method {}.{}{}", className, owner, name, descriptor)
        }

        mv.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitLdcInsn(value: Any?) {
        if (value is Handle) logger.error("{} loads a method handle for {}.{}", className, value.owner, value.name)
        super.visitLdcInsn(value)
    }

    override fun visitInvokeDynamicInsn(name: String, descriptor: String, bootstrap: Handle, vararg bootstrapMethodArguments: Any?) {
        logger.error(
            "{} has an INDY call to {}.{}{} generating {}{}:{}",
            className,
            bootstrap.owner,
            bootstrap.name,
            bootstrap.desc,
            name,
            descriptor,
            bootstrapMethodArguments.withIndex().joinToString { (i, x) -> "\n [$i] $x" },
        )
        super.visitInvokeDynamicInsn(name, descriptor, bootstrap, *bootstrapMethodArguments)
    }
}
