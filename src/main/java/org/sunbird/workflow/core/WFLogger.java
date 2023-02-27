package org.sunbird.workflow.core;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

public class WFLogger {

	private Logger logger;

	public WFLogger(String className) {
		this.logger = LogManager.getLogger(className);
	}

	public void debug(String message) {
		logger.log(Level.DEBUG, message);
	}

	public void info(String message) {
		logger.log(Level.INFO, message);
	}

	public void warn(String message) {
		logger.log(Level.WARN, message);
	}

	public void error(Exception exception) throws JsonProcessingException {
		ObjectMapper ow = new ObjectMapper();
		// log the exception
		try {
			Map<String, Object> message = new HashMap<>();
			message.put("event", exception.getClass());
			message.put("message", exception.getMessage());
			message.put("trace",  ow.writeValueAsString(exception));
			logger.log(Level.ERROR, ow.writeValueAsString(message));
		} catch (Exception e) {
			logger.log(Level.ERROR,
					"{\"event\":\"" + exception.getClass() + "\", \"message\":\"" + exception.getMessage()
							+ "\", \"trace\":\"" + ow.writeValueAsString(exception) + "\"}");
		}
	}

	public void fatal(Exception exception) throws JsonProcessingException {
		ObjectMapper ow = new ObjectMapper();
		// log the exception
		try {
			Map<String, Object> message = new HashMap<>();
			message.put("event", exception.getClass());
			message.put("message", exception.getMessage());
			message.put("trace", ow.writeValueAsString(exception));
			logger.log(Level.FATAL, ow.writeValueAsString(message));
		} catch (Exception e) {
			logger.log(Level.FATAL,
					"{\"event\":\"" + exception.getClass() + "\", \"message\":\"" + exception.getMessage()
							+ "\", \"trace\":\"" + ow.writeValueAsString(exception) + "\"}");
		}
	}

	public void trace(String message) {
		logger.log(Level.TRACE, message);
	}

	public void performance(String message) {
		Level performance = Level.forName("PERF", 350);
		logger.log(performance, message);
	}
}