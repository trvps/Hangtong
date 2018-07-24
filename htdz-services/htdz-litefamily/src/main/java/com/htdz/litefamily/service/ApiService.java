package com.htdz.litefamily.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.htdz.common.LanguageManager;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;
import com.htdz.common.utils.DateTimeUtil;
import com.htdz.common.utils.EnumUtils;
import com.htdz.common.utils.PureNetUtil;
import com.htdz.common.utils.StringSubUtil;
import com.htdz.db.service.ChatTokenService;
import com.htdz.db.service.CityService;
import com.htdz.db.service.DeviceInfoService;
import com.htdz.db.service.DeviceRemindService;
import com.htdz.db.service.DeviceService;
import com.htdz.db.service.DeviceStepService;
import com.htdz.db.service.GpsDataLastService;
import com.htdz.db.service.TalarmDataService;
import com.htdz.db.service.TgpsDataService;
import com.htdz.db.service.UserService;
import com.htdz.db.service.UserTokenService;
import com.htdz.db.service.WeatherService;
import com.htdz.def.data.ApiResult;
import com.htdz.def.data.Errors;
import com.htdz.def.data.RPCResult;
import com.htdz.def.dbmodel.ChatToken;
import com.htdz.def.dbmodel.City;
import com.htdz.def.dbmodel.DeviceRemind;
import com.htdz.def.dbmodel.DeviceStep;
import com.htdz.def.dbmodel.GpsDataLast;
import com.htdz.def.dbmodel.TalarmData;
import com.htdz.def.dbmodel.Tdevice;
import com.htdz.def.dbmodel.TgpsData;
import com.htdz.def.dbmodel.UserToken;
import com.htdz.def.dbmodel.Weather;
import com.htdz.def.view.DeviceInfo;
import com.htdz.def.view.GpsAddress;
import com.htdz.def.view.PushInfo;
import com.htdz.def.view.UserConn;
import com.htdz.litefamily.dubbo.RegServiceConsumer;
import com.htdz.litefamily.dubbo.TaskServiceConsumer;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

@Service
public class ApiService {
	@Autowired
	private Environment env;
	@Autowired
	private WeatherService weatherService;
	@Autowired
	private DeviceService deviceService;
	@Autowired
	private DeviceRemindService deviceRemindService;
	@Autowired
	private TalarmDataService talarmDataService;
	@Autowired
	private DeviceStepService deviceStepService;
	@Autowired
	private TgpsDataService tgpsDataService;
	@Autowired
	private ChatTokenService chatTokenService;
	@Autowired
	private RegServiceConsumer regConsumerService;
	@Autowired
	private DeviceInfoService deviceInfoService;
	@Autowired
	private RemindServerManager remindServerManager;
	@Autowired
	private UserTokenService userTokenService;
	@Autowired
	private TaskServiceConsumer taskServiceConsumer;
	@Autowired
	private CityService cityService;
	@Autowired
	UserService userService;
	@Autowired
	private GpsDataLastService gpsDataLastService;

	@Value("${weather.url}")
	private String weatherUrl;
	@Value("${weather.key}")
	private String weatherKey;
	@Value("${recource.url}")
	private String imgUrl;

