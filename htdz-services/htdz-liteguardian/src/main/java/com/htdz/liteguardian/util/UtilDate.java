package com.htdz.liteguardian.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

/* *
 *类名：UtilDate
 *功能：自定义订单类
 *详细：工具类，可以用作获取系统日期、订单编号等
 *版本：3.3
 *日期：2012-08-17
 *说明：
 *以下代码只是为了方便商户测试而提供的样例代码，商户可以根据自己网站的需要，按照技术文档编写,并非一定要使用该代码。
 *该代码仅供学习和研究支付宝接口使用，只是提供一个参考。
 */
public class UtilDate {

	/** 年月日时分秒(无下划线) yyyyMMddHHmmss */
	public static final String dtLong = "yyyyMMddHHmmss";

	/** 完整时间 yyyy-MM-dd HH:mm:ss */
	public static final String simple = "yyyy-MM-dd HH:mm:ss";

	/** 年月日(无下划线) yyyyMMdd */
	public static final String dtShort = "yyyyMMdd";

	/** 年月日(无下划线) yyyyMMdd HH:mm */
	public static final String nyrsf = "yyyy-MM-dd HH:mm";

	/** 年月(无下划线) yyyyMM */
	public static final String dtSmall = "yyyy.MM";

	/**
	 * 返回系统当前时间(精确到毫秒),作为一个唯一的订单编号
	 * 
	 * @return 以yyyyMMddHHmmss为格式的当前系统时间
	 */
	public static String getOrderNum() {
		Date date = new Date();
		DateFormat df = new SimpleDateFormat(dtLong);
		return df.format(date);
	}

	/**
	 * 获取系统当前日期(精确到毫秒)，格式：yyyy-MM-dd HH:mm:ss
	 * 
	 * @return
	 */
	public static String getDateFormatter() {
		Date date = new Date();
		DateFormat df = new SimpleDateFormat(simple);
		return df.format(date);
	}

	/**
	 * 获取系统当期年月日(精确到天)，格式：yyyyMMdd
	 * 
	 * @return
	 */
	public static String getDate() {
		Date date = new Date();
		DateFormat df = new SimpleDateFormat(dtShort);
		return df.format(date);
	}

	/**
	 * 产生随机的三位数
	 * 
	 * @return
	 */
	public static String getThree() {
		Random rad = new Random();
		return rad.nextInt(1000) + "";
	}

	/**
	 * 微信支付订单号
	 * 
	 * @return
	 */
	/*
	 * public static String getNonCeStr() { Random rad = new Random(); return
	 * com
	 * .castel.family.weixin.MD5.getMessageDigest(String.valueOf(rad.nextInt(10000
	 * )).getBytes()); }
	 */

	/**
	 * 将指定格式的字符串 转换成 date
	 * 
	 * @param date
	 * @param srcFormat
	 * @return
	 */
	public static Date getDate(String date, String srcFormat) {
		if (date != null && !date.isEmpty() && null != srcFormat
				&& !srcFormat.isEmpty()) {
			SimpleDateFormat format = new SimpleDateFormat(srcFormat);
			try {
				return format.parse(date);
			} catch (ParseException e) {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * 根据时区获取UTC时间串
	 * 
	 * @param date
	 * @param timezone
	 *            对应javascript对象Date的timezoneOffset
	 * @return
	 */
	public static String getUTCDateString(Date date, int timezone) {
		if (date != null) {
			Calendar cale = Calendar.getInstance();
			cale.setTime(new Date(date.getTime() - timezone * 1000));
			// cale.add(cale.MINUTE, timezone);
			return getDateString(cale.getTime());
		} else {
			return "";
		}
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
	 * 格式化日期,返回短时间格式 yyyy-MM
	 * 
	 * @param date
	 * @return
	 */
	public static String getDateSmall(Object date) {
		if (date instanceof Date) {
			SimpleDateFormat format = new SimpleDateFormat(dtSmall);
			return format.format((Date) date);
		} else {
			return "";
		}
	}

	/**
	 * 获取时间差 yyyy-MM-dd HH:mm:ss
	 * 
	 * @param date1
	 *            ：开始时间 date2：结束时间
	 * @return
	 */
	public static String getDateDiff(String date1, String date2) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date startDate = null;
		Date endDate = null;
		try {
			startDate = df.parse(date1);
			endDate = df.parse(date2);
		} catch (ParseException e) {
			return "00:00:00";
		}
		long l = endDate.getTime() - startDate.getTime();
		long day = l / (24 * 60 * 60 * 1000);
		long hour = (l / (60 * 60 * 1000) - day * 24);
		long min = ((l / (60 * 1000)) - day * 24 * 60 - hour * 60);
		long s = (l / 1000 - day * 24 * 60 * 60 - hour * 60 * 60 - min * 60);
		if (day > 0) {
			hour = day * 24 + hour;
		}
		String hourTime = hour > 9 ? String.valueOf(hour) : "0" + hour;
		String minTime = min > 9 ? String.valueOf(min) : "0" + min;
		String sTime = s > 9 ? String.valueOf(s) : "0" + s;
		return hourTime + ":" + minTime + ":" + sTime;
	}

	/*
	 * 把多少秒 换算成时分秒 **：**：**
	 */
	public static String getHourMinSec(int second) {
		int h = 0;
		int d = 0;
		int s = 0;
		String hstr = "";
		String dstr = "";
		String sstr = "";
		int temp = second % 3600;
		if (second > 3600) {
			h = second / 3600;
			if (temp != 0) {
				if (temp > 60) {
					d = temp / 60;
					if (temp % 60 != 0) {
						s = temp % 60;
					}
				} else {
					s = temp;
				}
			}
		} else {
			d = second / 60;
			if (second % 60 != 0) {
				s = second % 60;
			}
		}
		if (h < 9) {
			hstr = "0" + String.valueOf(h);
		} else {
			hstr = String.valueOf(h);
		}
		if (d < 9) {
			dstr = "0" + String.valueOf(d);
		} else {
			dstr = String.valueOf(d);
		}
		if (s < 9) {
			sstr = "0" + String.valueOf(s);
		} else {
			sstr = String.valueOf(s);
		}
		return hstr + ":" + dstr + ":" + sstr;
	}

	public static void main(String[] args) {
		String time = getDateDiff("2017-11-15 10:25:00", "2017-11-15 10:40:00");
		System.out.println(time.compareTo("00:05:00"));
		System.out.println("time:" + time);
	}
}
