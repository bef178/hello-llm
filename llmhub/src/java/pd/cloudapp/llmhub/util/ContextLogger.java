package pd.cloudapp.llmhub.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;
import pd.cloudapp.llmhub.entity.RequestContext;

public final class ContextLogger {

    private final Logger delegate;

    private ContextLogger(Class<?> owner) {
        this.delegate = LogManager.getLogger(owner);
    }

    public static ContextLogger of(Class<?> owner) {
        return new ContextLogger(owner);
    }

    public void fatal(RequestContext context, String message, Object... args) {
        if (!delegate.isFatalEnabled()) {
            return;
        }
        set(context);
        try {
            delegate.fatal(message, args);
        } finally {
            clear();
        }
    }

    public void error(RequestContext context, String message, Object... args) {
        if (!delegate.isErrorEnabled()) {
            return;
        }
        set(context);
        try {
            delegate.error(message, args);
        } finally {
            clear();
        }
    }

    public void warn(RequestContext context, String message, Object... args) {
        if (!delegate.isWarnEnabled()) {
            return;
        }
        set(context);
        try {
            delegate.warn(message, args);
        } finally {
            clear();
        }
    }

    public void info(RequestContext context, String message, Object... args) {
        // skip the ThreadContext put/remove entirely when the level is disabled, since that's not free
        if (!delegate.isInfoEnabled()) {
            return;
        }
        set(context);
        try {
            delegate.info(message, args);
        } finally {
            clear();
        }
    }

    public void debug(RequestContext context, String message, Object... args) {
        if (!delegate.isDebugEnabled()) {
            return;
        }
        set(context);
        try {
            delegate.debug(message, args);
        } finally {
            clear();
        }
    }

    public void trace(RequestContext context, String message, Object... args) {
        if (!delegate.isTraceEnabled()) {
            return;
        }
        set(context);
        try {
            delegate.trace(message, args);
        } finally {
            clear();
        }
    }

    private void set(RequestContext context) {
        putIfPresent("traceId", context.traceId);
        putIfPresent("requestId", context.requestId);
        putIfPresent("sessionId", context.sessionId);
        putIfPresent("deviceId", context.deviceId);
    }

    private void putIfPresent(String key, String value) {
        if (value != null) {
            ThreadContext.put(key, value);
        }
    }

    private void clear() {
        ThreadContext.remove("traceId");
        ThreadContext.remove("requestId");
        ThreadContext.remove("sessionId");
        ThreadContext.remove("deviceId");
    }
}
