package com.htdz.db.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.db.sqlprovider.SqlProvider;
import com.htdz.def.dbmodel.GpsDataLast;

public interface GpsDataLastMapper {
	@Update("UPDATE `gps_data_last` SET `online`=#{1} WHERE `device_sn`=#{0}")
	public Integer updateOnlineStatus(String deviceSn, Integer online);

	@Delete("DELETE FROM `gps_data_last` WHERE `device_sn`=#{0}")
	public int delete(String deviceSn);

	@InsertProvider(type = SqlProvider.class, method = "executeSQL")
	public Integer insertLastGps(String sql);

	@Select("select * from gps_data_last where device_sn=#{0}")
	public GpsDataLast select(String deviceSn);

	@Insert("insert into gps_data_last(device_sn, collect_datetime, rcv_time, lat, lng, speed, direction, gps_flag, LBS_WIFI_Range, online) values(#{deviceSn}, #{collectDatetime}, #{rcvTime}, #{lat}, #{lng}, #{speed}, #{direction}, #{gpsFlag}, #{lbsWifiRange}, #{online})")
	@Options(useGeneratedKeys = true)
	public int add(GpsDataLast gpsData);

	@Update("update gps_data_last set collect_datetime=#{collectDatetime}, rcv_time=#{rcvTime}, lat=#{lat}, lng=#{lng}, speed=#{speed}, direction=#{direction}, gps_flag=#{gpsFlag}, LBS_WIFI_Range=#{lbsWifiRange}, online=#{online} where device_sn=#{deviceSn}")
	public int modify(GpsDataLast gpsData);

	@Update("UPDATE `gps_data_last` SET `battery`=#{1} WHERE `device_sn`=#{0} ")
	public int updateBattery(String deviceSn,String battery);
}
