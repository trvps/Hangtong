package com.htdz.device.handler.HT790;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;
import org.jboss.netty.util.internal.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.htdz.common.Consts;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;
import com.htdz.common.utils.DateTimeUtil;
import com.htdz.common.utils.FileUtils;
import com.htdz.common.utils.HttpUtils;
import com.htdz.common.utils.StringEncoding;
import com.htdz.common.utils.HttpUtils.ByteArrayBodyPair;
import com.htdz.common.utils.HttpUtils.FileBodyPair;
import com.htdz.common.utils.HttpUtils.InputStreamBodyPair;
import com.htdz.common.utils.HttpUtils.StringBodyPair;
import com.htdz.db.service.DevicePhotoService;
import com.htdz.db.service.DeviceService;
import com.htdz.db.service.DeviceSessionMapService;
import com.htdz.db.service.DeviceStepService;
import com.htdz.db.service.DeviceTurnService;
import com.htdz.db.service.GpsDataLastService;
import com.htdz.db.service.TAlarmSettingService;
import com.htdz.db.service.TAreaInfoService;
import com.htdz.db.service.TDeviceUserService;
import com.htdz.db.service.TPhoneBookService;
import com.htdz.db.service.TalarmDataService;
import com.htdz.db.service.TgpsDataService;
import com.htdz.db.service.UserService;
import com.htdz.def.data.RPCResult;
import com.htdz.def.dbmodel.DeviceFriendData;
import com.htdz.def.dbmodel.DevicePhoto;
import com.htdz.def.dbmodel.DeviceSessionMap;
import com.htdz.def.dbmodel.DeviceStep;
import com.htdz.def.dbmodel.DeviceTurn;
import com.htdz.def.dbmodel.GpsAndAlarmDataView;
import com.htdz.def.dbmodel.GpsDataLast;
import com.htdz.def.dbmodel.GpsDataView;
import com.htdz.def.dbmodel.TAlarmSetting;
import com.htdz.def.dbmodel.TDeviceUser;
import com.htdz.def.dbmodel.TalarmData;
import com.htdz.def.dbmodel.TareaInfo;
import com.htdz.def.dbmodel.Tdevice;
import com.htdz.def.dbmodel.TgpsData;
import com.htdz.def.view.GpsAreaInfo;
import com.htdz.def.view.PushInfo;
import com.htdz.def.view.UserConn;
import com.htdz.def.view.VoiceMsg;
import com.htdz.device.data.GSMBSItem;
import com.htdz.device.data.Message77X;
import com.htdz.device.data.Message790;
import com.htdz.device.data.UDMessage;
import com.htdz.device.data.WifiItem;
import com.htdz.device.dubbo.GatewaySerivceConsumer;
import com.htdz.device.dubbo.TaskServiceConsumer;
import com.htdz.device.handler.DeviceBaseProtocol;
import com.htdz.device.handler.PPFriendTask;

@Component
public class Device790Protocol extends DeviceBaseProtocol {
	// 缓存当前链接设备信息
	public static Map<String, Tdevice> deviceMap = new ConcurrentHashMap<String, Tdevice>();
	// 缓存设备区域信息
	public static Map<String, List<TareaInfo>> areaMap = new ConcurrentHashMap<String, List<TareaInfo>>();
	// 缓存设备最新GPS数据
	public static Map<String, TgpsData> gpsDataMap = new ConcurrentHashMap<String, TgpsData>();
	// 缓存电量
	public static Map<String, Integer> batteryMap = new HashMap<String, Integer>();

	@Autowired
	DeviceSessionMapService deviceSessionMapService;

	@Autowired
	private DeviceService deviceService;

	@Autowired
	private TgpsDataService tgpsDataService;

	@Autowired
	private TalarmDataService talarmDataService;

	@Autowired
	private GatewaySerivceConsumer gsConsumer;

	@Autowired
	TAreaInfoService tAreaInfoService;

	@Autowired
	TAlarmSettingService tAlarmSettingService;

	@Autowired
	TPhoneBookService tPhoneBookService;

	@Autowired
	TaskServiceConsumer taskServiceConsumer;

	@Autowired
	UserService userService;

	@Autowired
	Environment env;

	@Autowired
	GpsDataLastService gpsDataLastService;

	@Autowired
	DevicePhotoService devicePhotoService;

	@Autowired
	DeviceTurnService deviceTurnService;

	@Autowired
	private TDeviceUserService tDeviceUserService;

	@Autowired
	private DeviceStepService deviceStepService;

	private PPFriendTask pPFriendTask;

	public void setPPFriendTask(PPFriendTask pPFriendTask) {
		this.pPFriendTask = pPFriendTask;
	}

	/**
	 * LK
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_LK(String deviceSession, Message790 message,
			String deviceName) {
		try {
			RPCResult result = RPCResult.success();

			String deviceSn = message.getDeviceSn();
			try {
				// 设备链接上来，缓存设备数据
				Tdevice device = deviceService.select(deviceSn);
				if (device != null) {
					deviceMap.put(deviceSn, device);
				}

				// 更新数据库设备版本号
				deviceService.updateHardware(17, deviceSn);

				// 保存设备号与deviceSession的对应关系
				DeviceSessionMap deviceSessionMap = deviceSessionMapService
						.getByDeviceSn(message.getDeviceSn());
				if (deviceSessionMap != null) {
					if (deviceSessionMapService.deleteByDeviceSn(message
							.getDeviceSn())) {
						deviceSessionMapService.save(deviceName,
								message.getDeviceSn(), deviceSession);
					} else {
						LogManager.info("设备{}删除deviceSessionMap记录失败",
								message.getDeviceSn());
					}
				} else {
					deviceSessionMapService.save(deviceName,
							message.getDeviceSn(), deviceSession);
				}

				// 推送在线状态（上线）
				LogManager.info("设备:{}发送LK保持链接，推送在线状态", message.getDeviceSn());
				PushInfo pushInfo = new PushInfo();
				pushInfo.setEquipId(message.getDeviceSn());
				pushInfo.setOnlinestatus("1");
				pushInfo.setMsgType(2);

				List<UserConn> userConnList = userService
						.getUserDeviceInfo(pushInfo.getEquipId());
				if (null != userConnList && userConnList.size() > 0) {
					for (UserConn userConn : userConnList) {
						if (StringUtils.isEmpty(userConn.getToken())) {
							RPCResult pushRet = taskServiceConsumer.pushMsg(
									pushInfo, userConn);
							LogManager.info("设备:{}发送LK保持链接，推送在线状态:{}",
									message.getDeviceSn(),
									pushRet.getRpcErrCode() == 0 ? "成功" : "失败");
						}
					}
				}

				// 更新本地围栏缓存
				Tdevice tdevice = deviceService.select(deviceSn);
				Integer did = tdevice.getId();
				List<TareaInfo> list = tAreaInfoService.getAreaListById(did);
				areaMap.put(deviceSn, list);
			} catch (Exception e) {
				LogManager.exception(e.getMessage(), e);
			}

			// 回复LK指令
			gsConsumer.pushMessageToDevice(true, deviceName, deviceSession,
					message.buildResponse("LK").getBytes());
			TimeUnit.MILLISECONDS.sleep(100);

			// 初始化指令（短信总开关，低电短信开关，SOS短信开关）
			String ininCommand = setinit(deviceSn);
			String[] cmdArr = ininCommand.split("\\|");
			for (String cmd : cmdArr) {
				gsConsumer.pushMessageToDevice(true, deviceName, deviceSession,
						cmd.getBytes());
				TimeUnit.MILLISECONDS.sleep(100);
			}

			return result;
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);

			return RPCResult.failed();
		}
	}

	/**
	 * 设备链接上服务器，第一时间进行初始化设置
	 * 
	 * @param deviceSn
	 * @param deviceName
	 * @return
	 */
	public String setinit(String deviceSn) {
		StringBuilder result = new StringBuilder();

		// 取下手表报警开关
		String REMOVE = "REMOVE,1";
		// 取下手表短信报警开关
		String REMOVESMS = "REMOVESMS,0";
		// 低电短信报警开关关
		String LOWBAT = "LOWBAT,0";
		// SOS 短信报警开关开
		String SOS = "SOSSMS,1";
		//计步开关开
		String walkstatus = "PEDO,0";

		Tdevice device = deviceMap.get(deviceSn);
		TAlarmSetting tAlarmSetting = tAlarmSettingService
				.getAlarmSettingById(device.getId());
		if (tAlarmSetting != null) {
			Short sos = tAlarmSetting.getSos();
			if (sos == 0) {
				SOS = "SOSSMS,0";
			}

			Short takeoff = tAlarmSetting.getTakeOff();
			if (takeoff == 0) {
				REMOVE = "REMOVE,0";
			}
		}

		Message790 message = new Message790();
		message.setFactoryName("SG");
		message.setDeviceSn(deviceSn);

		// 取下手表报警开关
		REMOVE = message.buildResponse(REMOVE);

		// 取下手表短信报警开关
		REMOVESMS = message.buildResponse(REMOVESMS);

		// 设置短信总开关
		LOWBAT = message.buildResponse(LOWBAT);

		// 设置短信总开关
		SOS = message.buildResponse(SOS);
		
		//计步开关开
		walkstatus = message.buildResponse(walkstatus);

		result.append(REMOVE).append("|").append(REMOVESMS).append("|")
				.append(LOWBAT).append("|").append(SOS).append("|").append(walkstatus);

		return result.toString();
	}

