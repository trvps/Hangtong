package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.DeviceStepMapper;
import com.htdz.def.dbmodel.DeviceStep;

@Service
@MapperScan("com.htdz.db.mapper")
public class DeviceStepService {
	@Autowired
	private DeviceStepMapper deviceStepMapper;

	public DeviceStep select(String deviceSn) {
		return deviceStepMapper.getByDeviceSn(deviceSn);
	}
	
	public DeviceStep select(String deviceSn,String createDate)
	{
		return deviceStepMapper.select(deviceSn,createDate);
	}

	public List<DeviceStep> selectAll() {
		return deviceStepMapper.selectAll();
	}

	public List<DeviceStep> selectPage(int pageIndex, int pageCount) {
		Page<DeviceStep> page = PageHelper.startPage(pageIndex, pageCount);
		deviceStepMapper.selectAll();
		return page;
	}

	public boolean add(DeviceStep deviceStep) {
		int result = deviceStepMapper.add(deviceStep);
		return result == 1 ? true : false;
	}

	public boolean modify(DeviceStep deviceStep) {
		int result = deviceStepMapper.modify(deviceStep);
		return result == 1 ? true : false;
	}

	public DeviceStep selectOne(String deviceSn) {
		return deviceStepMapper.selectOne(deviceSn);
	}

	/**
	 * 根据时间查询数据
	 * 
	 * @param deviceSn
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public List<DeviceStep> selectByDate(String deviceSn, String startDate, String endDate) {
		return deviceStepMapper.selectByDate(deviceSn, startDate, endDate);

	}

	/**
	 * 查询一天步数数据
	 * 
	 * @param deviceSn
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public List<DeviceStep> selectByOneDate(String deviceSn, String startDate) {
		return deviceStepMapper.selectByOneDate(deviceSn, startDate);

	}

}
