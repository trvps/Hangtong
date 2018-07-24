package com.htdz.common.utils;


import javax.servlet.http.HttpServletRequest;


public class NetUtils {
	/**
	 * 获取客服端IP真实IP地址
	 * @param request
	 * @return
	 */
	public static String getRemoteIpAddr(HttpServletRequest request) {
		/**
		 * 经过代理以后，由于在客户端和服务之间增加了中间层，因此服务器无法直接拿到客户端的IP，
		 * 服务器端应用也无法直接通过转发请求的地址返回给客户端。但是在转发请求的HTTP头信息中，
		 * 增加了X－FORWARDED－FOR信息。用以跟踪原有的客户端IP地址和原来客户端请求的服务器地
		 * 址。如果通过了多级反向代理的话，X-Forwarded-For的值并不止一个，而是一串IP值，需要
		 * 取X-Forwarded-For中第一个非unknown的有效IP字符串。
		 */
		
		// X-Forwarded-For: client1, proxy1, proxy2, proxy3
		String ip = request.getHeader("x-forwarded-for");
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		
		String iparr[] = ip.split(",");
		String ipport[] = iparr[0].split(":");
		
		return ipport[0];
	}
}