	/**
	 * 根据经纬度获取天气
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getWeather(Map<String, String[]> params) {
		ApiResult result = new ApiResult();
		City city = new City();
		Weather weather = new Weather();
		String lng = DataUtil.getStringFromMap(params, "lng");// 经度
		String lat = DataUtil.getStringFromMap(params, "lat");// 纬度
		GpsAddress gpsAddress = new GpsAddress();
		gpsAddress.setLocation(lng + "," + lat);
		gpsAddress.setLang("zh_cn");
		RPCResult taskResult = taskServiceConsumer.getGpsAddress(gpsAddress);

		if (!TextUtils.isEmpty(lng) && !TextUtils.isEmpty(lat)) {
			if (taskResult.getRpcErrCode() == Errors.ERR_SUCCESS) {
				GpsAddress g = (GpsAddress) taskResult.getRpcResult();

				if (null == g.getCitycode()) {
					LogManager.info("逆地理解析失败");
				} else {
					city = cityService.selectByMapCode(g.getCitycode());
				}

				if (null == city || null == city.getWeatherCode()) {
					city = new City();
					city.setWeatherCode("CN101320101");// default hongkong
				}
				weather = weatherService.selectByCityCode(city.getWeatherCode(), DateTimeUtil.getCurrentUtcDate());

				if (null != weather) {
					weather.setImg(imgUrl + "/comm/cond_icon_heweather/" + weather.getWCode() + ".png");
					result.setData(weather);
					result.setCode(Errors.ERR_SUCCESS);
				} else {
					weather = setWeather(city.getWeatherCode());
					result.setData(weather);
					result.setCode(Errors.ERR_SUCCESS);
				}
			} else {
				result.setCode(Errors.ERR_FAILED);
				result.setMsg("task RPC error!");
			}
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
		}
		return result;

	}

	/**
	 * 调用天气接口设置天气对象
	 * 
	 * @return
	 */
	private Weather setWeather(String location) {

		String s = "location=" + location + "&lang=en&key=" + weatherKey;
		String response = PureNetUtil.get(weatherUrl + s);
		Weather weather = null;
		if (response != null) {
			JSONObject jobj = JSONObject.fromObject(response);
			JSONArray jsonArray = (JSONArray) jobj.get("HeWeather6");

			for (Object curobj : jsonArray) {
				JSONObject json = (JSONObject) curobj;
				String status = json.get("status").toString();
				if (status.equals("ok")) {
					weather = new Weather();

					JSONObject updateObj = (JSONObject) json.get("update");
					String utdDate = ((String) updateObj.get("utc")) + ":00";// 时间

					JSONObject now = (JSONObject) json.get("now");
					String tmp = (String) now.get("tmp");// 温度
					String condCode = (String) now.get("cond_code");// 实况天气状况代码

					JSONObject basic = (JSONObject) json.get("basic");
					String city = (String) basic.get("location");// 城市

					weather.setWTime(DateTimeUtil.strToDateLong(utdDate));
					weather.setWCode(condCode);
					weather.setTemperature(tmp);
					weather.setCityCode(location);
					weather.setCreateTime(DateTimeUtil.strToDate(utdDate));
					weather.setCityName(city);
					weather.setImg(imgUrl + "/comm/cond_icon_heweather/" + condCode + ".png");
					weatherService.add(weather);
					return weather;
				}
			}
		}
		return weather;
	}

