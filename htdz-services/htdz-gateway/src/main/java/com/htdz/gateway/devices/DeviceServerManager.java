package com.htdz.gateway.devices;


import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;
import com.htdz.common.utils.SpringContextUtil;
import com.htdz.def.data.Errors;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.gateway.APIEngine;
import com.htdz.gateway.GatewayEngine;
import com.htdz.gateway.SafeEngine;

import io.netty.channel.ChannelHandlerContext;


@Service
public class DeviceServerManager {
	@Autowired  
	private Environment env;
	
	@Autowired
	private SafeEngine safeEngine;
	
	private String[] deviceList;
	
	// 设备服务映射 <设备名, NettyTcpServer>
	private LinkedHashMap<String, NettyTcpServer> ntServerMap = null;
	
	// 远端设备映射 <设备名, <设备ID, ChannelHandlerContext>>
	private ConcurrentHashMap<String, ConcurrentHashMap<String, ChannelHandlerContext>> deviceRegisterdMap = null;
	
	private AtomicInteger atomicInteger = new AtomicInteger(0);
	
	private Random random = new Random();
	private ExecutorService executor = Executors.newFixedThreadPool(30);
	
	public static DeviceServerManager getDeviceServerManager() {
		return (DeviceServerManager)SpringContextUtil.getBean(DeviceServerManager.class);
	}
	
	public DeviceServerManager() {
		ntServerMap = new LinkedHashMap<String, NettyTcpServer>();
		deviceRegisterdMap = new ConcurrentHashMap<String, ConcurrentHashMap<String, ChannelHandlerContext>>(5);
	}
	
	public String createDeviceSession(ChannelHandlerContext ctx) {
		// 会话格式
		// 本地网关名-ip-port-创建顺序-4位随机数
		InetSocketAddress socketaddress = (InetSocketAddress)ctx.channel().remoteAddress();
		String ip = socketaddress.getAddress().getHostAddress();
		int port = socketaddress.getPort();
		
		StringBuilder sb = new StringBuilder();
		sb.append(env.getProperty("htdz.serverlocal"));
		sb.append("-");
		sb.append(ip);
		sb.append("-");
		sb.append(port);
		sb.append("-");
		sb.append(atomicInteger.getAndIncrement());
		sb.append("-");
		
		String srandom = "0000" + random.nextInt(10000);
		sb.append(srandom.substring(srandom.length()-4));
		
		if (atomicInteger.get() < 0)
			atomicInteger.set(0);
		
		return sb.toString();
	}
	
	/**
	 * 启动设备监听服务
	 */
	public void startDeviceServer() {
		LogManager.info("startDeviceServer......");
		
		String devicelist = env.getProperty("netty.device.devicelist");
		deviceList = devicelist.split(",");
		
		for (int i=0; i<deviceList.length; i++) {
			startDeviceServer(deviceList[i]);
		}
	}
	
	/**
	 * 停止设备监听服务
	 */
	public void stopDeviceServer() {
		for (Entry<String, NettyTcpServer> entry : ntServerMap.entrySet()) {
			entry.getValue().stop();
		}
		
		ntServerMap.clear();
		deviceRegisterdMap.clear();
	}
	
	/**
	 * 启动设备监听服务
	 * @param devicename
	 * @return
	 */
	public boolean startDeviceServer(String devicename) {
		String host = env.getProperty("netty.device.host");
		String strport = env.getProperty("netty.device." + devicename + ".port");
		int port = DataUtil.stringToInt(strport, 0);
		return startDeviceServer(host, port, devicename);
	}
	
	/**
	 * 启动设备监听服务
	 * @param devicename
	 * @return
	 */
	public boolean startDeviceServer(String ip, int port, String devicename) {
		if (stopDeviceServer(devicename)) {
			// 暂停200毫秒, 等待停止后再启动
			try {
				TimeUnit.MILLISECONDS.sleep(200);
			} catch (Exception e) {
				LogManager.exception(e.getMessage(), e);
			}
		}
		
		ConcurrentHashMap<String, ChannelHandlerContext> map = deviceRegisterdMap.get(devicename);
		if (map == null)
			deviceRegisterdMap.put(devicename, new ConcurrentHashMap<String, ChannelHandlerContext>(5));
		
		if (port > 0) {
			int rwIdelSeconds = DataUtil.stringToInt(env.getProperty("netty.device.rwidel"), 30*60);
			
			NettyTcpServer ntServer = new NettyTcpServer(ip, port, devicename, rwIdelSeconds);
			ntServer.start();
			
			ntServerMap.put(devicename, ntServer);
			return true;
		} else {
			LogManager.info("start " + devicename + " server failed port=0");
		}
		
		return false;
	}
	
