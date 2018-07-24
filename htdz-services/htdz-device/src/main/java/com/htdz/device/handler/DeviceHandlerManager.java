package com.htdz.device.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.htdz.db.service.DeviceService;
import com.htdz.db.service.DeviceSessionMapService;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.dbmodel.DeviceSessionMap;
import com.htdz.def.dbmodel.Tdevice;

@Component
public class DeviceHandlerManager {
	@Autowired
	DeviceService deviceService;

	@Autowired
	Environment env;

	@Autowired
	DeviceSessionMapService deviceSessionMapService;

	private Map<String, DeviceBaseHandler> deviceHandleMap = new HashMap<String, DeviceBaseHandler>();

	public void onContextInitCompleted() {
	}

	/**
	 * 定时任务，可清理资源
	 */
	public void scheduledWork() {
		for (Entry<String, DeviceBaseHandler> entry : deviceHandleMap
				.entrySet()) {
			entry.getValue().scheduledWork();
		}
	}

	/**
	 * 设备Handler自动注册
	 * 
	 * @param deviceName
	 * @param dh
	 */
	public void onDeviceHandleRegister(String deviceName, DeviceBaseHandler dh) {
		deviceHandleMap.put(deviceName, dh);
	}

	/**
	 * 设备注册通知
	 * 
	 * @param ri
	 * @param deviceName
	 * @param deviceSession
	 */
	public void handleDeviceRegisted(RouteInfo ri, String deviceName,
			String deviceSession) {
		DeviceBaseHandler dbh = deviceHandleMap.get(deviceName);
		if (dbh != null)
			dbh.handleDeviceRegisted(ri, deviceName, deviceSession);
	}

	/**
	 * 设备注销通知
	 * 
	 * @param ri
	 * @param deviceName
	 * @param deviceSession
	 */
	public void handleDeviceUnregisted(RouteInfo ri, String deviceName,
			String deviceSession) {
		DeviceBaseHandler dbh = deviceHandleMap.get(deviceName);
		if (dbh != null)
			dbh.handleDeviceUnregisted(ri, deviceName, deviceSession);
	}

	/**
	 * 分发数据到对应的设备处理器
	 * 
	 * @param ri
	 * @param deviceName
	 * @param deviceSession
	 * @param data
	 * @return
	 */
	public RPCResult handleDeviceMessage(RouteInfo ri, String deviceName,
			String deviceSession, byte[] data) {
		DeviceBaseHandler dbh = deviceHandleMap.get(deviceName);
		if (dbh != null) {
			return dbh.handleDeviceMessage(ri, deviceName, deviceSession, data);
		} else {
			return null;
		}
	}

	/**
	 * 处理平台发送到设备数据
	 * 
	 * @param ri
	 * @param path
	 * @param headers
	 * @param params
	 * @param reqBody
	 * @return
	 */
	public RPCResult hanleHttpRequest(RouteInfo ri, String path,
			Map<String, String> headers, Map<String, String[]> params,
			byte[] reqBody) {
		RPCResult ret = RPCResult.success();
		Map<String, String> map = new HashMap<String, String>();
		
		String deviceSn = params.get("deviceSn")[0];

		DeviceSessionMap deviceSessionMap = deviceSessionMapService
				.getByDeviceSn(deviceSn);
		if (deviceSessionMap != null) {
			String deviceName = deviceSessionMap.getDeviceName().toUpperCase();
			DeviceBaseHandler dbh = deviceHandleMap.get(deviceName);
			if (dbh != null) {
				return dbh.handleWebMessage(ri, path, deviceName, deviceSn,
						params);
			} else {
				map.put("result", "3");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}
		} else {
			map.put("result", "4");
			ret.setRpcResult(JSON.toJSONString(map,
					SerializerFeature.WriteDateUseDateFormat,
					SerializerFeature.DisableCircularReferenceDetect,
					SerializerFeature.WriteMapNullValue,
					SerializerFeature.DisableCheckSpecialChar));

			return ret;
		}
	}
}
