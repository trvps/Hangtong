package com.htdz.db.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.htdz.db.mapper.DeviceMapper;
import com.htdz.def.dbmodel.Tdevice;


import lombok.Data;


@Service
@MapperScan("com.htdz.db.mapper")
public class DeviceService {
	@Autowired
	private DeviceMapper deviceMapper;
	
//	// 30 分钟失效
//	private static final long Expire = 30 * 60 * 1000;
//	
//	@Data
//	private static class TdeviceItem {
//		private Tdevice td;
//		private long timestamp;
//		private boolean updated;
//	}
//	
//	private ConcurrentHashMap<String, TdeviceItem> tdmap = new ConcurrentHashMap<String, TdeviceItem>(1000);

	public Tdevice select(String deviceSn) {
		return deviceMapper.select(deviceSn);
		
//		TdeviceItem tdItem = tdmap.get(deviceSn);
//		if (tdItem == null || (System.currentTimeMillis() - tdItem.getTimestamp() >= Expire)) {
//			Tdevice td = deviceMapper.select(deviceSn);
//			
//			if (tdItem == null)
//				tdItem = new TdeviceItem();
//			
//			tdItem.setTd(td);
//			tdItem.setTimestamp(System.currentTimeMillis());
//			tdmap.put(deviceSn, tdItem);
//			
//			return td;
//		} 
//		
//		return tdItem.getTd();
	}

	public List<Tdevice> selectAll() {
		return deviceMapper.selectAll();
	}

	public List<Tdevice> selectPage(int pageIndex, int pageCount) {
		Page<Tdevice> page = PageHelper.startPage(pageIndex, pageCount);
		deviceMapper.selectAll();
		return page;
	}

	public boolean add(Tdevice device) {
		int result = deviceMapper.add(device);
		return result == 1 ? true : false;
	}

	public boolean modify(Tdevice device) {
		int result = deviceMapper.modify(device);
		return result == 1 ? true : false;
	}

	public Integer getIsBinding(String deviceSn) {
		return deviceMapper.getIsBinding(deviceSn);
	}

	public boolean updateMobile(Tdevice device) {
		int result = deviceMapper.updateMobile(device);
		return result == 1 ? true : false;
	}

	public boolean updateSteps(Tdevice device) {
		int result = deviceMapper.updateSteps(device);
		return result == 1 ? true : false;
	}

	public Tdevice selectMobile(String deviceSn) {
		return deviceMapper.selectMobile(deviceSn);
	}

	public Tdevice selectSteps(String deviceSn) {
		return deviceMapper.selectSteps(deviceSn);
	}

	public boolean updatePortrait(Tdevice device) {
		int result = deviceMapper.updatePortrait(device);
		return result == 1 ? true : false;
	}

	public Tdevice selectBindingDevice(String deviceSn) {
		return deviceMapper.selectBindingDevice(deviceSn);
	}
	
	public int updateHardware(int hardware, String deviceSn)
	{
//		TdeviceItem tdItem = tdmap.get(deviceSn);
//		if (tdItem != null) {
//			if (!tdItem.isUpdated()) {
//				deviceMapper.updateHardware(hardware, deviceSn);
//				tdItem.setUpdated(true);
//			}
//			
//			return 1;
//		}
		
		return deviceMapper.updateHardware(hardware, deviceSn);
	}
}
