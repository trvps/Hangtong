package com.htdz.def.dbmodel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GpsAndAlarmDataView {
	/**
	 * GPS数据
	 */
	private TgpsData gpsData;
	
	/**
	 * 警情数据
	 */
	private TalarmData alarmData;
	
	/**
	 * 当前数据类型,0:GPS数据  1：警情数据  2：GPS和警情数据
	 */
	private  Integer dataType;
}
