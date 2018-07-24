package com.htdz.litefamily.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.htdz.common.LogManager;
import com.htdz.common.utils.DateTimeUtil;
import com.htdz.common.utils.EnumUtils;
import com.htdz.db.service.DeviceRemindService;
import com.htdz.db.service.PrePushLogService;
import com.htdz.db.service.UserService;
import com.htdz.def.dbmodel.DeviceRemind;
import com.htdz.def.view.PushInfo;
import com.htdz.def.view.UserConn;
import com.htdz.litefamily.dubbo.TaskServiceConsumer;

@Service
public class RemindServerManager {

	@Autowired
	private DeviceRemindService deviceRemindService;

	@Autowired
	UserService userService;

	@Autowired
	TaskServiceConsumer taskServiceConsumer;

	@Autowired
	private PrePushLogService prePushLogService;

	private ArrayList<DeviceRemind> deviceRemindList = null;

	public RemindServerManager() {
		deviceRemindList = new ArrayList<DeviceRemind>();
	}

	public void load() {
		deviceRemindList = new ArrayList<DeviceRemind>(
				deviceRemindService.selectDeviceRemindByOneDate(DateTimeUtil.getCurrentUtcDate()));
	}

	public boolean add(DeviceRemind remind) {
		return deviceRemindList.add(remind);
	}

	public boolean delete(DeviceRemind remind) {
		return deviceRemindList.remove(remind);
	};

	@Scheduled(cron = "0 0 0 * * ?", zone = "GMT")
	public void daySchedule() {
		LogManager.info("litefamily load deviceRemindList at 00:00");
		load();
	}

	@Scheduled(cron = "0 */1 * * * ?")
	public void minuteSchedule() {

		try {
			for (DeviceRemind remind : deviceRemindList) {
				String remimdTime = DateTimeUtil.dateToStrLong(remind.getRemindTimeUTC());

				if (0 == DateTimeUtil.comPareTime(remimdTime, DateTimeUtil.getCurrentUtcDatetime())) {

					PushInfo pushInfo = new PushInfo();
					pushInfo.setEquipId(remind.getDeviceSn());
					pushInfo.setMsgType(EnumUtils.PushMsgType.REMIND);
					pushInfo.setDatetime(remind.getRemindTimeUTC());
					pushInfo.setTitle(remind.getTitle());

					List<UserConn> userConnList = userService.getUserDeviceInfo(pushInfo.getEquipId());
					UserConn deviceAsUser = userService.getDeviceAsUserInfo(pushInfo.getEquipId());
					deviceAsUser.setIsCustomizedApp(0);
					deviceAsUser.setCertificate(1);
					userConnList.add(deviceAsUser);

					if (null != userConnList && userConnList.size() > 0) {
						for (UserConn userConn : userConnList) {
							taskServiceConsumer.pushMsg(pushInfo, userConn);
						}
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
			LogManager.exception("minuteSchedule exception={}", e);
		}
	}

}
