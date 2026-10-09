// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.linter;

import com.google.common.base.Predicates;
import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

public class TestForbiddenAPI {
    private final CompilationTestHelper compilationHelper = CompilationTestHelper.newInstance(ForbiddenAPI.class, getClass());

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

    @Test
    public void testForbiddenNewInstance() {
        compilationHelper
            .addSourceLines(
                "Import.java",
                """
                import java.util.concurrent.locks.ReentrantLock;

                class X {
                  // BUG: Diagnostic matches: X
                  private final ReentrantLock lock = new ReentrantLock();
                }
                """
            )
            .expectErrorMessage("X", Predicates.containsPattern("private final ReentrantLock lock = ThreadUtils.newReentrantLock\\(\\);"))
            .doTest();
    }
}
