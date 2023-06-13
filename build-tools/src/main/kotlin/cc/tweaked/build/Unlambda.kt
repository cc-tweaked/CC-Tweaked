// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.build

import org.objectweb.asm.*
import org.objectweb.asm.Opcodes.*

class Unlambda(private val emitter: ClassEmitter, visitor: ClassVisitor) :
    ClassVisitor(ASM9, visitor) {
    internal lateinit var className: String
    private var isInterface: Boolean = false

    private var lambda = 0

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>?) {
        super.visit(V1_6, access, name, signature, superName, interfaces)

        if (version != V1_8) throw IllegalStateException("Expected Java version 8")
        className = name
        isInterface = (access and ACC_INTERFACE) != 0
    }

    override fun visitMethod(access: Int, name: String, descriptor: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        val access = if (access.and(ACC_STATIC) != 0) access.and(ACC_PRIVATE.inv()) else access
        val mw = super.visitMethod(access, name, descriptor, signature, exceptions) ?: return null

        if (isInterface && name != "<clinit>") {
            if ((access and ACC_STATIC) != 0) println("[WARN] $className.$name is a static method")
            else if ((access and ACC_ABSTRACT) == 0) println("[WARN] $className.$name is a default method")
        }

        return UnlambdaMethodVisitor(this, emitter, mw)
    }

    internal fun nextLambdaName(): String {
        val name = "lambda$lambda"
        lambda++
        return name
    }
}

