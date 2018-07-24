package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.DevicePhotoMapper;
import com.htdz.def.dbmodel.DevicePhoto;

@Service
@MapperScan("com.htdz.db.mapper")
public class DevicePhotoService {
	@Autowired
	private DevicePhotoMapper devicePhotoMapper;

	public DevicePhoto select(String name) {
		return devicePhotoMapper.select(name);
	}

	public List<DevicePhoto> selectAll() {
		return devicePhotoMapper.selectAll();
	}

	public List<DevicePhoto> selectPage(int pageIndex, int pageCount) {
		Page<DevicePhoto> page = PageHelper.startPage(pageIndex, pageCount);
		devicePhotoMapper.selectAll();
		return page;
	}

	public boolean add(DevicePhoto devicePhoto) {
		int result = devicePhotoMapper.add(devicePhoto);
		return result == 1 ? true : false;
	}

	public boolean modify(DevicePhoto devicePhoto) {
		int result = devicePhotoMapper.modify(devicePhoto);
		return result == 1 ? true : false;
	}
}
