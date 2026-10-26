// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.linter;

import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;

import javax.lang.model.element.Modifier;
import java.util.Set;

@BugPattern(
    summary = "Validates Mixin classes are package-private and abstract",
    severity = BugPattern.SeverityLevel.ERROR,
    tags = BugPattern.StandardTags.LIKELY_ERROR,
    linkType = BugPattern.LinkType.NONE
)
@SuppressWarnings("serial")
public class MixinModifiers extends BugChecker implements BugChecker.ClassTreeMatcher {
    private static final String MIXIN_ANNOTATION = "org.spongepowered.asm.mixin.Mixin";

    private static final Set<Modifier> CLASS_MODIFIERS = Set.of(Modifier.ABSTRACT);

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        if (!ASTHelpers.hasAnnotation(tree, MIXIN_ANNOTATION, state)) return Description.NO_MATCH;

        var modifiers = tree.getModifiers();
        var flags = modifiers.getFlags();

        return switch (tree.getKind()) {
            case CLASS -> flags.equals(CLASS_MODIFIERS)
                ? Description.NO_MATCH
                : buildDescription(modifiers).setMessage("Expected @Mixin classes to be package-local and abstract").build();
            case INTERFACE -> Description.NO_MATCH;
            default -> buildDescription(tree).setMessage("Can only apply @Mixin to classes and interfaces").build();
        };
    }
}
