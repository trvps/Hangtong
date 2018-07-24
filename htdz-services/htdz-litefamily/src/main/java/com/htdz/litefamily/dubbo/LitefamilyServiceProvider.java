package com.htdz.litefamily.dubbo;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.htdz.common.LogManager;
import com.htdz.def.data.ApiResult;
import com.htdz.def.data.Errors;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.LitefamilyService;
import com.htdz.litefamily.service.ApiService;

@Service
@com.alibaba.dubbo.config.annotation.Service(interfaceClass = LitefamilyService.class, version = "1.0.0")
public class LitefamilyServiceProvider implements LitefamilyService {
	@Autowired
	private ApiService apiService;

	@Override
	public RPCResult hanleHttpRequest(RouteInfo ri, String method, Map<String, String> headers,
			Map<String, String[]> params, byte[] reqBody) {

		RPCResult result = new RPCResult();
		LogManager.info("hanleHttpRequest ri={}, method={}, headers={}", ri, method, headers);

		if (method == null) {
			return RPCResult.failed();
		}

		// 供反射调用的参数
		ApiResult returnObject = new ApiResult();
		try {
			Method invokeMethod = apiService.getClass().getMethod(method, Map.class);
			// 调用业务逻辑方法
			returnObject = (ApiResult) invokeMethod.invoke(apiService, params);
			// 返回业务逻辑结果 json格式
			// outJson(response, returnObject,request);

			result.setRpcResult(JSON.toJSONString(returnObject, SerializerFeature.WriteDateUseDateFormat,
					SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue));

		} catch (Exception e) {
			e.printStackTrace();
			returnObject.setCode(Errors.ERR_FAILED);
			returnObject.setMsg("exception!");
			result.setRpcResult(JSON.toJSONString(returnObject, SerializerFeature.WriteDateUseDateFormat,
					SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue));
			LogManager.exception("hanleHttpRequest exception={}", e);
		}

		return result;
	}

}
