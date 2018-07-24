package com.htdz.device.handler;


import java.util.Map;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import com.htdz.common.utils.SpringContextUtil;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.device.DeviceEngine;


public interface DeviceBaseHandler extends ApplicationListener<ContextRefreshedEvent> {
	public void scheduledWork();
	
	public String getDeviceName();

	// 处理设备上传数据
	public RPCResult handleDeviceMessage(RouteInfo ri, String deviceName,
			String deviceSession, byte[] data);
	
	public RPCResult handleWebMessage(RouteInfo ri, 
										String path, 
										String deviceName, 
										String deviceSn, 
										Map<String, String[]> params);

	public void handleDeviceRegisted(RouteInfo ri, String deviceName,
			String deviceSession);

	public void handleDeviceUnregisted(RouteInfo ri, String deviceName,
			String deviceSession);
	
	@Override
	default public void onApplicationEvent(ContextRefreshedEvent event) {
		SpringContextUtil.setApplicationContext(event.getApplicationContext());
		DeviceHandlerManager dhm = DeviceEngine.getDeviceEngine().getDeviceHandlerManager();
		dhm.onDeviceHandleRegister(getDeviceName(), this);
	}
}
