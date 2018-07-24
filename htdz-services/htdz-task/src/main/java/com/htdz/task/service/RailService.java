package com.htdz.task.service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.htdz.common.LogManager;
import com.htdz.common.utils.EnumUtils;
import com.htdz.common.utils.MapUtils;
import com.htdz.db.service.TalarmDataService;
import com.htdz.db.service.UserService;
import com.htdz.def.dbmodel.TalarmData;
import com.htdz.def.dbmodel.TareaInfo;
import com.htdz.def.view.GpsAreaInfo;
import com.htdz.def.view.PushInfo;
import com.htdz.def.view.Rail;
import com.htdz.def.view.UserConn;

/**
 * 围栏分析服务
 * 
 * @author wj
 *
 */
@Service
public class RailService {
	@Autowired
	private PushService pushService;
	@Autowired
	private UserService userService;
	@Autowired
	private TalarmDataService talarmDataService;
	private BlockingQueue<GpsAreaInfo> gpsDataQueue = new ArrayBlockingQueue<GpsAreaInfo>(
			10); // 阻塞队列。BlockingQueue是为了解决多线程中数据高效安全传输而提出的。
	ExecutorService executor = Executors.newFixedThreadPool(1); // 线程池 设置一个线程
	static Map<String, Map<String, Rail>> railMap = new HashMap<String, Map<String, Rail>>();

	public void onContextInitCompleted() {
		executor.execute(new Runnable() {
			@Override
			public void run() {
				while (true) {
					try {
						// 取走BlockingQueue里面排在首位的对象，
						// 如果BlockingQueue为空，则调用线程被阻塞，进入等待状态，直到BlockingQueue有新的数据被加入
						GpsAreaInfo gpsAreaInfoView = gpsDataQueue.take();
						regionAnalysis(gpsAreaInfoView);
					} catch (Exception e) {
						e.printStackTrace();
						LogManager.exception(e.getMessage(), e);
					}
				}
			}
		});
	}

	/**
	 * 把gpsAreaInfoView添加进BlockingQueue中，如果BlockingQueue中没有空间
	 * 则调用线程被阻塞，进入等待状态，直到BlockingQueue中有空间再继续
	 * 
	 * @param gpsAreaInfoView
	 */
	public void putTgpsData(GpsAreaInfo gpsAreaInfoView) {
		try {
			gpsDataQueue.put(gpsAreaInfoView);
		} catch (InterruptedException e) {
			e.printStackTrace();
			LogManager.exception(e.getMessage(), e);
		}
	}

	/**
	 * 计算围栏是否越界
	 * 
	 * @param gpsAreaInfoView
	 * @return
	 */
	public boolean regionAnalysis(GpsAreaInfo gpsAreaInfoView) {
		List<TareaInfo> areaInfoList = gpsAreaInfoView.getAreaInfoList();// 围栏集合
		List<PushInfo> gpsDataList = gpsAreaInfoView.getGpsDataList();// gps数据集合

		for (TareaInfo areaInfo : areaInfoList) {
			for (PushInfo gps : gpsDataList) {
				// 允许误差50米范围
				Integer radius = areaInfo.getRadius() + 50;
				boolean outSide = MapUtils.getDistance(areaInfo.getLat(),
						areaInfo.getLng(), gps.getLat(), gps.getLng(), radius);

				LogManager.info("设备GPS信息：" + JSON.toJSONString(gps));
				LogManager.info(gps.getEquipId() + "_设备围栏信息："
						+ JSON.toJSONString(areaInfo));

				/*
				 * gps.setMsgType(EnumUtils.PushMsgType.REMIND);
				 * gps.setAlarmtype(EnumUtils.AlarmType.OUT);
				 * gps.setTitle(gps.getEquipId() + "围栏ID：" +
				 * areaInfo.getAreaid() + "_" +
				 * MapUtils.getDistanceTest(areaInfo.getLat(),
				 * areaInfo.getLng(), gps.getLat(), gps.getLng(), radius));
				 * List<UserConn> userConnList =
				 * userService.getUserDeviceInfo(gps .getEquipId());
				 * 
				 * if (null != userConnList && userConnList.size() > 0) { for
				 * (UserConn userConn : userConnList) { pushService.pushMsg(gps,
				 * userConn); } }
				 */

				notifyArea(gps, areaInfo, outSide);
			}
		}
		return true;
	}

