package com.htdz.reg.service;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.htdz.common.LanguageManager;
import com.htdz.common.utils.DataUtil;
import com.htdz.common.utils.StringCompare;
import com.htdz.common.utils.StringSubUtil;
import com.htdz.db.service.DeviceService;
import com.htdz.db.service.GroupUserService;
import com.htdz.db.service.TConnServerService;
import com.htdz.db.service.UserTokenService;
import com.htdz.def.data.ApiResult;
import com.htdz.def.data.Errors;
import com.htdz.def.dbmodel.Tdevice;
import com.htdz.def.dbmodel.UserToken;
import com.htdz.def.view.GroupUserInfo;
import com.htdz.reg.util.DaoUtil;
import com.htdz.reg.util.Verifier;

@Service
public class ApiService {

	@Autowired
	private DaoUtil daoUtil;
	@Autowired
	private GroupUserService groupUserService;
	@Autowired
	private UserTokenService userTokenService;
	@Autowired
	private DeviceService deviceService;
	@Autowired
	private TConnServerService tConnServerService;

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public Map<String, Object> getServerConnInfo(Map<String, String[]> params, Map<String, String> headers) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		List<Map<String, Object>> returnlist = new ArrayList<Map<String, Object>>();

		String version = DataUtil.getStringFromMap(params, "version");
		String language = headers.get("accept-language");

		String country = "";
		if (null != language && language.toUpperCase().indexOf("CN") != -1) {
			country = "CN";
		} else {
			country = "HK";
		}

		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> list1 = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> list2 = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> list3 = new ArrayList<Map<String, Object>>();

		if (!TextUtils.isEmpty(version) && StringCompare.compareVersion(version, "3.1.5") > 0) {
			list = daoUtil.getServerConnInfoByCountry(country);
		} else {
			list1 = daoUtil.getServerConnInfoByCountry("CN");

			list2 = daoUtil.getServerConnInfoByCountry("SG");
			list3 = daoUtil.getServerConnInfoByCountry("SG");

			for (Map<String, Object> map : list2) {

				if (map.get("conn_country").equals("SG")) {
					map.put("conn_country", "HK");
				}
			}

			returnlist.addAll(list1);
			returnlist.addAll(list2);
			returnlist.addAll(list3);
		}

		if (list != null && list.size() >= 1) {
			for (Map<String, Object> map : list) {
				map.put("conn_country_des", "");
				returnlist.add(map);
			}
		}

		mapResponse.put("code", "0");
		mapResponse.put("ret", returnlist);
		mapResponse.put("what", "");
		return mapResponse;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public Map<String, Object> getServerConnInfoByUser(Map<String, String[]> params, Map<String, String> headers) {

		String username = DataUtil.getStringFromMap(params, "username");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		List<Map<String, Object>> list = daoUtil.getServerConnInfoByUser(username);
		if (list != null && list.size() >= 1) {
			Map<String, Object> map = list.get(0);
			map.put("conn_country_des", "");

			mapResponse.put("code", "0");
			mapResponse.put("ret", map);
			mapResponse.put("what", "");
			return mapResponse;
		}

		mapResponse.put("code", "300");
		mapResponse.put("ret", "");
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
		return mapResponse;

	}

