// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package cc.tweaked.linter

import com.google.errorprone.BugPattern
import com.google.errorprone.VisitorState
import com.google.errorprone.bugpatterns.BugChecker
import com.google.errorprone.fixes.SuggestedFix
import com.google.errorprone.matchers.Description
import com.google.errorprone.util.ASTHelpers
import com.sun.source.tree.ImportTree
import com.sun.source.tree.Tree

@BugPattern(
    summary = "Checks for forbidden imports.",
    severity = BugPattern.SeverityLevel.ERROR,
    tags = [BugPattern.StandardTags.LIKELY_ERROR],
)
class ForbiddenImport : BugChecker(), BugChecker.ImportTreeMatcher {
    override fun matchImport(tree: ImportTree, state: VisitorState): Description {
        if (tree.isStatic || tree.qualifiedIdentifier.kind != Tree.Kind.MEMBER_SELECT) return Description.NO_MATCH

        val sym = ASTHelpers.getSymbol(tree.qualifiedIdentifier) ?: return Description.NO_MATCH
        val importedName = sym.qualifiedName.toString()
        if (!IMPORTS.contains(importedName)) return Description.NO_MATCH

        val message = buildDescription(tree.qualifiedIdentifier).setMessage("Cannot import this symbol")
        val replacement = ALTERNATIVE_IMPORTS[importedName]
        if (replacement != null) message.addFix(SuggestedFix.replace(tree.qualifiedIdentifier, replacement))

        return message.build()
    }

    companion object {
        private val ALTERNATIVE_IMPORTS = mapOf(
            // Ban JSR 305 and JetBrains @Nullable, and prefer the JSpecify one.
            "javax.annotation.Nullable" to "org.jspecify.annotations.Nullable",
            "org.jetbrains.annotations.Nullable" to "org.jspecify.annotations.Nullable",
            // Prefer ErrorProne annotations over JSR ones.
            "javax.annotation.CheckReturnValue" to "com.google.errorprone.annotations.CheckReturnValue",
            "javax.annotation.OverridingMethodsMustInvokeSuper" to "com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper",
            "javax.annotation.concurrent.GuardedBy" to "com.google.errorprone.annotations.concurrent.GuardedBy",
        )

        private val IMPORTS: Set<String> = setOf(
            // We ban all @Nonnull annotations, because that should be default already.
            "javax.annotation.Nonnull",
            "org.jetbrains.annotations.NotNull",
            "org.jspecify.annotations.NonNull",
        ) + ALTERNATIVE_IMPORTS.keys
    }
}
