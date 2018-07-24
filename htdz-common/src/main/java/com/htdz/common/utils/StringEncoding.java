package com.htdz.common.utils;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import sun.misc.BASE64Encoder;

public class StringEncoding {
	/**
	 * 将字符串转成ASCII
	 * 
	 * @param value
	 * @return
	 */
	public static String stringToAscii(String value) {
		StringBuffer sbu = new StringBuffer();
		char[] chars = value.toCharArray();
		for (int i = 0; i < chars.length; i++) {
			if (i != chars.length - 1) {
				sbu.append((int) chars[i]).append(",");
			} else {
				sbu.append((int) chars[i]);
			}
		}

		return sbu.toString();
	}

	/**
	 * 将ASCII转成字符串
	 * 
	 * @param value
	 * @return
	 */
	public static String asciiToString(String value) {
		StringBuffer sbu = new StringBuffer();
		String[] chars = value.split(",");
		for (int i = 0; i < chars.length; i++) {
			sbu.append((char) Integer.parseInt(chars[i]));
		}

		return sbu.toString();
	}

	/**
	 * 字符串转换unicode
	 * 
	 * @param string
	 * @return
	 */
	public static String stringToUnicode2(String string) {
		StringBuffer unicode = new StringBuffer();
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i); // 取出每一个字符
			String tmp = "000" + Integer.toHexString(c);
			tmp = tmp.substring(tmp.length() - 4);
			unicode.append(tmp);// 转换为unicode
		}
		return unicode.toString();
	}

	/**
	 * 字符串转换unicode
	 * 
	 * @param string
	 * @return
	 */
	public static String stringToUnicode(String string) {
		StringBuffer unicode = new StringBuffer();
		for (int i = 0; i < string.length(); i++) {
			char c = string.charAt(i); // 取出每一个字符
			unicode.append("\\u" + Integer.toHexString(c));// 转换为unicode
		}
		return unicode.toString();
	}

	/**
	 * unicode 转字符串
	 * 
	 * @param unicode
	 * @return
	 */
	public static String unicodeToString2(String unicode) {
		StringBuffer string = new StringBuffer();

		for (int i = 0; i < unicode.length() - 1; i++) {
			String curCode = unicode.substring(i, i+4);
			char c = (char) Integer.parseInt(curCode, 16);
			string.append(c);// 追加成string
			i = i+3;
		}

		return string.toString();
	}

	/**
	 * unicode 转字符串
	 * 
	 * @param unicode
	 * @return
	 */
	public static String unicodeToString(String unicode) {
		StringBuffer string = new StringBuffer();
		String[] hex = unicode.split("\\\\u");
		for (int i = 1; i < hex.length; i++) {
			int data = Integer.parseInt(hex[i], 16);// 转换出每一个代码点
			string.append((char) data);// 追加成string
		}
		return string.toString();
	}

	/**
	 * byte[] to hex string
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytesToHexString(byte[] bytes) {
		StringBuilder buf = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			// 使用String的format方法进行转换
			buf.append(String.format("%02x", new Integer(b & 0xff)));
		}

		return buf.toString();
	}

	/**
	 * 将16进制字符串转换为byte[]
	 * 
	 * @param str
	 * @return
	 */
	public static byte[] hexStringtoBytes(String str) {
		if (str == null || str.trim().equals("")) {
			return new byte[0];
		}

		byte[] bytes = new byte[str.length() / 2];
		for (int i = 0; i < str.length() / 2; i++) {
			String subStr = str.substring(i * 2, i * 2 + 2);
			bytes[i] = (byte) Integer.parseInt(subStr, 16);
		}

		return bytes;
	}

	/**
	 * 和风天气签名生成算法-JAVA版本
	 * 
	 * @param HashMap
	 *            params 请求参数集，所有参数必须已转换为字符串类型
	 * @param String
	 *            secret 签名密钥（用户的认证key）
	 * @return 签名
	 * @throws IOException
	 */
	public static String getSignature(HashMap params, String secret)
			throws IOException {
		// 先将参数以其参数名的字典序升序进行排序
		Map sortedParams = new TreeMap(params);
		Set<Map.Entry> entrys = sortedParams.entrySet();
		// 遍历排序后的字典，将所有参数按"key=value"格式拼接在一起

		StringBuilder baseString = new StringBuilder();
		for (Map.Entry param : entrys) {
			// sign参数 和 空值参数 不加入算法
			if (param.getValue() != null && !"".equals(param.getKey())
					&& !"sign".equals(param.getKey())
					&& !"key".equals(param.getKey())
					&& !"".equals(param.getValue())) {
				baseString.append(param.getKey()).append("=")
						.append(param.getValue()).append("&");
			}
		}
		if (baseString.length() > 0) {
			baseString.deleteCharAt(baseString.length() - 1).append(secret);
		}

		// 使用MD5对待签名串求签
		try {
			MessageDigest md5 = MessageDigest.getInstance("MD5");
			byte[] bytes = md5.digest(baseString.toString().getBytes("UTF-8"));
			return new BASE64Encoder().encode(bytes);
		} catch (GeneralSecurityException ex) {
			throw new IOException(ex);
		}
	}
}