	/**
	 * 获取群成员信息
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getServerConnInfoByUser(Map<String, String> params) {
		ApiResult result = new ApiResult();
		String username = params.get("username");
		Map<String, Object> tConnServer = tConnServerService
				.select("SELECT cs.`conn_country`,cs.`connid`,cs.`conn_name`,cs.`conn_ext` AS conn_dns,cs.`conn_port` FROM `TConnServer` AS cs INNER JOIN `TUser` AS u ON u.`conn_country`=cs.`conn_country` AND cs.`conn_device`=2 AND cs.`conn_type`=2 WHERE u.`name`= '"
						+ username + "'");
		if (!tConnServer.isEmpty()) {
			result.setData(tConnServer);
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg("usernoregister");
		}

		return result;
	}

	/**
	 * 获取群成员信息
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getGroupUser(Map<String, String> params) {
		ApiResult result = new ApiResult();
		String deviceSn = params.get("deviceSn");
		List<GroupUserInfo> groupUserInfo = groupUserService.getGroupUserInfo(deviceSn);
		if (!groupUserInfo.isEmpty()) {
			result.setData(groupUserInfo);
		}
		result.setCode(Errors.ERR_SUCCESS);

		return result;
	}

	/**
	 * 获取群成员信息
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getGroupUserByName(Map<String, String> params) {
		ApiResult result = new ApiResult();
		String deviceSn = params.get("deviceSn");
		String username = params.get("username");
		GroupUserInfo groupUserInfo = groupUserService.getGroupUserByName(deviceSn, username);
		if (null != groupUserInfo) {
			result.setData(groupUserInfo);
		}
		result.setCode(Errors.ERR_SUCCESS);

		return result;
	}

	/**
	 * saveUserToken
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult saveUserToken(Map<String, String> params) {

		ApiResult result = new ApiResult();

		String username = params.get("username");
		String token = params.get("token");
		if (!TextUtils.isEmpty(username) && token != null) {
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
		} else {
			result.setCode(Errors.ERR_FAILED);
			result.setMsg(LanguageManager.getMsg("commom.paramerror"));
		}
		return result;
	}

	/**
	 * 设置头像
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult setHeadPic(Map<String, String> params) {
		ApiResult result = new ApiResult();
		String deviceSn = params.get("deviceSn");
		String url = params.get("url");
		Tdevice device = new Tdevice();

		if (!TextUtils.isEmpty(deviceSn) && !TextUtils.isEmpty(url)) {
			device.setHeadPortrait(StringSubUtil.getRelativeURL(url));
			device.setDeviceSn(deviceSn);
			if (deviceService.updatePortrait(device)) {
				result.setCode(Errors.ERR_SUCCESS);
			} else {
				result.setCode(Errors.ERR_FAILED);
			}
		}

		result.setCode(Errors.ERR_SUCCESS);
		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult binding(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");
		String enabled = params.get("enabled");
		String expired_time = params.get("expired_time");
		String expired_time_de = params.get("expired_time_de");
		String ranges = params.get("ranges");
		String tracker_sim = params.get("tracker_sim");
		String nickname = params.get("nickname");
		int aroundRanges = params.get("aroundRanges") != null ? Integer.parseInt(params.get("aroundRanges").toString())
				: 0;
		String protocoltype = params.get("protocoltype");
		String product_type = params.get("product_type");
		String username = params.get("username");

		if (daoUtil.trackerBindUser(deviceSn, username, protocoltype, product_type, enabled, expired_time,
				expired_time_de, ranges, tracker_sim, nickname, aroundRanges)) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	public ApiResult getDeviceInfoCN(Map<String, String> params) {
		ApiResult result = new ApiResult();
		String username = params.get("username");
		List<Map<String, Object>> list = null;

		list = daoUtil.getDeviceInfoCN(username);
		if (list != null && list.size() >= 1) {
			for (Map<String, Object> map : list) {
				if (map.containsKey("nickname") && map.get("nickname") != null) {
					try {
						map.put("nickname", URLEncoder.encode(map.get("nickname").toString(), "UTF-8"));
					} catch (Exception e) {
						//
					}
				}

				if (map.containsKey("cdt_enable") && map.get("cdt_enable") != null) {
					if (map.get("cdt_enable").toString().length() > 1) {
						map.put("cdt_enable", 0);
					} else {
						map.put("cdt_enable", Integer.parseInt(map.get("cdt_enable").toString()));
					}
				}
			}
		}

		List<Map<String, Object>> listNotINDevice = daoUtil.getDeviceUserNotINDevice(username);
		/*
		 * if (null != listNotINDevice && listNotINDevice.size() > 0) {
		 * StringBuffer buffer =new StringBuffer(); String RemoteDevice = null;
		 * for (Map<String, Object> map : listNotINDevice) {
		 * buffer.append("\"").append(map.get("device_sn")).append("\"").append(
		 * ","); } RemoteDevice = buffer.substring(0, buffer.length()-1);
		 * 
		 * try { Map<String, String> param = new HashMap<String, String>();
		 * param.put("function", "getDeviceInfoRemote");
		 * param.put("remoteServerRequest", "true"); param.put("devices",
		 * RemoteDevice); String requestPath =
		 * PropertyUtil.getPropertyValue("web","remotingServer_Reg"); JSONObject
		 * json = doPost_urlconn(requestPath, param); List<Map<String,Object>>
		 * listRe =
		 * (List<Map<String,Object>>)json.get(ReturnObject.RETURN_OBJECT);
		 * list.addAll(listRe); } catch (Exception e) {
		 * 
		 * } }
		 */