	/**
	 * PING
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_PING(String deviceSession, Message790 message,
			String deviceName) {
		try {
			RPCResult result = RPCResult.success();

			List<TDeviceUser> list = tDeviceUserService.getByDeviceSn(message
					.getDeviceSn());
			if (list == null || list.size() < 1) {
				// 回复PING指令
				result = gsConsumer.pushMessageToDevice(true, deviceName,
						deviceSession, message.buildResponse("PING,0")
								.getBytes());
			} else {
				// 回复PING指令
				result = gsConsumer.pushMessageToDevice(true, deviceName,
						deviceSession, message.buildResponse("PING,1")
								.getBytes());
			}
		    
			if(result.getRpcResult() != null && (Integer)result.getRpcResult() > 0)
			{
				result = RPCResult.success();
			} else {
				result = RPCResult.failed();
			}
				
			return result;
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);

			return RPCResult.failed();
		}
	}

	/**
	 * KA日期，步数,翻滚次数,电量百分数
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_KA(String deviceSession, Message790 message,
			String deviceName) {
		RPCResult result = RPCResult.failed();

		try {
			boolean saveStepsSuccess = false;
			boolean saveTurnSuccess = false;
			boolean saveBatterySuccess = false;

			String deviceSn = message.getDeviceSn();
			String data = message.getData();
			if (data.indexOf(",") != -1) {
				String[] dataArr = data.split("\\,");
				String dateTime = getDateTime(dataArr[0], "000000");
				Integer steps = dataArr[1] == null ? 0 : Integer
						.parseInt(dataArr[1]);
				Integer turn = dataArr[2] == null ? 0 : Integer
						.parseInt(dataArr[2]);
				Integer battery = dataArr[3] == null ? 0 : Integer
						.parseInt(dataArr[3]);

				SimpleDateFormat df = new SimpleDateFormat(
						"yyyy-MM-dd HH:mm:ss");
				Date date = df.parse(dateTime);

				// 保存翻滚次数
				DeviceTurn deviceTurn = new DeviceTurn();
				deviceTurn.setDeviceSn(deviceSn);
				deviceTurn.setCreateTime(date);

				DeviceTurn dt = deviceTurnService.select(deviceTurn);
				deviceTurn.setTurn(turn);
				if (null != dt && turn > dt.getTurn()) {
					// 修改
					saveTurnSuccess = deviceTurnService.modify(deviceTurn);
				} else if (null == dt) {
					// 新增
					saveTurnSuccess = deviceTurnService.add(deviceTurn);
				}

				// 保存电量
				batteryMap.put(deviceSn, battery);
				GpsDataLast gpsDataLast = gpsDataLastService.select(deviceSn);
				if (gpsDataLast != null) {
					saveBatterySuccess = gpsDataLastService.updateBattery(
							deviceSn, battery.toString());
				}

				// 保存步数
				DeviceStep deviceStep = deviceStepService.select(deviceSn,
						dateTime);
				if (deviceStep != null) {
					// 修改
					deviceStep.setCreateDate(date);
					deviceStep.setStep(steps.toString());
					deviceStep.setUpdateDate(new Date());
					saveStepsSuccess = deviceStepService.modify(deviceStep);
				} else {
					// 添加
					deviceStep = new DeviceStep();
					deviceStep.setDeviceSn(deviceSn);
					deviceStep.setCreateDate(date);
					deviceStep.setStep(steps.toString());
					deviceStep.setUpdateDate(new Date());
					saveStepsSuccess = deviceStepService.add(deviceStep);
				}
			}

			// 回复KA指令
			if (saveStepsSuccess && saveTurnSuccess && saveBatterySuccess) {
				result = gsConsumer.pushMessageToDevice(true, deviceName,
						deviceSession, message.buildResponse("KA").getBytes());
			}
			
			if(result.getRpcResult() != null && (Integer)result.getRpcResult() > 0)
			{
				result = RPCResult.success();
			} else {
				result = RPCResult.failed();
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return result;
	}

	/**
	 * UP 位置数据上报
	 * 
	 * @param factoryName
	 * @param deviceSn
	 * @param data
	 * @return
	 */
	public RPCResult call_UP(String deviceSession, Message790 message,
			String deviceName) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		Tdevice tdevice = deviceService.select(message.getDeviceSn());
		if (tdevice == null)
			return RPCResult.serviceRefuse();

