package com.htdz.device.handler.HT77X;

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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
import com.htdz.common.utils.HttpUtils.ByteArrayBodyPair;
import com.htdz.common.utils.HttpUtils.FileBodyPair;
import com.htdz.common.utils.HttpUtils.InputStreamBodyPair;
import com.htdz.common.utils.HttpUtils.StringBodyPair;
import com.htdz.common.utils.StringEncoding;
import com.htdz.db.service.DevicePhotoService;
import com.htdz.db.service.DeviceService;
import com.htdz.db.service.DeviceSessionMapService;
import com.htdz.db.service.DeviceTurnService;
import com.htdz.db.service.GpsDataLastService;
import com.htdz.db.service.TAlarmSettingService;
import com.htdz.db.service.TAreaInfoService;
import com.htdz.db.service.TPhoneBookService;
import com.htdz.db.service.TalarmDataService;
import com.htdz.db.service.TgpsDataService;
import com.htdz.db.service.UserService;
import com.htdz.def.data.RPCResult;
import com.htdz.def.dbmodel.DevicePhoto;
import com.htdz.def.dbmodel.DeviceSessionMap;
import com.htdz.def.dbmodel.DeviceTurn;
import com.htdz.def.dbmodel.GpsAndAlarmDataView;
import com.htdz.def.dbmodel.GpsDataView;
import com.htdz.def.dbmodel.TAlarmSetting;
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

@Component
public class Device77XProtocol extends DeviceBaseProtocol {
	// 缓存当前链接设备信息
	public static Map<String, Tdevice> deviceMap = new ConcurrentHashMap<String, Tdevice>();
	// 缓存设备区域信息
	public static Map<String, List<TareaInfo>> areaMap = new ConcurrentHashMap<String, List<TareaInfo>>();
	// 缓存设备最新GPS数据
	public static Map<String, TgpsData> gpsDataMap = new ConcurrentHashMap<String, TgpsData>();
	// 缓存警情数据
	public static Map<String, String> alarmDataMap = new ConcurrentHashMap<String, String>();

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
	
	
	private ExecutorService executor;
	
	public void setExecutorService(ExecutorService executor) {
		this.executor = executor;
	}

