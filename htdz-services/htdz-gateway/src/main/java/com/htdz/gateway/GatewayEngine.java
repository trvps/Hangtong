package com.htdz.gateway;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.htdz.common.LogManager;
import com.htdz.common.utils.SpringContextUtil;
import com.htdz.gateway.devices.DeviceServerManager;


@Service
public class GatewayEngine implements SafeEngine.AttackListener {
	@Autowired
	private APIEngine apiEngine;
	
	@Autowired
	private SafeEngine safeEngine;
	
	@Autowired
	private DeviceServerManager deviceServerManager;
	
	@Autowired
	private Environment env;

	
	public static GatewayEngine getGatewayEngine() {
		return (GatewayEngine)SpringContextUtil.getBean(GatewayEngine.class);
	}
	
	public DeviceServerManager getDeviceServerManager() {
		return deviceServerManager;
	}
	
	public APIEngine getAPIEngine() {
		return apiEngine;
	}
	
	public Environment getEnvironment() {
		return env;
	}
	
	public static String getServerLocal() {
		return getGatewayEngine().getEnvironment().getProperty("htdz.serverlocal");
	}

	public void onContextInitCompleted() {
		LogManager.info("----------容器初始化完成----------");
		
		deviceServerManager.startDeviceServer();
		
		safeEngine.onContextInitCompleted();
		safeEngine.setAttackListener(this);
	}

	@Override
	public void detectedAttack(String ip, int port, String tag) {
		String[] tags = tag.split(",");
		if (tags[0].equalsIgnoreCase("Device")) {
			// 关闭设备连接
			LogManager.info("关闭攻击的设备连接 ip={}, port={}, deviceName={}, deviceSession={}", 
							ip, 
							port, 
							tags[1], 
							tags[2]);
			deviceServerManager.closeDevice(tags[1], tags[2]);
		}
	}

	@Override
	public void detectedAttack(String ip) {
	}
}