		try {
			GpsAndAlarmDataView gpsAndAlarmData = getGpsOrAlarmData(tdevice,
					message);

			if (gpsAndAlarmData != null && gpsAndAlarmData.getDataType() == 0) {
				TgpsData tgpsData = gpsAndAlarmData.getGpsData();

				String sql = String
						.format("insert into TGpsData("
								+ "did, device_sn, collect_datetime, rcv_time,"
								+ "lat, lng, speed, direction,"
								+ "altitude, longitude_way, latitude_way, satellite_num,"
								+ "location_id, cell_id, msg_id, gps_flag,"
								+ "flag, battery, steps, LBS_WIFI_Range,"
								+ "LBS_WIFI_Flag, acc_status, calorie) "
								+ "values(" + "'%s', '%s', '%s', '%s',"
								+ "'%s', '%s', '%s', '%s',"
								+ "'%s', '%s', '%s', '%s',"
								+ "'%s', '%s', '%s', '%s',"
								+ "'%s', '%s', '%s', '%s',"
								+ "'%s', '%s', '%s')",
								tgpsData.getDid(),
								tgpsData.getDeviceSn(),
								df.format(tgpsData.getCollectDatetime()),
								df.format(tgpsData.getRcvTime()),
								tgpsData.getLat(),
								tgpsData.getLng(),
								tgpsData.getSpeed(),
								tgpsData.getDirection(),
								tgpsData.getAltitude(),
								tgpsData.getLongitudeWay(),
								tgpsData.getLatitudeWay(),
								tgpsData.getSatelliteNum(),
								tgpsData.getLocationId(),
								tgpsData.getCellId(),
								tgpsData.getMsgId(),
								tgpsData.getGpsFlag(),
								tgpsData.getFlag(),
								batteryMap.containsKey(tgpsData.getDeviceSn()) ? batteryMap
										.get(tgpsData.getDeviceSn()) : 0,
								tgpsData.getSteps(),
								tgpsData.getLbsWifiRange(), tgpsData
										.getLbsWifiFlag(), tgpsData
										.getAccStatus(), tgpsData.getCalorie());

				// 如果为补传数据则只入库
				if (message.getMethod().equals("UP2")) {
					if (tgpsDataService.insertGpsDataBySql(sql))
						return RPCResult.success();
				}

				// 保存GPS数据成功
				if (tgpsDataService.insertGpsDataBySql(sql)) {
					boolean onlineStatus = gsConsumer.isDeviceOnline(true,
							deviceName, deviceSession);

					LogManager.info("设备：{}在线状态为：{}", tgpsData.getDeviceSn(),
							onlineStatus ? "在线" : "下线");

					// 构建推送数据
					PushInfo pushInfo = new PushInfo();
					pushInfo.setEquipId(tgpsData.getDeviceSn());
					pushInfo.setOnlinestatus(onlineStatus ? "1" : "0");

					List<UserConn> userConnList = userService
							.getUserDeviceInfo(pushInfo.getEquipId());

					LogManager.info("设备：{}当前上传GPS数据定位类型：{}，lng：{}，lat：{}",
							tgpsData.getGpsFlag(), tgpsData.getLng(),
							tgpsData.getLat());

					// GPS有效才插入数据到最后位置数据表、跟新缓存、推送位置、区域
					if (tgpsData.getLat() != 0 && tgpsData.getLng() != 0
							&& !tgpsData.getGpsFlag().equals("0")
							&& !tgpsData.getGpsFlag().equals("1")) {
						// 更新最后位置数据
						gpsDataLastService.delete(tgpsData.getDeviceSn());
						String sqlLastGps = String
								.format("insert into gps_data_last("
										+ "device_sn, collect_datetime, rcv_time,"
										+ "lat, lng, speed, direction,"
										+ "altitude, longitude_way, latitude_way, satellite_num,"
										+ "location_id, cell_id, msg_id, gps_flag,"
										+ "flag, battery, steps, LBS_WIFI_Range,"
										+ "LBS_WIFI_Flag, Acc_Status, calorie,online) "
										+ "values(" + "'%s', '%s', '%s',"
										+ "'%s', '%s', '%s', '%s',"
										+ "'%s', '%s', '%s', '%s',"
										+ "'%s', '%s', '%s', '%s',"
										+ "'%s', '%s', '%s', '%s',"
										+ "'%s', '%s', '%s', '%s')",
										tgpsData.getDeviceSn(),
										df.format(tgpsData.getCollectDatetime()),
										df.format(tgpsData.getRcvTime()),
										tgpsData.getLat(),
										tgpsData.getLng(),
										tgpsData.getSpeed(),
										tgpsData.getDirection(),
										tgpsData.getAltitude(),
										tgpsData.getLongitudeWay(),
										tgpsData.getLatitudeWay(),
										tgpsData.getSatelliteNum(),
										tgpsData.getLocationId(),
										tgpsData.getCellId(),
										tgpsData.getMsgId(),
										tgpsData.getGpsFlag(),
										tgpsData.getFlag(),
										batteryMap.containsKey(tgpsData
												.getDeviceSn()) ? batteryMap
												.get(tgpsData.getDeviceSn())
												: 0, tgpsData.getSteps(),
										tgpsData.getLbsWifiRange(), tgpsData
												.getLbsWifiFlag(), tgpsData
												.getAccStatus(), tgpsData
												.getCalorie(), onlineStatus ? 1
												: 0);
						gpsDataLastService.insertGpsDataBySql(sqlLastGps);

						// 跟新最新位置到缓存
						gpsDataMap.put(tgpsData.getDeviceSn(), tgpsData);

						// 构建位置信息
						// 推送消息类型 0表示位置数据，1表示警情数据，：2表示状态数据
						pushInfo.setMsgType(0);
						pushInfo.setDatetime(tgpsData.getCollectDatetime());
						pushInfo.setRcvTime(tgpsData.getRcvTime());
						pushInfo.setLat(tgpsData.getLat());
						pushInfo.setLng(tgpsData.getLng());
						pushInfo.setSpeed(tgpsData.getSpeed().toString());
						pushInfo.setDirection(tgpsData.getDirection()
								.toString());

						// 消息推送,推送位置信息
						LogManager
								.info("开始推送设备：{}位置数据", tgpsData.getDeviceSn());
						if (null != userConnList && userConnList.size() > 0) {
							for (UserConn userConn : userConnList) {
								if (StringUtils.isEmpty(userConn.getToken())) {
									RPCResult pushRet = taskServiceConsumer
											.pushMsg(pushInfo, userConn);
									LogManager
											.info("设备:{}推送位置数据：{}，GPS数据定位类型：{}，lng：{}，lat：{}",
													tgpsData.getDeviceSn(),
													pushRet.getRpcErrCode() == 0 ? "成功"
															: "失败", tgpsData
															.getGpsFlag(),
													tgpsData.getLng(), tgpsData
															.getLat());
								}
							}
						}

						// 推送区域和位置
						LogManager.info("开始推送设备：{}区域和位置数据",
								tgpsData.getDeviceSn());
						// 获取警情开关
						TAlarmSetting tAlarmSetting = tAlarmSettingService
								.getAlarmSettingById(tgpsData.getDid());
						List<TareaInfo> list = areaMap.get(tgpsData
								.getDeviceSn());
						if (tAlarmSetting != null
								&& tAlarmSetting.getBoundary() == 1
								&& list != null && !list.isEmpty()) {
							GpsAreaInfo gpsAreaInfoView = new GpsAreaInfo();
							gpsAreaInfoView.setAreaInfoList(list);

							List<PushInfo> listGps = new ArrayList<PushInfo>();
							listGps.add(pushInfo);
							gpsAreaInfoView.setGpsDataList(listGps);

							RPCResult pushRet = taskServiceConsumer
									.regionAnalysis(gpsAreaInfoView);
							LogManager.info("设备:{}推送区域和位置数据：{}",
									tgpsData.getDeviceSn(),
									pushRet.getRpcErrCode() == 0 ? "成功" : "失败");
						}
					} else {
						// 跟新在线状态到最后位置表
						gpsDataLastService.updateOnlineStatus(
								tgpsData.getDeviceSn(), onlineStatus ? 1 : 0);
					}

					pushInfo.setMsgType(2);
					// 消息推送,推送状态信息
					LogManager.info("开始推送设备：{}在线状态数据", tgpsData.getDeviceSn());
					if (null != userConnList && userConnList.size() > 0) {
						for (UserConn userConn : userConnList) {
							if (StringUtils.isEmpty(userConn.getToken())) {
								RPCResult pushRet = taskServiceConsumer
										.pushMsg(pushInfo, userConn);
								LogManager.info("设备:{}推送在线状态数据：{}", tgpsData
										.getDeviceSn(),
										pushRet.getRpcErrCode() == 0 ? "成功"
												: "失败");
							}
						}
					}
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);

			return RPCResult.failed();
		}

		return RPCResult.success();
	}

	/**
	 * UP2
	 * 
	 * @param factoryName
	 * @param deviceSn
	 * @param data
	 * @return
	 */
	public RPCResult call_UP2(String deviceSession, Message790 message,
			String deviceName) {
		return call_UP(deviceSession, message, deviceName);
		// return RPCResult.success();
	}

	/**
	 * ALARM
	 * 
	 * @param factoryName
	 * @param deviceSn
	 * @param data
	 * @return
	 */
	public RPCResult call_ALARM(String deviceSession, Message790 message,
			String deviceName) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try {
			Tdevice tdevice = deviceService.select(message.getDeviceSn());
			if (tdevice == null)
				return RPCResult.serviceRefuse();

			GpsAndAlarmDataView gpsAndAlarmData = getGpsOrAlarmData(tdevice,
					message);

			if (gpsAndAlarmData != null && gpsAndAlarmData.getDataType() == 1) {
				TalarmData talarmData = gpsAndAlarmData.getAlarmData();

				String sql = String
						.format("insert into TAlarmData("
								+ "did, device_sn, collect_datetime,"
								+ "rcv_time, type, lat,lng, speed, "
								+ "direction, satellite_num, location_id, cell_id,"
								+ "gps_flag, flag, battery) " + "values("
								+ "'%s', '%s', '%s', '%s', "
								+ "'%s', '%s', '%s', '%s', "
								+ "'%s', '%s', '%s', '%s', "
								+ "'%s', '%s', '%s')",
								talarmData.getDid(),
								talarmData.getDeviceSn(),
								df.format(talarmData.getCollectDatetime()),
								df.format(talarmData.getRcvTime()),
								talarmData.getType(),
								talarmData.getLat(),
								talarmData.getLng(),
								talarmData.getSpeed(),
								talarmData.getDirection(),
								talarmData.getSatelliteNum(),
								talarmData.getLocationId(),
								talarmData.getCellId(),
								talarmData.getGpsFlag(),
								talarmData.getFlag(),
								batteryMap.containsKey(talarmData.getDeviceSn()) ? batteryMap
										.get(talarmData.getDeviceSn()) : 0);

				boolean result = talarmDataService.insertAlarmDataBySql(sql);
				LogManager.info("执行SQL:{} 插入警情数据结果:{}", sql, result);
				if (result) {
					boolean onlineStatus = gsConsumer.isDeviceOnline(true,
							deviceName, deviceSession);

					TAlarmSetting tAlarmSetting = tAlarmSettingService
							.getAlarmSettingById(talarmData.getDid());

					LogManager.info("警情推送相关信息：警情类型为：{}，tAlarmSetting是否为空：{}",
							talarmData.getType(), tAlarmSetting == null);
					if (tAlarmSetting != null) {
						LogManager.info("警情开关设置相关信息：SOS开关：{}，低电开关{}，脱落报警开关{}",
								tAlarmSetting.getSos(),
								tAlarmSetting.getVoltage(),
								tAlarmSetting.getTakeOff());
					}

					if (tAlarmSetting == null
							|| (tAlarmSetting != null
									&& tAlarmSetting.getSos() == 1 && talarmData
									.getType() == 6)
							|| (tAlarmSetting != null
									&& tAlarmSetting.getVoltage() == 1 && talarmData
									.getType() == 18)
							|| (tAlarmSetting != null
									&& tAlarmSetting.getTakeOff() == 1 && talarmData
									.getType() == 35)) {
						// 推送数据
						PushInfo pushInfo = new PushInfo();
						pushInfo.setDid(talarmData.getDid());
						pushInfo.setEquipId(talarmData.getDeviceSn());
						// 推送消息类型 0表示位置数据，1表示警情数据，：2表示状态数据
						pushInfo.setMsgType(1);
						pushInfo.setAlarmtype(talarmData.getType());
						pushInfo.setDatetime(talarmData.getCollectDatetime());
						pushInfo.setRcvTime(talarmData.getRcvTime());
						pushInfo.setLat(talarmData.getLat());
						pushInfo.setLng(talarmData.getLng());
						pushInfo.setSpeed(talarmData.getSpeed().toString());
						pushInfo.setDirection(talarmData.getDirection()
								.toString());
						pushInfo.setOnlinestatus(onlineStatus ? "1" : "0");

						List<UserConn> userConnList = userService
								.getUserDeviceInfo(pushInfo.getEquipId());

						if (null != userConnList && userConnList.size() > 0) {
							for (UserConn userConn : userConnList) {
								taskServiceConsumer.pushMsg(pushInfo, userConn);// 消息推送
							}
						}
					}

					RPCResult ret = RPCResult.success();
					ret.setRpcResult(message.buildResponse("ALARM"));
					return ret;
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);

			return RPCResult.failed();
		}

		return RPCResult.success();
	}

