package com.yieldnull.httpd;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Logging with thread name
 * <p/>
 * Created by finalize on 7/29/16.
 */
class Log {

    private Logger logger;

    Log(Logger logger) {
        this.logger = logger;
    }

    static Log of(Class clazz) {
        return new Log(Logger.getLogger(clazz.getSimpleName()));
    }

    void i(String message) {
        logger.log(Level.INFO, thread() + message);
    }

    void d(String message) {
        logger.log(Level.CONFIG, thread() + message);
    }

    void w(String message) {
        logger.log(Level.WARNING, thread() + message);
    }

    void w(String message, Throwable throwable) {
        logger.log(Level.WARNING, thread() + message, throwable);
    }

    void e(String message) {
        logger.log(Level.SEVERE, thread() + message);
    }

    void e(String message, Throwable throwable) {
        logger.log(Level.SEVERE, thread() + message, throwable);
    }

    private String thread() {
        return "[" + Thread.currentThread().getName() + "] ";
    }

}
