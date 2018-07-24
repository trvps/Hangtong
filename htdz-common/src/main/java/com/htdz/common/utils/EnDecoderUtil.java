package com.htdz.common.utils;


import java.security.MessageDigest;


public class EnDecoderUtil {
	public static String MD5Encode(String sourceString) {
		return MD5Encode(sourceString, "");
	}
	
	public static String MD5Encode(String sourceString, String password) {
		return MessageDigest_Encode("MD5", sourceString, password);
	}
	
	public static String SHA1Encode(String sourceString, String password) {
		return MessageDigest_Encode("SHA1", sourceString, password);
	}
	
	protected static String MessageDigest_Encode(String algorithm, String sourceString, String password) {
		String resultString = null;
		try {
			resultString = new String(sourceString);
			MessageDigest md = MessageDigest.getInstance(algorithm);
			md.update(password.getBytes("UTF-8"));
			resultString = byte2hexString(md.digest(resultString.getBytes("UTF-8")));
		} catch (Exception ex) {
		}
		return resultString;
	}
	
	public static final String byte2hexString(byte[] bytes) {
		StringBuffer bf = new StringBuffer(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			if ((bytes[i] & 0xff) < 0x10) {
				bf.append("0");
			}
			bf.append(Long.toString(bytes[i] & 0xff, 16));
		}
		return bf.toString();
	}
	
	public static void main(String args[]) {
		System.out.println(MD5Encode("1", ""));
		System.out.println(SHA1Encode("1", ""));
	}
}