	// 获取定位时间(转换为UTC时间)
	public String getDateTime(String date, String time, Integer tz) {
		try {
			if (date != null && !date.isEmpty()) {
				String localDateTime = "20" + date.substring(4, 6) + "-"
						+ date.substring(2, 4) + "-" + date.substring(0, 2)
						+ " " + time.substring(0, 2) + ":"
						+ time.substring(2, 4) + ":" + time.substring(4, 6);
				return DateTimeUtil.local2utc(localDateTime, tz.toString());
			} else {
				return getCurrentUtcDateTime();
			}
		} catch (Exception e) {
			return getCurrentUtcDateTime();
		}
	}

	// 获取定位时间(本地时间)
	public String getDateTime(String date, String time) {
		try {
			if (date != null && !date.isEmpty()) {
				String localDateTime = "20" + date.substring(0, 2) + "-"
						+ date.substring(2, 4) + "-" + date.substring(4, 6)
						+ " " + time.substring(0, 2) + ":"
						+ time.substring(2, 4) + ":" + time.substring(4, 6);

				return localDateTime;
			} else {
				return "";
			}
		} catch (Exception e) {
			return "";
		}
	}

	// 获取当前UTC时间
	public String getCurrentUtcDateTime() {
		GregorianCalendar gc = new GregorianCalendar();
		int zoneOffSet = gc.get(Calendar.ZONE_OFFSET);
		int dstOffSet = gc.get(Calendar.DST_OFFSET);
		gc.add(Calendar.MILLISECOND, -(zoneOffSet + dstOffSet));

		int year = gc.get(Calendar.YEAR);
		int month = gc.get(Calendar.MONTH) + 1;
		int day = gc.get(Calendar.DAY_OF_MONTH);
		int hour = gc.get(Calendar.HOUR_OF_DAY);
		int minute = gc.get(Calendar.MINUTE);
		int second = gc.get(Calendar.SECOND);

		StringBuffer sb = new StringBuffer();
		sb.append(year)
				.append("-")
				.append(month < 10 ? ("0" + Integer.toString(month)) : month)
				.append("-")
				.append(day < 10 ? ("0" + Integer.toString(day)) : day)
				.append(" ")
				.append(hour < 10 ? ("0" + Integer.toString(hour)) : hour)
				.append(":")
				.append(minute < 10 ? ("0" + Integer.toString(minute)) : minute)
				.append(":")
				.append(second < 10 ? ("0" + Integer.toString(second)) : second);

		return sb.toString();
	}

	// 解析上传GPS数据和警情数据
	public GpsAndAlarmDataView getGpsOrAlarmData(Tdevice tdevice,
			Message790 message) {
		GpsAndAlarmDataView gpsAndAlarmData = new GpsAndAlarmDataView();

		try {
			String[] data = (StringEncoding.unicodeToString2(new String(message.getDataBytes(),"UTF-8")))
					.split(",");

			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			// 上行数据类型 UP:GPS数据 ALARM:警情数据
			String dataType = message.getMethod();

			// 设备信息
			Integer did = tdevice.getId();
			String device_sn = tdevice.getDeviceSn();

			// 定位时间
			Date collect_datetime = df.parse(getDateTime(data[0], data[1],0));
			Date rcv_time = df.parse(getCurrentUtcDateTime());

			// 纬度 保留7位小数点（正负）
			double lat = Double.parseDouble(data[2]);

			// 经度 保留7位小数点（正负）
			double lng = Double.parseDouble(data[3]);

			// 定位精度 单位(米)
			Double range = Double.parseDouble(data[4]);
			Integer LBS_WIFI_Range = range.intValue();

			// 定位数据
			if (dataType.equals("UP") || dataType.equals("UP2")) {
				// 保存GPS数据
				TgpsData tgpsData = new TgpsData();
				tgpsData.setDid(did);
				tgpsData.setDeviceSn(device_sn);
				tgpsData.setCollectDatetime(collect_datetime);
				tgpsData.setRcvTime(rcv_time);
				tgpsData.setLat(lat);
				tgpsData.setLng(lng);
				tgpsData.setLbsWifiRange(LBS_WIFI_Range);

				// 0：未定位，1：2D定位，2:LBS基站定位，3：3D gps定位 10:wifi定位
				tgpsData.setGpsFlag("3");

				// 0:正常上传 1：补传
				tgpsData.setFlag("0");
				if (dataType.equals("UP2")) {
					tgpsData.setFlag("1");
				}

				tgpsData.setSpeed(0f);
				tgpsData.setDirection(0f);
				tgpsData.setAltitude(0);
				tgpsData.setLongitudeWay(0);
				tgpsData.setLatitudeWay(0);
				tgpsData.setSatelliteNum(5);
				tgpsData.setLocationId(0);
				tgpsData.setCellId(0);
				tgpsData.setMsgId("");
				tgpsData.setBattery("0");
				tgpsData.setSteps(0);
				tgpsData.setLbsWifiFlag((char) 0);
				tgpsData.setAccStatus((char) 0);
				tgpsData.setCalorie(0d);

				gpsAndAlarmData.setGpsData(tgpsData);
				gpsAndAlarmData.setDataType(0);
			}

			// 警情数据
			if (dataType.equals("ALARM")) {
				String deviceStatus = data[9];
				Integer alarmFlag = Integer.parseInt(
						deviceStatus.substring(2, 4), 16);

				boolean isSOS = (alarmFlag & 1) == 1 ? true : false;
				boolean isLowAattery = (alarmFlag & 2) == 2 ? true : false;
				boolean isTakeOff = (alarmFlag & 16) == 16 ? true : false;
				boolean isTumble = (alarmFlag & 32) == 32 ? true : false;

				// 警情类型 SOS:6 低电：18 手环拆除报警：35 跌倒报警：36
				Integer alarmType = 6;
				if (isLowAattery)
					alarmType = 18;

				if (isTakeOff)
					alarmType = 35;

				if (isTumble)
					alarmType = 36;

				if (isSOS)
					alarmType = 6;

				// 保存警情数据
				TalarmData talarmData = new TalarmData();
				talarmData.setDid(did);
				talarmData.setDeviceSn(device_sn);
				talarmData.setCollectDatetime(collect_datetime);
				talarmData.setRcvTime(rcv_time);
				talarmData.setType(alarmType);
				talarmData.setLat(lat);
				talarmData.setLng(lng);
				talarmData.setSpeed(0f);
				talarmData.setDirection(0f);
				talarmData.setSatelliteNum(5);
				talarmData.setLocationId(0);
				talarmData.setCellId(0);
				talarmData.setGpsFlag("3");
				talarmData.setFlag("0");
				talarmData.setBattery("0");

				gpsAndAlarmData.setAlarmData(talarmData);
				gpsAndAlarmData.setDataType(1);
			}

			return gpsAndAlarmData;
		} catch (Exception e) {
			LogManager.info("解析GPS或警情信息异常，异常信息为:");
			LogManager.info(e.toString(), e);

			return null;
		}
	}