	/**
	 * 根据设备名停止设备监听服务
	 * @param devicename
	 * @return
	 */
	public boolean stopDeviceServer(String devicename) {
		NettyTcpServer ntServer = ntServerMap.remove(devicename);
		if (ntServer != null) {
			LogManager.info("正在停止{}设备监听服务", devicename);
			ntServer.stop();
			return true;
		}
		
		return false;
	}
	
	/**
	 * 根据端口号停止设备监听服务
	 * @param port
	 * @return
	 */
	public boolean stopDeviceServer(int port) {
		NettyTcpServer ntServer = getNettyTcpServerByPort(port);
		if (ntServer != null) {
			ntServer.stop();
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * 根据设备名判断设备监听服务是否启动
	 * @param devicename
	 * @return
	 */
	public boolean isDeviceServerStart(String devicename) {
		NettyTcpServer ntServer = ntServerMap.get(devicename); 
		return ntServer != null ? true : false;
	}
	
	
	/**
	 * 根据端口号判断设备监听服务是否启动
	 * @param devicename
	 * @return
	 */
	public boolean isDeviceServerStart(int port) {
		return getNettyTcpServerByPort(port) != null ? true : false;
	}
	
	/**
	 * 根据端口号获取设备监听服务
	 * @param port
	 * @return
	 */
	private NettyTcpServer getNettyTcpServerByPort(int port) {
		Iterator<Entry<String, NettyTcpServer>> iterator = ntServerMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, NettyTcpServer> entry = iterator.next();
			NettyTcpServer ntServer = entry.getValue();
			if (ntServer.getPort() == port)
				return ntServer;
		}
		
		return null;
	}
	
	/**
	 * 显示当前监听的设备服务
	 * @return
	 */
	public String showDeviceServers() {
		if (ntServerMap.size() == 0)
			return "当前没有设备监听服务";
		
		StringBuilder sb = new StringBuilder();
		
		Iterator<Entry<String, NettyTcpServer>> iterator = ntServerMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, NettyTcpServer> entry = iterator.next();
			// 设备名 监听端口 当前连接设备数量
			sb.append("设备名=" + entry.getKey());
			sb.append(" ");
			sb.append("监听端口=" + entry.getValue().getPort());
			sb.append(" ");
			sb.append("当前连接设备数量=" + deviceRegisterdMap.get(entry.getKey()).size());
			sb.append("<br> ");
		}
		
		return sb.toString();
	}
	
	
	/**
	 * 设备上线
	 * @param deviceName
	 * @param deviceSession
	 * @param ctx
	 */
	public void deviceRegistered(String deviceName, String deviceSession, ChannelHandlerContext ctx) {
		// 检测设备是否攻击过
		InetSocketAddress socketaddress = (InetSocketAddress)ctx.channel().remoteAddress();
		String ip = socketaddress.getAddress().getHostAddress();
		if (safeEngine.deviceAttacked(ip)) {
			LogManager.info("[设备恶意攻击，拒绝服务] deviceName={}, ip={}", deviceName, ip);
			ctx.close();
			return ;
		}
		
		LogManager.info("[设备上线] deviceName={}, deviceSession={}", deviceName, deviceSession);

		ConcurrentHashMap<String, ChannelHandlerContext> map = deviceRegisterdMap.get(deviceName);
		
		int maxConnect = DataUtil.stringToInt(env.getProperty("netty.device."+deviceName+".maxConnect"), 0);
		if (maxConnect > 0 && map.size() >= maxConnect) {
			LogManager.info("[设备连接达到最大限制] deviceName={} maxConnect={}, curConnect={}", 
					deviceName, maxConnect, map.size());
			ctx.close();
			return ;
		}
		
		map.put(deviceSession, ctx);
		
		// 异步调用
		executor.execute(new Runnable() {
			public void run() {
				RouteInfo ri = RouteInfo.build(GatewayEngine.getServerLocal());
				APIEngine apiEngine = GatewayEngine.getGatewayEngine().getAPIEngine();
				apiEngine.handleDeviceRegisted(ri, deviceName, deviceSession);
			}
		});
	}
	
