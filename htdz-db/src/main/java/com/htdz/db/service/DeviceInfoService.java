package com.htdz.db.service;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.db.mapper.DeviceInfoMapper;
import com.htdz.def.view.DeviceInfo;

@Service
@MapperScan("com.htdz.db.mapper")
public class DeviceInfoService {
	@Autowired
	private DeviceInfoMapper deviceInfoMapper;

	public DeviceInfo select(Integer did) {
		return deviceInfoMapper.select(did);
	}

	public DeviceInfo getDeviceInfo(String deviceSn) {
		return deviceInfoMapper.getDeviceInfo(deviceSn);
	}

	public Integer addDeviceInfo(DeviceInfo deviceInfoView) {
		return deviceInfoMapper.addDeviceInfo(deviceInfoView);
	}

	public boolean updateDeviceInfo(DeviceInfo DeviceInfoView) {
		int result = deviceInfoMapper.updateDeviceInfo(DeviceInfoView);
		return result == 1 ? true : false;
	}

}
