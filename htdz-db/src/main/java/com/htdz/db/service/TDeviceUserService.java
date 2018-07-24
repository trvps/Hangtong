package com.htdz.db.service;


import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.htdz.db.mapper.TDeviceUserMapper;
import com.htdz.def.dbmodel.TDeviceUser;
import lombok.Data;


@Service
@MapperScan("com.htdz.db.mapper")
public class TDeviceUserService {
	@Autowired
	TDeviceUserMapper tDeviceUserMapper;
	
	// 30 分钟失效
//	private static final long Expire = 30 * 60 * 1000;
//	@Data
//	private static class TdeviceUserItem {
//		private List<TDeviceUser> tdulist;
//		private long timestamp;
//		private boolean updated;
//	}
//	
//	private ConcurrentHashMap<String, TdeviceUserItem> tdlistmap = new ConcurrentHashMap<String, TdeviceUserItem>(1000);
	
	public List<TDeviceUser> getByUid(Integer uid)
	{
		return tDeviceUserMapper.getByUid(uid);
	}
	
	public List<TDeviceUser> getByDid(Integer did)
	{
		return tDeviceUserMapper.getByDid(did);
	}
	
	public List<TDeviceUser> getByDeviceSn(String deviceSn) {
		return tDeviceUserMapper.getByDeviceSn(deviceSn);
		
//		TdeviceUserItem tduItem = tdlistmap.get(deviceSn);
//		if (tduItem == null || (System.currentTimeMillis() - tduItem.getTimestamp() >= Expire)) {
//			List<TDeviceUser> tdulist = tDeviceUserMapper.getByDeviceSn(deviceSn);
//			
//			if (tduItem == null)
//				tduItem = new TdeviceUserItem();
//			
//			tduItem.setTdulist(tdulist);
//			tduItem.setTimestamp(System.currentTimeMillis());
//			tdlistmap.put(deviceSn, tduItem);
//			
//			return tdulist;
//		} 
//		
//		return tduItem.getTdulist();
	}
}
