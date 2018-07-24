package com.htdz.liteguardian.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Verifier {
	// private static final Log log = LogFactory.getLog(WebAPIHandler.class);

	public static final Pattern VALID_EMAIL_ADDRESS_REGEX = Pattern.compile(
			"^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$",
			Pattern.CASE_INSENSITIVE);

	public static final Pattern REGEX_MOBILE = Pattern.compile("^\\d{6,20}$",
			Pattern.CASE_INSENSITIVE);

	// 验证邮箱格式
	public static boolean validateEmail(String email) {
		Matcher matcher = VALID_EMAIL_ADDRESS_REGEX.matcher(email);
		return matcher.find();
	}

	// 正则表达式：验证手机号
	public static boolean validateMobile(String mobile) {
		Matcher matcher = REGEX_MOBILE.matcher(mobile);
		return matcher.find();
	}

	// public static void iteratorMap(Map mapdata) {
	//
	// for (Iterator i = mapdata.keySet().iterator(); i.hasNext();) {
	// Object obj = i.next();
	// log.info("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~key="
	// + obj + " value=" + mapdata.get(obj));
	//
	// }
	// }

	public static void main(String[] args) {

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 设置时间格式
		Calendar calendar = Calendar.getInstance(); // 得到日历
		String dNow = sdf.format(new Date());
		String dNowUtc = "";
		int timezone = 28800;

		Date startDateUtc = new Date();
		Date endDateUtc = new Date();
		String startDateStr = "";
		String endDateStr = "";

		try {
			dNowUtc = DateTimeUtil.local2utc(dNow, Integer.toString(timezone));
			startDateUtc = sdf.parse(dNow);
			// 后一天
			calendar.setTime(sdf.parse(dNow));
			calendar.add(Calendar.DAY_OF_YEAR, 1); // 设置为后一天 00:00:00
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MINUTE, 0);
			startDateUtc = calendar.getTime();
			startDateStr = sdf.format(startDateUtc);
			startDateStr = DateTimeUtil.local2utc(startDateStr,
					Integer.toString(timezone));
			// 后一年
			calendar.setTime(sdf.parse(dNow));
			calendar.add(Calendar.DAY_OF_YEAR, 364); // 设置为后一年 23:59:59
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.SECOND, 59);
			calendar.set(Calendar.MINUTE, 59);
			endDateUtc = calendar.getTime();
			endDateStr = sdf.format(endDateUtc);
			endDateStr = DateTimeUtil.local2utc(endDateStr,
					Integer.toString(timezone));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("dNow:" + dNow);
		System.out.println("dNowUtc:" + dNowUtc);

		System.out.println("startDateStr:" + startDateStr);
		System.out.println("endDateStr:" + endDateStr);

		String deviceSn = "603140000467";
		timezone = 7200;

	}

}
