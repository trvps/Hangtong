package com.htdz.db.service;

import java.util.List;

import org.apache.ibatis.annotations.SelectProvider;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.db.mapper.DeviceSessionMapMapper;
import com.htdz.db.sqlprovider.SqlProvider;
import com.htdz.def.dbmodel.DeviceSessionMap;

@Service
@MapperScan("com.htdz.db.mapper")
public class DeviceSessionMapService {
	@Autowired DeviceSessionMapMapper deviceSessionMap;
	
	public DeviceSessionMap getByDeviceSn(String deviceSn)
	{
		return deviceSessionMap.getByDeviceSn(deviceSn);
	}
	
	public DeviceSessionMap getBySessionId(String sessionId)
	{
		return deviceSessionMap.getBySessionId(sessionId);
	}	
	
	public DeviceSessionMap getdeviceSessionMap(String deviceSn,String sessionId)
	{
		return deviceSessionMap.getdeviceSessionMap(deviceSn, sessionId);
	}
	
	public boolean delete(String sessionId)
	{
		return deviceSessionMap.delete(sessionId)>0;
	}
	
	public boolean deleteByDeviceSn(String deviceSn)
	{
		return deviceSessionMap.deleteByDeviceSn(deviceSn) > 0;
	}
	
	public boolean save(String deviceName,String deviceSn,String sessionId)
	{
		return deviceSessionMap.save(deviceName,deviceSn, sessionId) >0;
	}
	
	public int update(String deviceName,String deviceSn,String sessionId) {
		return deviceSessionMap.update(deviceName, deviceSn, sessionId);
	}
	
	public List<DeviceSessionMap> getListByDeviceSn(String deviceSn)
	{
		return deviceSessionMap.getListByDeviceSn(deviceSn);
	}
	
	public List<DeviceSessionMap> getAll(String sql)
	{
		return deviceSessionMap.getAll(sql);
	}
}
