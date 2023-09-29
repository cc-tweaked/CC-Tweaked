// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package cc.tweaked.linter

import com.google.errorprone.BugPattern
import com.google.errorprone.ErrorProneFlags
import com.google.errorprone.VisitorState
import com.google.errorprone.bugpatterns.BugChecker
import com.google.errorprone.fixes.SuggestedFix
import com.google.errorprone.matchers.Description
import com.google.errorprone.util.ASTHelpers
import com.sun.source.tree.MethodTree
import com.sun.tools.javac.code.Symbol
import java.util.*
import javax.lang.model.element.Modifier

internal object LoaderOverrides {
    private const val FORGE_ANNOTATION: String = "dan200.computercraft.annotations.ForgeOverride"
    private const val FABRIC_ANNOTATION: String = "dan200.computercraft.annotations.FabricOverride"

    fun hasOverrideAnnotation(symbol: Symbol.MethodSymbol, state: VisitorState) =
        ASTHelpers.hasAnnotation(symbol, "java.lang.Override", state)

    fun getAnnotation(flags: ErrorProneFlags) = when (flags.get("ModLoader").orElse(null)) {
        "forge" -> FORGE_ANNOTATION
        "fabric" -> FABRIC_ANNOTATION
        null -> null
        else -> throw IllegalArgumentException("Unknown mod loader")
    }
}

@BugPattern(
    summary = "Require an @Override or @ForgeOverride/@FabricOverride annotation on a class.",
    explanation = """
        All methods which override a method or implement an interface should use @Override. When overriding
        loader-specific methods, you should use @ForgeOverride or @FabricOverride instead.
    """,
    severity = BugPattern.SeverityLevel.WARNING,
    tags = [BugPattern.StandardTags.STYLE],
)
class MissingLoaderOverride(flags: ErrorProneFlags? = null) : BugChecker(), BugChecker.MethodTreeMatcher {
    private val annotation = if (flags == null) null else LoaderOverrides.getAnnotation(flags)

    override fun matchMethod(tree: MethodTree, state: VisitorState): Description {
        if (annotation == null) throw IllegalStateException("MissingLoaderOverride requires the ModLoader flag")

        val symbol = ASTHelpers.getSymbol(tree)
        if (symbol.isStatic) return Description.NO_MATCH

        // If the method is annotated with @Override or our annotation, then skip.
        if (LoaderOverrides.hasOverrideAnnotation(symbol, state) || ASTHelpers.hasAnnotation(symbol, annotation, state)) {
            return Description.NO_MATCH
        }

        return Optional.ofNullable(symbol.getAnySuperMethod(state.types)).map { override ->
            buildDescription(tree)
                .addFix(SuggestedFix.prefixWith(tree, "@Override "))
                .setMessage(
                    String.format(
                        "%s %s method in %s; expected @Override",
                        symbol.simpleName,
                        if (override.enclClass().isInterface || override.modifiers.contains(Modifier.ABSTRACT)) "implements" else "overrides",
                        override.enclClass().simpleName,
                    ),
                )
                .build()
        }.orElse(Description.NO_MATCH)
    }
}

@BugPattern(
    summary = "@ForgeOverride does not match a super method.",
    severity = BugPattern.SeverityLevel.ERROR,
    tags = [BugPattern.StandardTags.LIKELY_ERROR],
)
class LoaderOverride(flags: ErrorProneFlags? = null) : BugChecker(), BugChecker.MethodTreeMatcher {
    private val annotation = if (flags == null) null else LoaderOverrides.getAnnotation(flags)

    override fun matchMethod(tree: MethodTree, state: VisitorState): Description {
        if (annotation == null) throw IllegalStateException("LoaderOverride requires the ModLoader flag")

        val symbol = ASTHelpers.getSymbol(tree)
        if (symbol.isStatic) return Description.NO_MATCH

        // Check we're annotated and missing a parent method.
        if (!ASTHelpers.hasAnnotation(symbol, annotation, state)) return Description.NO_MATCH
        if (symbol.getAnySuperMethod(state.types) != null) return Description.NO_MATCH

        return buildDescription(tree)
            .setMessage("Method ${symbol.simpleName} does not override method from its superclass")
            .build()
    }
}
