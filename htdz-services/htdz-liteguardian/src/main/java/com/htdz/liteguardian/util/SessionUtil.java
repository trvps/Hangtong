package com.htdz.liteguardian.util;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SessionUtil {
	// add by ljl key:用户名,value:session对象
	public static Map<String, HttpSession> httpSessionMap = new HashMap<String, HttpSession>();
	// add by ljl key:sessionId,value:Username
	public static Map<String, String> sessionIdToUsernameMap = new HashMap<String, String>();
	public static Map<String, String> usernameToSessionIdMap = new HashMap<String, String>();

	public static void setSession(HttpServletRequest request, String key,
			String value) {
		request.getSession().setAttribute(key, value);
	}

	public static Object getSession(HttpServletRequest request, String key) {
		return request.getSession().getAttribute(key);
	}

	public static void setUser(HttpServletRequest request, String username) {
		setSession(request, "username", username);
		// add by ljl 20160511 同一个账号不能同时在两台设备上登录，后面登录的会挤掉前面登录的
		usernameToSessionIdMap.put(username, request.getSession().getId());
		sessionIdToUsernameMap.put(request.getSession().getId(), username);
	}

	public static String getUser(HttpServletRequest request) {
		return getSession(request, "username") != null ? (String) getSession(
				request, "username") : "";
	}

	public static void removeSession(HttpServletRequest request) {
		request.getSession().removeAttribute("username");
	}
}