		result.setData(list);
		result.setCode(Errors.ERR_SUCCESS);

		return result;
	}

	public ApiResult addGroupUser(Map<String, String> params) {
		ApiResult result = new ApiResult();
		String deviceSn = params.get("deviceSn");
		String names = params.get("names");

		String[] name = names.split(",");
		int flag = 0;
		for (String string : name) {
			int type = 0;
			if (!Verifier.validateEmail(string)) {
				type = 1;
			}
			if (!string.isEmpty()) {
				if (!daoUtil.checkGroupUser(string, deviceSn)) {
					flag = daoUtil.addGroupUser(string, type, deviceSn);
				} else {
					result.setCode(Errors.ERR_FAILED);
					return result;
				}
			}
		}

		if (flag > 0) {
			result.setCode(Errors.ERR_SUCCESS);
			return result;
		}

		result.setCode(Errors.ERR_FAILED);
		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult deletedevice(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");

		if (daoUtil.deletedevice(deviceSn)) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult authorization(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");
		String username = params.get("username");
		String nickname = params.get("nickname") != null ? params.get("nickname").toString() : "";
		int isGps = (params.get("isGps") != null && !params.get("isGps").isEmpty())
				? Integer.parseInt(params.get("isGps").toString()) : 1;

		if (!daoUtil.authorization(deviceSn, username, nickname, isGps)) {
			result.setCode(Errors.ERR_FAILED);
		}

		result.setData(daoUtil.getTrackerUser(deviceSn));
		result.setCode(Errors.ERR_SUCCESS);

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult deleteGroup(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");

		if (daoUtil.deleteGroup(deviceSn)) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult unauthorization(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");
		String username = params.get("username");

		if (daoUtil.unauthorization(deviceSn, username)) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult updatedeviceinfo(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");
		String nickname = "";
		try {
			nickname = URLDecoder.decode(params.get("nickname"), "UTF-8");
		} catch (Exception e) {
			nickname = params.get("nickname");
		}

		String defensive = params.get("defensive");
		String head_portrait = params.get("head_portrait");
		String gps_interval = params.get("gps_interval");
		String bt_enable = params.get("bt_enable");
		String cdt_enable = params.get("cdt_enable");
		String tracker_sim = params.get("tracker_sim");
		String ranges = params.get("ranges");
		int aroundRanges = params.get("aroundRanges") != null ? Integer.parseInt(params.get("aroundRanges").toString())
				: 0;

		if (daoUtil.updatedeviceinfo(nickname, defensive, head_portrait, gps_interval, bt_enable, cdt_enable,
				tracker_sim, ranges, deviceSn, aroundRanges)) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult settimezone(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String username = params.get("username");
		String timezone = params.get("timezone");
		String timezoneId = params.get("timezoneId");
		String timezoneCheck = params.get("timezoneCheck");

		if (daoUtil.settimezone(username, timezone, timezoneId, timezoneCheck)) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getTrackerUser(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");

		result.setCode(Errors.ERR_SUCCESS);
		result.setData(daoUtil.getTrackerUser(deviceSn));

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult updateDeviceExpirationTime(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");
		String expired_time = params.get("expired_time");
		String expired_time_de = params.get("expired_time_de");
		String disable = params.get("disable");

		if (daoUtil.updateDeviceExpirationTime(deviceSn, expired_time_de, expired_time, disable) > 0) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult issuperuser(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");
		String username = params.get("username");

		result.setCode(Errors.ERR_SUCCESS);
		result.setData(daoUtil.issuperuser(username, deviceSn));

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getdeviceserverandusername(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");

		result.setCode(Errors.ERR_SUCCESS);
		result.setData(daoUtil.getdeviceserverandusername(deviceSn));

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getauthorizationuserinfo(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");
		String username = params.get("username");

		result.setCode(Errors.ERR_SUCCESS);
		result.setData(daoUtil.getauthorizationuserinfo(deviceSn, username));

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult updateIsCustomizedApp(Map<String, String> params) {
		ApiResult result = new ApiResult();

		int isCustomizedApp = (params.get("isCustomizedApp") == null || params.get("isCustomizedApp").isEmpty()) ? 0
				: Integer.parseInt(params.get("isCustomizedApp"));
		String username = params.get("username");

		if (daoUtil.updateIsCustomizedApp(username, isCustomizedApp) > 0) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult registerCN(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String username = params.get("username");
		String serverNo = params.get("serverNo");
		String timezone = params.get("timezone");
		String timezoneid = params.get("timezoneid");
		String isCustomizedApp = params.get("isCustomizedApp");

		if (daoUtil.checkUserExistCN(username)) {
			result.setCode(100);
		}

		if (daoUtil.registerUserCN(username, Integer.parseInt(serverNo), timezone, timezoneid, isCustomizedApp)) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult modifyAccountRemark(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String authorizedname = params.get("authorizedname");
		String deviceSn = params.get("deviceSn");
		String nickname = params.get("nickname");
		int isGps = (params.get("isGps") != null && !params.get("isGps").isEmpty())
				? Integer.parseInt(params.get("isGps").toString()) : 1;

		if (daoUtil.modifyAccountRemark(authorizedname, deviceSn, nickname, isGps) > 0) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult getGpsInfo(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");
		String username = params.get("username");

		result.setCode(Errors.ERR_SUCCESS);
		result.setData(daoUtil.getGpsInfo(deviceSn, username));

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult deleteGroupUser(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");
		String names = params.get("name");

		String[] name = names.split(",");
		boolean flag = false;
		for (String string : name) {

			if (!string.isEmpty()) {
				if (daoUtil.checkGroupUser(string, deviceSn)) {
					flag = daoUtil.deleteGroupUser(string, deviceSn);
				} else {
					result.setCode(Errors.ERR_FAILED);
					return result;
				}
			}
		}
		if (flag) {
			result.setCode(Errors.ERR_SUCCESS);
			return result;

		}

		result.setCode(Errors.ERR_FAILED);
		return result;
	}

	public ApiResult getInviteAuthUser(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");

		List<Map<String, Object>> groupUserMap = daoUtil.getInviteAuthUser(deviceSn);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("obj", groupUserMap);
		result.setCode(Errors.ERR_SUCCESS);
		result.setData(map);

		return result;
	}

	/**
	 * 
	 * 
	 * @param params
	 * @return
	 */
	public ApiResult updateUserInfo(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String username = params.get("username");
		String nickname = params.get("nickname");
		String portrait = params.get("portrait");

		if (daoUtil.updateUserInfo(username, nickname, portrait)) {
			result.setCode(Errors.ERR_SUCCESS);
		} else {
			result.setCode(Errors.ERR_FAILED);
		}

		return result;
	}

	public ApiResult getGroupUserForLiteG(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");

		List<Map<String, Object>> groupUserMap = daoUtil.getGroupUser(deviceSn);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("obj", groupUserMap);
		result.setData(map);
		if (groupUserMap == null) {
			result.setCode(Errors.ERR_FAILED);
		} else {
			result.setCode(Errors.ERR_SUCCESS);
		}
		return result;

	}

	public ApiResult getGroupUserByNameForLiteG(Map<String, String> params) {
		ApiResult result = new ApiResult();

		String deviceSn = params.get("deviceSn");
		String name = params.get("name");

		List<Map<String, Object>> groupUserMap = daoUtil.getGroupUser(name, deviceSn);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("obj", groupUserMap);
		result.setData(map);

		if (groupUserMap == null) {
			result.setCode(Errors.ERR_FAILED);
		} else {
			result.setCode(Errors.ERR_SUCCESS);
		}

		return result;
	}
}
