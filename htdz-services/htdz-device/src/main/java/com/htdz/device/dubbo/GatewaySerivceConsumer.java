package com.htdz.device.dubbo;


import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;
import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.htdz.common.LogManager;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.GatewaySerivce;


@Component
public class GatewaySerivceConsumer implements ApplicationListener<ContextRefreshedEvent> {
	/*
	 	random=com.alibaba.dubbo.rpc.cluster.loadbalance.RandomLoadBalance
		roundrobin=com.alibaba.dubbo.rpc.cluster.loadbalance.RoundRobinLoadBalance
		leastactive=com.alibaba.dubbo.rpc.cluster.loadbalance.LeastActiveLoadBalance
		consistenthash=com.alibaba.dubbo.rpc.cluster.loadbalance.ConsistentHashLoadBalance
		devicelb=com.alibaba.dubbo.rpc.cluster.loadbalance.DeviceServiceLoadBalance
	 */
	@Reference(version="1.0.0", check=false, loadbalance = "roundrobin")
	private GatewaySerivce gatewaySerivce_Local;
	private GatewaySerivce gatewaySerivce_transfer;
	
	@Value("${htdz.serverlocal}")
	private String htdz_serverlocal;
	
	@Value("${spring.dubbo.appname}")
	private String dubbo_application_name;
	
	@Value("${spring.dubbo.timeout}")
	private Integer spring_dubbo_timeout;
	
	@Value("${spring.dubbo.retries}")
	private Integer spring_dubbo_retries;
	
	@Value("${spring.dubbo.service.gateway.transfer.name}")
	private String dubbo_service_gateway_transfer_name;
	
	@Value("${spring.dubbo.service.gateway.transfer.url}")
	private String dubbo_service_gateway_transfer_url;
	
	
	
	public Integer deviceOnlineCount(boolean local, String deviceName) {
		if (local)
			return gatewaySerivce_Local.deviceOnlineCount(deviceName);
		else
			return gatewaySerivce_transfer.deviceOnlineCount(deviceName);
	}

	
	public Boolean isDeviceOnline(boolean local, String deviceName, String deviceSession) {
		if (local) {
			//return gatewaySerivce_Local.isDeviceOnline(deviceName, deviceSession);
			Boolean result = gatewaySerivce_Local.isDeviceOnline(deviceName, deviceSession);
			// 临时处理一下香港服务器和新加坡服务器两个网关的情况
			if (result) {
				return result;
			}

			int trycount = 10;
			for (int i=0; i<trycount; i++) {
				result = gatewaySerivce_Local.isDeviceOnline(deviceName, deviceSession);
				if (result) {
					return result;
				}
			}
				
			return result;
		} else {
			return gatewaySerivce_transfer.isDeviceOnline(deviceName, deviceSession);
		}
	}
	
	
	public RPCResult pushMessageToDevice(boolean local, String deviceName, String deviceSession, byte[] data) {
		try {
			if (local) {
				// return gatewaySerivce_Local.pushMessageToDevice(deviceName, deviceSession, data);
				RPCResult result = gatewaySerivce_Local.pushMessageToDevice(deviceName, deviceSession, data);
				// 临时处理一下香港服务器和新加坡服务器两个网关的情况
				int count = (Integer)result.getRpcResult();
				if (count != 0) {
					return result;
				}

				int trycount = 10;
				for (int i=0; i<trycount; i++) {
					result = gatewaySerivce_Local.pushMessageToDevice(deviceName, deviceSession, data);
					count = (Integer)result.getRpcResult();
					if (count != 0) {
						return result;
					}
				}
					
				return result;
			} else {
				return gatewaySerivce_transfer.pushMessageToDevice(deviceName, deviceSession, data);
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
		
		return RPCResult.failed();
	}
	
	public RPCResult closeDevice(boolean local, String deviceName, String deviceSession) {
		try {
			if (local) {
				//return gatewaySerivce_Local.closeDevice(deviceName, deviceSession);
				RPCResult result = gatewaySerivce_Local.closeDevice(deviceName, deviceSession);
				// 临时处理一下香港服务器和新加坡服务器两个网关的情况
				int count = (Integer)result.getRpcResult();
				if (count != 0) {
					return result;
				}

				int trycount = 10;
				for (int i=0; i<trycount; i++) {
					result = gatewaySerivce_Local.closeDevice(deviceName, deviceSession);
					count = (Integer)result.getRpcResult();
					if (count != 0) {
						return result;
					}
				}
					
				return result;
			} else {
				return gatewaySerivce_transfer.closeDevice(deviceName, deviceSession);
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
		
		return RPCResult.failed();
	}


	public RPCResult transferDeviceRequest(RouteInfo ri, String deviceName, String deviceSession, byte[] data) {
		try {
			if (ri == null) {
				ri = RouteInfo.build(htdz_serverlocal);
			} else {
				// 如果将要路由的目的地已经在路由信息里面，则终止路由，返回失败
				if (ri.isInRoute(dubbo_service_gateway_transfer_name))
					return RPCResult.failed();
				
				if (ri.ttl() <= 0) {
					// 中转次数太多
					return RPCResult.failed();
				}
				
				// 如果本地域不在路由链表里，则添加
				if (!ri.isInRoute(htdz_serverlocal)) {
					ri.addRouteInfo(htdz_serverlocal);
				}
			}
			
			return gatewaySerivce_transfer.transferDeviceRequest(ri, deviceName, deviceSession, data);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return RPCResult.failed();
	}

	public RPCResult transferURLRequest(RouteInfo ri, 
											String serviceName, 
											String path,
											Map<String, String> headers, 
											Map<String, String[]> params, 
											byte[] reqBody) {
		try {
			if (ri == null) {
				ri = RouteInfo.build(htdz_serverlocal);
			} else {
				// 如果将要路由的目的地已经在路由信息里面，则终止路由，返回失败
				if (ri.isInRoute(dubbo_service_gateway_transfer_name))
					return RPCResult.failed();
				
				if (ri.ttl() <= 0) {
					// 中转次数太多
					return RPCResult.failed();
				}
				
				// 如果本地域不在路由链表里，则添加
				if (!ri.isInRoute(htdz_serverlocal)) {
					ri.addRouteInfo(htdz_serverlocal);
				}
			}
			
			return gatewaySerivce_transfer.transferURLRequest(ri, serviceName, path, headers, params, reqBody);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return RPCResult.failed();
	}

    public ReferenceBean<GatewaySerivce> gatewaySerivceConsumer() {
    	ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName(dubbo_application_name);
        
        ReferenceBean<GatewaySerivce> refBean = new ReferenceBean<>();
        refBean.setApplication(applicationConfig);
        refBean.setVersion("1.0.0");
        refBean.setInterface(GatewaySerivce.class);
        refBean.setTimeout(spring_dubbo_timeout);
        refBean.setRetries(spring_dubbo_retries);
        refBean.setCheck(false);
        refBean.setUrl(dubbo_service_gateway_transfer_url);
        
        
        
        return refBean;
    }

	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		gatewaySerivce_transfer = gatewaySerivceConsumer().get();
	}
}

