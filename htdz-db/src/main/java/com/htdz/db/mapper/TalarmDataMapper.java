package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import com.htdz.db.sqlprovider.SqlProvider;
import com.htdz.def.dbmodel.TalarmData;

public interface TalarmDataMapper {

	@Select("select * from TAlarmData where id=#{0}")
	public TalarmData select(String id);

	@Select("select * from TAlarmData")
	public List<TalarmData> selectAll();

	@Insert("insert into TAlarmData(did, device_sn,collect_datetime,rcv_time,type,lat,lng,speed,direction,gps_flag) values(#{did}, #{deviceSn}, #{collectDatetime}, #{rcvTime}, #{type}, #{lat}, #{lng}, #{speed}, #{direction}, #{gpsFlag})")
	@Options(useGeneratedKeys = true)
	public int add(TalarmData talarmData);

	// @Update("update TAlarmData set name=#{name}, age=#{age} where id=#{id}")
	public int modify(TalarmData talarmData);

	@InsertProvider(type = SqlProvider.class, method = "executeSQL")
	// @InsertProvider @UpdateProvider @DeleteProvider
	public Integer insertAlarmDataBySql(String sql);
}
