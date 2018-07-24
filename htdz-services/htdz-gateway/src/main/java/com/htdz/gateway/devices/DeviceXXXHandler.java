package com.htdz.gateway.devices;


import com.htdz.gateway.GatewayEngine;
import io.netty.channel.ChannelHandlerContext;


public class DeviceXXXHandler extends DeviceHandler {
	private String deviceName;
	private String deviceSession;
	
	public DeviceXXXHandler(String deviceName) {
		this.deviceName = deviceName;
	}
	
	@Override
	public void handleRegistered(ChannelHandlerContext ctx) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		
		deviceSession = dsm.createDeviceSession(ctx);
		dsm.deviceRegistered(deviceName, deviceSession, ctx);
	}

	@Override
	public void handleUnregisered(ChannelHandlerContext ctx) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		//dsm.deviceUnregistered(deviceName, deviceSession);
		dsm.deviceUnregistered(deviceName, ctx);
	}

	@Override
	public void handleData(byte[] data, ChannelHandlerContext ctx) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		dsm.handleDeviceData(deviceName, deviceSession, data, ctx);
	}
}

