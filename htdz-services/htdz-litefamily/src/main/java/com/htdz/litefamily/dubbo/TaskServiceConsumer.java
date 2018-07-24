package com.htdz.litefamily.dubbo;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.dubbo.config.annotation.Reference;
import com.htdz.db.service.PrePushLogService;
import com.htdz.db.service.UserService;
import com.htdz.def.data.Errors;
import com.htdz.def.data.RPCResult;
import com.htdz.def.dbmodel.PrePushLog;
import com.htdz.def.interfaces.TaskService;
import com.htdz.def.view.GpsAddress;
import com.htdz.def.view.GpsAreaInfo;
import com.htdz.def.view.PushInfo;
import com.htdz.def.view.UserConn;

@Component
public class TaskServiceConsumer {
	@Reference(version = "1.0.0", timeout = 20000, check = false)
	TaskService taskService;
	@Autowired
	UserService userService;
	@Autowired
	private PrePushLogService prePushLogService;

	public RPCResult regionAnalysis(GpsAreaInfo gpsAreaInfoView) {
		return taskService.regionAnalysis(gpsAreaInfoView);
	}

	public RPCResult pushMsg(PushInfo pushInfo, UserConn userConn) {
		RPCResult rpcResult = new RPCResult();

		PrePushLog pushLog = new PrePushLog();
		pushLog.setDeviceSn(pushInfo.getEquipId());
		pushLog.setCreateTime(new Date());
		pushLog.setMsg(pushInfo.getTitle());
		pushLog.setMsgType(pushInfo.getMsgType());
		pushLog.setPushUser(userConn.getName());
		pushLog.setPushState(Errors.ERR_FAILED);
		prePushLogService.add(pushLog); // 推送信息入DB

		rpcResult = taskService.pushMsg(pushInfo, userConn);

		if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS) {
			pushLog.setPushState(Errors.ERR_SUCCESS);
		}
		pushLog.setOverTime(new Date());
		prePushLogService.modify(pushLog); // 推送信息入DB

		return rpcResult;

	}

	public RPCResult getRongcloudToken(String name) {
		return taskService.getRongcloudToken(name);
	}

	public RPCResult getGpsAddress(GpsAddress gpsAddress) {
		return taskService.getGpsAddress(gpsAddress);
	}
}
