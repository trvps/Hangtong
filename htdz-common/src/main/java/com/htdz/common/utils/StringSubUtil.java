package com.htdz.common.utils;

public class StringSubUtil {

	/**
	 * 获取URL中的相对路径
	 * 
	 * @param str
	 * @return
	 */
	public static String getRelativeURL(String str) {
		String s[] = str.split("//");
		int i = s[1].indexOf("/");
		return s[1].substring(i);
	}

	/**
	 * 长日期格式转为短日期格式
	 * 
	 * @param timeStart
	 * @return
	 */
	public static String StrLongToStr(String timeStart) {
		String param[] = timeStart.split(" ");
		return param[0];
	}
}
