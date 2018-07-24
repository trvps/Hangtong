package com.htdz.common;


import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.bind.annotation.RequestMapping;

import net.sf.json.JSONObject;


public class BaseController {
	private byte[] bodybtyes = null;
	
	/**
	 * 打印请求
	 * @param request
	 * @return
	 */
	public static String printRequestHeader(HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();
		
		sbAppendln(sb, "http method: "+request.getMethod());
		sbAppendln(sb, "request url: "+request.getRequestURL());
		sbAppendln(sb, "request path: "+request.getServletPath());
		
		Map<String, String[]> paramsmap = request.getParameterMap();
		if (paramsmap != null) {
			sbAppend(sb, "request params: ");
			
			Iterator<Entry<String, String[]>> iterator = paramsmap.entrySet().iterator();
			while (iterator.hasNext()) {
				Entry<String, String[]> entry = iterator.next();
				
				sbAppend(sb, entry.getKey()+"="+entry.getValue()[0]+"&");
			}
			sb.delete(sb.length()-1, sb.length());
			sbAppendln(sb, "");
		}
		
		sbAppendln(sb, "remote ip: "+request.getRemoteHost());
		sbAppendln(sb, "remote port: "+request.getRemotePort());
		
		Enumeration<String> enumheaders = request.getHeaderNames();
		while (enumheaders.hasMoreElements()) {
			String header = enumheaders.nextElement();
			sbAppendln(sb, header+": "+request.getHeader(header));
		}

		//sbAppendln(sb, getRequestBody(request));
		
		return sb.toString();
	}
	
	public static byte[] readRequestBody(HttpServletRequest request) {
		byte[] body = null;
		ServletInputStream input = null;
		try {
			int length = request.getContentLength();
			if (length > 0) {
				body = new byte[length];
				
				input = request.getInputStream();
				int readlen = 0;
				int len = 0;
				while (len != -1 && readlen < length) {
					len = input.read(body, readlen, length-readlen);
				}
				
				return body;
			}
		} catch (Exception e) {
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (Exception e) {
			}
		}
		
		return body;
	}
	
	/**
	 * 获取包体byte[]
	 * @param request
	 * @return
	 */
	public byte[] getRequestBodyBytes(HttpServletRequest request) {
		if (bodybtyes == null)
			bodybtyes = readRequestBody(request);
		
		return bodybtyes;
	}
	
	/**
	 * 获取包体字符串
	 * @param request
	 * @return
	 */
	public String getRequestBody(HttpServletRequest request) {
		byte[] data = getRequestBodyBytes(request);
		String body = "";
		try {
			if (data != null)
				body = new String(data, "UTF-8");
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}
		
		return body;
	}
	
	private static void sbAppendln(StringBuilder sb, String text) {
		sb.append(text+"\n");
	}
	
	private static void sbAppend(StringBuilder sb, String text) {
		sb.append(text);
	}
	
	@RequestMapping("/baseerror")
	public String baseError() { 
		JSONObject jsonobject = new JSONObject();
		
		try {
			jsonobject.put("code", 2);
			jsonobject.put("msg", "你访问的路径不存在！");
		} catch (Exception ee) {
			LogManager.exception(ee.getMessage(), ee);
		}
		
		return jsonobject.toString();
	}
}