	/**
	 * 围栏越界通知
	 * 
	 * @param pushInfo
	 * @param areaid
	 * @param outSide
	 */
	public void notifyArea(PushInfo pushInfo, TareaInfo areaInfo,
			boolean outSide) {
		String deviceSn = pushInfo.getEquipId();
		Map<String, Rail> map = null;

		Boolean isNotity = false;// 当前是否要推送
		Rail rail = new Rail();

		String areaKey = areaInfo.getLat() + "_" + areaInfo.getLng() + "_"
				+ areaInfo.getRadius();
		if (railMap.containsKey(deviceSn)) {
			map = railMap.get(deviceSn);
			if (map.containsKey(areaKey)) {
				rail = map.get(areaKey);
				isNotity = rail.getIsNotity();
			} else {

				Integer areaInfoIsOut = areaInfo.getIsOut() == null ? 0
						: areaInfo.getIsOut();
				rail.setDeviceSn(pushInfo.getEquipId());
				rail.setAreaid(areaKey);
				if (areaInfoIsOut == 0) {
					rail.setOutSide(false);
				} else {
					rail.setOutSide(true);
				}
				rail.setIsNotity(false);
				rail.setCollectDatetime(pushInfo.getDatetime());
				map.put(areaKey, rail);
			}
		} else {
			Integer areaInfoIsOut = areaInfo.getIsOut() == null ? 0 : areaInfo
					.getIsOut();
			rail.setDeviceSn(pushInfo.getEquipId());
			rail.setAreaid(areaKey);
			if (areaInfoIsOut == 0) {
				rail.setOutSide(false);
			} else {
				rail.setOutSide(true);
			}

			rail.setIsNotity(false);
			rail.setCollectDatetime(pushInfo.getDatetime());
			map = new HashMap<String, Rail>();
			map.put(areaKey, rail);
			railMap.put(deviceSn, map);
		}

		LogManager.info(deviceSn + " 上一次是否越界:" + rail.getOutSide() + "_当前是否越界:"
				+ outSide);

		Integer areaInfoType = areaInfo.getType() == null ? 0 : areaInfo
				.getType();

		if (outSide != rail.getOutSide()) {
			if (areaInfoType == 0 && outSide) {
				isNotity = true;
			} else if (areaInfoType == 1) {
				isNotity = true;
			}

			rail.setOutSide(outSide);
		}

		LogManager.info(areaKey + "_围栏key," + deviceSn + "_否推是送：" + isNotity);

		if (isNotity) {
			rail.setIsNotity(false);

			TalarmData talarmData = new TalarmData();
			talarmData.setDid(pushInfo.getDid());
			talarmData.setDeviceSn(deviceSn);
			talarmData.setCollectDatetime(pushInfo.getDatetime());
			talarmData.setRcvTime(new Date());
			talarmData.setLat(pushInfo.getLat());
			talarmData.setLng(pushInfo.getLat());
			talarmData.setAlarmFlag(EnumUtils.AlarmType.OUT);
			if (!TextUtils.isEmpty(pushInfo.getSpeed())) {
				talarmData.setSpeed(Float.parseFloat(pushInfo.getSpeed()));
			}
			if (!TextUtils.isEmpty(pushInfo.getDirection())) {
				talarmData.setDirection(Float.parseFloat(pushInfo
						.getDirection()));
			}
			// 越界警情消息入库

			talarmDataService.add(talarmData);

			pushInfo.setMsgType(EnumUtils.PushMsgType.ALARM);
			pushInfo.setAlarmtype(EnumUtils.AlarmType.OUT);

			List<UserConn> userConnList = userService
					.getUserDeviceInfo(pushInfo.getEquipId());

			if (null != userConnList && userConnList.size() > 0) {
				for (UserConn userConn : userConnList) {
					// 消息推送
					pushService.pushMsg(pushInfo, userConn);
				}
			}

		}
	}
	/*
	 * 超过多长时间再提醒越界的算法 public void notifyArea(PushInfo pushInfo, Integer areaid,
	 * boolean outSide) { String deviceSn = pushInfo.getEquipId(); Map<Integer,
	 * Rail> map = null; if (railMap.containsKey(deviceSn)) {
	 * 
	 * map = railMap.get(deviceSn); if (map.containsKey(areaid)) { Rail prerail
	 * = map.get(areaid); // 上一次越界 if (prerail.getOutSide()) { if (outSide) {//
	 * 当前也越界 int td = DateTimeUtil.getTimeDelta( pushInfo.getDatetime(),
	 * prerail.getCollectDatetime()); // 计算时间差，大于10秒 并且 上一次没有通知 if (td >
	 * durationTime && null != prerail.getIsNotity() && !prerail.getIsNotity())
	 * {
	 * 
	 * prerail.setIsNotity(true);
	 * pushInfo.setMsgType(EnumUtils.PushMsgType.ALARM);
	 * pushInfo.setAlarmtype(EnumUtils.AlarmType.OUT);
	 * prerail.setCollectDatetime(pushInfo.getDatetime());
	 * 
	 * List<UserConn> userConnList = userService
	 * .getUserDeviceInfo(pushInfo.getEquipId()); if (null != userConnList &&
	 * userConnList.size() > 0) { for (UserConn userConn : userConnList) {
	 * pushService.pushMsg(pushInfo, userConn);// 消息推送 } } } } else {//
	 * 上一次有越界，当前没有越界 修改越界状态/通知状态 prerail.setOutSide(false);
	 * prerail.setIsNotity(false);
	 * prerail.setCollectDatetime(pushInfo.getDatetime()); } } else {// 上一次没有越界
	 * prerail.setOutSide(outSide);
	 * prerail.setCollectDatetime(pushInfo.getDatetime()); }
	 * 
	 * } else { Rail rail = new Rail(); rail.setDeviceSn(pushInfo.getEquipId());
	 * rail.setAreaid(areaid); rail.setOutSide(outSide);
	 * rail.setCollectDatetime(pushInfo.getDatetime()); map.put(areaid, rail); }
	 * 
	 * } else { Rail rail = new Rail(); rail.setDeviceSn(pushInfo.getEquipId());
	 * rail.setAreaid(areaid); rail.setOutSide(outSide);
	 * rail.setCollectDatetime(pushInfo.getDatetime()); map = new
	 * HashMap<Integer, Rail>(); map.put(areaid, rail); railMap.put(deviceSn,
	 * map); }
	 * 
	 * }
	 */
}
