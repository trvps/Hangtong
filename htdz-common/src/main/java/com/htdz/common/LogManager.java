package com.htdz.common;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LogManager {
	private static final Logger logger_log_log = LoggerFactory.getLogger("log_log");
	private static final Logger logger_exception_log = LoggerFactory.getLogger("exception_log");
	
	public static Logger getApiLog() {
		return logger_log_log;
	}
	
	public static Logger getExceptionLog() {
		return logger_exception_log;
	}
	
	public static void debug(String format) {
		logger_log_log.debug(format);
	}
	
	public static void info(String format) {
		logger_log_log.info(format);
	}
	
	public static void trace(String format) {
		logger_log_log.trace(format);
	}
	
	public static void warn(String format) {
		logger_log_log.warn(format);
	}
	
	public static void error(String format) {
		logger_log_log.error(format);
	}
	
	public static void debug(String format, Object... arguments) {
		logger_log_log.debug(format, arguments);
	}
	
	public static void info(String format, Object... arguments) {
		logger_log_log.info(format, arguments);
	}
	
	public static void trace(String format, Object... arguments) {
		logger_log_log.trace(format, arguments);
	}
	
	public static void warn(String format, Object... arguments) {
		logger_log_log.warn(format, arguments);
	}
	
	public static void error(String format, Object... arguments) {
		logger_log_log.error(format, arguments);
	}
	
	public static void exception(String format) {
		logger_exception_log.debug(format);
	}
	
	public static void exception(String format, Object... arguments) {
		logger_exception_log.debug(format, arguments);
	}
}
