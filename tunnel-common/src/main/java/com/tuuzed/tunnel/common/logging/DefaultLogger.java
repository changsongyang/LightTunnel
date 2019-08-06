package com.tuuzed.tunnel.common.logging;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

import static com.tuuzed.tunnel.common.logging.LoggerFactory.logAdapters;
import static com.tuuzed.tunnel.common.logging.Utils.*;


public class DefaultLogger extends AbstractLogger {
    @NotNull
    private String name;
    private String shortName;
    private final Date date = new Date();

    public DefaultLogger(@NotNull String name) {
        this.name = name;
        this.shortName = getShortClassName(name);
    }

    @Override
    void log(int level, @NotNull String format, Object... args) {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        if (traces.length >= 3) {
            log(level, formatPlaceholderMsg(format, args), null, traces[3]);
        } else {
            log(level, formatPlaceholderMsg(format, args), null, null);
        }
    }

    @Override
    void log(int level, @NotNull String msg, @Nullable Throwable cause) {
        StackTraceElement[] traces = Thread.currentThread().getStackTrace();
        if (traces.length >= 3) {
            log(level, msg, cause, traces[3]);
        } else {
            log(level, msg, cause, null);
        }
    }

    private synchronized void log(
        int level,
        @NotNull String msg,
        @Nullable Throwable cause,
        @Nullable StackTraceElement trace
    ) {
        String message = createMessage(level, msg, trace);
        for (LogAdapter logAdapter : logAdapters.values()) {
            if (logAdapter.isLoggable(level)) {
                logAdapter.log(level, message, cause);
            }
        }
    }

    private synchronized String createMessage(
        int level,
        @NotNull String msg,
        @Nullable StackTraceElement trace
    ) {
        date.setTime(System.currentTimeMillis());
        StringBuilder log = new StringBuilder();
        log
            .append(formatDate(date)).append(" [")
            .append(getLevelName(level)).append("] ")
            .append(Thread.currentThread().getName()).append(" ");

        if (trace != null) {
            log
                .append(getShortClassName(trace.getClassName())).append("#")
                .append(trace.getMethodName()).append(": ");
        } else {
            log
                .append(shortName).append(": ");
        }
        log.append(msg);
        return log.toString();
    }


}
