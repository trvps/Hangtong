package com.htdz.liteguardian.util;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

public class Language {

	public static void setLanguage(HttpServletRequest request) {
		String language = "";
		if (request != null && request.getHeader("Accept-Language") != null
				&& !request.getHeader("Accept-Language").isEmpty()) {
			language = request.getHeader("Accept-Language").toLowerCase();
			if (language.length() > 5) {
				language = language.substring(0, 5);
			}

			String supportLang = PropertyUtil.getPropertyValue("web", "support_language").toLowerCase();
			if (supportLang.indexOf(language) == -1) {
				language = PropertyUtil.getPropertyValue("web", "default_language");
			}
		} else {
			language = PropertyUtil.getPropertyValue("web", "default_language");
		}

		request.getSession().setAttribute("language", language);
	}

	public static String getLanguage(Map<String, String[]> params) {
		String language = PropertyUtil.getPropertyValue("web", "default_language");

		return language;
	}
}
