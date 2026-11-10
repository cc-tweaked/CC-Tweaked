// SPDX-FileCopyrightText: 2026 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package dan200.computercraft.util;

import cc.tweaked.CCTweaked;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.helpers.BasicMDCAdapter;
import org.slf4j.helpers.BasicMarkerFactory;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

import java.util.logging.Level;
import java.util.logging.Logger;

public class SLF4JProvider implements SLF4JServiceProvider {
    private final ILoggerFactory loggerFactory = n -> {
        var logger = Logger.getLogger(n);
        logger.setParent(CCTweaked.LOG);
        return new LogAdapter(logger);
    };
    private final IMarkerFactory markerFactory = new BasicMarkerFactory();
    private final MDCAdapter mdc = new BasicMDCAdapter();

    @Override
    public void initialize() {
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdc;
    }

    @Override
    public String getRequestedApiVersion() {
        return "2.0.20";
    }

    private static final class LogAdapter extends LegacyAbstractLogger {
        private final java.util.logging.Logger logger;

        private LogAdapter(java.util.logging.Logger logger) {
            this.logger = logger;
        }

        private void log(Level level, String message) {
            logger.log(level, message);
        }

        private void log(Level level, String format, Object arg) {
            var tuple = MessageFormatter.format(format, arg);
            logger.log(level, tuple.getMessage(), tuple.getThrowable());
        }

        private void log(Level level, String format, Object arg1, Object arg2) {
            var tuple = MessageFormatter.format(format, arg1, arg2);
            logger.log(level, tuple.getMessage(), tuple.getThrowable());
        }

        private void log(Level level, String format, Object[] args) {
            var tuple = MessageFormatter.format(format, args);
            logger.log(level, tuple.getMessage(), tuple.getThrowable());
        }

        @Override
        protected String getFullyQualifiedCallerName() {
            return logger.getName();
        }

        @Override
        protected void handleNormalizedLoggingCall(org.slf4j.event.Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
            var message = MessageFormatter.format(messagePattern, arguments, throwable);
            logger.log(switch (level) {
                case ERROR -> Level.SEVERE;
                case WARN -> Level.WARNING;
                case INFO -> Level.INFO;
                case DEBUG -> Level.FINE;
                case TRACE -> Level.FINER;
            }, message.getMessage(), message.getThrowable());
        }

        @Override
        public boolean isTraceEnabled() {
            return logger.isLoggable(Level.FINER);
        }

        @Override
        public boolean isDebugEnabled() {
            return logger.isLoggable(Level.FINE);
        }

        @Override
        public boolean isInfoEnabled() {
            return logger.isLoggable(Level.INFO);
        }

        @Override
        public boolean isWarnEnabled() {
            return logger.isLoggable(Level.WARNING);
        }

        @Override
        public boolean isErrorEnabled() {
            return logger.isLoggable(Level.SEVERE);
        }
    }
}