	// 根据WIFI或LBS获取GPS
	public Map<Integer, Object> getGps(Tdevice tdevice, UDMessage ud) {
		Map<Integer, Object> map = new HashMap<Integer, Object>();

		// 调用远程接口进行WIFI解析或LBS解析，解析完成对以下变量赋值
		List<GSMBSItem> listGSM = ud.getGsmBSGrpup().getBsItems();
		List<WifiItem> listWifi = ud.getWifiGroup().getBsItems();
		Collections.sort(listGSM);
		Collections.sort(listWifi);

		StringBuilder cellTowers = new StringBuilder();
		StringBuilder wifiAccessPoints = new StringBuilder();

		String connCountry = tdevice.getConnCountry();
		if (connCountry.equals("CN")) // 国内用高德地图
		{
			// 获取基站列表
			String currentGSM = "";
			if (listGSM.size() > 0) {
				currentGSM = ud.getGsmBSGrpup().getMcc() + ","
						+ ud.getGsmBSGrpup().getMnc() + ","
						+ listGSM.get(0).getAreaCode() + ","
						+ listGSM.get(0).getBscode() + ","
						+ listGSM.get(0).getBsSignalStrength();

				for (int i = 1; i <= listGSM.size() - 1; i++) {
					cellTowers.append(ud.getGsmBSGrpup().getMcc()).append(",")
							.append(ud.getGsmBSGrpup().getMnc()).append(",")
							.append(listGSM.get(i).getAreaCode()).append(",")
							.append(listGSM.get(i).getBscode()).append(",")
							.append(listGSM.get(i).getBsSignalStrength())
							.append("|");
				}
			}
			String cellTowerStr = cellTowers.toString();
			if (!cellTowerStr.isEmpty() && cellTowerStr.endsWith("|")) {
				cellTowerStr = cellTowerStr.substring(0,
						cellTowerStr.length() - 1);
			}

			// 获取Wifi接入点列表
			String currentWifi = "";
			if (listWifi.size() > 0) {
				currentWifi = listWifi.get(0).getMacaddr() + ","
						+ listWifi.get(0).getSignalStrength() + ","
						+ listWifi.get(0).getName();
				for (int i = 1; i <= listWifi.size() - 1; i++) {
					wifiAccessPoints.append(listWifi.get(i).getMacaddr())
							.append(",")
							.append(listWifi.get(i).getSignalStrength())
							.append(",").append(listWifi.get(i).getName())
							.append("|");
				}
			}
			String wifiAccessPointsStr = wifiAccessPoints.toString();
			if (!wifiAccessPointsStr.isEmpty()
					&& wifiAccessPointsStr.endsWith("|")) {
				wifiAccessPointsStr = wifiAccessPointsStr.substring(0,
						wifiAccessPointsStr.length() - 1);
			}

			Integer accesstype = 0;
			if (listWifi.size() > 2) {
				accesstype = 1;
			}

			map.put(1, accesstype);
			map.put(2, tdevice.getDeviceSn());
			map.put(3, 0);
			map.put(4, "GSM");
			map.put(5, currentGSM);
			map.put(6, cellTowerStr);
			map.put(7, currentWifi);
			map.put(8, wifiAccessPointsStr);
			map.put(9, "htbdgwserver");
		} else // 其他用GOOGLE地图
		{
			// 获取基站列表
			if (listGSM.size() > 0) {
				for (int i = 0; i <= listGSM.size() - 1; i++) {
					cellTowers.append(listGSM.get(i).getBscode()).append(",")
							.append(listGSM.get(i).getAreaCode()).append(",")
							.append(ud.getGsmBSGrpup().getMcc()).append(",")
							.append(ud.getGsmBSGrpup().getMnc()).append(",")
							.append("0").append(",")
							.append(listGSM.get(i).getBsSignalStrength())
							.append(",").append(ud.getGsmBSGrpup().getTa())
							.append(";");
				}
			}
			String cellTowerStr = cellTowers.toString();
			if (!cellTowerStr.isEmpty() && cellTowerStr.endsWith(";")) {
				cellTowerStr = cellTowerStr.substring(0,
						cellTowerStr.length() - 1);
			}

			// 获取Wifi接入点列表
			if (listWifi.size() > 0) {
				for (int i = 0; i <= listWifi.size() - 1; i++) {
					wifiAccessPoints.append(listWifi.get(i).getMacaddr())
							.append(",")
							.append(listWifi.get(i).getSignalStrength())
							.append(",").append("0").append(",").append("0")
							.append(",").append("0").append(";");
				}
			}
			String wifiAccessPointsStr = wifiAccessPoints.toString();
			if (!wifiAccessPointsStr.isEmpty()
					&& wifiAccessPointsStr.endsWith(";")) {
				wifiAccessPointsStr = wifiAccessPointsStr.substring(0,
						wifiAccessPointsStr.length() - 1);
			}

			map.put(1, ud.getGsmBSGrpup().getMcc());
			map.put(2, ud.getGsmBSGrpup().getMnc());
			map.put(3, "GSM");
			map.put(4, "");
			map.put(5, cellTowerStr);
			map.put(6, wifiAccessPointsStr);
			map.put(7, "htbdgwserver");
		}

		return map;
	}

