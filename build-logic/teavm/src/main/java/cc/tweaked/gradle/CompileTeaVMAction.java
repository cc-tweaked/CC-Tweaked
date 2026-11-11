// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package cc.tweaked.gradle;

import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.problems.Problem;
import org.gradle.api.problems.Problems;
import org.gradle.api.provider.Property;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.backend.javascript.JSModuleType;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.ProblemSeverity;
import org.teavm.tooling.TeaVMProblemRenderer;
import org.teavm.tooling.TeaVMTargetType;
import org.teavm.tooling.TeaVMTool;
import org.teavm.tooling.TeaVMToolLog;
import org.teavm.vm.TeaVMOptimizationLevel;

import javax.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Worker action for {@link CompileTeaVM}.
 */
@SuppressWarnings("UnstableApiUsage")
public abstract class CompileTeaVMAction implements WorkAction<CompileTeaVMAction.Parameters> {
    interface Parameters extends WorkParameters {
        ConfigurableFileCollection getClasspath();

        Property<String> getMainClass();

        Property<Boolean> getMinify();

        RegularFileProperty getOutput();
    }

    @Inject
    protected abstract Problems getProblems();

    @Override
    public void execute() {
        var tool = new TeaVMTool();
        tool.setLog(new ToolLog());

        // Set up classpath
        var classes = StreamSupport.stream(getParameters().getClasspath().spliterator(), false).toList();
        tool.setClassPath(classes);
        tool.setClassLoader(new URLClassLoader(
            classes.stream().map(CompileTeaVMAction::toURL).toArray(URL[]::new),
            getClass().getClassLoader()
        ));
        tool.setMainClass("cc.tweaked.web.Main");

        // Set up output destination
        var output = getParameters().getOutput().get().getAsFile();
        tool.setTargetDirectory(output.getParentFile());
        tool.setTargetFileName(output.getName());
        tool.setTargetType(output.getName().endsWith(".wasm") ? TeaVMTargetType.WEBASSEMBLY_GC : TeaVMTargetType.JAVASCRIPT);

        // Configure various output options.
        tool.setStrict(true);
        tool.setJsModuleType(JSModuleType.ES2015);
        tool.setOptimizationLevel(TeaVMOptimizationLevel.ADVANCED);
        tool.setAssertionsRemoved(true);
        tool.setObfuscated(getParameters().getMinify().get());
        tool.setDebugInformationGenerated(!getParameters().getMinify().get());

        try {
            tool.generate();
        } catch (Exception e) {
            throw e instanceof RuntimeException re ? re : new GradleException("TeaVM compilation error", e);
        }

        var reporter = getProblems().getReporter();
        List<Problem> severeProblems = new ArrayList<>();
        for (var problem : tool.getProblemProvider().getProblems()) {
            var id = getProblems().getGroups().getCompilation().group("TeaVM").problemId("TeaVM error");
            var convertedProblem = reporter.create(id, p -> {
                var label = new DefaultProblemTextConsumer();
                problem.render(label);
                p.contextualLabel(label.getText());

                var location = problem.getLocation().getSourceLocation();
                p.lineInFileLocation(location.getFileName(), location.getLine());

                var callStack = new StringBuilder();
                TeaVMProblemRenderer.renderCallStack(tool.getDependencyInfo().getCallGraph(), problem.getLocation(), callStack);
                p.details(callStack.toString());
            });

            if (problem.getSeverity() == ProblemSeverity.ERROR) {
                severeProblems.add(convertedProblem);
            } else {
                reporter.report(convertedProblem);
            }
        }

        if (!severeProblems.isEmpty()) {
            throw reporter.throwing(new GradleException("Failed to compile"), severeProblems);
        }
    }

    private static URL toURL(File file) {
        try {
            return file.toURI().toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static final class ToolLog implements TeaVMToolLog {
        private static final Logger LOG = LoggerFactory.getLogger(ToolLog.class);

        @Override
        public void info(String text) {
            LOG.info("{}", text);
        }

        @Override
        public void debug(String text) {
            LOG.debug("{}", text);
        }

        @Override
        public void warning(String text) {
            LOG.warn("{}", text);
        }

        @Override
        public void error(String text) {
            LOG.error("{}", text);
        }

        @Override
        public void info(String text, Throwable e) {
            LOG.info("{}", text, e);
        }

        @Override
        public void debug(String text, Throwable e) {
            LOG.debug("{}", text, e);
        }

        @Override
        public void warning(String text, Throwable e) {
            LOG.warn("{}", text, e);
        }

        @Override
        public void error(String text, Throwable e) {
            LOG.error("{}", text, e);
        }
    }
}
