package com.htdz.task.controllers;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.htdz.common.LogManager;
import com.htdz.common.utils.DateTimeUtil;
import com.htdz.common.utils.EnumUtils;
import com.htdz.db.service.UserService;
import com.htdz.def.view.PushInfo;
import com.htdz.def.view.UserConn;
import com.htdz.task.service.PushService;

@RestController
public class APIController {

	@Autowired
	UserService userService;

	@Autowired
	private PushService pushService;

	@RequestMapping("/info")
	public String info() {
		return "Task APIController";
	}

	@RequestMapping("/pushTest")
	public void pushTest(@RequestParam("deviceSn") String deviceSn) {

		LogManager.info("pushTest start");

		PushInfo pushInfo = new PushInfo();
		pushInfo.setEquipId(deviceSn);
		pushInfo.setMsgType(EnumUtils.PushMsgType.REMIND);
		pushInfo.setTitle("pushTest");
		pushInfo.setDatetime(DateTimeUtil.strToDateLong(DateTimeUtil.getCurrentUtcDatetime()));

		List<UserConn> userConnList = userService.getUserDeviceInfo(pushInfo.getEquipId());
		if (null != userConnList && userConnList.size() > 0) {
			for (UserConn userConn : userConnList) {
				pushService.pushMsg(pushInfo, userConn);
			}
		}

	}
}