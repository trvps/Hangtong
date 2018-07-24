package com.htdz.device.handler.HT720;


import java.util.Map;
import org.springframework.stereotype.Component;
import com.htdz.common.Consts;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.device.handler.DeviceBaseHandler;


@Component
public class Device720Handler implements DeviceBaseHandler {
	@Override
	public void scheduledWork() {
	}
	
	public String getDeviceName() {
		return Consts.Device_HT720;
	}

	@Override
	public void handleDeviceRegisted(RouteInfo ri, String deviceName,
			String deviceSession) {
	}

	@Override
	public void handleDeviceUnregisted(RouteInfo ri, String deviceName,
			String deviceSession) {

	}

	@Override
	public RPCResult handleDeviceMessage(RouteInfo ri, String deviceName,
			String deviceSession, byte[] data) {
		return null;
	}
	
	public RPCResult handleWebMessage(RouteInfo ri,String path,String deviceName,String deviceSn, Map<String, String[]> params)
	{
		return null;
	}
}
