package com.htdz.def.dbmodel;

import java.io.Serializable;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpsDataView implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private String result;
	private String lat;
	private String lng;
	private String gpstime;
	private String speed;
	private String direction;
	private String satellite_num;
	private String gps_flag;
	private String flag;
	private String battery;
	private String steps;
	private String online;
	private String LBS_WIFI_Range;
	private String calorie;
	private String carstatus;
}
