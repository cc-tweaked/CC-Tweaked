// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Classpath;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;

/**
 * Compile Java to Javascript or WASM via TeaVM.
 */
public abstract class CompileTeaVM extends DefaultTask {
    /**
     * The classpath to compile.
     */
    @Classpath
    public abstract ConfigurableFileCollection getClasspath();

    /**
     * The main class. This is used as the entrypoint to the program.
     */
    @Input
    public abstract Property<String> getMainClass();

    /**
     * Whether to minify the resulting files.
     */
    @Input
    public abstract Property<Boolean> getMinify();

    /**
     * The folder to write files to.
     */
    @OutputFile
    public abstract RegularFileProperty getOutput();

    /**
     * The classpath containing our tooling jars.
     */
    @Classpath
    public abstract ConfigurableFileCollection getTooling();

    @Inject
    protected abstract WorkerExecutor getWorkerExecutor();

    public CompileTeaVM() {
        setDescription("Compile TeaVM");
        setGroup(BasePlugin.BUILD_GROUP);

        getMinify().convention(true);
        getTooling().convention(getProject().getConfigurations().named(TeaVMPlugin.CONFIGURATION_RESOLVABLE));
    }

    @TaskAction
    public void run() {
        getWorkerExecutor()
            .classLoaderIsolation(action -> action.getClasspath().from(getTooling()))
            .submit(CompileTeaVMAction.class, params -> {
                params.getClasspath().from(getClasspath());
                params.getMainClass().set(getMainClass());
                params.getMinify().set(getMinify());
                params.getOutput().set(getOutput());
            });
    }
}