	/**
	 * 设备已下线
	 * @param deviceName
	 * @param deviceSession
	 */
	public void deviceUnregistered(String deviceName, String deviceSession) {
		LogManager.info("[设备下线] deviceName={}, deviceSession={}", deviceName, deviceSession);
		
		ConcurrentHashMap<String, ChannelHandlerContext> map = deviceRegisterdMap.get(deviceName);
		map.remove(deviceSession);

		// 异步调用
		executor.execute(new Runnable() {
			public void run() {
				RouteInfo ri = RouteInfo.build(GatewayEngine.getServerLocal());
				APIEngine apiEngine = GatewayEngine.getGatewayEngine().getAPIEngine();
				apiEngine.handleDeviceUnregisted(ri, deviceName, deviceSession);
			}
		});
	}
	
	/**
	 * 设备已下线
	 * @param deviceName
	 * @param ctx
	 */
	public void deviceUnregistered(String deviceName, ChannelHandlerContext ctx) {
		String deviceSession = "";
		ConcurrentHashMap<String, ChannelHandlerContext> map = deviceRegisterdMap.get(deviceName);
		Iterator<Entry<String, ChannelHandlerContext>> it = map.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ChannelHandlerContext> entry = it.next();
			if (entry.getValue() == ctx) {
				deviceSession = entry.getKey();
				it.remove();
				
				break;
			}
		}
		
		LogManager.info("[设备下线] deviceName={}, deviceSession={}", deviceName, deviceSession);
		
