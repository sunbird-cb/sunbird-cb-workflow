package org.sunbird.workflow.utils;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class will contains all the common utility methods.
 *
 * @author Manzarul
 */
public class ProjectUtil {

	static Logger logger = LogManager.getLogger(ProjectUtil.class);

	public static PropertiesCache propertiesCache;

	static {
		propertiesCache = PropertiesCache.getInstance();
	}

	public static String getConfigValue(String key) {
		if (StringUtils.isNotBlank(System.getenv(key))) {
			return System.getenv(key);
		}
		return propertiesCache.readProperty(key);
	}
}