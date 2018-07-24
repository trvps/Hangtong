package com.htdz.common.utils;

import java.io.UnsupportedEncodingException;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;

public class PropertyUtil {
	/**
	 * 通过文件名和key获取资源文件的value
	 */
	public static String getPropertyValue(String docName, String key) {
		try {
			ResourceBundle resource = ResourceBundle.getBundle(docName);
			if (resource == null) {
				return "";
			}
			
			return new String(resource.getString(key).getBytes("ISO-8859-1"), "GBK");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		return "";
	}
	
	/**
	 * 通过语言、文件名，key获取资源文件的value
	 * @param 资源文件名
	 * @param 资源文件的key
	 * @return 资源文件的值
	 */
	public static String getReturnWhat(String key, String language) {
		ResourceBundle resource = ResourceBundle.getBundle("language", new Locale(language));
		try {
			return new String(resource.getString(key).getBytes("ISO-8859-1"),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return "";
	}
	
	/**
	 * 根据timezone.properties表的KEY得到时区
	 * @param timezoneID
	 * @return
	 */
	public static int getTimezoneBylocalID(String timezoneID) {
		ResourceBundle resource = ResourceBundle.getBundle("timeZone");
		String value = resource.getString("timeZone" + timezoneID);
		int timezone = Integer.parseInt(value);
		if (timezone > 12 || timezone < -12) {
			return 0;
		}
		//返回以秒为单位的时区
		return timezone * 3600;
	}	
	
	/**
	 * true 不是空白字符串
	 * false 是空白字符串
	 * @param str
	 * @return
	 */
	public static boolean isNotBlank(String str) {
		return str != null && !str.trim().equals("");
	}	
	
	public static String getValue(String lang,String key){
	    ResourceBundle bundle = ResourceBundle.getBundle("language",new Locale(lang));//根据指定的国家/语言环境加载对应的资源文件
	    try {
			return new String(bundle.getString(key).getBytes("ISO-8859-1"),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	
	/**
	 * 获取timeZone.properties文件的内容根据key
	 * @param key
	 * @return
	 */
	public static String getTimeZoneById(String key){
	    ResourceBundle bundle = ResourceBundle.getBundle("timeZone");
	    try {
			return new String(bundle.getString("timeZone"+key).getBytes("ISO-8859-1"),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	public static Date getUTCDate() {  
        StringBuffer UTCTimeBuffer = new StringBuffer();  
        // 1、取得本地时间：  
        Calendar cal = Calendar.getInstance() ;
        // 2、取得时间偏移量：  
        int zoneOffset = cal.get(Calendar.ZONE_OFFSET);
//        // 3、取得夏令时差：  
//        int dstOffset = cal.get(Calendar.DST_OFFSET);
        // 4、从本地时间里扣除这些差量，即可以取得UTC时间：
        //cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));  
        cal.add(Calendar.MILLISECOND, -zoneOffset);  
        return cal.getTime();  
    }  
	
	public static int ordinalIndexOf(String str, String substr, int n) {
	    int pos = str.indexOf(substr);
	    while (--n > 0 && pos != -1)
	        pos = str.indexOf(substr, pos + 1);
	    return pos;
	}
}