	/**
	 * 查询帐号是否绑定
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getIsBinding(Map<String, String[]> params) {
		ApiResult result = new ApiResult();
		Map<String, String> map = new HashMap<String, String>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		if (!TextUtils.isEmpty(deviceSn)) {
			Tdevice isBinding = deviceService.selectBindingDevice(deviceSn);
			if (isBinding == null) {
				map.put("isBinding", "-1");
				if (null == deviceService.select(deviceSn)) {
					Tdevice device = new Tdevice();
					device.setDeviceSn(deviceSn);
					device.setTimezone(0);
					device.setTimezoneid(31);
					device.setDataSource(0);
					device.setDisable(0);
					device.setCreateTime(new Date());
					device.setGpsInterval(60);
					device.setRanges(5);
					device.setProtocolType(8);
					device.setProductType(29);
					device.setExpiredTime(DateTimeUtil.DateAddYear(new Date(), 3));
					device.setExpiredTimeDe(DateTimeUtil.DateAddYear(new Date(), 3));
					if (!deviceService.add(device)) {
						result.setMsg("Create device fail!");
					} else {
						result.setMsg("Create device success!");
					}
				}

			} else {
				map.put("isBinding", isBinding.getId().toString());
			}
			result.setData(map);
			result.setCode(Errors.ERR_SUCCESS);
			return result;
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
	}

	/**
	 * 查询提醒
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getRemind(Map<String, String[]> params) {
		ApiResult result = new ApiResult();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String startDate = DataUtil.getStringFromMap(params, "startDate");
		String endDate = DataUtil.getStringFromMap(params, "endDate");

		if (!TextUtils.isEmpty(deviceSn) && !TextUtils.isEmpty(startDate) && !TextUtils.isEmpty(endDate)) {
			List<DeviceRemind> deviceRemind = deviceRemindService.selectDeviceRemindByTime(deviceSn, startDate,
					endDate);

			List<String> collectDate = DateTimeUtil.collectLocalDates(StringSubUtil.StrLongToStr(startDate),
					StringSubUtil.StrLongToStr(endDate));
			List<HashMap<String, Object>> l = new ArrayList<HashMap<String, Object>>();
			for (String cd : collectDate) {
				HashMap<String, Object> m = new HashMap<String, Object>();
				List<DeviceRemind> remindList = new ArrayList<DeviceRemind>();
				m.put("date", cd);
				for (DeviceRemind r : deviceRemind) {
					String time = DateTimeUtil.dateToStr(r.getRemindTime());
					if (m.get("date").equals(time)) {
						remindList.add(r);
					}
				}
				m.put("remind", remindList);
				l.add(m);
			}
			result.setData(l);

			result.setCode(Errors.ERR_SUCCESS);
			return result;

		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
	}

	/**
	 * 新增提醒
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult setRemind(Map<String, String[]> params) {

		ApiResult result = new ApiResult();
		Map<String, String> map = new HashMap<String, String>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String title = DataUtil.getStringFromMap(params, "title");
		String remindTime = DataUtil.getStringFromMap(params, "remindTime");
		String remindTimeUTC = DataUtil.getStringFromMap(params, "remindTimeUTC");
		String type = DataUtil.getStringFromMap(params, "type");

		if (!TextUtils.isEmpty(deviceSn) && !TextUtils.isEmpty(title) && !TextUtils.isEmpty(remindTime)
				&& !TextUtils.isEmpty(remindTimeUTC) && !TextUtils.isEmpty(type)) {

			List<DeviceRemind> curDeviceRemind = deviceRemindService.selectOneDeviceByOneDate(deviceSn,
					StringSubUtil.StrLongToStr(remindTime));
			if (curDeviceRemind.size() >= 10) {
				result.setCode(Errors.ERR_FAILED);
				result.setMsg(LanguageManager.getMsg("common.exceed.maximum.remind"));
			} else {
				DeviceRemind deviceRemind = new DeviceRemind();
				deviceRemind.setDeviceSn(deviceSn);
				deviceRemind.setTitle(title);
				deviceRemind.setRemindTime(DateTimeUtil.strToDateLong(remindTime));
				deviceRemind.setRemindTimeUTC(DateTimeUtil.strToDateLong(remindTimeUTC));
				deviceRemind.setType(Integer.parseInt(type));
				deviceRemindService.add(deviceRemind);

				if (DateTimeUtil.dateToStr(deviceRemind.getRemindTimeUTC()).equals(DateTimeUtil.getCurrentUtcDate())) {
					if (remindServerManager.add(deviceRemind)) {
						LogManager.info("remindServerManager add deviceRemind(id={}) success", deviceRemind.getId());
					} else {
						LogManager.info("remindServerManager add deviceRemind(id={}) fail", deviceRemind.getId());
					}
				}

				map.put("id", deviceRemind.getId().toString());
				result.setData(map);
				result.setCode(Errors.ERR_SUCCESS);
			}

		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
		}
		return result;
	}

	/**
	 * 删除提醒
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult deleteRemind(Map<String, String[]> params) {

		ApiResult result = new ApiResult();
		String id = DataUtil.getStringFromMap(params, "id");

		if (!TextUtils.isEmpty(id)) {

			DeviceRemind deviceRemind = deviceRemindService.select(Integer.parseInt(id));
			if (null != deviceRemind) {
				if (StringSubUtil.StrLongToStr(DateTimeUtil.dateToStrLong(deviceRemind.getRemindTimeUTC()))
						.equals(DateTimeUtil.getCurrentUtcDate())) {
					if (remindServerManager.delete(deviceRemind)) {
						LogManager.info("remindServerManager delete deviceRemind success id={}", id);
					} else {
						LogManager.info("remindServerManager delete deviceRemind fail id={}", id);
					}
				}
			}

			if (deviceRemindService.delete(Integer.parseInt(id))) {
				result.setCode(Errors.ERR_SUCCESS);
			} else {
				result.setCode(Errors.ERR_FAILED);
			}

		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
		return result;
	}

	/**
	 * 获取群成员信息
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getGroupUser(Map<String, String[]> params) {
		ApiResult result = new ApiResult();
		Map<String, String> map = new HashMap<String, String>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		if (!TextUtils.isEmpty(deviceSn)) {
			map.put("deviceSn", deviceSn);
			RPCResult RpcResult = regConsumerService.hanleRegService("getGroupUser", map);

			if (RpcResult.getRpcErrCode() == Errors.ERR_SUCCESS) {
				JSONObject objApi = JSONObject.fromObject(RpcResult.getRpcResult());
				result.setData(objApi.get("data"));
				result.setCode(Errors.ERR_SUCCESS);
			} else {
				result.setCode(Errors.ERR_FAILED);
				result.setMsg("reg RPC error!");
			}
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
		}

		return result;
	}

	/**
	 * 获取单个群成员信息
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getGroupUserByName(Map<String, String[]> params) {
		ApiResult result = new ApiResult();
		Map<String, String> map = new HashMap<String, String>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String username = DataUtil.getStringFromMap(params, "username");
		if (!TextUtils.isEmpty(deviceSn) && !TextUtils.isEmpty(username)) {
			map.put("deviceSn", deviceSn);
			map.put("username", username);

			RPCResult RpcResult = regConsumerService.hanleRegService("getGroupUserByName", map);

			if (RpcResult.getRpcErrCode() == Errors.ERR_SUCCESS) {
				JSONObject objApi = JSONObject.fromObject(RpcResult.getRpcResult());
				result.setData(objApi.get("data"));
				result.setCode(Errors.ERR_SUCCESS);
			} else {
				result.setCode(Errors.ERR_FAILED);
				result.setMsg("reg RPC error!");
			}
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
		}
		return result;
	}

	/**
	 * 新增SOS报警
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult setAlarm(Map<String, String[]> params) {
		ApiResult result = new ApiResult();
		Map<String, String> map = new HashMap<String, String>();

		String did = DataUtil.getStringFromMap(params, "did");
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String collectDatetime = DataUtil.getStringFromMap(params, "collectDatetime");
		String lat = DataUtil.getStringFromMap(params, "lat");
		String lng = DataUtil.getStringFromMap(params, "lng");
		String speed = DataUtil.getStringFromMap(params, "speed");
		String direction = DataUtil.getStringFromMap(params, "direction");
		String gpsFlag = DataUtil.getStringFromMap(params, "gpsFlag");

		if (!TextUtils.isEmpty(did) && !TextUtils.isEmpty(deviceSn) && !TextUtils.isEmpty(collectDatetime)
				&& !TextUtils.isEmpty(lat) && !TextUtils.isEmpty(lng)) {
			TalarmData talarmData = new TalarmData();
			talarmData.setDid(Integer.parseInt(did));
			talarmData.setDeviceSn(deviceSn);
			talarmData.setCollectDatetime(DateTimeUtil.strToDateLong(collectDatetime));
			talarmData.setRcvTime(new Date());
			talarmData.setLat(Double.parseDouble(lat));
			talarmData.setLng(Double.parseDouble(lng));
			talarmData.setType(EnumUtils.AlarmType.SOS);

			if (!TextUtils.isEmpty(speed)) {
				talarmData.setSpeed(Float.parseFloat(speed));
			}
			if (!TextUtils.isEmpty(direction)) {
				talarmData.setDirection(Float.parseFloat(direction));
			}
			if (!TextUtils.isEmpty(gpsFlag)) {
				talarmData.setGpsFlag(gpsFlag);
			}
			talarmDataService.add(talarmData);

			PushInfo pushInfo = new PushInfo();
			pushInfo.setEquipId(deviceSn);
			pushInfo.setMsgType(EnumUtils.PushMsgType.ALARM);
			pushInfo.setAlarmtype(EnumUtils.AlarmType.SOS);
			pushInfo.setDatetime(DateTimeUtil.strToDateLong(DateTimeUtil.getCurrentUtcDatetime()));

			List<UserConn> userConnList = userService.getUserDeviceInfo(pushInfo.getEquipId());
			if (null != userConnList && userConnList.size() > 0) {
				for (UserConn userConn : userConnList) {
					taskServiceConsumer.pushMsg(pushInfo, userConn);
				}
			}

			map.put("id", talarmData.getId().toString());
			result.setData(map);
			result.setCode(Errors.ERR_SUCCESS);
			return result;

		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
	}

	/**
	 * 获取最后一次上传步数日期
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getLastStep(Map<String, String[]> params) {
		ApiResult result = new ApiResult();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		if (!TextUtils.isEmpty(deviceSn)) {
			DeviceStep deviceStep = deviceStepService.selectOne(deviceSn);

			result.setData(deviceStep);
			result.setCode(Errors.ERR_SUCCESS);
			return result;
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
	}

	/**
	 * 新增多天步数
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult setStep(Map<String, String[]> params) {

		ApiResult result = new ApiResult();

		String Steps = DataUtil.getStringFromMap(params, "map");
		if (!TextUtils.isEmpty(Steps)) {
			JSONArray jsonArray = JSONArray.fromObject(Steps);
			for (Object curobj : jsonArray) {
				JSONObject json = (JSONObject) curobj;
				if (json.containsKey("deviceSn") && json.containsKey("createDate") && json.containsKey("step")) {
					String deviceSn = json.get("deviceSn").toString();
					String createDate = json.get("createDate").toString();
					String step = json.get("step").toString();
					List<DeviceStep> deviceStepList = deviceStepService.selectByOneDate(deviceSn,
							DateTimeUtil.dateToStr(DateTimeUtil.strToDate(createDate)));

					DeviceStep deviceStep = new DeviceStep();
					deviceStep.setDeviceSn(deviceSn);
					deviceStep.setCreateDate(DateTimeUtil.strToDateLong(createDate));
					deviceStep.setStep(step);
					deviceStep.setUpdateDate(DateTimeUtil.strToDateLong(DateTimeUtil.getCurrentUtcDatetime()));
					if (!deviceStepList.isEmpty()) {
						if (Integer.parseInt(step) > Integer.parseInt(deviceStepList.get(0).getStep())) {
							if (!deviceStepService.modify(deviceStep)) {
								result.setCode(Errors.ERR_FAILED);
							}
						}
					} else {
						if (!deviceStepService.add(deviceStep)) {
							result.setCode(Errors.ERR_FAILED);
						}
					}
				}

			}

			result.setCode(Errors.ERR_SUCCESS);
			return result;
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
	}

	/**
	 * 查询步数
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getStep(Map<String, String[]> params) {

		ApiResult result = new ApiResult();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String startDate = DataUtil.getStringFromMap(params, "startDate");
		String endDate = DataUtil.getStringFromMap(params, "endDate");

		if (!TextUtils.isEmpty(deviceSn) && !TextUtils.isEmpty(startDate) && !TextUtils.isEmpty(endDate)) {
			List<DeviceStep> deviceStep = deviceStepService.selectByDate(deviceSn, startDate, endDate);

			if (!deviceStep.isEmpty()) {
				result.setData(deviceStep);
			}

			result.setCode(Errors.ERR_SUCCESS);
			return result;
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
	}

	/**
	 * 设置头像
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult setHeadPic(Map<String, String[]> params) {
		ApiResult result = new ApiResult();
		Map<String, String> map = new HashMap<String, String>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String url = DataUtil.getStringFromMap(params, "url");
		Tdevice device = new Tdevice();

		if (!TextUtils.isEmpty(deviceSn) && !TextUtils.isEmpty(url)) {

			map.put("deviceSn", deviceSn);
			map.put("url", url);
			RPCResult RpcResult = regConsumerService.hanleRegService("setHeadPic", map);

			if (RpcResult.getRpcErrCode() == Errors.ERR_SUCCESS) {
				JSONObject objApi = JSONObject.fromObject(RpcResult.getRpcResult());
				if (Integer.parseInt(objApi.get("code").toString()) == Errors.ERR_SUCCESS) {
					device.setHeadPortrait(StringSubUtil.getRelativeURL(url));
					device.setDeviceSn(deviceSn);
					if (deviceService.updatePortrait(device)) {
						result.setCode(Errors.ERR_SUCCESS);
					} else {
						result.setCode(Errors.ERR_FAILED);
					}
				} else {
					result.setMsg("reg data code error!");
					result.setCode(Errors.ERR_FAILED);
				}
			} else {
				result.setCode(Errors.ERR_FAILED);
				result.setMsg("reg RPC error!");
			}
		}

		result.setCode(Errors.ERR_SUCCESS);
		return result;
	}

	/**
	 * 查询我的资料
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getDeviceInfo(Map<String, String[]> params) {

		ApiResult result = new ApiResult();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		if (!TextUtils.isEmpty(deviceSn)) {
			DeviceInfo deviceInfoView = deviceInfoService.getDeviceInfo(deviceSn);
			if (!TextUtils.isEmpty(deviceInfoView.getHeadPortrait())) {
				deviceInfoView.setHeadPortrait(env.getProperty("recource.url") + deviceInfoView.getHeadPortrait());
			}
			result.setData(deviceInfoView);
			result.setCode(Errors.ERR_SUCCESS);
			return result;
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
	}

	/**
	 * 新增/修改我的资料
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult setDeviceInfo(Map<String, String[]> params) {

		ApiResult result = new ApiResult();

		String did = DataUtil.getStringFromMap(params, "did");
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String humanName = DataUtil.getStringFromMap(params, "humanName");
		String humanSex = DataUtil.getStringFromMap(params, "humanSex");
		String humanBirthday = DataUtil.getStringFromMap(params, "humanBirthday");
		String humanHeight = DataUtil.getStringFromMap(params, "humanHeight");
		String humanWeight = DataUtil.getStringFromMap(params, "humanWeight");
		String humanStep = DataUtil.getStringFromMap(params, "humanStep");
		String humanFeature = DataUtil.getStringFromMap(params, "humanFeature");

		DeviceInfo deviceInfoView = new DeviceInfo();

		if (!TextUtils.isEmpty(did)) {
			DeviceInfo deviceInfoViewSelect = deviceInfoService.select(Integer.parseInt(did));

			if (null != deviceInfoViewSelect) {
				deviceInfoView = deviceInfoService.getDeviceInfo(deviceSn);
			}
			if (!TextUtils.isEmpty(did)) {
				deviceInfoView.setDid(Integer.parseInt(did));
			}

			if (!TextUtils.isEmpty(humanName)) {
				deviceInfoView.setHumanName(humanName);
			}
			if (!TextUtils.isEmpty(humanSex)) {
				deviceInfoView.setHumanSex(Integer.parseInt(humanSex));
			}
			if (!TextUtils.isEmpty(humanBirthday)) {
				deviceInfoView.setHumanBirthday(DateTimeUtil.strToDateLong(humanBirthday));
			}
			if (!TextUtils.isEmpty(humanHeight)) {
				deviceInfoView.setHumanHeight(Float.parseFloat(humanHeight));
			}
			if (!TextUtils.isEmpty(humanWeight)) {
				deviceInfoView.setHumanWeight(Float.parseFloat(humanWeight));

			}
			if (!TextUtils.isEmpty(humanStep)) {
				deviceInfoView.setHumanStep(Float.parseFloat(humanStep));
			}
			if (!TextUtils.isEmpty(humanFeature)) {
				deviceInfoView.setHumanFeature(humanFeature);
			}
			if (deviceInfoViewSelect != null) {
				deviceInfoService.updateDeviceInfo(deviceInfoView);
			} else {
				deviceInfoService.addDeviceInfo(deviceInfoView);
			}

			result.setMsg(LanguageManager.getMsg("common.save.success"));
			result.setCode(Errors.ERR_SUCCESS);
			return result;
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}

	}

	/**
	 * 新增/修改SOS号码
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult setSosNo(Map<String, String[]> params) {

		ApiResult result = new ApiResult();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String mobile1 = DataUtil.getStringFromMap(params, "mobile1");
		String mobile2 = DataUtil.getStringFromMap(params, "mobile2");
		String mobile3 = DataUtil.getStringFromMap(params, "mobile3");

		Tdevice device = null;

		if (!TextUtils.isEmpty(deviceSn)) {
			device = new Tdevice();
			device.setDeviceSn(deviceSn);
			if (!TextUtils.isEmpty(mobile1)) {
				device.setMobile1(mobile1);
			}
			if (!TextUtils.isEmpty(mobile2)) {
				device.setMobile2(mobile2);
			}
			if (!TextUtils.isEmpty(mobile3)) {
				device.setMobile3(mobile3);
			}

			if (deviceService.updateMobile(device)) {
				result.setCode(Errors.ERR_SUCCESS);
			} else {
				result.setCode(Errors.ERR_FAILED);
			}
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));

		}
		return result;
	}

	/**
	 * 查询SOS号码
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getSosNo(Map<String, String[]> params) {

		ApiResult result = new ApiResult();
		Map<String, String> map = new HashMap<String, String>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		if (!TextUtils.isEmpty(deviceSn)) {
			Tdevice device = deviceService.selectMobile(deviceSn);

			map.put("mobile1", device.getMobile1());
			map.put("mobile2", device.getMobile2());
			map.put("mobile3", device.getMobile3());

			result.setData(map);
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
		}
		return result;
	}

	/**
	 * 新增运动目标
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult setStepTarget(Map<String, String[]> params) {

		ApiResult result = new ApiResult();
		result.setCode(Errors.ERR_FAILED);

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String steps = DataUtil.getStringFromMap(params, "steps");

		Tdevice device = null;

		if (!TextUtils.isEmpty(deviceSn) && !TextUtils.isEmpty(steps)) {
			device = new Tdevice();
			device.setDeviceSn(deviceSn);
			device.setSteps(Integer.parseInt(steps));
			if (deviceService.updateSteps(device)) {
				result.setCode(Errors.ERR_SUCCESS);
			}
			return result;

		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
	}

	/**
	 * 查询运动目标
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getStepTarget(Map<String, String[]> params) {

		ApiResult result = new ApiResult();
		Map<String, String> map = new HashMap<String, String>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		if (!TextUtils.isEmpty(deviceSn)) {
			Tdevice device = deviceService.selectSteps(deviceSn);
			if (device != null) {
				map.put("steps", device.getSteps().toString());
				result.setData(map);
			} else {
				result.setData(device);
			}
			result.setCode(Errors.ERR_SUCCESS);
			return result;
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}

	}

	/**
	 * 新增GPS数据
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult setGps(Map<String, String[]> params) {
		ApiResult result = new ApiResult();

		String did = DataUtil.getStringFromMap(params, "did");
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String collectDatetime = DataUtil.getStringFromMap(params, "collectDatetime");
		String lat = DataUtil.getStringFromMap(params, "lat");
		String lng = DataUtil.getStringFromMap(params, "lng");
		String speed = DataUtil.getStringFromMap(params, "speed");
		String direction = DataUtil.getStringFromMap(params, "direction");
		String gpsFlag = DataUtil.getStringFromMap(params, "gpsFlag");
		String lbsWifiRange = DataUtil.getStringFromMap(params, "lbsWifiRange");

		if (!TextUtils.isEmpty(did) && !TextUtils.isEmpty(deviceSn) && !TextUtils.isEmpty(collectDatetime)
				&& !TextUtils.isEmpty(lat) && !TextUtils.isEmpty(lng)) {
			TgpsData gpsData = new TgpsData();
			GpsDataLast gpsDataLast = new GpsDataLast();
			gpsData.setDid(Integer.parseInt(did));
			gpsData.setDeviceSn(deviceSn);
			gpsData.setCollectDatetime(DateTimeUtil.strToDateLong(collectDatetime));
			gpsData.setRcvTime(new Date());
			gpsData.setLat(Double.parseDouble(lat));
			gpsData.setLng(Double.parseDouble(lng));

			gpsDataLast.setDeviceSn(deviceSn);
			gpsDataLast.setCollectDatetime(DateTimeUtil.strToDateLong(collectDatetime));
			gpsDataLast.setRcvTime(new Date());
			gpsDataLast.setLat(Double.parseDouble(lat));
			gpsDataLast.setLng(Double.parseDouble(lng));
			gpsDataLast.setOnline(1);

			if (!TextUtils.isEmpty(speed)) {
				gpsData.setSpeed(Float.parseFloat(speed));
				gpsDataLast.setSpeed(Float.parseFloat(speed));
			}
			if (!TextUtils.isEmpty(direction)) {
				gpsData.setDirection(Float.parseFloat(direction));
				gpsDataLast.setDirection(Float.parseFloat(direction));
			}
			if (!TextUtils.isEmpty(gpsFlag)) {
				gpsData.setGpsFlag(gpsFlag);
				gpsDataLast.setGpsFlag(gpsFlag);
			}

			if (!TextUtils.isEmpty(lbsWifiRange)) {
				gpsData.setLbsWifiRange(Integer.parseInt(lbsWifiRange));
				gpsDataLast.setLbsWifiRange(Integer.parseInt(lbsWifiRange));
			}

			PushInfo pushInfo = new PushInfo();
			pushInfo.setEquipId(deviceSn);
			pushInfo.setLat(Double.parseDouble(lat));
			pushInfo.setLng(Double.parseDouble(lng));
			pushInfo.setMsgType(EnumUtils.PushMsgType.GPS);
			pushInfo.setDatetime(DateTimeUtil.strToDateLong(DateTimeUtil.getCurrentUtcDatetime()));

			List<UserConn> userConnList = userService.getUserDeviceInfo(pushInfo.getEquipId());
			if (null != userConnList && userConnList.size() > 0) {
				for (UserConn userConn : userConnList) {
					if (null == userConn.getToken()) {
						taskServiceConsumer.pushMsg(pushInfo, userConn);
					}
				}
			}

			if (null == gpsDataLastService.select(deviceSn)) {
				gpsDataLastService.add(gpsDataLast);
			} else {
				gpsDataLastService.modify(gpsDataLast);
			}
			tgpsDataService.add(gpsData);

			result.setCode(Errors.ERR_SUCCESS);
			return result;

		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
	}

	/**
	 * 查询设备token
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getDeviceToken(Map<String, String[]> params) {

		ApiResult result = new ApiResult();
		ChatToken chatToken = null;

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		if (!TextUtils.isEmpty(deviceSn)) {
			chatToken = chatTokenService.select(deviceSn);
			if (null != chatToken) {
				result.setData(chatToken);
				result.setCode(Errors.ERR_SUCCESS);
			} else {
				RPCResult rpcResult = taskServiceConsumer.getRongcloudToken(deviceSn);
				if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS) {
					ChatToken rpcChatToken = (ChatToken) rpcResult.getRpcResult();
					if (rpcChatToken.getName().equals(deviceSn) && null != rpcChatToken.getToken()
							&& chatTokenService.add(rpcChatToken)) {
						result.setData(rpcChatToken);
						result.setCode(Errors.ERR_SUCCESS);
						LogManager.info("Token save OK deviceSn={}, token={}", deviceSn, rpcChatToken.getToken());
					} else {
						result.setCode(Errors.ERR_FAILED);
						LogManager.info("Token save error deviceSn={}, token={}", deviceSn, rpcChatToken.getToken());
					}
				}
			}
			return result;
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
			return result;
		}
	}

	/**
	 * saveUserToken
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult saveUserToken(Map<String, String[]> params) {

		ApiResult result = new ApiResult();
		Map<String, String> map = new HashMap<String, String>();
		String username = DataUtil.getStringFromMap(params, "username");
		String token = DataUtil.getStringFromMap(params, "token");
		if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(token)) {

			map.put("username", username);
			map.put("token", token);
			RPCResult RpcResult = regConsumerService.hanleRegService("saveUserToken", map);
			if (RpcResult.getRpcErrCode() == Errors.ERR_SUCCESS) {
				JSONObject rpcData = JSONObject.fromObject(RpcResult.getRpcResult());
				if (rpcData.get("code").toString().equals("0")) {
					UserToken userToken = userTokenService.select(username);
					if (null != userToken) {
						userToken.setToken(token);
						if (userTokenService.modify(userToken)) {
							result.setCode(Errors.ERR_SUCCESS);
						} else {
							result.setCode(Errors.ERR_FAILED);
						}
					} else {
						userToken = new UserToken();
						userToken.setName(username);
						userToken.setToken(token);
						userToken.setVersions(3);
						userToken.setCertificate(1);
						if (userTokenService.add(userToken)) {
							result.setCode(Errors.ERR_SUCCESS);
						} else {
							result.setCode(Errors.ERR_FAILED);
						}
					}

				}
			} else {
				result.setCode(Errors.ERR_FAILED);
				result.setMsg("saveUserToken Reg RPC fail!");
			}

		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
		}
		return result;
	}

}
