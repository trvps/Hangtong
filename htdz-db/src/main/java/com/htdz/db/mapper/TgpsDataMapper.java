package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.htdz.db.sqlprovider.SqlProvider;
import com.htdz.def.dbmodel.TgpsData;

public interface TgpsDataMapper {

	@Select("select * from TGpsData where device_sn=#{0}")
	public TgpsData select(String deviceSn);

	@Select("select * from TGpsData")
	public List<TgpsData> selectAll();

	@Insert("insert into TGpsData(did, device_sn,collect_datetime,rcv_time,lat,lng,speed,direction,gps_flag,LBS_WIFI_Range) values(#{did}, #{deviceSn}, #{collectDatetime}, #{rcvTime}, #{lat}, #{lng}, #{speed}, #{direction}, #{gpsFlag}, #{lbsWifiRange})")
	@Options(useGeneratedKeys = true)
	public int add(TgpsData gpsData);

	// @Update("update TGpsData set name=#{name}, age=#{age} where id=#{id}")
	public int modify(TgpsData gpsData);

	@InsertProvider(type = SqlProvider.class, method = "executeSQL")
	// @InsertProvider @UpdateProvider @DeleteProvider
	public Integer insertGpsDataBySql(String sql);
}
