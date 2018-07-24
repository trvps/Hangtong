package com.htdz.device.dubbo;

import java.util.Map;

import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Reference;
import com.htdz.def.data.RPCResult;
import com.htdz.def.interfaces.TaskService;
import com.htdz.def.view.GpsAreaInfo;
import com.htdz.def.view.PushInfo;
import com.htdz.def.view.UserConn;
import com.htdz.def.view.VoiceMsg;

@Component
public class TaskServiceConsumer {
	@Reference(version = "1.0.0", check = false, timeout = 20000)
	TaskService taskService;

	public RPCResult regionAnalysis(GpsAreaInfo gpsAreaInfoView) {
		return taskService.regionAnalysis(gpsAreaInfoView);
	}

	public RPCResult pushMsg(PushInfo pushInfo, UserConn userConn) {
		return taskService.pushMsg(pushInfo, userConn);
	}

	public RPCResult pushVoiceMsg(VoiceMsg voiceMsg) {
		return taskService.pushVoiceMsg(voiceMsg);
	}

	public RPCResult getGpsByWifiAndLbs(String equipId, String mapType,
			Map<Integer, Object> bodyVo) {
		return taskService.getGpsByWifiAndLbs(equipId, mapType, bodyVo);
	}
}
