package com.htdz.gateway.service.dubbo;


import java.util.Map;
import org.springframework.stereotype.Service;
import com.htdz.common.LogManager;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.GatewaySerivce;
import com.htdz.gateway.APIEngine;
import com.htdz.gateway.GatewayEngine;
import com.htdz.gateway.devices.DeviceServerManager;


@Service
@com.alibaba.dubbo.config.annotation.Service(interfaceClass = GatewaySerivce.class, version="1.0.0")
public class GatewaySerivceProvider implements GatewaySerivce {
	/**
	 * 指定设备类型的在线设备数量，如果deviceName是null或者为空，则返回所有类型设备在线数量
	 */
	@Override
	public Integer deviceOnlineCount(String deviceName) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		return dsm.deviceOnlineCount(deviceName);
	}

	/**
	 * 设备是否在线
	 */
	@Override
	public Boolean isDeviceOnline(String deviceName, String deviceSession) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		return dsm.deviceIsResistered(deviceName, deviceSession);
	}
	
	/**
	 * 推送消息到设备
	 */
	@Override
	public RPCResult pushMessageToDevice(String deviceName, String deviceSession, byte[] data) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		int resultcount = dsm.pushMessageToDevice(deviceName, deviceSession, data);
		RPCResult result = RPCResult.success();
		result.setRpcResult(resultcount);
		return result;
	}
	
	/**
	 * 关闭设备
	 */
	public RPCResult closeDevice(String deviceName, String deviceSession) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		int resultcount = dsm.closeDevice(deviceName, deviceSession);
		RPCResult result = RPCResult.success();
		result.setRpcResult(resultcount);
		return result;
	}
	
	/**
	 * 接收到中转过来的设备数据
	 */
	@Override
	public RPCResult transferDeviceRequest(RouteInfo ri, String deviceName, String deviceSession, byte[] data) {
		try {
			String serverlocal = GatewayEngine.getGatewayEngine().getEnvironment().getProperty("htdz.serverlocal");
			
			if (ri.isInRoute(serverlocal)) {
				// 循环中转
				return RPCResult.failed();
			}
			
			if (ri.decreasementTTL() < 0) {
				// 中转次数太多
				return RPCResult.failed();
			}
			
			RPCResult result = GatewayEngine.getGatewayEngine().getAPIEngine().deviceRequestDispatch(ri, deviceName, deviceSession, data);
			return result;
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
		
		return RPCResult.failed();
	}

	
	/**
	 * 接收到中转过来的服务请求
	 */
	@Override
	public RPCResult transferURLRequest(RouteInfo ri, 
											String serviceName, 
											String path,
											Map<String, String> headers,
											Map<String, String[]> params, 
											byte[] reqBody) {
		try {
			LogManager.info("transferURLRequest from {}", ri.printRouteInfo());
			LogManager.info("serviceName {}", serviceName);
			LogManager.info("path {}", path);
			if (reqBody != null)
				LogManager.info("reqBody {}", new String(reqBody, "utf-8"));
			
			String serverlocal = GatewayEngine.getGatewayEngine().getEnvironment().getProperty("htdz.serverlocal");
			
			if (ri.isInRoute(serverlocal)) {
				// 循环中转
				return RPCResult.failed();
			}
			
			if (ri.decreasementTTL() < 0) {
				// 中转次数太多
				return RPCResult.failed();
			}
			
			// 加入路由链表
			ri.addRouteInfo(serverlocal);
			
			APIEngine apiEngine = GatewayEngine.getGatewayEngine().getAPIEngine();
			RPCResult result = apiEngine.serviceRequestDispatch(ri, serviceName, path, headers, params, reqBody);
			return result;
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
		
		return RPCResult.failed();
	}
}

