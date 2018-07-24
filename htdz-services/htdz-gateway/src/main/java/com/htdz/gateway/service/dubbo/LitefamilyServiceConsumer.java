package com.htdz.gateway.service.dubbo;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Reference;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.LitefamilyService;

@Component
public class LitefamilyServiceConsumer {
	@Reference(version = "1.0.0", timeout = 20000, check = false)
	private LitefamilyService litefamilyServiceProvider;

	public RPCResult hanleHttpRequest(RouteInfo ri, String path, Map<String, String> headers,
			Map<String, String[]> params, byte[] reqBody) {
		return litefamilyServiceProvider.hanleHttpRequest(ri, path, headers, params, reqBody);
	}
}
