package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.DeviceTurnMapper;
import com.htdz.def.dbmodel.DeviceTurn;

@Service
@MapperScan("com.htdz.db.mapper")
public class DeviceTurnService {
	@Autowired
	private DeviceTurnMapper deviceTurnMapper;

	public DeviceTurn select(DeviceTurn deviceTurn) {
		return deviceTurnMapper.select(deviceTurn);
	}

	public List<DeviceTurn> selectAll() {
		return deviceTurnMapper.selectAll();
	}

	public List<DeviceTurn> selectPage(int pageIndex, int pageCount) {
		Page<DeviceTurn> page = PageHelper.startPage(pageIndex, pageCount);
		deviceTurnMapper.selectAll();
		return page;
	}

	public boolean add(DeviceTurn deviceTurn) {
		int result = deviceTurnMapper.add(deviceTurn);
		return result == 1 ? true : false;
	}

	public boolean modify(DeviceTurn deviceTurn) {
		int result = deviceTurnMapper.modify(deviceTurn);
		return result == 1 ? true : false;
	}
}