	/**
	 * LK
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_LK(String deviceSession, Message77X message,
			String deviceName) {
		
		executor.execute(new Runnable() {
			public void run() {
				try {
					// 设备链接上来，缓存设备数据
					String deviceSn = message.getDeviceSn();

					try {
						Tdevice device = deviceService.select(deviceSn);
						if (device != null) {
							deviceMap.put(deviceSn, device);
						}

						// 更新数据库设备版本号
						deviceService.updateHardware(16, deviceSn);

						// 保存设备号与deviceSession的对应关系
						DeviceSessionMap deviceSessionMap = deviceSessionMapService.getByDeviceSn(message.getDeviceSn());
						if (deviceSessionMap != null) {
//							if (deviceSessionMapService.deleteByDeviceSn(message.getDeviceSn())) {
//								deviceSessionMapService.save(deviceName, message.getDeviceSn(), deviceSession);
//							} else {
//								LogManager.info("设备{}删除deviceSessionMap记录失败", message.getDeviceSn());
//							}
							deviceSessionMapService.update(deviceName, deviceSn, deviceSession);
						} else {
							deviceSessionMapService.save(deviceName, message.getDeviceSn(), deviceSession);
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
						Integer did = device.getId();
						List<TareaInfo> list = tAreaInfoService.getAreaListById(did);
						areaMap.put(deviceSn, list);
					} catch (Exception e) {
						LogManager.exception(e.getMessage(), e);
					}

					// 回复LK指令
					gsConsumer.pushMessageToDevice(true, deviceName, deviceSession,
							message.buildResponse("LK").getBytes());
					//TimeUnit.MILLISECONDS.sleep(100);

					// 初始化指令（短信总开关，低电短信开关，SOS短信开关）
					String ininCommand = setinit(deviceSn, deviceName);

					LogManager.info(ininCommand);

					String[] cmdArr = ininCommand.split("\\|");
					for (String cmd : cmdArr) {
						TimeUnit.MILLISECONDS.sleep(20);
						
						gsConsumer.pushMessageToDevice(true, deviceName, deviceSession,
								cmd.getBytes());
					}
				} catch (Exception e) {
					LogManager.exception(e.getMessage(), e);
				}
			}
		});
		
		return RPCResult.success();
	}

	/**
	 * UD 位置数据上报
	 * 
	 * @param factoryName
	 * @param deviceSn
	 * @param data
	 * @return
	 */
	public RPCResult call_UD(String deviceSession, Message77X message,
			String deviceName) {
		executor.execute(new Runnable() {
			public void run() {
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
				Tdevice tdevice = deviceService.select(message.getDeviceSn());
				if (tdevice == null)
					//return RPCResult.serviceRefuse();
					return ;
		
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
										+ "'%s', '%s', '%s')", tgpsData.getDid(),
										tgpsData.getDeviceSn(),
										df.format(tgpsData.getCollectDatetime()),
										df.format(tgpsData.getRcvTime()),
										tgpsData.getLat(), tgpsData.getLng(),
										tgpsData.getSpeed(), tgpsData.getDirection(),
										tgpsData.getAltitude(),
										tgpsData.getLongitudeWay(),
										tgpsData.getLatitudeWay(),
										tgpsData.getSatelliteNum(),
										tgpsData.getLocationId(), tgpsData.getCellId(),
										tgpsData.getMsgId(), tgpsData.getGpsFlag(),
										tgpsData.getFlag(), tgpsData.getBattery(),
										tgpsData.getSteps(),
										tgpsData.getLbsWifiRange(),
										tgpsData.getLbsWifiFlag(),
										tgpsData.getAccStatus(), tgpsData.getCalorie());
		
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
												+ "'%s', '%s', '%s', '%s')", tgpsData
												.getDeviceSn(), df.format(tgpsData
												.getCollectDatetime()), df
												.format(tgpsData.getRcvTime()),
												tgpsData.getLat(), tgpsData.getLng(),
												tgpsData.getSpeed(), tgpsData
														.getDirection(), tgpsData
														.getAltitude(), tgpsData
														.getLongitudeWay(), tgpsData
														.getLatitudeWay(), tgpsData
														.getSatelliteNum(), tgpsData
														.getLocationId(), tgpsData
														.getCellId(), tgpsData
														.getMsgId(), tgpsData
														.getGpsFlag(), tgpsData
														.getFlag(), tgpsData
														.getBattery(), tgpsData
														.getSteps(), tgpsData
														.getLbsWifiRange(), tgpsData
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
								if ((tAlarmSetting == null && list != null && !list
										.isEmpty())
										|| (tAlarmSetting != null
												&& tAlarmSetting.getBoundary() == 1
												&& list != null && !list.isEmpty())) {
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
				}
			}
		});

