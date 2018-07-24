package com.htdz.device;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.htdz.common.LogManager;
import com.htdz.common.utils.SpringContextUtil;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.device.handler.DeviceHandlerManager;


@Service
public class DeviceEngine {
	@Autowired
	private DeviceHandlerManager dhManager;
	
	private ScheduledExecutorService scheduledES = null;
	
	public DeviceEngine() {		
		scheduledES = Executors.newScheduledThreadPool(1);
	}
	
	public static DeviceEngine getDeviceEngine() {
		return (DeviceEngine)SpringContextUtil.getBean(DeviceEngine.class);
	}
	
	public DeviceHandlerManager getDeviceHandlerManager() {
		return dhManager;
	}
	
	
	public void onContextInitCompleted() {
		LogManager.info("----------容器初始化完成----------");
		dhManager.onContextInitCompleted();
		
		// 启动5分钟定时器，主要用于清理资源
		scheduledES.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				dhManager.scheduledWork();
			}
			
		}, 5, 5, TimeUnit.MINUTES);
	}
	
	public RPCResult hanleHttpRequest(RouteInfo ri, 
			String path, 
			Map<String, String> headers, 
			Map<String, String[]> params, 
			byte[] reqBody) {
		RPCResult ret = RPCResult.success();
		
		if (path.startsWith("setdevice/")) {
			ret = dhManager.hanleHttpRequest(ri, path, headers, params, reqBody);
		} else {			
			Map<String, String> map = new HashMap<String, String>();
			map.put("result", "3");
			ret.setRpcResult(JSON.toJSONString(map,
					SerializerFeature.WriteDateUseDateFormat,
					SerializerFeature.DisableCircularReferenceDetect,
					SerializerFeature.WriteMapNullValue,
					SerializerFeature.DisableCheckSpecialChar));
		}
		
		LogManager.info(ret.toString());
		
		return ret;
	}
}
