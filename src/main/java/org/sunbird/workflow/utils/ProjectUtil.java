package org.sunbird.workflow.utils;

import org.apache.commons.lang.StringUtils;
import org.sunbird.workflow.core.WFLogger;

public class ProjectUtil {

	public static WFLogger logger = new WFLogger(ProjectUtil.class.getName());

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