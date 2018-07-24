package com.htdz.gateway.service.dubbo;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Reference;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.DeviceService;

@Component
public class DeviceServiceConsumer implements DeviceService {
	@Reference(version = "1.0.0", check = false, loadbalance = "devicelb", timeout=60000)
	private DeviceService deviceServiceProvider;

	@Override
	public RPCResult handleDeviceMessage(RouteInfo ri, String deviceName,
			String deviceSession, byte[] data) {
		return deviceServiceProvider.handleDeviceMessage(ri, deviceName,
				deviceSession, data);
	}

	public void handleDeviceRegisted(RouteInfo ri, String deviceName,
			String deviceSession) {
		deviceServiceProvider.handleDeviceRegisted(ri, deviceName,
				deviceSession);
	}

	public void handleDeviceUnregisted(RouteInfo ri, String deviceName,
			String deviceSession) {
		deviceServiceProvider.handleDeviceUnregisted(ri, deviceName,
				deviceSession);
	}

	@Override
	public RPCResult hanleHttpRequest(RouteInfo ri, String path,
			Map<String, String> headers, Map<String, String[]> params,
			byte[] reqBody) {
		return deviceServiceProvider.hanleHttpRequest(ri, path, headers,
				params, reqBody);
	}
}
