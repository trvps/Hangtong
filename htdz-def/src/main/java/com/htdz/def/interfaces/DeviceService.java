package com.htdz.def.interfaces;


import java.util.Map;

import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;


/*
 * 建议设备服务接口至少有3个参数，并且第三个参数必须是String deviceSession，
 * 因为该参数会用于dubbo的负载均衡策略计算
 */
public interface DeviceService {
	public RPCResult handleDeviceMessage(RouteInfo ri, 
											String deviceName, 
											String deviceSession, 
											byte[] data);
	public void handleDeviceRegisted(RouteInfo ri, 
											String deviceName, 
											String deviceSession);
	public void handleDeviceUnregisted(RouteInfo ri, 
											String deviceName, 
											String deviceSession);
	
	public RPCResult hanleHttpRequest(RouteInfo ri, 
			String path, 
			Map<String, String> headers, 
			Map<String, String[]> params, 
			byte[] reqBody);
}
