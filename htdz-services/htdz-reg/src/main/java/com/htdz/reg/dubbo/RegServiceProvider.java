package com.htdz.reg.dubbo;

import java.lang.reflect.Method;
import java.util.Map;

import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;
import com.htdz.def.data.ApiResult;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.RegService;
import com.htdz.reg.service.ApiService;

@Service
@com.alibaba.dubbo.config.annotation.Service(interfaceClass = RegService.class, version = "1.0.0")
public class RegServiceProvider implements RegService {

	@Autowired
	GatewaySerivceConsumer gatewaySerivceConsumer;

	@Autowired
	private ApiService apiService;

	@Override
	public RPCResult hanleHttpRequest(RouteInfo ri, String path, Map<String, String> headers,
			Map<String, String[]> params, byte[] reqBody) {
		RPCResult result = RPCResult.success();

		String function = DataUtil.getStringFromMap(params, "function");
		if (!TextUtils.isEmpty(function)) {
			path = function;
		}

		String language = headers.get("accept-language");
		if (!TextUtils.isEmpty(language) && language.length() >= 5) {
			language.substring(0, 5);
		} else {
			language = "";
		}
		headers.put("accept-language", language);

		if (true) {

			Method invokeMethod;
			Object returnObject = new Object();
			try {
				invokeMethod = apiService.getClass().getMethod(path, Map.class, Map.class);
				// 调用业务逻辑方法
				returnObject = invokeMethod.invoke(apiService, params, headers);

			} catch (Exception e) {
				e.printStackTrace();
				LogManager.exception("hanleHttpRequest exception={}", e);
				return RPCResult.failed();
			}

			// JSON.toJSONString(apiService.getServerConnInfoByUserRetMap(params,
			// headers),
			result.setRpcResult(JSON.toJSONString(returnObject, SerializerFeature.WriteDateUseDateFormat,
					SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue,
					SerializerFeature.DisableCheckSpecialChar));
			return result;

		} else {
			result = gatewaySerivceConsumer.transferURLRequest(ri, "reg", path, headers, params, reqBody);
		}
		return result;
	}

	@Override
	public RPCResult hanleRegService(String method, Map<String, String> params) {

		LogManager.info("hanleRegService method={}, params={}, headers={}", method, params);

		if (method == null) {
			return RPCResult.failed();
		}

		try {
			Method invokeMethod = apiService.getClass().getMethod(method, Map.class);
			// 调用业务逻辑方法
			ApiResult returnObject = (ApiResult) invokeMethod.invoke(apiService, params);
			// 返回业务逻辑结果 json格式
			// outJson(response, returnObject,request);
			RPCResult result = new RPCResult();
			result.setRpcResult(JSON.toJSONString(returnObject, SerializerFeature.WriteDateUseDateFormat,
					SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue,
					SerializerFeature.DisableCheckSpecialChar));
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			LogManager.exception("hanleHttpRequest exception={}", e);
			return RPCResult.failed();
		}

	}

}
