// SPDX-FileCopyrightText: 2025 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.linter;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.NewClassTree;

import java.util.Map;
import java.util.Optional;

@BugPattern(
    summary = "Checks for usage of forbidden APIs.",
    severity = BugPattern.SeverityLevel.ERROR,
    tags = BugPattern.StandardTags.LIKELY_ERROR
)
@SuppressWarnings("serial")
public final class ForbiddenAPI extends BugChecker implements BugChecker.ImportTreeMatcher, BugChecker.NewClassTreeMatcher {
    private static final Map<String, Optional<String>> FORBIDDEN_IMPORTS = Map.of(
        // We ban all @Nonnull annotations, because that should be default already.
        "javax.annotation.Nonnull", Optional.empty(),
        "org.jetbrains.annotations.NotNull", Optional.empty(),
        "org.jspecify.annotations.NonNull", Optional.empty(),
        // Ban JSR 305 and JetBrains @Nullable, and prefer the JSpecify one.
        "javax.annotation.Nullable", Optional.of("org.jspecify.annotations.Nullable"),
        "org.jetbrains.annotations.Nullable", Optional.of("org.jspecify.annotations.Nullable"),
        // Prefer ErrorProne annotations over JSR ones.
        "javax.annotation.CheckReturnValue", Optional.of("com.google.errorprone.annotations.CheckReturnValue"),
        "javax.annotation.OverridingMethodsMustInvokeSuper", Optional.of("com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper"),
        "javax.annotation.concurrent.GuardedBy", Optional.of("com.google.errorprone.annotations.concurrent.GuardedBy")
    );

    private static final Map<String, Optional<String>> FORBIDDEN_INSTANCES = Map.of(
        "java.util.concurrent.locks.ReentrantLock", Optional.of("ThreadUtils.newReentrantLock()"),
        "java.util.concurrent.locks.ReentrantReadWriteLock", Optional.of("ThreadUtils.newReentrantReadWriteLock()")
    );

    @Override
    public Description matchImport(ImportTree tree, VisitorState state) {
        if (tree.isStatic()) return Description.NO_MATCH;

        var sym = ASTHelpers.getSymbol(tree.getQualifiedIdentifier());
        if (sym == null) return Description.NO_MATCH;

        var importedName = sym.getQualifiedName().toString();
        var forbidden = FORBIDDEN_IMPORTS.get(importedName);
        if (forbidden == null) return Description.NO_MATCH;

        var message = buildDescription(tree.getQualifiedIdentifier()).setMessage("Cannot import this symbol");
        forbidden.ifPresent(s -> message.addFix(SuggestedFix.replace(tree.getQualifiedIdentifier(), s)));

        return message.build();
    }

    @Override
    public Description matchNewClass(NewClassTree tree, VisitorState state) {
        var sym = ASTHelpers.getSymbol(tree.getIdentifier());
        if (sym == null) return Description.NO_MATCH;

        var className = sym.getQualifiedName().toString();
        var forbidden = FORBIDDEN_INSTANCES.get(className);
        if (forbidden == null) return Description.NO_MATCH;

        var message = buildDescription(tree.getIdentifier()).setMessage("Cannot construct this class");
        forbidden.ifPresent(s -> message.addFix(SuggestedFix.replace(tree, s)));

        return message.build();
    }
}
