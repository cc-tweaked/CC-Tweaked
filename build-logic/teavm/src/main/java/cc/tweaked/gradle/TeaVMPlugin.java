// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class TeaVMPlugin implements Plugin<Project> {
    public static final String CONFIGURATION = "teavm";
    public static final String CONFIGURATION_RESOLVABLE = "teavmResolvable";

    @Override
    public void apply(Project target) {
        var config = target.getConfigurations().dependencyScope(CONFIGURATION);
        target.getConfigurations().resolvable(CONFIGURATION_RESOLVABLE, c -> c.extendsFrom(config));
    }
}
