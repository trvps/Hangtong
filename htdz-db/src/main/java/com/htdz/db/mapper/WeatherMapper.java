package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.htdz.def.dbmodel.DeviceRemind;
import com.htdz.def.dbmodel.Weather;

public interface WeatherMapper {

	@Select("SELECT city_name as cityName,temperature, w_code as Wcode FROM weather WHERE city_code = #{cityCode} and DATE(create_time) = #{Date} order BY w_time DESC LIMIT 1")
	public Weather selectByCityCode(@Param("cityCode") String cityCode, @Param("Date") String Date);

	@Select("select id,`device_sn` as deviceSn,`remind_time` as remindTime, `remind_time_utc` as remindTimeUTC,`title`, `type` from device_remind where DATE(remind_time_utc)=#{Date}")
	public List<DeviceRemind> selectByOneDate(@Param("Date") String Date);

	@Select("select * from weather")
	public List<Weather> selectAll();

	@Insert("insert into `weather`(city_code, city_name, temperature, w_code, w_time, create_time) values(#{cityCode}, #{cityName}, #{temperature}, #{wCode}, #{wTime}, #{createTime})")
	@Options(useGeneratedKeys = true)
	public int add(Weather weather);

}
