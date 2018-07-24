package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.DeviceStep;

public interface DeviceStepMapper {

	@Select("select `device_sn` as deviceSn,`step`,`create_date` as createDate,`update_date` as updateDate from `device_step` where device_sn=#{0}")
	public DeviceStep getByDeviceSn(String deviceSn);
	
	@Select("select `device_sn` as deviceSn,`step`,`create_date` as createDate,`update_date` as updateDate from `device_step` where device_sn=#{0} AND create_date=#{1}")
	public DeviceStep select(String deviceSn,String createDate);

	@Select("select `device_sn` as deviceSn,`step`,`create_date` as createDate,`update_date` as updateDate from `device_step`")
	public List<DeviceStep> selectAll();

	@Insert("insert into device_step(device_sn, step, create_date, update_date) values(#{deviceSn}, #{step}, #{createDate}, #{updateDate})")
	@Options(useGeneratedKeys = true)
	public int add(DeviceStep deviceStep);

	@Update("update device_step set step=#{step}, update_date=#{updateDate} where device_sn=#{deviceSn} and create_date=#{createDate}")
	public int modify(DeviceStep deviceStep);

	@Select("select `device_sn` as deviceSn,`step`,`create_date` as createDate,`update_date` as updateDate from `device_step` where device_sn=#{deviceSn} order by create_date desc limit 1")
	public DeviceStep selectOne(@Param("deviceSn") String deviceSn);

	@Select("select `device_sn` as deviceSn,`step`,`create_date` as createDate,`update_date` as updateDate  from device_step where device_sn=#{deviceSn} and DATE(create_date)>=DATE(#{startDate}) and DATE(create_date)<=DATE(#{endDate}) order BY createDate ASC")
	public List<DeviceStep> selectByDate(@Param("deviceSn") String deviceSn, @Param("startDate") String startDate,
			@Param("endDate") String endDate);

	@Select("select `device_sn` as deviceSn,`step`,`create_date` as createDate,`update_date` as updateDate  from device_step where device_sn=#{deviceSn} and DATE(create_date)=#{startDate}")
	public List<DeviceStep> selectByOneDate(@Param("deviceSn") String deviceSn, @Param("startDate") String startDate);
}
