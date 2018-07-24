package com.htdz.db.service;

import java.util.List;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.DeviceRemindMapper;
import com.htdz.def.dbmodel.DeviceRemind;

@Service
@MapperScan("com.htdz.db.mapper")
public class DeviceRemindService {
	@Autowired
	private DeviceRemindMapper deviceRemindMapper;

	public DeviceRemind select(Integer id) {
		return deviceRemindMapper.select(id);
	}

	public List<DeviceRemind> selectAll() {
		return deviceRemindMapper.selectAll();
	}

	public List<DeviceRemind> selectPage(int pageIndex, int pageCount) {
		Page<DeviceRemind> page = PageHelper.startPage(pageIndex, pageCount);
		deviceRemindMapper.selectAll();
		return page;
	}

	public Integer add(DeviceRemind deviceRemind) {
		return deviceRemindMapper.add(deviceRemind);
	}

	public boolean modify(DeviceRemind deviceRemind) {
		int result = deviceRemindMapper.modify(deviceRemind);
		return result == 1 ? true : false;
	}

	/**
	 * 根据设备号，时间查询数据
	 * 
	 * @param deviceSn
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public List<DeviceRemind> selectDeviceRemindByTime(String deviceSn, String startDate, String endDate) {
		return deviceRemindMapper.selectByDate(deviceSn, startDate, endDate);
	}

	/**
	 * 删除提醒
	 * 
	 * @param id
	 * @return
	 */
	public boolean delete(Integer id) {
		int result = deviceRemindMapper.delete(id);
		return result == 1 ? true : false;
	}

	/**
	 * 根据日期查询数据
	 * 
	 * @param deviceSn
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public List<DeviceRemind> selectDeviceRemindByOneDate(String Date) {
		return deviceRemindMapper.selectByOneDate(Date);
	}

	/**
	 * 根据设备号日期查询数据
	 * 
	 * @param deviceSn
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	public List<DeviceRemind> selectOneDeviceByOneDate(String deviceSn, String Date) {
		return deviceRemindMapper.selectOneDeviceByOneDate(deviceSn, Date);
	}
}