		// 异步调用
		final String tmpdeviceSession = deviceSession;
		executor.execute(new Runnable() {
			public void run() {
				RouteInfo ri = RouteInfo.build(GatewayEngine.getServerLocal());
				APIEngine apiEngine = GatewayEngine.getGatewayEngine().getAPIEngine();
				apiEngine.handleDeviceUnregisted(ri, deviceName, tmpdeviceSession);
			}
		});
	}
	
	/**
	 * 转发设备数据
	 * @param deviceName
	 * @param deviceSession
	 * @param data
	 */
	public void handleDeviceData(String deviceName, String deviceSession, byte[] data, ChannelHandlerContext ctx) {
		// 加入防止设备恶意攻击检测
		InetSocketAddress socketaddress = (InetSocketAddress)ctx.channel().remoteAddress();
		String ip = socketaddress.getAddress().getHostAddress();
		int port = socketaddress.getPort();
		safeEngine.addToAttackDetect(ip, port, data, "Device"+","+deviceName+","+deviceSession);
		
		// 异步调用
		executor.execute(new Runnable() {
			public void run() {
				try {
					RouteInfo ri = RouteInfo.build(GatewayEngine.getServerLocal());
					APIEngine apiEngine = GatewayEngine.getGatewayEngine().getAPIEngine();
					RPCResult result = apiEngine.deviceRequestDispatch(ri, deviceName, deviceSession, data);
					if (result.getRpcErrCode() == Errors.ERR_SUCCESS && result.getRpcResult() != null) {
						byte[] respBytes = (byte[])result.resultToBytes();
						if (respBytes != null)
							DeviceHandler.write(respBytes, ctx);
					} else if (result.getRpcErrCode() == Errors.ERR_SERVICEREFUSE) {
						// 关闭连接
						ctx.close();
					}
				} catch (Exception e) {
					LogManager.exception(e.getMessage(), e);
				}
			}
		});
	}
	
	/**
	 * 设备是否在线
	 * @param deviceName
	 * @param deviceSession
	 * @return
	 */
	public boolean deviceIsResistered(String deviceName, String deviceSession) {
		return getDeviceChannelHandlerContext(deviceName, deviceSession) != null ? true : false;
	}
	
	/**
	 * 获取指定类型设备连接数量
	 * @param deviceName
	 * @return
	 */
	public Integer deviceOnlineCount(String deviceName) {
		int count = 0;
		// 如果没有传入设备类型，则遍历
		if (deviceName == null || deviceName.length()==0) {
			for (Entry<String, ConcurrentHashMap<String, ChannelHandlerContext>> entry : deviceRegisterdMap.entrySet()) {
				count += entry.getValue().size();
			}
		} else {
			ConcurrentHashMap<String, ChannelHandlerContext> map = deviceRegisterdMap.get(deviceName);
			if (map != null)
				count = map.size();
		}
		
		return count;
	}
	
	/**
	 * 获取设备连接的Context
	 * @param deviceName
	 * @param deviceSession
	 * @return
	 */
	public ChannelHandlerContext getDeviceChannelHandlerContext(String deviceName, String deviceSession) {
		// 如果没有传入设备类型，则遍历
		if (deviceName == null || deviceName.length()==0) {
			for (Entry<String, ConcurrentHashMap<String, ChannelHandlerContext>> entry : deviceRegisterdMap.entrySet()) {
				ChannelHandlerContext chContext = entry.getValue().get(deviceSession);
				if (chContext != null) {
					return chContext;
				}
			}
		} else {
			ConcurrentHashMap<String, ChannelHandlerContext> map = deviceRegisterdMap.get(deviceName);
			if (map != null)
				return map.get(deviceSession);
		}
		
		return null;
	}
	
	/**
	 * 
	 * @param deviceName
	 * @param deviceSession		多个设备用,分割如deviceSession1,deviceSession2
	 * @param data
	 */
	public int pushMessageToDevice(String deviceName, String deviceSession, byte[] data) {
		try {
			LogManager.info("往设备推送消息 {} {} {}", deviceName, deviceSession, DataUtil.bytesToString(data));
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
		
		int resultCount = 0;
		
		// 往deviceName所有设备推送消息，此功能需要谨慎开放
//		if ((deviceSession == null || deviceSession.length() == 0) 
//				&& (deviceName != null && deviceName.length() != 0)) {
//			ConcurrentHashMap<String, ChannelHandlerContext> map = deviceRegisterdMap.get(deviceName);
//			if (map != null && map.size() > 0) {
//				for (Entry<String, ChannelHandlerContext> entry : map.entrySet()) {
//					ChannelHandlerContext ctx = entry.getValue();
//					if (ctx != null) {
//						try {
//							DeviceHandler.write(data, ctx);
//							resultCount++;
//						} catch (Exception e) {
//							LogManager.exception(e.getMessage(), e);
//						}
//					}
//				}
//			}
//		
//		} else {
			String[] deviceSessions = deviceSession.split(",");
			for (int i=0; i<deviceSessions.length; i++ ) {
				ChannelHandlerContext ctx = getDeviceChannelHandlerContext(deviceName, deviceSessions[i]);
				if (ctx != null) {
					try {
						DeviceHandler.write(data, ctx);
						resultCount++;
					} catch (Exception e) {
						LogManager.exception(e.getMessage(), e);
					}
				}
			}
//		}
		
		LogManager.info("成功推送: {}", resultCount);
		return resultCount;
	}
	
	/**
	 * 关闭设备
	 * @param deviceName
	 * @param deviceSession
	 * @return
	 */
	public int closeDevice(String deviceName, String deviceSession) {
		try {
			LogManager.info("关闭设备 {} {}", deviceName, deviceSession);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
		
		int resultCount = 0;
		
		// 关闭所有设备，此功能需要谨慎开放
//		if ((deviceSession == null || deviceSession.length() == 0) 
//				&& (deviceName != null && deviceName.length() != 0)) {
//			ConcurrentHashMap<String, ChannelHandlerContext> map = deviceRegisterdMap.get(deviceName);
//			if (map != null && map.size() > 0) {
//				for (Entry<String, ChannelHandlerContext> entry : map.entrySet()) {
//					ChannelHandlerContext ctx = entry.getValue();
//					if (ctx != null) {
//						try {
//							ctx.close();
//							resultCount++;
//						} catch (Exception e) {
//							LogManager.exception(e.getMessage(), e);
//						}
//					}
//				}
//			}
//		
//		} else {
			String[] deviceSessions = deviceSession.split(",");
			for (int i=0; i<deviceSessions.length; i++ ) {
				ChannelHandlerContext ctx = getDeviceChannelHandlerContext(deviceName, deviceSessions[i]);
				if (ctx != null) {
					try {
						ctx.close();
						resultCount++;
					} catch (Exception e) {
						LogManager.exception(e.getMessage(), e);
					}
				}
			}
//		}
		
		LogManager.info("成功关闭设备: {}", resultCount);
		return resultCount;
	}
}

