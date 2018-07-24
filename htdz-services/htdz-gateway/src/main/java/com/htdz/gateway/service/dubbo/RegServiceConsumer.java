package com.htdz.gateway.service.dubbo;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Reference;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.RegService;

@Component
public class RegServiceConsumer {
	@Reference(version = "1.0.0", timeout = 20000, check = false)
	private RegService regServiceProvider;

	public RPCResult hanleHttpRequest(RouteInfo ri, String path, Map<String, String> headers,
			Map<String, String[]> params, byte[] reqBody) {
		return regServiceProvider.hanleHttpRequest(ri, path, headers, params, reqBody);
	}

	public RPCResult hanleRegService(String method, Map<String, String> params) {
		return regServiceProvider.hanleRegService(method, params);

	};
}
