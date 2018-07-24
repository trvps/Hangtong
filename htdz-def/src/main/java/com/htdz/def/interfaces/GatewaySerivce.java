package com.htdz.def.interfaces;


import java.util.Map;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;


public interface GatewaySerivce {
	/**
	 * 指定设备类型的在线设备数量，如果deviceName是null或者为空，则返回所有类型设备在线数量
	 * @param deviceName
	 * @return
	 */
	public Integer deviceOnlineCount(String deviceName);
	
	/**
	 * 设备是否在线
	 * @param deviceName
	 * @param deviceSession
	 * @return
	 */
	public Boolean isDeviceOnline(String deviceName, String deviceSession);
	
	/**
	 * 往设备推送消息
	 * @param deviceName
	 * @param deviceSession
	 * @param data
	 * @return
	 */
	public RPCResult pushMessageToDevice(String deviceName, String deviceSession, byte[] data);
	
	/**
	 * 关闭设备
	 * @param deviceName
	 * @param deviceSession
	 * @return
	 */
	public RPCResult closeDevice(String deviceName, String deviceSession);
	
	/**
	 * @param ri		路由链，每发起一次中转请求，把自己的信息添加，避免循环调用
	 * @param deviceName
	 * @param deviceSession
	 * @param data
	 * @return
	 */
	public RPCResult transferDeviceRequest(RouteInfo ri, 
											String deviceName, 
											String deviceSession, 
											byte[] data);
	
	
	/**
	 * @param ri		路由链，每发起一次中转请求，把自己的信息添加，避免循环调用
	 * @param serviceName
	 * @param path
	 * @param headers
	 * @param params
	 * @param reqBody
	 * @return
	 */
	public RPCResult transferURLRequest(RouteInfo ri, 
										String serviceName, 
										String path, 
										Map<String, String> headers, 
										Map<String, String[]> params, 
										byte[] reqBody);
}

