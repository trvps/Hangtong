package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.SelectProvider;

import com.htdz.db.sqlprovider.SqlProvider;
import com.htdz.def.dbmodel.DeviceSessionMap;

public interface DeviceSessionMapMapper {
	@Select("SELECT deviceName,device_sn,deviceSession,createTime FROM `deviceSessionMap` WHERE `device_sn`=#{0} ORDER BY `createTime` DESC LIMIT 1")
	public DeviceSessionMap getByDeviceSn(String deviceSn);
	
	@Select("SELECT deviceName,device_sn,deviceSession,createTime FROM `deviceSessionMap` WHERE `deviceSession`=#{0}")
	public DeviceSessionMap getBySessionId(String sessionId);
	
	@Select("SELECT deviceName,device_sn,deviceSession,createTime FROM `deviceSessionMap` WHERE `device_sn`=#{0} AND `deviceSession`=#{1}")
	public DeviceSessionMap getdeviceSessionMap(String deviceSn,String sessionId);
	
	@Delete("DELETE FROM `deviceSessionMap` WHERE `deviceSession`=#{0}")
	public int delete(String sessionId);
	
	@Delete("DELETE FROM `deviceSessionMap` WHERE `device_sn`=#{0}")
	public int deleteByDeviceSn(String deviceSn);
	
	@Insert("INSERT INTO `deviceSessionMap`(deviceName,device_sn,deviceSession) VALUES (#{0},#{1},#{2})")
	public int save(String deviceName,String deviceSn,String sessionId);
	
	@Insert("update deviceSessionMap`set deviceSession = #{2} where device_sn = #{1}) and deviceName = #{0}")
	public int update(String deviceName,String deviceSn,String sessionId);
	
	@Select("SELECT deviceName,device_sn,deviceSession,createTime FROM `deviceSessionMap` WHERE `device_sn`=#{0}")
	public List<DeviceSessionMap> getListByDeviceSn(String deviceSn);
	
	@SelectProvider(type=SqlProvider.class,method="executeSQL")
	public List<DeviceSessionMap> getAll(String sql);
}
