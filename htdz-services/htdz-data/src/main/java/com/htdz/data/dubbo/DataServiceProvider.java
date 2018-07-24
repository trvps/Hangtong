package com.htdz.data.dubbo;


import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.alibaba.dubbo.rpc.RpcContext;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.DataService;


@Service
@com.alibaba.dubbo.config.annotation.Service(interfaceClass = DataService.class, version="1.0.0")
public class DataServiceProvider implements DataService {
	@Value("${spring.dubbo.service.auth.iplist}")
	private String service_auth_iplist;
	
	@Override
	public RPCResult hanleHttpRequest(RouteInfo ri, 
										String path, 
										Map<String, String> headers, 
										Map<String, String[]> params, 
										byte[] reqBody) {
		if (service_auth_iplist != null 
			&& service_auth_iplist.length() > 0 
			&& !service_auth_iplist.trim().equals("*")) {
			// 需要鉴权
			String remotehost = RpcContext.getContext().getRemoteHost();
			if (service_auth_iplist.indexOf(remotehost) == -1) {
				return RPCResult.ipAuthFailed();
			}
		}
		
		return RPCResult.success();
	}
}
