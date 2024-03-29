// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

package cc.tweaked.linter

import com.google.errorprone.BugPattern
import com.google.errorprone.VisitorState
import com.google.errorprone.bugpatterns.BugChecker
import com.google.errorprone.matchers.Description
import com.google.errorprone.util.ASTHelpers
import com.sun.source.tree.IdentifierTree
import com.sun.source.tree.MemberSelectTree
import com.sun.source.tree.Tree
import com.sun.tools.javac.code.Symbol
import java.util.*
import java.util.stream.Stream
import javax.lang.model.element.AnnotationMirror
import javax.lang.model.element.ElementKind

enum class Side { CLIENT, SERVER, BOTH }

@BugPattern(
    summary = "Checks client-only code is not used.",
    explanation = """
        Errors on any reference to client-only classes, fields or identifiers.
    """,
    severity = BugPattern.SeverityLevel.ERROR,
    tags = [BugPattern.StandardTags.LIKELY_ERROR],
)
class SideChecker : BugChecker(), BugChecker.IdentifierTreeMatcher, BugChecker.MemberSelectTreeMatcher {
    override fun matchIdentifier(tree: IdentifierTree, state: VisitorState): Description {
        val sym = ASTHelpers.getSymbol(tree)!!
        return when (sym.getKind()) {
            ElementKind.LOCAL_VARIABLE, ElementKind.TYPE_PARAMETER -> Description.NO_MATCH
            else -> report(tree, state)
        }
    }

    override fun matchMemberSelect(tree: MemberSelectTree, state: VisitorState): Description {
        // Skip imports: We'll catch these later on.
        if (state.path.any { it.kind == Tree.Kind.IMPORT }) return Description.NO_MATCH

        val reify = ASTHelpers.getSymbol(tree)
        return if (reify is Symbol.TypeSymbol) report(tree, state) else Description.NO_MATCH
    }

    private fun report(tree: Tree, state: VisitorState): Description {
        return when (state.sideProvider.getSide(tree)) {
            Side.CLIENT -> buildDescription(tree).setMessage("Using client-only symbol in common source set").build()
            Side.SERVER -> buildDescription(tree).setMessage("Using server-only symbol in common source set").build()
            Side.BOTH -> Description.NO_MATCH
        }
    }
}

internal class SideProvider {
    private val cache = mutableMapOf<Symbol, Optional<Side>>()

    fun getSide(tree: Tree?): Side {
        val sym = ASTHelpers.getSymbol(tree)
        return if (sym == null) Side.BOTH else getSide(sym).orElse(Side.BOTH)
    }

    private fun getSide(sym: Symbol): Optional<Side> {
        val existing = cache[sym]
        if (existing != null) return existing

        val side = getSideImpl(sym)
        cache[sym] = side
        return side
    }

    private fun getSideImpl(sym: Symbol): Optional<Side> = when (sym.getKind()) {
        ElementKind.MODULE -> Optional.empty()
        ElementKind.PACKAGE -> {
            val pkg = sym.toString()
            when {
                (clientPackages.any { pkg.startsWith(it) } || pkg.splitToSequence(".").contains("client")) &&
                    !notClientPackages.contains(pkg) -> Optional.of(Side.CLIENT)

                else -> Optional.empty()
            }
        }

        else ->
            fromAnnotationStream(sym.annotationMirrors.stream())
                .or { Optional.ofNullable(sym.enclosingElement).flatMap { getSide(it) } }
    }

    private fun fromAnnotationStream(annotations: Stream<out AnnotationMirror>) =
        annotations.flatMap {
            when (it.annotationType.toString()) {
                "net.minecraftforge.api.distmarker.OnlyIn" -> {
                    val value = it.getValue("value", AnnotationGetters.enum<Side>())!!
                    Stream.of(value)
                }

                else -> Stream.empty()
            }
        }.findFirst()

    companion object {
        private val notClientPackages = listOf(
            // Ugly! But we do what we must.
            "dan200.computercraft.shared.network.client",
        )

        private val clientPackages = listOf(
            "org.lwjgl.",
        )
    }
}

internal val VisitorState.sideProvider: SideProvider
    get() = getContext { SideProvider() }