	/**
	 * Time 获取服务器端时间
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_TIME(String deviceSession, Message790 message,
			String deviceName) {
		RPCResult result = RPCResult.failed();

		try {
			Tdevice device = null;
			if (deviceMap.containsKey(message.getDeviceSn())) {
				device = deviceMap.get(message.getDeviceSn());
			} else {
				device = deviceService.select(message.getDeviceSn());
			}

			Integer tz = device.getTimezone();
			String currentUtcDate = getCurrentUtcDateTime();
			String currentLocalDate = DateTimeUtil
					.utc2Local(currentUtcDate, tz);

			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Date localDate = df.parse(currentLocalDate);

			Long second = localDate.getTime() / 1000;
			String timestamp = second.toString();

			result = gsConsumer.pushMessageToDevice(true, deviceName,
					deviceSession, message.buildResponse("Time," + timestamp)
							.getBytes());
			if(result.getRpcResult() != null && (Integer)result.getRpcResult() > 0)
			{
				result = RPCResult.success();
			} else {
				result = RPCResult.failed();
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return result;
	}

	/**
	 * LGZONE 获取服务器端时间
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_LGZONE(String deviceSession, Message790 message,
			String deviceName) {
		RPCResult result = RPCResult.failed();

		try {
			Tdevice device = null;
			if (deviceMap.containsKey(message.getDeviceSn())) {
				device = deviceMap.get(message.getDeviceSn());
			} else {
				device = deviceService.select(message.getDeviceSn());
			}

			Float tzSecond = Float.parseFloat(device.getTimezone().toString());
			Float tz = tzSecond / 3600;
			String currentUtcDate = getCurrentUtcDateTime();
			String currentLocalDate = DateTimeUtil.utc2Local(currentUtcDate,
					device.getTimezone());
			String[] dateArr = currentLocalDate.split("\\ ", 2);

			String cmd = "LGZONE," + tz + "," + dateArr[1] + "," + dateArr[0];
			result = gsConsumer.pushMessageToDevice(true, deviceName,
					deviceSession, message.buildResponse(cmd).getBytes());
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return result;
	}

	/**
	 * WEA 获取天气
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_WEA(String deviceSession, Message790 message,
			String deviceName) {
		RPCResult result = RPCResult.success();
		result.setRpcResult(message.buildResponse("WEA"));
		return result;
	}
	
	/**
	 * 获取群成员列表
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_MEMBERS(String deviceSession, Message790 message,
			String deviceName) {
		RPCResult result = RPCResult.success();
		result.setRpcResult(message.buildResponse("MEMBERS,"));
		return result;
	}

	/**
	 * 远程拍照 终端上传照片
	 * 
	 * @param deviceSession
	 * @param message
	 * @param deviceName
	 * @return
	 */
	public RPCResult call_IMG(String deviceSession, Message790 message,
			String deviceName) {
		try {
			byte[] data = message.getDataBytes();
			byte[] type = DataUtil.bytesFromBytes(data, 0, (byte) 0,
					Consts.TAG_Comma, false);
			byte[] time = DataUtil.bytesFromBytes(data, type.length,
					Consts.TAG_Comma, Consts.TAG_Comma, false);
			byte[] img = DataUtil.bytesFromBytes(data, type.length
					+ time.length + 1, Consts.TAG_Comma, (byte) 0, false);

			if (img != null && img.length > 1) {
				img = Message77X.from77XAudio(img);

				String resServerURL = env.getProperty("resource.service.url");
				saveDevicePhoto(message.getDeviceSn(), img, resServerURL,
						devicePhotoService);
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return null;
	}

	/**
	 * TKQ 终端请求录音下发
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_TKQ(String deviceSession, Message790 message,
			String deviceName) {
		RPCResult result = RPCResult.success();
		result.setRpcResult(message.buildResponse("TKQ"));
		return result;
	}

	/**
	 * TK 终端发送
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_TK(String deviceSession, Message790 message,
			String deviceName) {
		RPCResult result = RPCResult.failed();

		try {
			byte[] data = message.getDataBytes();

			if (data != null && data.length > 1) {
				data = Message790.from77XAudio(data);
				LogManager.info("设备上传语音数据为："
						+ StringEncoding.bytesToHexString(data));
				// 获取文件夹路径
				String dirPath = env.getProperty("device.voice.dir.path");

				// 获取文件名称
				SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmssSSS");
				Random rnd = new Random();
				Integer mark = rnd.nextInt();

				String fileName = message.getDeviceSn() + "_"
						+ df.format(new Date()) + "_" + mark.toString()
						+ ".amr";

				// 保存文件
				FileUtils.saveFile(data, dirPath, fileName);

				// 全路径
				String filePath = dirPath + File.separator + fileName;
				File file = new File(filePath);

				String base64str = FileUtils.encodeBase64File(filePath).trim()
						.replaceAll("[\\t\\n\\r]", "");
				Long duration = FileUtils.getAmrDuration(file);

				VoiceMsg voiceMsg = new VoiceMsg();
				voiceMsg.setDeviceSn(message.getDeviceSn());
				voiceMsg.setDuration(duration);
				voiceMsg.setContent(base64str);
				result = taskServiceConsumer.pushVoiceMsg(voiceMsg);

				LogManager.info("设备{}推送语音数据,状态：{},结果：{}",
						message.getDeviceSn(), result.getRpcErrCode(),
						result.getRpcErrMsg());
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (result.getRpcErrCode() == 0) {
			result.setRpcResult(message.buildResponse("TK,1"));
		} else {
			result.setRpcResult(message.buildResponse("TK,0"));
		}

		return result;
	}

	/**
	 * 设置电子围栏
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setenclosure(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.success();
		Map<String, String> map = new HashMap<String, String>();

		try {
			if (param != null && param.containsKey("areaid")
					&& param.get("areaid").length > 0) {
				// 更新本地围栏缓存
				String areaid = param.get("areaid")[0];

				TareaInfo tareaInfo = new TareaInfo();
				tareaInfo.setAreaid(Integer.parseInt(areaid));
				tareaInfo.setEnabled(param.containsKey("enabled") ? Integer
						.parseInt(param.get("enabled")[0]) : 0);
				tareaInfo.setLat(param.containsKey("lat") ? Double
						.parseDouble(param.get("lat")[0]) : 0);
				tareaInfo.setLng(param.containsKey("lng") ? Double
						.parseDouble(param.get("lng")[0]) : 0);
				tareaInfo.setRadius(param.containsKey("radius") ? Integer
						.parseInt(param.get("radius")[0]) : 0);
				tareaInfo.setIsOut(param.containsKey("is_out") ? Integer
						.parseInt(param.get("is_out")[0]) : 0);
				tareaInfo.setType(param.containsKey("type") ? Integer
						.parseInt(param.get("type")[0]) : 0);

				List<TareaInfo> list = null;
				if (!areaMap.containsKey(deviceSn)) {
					if (tareaInfo.getEnabled() == 1) {
						list = new ArrayList<TareaInfo>();
						list.add(tareaInfo);
					}
				} else {
					list = areaMap.get(deviceSn);
					boolean inContains = false;
					for (int i = 0; i <= list.size() - 1; i++) {
						if (list.get(i).getAreaid().intValue() == tareaInfo.getAreaid().intValue()) {
							inContains = true;
							if (tareaInfo.getEnabled() == 1) {
								list.set(i, tareaInfo);
							} else {
								list.remove(i);
							}

							break;
						}
					}

					if (!inContains && tareaInfo.getEnabled() == 1) {
						list.add(tareaInfo);
					}
				}

				if (list != null)
					areaMap.put(deviceSn, list);
			}

			map.put("result", "0");
		} catch (Exception e) {
			ret = RPCResult.failed();
			LogManager.exception(e.getMessage(), e);

			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 取消电子围栏
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult deleteenclosure(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.success();
		Map<String, String> map = new HashMap<String, String>();

		try {
			if (param != null && param.containsKey("areaid")
					&& param.get("areaid").length > 0) {
				// 更新本地围栏缓存
				String areaid = param.get("areaid")[0];

				TareaInfo tareaInfo = new TareaInfo();
				tareaInfo.setAreaid(Integer.parseInt(areaid));
				tareaInfo.setEnabled(param.containsKey("enabled") ? Integer
						.parseInt(param.get("enabled")[0]) : 0);
				tareaInfo.setLat(param.containsKey("lat") ? Double
						.parseDouble(param.get("lat")[0]) : 0);
				tareaInfo.setLng(param.containsKey("lng") ? Double
						.parseDouble(param.get("lng")[0]) : 0);
				tareaInfo.setRadius(param.containsKey("radius") ? Integer
						.parseInt(param.get("radius")[0]) : 0);
				tareaInfo.setIsOut(param.containsKey("is_out") ? Integer
						.parseInt(param.get("is_out")[0]) : 0);
				tareaInfo.setType(param.containsKey("type") ? Integer
						.parseInt(param.get("type")[0]) : 0);

				List<TareaInfo> list = null;
				if (!areaMap.containsKey(deviceSn)) {
					if (tareaInfo.getEnabled() == 1) {
						list = new ArrayList<TareaInfo>();
						list.add(tareaInfo);
					}
				} else {
					list = areaMap.get(deviceSn);
					boolean inContains = false;
					for (int i = 0; i <= list.size() - 1; i++) {
						if (list.get(i).getAreaid() == tareaInfo.getAreaid()) {
							inContains = true;
							if (tareaInfo.getEnabled() == 1) {
								list.set(i, tareaInfo);
							} else {
								list.remove(i);
							}

							break;
						}
					}

					if (!inContains && tareaInfo.getEnabled() == 1) {
						list.add(tareaInfo);
					}
				}

				if (list != null)
					areaMap.put(deviceSn, list);
			}

			map.put("result", "0");
		} catch (Exception e) {
			ret = RPCResult.failed();
			LogManager.exception(e.getMessage(), e);

			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * GPS定位时间间隔设置
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setgpsinterval(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			if (param != null && param.containsKey("gpsinterval")
					&& param.get("gpsinterval").length > 0) {
				DeviceSessionMap sessionmap = deviceSessionMapService
						.getByDeviceSn(deviceSn);
				if (sessionmap == null) {
					map.put("result", "4");
					ret.setRpcResult(JSON.toJSONString(map,
							SerializerFeature.WriteDateUseDateFormat,
							SerializerFeature.DisableCircularReferenceDetect,
							SerializerFeature.WriteMapNullValue,
							SerializerFeature.DisableCheckSpecialChar));

					return ret;
				}

				String deviceSession = sessionmap.getDeviceSession();
				String gpsinterval = param.get("gpsinterval")[0];

				Message790 message = new Message790();
				message.setFactoryName("SG");
				message.setDeviceSn(deviceSn);
				ret = gsConsumer.pushMessageToDevice(true, deviceName,
						deviceSession,
						message.buildResponse("UPLOAD," + gpsinterval)
								.getBytes());
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 获取最后设置最后一次上传的位置信息
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult getlastgps(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		GpsDataView gpsdataview = new GpsDataView();

		TgpsData tgpsdata = gpsDataMap.get(deviceSn);
		if (tgpsdata != null) {
			SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

			// 0：成功/确认；1：失败；2：消息有误
			gpsdataview.setResult("0");

			gpsdataview.setBattery(tgpsdata.getBattery());
			gpsdataview.setCalorie(tgpsdata.getCalorie().toString());

			// 0：熄火，1：运行，2:怠速
			gpsdataview.setCarstatus("1");
			gpsdataview.setDirection(tgpsdata.getDirection().toString());
			gpsdataview.setFlag(tgpsdata.getFlag());

			// 0：未定位，1：2D定位，2:LBS定位，3：3D定位
			gpsdataview.setGps_flag(tgpsdata.getGpsFlag());
			gpsdataview.setGpstime(df.format(tgpsdata.getCollectDatetime()));
			gpsdataview.setLat(tgpsdata.getLat().toString());
			gpsdataview
					.setLBS_WIFI_Range(tgpsdata.getLbsWifiRange().toString());
			gpsdataview.setLng(tgpsdata.getLng().toString());

			// 在线状态 0:不在线 1：在线
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				gpsdataview.setOnline("0");
			} else {
				gpsdataview.setOnline("1");
			}

			gpsdataview.setSatellite_num(tgpsdata.getSatelliteNum().toString());
			gpsdataview.setSpeed(tgpsdata.getSpeed().toString());
			gpsdataview.setSteps(tgpsdata.getSteps().toString());
		} else {
			gpsdataview.setResult("1");
		}

		RPCResult result = RPCResult.success();
		result.setRpcResult(JSON.toJSONString(gpsdataview,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));

		return result;
	}

	/**
	 * 设备点名
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult devicenaming(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			String deviceSession = sessionmap.getDeviceSession();
			Message790 message = new Message790();
			message.setFactoryName("SG");
			message.setDeviceSn(deviceSn);

			ret = gsConsumer.pushMessageToDevice(true, deviceName,
					deviceSession, message.buildResponse("CR").getBytes());
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设备重置
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult devicereset(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			String deviceSession = sessionmap.getDeviceSession();
			Message790 message = new Message790();
			message.setFactoryName("SG");
			message.setDeviceSn(deviceSn);

			ret = gsConsumer.pushMessageToDevice(true, deviceName,
					deviceSession, message.buildResponse("FACTORY").getBytes());
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置绑定解绑(解绑只要设备在线)
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult bindingdevice(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			if (param != null && param.containsKey("optype")
					&& param.get("optype").length > 0) {
				String optype = param.get("optype")[0];
				if (optype.equals("0")) {
					String deviceSession = sessionmap.getDeviceSession();
					Message790 message = new Message790();
					message.setFactoryName("SG");
					message.setDeviceSn(deviceSn);

					ret = gsConsumer.pushMessageToDevice(true, deviceName,
							deviceSession, message.buildResponse("FACTORY")
									.getBytes());
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * map模糊取值
	 * 
	 * @param key
	 * @param map
	 * @return
	 */
	public static List<String[]> getLikeByMap(String key,
			Map<String, String[]> map) {
		List<String[]> list = new ArrayList<String[]>();

		for (Map.Entry<String, String[]> entry : map.entrySet()) {
			if (entry.getKey().indexOf(key) != -1) {
				list.add(entry.getValue());
			}
		}

		return list;
	}

	/**
	 * TODO:需进行验证测试，看是否需要占坑 设置电话本信息到远程
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setphonebook(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			String PHL = "";
			String SOS = "";

			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			List<String[]> list = getLikeByMap("tel", param);
			if (list.size() >= 1) {
				int sosCount = 0;
				for (int i = 0; i <= list.size() - 1; i++) {
					String nickname = "";
					String tel = "";

					String nicknameAndTel = list.get(i)[0];
					if (nicknameAndTel.indexOf(":") != -1) {
						nickname = nicknameAndTel.split("\\:", 2)[0];
						tel = nicknameAndTel.split("\\:", 2)[1];

						if (tel != null && !tel.isEmpty()) {
							nickname = StringEncoding
									.stringToUnicode2(nickname);
						} else {
							nickname = "";
						}
					}

					PHL += tel + "," + nickname + ",";

					// SOS号码取头两个
					if (!tel.isEmpty() && sosCount < 2) {
						SOS += tel + ",";
						sosCount++;
					}
				}

				// 电话本指令
				PHL = "PHL," + PHL.substring(0, PHL.length() - 1);

				// SOS指令
				SOS = "SOS," + SOS.substring(0, SOS.length() - 1);
			}

			String deviceSession = sessionmap.getDeviceSession();
			Message790 message = new Message790();
			message.setFactoryName("SG");
			message.setDeviceSn(deviceSn);

			// 设置电话本头五个
			RPCResult retSetPhonebook = gsConsumer.pushMessageToDevice(true,
					deviceName, deviceSession, message.buildResponse(PHL)
							.getBytes());

			// 设置SOS号码
			RPCResult retSetSos = gsConsumer.pushMessageToDevice(true,
					deviceName, deviceSession, message.buildResponse(SOS)
							.getBytes());

			if (retSetPhonebook.getRpcErrCode() == 0
					&& retSetSos.getRpcErrCode() == 0) {
				ret = RPCResult.success();
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置免打扰
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setcoursedisabletime(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			if (param != null && param.containsKey("status")
					&& param.get("status").length > 0
					&& param.containsKey("starttime1")
					&& param.get("starttime1").length > 0
					&& param.containsKey("endtime1")
					&& param.get("endtime1").length > 0
					&& param.containsKey("starttime2")
					&& param.get("starttime2").length > 0
					&& param.containsKey("endtime2")
					&& param.get("endtime2").length > 0) {
				String SILENCETIME = "SILENCETIME,,,,";

				String status = param.get("status")[0];
				if (status.equals("1")) {
					String starttime1 = param.get("starttime1")[0];
					String endtime1 = param.get("endtime1")[0];
					String starttime2 = param.get("starttime2")[0];
					String endtime2 = param.get("endtime2")[0];

					/*
					SILENCETIME = "SILENCETIME," + starttime1 + "-" + endtime1
							+ "," + starttime2 + "-" + endtime2
							+ ",00:00-00:00,00:00-00:00";
					*/
					SILENCETIME = "SILENCETIME," + starttime1 + "-" + endtime1
							+ "," + starttime2 + "-" + endtime2;
				}

				String deviceSession = sessionmap.getDeviceSession();
				Message790 message = new Message790();
				message.setFactoryName("SG");
				message.setDeviceSn(deviceSn);

				// 设置免打扰时间段
				ret = gsConsumer.pushMessageToDevice(true, deviceName,
						deviceSession, message.buildResponse(SILENCETIME)
								.getBytes());
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置设备时区、语言到远程
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult settzandlang(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			if (param != null && param.containsKey("tz")
					&& param.get("tz").length > 0 && param.containsKey("lang")
					&& param.get("lang").length > 0) {
				String tz = param.get("tz")[0];
				String lang = param.get("lang")[0];

				float timezone = Float.parseFloat(tz);
				timezone = timezone / 3600;

				String LZ = "LZ," + lang + "," + timezone;

				String deviceSession = sessionmap.getDeviceSession();
				Message790 message = new Message790();
				message.setFactoryName("SG");
				message.setDeviceSn(deviceSn);

				// 设置时区语言
				ret = gsConsumer.pushMessageToDevice(true, deviceName,
						deviceSession, message.buildResponse(LZ).getBytes());
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置闹钟信息
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setclock(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			if (param != null && param.containsKey("clock1")
					&& param.get("clock1").length > 0
					&& param.containsKey("clock2")
					&& param.get("clock2").length > 0
					&& param.containsKey("clock3")
					&& param.get("clock3").length > 0) {
				String clock1 = param.get("clock1")[0];
				String clock2 = param.get("clock2")[0];
				String clock3 = param.get("clock3")[0];

				String REMIND = "REMIND," + clock1 + "," + clock2 + ","
						+ clock3;

				String deviceSession = sessionmap.getDeviceSession();
				Message790 message = new Message790();
				message.setFactoryName("SG");
				message.setDeviceSn(deviceSn);

				// 设置闹钟
				ret = gsConsumer
						.pushMessageToDevice(true, deviceName, deviceSession,
								message.buildResponse(REMIND).getBytes());
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置监听
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setmonitor(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			if (param != null && param.containsKey("centertel")
					&& param.get("centertel").length > 0) {
				String CENTER = "CENTER," + param.get("centertel")[0];

				String deviceSession = sessionmap.getDeviceSession();
				Message790 message = new Message790();
				message.setFactoryName("SG");
				message.setDeviceSn(deviceSn);

				// 设置中心号码
				RPCResult retSetCenter = gsConsumer.pushMessageToDevice(true,
						deviceName, deviceSession, message
								.buildResponse(CENTER).getBytes());

				// 设置监听
				RPCResult retSetMonitor = gsConsumer.pushMessageToDevice(true,
						deviceName, deviceSession,
						message.buildResponse("MONITOR").getBytes());

				if (retSetCenter.getRpcErrCode() == 0
						&& retSetMonitor.getRpcErrCode() == 0) {
					ret = RPCResult.success();
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置关机
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setturnoffdevice(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			String deviceSession = sessionmap.getDeviceSession();
			Message790 message = new Message790();
			message.setFactoryName("SG");
			message.setDeviceSn(deviceSn);

			// 设置关机
			ret = gsConsumer
					.pushMessageToDevice(true, deviceName, deviceSession,
							message.buildResponse("POWEROFF").getBytes());
			gsConsumer.closeDevice(true, deviceName, deviceSession);

			// 跟新最后位置表的在线状态
			gpsDataLastService.updateOnlineStatus(deviceSn, 0);

			// 关机前变更设备在线状态为下线
			LogManager.info("设备:{}下线，推送在线状态", deviceSn);
			PushInfo pushInfo = new PushInfo();
			pushInfo.setEquipId(deviceSn);
			pushInfo.setOnlinestatus("0");
			pushInfo.setMsgType(2);

			List<UserConn> userConnList = userService
					.getUserDeviceInfo(pushInfo.getEquipId());
			if (null != userConnList && userConnList.size() > 0) {
				for (UserConn userConn : userConnList) {
					if (StringUtils.isEmpty(userConn.getToken())) // 只推送安卓
					{
						RPCResult pushRet = taskServiceConsumer.pushMsg(
								pushInfo, userConn);
						LogManager.info("设备:{}下线，推送在线状态:{}", deviceSn,
								pushRet.getRpcErrCode() == 0 ? "成功" : "失败");
					}
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置设备重启
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setrebootdevice(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			String deviceSession = sessionmap.getDeviceSession();
			Message790 message = new Message790();
			message.setFactoryName("SG");
			message.setDeviceSn(deviceSn);

			// 设置关机
			ret = gsConsumer.pushMessageToDevice(true, deviceName,
					deviceSession, message.buildResponse("RESET").getBytes());
			gsConsumer.closeDevice(true, deviceName, deviceSession);

			// 跟新最后位置表的在线状态
			gpsDataLastService.updateOnlineStatus(deviceSn, 0);

			// 关机前变更设备在线状态为下线
			LogManager.info("设备:{}下线，推送在线状态", deviceSn);
			PushInfo pushInfo = new PushInfo();
			pushInfo.setEquipId(deviceSn);
			pushInfo.setOnlinestatus("0");
			pushInfo.setMsgType(2);

			List<UserConn> userConnList = userService
					.getUserDeviceInfo(pushInfo.getEquipId());
			if (null != userConnList && userConnList.size() > 0) {
				for (UserConn userConn : userConnList) {
					if (StringUtils.isEmpty(userConn.getToken())) // 只推送安卓
					{
						RPCResult pushRet = taskServiceConsumer.pushMsg(
								pushInfo, userConn);
						LogManager.info("设备:{}下线，推送在线状态:{}", deviceSn,
								pushRet.getRpcErrCode() == 0 ? "成功" : "失败");
					}
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置找手表
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setfinddevice(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			String deviceSession = sessionmap.getDeviceSession();
			Message790 message = new Message790();
			message.setFactoryName("SG");
			message.setDeviceSn(deviceSn);

			// 设置找手表
			ret = gsConsumer.pushMessageToDevice(true, deviceName,
					deviceSession, message.buildResponse("FIND").getBytes());

		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置记步开关
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setwalkstatus(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			if (param != null && param.containsKey("walkstatus")
					&& param.get("walkstatus").length > 0) {
				String walkstatus = "PEDO,0";

				String PEDO = param.get("walkstatus")[0];
				if (PEDO.equals("1")) {
					walkstatus = "PEDO,1";
				}

				String deviceSession = sessionmap.getDeviceSession();
				Message790 message = new Message790();
				message.setFactoryName("SG");
				message.setDeviceSn(deviceSn);

				// 设置记步开关
				RPCResult retSetPEDO = gsConsumer.pushMessageToDevice(true,
						deviceName, deviceSession,
						message.buildResponse(walkstatus).getBytes());

				if (retSetPEDO.getRpcErrCode() == 0) {
					ret = RPCResult.success();
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 发送文本消息
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult sendtxtmessage(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			if (param != null && param.containsKey("txtmessage")
					&& param.get("txtmessage").length > 0) {
				String txtmessage = param.get("txtmessage")[0];

				if (txtmessage != null && !txtmessage.isEmpty()) {
					txtmessage = StringEncoding.stringToUnicode2(txtmessage);
					String MESSAGE = "MESSAGE," + txtmessage;

					String deviceSession = sessionmap.getDeviceSession();
					Message790 message = new Message790();
					message.setFactoryName("SG");
					message.setDeviceSn(deviceSn);

					// 发送短语到设备
					ret = gsConsumer.pushMessageToDevice(true, deviceName,
							deviceSession, message.buildResponse(MESSAGE)
									.getBytes());
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置情景模式
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setprofile(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			if (param != null && param.containsKey("profile")
					&& param.get("profile").length > 0) {
				String profile = "profile," + param.get("profile")[0];

				String deviceSession = sessionmap.getDeviceSession();
				Message790 message = new Message790();
				message.setFactoryName("SG");
				message.setDeviceSn(deviceSn);

				// 设置情景模式
				ret = gsConsumer.pushMessageToDevice(true, deviceName,
						deviceSession, message.buildResponse(profile)
								.getBytes());
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 设置警情开关
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult setalarmsetting(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			if (param != null 
					&& param.containsKey("sos")
					&& param.get("sos").length > 0
					&& param.containsKey("takeOff")
					&& param.get("takeOff").length > 0) {
				String SOSSMS = "SOSSMS,0";
				String REMOVE = "REMOVE,0";
				String REMOVESMS = "REMOVESMS,0";

				String sos = param.get("sos")[0];
				if (sos.equals("1")) {
					SOSSMS = "SOSSMS,1";
				}

				String takeOff = param.get("takeOff")[0];
				if (takeOff.equals("1")) {
					REMOVE = "REMOVE,1";
				}

				String deviceSession = sessionmap.getDeviceSession();
				Message790 message = new Message790();
				message.setFactoryName("SG");
				message.setDeviceSn(deviceSn);

				// 设置sos短信开关
				RPCResult retSetSOSSMS = gsConsumer.pushMessageToDevice(true,
						deviceName, deviceSession, message
								.buildResponse(SOSSMS).getBytes());

				// 设置脱落报警开关
				RPCResult retSetREMOVE = gsConsumer.pushMessageToDevice(true,
						deviceName, deviceSession, message
								.buildResponse(REMOVE).getBytes());

				// 设置脱落短信开关
				RPCResult retSetREMOVESMS = gsConsumer.pushMessageToDevice(
						true, deviceName, deviceSession,
						message.buildResponse(REMOVESMS).getBytes());

				if (retSetSOSSMS.getRpcErrCode() == 0
						&& retSetREMOVESMS.getRpcErrCode() == 0
						&& retSetREMOVE.getRpcErrCode() == 0) {
					ret = RPCResult.success();
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 发送语音消息
	 * 
	 * @param deviceSn
	 * @param param
	 * @return
	 */
	public RPCResult sendvoicemessage(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			if (param != null && param.containsKey("voicemessage")
					&& param.get("voicemessage").length > 0) {
				String voicemessage = param.get("voicemessage")[0];

				byte[] data = StringEncoding.hexStringtoBytes(voicemessage);
				data = Message790.to77XAudio(data);

				String deviceSession = sessionmap.getDeviceSession();
				Message790 message = new Message790();
				message.setFactoryName("SG");
				message.setDeviceSn(deviceSn);

				// 发送短语到设备
				ret = gsConsumer.pushMessageToDevice(true, deviceName,
						deviceSession, message.buildBytesResponse("TK", data));
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));

		return ret;
	}

	/**
	 * 远程拍照指令
	 * 
	 * @param deviceSn
	 * @param deviceName
	 * @param param
	 * @return
	 */
	public RPCResult setimg(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}

			String deviceSession = sessionmap.getDeviceSession();
			Message790 message = new Message790();
			message.setFactoryName("SG");
			message.setDeviceSn(deviceSn);

			ret = gsConsumer
					.pushMessageToDevice(true, deviceName, deviceSession,
							message.buildResponse("rcapture").getBytes());
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * 翻转检测时间段设置
	 * 
	 * @param deviceSn
	 * @param deviceName
	 * @param param
	 * @return
	 */
	public RPCResult setturn(String deviceSn, String deviceName,
			Map<String, String[]> param) {
		RPCResult ret = RPCResult.failed();
		Map<String, String> map = new HashMap<String, String>();

		try {
			DeviceSessionMap sessionmap = deviceSessionMapService
					.getByDeviceSn(deviceSn);
			if (sessionmap == null) {
				map.put("result", "4");
				ret.setRpcResult(JSON.toJSONString(map,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue,
						SerializerFeature.DisableCheckSpecialChar));

				return ret;
			}
			// 1 打开 0关闭
			String intArray[] = { "0" };
			String t[] = { "17:00-18:00" };

			param.put("status", intArray);
			param.put("time", t);
			if (param != null && param.containsKey("status")
					&& param.get("status").length > 0
					&& param.containsKey("time")
					&& param.get("time").length > 0) {
				String SILENCETIME = "SLEEPTIME,00:00-00:00";

				String status = param.get("status")[0];
				if (status.equals("1")) {
					String time = param.get("time")[0];
					SILENCETIME = "SLEEPTIME," + time;
				}

				String deviceSession = sessionmap.getDeviceSession();
				Message790 message = new Message790();
				message.setFactoryName("SG");
				message.setDeviceSn(deviceSn);

				// 设置免打扰时间段
				ret = gsConsumer.pushMessageToDevice(true, deviceName,
						deviceSession, message.buildResponse(SILENCETIME)
								.getBytes());
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		if (ret.getRpcErrCode() == 0) {
			map.put("result", "0");
		} else {
			map.put("result", "1");
		}

		ret.setRpcResult(JSON.toJSONString(map,
				SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect,
				SerializerFeature.WriteMapNullValue,
				SerializerFeature.DisableCheckSpecialChar));
		return ret;
	}

	/**
	 * PP 碰碰交友
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_PP(String deviceSession, Message790 message,
			String deviceName) {
		RPCResult result = RPCResult.success();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try {
			String deviceSn = message.getDeviceSn();
			Tdevice tdevice = deviceService.select(deviceSn);

			String data = new String(message.getDataBytes(), "UTF-8");
			if (data.indexOf(",") != -1) {
				String[] dataArr = data.split(",");
				// 定位时间
				Date collect_datetime = df.parse(getDateTime(dataArr[0],
						dataArr[1], tdevice.getTimezone() / 3600));
				// 纬度 保留7位小数点（正负）
				double lat = Double.parseDouble(dataArr[2]);
				// 经度 保留7位小数点（正负）
				double lng = Double.parseDouble(dataArr[3]);
				// 定位精度 单位(米)
				Integer LBS_WIFI_Range = Integer.parseInt(dataArr[4]);

				// DeviceFriendData deviceFriendData = new DeviceFriendData();

				pPFriendTask.addToPPList(deviceName, message.getFactoryName(),
						deviceSn, deviceSession, System.currentTimeMillis(),
						lat, lng);
			}

			return result;
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);

			return RPCResult.failed();
		}
	}
}