		return RPCResult.success();
	}

	/**
	 * UD2
	 * 
	 * @param factoryName
	 * @param deviceSn
	 * @param data
	 * @return
	 */
	public RPCResult call_UD2(String deviceSession, Message77X message,
			String deviceName) {
		// return call_UD(deviceSession, message, deviceName);
		return RPCResult.success();
	}

	/**
	 * AL
	 * 
	 * @param factoryName
	 * @param deviceSn
	 * @param data
	 * @return
	 */
	public RPCResult call_AL(String deviceSession, Message77X message,
			String deviceName) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		Tdevice tdevice = deviceService.select(message.getDeviceSn());
		if (tdevice == null)
			return RPCResult.serviceRefuse();

		try {
			GpsAndAlarmDataView gpsAndAlarmData = getGpsOrAlarmData(tdevice,
					message);

			if (gpsAndAlarmData != null && gpsAndAlarmData.getDataType() == 1) {
				TalarmData talarmData = gpsAndAlarmData.getAlarmData();

				String sql = String.format("insert into TAlarmData("
						+ "did, device_sn, collect_datetime,"
						+ "rcv_time, type, lat,lng, speed, "
						+ "direction, satellite_num, location_id, cell_id,"
						+ "gps_flag, flag, battery) " + "values("
						+ "'%s', '%s', '%s', '%s', "
						+ "'%s', '%s', '%s', '%s', "
						+ "'%s', '%s', '%s', '%s', " + "'%s', '%s', '%s')",
						talarmData.getDid(), talarmData.getDeviceSn(),
						df.format(talarmData.getCollectDatetime()),
						df.format(talarmData.getRcvTime()),
						talarmData.getType(), talarmData.getLat(),
						talarmData.getLng(), talarmData.getSpeed(),
						talarmData.getDirection(),
						talarmData.getSatelliteNum(),
						talarmData.getLocationId(), talarmData.getCellId(),
						talarmData.getGpsFlag(), talarmData.getFlag(),
						talarmData.getBattery());

				boolean result = talarmDataService.insertAlarmDataBySql(sql);

				LogManager.info("执行SQL:{} 插入警情数据结果:{}", sql, result);

				if (result
						&& (talarmData.getType() != 35 || (talarmData.getType() == 35 && deviceName
								.equals(Consts.Device_HT771)))) {
					boolean onlineStatus = gsConsumer.isDeviceOnline(true,
							deviceName, deviceSession);

					TAlarmSetting tAlarmSetting = tAlarmSettingService
							.getAlarmSettingById(talarmData.getDid());

					LogManager.info("警情推送相关信息：警情类型为：{}，tAlarmSetting是否为空：{}",
							talarmData.getType(), tAlarmSetting == null);
					if (tAlarmSetting != null) {
						LogManager.info("警情开关设置相关信息：SOS开关：{}，低电开关{}",
								tAlarmSetting.getSos(),
								tAlarmSetting.getVoltage());
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
						TgpsData tgpsData = gpsAndAlarmData.getGpsData();

						PushInfo pushInfo = new PushInfo();
						pushInfo.setDid(talarmData.getDid());
						pushInfo.setEquipId(tgpsData.getDeviceSn());
						// 推送消息类型 0表示位置数据，1表示警情数据，：2表示状态数据
						pushInfo.setMsgType(1);
						pushInfo.setAlarmtype(talarmData.getType());
						pushInfo.setDatetime(tgpsData.getCollectDatetime());
						pushInfo.setRcvTime(tgpsData.getRcvTime());
						pushInfo.setLat(tgpsData.getLat());
						pushInfo.setLng(tgpsData.getLng());
						pushInfo.setSpeed(tgpsData.getSpeed().toString());
						pushInfo.setDirection(tgpsData.getDirection()
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
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);

			return RPCResult.failed();
		}

		RPCResult ret = RPCResult.success();
		ret.setRpcResult(message.buildResponse("AL"));
		return ret;
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
			Message77X message) {
		GpsAndAlarmDataView gpsAndAlarmData = new GpsAndAlarmDataView();
		String[] data = message.getData().split(",");

		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		try {
			// 上行数据类型 UD:GPS数据 AL:警情数据
			String dataType = message.getMethod();

			// 设备信息
			Integer did = tdevice.getId();
			String device_sn = tdevice.getDeviceSn();

			// 定位时间
			Date collect_datetime = df.parse(getDateTime(data[0], data[1],
					tdevice.getTimezone() / 3600));
			Date rcv_time = df.parse(getCurrentUtcDateTime());

			// A:定位V:未定位
			String locationFlag = data[2];

			// 纬度 保留7位小数点（正负）
			double lat = Double.parseDouble(data[3]);

			// 经度 保留7位小数点（正负）
			double lng = Double.parseDouble(data[5]);

			// 速度 公里/小时 保留2位小数点
			float speed = Float.parseFloat(data[7]);

			// 方向 保留2位小数点
			Float direction = Float.parseFloat(data[8]);

			// 海拔高度，单位为米（m）
			Integer altitude = Integer.parseInt(data[9].split("\\.")[0]);
			// 0：东经；1：西经
			Integer longitude_way = 0;
			if (data[6].equals("E"))
				longitude_way = 0;
			if (data[6].equals("W"))
				longitude_way = 1;
			// 0：北纬；1：南纬
			Integer latitude_way = 0;
			if (data[4].equals("N"))
				longitude_way = 0;
			if (data[4].equals("S"))
				longitude_way = 1;
			// 定位星数
			Integer satellite_num = Integer.parseInt(data[10]);
			// 基站位置编号
			Integer location_id = 0;
			// 基站小区Id编号
			Integer cell_id = 0;
			// 定位数据的消息ID 0x0023
			String msg_id = "";
			// 0：未定位，1：2D定位，2:LBS基站定位，3：3D gps定位 10:wifi定位
			String gps_flag = "3";
			// 0:正常上传 1：补传
			String flag = "0";
			// 百分比值
			String battery = data[12];
			// 总步数
			Integer steps = Integer.parseInt(data[13]);

			// 翻转次数
			Integer turn = Integer.parseInt(data[14]);
			// 定位精度 单位(米)
			Integer LBS_WIFI_Range = 0;
			// 0没有WIFI和基站信息 1仅有基站定信息 2 仅有WIFI信息 3 基站和WIFI信息都有
			Character LBS_WIFI_Flag = 0;
			// ACC状态
			Character acc_status = 0;
			// 卡路里
			double calorie = 0;

			// 警情类型
			String deviceStatus = data[15];
			Integer alarmFlag = Integer.parseInt(deviceStatus.substring(2, 4),
					16);
			boolean isSOS = (alarmFlag & 1) == 1 ? true : false;
			boolean isLowAattery = (alarmFlag & 2) == 2 ? true : false;
			boolean isTakeOff = (alarmFlag & 16) == 16 ? true : false;

			Integer alarmType = 6;
			if (isSOS)
				alarmType = 6;

			if (isLowAattery && Integer.parseInt(battery) <= 20)
				alarmType = 18;

			if (isTakeOff)
				alarmType = 35;

			// 警情数据去重
			if (dataType.equals("AL")) {
				String alarmData = device_sn + "-"
						+ df.format(collect_datetime) + "-" + alarmType;
				if (alarmDataMap.containsKey(device_sn)
						&& alarmDataMap.get(device_sn).equals(alarmData)) {
					return null;
				} else {
					alarmDataMap.put(device_sn, alarmData);
				}
			}

			// 未定位
			if (locationFlag.equals("V")) {
				collect_datetime = df.parse(getCurrentUtcDateTime());

				// 定位无效时，以下参数给出默认值
				lat = 0;
				lng = 0;
				speed = 0;
				direction = 0f;
				altitude = 0;
				longitude_way = 0;
				latitude_way = 0;
				gps_flag = "2";
				satellite_num = 0;
				LBS_WIFI_Range = 0;
				LBS_WIFI_Flag = 0;

				UDMessage ud = UDMessage.parse(message.getData());

				String mapType = "";
				String connCountry = tdevice.getConnCountry();
				if (connCountry.equals("CN")) // 国内用高德地图
				{
					mapType = "amap";
				} else {
					mapType = "google";
				}

				Map<Integer, Object> map = getGps(tdevice, ud);

				LogManager.info("大数据解析准备，MAP不存在：{}", map == null);

				RPCResult result = taskServiceConsumer.getGpsByWifiAndLbs(
						device_sn, mapType, map);

				LogManager.info("大数据解析，解析状态{}", result.getRpcErrCode());

				if (result != null && result.getRpcErrCode() == 0) {
					JSONObject ret = JSON.parseObject(JSON.toJSONString(result
							.getRpcResult()));
					if (ret != null && ret.containsKey(1)
							&& ret.get(1).toString().equals("200")) {
						// 纬度 保留7位小数点（正负）
						lat = Double.parseDouble(ret.get(4).toString());

						// 经度 保留7位小数点（正负）
						lng = Double.parseDouble(ret.get(3).toString());

						// 定位精度 单位(米)
						LBS_WIFI_Range = Integer
								.parseInt(ret.get(5).toString());

						LogManager
								.info("设备{}通过WIFI定位返回经度为：lng:{}，维度为：lat:{}，定位精度 为：{}",
										device_sn, lng, lat, LBS_WIFI_Range);
					}
				}

				// 设置定位类型
				if (ud.getWifiGroup().getBsItems().size() >= 3) {
					gps_flag = "10";
				}
			}

			// 保存GPS数据
			TgpsData tgpsData = new TgpsData();
			tgpsData.setDid(did);
			tgpsData.setDeviceSn(device_sn);
			tgpsData.setCollectDatetime(collect_datetime);
			tgpsData.setRcvTime(rcv_time);
			tgpsData.setLat(lat);
			tgpsData.setLng(lng);
			tgpsData.setSpeed(speed);
			tgpsData.setDirection(direction);
			tgpsData.setAltitude(altitude);
			tgpsData.setLongitudeWay(longitude_way);
			tgpsData.setLatitudeWay(latitude_way);
			tgpsData.setSatelliteNum(satellite_num);
			tgpsData.setLocationId(location_id);
			tgpsData.setCellId(cell_id);
			tgpsData.setMsgId(msg_id);
			tgpsData.setGpsFlag(gps_flag);
			tgpsData.setFlag(flag);
			tgpsData.setBattery(battery);
			tgpsData.setSteps(steps);
			tgpsData.setLbsWifiRange(LBS_WIFI_Range);
			tgpsData.setLbsWifiFlag(LBS_WIFI_Flag);
			tgpsData.setAccStatus(acc_status);
			tgpsData.setCalorie(calorie);
			gpsAndAlarmData.setGpsData(tgpsData);

			// 保存警情数据
			TalarmData talarmData = new TalarmData();
			talarmData.setDid(did);
			talarmData.setDeviceSn(device_sn);
			talarmData.setCollectDatetime(collect_datetime);
			talarmData.setRcvTime(rcv_time);
			talarmData.setType(alarmType);
			talarmData.setLat(lat);
			talarmData.setLng(lng);
			talarmData.setSpeed(speed);
			talarmData.setDirection(direction);
			talarmData.setSatelliteNum(satellite_num);
			talarmData.setLocationId(location_id);
			talarmData.setCellId(cell_id);
			talarmData.setGpsFlag(gps_flag);
			talarmData.setFlag(flag);
			talarmData.setBattery(battery);
			gpsAndAlarmData.setAlarmData(talarmData);

			if (dataType.equals("UD")) // GPS数据
			{
				gpsAndAlarmData.setDataType(0);
			} else if (dataType.equals("AL")) // 警情数据
			{
				gpsAndAlarmData.setDataType(1);
			}

			// 翻转次数更新
			Date creatTime = DateTimeUtil.getDateByDay(collect_datetime);
			DeviceTurn deviceTurn = new DeviceTurn();
			deviceTurn.setDeviceSn(device_sn);
			deviceTurn.setCreateTime(creatTime);

			DeviceTurn dt = deviceTurnService.select(deviceTurn);
			deviceTurn.setTurn(turn);
			if (null != dt && turn > dt.getTurn()) {// 修改
				deviceTurnService.modify(deviceTurn);
			} else if (null == dt && turn > 0) {// 新增
				deviceTurnService.add(deviceTurn);
			}

			return gpsAndAlarmData;
		} catch (Exception e) {
			LogManager.info("解析GPS或警情信息异常，异常信息为{}", e.getMessage());
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
	 * TKQ 终端请求录音下发
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_TKQ(String deviceSession, Message77X message,
			String deviceName) {
		RPCResult result = RPCResult.success();
		result.setRpcResult(message.buildResponse("TKQ"));
		return result;
	}

	/**
	 * TKQ2 终端请求好友录音下发
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_TKQ2(String deviceSession, Message77X message,
			String deviceName) {
		RPCResult result = RPCResult.success();
		result.setRpcResult(message.buildResponse("TKQ2"));
		return result;
	}

	/**
	 * TK 终端发送
	 * 
	 * @param deviceSession
	 * @param message
	 * @return
	 */
	public RPCResult call_TK(String deviceSession, Message77X message,
			String deviceName) {
		RPCResult result = RPCResult.failed();

		try {
			byte[] data = message.getDataBytes();

			if (data != null && data.length > 1) {
				data = Message77X.from77XAudio(data);
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
			if (param != null && param.containsKey("lat")
					&& param.get("lat").length > 0 && param.containsKey("lng")
					&& param.get("lng").length > 0
					&& param.containsKey("radius")
					&& param.get("radius").length > 0) {
				Double lat = Double.parseDouble(param.get("lat")[0]);
				Double lng = Double.parseDouble(param.get("lng")[0]);
				Integer radius = Integer.parseInt(param.get("radius")[0]);

				TareaInfo tareaInfo = new TareaInfo();
				tareaInfo.setLat(lat);
				tareaInfo.setLng(lng);
				tareaInfo.setRadius(radius);

				List<TareaInfo> list = new ArrayList<TareaInfo>();
				list.add(tareaInfo);
				areaMap.put(deviceSn, list);

				map.put("result", "0");
			} else {
				map.put("result", "1");
			}
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
			// 清理缓存
			if (areaMap.containsKey(deviceSn)) {
				areaMap.remove(deviceSn);
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

				Message77X message = new Message77X("3G", deviceSn, 0, "", null);
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
			Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
			Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
	 * 设置电话本信息到远程
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
			String PHB1 = "";
			String PHB2 = "";
			String WHITELIST1 = "";
			String WHITELIST2 = "";
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

			for (int i = 1; i <= 10; i++) {
				String nickname = "";
				String tel = "";

				if (param != null && param.containsKey("tel" + i)
						&& param.get("tel" + i).length > 0) {
					String nicknameAndTel = param.get("tel" + i)[0];
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
				}

				// 电话本和白名单按五个分组
				if (i <= 5) {
					PHB1 += tel + "," + nickname + ",";
					WHITELIST1 += tel + ",";
				} else {
					PHB2 += tel + "," + nickname + ",";
					WHITELIST2 += tel + ",";
				}

				// SOS号码取头三个
				if (i <= 3) {
					SOS += tel + ",";
				}
			}

			// 电话本指令
			PHB1 = "PHB," + PHB1.substring(0, PHB1.length() - 1);
			PHB2 = "PHB2," + PHB2.substring(0, PHB2.length() - 1);

			// 白名单指令
			WHITELIST1 = "WHITELIST1,"
					+ WHITELIST1.substring(0, WHITELIST1.length() - 1);
			WHITELIST2 = "WHITELIST2,"
					+ WHITELIST2.substring(0, WHITELIST2.length() - 1);

			// SOS指令
			SOS = "SOS," + SOS.substring(0, SOS.length() - 1);

			String deviceSession = sessionmap.getDeviceSession();
			Message77X message = new Message77X("3G", deviceSn, 0, "", null);

			// 设置电话本头五个
			RPCResult retSetPhonebook1 = gsConsumer.pushMessageToDevice(true,
					deviceName, deviceSession, message.buildResponse(PHB1)
							.getBytes());

			// 设置电话本后五个
			RPCResult retSetPhonebook2 = gsConsumer.pushMessageToDevice(true,
					deviceName, deviceSession, message.buildResponse(PHB2)
							.getBytes());

			// 设置白名单头五个
			RPCResult retSetWhitelist1 = gsConsumer.pushMessageToDevice(true,
					deviceName, deviceSession, message
							.buildResponse(WHITELIST1).getBytes());

			// 设置白名单后五个
			RPCResult retSetWhitelist2 = gsConsumer.pushMessageToDevice(true,
					deviceName, deviceSession, message
							.buildResponse(WHITELIST2).getBytes());

			// 设置SOS号码
			RPCResult retSetSos = gsConsumer.pushMessageToDevice(true,
					deviceName, deviceSession, message.buildResponse(SOS)
							.getBytes());

			if (retSetPhonebook1.getRpcErrCode() == 0
					&& retSetPhonebook2.getRpcErrCode() == 0
					&& retSetWhitelist1.getRpcErrCode() == 0
					&& retSetWhitelist2.getRpcErrCode() == 0
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
				String SILENCETIME = "SILENCETIME,00:00-00:00,00:00-00:00,00:00-00:00,00:00-00:00";

				String status = param.get("status")[0];
				if (status.equals("1")) {
					String starttime1 = param.get("starttime1")[0];
					String endtime1 = param.get("endtime1")[0];
					String starttime2 = param.get("starttime2")[0];
					String endtime2 = param.get("endtime2")[0];

					SILENCETIME = "SILENCETIME," + starttime1 + "-" + endtime1
							+ "," + starttime2 + "-" + endtime2
							+ ",00:00-00:00,00:00-00:00";
				}

				String deviceSession = sessionmap.getDeviceSession();
				Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
				Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
				Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
				Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
			Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
			Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
				Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
	 * 设备链接上服务器，第一时间进行初始化设置
	 * 
	 * @param deviceSn
	 * @param deviceName
	 * @return
	 */
	public String setinit(String deviceSn, String deviceName) {
		StringBuilder result = new StringBuilder();

		// 短信总开关开
		String SMSONOFF = "SMSONOFF,1";
		// 低电短信报警开关关
		String LOWBAT = "LOWBAT,0";
		// SOS 短信报警开关开
		String SOS = "SOSSMS,1";

		Tdevice device = deviceMap.get(deviceSn);
		TAlarmSetting tAlarmSetting = tAlarmSettingService
				.getAlarmSettingById(device.getId());
		if (tAlarmSetting != null) {
			Short sos = tAlarmSetting.getSos();
			if (sos == 0) {
				SOS = "SOSSMS,0";
			}
		}

		Message77X message = new Message77X("3G", deviceSn, 0, "", null);
		// 设置短信总开关
		SMSONOFF = message.buildResponse(SMSONOFF);

		// 设置短信总开关
		LOWBAT = message.buildResponse(LOWBAT);

		// 设置短信总开关
		SOS = message.buildResponse(SOS);

		result.append(SMSONOFF).append("|").append(LOWBAT).append("|")
				.append(SOS);

		if (deviceName.equals(Consts.Device_HT771)) {
			// 取下手表报警开关
			String REMOVE = "REMOVE,1";
			// 取下手表短信报警开关
			String REMOVESMS = "REMOVESMS,0";

			if (tAlarmSetting != null) {
				Short takeoff = tAlarmSetting.getTakeOff();
				if (takeoff == 0) {
					REMOVE = "REMOVE,0";
				}
			}

			// 取下手表报警开关
			REMOVE = message.buildResponse(REMOVE);

			// 取下手表短信报警开关
			REMOVESMS = message.buildResponse(REMOVESMS);

			result.append("|").append(REMOVE).append("|").append(REMOVESMS);
		}

		return result.toString();
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

			if (param != null && param.containsKey("sos")
					&& param.get("sos").length > 0) {
				String SOSSMS = "SOSSMS,0";

				String sos = param.get("sos")[0];
				if (sos.equals("1")) {
					SOSSMS = "SOSSMS,1";
				}

				String deviceSession = sessionmap.getDeviceSession();
				Message77X message = new Message77X("3G", deviceSn, 0, "", null);

				// 设置短信开关
				RPCResult retSetSOSSMS = gsConsumer.pushMessageToDevice(true,
						deviceName, deviceSession, message
								.buildResponse(SOSSMS).getBytes());

				if (deviceName.equals(Consts.Device_HT771)) {
					if (param.containsKey("takeOff")
							&& param.get("takeOff").length > 0) {
						String REMOVE = "REMOVE,0";
						String REMOVESMS = "REMOVESMS,0";

						String takeOff = param.get("takeOff")[0];
						if (takeOff.equals("1")) {
							REMOVE = "REMOVE,1";
						}

						// 设置脱落报警开关
						RPCResult retSetREMOVE = gsConsumer
								.pushMessageToDevice(true, deviceName,
										deviceSession,
										message.buildResponse(REMOVE)
												.getBytes());

						// 设置脱落短信开关
						RPCResult retSetREMOVESMS = gsConsumer
								.pushMessageToDevice(true, deviceName,
										deviceSession,
										message.buildResponse(REMOVESMS)
												.getBytes());

						if (retSetSOSSMS.getRpcErrCode() == 0
								&& retSetREMOVESMS.getRpcErrCode() == 0
								&& retSetREMOVE.getRpcErrCode() == 0) {
							ret = RPCResult.success();
						}
					}
				} else {
					if (retSetSOSSMS.getRpcErrCode() == 0) {
						ret = RPCResult.success();
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
					Message77X message = new Message77X("3G", deviceSn, 0, "",
							null);

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
				data = Message77X.to77XAudio(data);

				String deviceSession = sessionmap.getDeviceSession();
				Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
				Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
					Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
			Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
	 * 远程拍照
	 * 
	 * @param deviceSession
	 * @param message
	 * @param deviceName
	 * @return
	 */
	// public RPCResult call_IMG(String deviceSession, Message77X message,
	// String deviceName) {
	// RPCResult result = RPCResult.failed();
	// // 5,time,img
	// try {
	// byte[] data = message.getDataBytes();
	// byte[] type = DataUtil.bytesFromBytes(data, 0, (byte) 0,
	// Consts.TAG_Comma, false);
	// byte[] time = DataUtil.bytesFromBytes(data, type.length,
	// Consts.TAG_Comma, Consts.TAG_Comma, false);
	// byte[] img = DataUtil.bytesFromBytes(data, type.length
	// + time.length + 1, Consts.TAG_Comma, (byte) 0, false);
	//
	// if (img != null && img.length > 1) {
	// img = Message77X.from77XAudio(img);
	//
	// LogManager.info("设备上传图片数据为："
	// + StringEncoding.bytesToHexString(img));
	//
	// Map<String, String> headers = new HashMap<String, String>();
	// List<StringBodyPair> strBodyParts = new ArrayList<StringBodyPair>();
	// List<ByteArrayBodyPair> bytearrayBodyParts = new
	// ArrayList<ByteArrayBodyPair>();
	//
	// strBodyParts.add(new StringBodyPair("function", new StringBody(
	// "upload", ContentType.MULTIPART_FORM_DATA)));
	//
	// strBodyParts.add(new StringBodyPair("name", new StringBody(
	// message.getDeviceSn(), ContentType.TEXT_PLAIN)));
	//
	// strBodyParts.add(new StringBodyPair("type", new StringBody("3",
	// ContentType.TEXT_PLAIN)));
	//
	// ByteArrayBodyPair babp = new ByteArrayBodyPair("device.jpg",
	// new ByteArrayBody(img,
	// ContentType.APPLICATION_OCTET_STREAM,
	// "device.jpg"));
	// bytearrayBodyParts.add(babp);
	//
	// LogManager
	// .info("发送请求-----------------------------------------");
	// String url = env.getProperty("resource.service.url");
	// HttpResponse response = HttpUtils.doPostMultiPart(url, null,
	// null, headers, null, strBodyParts, null,
	// null, bytearrayBodyParts);
	//
	// String respbody = EntityUtils.toString(response.getEntity(),
	// "utf-8");
	//
	// com.alibaba.fastjson.JSONObject objApi = JSON
	// .parseObject(respbody);
	// if (objApi.containsKey("code")
	// && objApi.get("code").toString().equals("0")) {
	// String obj = JSON.toJSONString(objApi.get("data"));
	// List<DevicePhoto> devicePhotoList = JSON.parseArray(obj,
	// DevicePhoto.class);
	// if (null != devicePhotoList && devicePhotoList.size() > 0) {
	// for (DevicePhoto devicePhoto : devicePhotoList) {
	// devicePhoto.setDeviceSn(message.getDeviceSn());
	// devicePhoto.setCreateTime(new Date());
	// devicePhotoService.add(devicePhoto);
	// }
	// }
	// }
	// LogManager.info("transferURL result: {}", respbody);
	//
	// }
	// } catch (Exception e) {
	// LogManager.exception(e.getMessage(), e);
	// }
	//
	// if (result.getRpcErrCode() == 0) {
	// result.setRpcResult(message.buildResponse("TK,1"));
	// } else {
	// result.setRpcResult(message.buildResponse("TK,0"));
	// }
	// return result;
	// }

	public RPCResult call_IMG(String deviceSession, Message77X message,
			String deviceName) {
		RPCResult result = RPCResult.success();
		// 5,time,img
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
				boolean saveresult = saveDevicePhoto(message.getDeviceSn(),
						img, resServerURL, devicePhotoService);
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return result;
	}

	/**
	 * .翻转检测时间段设置
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
				Message77X message = new Message77X("3G", deviceSn, 0, "", null);

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
}
