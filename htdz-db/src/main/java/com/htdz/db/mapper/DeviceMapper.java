package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Result;
import org.apache.ibatis.annotations.Results;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.Tdevice;

public interface DeviceMapper {

	@Select("select * from TDevice where device_sn=#{0}")
	@Results({ @Result(property = "deviceSn", column = "device_sn"),
			@Result(property = "protocolType", column = "protocol_type"),
			@Result(property = "connCountry", column = "conn_country"),
			@Result(property = "expiredTime", column = "expired_time") })
	public Tdevice select(String deviceSn);

	@Select("select * from TDevice")
	@Results({ @Result(property = "deviceSn", column = "device_sn"),
			@Result(property = "protocolType", column = "protocol_type"),
			@Result(property = "connCountry", column = "conn_country"),
			@Result(property = "expiredTime", column = "expired_time") })
	public List<Tdevice> selectAll();

	@Insert("insert into `TDevice`(device_sn, timezone, timezoneid, data_source, disable, create_time, gps_interval, ranges, protocol_type, product_type, expired_time, expired_time_de) values(#{deviceSn}, #{timezone}, #{timezoneid}, #{dataSource}, #{disable}, #{createTime}, #{gpsInterval}, #{ranges}, #{protocolType}, #{productType}, #{expiredTime}, #{expiredTimeDe})")
	@Options(useGeneratedKeys = true)
	public int add(Tdevice device);

	@Update("update TDevice set name=#{name}, age=#{age} where id=#{id}")
	public int modify(Tdevice user);

	@Select("select COUNT(0) from `TDevice` d  inner join  `TDeviceUser` du on d.id = du.`did` where d.`device_sn`=#{0}")
	public Integer getIsBinding(String deviceSn);

	@Update("update TDevice set mobile1=#{mobile1}, mobile2=#{mobile2},mobile3=#{mobile3} where device_sn=#{deviceSn}")
	public int updateMobile(Tdevice device);

	@Update("update TDevice set steps=#{steps} where device_sn=#{deviceSn}")
	public int updateSteps(Tdevice device);

	@Select("select device_sn as deviceSn, mobile1, mobile2, mobile3 from TDevice where device_sn=#{0}")
	public Tdevice selectMobile(String deviceSn);

	@Select("select steps from TDevice where device_sn=#{0}")
	public Tdevice selectSteps(String deviceSn);

	@Update("update TDevice set head_portrait=#{headPortrait} where device_sn=#{deviceSn}")
	public int updatePortrait(Tdevice device);

	@Select("select * from `TDevice` d  inner join  `TDeviceUser` du on d.id = du.`did` where d.`device_sn`=#{0} AND du.`is_super_user` = 1")
	@Results({ @Result(property = "deviceSn", column = "device_sn"),
			@Result(property = "protocolType", column = "protocol_type"),
			@Result(property = "connCountry", column = "conn_country"),
			@Result(property = "expiredTime", column = "expired_time") })
	public Tdevice selectBindingDevice(String deviceSn);

	@Update("UPDATE `TDevice` SET hardware = #{0} where device_sn=#{1}")
	public int updateHardware(int hardware, String deviceSn);
}
