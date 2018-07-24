package com.htdz.db.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.db.mapper.TAlarmSettingMapper;
import com.htdz.def.dbmodel.TAlarmSetting;

@Service
@MapperScan("com.htdz.db.mapper")
public class TAlarmSettingService {
	@Autowired
	TAlarmSettingMapper tAlarmSettingMapper;

	public TAlarmSetting getAlarmSettingById(Integer did) {
		return tAlarmSettingMapper.getAlarmSettingById(did);
	}
}