internal class UnlambdaMethodVisitor(
    private val parent: Unlambda,
    private val emitter: ClassEmitter,
    methodVisitor: MethodVisitor,
) : MethodVisitor(ASM9, methodVisitor) {
    private class Bridge(val lambda: Handle, val bridgeName: String)

    private val bridgeMethods = mutableListOf<Bridge>()

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
        if (opcode == INVOKESTATIC && isInterface) println("[WARN] Invoke interface $owner.$name in ${parent.className}")
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitInvokeDynamicInsn(name: String, descriptor: String, handle: Handle, vararg arguments: Any) {
        if (handle.owner == "java/lang/invoke/LambdaMetafactory" && handle.name == "metafactory" && handle.desc == "(Ljava/lang/invoke/MethodHandles\$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;") {
            visitLambda(name, descriptor, arguments[0] as Type, arguments[1] as Handle)
        } else {
            super.visitInvokeDynamicInsn(name, descriptor, handle, *arguments)
        }
    }

    private fun visitLambda(name: String, descriptor: String, signature: Type, lambda: Handle) {
        val interfaceTy = Type.getReturnType(descriptor)
        val fields = Type.getArgumentTypes(descriptor)

        val lambdaName = parent.nextLambdaName()
        val className = "${parent.className}\$$lambdaName"
        val bridgeName = "${lambdaName}Bridge"

        emitter.generate(className, flags = ClassWriter.COMPUTE_MAXS) { cw ->
            cw.visit(V1_6, ACC_FINAL, className, null, "java/lang/Object", arrayOf(interfaceTy.internalName))
            for ((i, ty) in fields.withIndex()) {
                cw.visitField(ACC_PRIVATE or ACC_FINAL, "field$i", ty.descriptor, null, null)
                    .visitEnd()
            }

            cw.visitMethod(ACC_STATIC, "create", Type.getMethodDescriptor(interfaceTy, *fields), null, null).let { mw ->
                mw.visitCode()
                mw.visitTypeInsn(NEW, className)
                mw.visitInsn(DUP)
                for ((i, ty) in fields.withIndex()) mw.visitVarInsn(ty.getOpcode(ILOAD), i)
                mw.visitMethodInsn(INVOKESPECIAL, className, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, *fields), false)
                mw.visitInsn(ARETURN)
                mw.visitMaxs(0, 0)
                mw.visitEnd()
            }

            cw.visitMethod(0, "<init>", Type.getMethodDescriptor(Type.VOID_TYPE, *fields), null, null).let { mw ->
                mw.visitCode()
                mw.visitVarInsn(ALOAD, 0)
                mw.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
                for ((i, ty) in fields.withIndex()) {
                    mw.visitVarInsn(ALOAD, 0)
                    mw.visitVarInsn(ty.getOpcode(ILOAD), i + 1)
                    mw.visitFieldInsn(PUTFIELD, className, "field$i", ty.descriptor)
                }
                mw.visitInsn(RETURN)
                mw.visitMaxs(0, 0)
                mw.visitEnd()
            }

            cw.visitMethod(ACC_PUBLIC, name, signature.descriptor, null, null).let { mw ->
                mw.visitCode()

                val targetArgs = when (lambda.tag) {
                    H_INVOKEVIRTUAL, H_INVOKESPECIAL -> arrayOf(
                        Type.getObjectType(lambda.owner),
                        *Type.getArgumentTypes(lambda.desc),
                    )

                    H_INVOKESTATIC, H_NEWINVOKESPECIAL -> Type.getArgumentTypes(lambda.desc)
                    else -> throw IllegalStateException("Unhandled opcode")
                }
                var targetArgOffset = 0

                // If we're a ::new method handle, create the object.
                if (lambda.tag == H_NEWINVOKESPECIAL) {
                    mw.visitTypeInsn(NEW, lambda.owner)
                    mw.visitInsn(DUP)
                }

                // Load our fields
                for ((i, ty) in fields.withIndex()) {
                    mw.visitVarInsn(ALOAD, 0)
                    mw.visitFieldInsn(GETFIELD, className, "field$i", ty.descriptor)

                    val expectedTy = targetArgs[targetArgOffset]
                    if (ty != expectedTy) println("$ty != $expectedTy")
                    targetArgOffset++
                }

                // Load the additional arguments
                val arguments = signature.argumentTypes
                for ((i, ty) in arguments.withIndex()) {
                    mw.visitVarInsn(ty.getOpcode(ILOAD), i + 1)
                    val expectedTy = targetArgs[targetArgOffset]
                    if (ty != expectedTy) {
                        println("[WARN] $ty != $expectedTy, adding a cast")
                        mw.visitTypeInsn(CHECKCAST, expectedTy.internalName)
                    }
                    targetArgOffset++
                }

                // Invoke our init call
                mw.visitMethodInsn(
                    when (lambda.tag) {
                        H_INVOKEVIRTUAL, H_INVOKESPECIAL -> INVOKEVIRTUAL
                        H_INVOKESTATIC -> INVOKESTATIC
                        H_NEWINVOKESPECIAL -> INVOKESPECIAL
                        else -> throw IllegalStateException("Unhandled opcode")
                    },
                    lambda.owner, if (lambda.tag == H_INVOKESPECIAL) bridgeName else lambda.name, lambda.desc, false,
                )


                if (lambda.tag != H_NEWINVOKESPECIAL) {
                    val expectedRetTy = signature.returnType
                    val retTy = Type.getReturnType(lambda.desc)
                    if (expectedRetTy != retTy) {
                        // println("[WARN] $retTy != $expectedRetTy, adding a cast")
                        if (retTy == Type.INT_TYPE && expectedRetTy.descriptor == "Ljava/lang/Object;") {
                            mw.visitMethodInsn(INVOKESTATIC, "jav/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false)
                        } else {
                            // println("[ERROR] Unhandled")
                        }

                    }
                }

                // A little ugly special handling for ::new
                mw.visitInsn(
                    if (lambda.tag == H_NEWINVOKESPECIAL) ARETURN else signature.returnType.getOpcode(IRETURN),
                )
                mw.visitMaxs(0, 0)
                mw.visitEnd()
            }

            cw.visitEnd()
        }

        // If we're a ::new method handle, create the object.
        if (lambda.tag == H_INVOKESPECIAL) {
            bridgeMethods.add(Bridge(lambda, bridgeName))
        }

        visitMethodInsn(INVOKESTATIC, className, "create", Type.getMethodDescriptor(interfaceTy, *fields), false)
    }

    override fun visitEnd() {
        super.visitEnd()

        for (bridge in bridgeMethods) {
            println("[INFO] Using bridge method ${bridge.bridgeName} for ${bridge.lambda}")
            val mw = parent.visitMethod(ACC_PUBLIC, bridge.bridgeName, bridge.lambda.desc, null, null) ?: continue
            mw.visitCode()
            mw.visitVarInsn(ALOAD, 0)
            for ((i, ty) in Type.getArgumentTypes(bridge.lambda.desc)
                .withIndex()) mw.visitVarInsn(ty.getOpcode(ILOAD), i + 1)
            mw.visitMethodInsn(INVOKESPECIAL, bridge.lambda.owner, bridge.lambda.name, bridge.lambda.desc, false)
            mw.visitInsn(Type.getReturnType(bridge.lambda.desc).getOpcode(IRETURN))
            val size = 1 + Type.getArgumentTypes(bridge.lambda.desc).size
            mw.visitMaxs(size, size)
            mw.visitEnd()
        }
    }
}
