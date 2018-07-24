package com.htdz.device.dubbo;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.def.data.Errors;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.DeviceService;
import com.htdz.device.DeviceEngine;
import com.htdz.device.handler.DeviceHandlerManager;

@Service
@com.alibaba.dubbo.config.annotation.Service(interfaceClass = DeviceService.class, version = "1.0.0")
public class DeviceServiceProvider implements DeviceService {
	@Autowired
	DeviceEngine deviceEngine;

	@Override
	public RPCResult handleDeviceMessage(RouteInfo ri, String deviceName,
			String deviceSession, byte[] data) {
		DeviceHandlerManager dhm = deviceEngine.getDeviceHandlerManager();
		return dhm.handleDeviceMessage(ri, deviceName, deviceSession, data);
	}
	
	public void handleDeviceRegisted(RouteInfo ri, String deviceName,
			String deviceSession) {
		DeviceHandlerManager dhm = deviceEngine.getDeviceHandlerManager();
		dhm.handleDeviceRegisted(ri, deviceName, deviceSession);
	}

	public void handleDeviceUnregisted(RouteInfo ri, String deviceName,
			String deviceSession) {
		DeviceHandlerManager dhm = deviceEngine.getDeviceHandlerManager();
		dhm.handleDeviceUnregisted(ri, deviceName, deviceSession);
	}
	
	public RPCResult hanleHttpRequest(RouteInfo ri, 
			String path, 
			Map<String, String> headers, 
			Map<String, String[]> params, 
			byte[] reqBody) {
		RPCResult ret = deviceEngine.hanleHttpRequest(ri, path, headers, params, reqBody);
		ret.setRpcErrCode(Errors.ERR_SUCCESS);
		
		return ret;
	}
}
