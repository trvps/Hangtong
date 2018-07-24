package com.htdz.gateway.service.dubbo;


import java.util.Map;
import org.springframework.stereotype.Component;
import com.alibaba.dubbo.config.annotation.Reference;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.DataService;


@Component
public class DataServiceConsumer implements DataService {
	@Reference(version="1.0.0", check=false)
	private DataService dataServiceProvider;
	

	@Override
	public RPCResult hanleHttpRequest(RouteInfo ri, 
										String path, 
										Map<String, String> headers, 
										Map<String, String[]> params, 
										byte[] reqBody) {
		return dataServiceProvider.hanleHttpRequest(ri, path, headers, params, reqBody);
	}
}
