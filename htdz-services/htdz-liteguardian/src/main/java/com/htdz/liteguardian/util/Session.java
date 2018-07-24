package com.htdz.liteguardian.util;

import java.util.HashMap;
import java.util.Map;

public class Session {
	public static Map<String, String> sessionIdToUsernameMap = new HashMap<String, String>();
	public static Map<String, String> usernameToSessionIdMap = new HashMap<String, String>();

	public static void setUser(String sessionID, String username) {
		usernameToSessionIdMap.put(username, sessionID);
		sessionIdToUsernameMap.put(sessionID, username);
	}

	public static String getUser(String sessionID) {
		return sessionIdToUsernameMap.get(sessionID) != null ? sessionIdToUsernameMap.get(sessionID) : "";
	}

	public static String getsessionID(String username) {
		return usernameToSessionIdMap.get(username) != null ? usernameToSessionIdMap.get(username) : "";
	}

}
