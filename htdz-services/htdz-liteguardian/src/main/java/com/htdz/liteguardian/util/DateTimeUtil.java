package com.htdz.liteguardian.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DateTimeUtil {
	public static String getCurrentUtcDatetime() {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy-MM-dd hh:mm:ss");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormatter.format(new Date());
	}

	/**
	 * @param DateStr1
	 *            DateStr2
	 * @return 1:DateStr1 在 DateStr2的后
	 */
	public static int comPareTime(String DateStr1, String DateStr2) {
		if (DateStr1.length() > 5) {
			DateStr1 = DateStr1.substring(11, 16);
		}
		if (DateStr2.length() > 5) {
			DateStr2 = DateStr2.substring(11, 16);
		}

		SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");
		Date dateTime1 = null;
		Date dateTime2 = null;
		try {
			dateTime1 = dateFormat.parse(DateStr1);
			dateTime2 = dateFormat.parse(DateStr2);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dateTime1.compareTo(dateTime2);
	}

	/**
	 * utc转北京时间
	 * 
	 * @param date
	 * @param timeZone
	 * @return
	 */
	public static String getUtc2Local(String utcTime, Integer timezone)
			throws ParseException {
		if (utcTime.isEmpty()) {
			return "";
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = sdf.parse(utcTime);
		Long d = date.getTime() + (timezone * 1000);

		date = new Date(d);

		return sdf.format(date);

	}

	public static String local2utc(String localTime, String timezone)
			throws ParseException {
		if (localTime.isEmpty()) {
			return "";
		}

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		Date date = sdf.parse(localTime);
		GregorianCalendar gca = new GregorianCalendar();
		gca.setTime(date);
		gca.add(java.util.Calendar.MILLISECOND,
				-Integer.parseInt(timezone) * 1000);
		int year = gca.get(Calendar.YEAR);
		int month = gca.get(Calendar.MONTH) + 1;
		int day = gca.get(Calendar.DAY_OF_MONTH);
		int hour = gca.get(Calendar.HOUR_OF_DAY);
		int minute = gca.get(Calendar.MINUTE);
		int second = gca.get(Calendar.SECOND);

		StringBuffer sb = new StringBuffer();
		sb.append(year)
				.append("-")
				.append(month < 10 ? ("0" + Integer.toString(month)) : month)
				.append("-")
				.append(day < 10 ? ("0" + Integer.toString(day)) : day)
				.append(" ")
				.append(hour < 10 ? ("0" + Integer.toString(hour)) : hour)
				.append(":")
				.append(minute < 10 ? ("0" + Integer.toString(minute)) : minute)
				.append(":")
				.append(second < 10 ? ("0" + Integer.toString(second)) : second);
		return sb.toString();
	}

	/**
	 * 构建完整时间
	 * 
	 * @param hhmmTime
	 * @return
	 */
	public static String getfulldatetime(String hhmmTime) {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String YYMMddhhmmssStr = sdf.format(date);
		String YYMMddhhmmss = YYMMddhhmmssStr.substring(0, 10) + " " + hhmmTime
				+ ":" + YYMMddhhmmssStr.substring(17, 19);
		return YYMMddhhmmss;

	}

	/**
	 * 格式化日期,返回短时间格式 yyyy-MM-dd HH:mm:ss
	 * 
	 * @param date
	 * @return
	 */
	public static String getDateString(Object date) {
		if (date instanceof Date) {
			SimpleDateFormat format = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			return format.format((Date) date);
		} else {
			return "";
		}
	}

	/**
	 * yyyy-MM-dd HH:mm:ss
	 * 
	 * @param DateStr1
	 * @param DateStr2
	 * @return
	 */
	public static int compareDateTime(String DateStr1, String DateStr2) {

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date dateTime1 = null;
		Date dateTime2 = null;
		try {
			dateTime1 = df.parse(DateStr1);
			dateTime2 = df.parse(DateStr2);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return dateTime1.compareTo(dateTime2);
	}

}
