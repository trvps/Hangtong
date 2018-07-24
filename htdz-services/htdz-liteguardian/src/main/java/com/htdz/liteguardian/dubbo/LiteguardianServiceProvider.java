package com.htdz.liteguardian.dubbo;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.htdz.common.LanguageManager;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.def.interfaces.LiteguardianService;
import com.htdz.liteguardian.service.ApiService;
import com.htdz.liteguardian.util.PropertyUtil;
import com.htdz.liteguardian.util.ReturnObject;
import com.htdz.liteguardian.util.Session;

@Service
@com.alibaba.dubbo.config.annotation.Service(interfaceClass = LiteguardianService.class, version = "1.0.0", timeout = 200000)
public class LiteguardianServiceProvider implements LiteguardianService {

	@Autowired
	GatewaySerivceConsumer gatewaySerivceConsumer;

	@Autowired
	private ApiService apiService;

	@Override
	public RPCResult hanleHttpRequest(RouteInfo ri, String method, Map<String, String> headers,
			Map<String, String[]> params, byte[] reqBody) {
		RPCResult result = new RPCResult();
		Map<String, Object> returnObject = new HashMap<String, Object>();
		boolean loginPast = true;

		String function = DataUtil.getStringFromMap(params, "function");
		if (!TextUtils.isEmpty(function)) {
			method = function;
		}
		LogManager.info("hanleRegService method {}", method);

		String language = headers.get("accept-language");
		if (language.length() >= 5) {
			language.substring(0, 5);
		} else {
			language = "";
		}
		headers.put("accept-language", language);

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		try {

			if (!function.equals("userLogin") && !function.equals("getServerNo") && !function.equals("register")
					&& !function.equals("verifyemail") && !function.equals("forgetPassword")
					&& !function.equals("forgotpasswordhand") && !function.equals("saveusertoken")
					&& !function.equals("getlostcardforhtml") && !function.equals("userLoginCN")
					&& !function.equals("registerCN") && !function.equals("forgetPasswordCN")
					&& !function.equals("unbound") && !function.equals("setHumanInsurance")
					&& !function.equals("getPublicKey")) {

				if (!PropertyUtil.isNotBlank(username)) {
					loginPast = false;
					returnObject.put(ReturnObject.RETURN_CODE, "1");
					returnObject.put(ReturnObject.RETURN_OBJECT, null);
					returnObject.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg("common.pls_login"));
				} else {
					if (!Session.getsessionID(username).equals(sessionID)) {
						loginPast = false;
						returnObject.put(ReturnObject.RETURN_CODE, "2");
						returnObject.put(ReturnObject.RETURN_OBJECT, null);
						returnObject.put(ReturnObject.RETURN_WHAT,
								LanguageManager.getMsg("common.account_login_other_place"));

					}
				}
			}

			if (loginPast) {
				Method invokeMethod = apiService.getClass().getMethod(method, Map.class, Map.class, byte[].class);
				// 调用业务逻辑方法
				returnObject = (Map<String, Object>) invokeMethod.invoke(apiService, params, headers, reqBody);

			}
		} catch (Exception e) {
			returnObject.put(ReturnObject.RETURN_CODE, "0");
			returnObject.put(ReturnObject.RETURN_OBJECT, "");
			returnObject.put(ReturnObject.RETURN_WHAT, "exception!");
			e.printStackTrace();
			LogManager.exception("hanleHttpRequest exception={}", e);
		}

		result.setRpcResult(JSON.toJSONString(returnObject, SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return result;

	}

}
