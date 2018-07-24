package com.htdz.common.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DateTimeUtil {
	public static String getCurrentUtcDatetime() {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(
				"yyyy-MM-dd hh:mm:ss");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormatter.format(new Date());
	}

	public static String getCurrentUtcDatetime(String pattern) {
		SimpleDateFormat dateFormatter = new SimpleDateFormat(pattern);
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormatter.format(new Date());
	}

	public static String getCurrentUtcDate() {
		SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
		dateFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
		return dateFormatter.format(new Date());
	}

	/**
	 * @param DateStr1
	 *            DateStr2
	 * @return the value <code>0</code> if DateStr1 is equal to this DateStr2; a
	 *         value less than <code>0</code> if this DateStr1 is before the
	 *         DateStr2; and a value greater than <code>0</code> if this
	 *         DateStr1 is after the DateStr2.
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
	 * utc转区域时间
	 * 
	 * @param date
	 * @param timeZone
	 * @return
	 */
	public static String utc2Local(String utcTime, Integer timezone)
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

	/**
	 * 区域时间转UTC
	 * 
	 * @param localTime
	 * @param timezone
	 * @return
	 * @throws ParseException
	 */
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

	/**
	 * 将长时间格式字符串转换为时间 yyyy-MM-dd HH:mm:ss
	 * 
	 * @param strDate
	 * @return
	 */
	public static Date strToDateLong(String strDate) {
		if (strDate.length() == 10) {
			strDate += " 00:00:00";
		}
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		ParsePosition pos = new ParsePosition(0);
		Date strtodate = formatter.parse(strDate, pos);
		return strtodate;
	}

	/**
	 * 将长时间格式时间转换为字符串 yyyy-MM-dd HH:mm:ss
	 * 
	 * @param dateDate
	 * @return
	 */
	public static String dateToStrLong(java.util.Date dateDate) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateString = formatter.format(dateDate);
		return dateString;
	}

	/**
	 * 将长时间格式时间转换为字符串 yyyy-MM-dd HH:mm
	 * 
	 * @param dateDate
	 * @return
	 */
	public static String dateToStrLongMin(java.util.Date dateDate) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		String dateString = formatter.format(dateDate);
		return dateString;
	}

	/**
	 * 将短时间格式时间转换为字符串 yyyy-MM-dd
	 * 
	 * @param dateDate
	 * @param k
	 * @return
	 */
	public static String dateToStr(java.util.Date dateDate) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = formatter.format(dateDate);
		return dateString;
	}

	/**
	 * 将短时间格式字符串转换为时间 yyyy-MM-dd
	 * 
	 * @param strDate
	 * @return
	 */
	public static Date strToDate(String strDate) {
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		ParsePosition pos = new ParsePosition(0);
		Date strtodate = formatter.parse(strDate, pos);
		return strtodate;
	}

	/***
	 * 两个日期相差多少秒
	 * 
	 * @param date1
	 * @param date2
	 * @return
	 */
	public static int getTimeDelta(Date date1, Date date2) {
		long timeDelta = (date1.getTime() - date2.getTime()) / 1000;// 单位是秒
		int secondsDelta = timeDelta > 0 ? (int) timeDelta : (int) Math
				.abs(timeDelta);
		return Math.abs(secondsDelta);
	}

	/**
	 * 收集起始时间到结束时间之间所有的时间并以字符串集合方式返回
	 * 
	 * @param timeStart
	 * @param timeEnd
	 * @return
	 */
	public static List<String> collectLocalDates(String timeStart,
			String timeEnd) {
		return collectLocalDates(LocalDate.parse(timeStart),
				LocalDate.parse(timeEnd));
	}

	/**
	 * 收集起始时间到结束时间之间所有的时间并以字符串集合方式返回
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public static List<String> collectLocalDates(LocalDate start, LocalDate end) {
		// 用起始时间作为流的源头，按照每次加一天的方式创建一个无限流
		return (List<String>) Stream
				.iterate(start, localDate -> localDate.plusDays(1))
				.limit(ChronoUnit.DAYS.between(start, end) + 1)
				.map(LocalDate::toString).collect(Collectors.toList());
	}

	/**
	 * 获取精确到秒的时间戳
	 * 
	 * @param date
	 * @return
	 */
	public static String getSecondTimestamp(Date date) {
		return String.valueOf(date.getTime() / 1000);
	}

	/**
	 * 
	 * @param date
	 * @param year
	 * @return
	 */
	public static Date DateAddYear(Date date, int year) {
		Calendar c = Calendar.getInstance();
		c.setTime(date);
		c.add(Calendar.YEAR, year);
		return c.getTime();
	}

	/**
	 * 获得日期零时零分零秒
	 * 
	 * @return
	 */
	public static Date getDateByDay(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		return calendar.getTime();
	}
}
