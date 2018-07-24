package com.htdz.liteguardian.util;

import java.util.HashMap;
import java.util.Map;

public class DistanceUtil {
	private static double EARTH_RADIUS = 6371;  //地球半径，平均半径为6371km
	
	/**
	 * 计算经纬度点对应内接正方形4个点的坐标,取得最大最小经纬度
	 * @param longitude
	 * @param latitude
	 * @param distance
	 * @return
	 */
	public static Map<String, Double> LLSquareMaxMinPoint(double longitude,

	        double latitude, double distance) {

	    Map<String, Double> squareMap = new HashMap<String, Double>();

	    // 计算经度弧度,从弧度转换为角度

	    double dLongitude = 2 * (Math.asin(Math.sin(distance

	            / (2 * EARTH_RADIUS))

	            / Math.cos(Math.toRadians(latitude))));

	    dLongitude = Math.toDegrees(dLongitude);

	    // 计算纬度角度

	    double dLatitude = distance / EARTH_RADIUS;

	    dLatitude = Math.toDegrees(dLatitude);

	    // 内接正方形

	    squareMap.put("maxLatitude", latitude + dLatitude);

	    squareMap.put("minLatitude", latitude - dLatitude);

	    squareMap.put("maxLongitude", longitude + dLongitude);

	    squareMap.put("minLongitude", longitude - dLongitude);

	    return squareMap;
	}
	/**
	 * 
	 * @param d
	 * @return
	 */
	private static double rad(double d) {
		return d * Math.PI / 180.0;
	}
	
	/**
	 * 计算2点间距离
	 * @param lat1
	 * @param lng1
	 * @param lat2
	 * @param lng2
	 * @return
	 */
	public static double getDistance(double lat1, double lng1, double lat2, double lng2) {

		double radLat1 = rad(lat1);
		double radLat2 = rad(lat2);
		double a = radLat1 - radLat2;
		double b = rad(lng1) - rad(lng2);
		double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
				+ Math.cos(radLat1) * Math.cos(radLat2)
				* Math.pow(Math.sin(b / 2), 2)));
		s = s * EARTH_RADIUS * 1000;
		s = Math.round(s * 10000) / 10000;
		return s;
	}
	
}
