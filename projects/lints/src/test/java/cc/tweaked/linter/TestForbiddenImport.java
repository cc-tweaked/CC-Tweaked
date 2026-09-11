// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.linter;

import com.google.common.base.Predicates;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

public class TestForbiddenImport {
    private final CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(ForbiddenImport.class, getClass());

    @Test
    public void testForbiddenImport() {
        compilationHelper
            .addSourceLines(
                "Import.java",
                """
                // BUG: Diagnostic matches: X
                import org.jspecify.annotations.NonNull;

                class X {}
                """
            )
            .expectErrorMessage("X", Predicates.containsPattern("Cannot import this symbol"))
            .doTest();
    }

    @Test
    public void testForbiddenImportSuggestion() {
        compilationHelper
            .addSourceLines(
                "Import.java",
                """
                // BUG: Diagnostic matches: X
                import javax.annotation.concurrent.GuardedBy;

                class X {}
                """
            )
            .expectErrorMessage("X", Predicates.containsPattern("Did you mean 'import com.google.errorprone.annotations.concurrent.GuardedBy;'"))
            .doTest();
    }
}
