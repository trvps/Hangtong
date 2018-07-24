package com.htdz.db.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.dbmodel.DeviceTurn;

public interface DeviceTurnMapper {

	@Select("select * from device_turn where device_sn=#{deviceSn} and create_time=#{createTime}")
	public DeviceTurn select(DeviceTurn deviceTurn);

	@Select("select * from device_turn")
	public List<DeviceTurn> selectAll();

	@Insert("insert into device_turn(device_sn, turn,create_time) values(#{deviceSn}, #{turn}, #{createTime})")
	@Options(useGeneratedKeys = true)
	public int add(DeviceTurn deviceTurn);

	@Update("update device_turn set turn=#{turn} where device_sn=#{deviceSn} and create_time=#{createTime}")
	public int modify(DeviceTurn deviceTurn);

}
