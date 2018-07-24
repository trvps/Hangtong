package com.htdz.gateway.controllers;

import java.io.UnsupportedEncodingException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.htdz.common.BaseController;
import com.htdz.common.LogManager;
import com.htdz.def.data.Errors;
import com.htdz.def.data.RPCResult;
import com.htdz.gateway.APIEngine;
import com.htdz.gateway.GatewayEngine;
import com.htdz.gateway.devices.DeviceServerManager;

@RestController
public class APIController extends BaseController {
	@Autowired
	private APIEngine apiEngine;

	@Autowired
	private Environment env;

	@RequestMapping("/hellogw")
	public String hellogw() {
		LogManager.debug("hellogw");

		String usage = "网关命令说明:																			<br>"
				+ "																						<br>"
				+ "服务转发 -------------------- /service/** 格式如: /service/servicename/path			<br>"
				+ "启动设备监听服务 ------------ /startdevice/{devicename}/{deviceport} 					<br>"
				+ "启动所有设备监听服务 -------- /startdevices 											<br>"
				+ "停止设备监听服务 ------------ /stopdevice/{devicename} 								<br>"
				+ "停止所有设备监听服务 -------- /stopdevices												<br>"
				+ "显示当前监听的设备服务 ------ /showdevices												<br>"
				+ "往设备推送消息 -------------- /push2device/{devicename}/{devicesession}/{message}		<br>" + "";

		return usage;
	}

	/**
	 * 转发
	 * 格式：/service/servicename/path...
	 */
	@RequestMapping("/service/**")
	public String serviceDispatch(HttpServletRequest request) {
		RPCResult result = apiEngine.serviceRequestDispatch(request);
		if (result.getRpcErrCode() == Errors.ERR_SUCCESS)
			return result.resultToString();
		else
			return Errors.failed();
	}

	/**
	 * 转发
	 */
	@RequestMapping("/WebApi2d/WebAPI")
	public void WebAPI(HttpServletRequest request, HttpServletResponse response) {
		String host = env.getProperty("webapi.address");
		String url = host + "/WebApi2d/WebAPI";
		apiEngine.serviceTransfer_HTTP(request, url, response);
	}

	/**
	 * 转发
	 */
	@RequestMapping("/WebApi2d/WebAPIVersion3")
	public void WebAPIVersion3(HttpServletRequest request, HttpServletResponse response) {
		String host = env.getProperty("webapi.address");
		String url = host + "/WebApi2d/WebAPIVersion3";
		apiEngine.serviceTransfer_HTTP(request, url, response);
	}

	/**
	 * 新
	 */
	@RequestMapping("/WebApi2d/WebAPIVersion3N")
	public String WebAPIVersionNew(HttpServletRequest request, HttpServletResponse response) {
		RPCResult result = apiEngine.serviceRequestDispatch(request);
		if (result.getRpcErrCode() == Errors.ERR_SUCCESS)
			return result.resultToString();
		else
			return Errors.failed();
	}

	/**
	 * 转发
	 */
	@RequestMapping("/SyncData/RemotingAPI")
	public void RemotingAPI(HttpServletRequest request, HttpServletResponse response) {
		String host = env.getProperty("webapi.address");
		String url = host + "/SyncData/RemotingAPI";
		apiEngine.serviceTransfer_HTTP(request, url, response);
	}

	/**
	 * 新
	 */
	@RequestMapping("/SyncData/RemotingAPIN")
	public String RemotingAPINew(HttpServletRequest request, HttpServletResponse response) {
		RPCResult result = apiEngine.serviceRequestDispatch(request);
		if (result.getRpcErrCode() == Errors.ERR_SUCCESS)
			return result.resultToString();
		else
			return Errors.failed();
	}

