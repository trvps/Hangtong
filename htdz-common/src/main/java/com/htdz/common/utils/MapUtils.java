package com.htdz.common.utils;

/**
 * Map 地图 工具类
 * 
 * @author wj
 * 
 */
public class MapUtils {
	// private static double EARTH_RADIUS = 6378.137;
	private static double EARTH_RADIUS = 6371.393; // 地图半径

	private static double rad(double d) {
		return d * Math.PI / 180.0;
	}

	/**
	 * 计算两个经纬度之间的距离
	 * 
	 * @param lat1
	 * @param lng1
	 * @param lat2
	 * @param lng2
	 * @return
	 */
	public static boolean getDistance(double lat1, double lng1, double lat2,
			double lng2, Integer radius) {
		double s = distance(lat1, lng1, lat2, lng2);
		// 在圆外
		if (s > radius) {
			return true;
		}
		return false;
	}

	public static double distance(double lat1, double lng1, double lat2,
			double lng2) {
		double radLat1 = rad(lat1);
		double radLat2 = rad(lat2);
		double a = radLat1 - radLat2;
		double b = rad(lng1) - rad(lng2);
		double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
				+ Math.cos(radLat1) * Math.cos(radLat2)
				* Math.pow(Math.sin(b / 2), 2)));
		s = s * EARTH_RADIUS;
		s = Math.round(s * 1000);

		return s;
	}

	public static String getDistanceTest(double lat1, double lng1, double lat2,
			double lng2, Integer radius) {
		double s = distance(lat1, lng1, lat2, lng2);
		return "围栏半径：" + radius + "_距离：" + s;
	}

	public static void main(String[] args) {
		// System.out.println(MapUtils.GetDistance(29.490295,106.486654,29.615467,106.581515));
	}
}