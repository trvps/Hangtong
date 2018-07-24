package com.htdz.task.dubbo;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.htdz.common.LogManager;
import com.htdz.common.utils.HttpUtils;
import com.htdz.def.data.Errors;
import com.htdz.def.data.RPCResult;
import com.htdz.def.dbmodel.ChatToken;
import com.htdz.def.interfaces.TaskService;
import com.htdz.def.view.GpsAddress;
import com.htdz.def.view.GpsAreaInfo;
import com.htdz.def.view.PushInfo;
import com.htdz.def.view.UserConn;
import com.htdz.def.view.VoiceMsg;
import com.htdz.gpsmessage.GpsMessage;
import com.htdz.task.service.PushService;
import com.htdz.task.service.RailService;
import com.htdz.task.service.gpsService;

import io.rong.RongCloud;
import io.rong.models.TokenResult;

@Service
@com.alibaba.dubbo.config.annotation.Service(interfaceClass = TaskService.class, version = "1.0.0",timeout = 20000)
public class TaskServiceProvider implements TaskService {
	@Autowired
	private RailService railEngine;
	@Autowired
	private PushService pushService;
	@Value("${appKey}")
	private String appKey;
	@Value("${appSecret}")
	private String appSecret;
	@Value("${amap.api}")
	private String amapApi;
	@Value("${amap.key}")
	private String amapKey;

	@Override
	public RPCResult regionAnalysis(GpsAreaInfo gpsAreaInfoView) {
		railEngine.putTgpsData(gpsAreaInfoView);
		return RPCResult.success();
	}

	@Override
	public RPCResult pushMsg(PushInfo pushInfo, UserConn userConn) {
		RPCResult rpcResult = new RPCResult();

		Integer pushState = pushService.pushMsg(pushInfo, userConn);
		if (null != pushState && pushState > 0) {
			rpcResult.setRpcErrCode(Errors.ERR_SUCCESS);
			return rpcResult;
		}
		rpcResult.setRpcErrCode(Errors.ERR_FAILED);
		return rpcResult;
	}

	@Override
	public RPCResult pushVoiceMsg(VoiceMsg voiceMsg) {
		RPCResult rpcResult = new RPCResult();
		rpcResult.setRpcErrCode(Errors.ERR_FAILED);
		if (pushService.sendMessageVoice(voiceMsg)) {
			rpcResult.setRpcErrCode(Errors.ERR_SUCCESS);
		}
		return rpcResult;
	}

	@Override
	public RPCResult getRongcloudToken(String name) {
		RPCResult rpcResult = new RPCResult();
		try {
			rpcResult.setRpcErrCode(Errors.ERR_FAILED);

			RongCloud rongCloud = RongCloud.getInstance(appKey, appSecret);
			TokenResult userGetTokenResult = rongCloud.user.getToken(name,
					name, "http://www.rongcloud.cn/images/logo.png");
			if (userGetTokenResult.getCode() == 200) {// 成功
				ChatToken chatToken = new ChatToken(name,
						userGetTokenResult.getToken());

				rpcResult.setRpcResult(chatToken);
				rpcResult.setRpcErrCode(Errors.ERR_SUCCESS);
			}

		} catch (Exception e) {
			e.printStackTrace();
			LogManager.error("用户获取融云toke异常：" + e);
		}

		return rpcResult;
	}

	@Override
	public RPCResult getGpsAddress(GpsAddress gpsAddress) {
		// lang 多语言
		RPCResult rPCResult = new RPCResult();
		String res = HttpUtils.post(amapApi,
				"location=" + gpsAddress.getLocation() + "&key=" + amapKey,
				"UTF-8");
		JSONObject objApi = JSON.parseObject(res);
		if (objApi.containsKey("infocode")
				&& objApi.get("infocode").toString().equals("10000")) {
			JSONObject regeocode = (JSONObject) objApi.get("regeocode");

			gpsAddress.setFormattedAddress(regeocode.get("formatted_address")
					.toString());

			JSONObject addressComponent = (JSONObject) regeocode
					.get("addressComponent");

			gpsAddress.setCitycode((String) addressComponent.get("citycode"));
			gpsAddress.setProvince((String) addressComponent.get("province"));
			gpsAddress.setCity((String) addressComponent.get("city"));
			gpsAddress.setDistrict((String) addressComponent.get("district"));

			rPCResult.setRpcResult(gpsAddress);
		} else {
			rPCResult.setRpcResult(gpsAddress);
		}

		return rPCResult;
	}

	@Override
	public RPCResult getGpsByWifiAndLbs(String equipId, String mapType,
			Map<Integer, Object> bodyVo) {
		RPCResult rPCResult = RPCResult.failed();

		try {
			GpsMessage message = gpsService.getGpsByWifiAndLbs(equipId,
					mapType, bodyVo);
			if (message != null) {
				rPCResult = RPCResult.success();
				rPCResult.setRpcResult(message.getMsgBodyMapVo());
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return rPCResult;
	}
}
