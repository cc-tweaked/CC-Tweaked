// SPDX-FileCopyrightText: 2024 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package cc.tweaked.linter

import com.google.errorprone.BugPattern
import com.google.errorprone.VisitorState
import com.google.errorprone.bugpatterns.BugChecker
import com.google.errorprone.matchers.Description
import com.google.errorprone.util.ASTHelpers
import com.sun.source.tree.*
import com.sun.source.util.TreeScanner
import com.sun.tools.javac.code.Symbol.MethodSymbol
import javax.lang.model.element.Modifier

@BugPattern(
    summary = "Checks that a methods invoke their super method.",
    explanation = """
        This extends ErrorProne's built in "MustCallSuper" with several additional Minecraft-specific methods.
    """,
    severity = BugPattern.SeverityLevel.ERROR,
    tags = [BugPattern.StandardTags.LIKELY_ERROR],
)
class ExtraMustCallSuper : BugChecker(), BugChecker.MethodTreeMatcher {
    companion object {
        private val REQUIRED_METHODS = setOf(
            MethodReference("net.minecraft.world.level.block.entity.BlockEntity", "setRemoved"),
            MethodReference("net.minecraft.world.level.block.entity.BlockEntity", "clearRemoved"),
        )
    }

    override fun matchMethod(tree: MethodTree, state: VisitorState): Description {
        val methodSym: MethodSymbol = ASTHelpers.getSymbol(tree)
        if (methodSym.modifiers.contains(Modifier.ABSTRACT)) return Description.NO_MATCH

        val superMethod: MethodReference = findRequiredSuper(methodSym, state) ?: return Description.NO_MATCH
        val foundSuper = SuperScanner(superMethod.method).scan(tree, Unit) ?: false
        if (foundSuper) return Description.NO_MATCH

        return buildDescription(tree)
            .setMessage("This method overrides %s#%s but does not call the super method.".format(superMethod.owner, superMethod.method))
            .build()
    }

    private fun findRequiredSuper(method: MethodSymbol, state: VisitorState): MethodReference? {
        for (superMethod in ASTHelpers.findSuperMethods(method, state.types)) {
            val superName = MethodReference(superMethod.owner.qualifiedName.toString(), superMethod.name.toString())
            if (REQUIRED_METHODS.contains(superName)) return superName
        }
        return null
    }

    private data class MethodReference(val owner: String, val method: String)

    private class SuperScanner(private val methodName: String) : TreeScanner<Boolean?, Unit>() {
        // Skip visiting other elements.
        override fun visitClass(tree: ClassTree, state: Unit): Boolean = false
        override fun visitLambdaExpression(tree: LambdaExpressionTree, state: Unit): Boolean = false

        override fun visitMethodInvocation(tree: MethodInvocationTree, state: Unit): Boolean? {
            val methodSelect: ExpressionTree = tree.methodSelect
            if (methodSelect.kind == Tree.Kind.MEMBER_SELECT) {
                val memberSelect = methodSelect as MemberSelectTree
                if (ASTHelpers.isSuper(memberSelect.expression) && memberSelect.identifier.contentEquals(methodName)) return true
            }

            return super.visitMethodInvocation(tree, state)
        }

        override fun reduce(r1: Boolean?, r2: Boolean?): Boolean = (r1 ?: false) || (r2 ?: false)
    }
}
