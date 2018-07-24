package com.htdz.db.mapper;

import org.apache.ibatis.annotations.Select;

import com.htdz.def.dbmodel.TAlarmSetting;

public interface TAlarmSettingMapper {
	@Select("SELECT * FROM `TAlarmSetting` WHERE `did`=#{0}")
	public TAlarmSetting getAlarmSettingById(Integer did);
}
