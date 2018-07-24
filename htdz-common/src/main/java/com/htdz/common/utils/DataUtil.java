package com.htdz.common.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.*;

public class DataUtil {
	public static int stringToInt(String val, int defval) {
		try {
			return Integer.parseInt(val);
		} catch (Exception e) {
			// LogManager.exception(e.getMessage(), e);
		}

		return defval;
	}

	public static float stringToFloat(String val, float defval) {
		try {
			return Float.parseFloat(val);
		} catch (Exception e) {
			// LogManager.exception(e.getMessage(), e);
		}

		return defval;
	}

	public static double stringToDouble(String val, double defval) {
		try {
			return Double.parseDouble(val);
		} catch (Exception e) {
			// LogManager.exception(e.getMessage(), e);
		}

		return defval;
	}

	public static String bytesToString(byte[] bytes) {
		try {
			return new String(bytes, "utf-8");
		} catch (Exception e) {
			// e.printStackTrace();
		}
		return "";
	}

	public static String bytesToHexString(byte[] bytes) {
		return bytesToHexString(bytes, null, null, null, false);
	}

	public static String bytesToHexString(byte[] bytes, String headStr,
			String tailStr, String splitStr, boolean include0x) {
		if (bytes == null || bytes.length == 0)
			return "[]";

		// headStr0x00splitStr0x01tailStr 格式
		StringBuilder sb = new StringBuilder();
		if (headStr != null)
			sb.append(headStr);
		if (bytes != null && bytes.length > 0) {
			sb.append(byteToHexString(bytes[0], include0x));
			for (int i = 1; i < bytes.length; i++) {
				if (splitStr != null)
					sb.append(splitStr);
				sb.append(byteToHexString(bytes[i], include0x));
			}
		}

		if (tailStr != null)
			sb.append(tailStr);

		return sb.toString();
	}

	public static String byteToHexString(byte b) {
		return byteToHexString(b, false);
	}

	public static String byteToHexString(byte b, boolean include0x) {
		String s = Integer.toHexString(b);

		if (b < 16)
			s = "0" + s;

		if (include0x)
			s = "0x" + s;

		return s;
	}

	public static byte[] hexStringToBytes(String hexStr, String headStr,
			String tailStr, String splitStr) {
		if (headStr != null && hexStr.startsWith(headStr)) {
			hexStr = hexStr.substring(headStr.length());
		}

		if (tailStr != null && hexStr.endsWith(tailStr)) {
			hexStr = hexStr.substring(0, hexStr.length() - tailStr.length());
		}

		if (splitStr != null) {
			hexStr = hexStr.replace(splitStr, "");
		}

		hexStr = hexStr.replace("0x", "");
		hexStr = hexStr.replace("0X", "");

		try {
			byte[] b = new byte[hexStr.length() / 2];
			for (int i = 0; i < b.length; i++) {
				int start = i * 2;
				b[i] = (byte) Integer.parseInt(
						hexStr.substring(start, start + 2), 16);
			}

			return b;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public static byte hexStringToByte(String hexStr) {
		try {
			return (byte) Integer.parseInt(hexStr.substring(0, 2), 16);
		} catch (Exception e) {
		}

		return 0;
	}

	public static String getStringFromMap(Map<String, String[]> params,
			String key) {
		String[] strings = params.get(key);
		if (null != strings && strings.length > 0) {
			return strings[0];
		}
		return null;
	}

	public static int stringToHash(String key) {
		// 数组大小一般取质数
		int arraySize = 11113;
		int hashCode = 0;
		for (int i = 0; i < key.length(); i++) {
			int letterValue = key.charAt(i) - 96;
			// 防止编码溢出，对每步结果都进行取模运算
			hashCode = ((hashCode << 5) + letterValue) % arraySize;
		}
		return hashCode;
	}

	public static int indexOfByteArray(byte[] bytes, int start, byte b) {
		assert (start >= 0 && start < bytes.length);
		for (int i = start; i < bytes.length; i++) {
			if (bytes[i] == b)
				return i;
		}

		return -1;
	}

	public static byte[] bytesFromBytes(byte[] bytes, int start, int len) {
		byte[] b = new byte[len];
		System.arraycopy(bytes, start, b, 0, len);
		return b;
	}

	public static byte[] bytesFromBytes(byte[] bytes, int start, byte startTAG,
			byte endTAG, boolean includeTAG) {
		int startIndex = start;
		int endIndex = -1;

		// 查找数据起始位置
		if (startTAG == 0) {
			startIndex = start;
		} else {
			for (; startIndex < bytes.length; startIndex++) {
				if (bytes[startIndex] == startTAG) {
					if (!includeTAG)
						startIndex++;
					break;
				}
			}
		}

		if (startIndex >= bytes.length)
			return null;

		// 查找数据结束位置
		if (endTAG == 0) {
			endIndex = bytes.length - 1;
		} else {
			for (endIndex = startIndex; endIndex < bytes.length; endIndex++) {
				if (bytes[endIndex] == endTAG) {
					if (!includeTAG)
						endIndex--;
					break;
				}
			}
		}

		if (endIndex >= bytes.length)
			return null;

		int len = endIndex - startIndex + 1;
		return bytesFromBytes(bytes, startIndex, len);
	}

	public static double doubleNDots(double value, int ndot) {
		BigDecimal bigDecimal = new BigDecimal(value);
		return bigDecimal.setScale(ndot, BigDecimal.ROUND_HALF_UP)
				.doubleValue();
	}

	public static float floatNDots(float value, int ndot) {
		BigDecimal bigDecimal = new BigDecimal(value);
		return bigDecimal.setScale(ndot, BigDecimal.ROUND_HALF_UP).floatValue();
	}

	public static void main(String[] args) throws Exception {
		System.out.println("--------------- DataUtil Main ---------------");

		String str = "15413,1110,1108,12,0,-93,255;";
		String hexstr = DataUtil.bytesToHexString(str.getBytes("utf-8"), null,
				null, null, false);
		System.out.println(hexstr.toUpperCase());
	}
}
