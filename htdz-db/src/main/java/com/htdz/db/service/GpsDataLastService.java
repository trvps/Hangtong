package com.htdz.db.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.db.mapper.GpsDataLastMapper;
import com.htdz.def.dbmodel.GpsDataLast;

@Service
@MapperScan("com.htdz.db.mapper")
public class GpsDataLastService {
	@Autowired
	private GpsDataLastMapper gpsDataLastMapper;

	public boolean updateOnlineStatus(String deviceSn, Integer online) {
		int result = gpsDataLastMapper.updateOnlineStatus(deviceSn, online);
		return result == 1 ? true : false;
	}

	public boolean delete(String deviceSn) {
		return gpsDataLastMapper.delete(deviceSn) > 0;
	}

	public boolean insertGpsDataBySql(String sql) {
		int result = gpsDataLastMapper.insertLastGps(sql);
		return result == 1 ? true : false;
	}

	public GpsDataLast select(String deviceSn) {
		return gpsDataLastMapper.select(deviceSn);
	}

	public Integer add(GpsDataLast tgpsData) {
		return gpsDataLastMapper.add(tgpsData);

	}

	public boolean modify(GpsDataLast tgpsData) {
		int result = gpsDataLastMapper.modify(tgpsData);
		return result == 1 ? true : false;
	}
	
	public boolean updateBattery(String deviceSn,String battery)
	{
		int result = gpsDataLastMapper.updateBattery(deviceSn,battery);
		return result == 1 ? true : false;
	}
}
