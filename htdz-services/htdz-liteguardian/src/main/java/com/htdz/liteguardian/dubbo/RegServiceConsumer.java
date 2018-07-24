package com.htdz.liteguardian.dubbo;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Reference;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.RegService;

@Component
public class RegServiceConsumer implements RegService {
	@Reference(version = "1.0.0", timeout = 100000, check = false)
	private RegService regServiceProvider;

	@Override
	public RPCResult hanleHttpRequest(RouteInfo ri, String path, Map<String, String> headers,
			Map<String, String[]> params, byte[] reqBody) {
		return regServiceProvider.hanleHttpRequest(ri, path, headers, params, reqBody);
	}

	@Override
	public RPCResult hanleRegService(String method, Map<String, String> params) {
		return regServiceProvider.hanleRegService(method, params);

	};
}
