package com.htdz.db.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import com.htdz.def.view.DeviceInfo;

public interface DeviceInfoMapper {

	@Select("select d.id as did,d.device_sn as deviceSn, d.head_portrait as headPortrait, di.human_name as humanName,di.human_sex as humanSex,di.human_birthday as humanBirthday,di.human_height as humanHeight,di.human_weight as humanWeight,di.human_step as humanStep ,di.human_feature as humanFeature from TDevice d left join `TDeviceInfo` di on d.id = di.did  where device_sn=#{0}")
	public DeviceInfo getDeviceInfo(String deviceSn);

	@Update("update TDeviceInfo set human_name=#{humanName}, human_sex=#{humanSex},human_birthday=#{humanBirthday},human_weight=#{humanWeight},human_height=#{humanHeight},human_step=#{humanStep},human_feature=#{humanFeature} where did=#{did}")
	public Integer updateDeviceInfo(DeviceInfo deviceInfoView);

	@Insert("insert into TDeviceInfo(did,human_name, human_sex,human_birthday,human_height,human_weight,human_step,human_feature) values(#{did},#{humanName}, #{humanSex}, #{humanBirthday}, #{humanHeight}, #{humanWeight}, #{humanStep}, #{humanFeature})")
	public Integer addDeviceInfo(DeviceInfo deviceInfoView);

	@Select("select * from TDeviceInfo where did=#{did}")
	public DeviceInfo select(Integer did);

}
