// SPDX-FileCopyrightText: 2023 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package org.slf4j;

import cc.tweaked.web.js.Console;
import org.slf4j.event.Level;
import org.slf4j.helpers.AbstractLogger;

import java.io.Serial;
import java.util.Arrays;

/**
 * A replacement for SLF4J's {@link LoggerFactory}, which skips service loading, returning a logger which prints to the
 * JS console.
 */
public final class TLoggerFactory {
    private static final Logger INSTANCE = new LoggerImpl();

    private TLoggerFactory() {
    }

    public static Logger getLogger(Class<?> klass) {
        return INSTANCE;
    }

    private static final class LoggerImpl extends AbstractLogger {
        @Serial
        private static final long serialVersionUID = 3442920913507872371L;

        @Override
        protected String getFullyQualifiedCallerName() {
            return "logger";
        }

        @Override
        protected void handleNormalizedLoggingCall(Level level, Marker marker, String msg, Object[] arguments, Throwable throwable) {
            if (arguments != null) msg += " " + Arrays.toString(arguments);
            switch (level) {
                case TRACE, DEBUG, INFO -> Console.log(msg);
                case WARN -> Console.warn(msg);
                case ERROR -> Console.error(msg);
            }

            if (throwable != null) throwable.printStackTrace();
        }

        @Override
        public boolean isTraceEnabled() {
            return true;
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            return true;
        }

        @Override
        public boolean isDebugEnabled() {
            return true;
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            return true;
        }

        @Override
        public boolean isInfoEnabled() {
            return true;
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            return true;
        }

        @Override
        public boolean isWarnEnabled() {
            return true;
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            return true;
        }

        @Override
        public boolean isErrorEnabled() {
            return true;
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            return true;
        }
    }
}
