// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.linter;

import com.google.common.base.Predicates;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

public class TestSideChecker {
    private final CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(SideChecker.class, getClass());

    @Test
    public void textExtendsAnnotated() {
        compilationHelper
            .addSourceLines("UsesClientOnly.java", """
                // BUG: Diagnostic matches: X
                class UsesClientOnly extends cc.tweaked.linter.AnnotatedClientClass {
                }
                """)
            .expectErrorMessage("X", Predicates.containsPattern("Using client-only symbol in common source set"))
            .doTest();
    }

    @Test
    public void testImportsAnnotated() {
        compilationHelper
            .addSourceLines("UsesClientOnly.java", """
                import cc.tweaked.linter.AnnotatedClientClass;
                // BUG: Diagnostic matches: X
                class UsesClientOnly extends AnnotatedClientClass {
                }
                """)
            .expectErrorMessage("X", Predicates.containsPattern("Using client-only symbol in common source set"))
            .doTest();
    }

    @Test
    public void textUsesAnnotated() {
        compilationHelper
            .addSourceLines("UsesClientOnly.java", """
                import cc.tweaked.linter.AnnotatedClientClass;
                class UsesClientOnly {
                    public void f() {
                        // BUG: Diagnostic matches: X
                        AnnotatedClientClass.doSomething();
                        // BUG: Diagnostic matches: Y
                        System.out.println(AnnotatedClientClass.field);
                        // BUG: Diagnostic matches: Z
                        AnnotatedClientClass.field = 0;
                    }
                }
                """)
            .expectErrorMessage("X", Predicates.containsPattern("Using client-only symbol in common source set"))
            .expectErrorMessage("Y", Predicates.containsPattern("Using client-only symbol in common source set"))
            .expectErrorMessage("Z", Predicates.containsPattern("Using client-only symbol in common source set"))
            .doTest();
    }

    @Test
    public void testExtendsPackage() {
        compilationHelper
            .addSourceLines("UsesClientOnly.java", """
                // BUG: Diagnostic matches: X
                class UsesClientOnly extends cc.tweaked.linter.client.PackageClientClass {
                }
                """)
            .expectErrorMessage("X", Predicates.containsPattern("Using client-only symbol in common source set"))
            .doTest();
    }
}
