package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.DeviceRemind;

public interface DeviceRemindMapper {

	@Select("select id,`device_sn` as deviceSn,`remind_time` as remindTime, `remind_time_utc` as remindTimeUTC, `title`,`type` from device_remind where id=#{0}")
	public DeviceRemind select(Integer id);

	@Select("select id,`device_sn` as deviceSn,`remind_time` as remindTime, `remind_time_utc` as remindTimeUTC, `title`,`type`  from device_remind")
	public List<DeviceRemind> selectAll();

	@Insert("insert into device_remind(device_sn, remind_time, remind_time_utc, title, type) values(#{deviceSn}, #{remindTime}, #{remindTimeUTC}, #{title}, #{type})")
	@Options(useGeneratedKeys = true)
	public int add(DeviceRemind deviceRemind);

	@Update("update device_remind set name=#{name}, age=#{age} where id=#{id}")
	public int modify(DeviceRemind deviceRemind);

	@Select("select id, `device_sn` as deviceSn, `remind_time` as remindTime, `remind_time_utc` as remindTimeUTC, `title`, `type` from device_remind where device_sn=#{deviceSn} and remind_time>=#{startDate} and remind_time<=#{endDate}")
	public List<DeviceRemind> selectByDate(@Param("deviceSn") String deviceSn, @Param("startDate") String startDate,
			@Param("endDate") String endDate);

	@Delete("delete from `device_remind` where id = #{0}")
	public int delete(Integer id);

	@Select("select id,`device_sn` as deviceSn,`remind_time` as remindTime, `remind_time_utc` as remindTimeUTC,`title`, `type` from device_remind where DATE(remind_time_utc)=#{Date}")
	public List<DeviceRemind> selectByOneDate(@Param("Date") String Date);

	@Select("select id,`device_sn` as deviceSn,`remind_time` as remindTime, `remind_time_utc` as remindTimeUTC,`title`, `type` from device_remind where DATE(remind_time_utc)=#{Date} and device_sn=#{deviceSn}")
	public List<DeviceRemind> selectOneDeviceByOneDate(@Param("deviceSn") String deviceSn, @Param("Date") String Date);
}
