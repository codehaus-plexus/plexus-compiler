package org.codehaus.plexus.compiler;

import org.slf4j.Logger;

class PlexusLoggerWrapper implements org.codehaus.plexus.logging.Logger {

    private final Logger log;

    PlexusLoggerWrapper(Logger log) {
        this.log = log;
    }

    @Override
    public void debug(String message) {
        log.debug(message);
    }

    @Override
    public void debug(String message, Throwable throwable) {
        log.debug(message, throwable);
    }

    @Override
    public boolean isDebugEnabled() {
        return log.isDebugEnabled();
    }

    @Override
    public void info(String message) {
        log.info(message);
    }

    @Override
    public void info(String message, Throwable throwable) {
        log.info(message, throwable);
    }

    @Override
    public boolean isInfoEnabled() {
        return log.isInfoEnabled();
    }

    @Override
    public void warn(String message) {
        log.warn(message);
    }

    @Override
    public void warn(String message, Throwable throwable) {
        log.warn(message, throwable);
    }

    @Override
    public boolean isWarnEnabled() {
        return log.isWarnEnabled();
    }

    @Override
    public void error(String message) {
        log.error(message);
    }

    @Override
    public void error(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    @Override
    public boolean isErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public void fatalError(String message) {
        log.error(message);
    }

    @Override
    public void fatalError(String message, Throwable throwable) {
        log.error(message, throwable);
    }

    @Override
    public boolean isFatalErrorEnabled() {
        return log.isErrorEnabled();
    }

    @Override
    public int getThreshold() {
        return 0;
    }

    @Override
    public void setThreshold(int threshold) {
        // not implemented
    }

    @Override
    public org.codehaus.plexus.logging.Logger getChildLogger(String name) {
        return null;
    }

    @Override
    public String getName() {
        return log.getName();
    }
}