	/**
	 * 头像上传路径
	 * 转发
	 */
	@RequestMapping("/data/HeadPortrait")
	// @RequestMapping("/WebApi2d/WebAPIVersion3")
	public void dataHeadPortrait(HttpServletRequest request, HttpServletResponse response) {
		String host = env.getProperty("webapi.address");
		String url = host + "/data/HeadPortrait";
		apiEngine.serviceTransfer_HTTP_Upload(request, url, response);
	}

	/**
	 * 动态启动设备监听服务
	 * @param devicename
	 * @param deviceport
	 * @param request
	 * @return
	 */
	@RequestMapping("/startdevice/{devicename}/{deviceport}")
	public String startDeviceServer(@PathVariable("devicename") String devicename,
			@PathVariable("deviceport") Integer deviceport, HttpServletRequest request) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		String ip = "127.0.0.1";
		boolean result = dsm.startDeviceServer(ip, deviceport, devicename);
		return result ? Errors.success() : Errors.failed();
	}

	/**
	 * 启动所有设备监听服务
	 * @param request
	 * @return
	 */
	@RequestMapping("/startdevices")
	public String startDeviceServers(HttpServletRequest request) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		dsm.startDeviceServer();
		return Errors.success();
	}

	/**
	 * 动态关闭设备监听服务
	 * @param devicename
	 * @param request
	 * @return
	 */
	@RequestMapping("/stopdevice/{devicename}")
	public String stopDeviceServer(@PathVariable("devicename") String devicename, HttpServletRequest request) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		boolean result = dsm.stopDeviceServer(devicename);
		return result ? Errors.success() : Errors.failed();
	}

	/**
	 * 停止所有设备监听服务
	 * @param request
	 * @return
	 */
	@RequestMapping("/stopdevices")
	public String stopDeviceServers(HttpServletRequest request) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		dsm.stopDeviceServer();
		return Errors.success();
	}

	/**
	 * 显示当前监听的设备服务
	 * 设备名= 监听端口= 当前连接设备数量=
	 * 
	 */
	@RequestMapping("/showdevices")
	public String showDeviceServers(HttpServletRequest request) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		return dsm.showDeviceServers();
	}

	/**
	 * 往设备推送消息
	 * @param deviceName
	 * @param deviceSession
	 * @param message
	 * @param request
	 * @return
	 */
	@RequestMapping("/push2device/{devicename}/{devicesession}/{message}")
	public String push2device(@PathVariable("devicename") String deviceName,
			@PathVariable("devicesession") String deviceSession, @PathVariable("message") String message,
			HttpServletRequest request) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		try {
			dsm.pushMessageToDevice(deviceName, deviceSession, message.getBytes("utf-8"));
		} catch (UnsupportedEncodingException e) {
			LogManager.exception(e.getMessage(), e);
		}
		return Errors.success();
	}

	@RequestMapping("/push2device/{devicename}/{devicesession}")
	public String push2devicepost(@PathVariable("devicename") String deviceName,
			@PathVariable("devicesession") String deviceSession, @RequestBody String body, HttpServletRequest request) {
		DeviceServerManager dsm = GatewayEngine.getGatewayEngine().getDeviceServerManager();
		try {
			int resultCount = dsm.pushMessageToDevice(deviceName, deviceSession, body.getBytes("utf-8"));
			if (resultCount == 0)
				return Errors.failed();
		} catch (UnsupportedEncodingException e) {
			LogManager.exception(e.getMessage(), e);
		}

		return Errors.success();
	}

	// 重点注意
	// 本接口是为了中转新加坡的图片获取请求
	// 后续新加坡服务取消商用环境时，需把本接口删除
	// 打包新加坡网关时，本接口放开
	// 打包其他网关时，本接口注释
	// @RequestMapping("/image/**")
	// public void transferImageDownload(HttpServletRequest request,
	// HttpServletResponse response) {
	// // /image/HeadPortrait/201807040953418505.png
	// String url = "http://hkgw.litguardian.com:10000" +
	// request.getServletPath();
	// apiEngine.serviceTransfer_HTTP_Download(request, url, response);
	// }
}
