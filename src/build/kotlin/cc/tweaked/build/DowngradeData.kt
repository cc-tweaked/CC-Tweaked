// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.build

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*
import org.objectweb.asm.commons.Remapper
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

private val logger = logger { }

/**
 * Gathers data about method calls in the program in order to find what private methods need to be widened.
 */
class DowngradeData {
    private class AccessInfo {
        var access: Int = 0
        var accessedExternally: Boolean = false
        var uniqueName: String? = null
    }

    private val members = HashMap<Desc, AccessInfo>()

    class ClassInfo(
        val access: Int,
        val name: String,
        val superclass: String?,
        val interfaces: Array<out String>?,
        val methods: List<MethodInfo>,
    ) {
        val isInterface: Boolean
            get() = access.testMask(ACC_INTERFACE)
    }

    class MethodInfo(val access: Int, val name: String, val descriptor: String)

    private val classes: MutableMap<String, ClassInfo> = HashMap()
    private val parentClasses: MutableMap<String, Set<ClassInfo>> = HashMap()

    private fun getMember(owner: String, name: String, descriptor: String) =
        members.computeIfAbsent(Desc(owner, name, descriptor)) { AccessInfo() }

    private inner class ClassScanner : ClassVisitor(ASM9) {
        private var access: Int = 0
        lateinit var className: String
        private var superName: String? = null
        private var interfaces: Array<out String>? = null
        private val methods: MutableList<MethodInfo> = mutableListOf()

        override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
            this.className = name
            this.access = access
            this.superName = superName
            this.interfaces = interfaces

            super.visit(version, access, name, signature, superName, interfaces)
        }

        override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            getMember(className, name, descriptor).access = access
            methods.add(MethodInfo(access, name, descriptor))

            return object : MethodVisitor(ASM9) {
                override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
                    if (owner != className) getMember(owner, name, descriptor).accessedExternally = true
                    super.visitFieldInsn(opcode, owner, name, descriptor)
                }

                override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
                    if (owner != className) getMember(owner, name, descriptor).accessedExternally = true
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
                }

                override fun visitInvokeDynamicInsn(name: String, descriptor: String, bootstrapMethodHandle: Handle, vararg bootstrapMethodArguments: Any) {
                    for (arg in bootstrapMethodArguments) {
                        if (arg is Handle) getMember(arg.owner, arg.name, arg.desc).accessedExternally = true
                    }
                    super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
                }
            }
        }

        override fun visitField(access: Int, name: String, descriptor: String, signature: String?, value: Any?): FieldVisitor? {
            getMember(className, name, descriptor).access = access
            return super.visitField(access, name, descriptor, signature, value)
        }

        override fun visitEnd() {
            super.visitEnd()
            classes[className] = ClassInfo(access, className, superName, interfaces, methods)
        }
    }

    /**
     * Determine if a method or field should be widened, removing its [ACC_PRIVATE] modifier.
     */
    fun shouldWiden(owner: String, name: String, descriptor: String): Boolean {
        val info = members[Desc(owner, name, descriptor)] ?: return false
        return info.accessedExternally && info.access.testMask(ACC_PRIVATE)
    }

    /**
     * Get the name of a method.
     *
     * When accessing a private instance method, we rename it to a unique method, to prevent accidental overriding
     * in child/parent classes.
     *
     * Typically one would use the [remapper] instead.
     */
    fun getMethodName(owner: String, name: String, descriptor: String): String {
        val info = members[Desc(owner, name, descriptor)] ?: return name
        return if (info.accessedExternally && info.access.testMask(ACC_PRIVATE) && !info.access.testMask(ACC_STATIC) && name != "<init>") {
            //  If this is a private instance member, which is accessed from another class, then we must rename it.
            info.uniqueName ?: run {
                val digest = MessageDigest.getInstance("MD5")
                digest.update(owner.toByteArray(StandardCharsets.UTF_8))
                digest.update(name.toByteArray(StandardCharsets.UTF_8))
                digest.update(descriptor.toByteArray(StandardCharsets.UTF_8))
                val hash = digest.digest().toHexString(0, 4)

                val uniqueName = String.format("%s$%s", name, hash)
                logger.debug("Renaming private member {}.{}{} to {}", owner, name, descriptor, uniqueName)
                info.uniqueName = uniqueName
                uniqueName
            }
        } else {
            name
        }
    }

    val remapper = object : Remapper(ASM9) {
        override fun mapMethodName(owner: String, name: String, desc: String): String = getMethodName(owner, name, desc)
    }

    fun getClassInfo(name: String): ClassInfo? = classes[name]

    fun getAllClassInfo(name: String): Set<ClassInfo> = parentClasses.computeIfAbsent(
        name,
        {
            val info = getClassInfo(name) ?: return@computeIfAbsent emptySet()

            val infos = mutableSetOf<ClassInfo>()
            infos.add(info)
            if (info.superclass != null) infos.addAll(getAllClassInfo(info.superclass))
            if (info.interfaces != null) {
                for (iface in info.interfaces) infos.addAll(getAllClassInfo(iface))
            }
            infos
        },
    )

    fun addClass(classReader: ClassReader) {
        classReader.accept(ClassScanner(), 0)
    }
}
