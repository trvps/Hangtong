package com.htdz.device.handler;


import com.htdz.def.data.RPCResult;


public interface DevicePackageHandler {
	public RPCResult handleDevicePackage(String deviceName, String deviceSession, byte[] data);
}
