package com.htdz.liteguardian.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.util.TextUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alipay.config.AlipayConfig;
import com.alipay.util.MD5;
import com.htdz.common.LanguageManager;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;
import com.htdz.common.utils.DateTimeUtil;
import com.htdz.common.utils.EnumUtils;
import com.htdz.def.data.Errors;
import com.htdz.def.data.RPCResult;
import com.htdz.liteguardian.dubbo.RegServiceConsumer;
import com.htdz.liteguardian.message.CastelMessage;
import com.htdz.liteguardian.message.GatewayHandler;
import com.htdz.liteguardian.message.GatewayHttpHandler;
import com.htdz.liteguardian.obd.DriveBehaviorHandler;
import com.htdz.liteguardian.obd.TdriverBehaviorAnalysis;
import com.htdz.liteguardian.util.DaoUtil;
import com.htdz.liteguardian.util.DistanceUtil;
import com.htdz.liteguardian.util.EmailUtil;
import com.htdz.liteguardian.util.EncryptRSA;
import com.htdz.liteguardian.util.FileUtil;
import com.htdz.liteguardian.util.PropertyUtil;
import com.htdz.liteguardian.util.RandomUtil;
import com.htdz.liteguardian.util.ReturnObject;
import com.htdz.liteguardian.util.Session;
import com.htdz.liteguardian.util.SessionUtil;
import com.htdz.liteguardian.util.UtilDate;
import com.htdz.liteguardian.util.Verifier;
import com.paypal.api.payments.Payment;
import com.paypal.base.rest.APIContext;
import com.paypal.base.rest.OAuthTokenCredential;
import com.paypal.base.rest.PayPalRESTException;

import io.rong.RongCloud;
import io.rong.messages.CmdNtfMessage;
import io.rong.messages.InfoNtfMessage;
import io.rong.messages.TxtMessage;
import io.rong.messages.VoiceMessage;
import io.rong.models.CodeSuccessResult;
import io.rong.models.GroupUserQueryResult;
import io.rong.models.TokenResult;

/**
 * 业务层，具体业务的实现
 */
@Service
public class ApiService {

	@Autowired
	private DaoUtil daoUtil;

	@Autowired
	private EmailUtil emailUtil;
	@Autowired
	private Environment env;

	@Autowired
	private RegServiceConsumer regConsumerService;

	@Autowired
	private GatewayHandler gatewayHandler;

	@Autowired
	private GatewayHttpHandler gatewayHttpHandler;

	@Value("${app.key}")
	private String appKey;
	@Value("${app.secret}")
	private String appSecret;

	// private final DaoStatic ds = DaoStatic.getInstance();
	// private WebAPIService instance = null;

	public final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(20000).setConnectTimeout(20000)
			.build();

	public final String encoding = "UTF-8";

	private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 设置时间格式

	private static final Integer loginTryTimes = 10;
	public static final int maxGeoFenceAmount = 2;

	static KeyPair keyPair;

	public Map<String, Object> setUserPortrait(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> mapReturn = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);

		String newfilename = DateTimeUtil.getCurrentUtcDatetime("yyyyMMddHHmmss")
				+ Integer.toString(new Random().nextInt(10000)) + ".png";

		String localfileDir = env.getProperty("upload.path") + "/" + env.getProperty("user.folder");

		FileUtil.getFile(reqBody, localfileDir, newfilename);

		String relPath = env.getProperty("virtual.file.location") + "/" + env.getProperty("user.folder") + "/"
				+ newfilename;

		// 取文件路径
		Map<String, String> returnMap = new HashMap<String, String>();
		returnMap.put("headPortrait", relPath);
		returnMap.put("headerImage", relPath);
		returnMap.put("localpath", "");

		String neturl = "";
		if (null != returnMap) {
			neturl = returnMap.get("headPortrait");
		}
		if (daoUtil.updateUserPortrait(username, neturl)) {
			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "updateUserInfo");
			param.put("username", username);
			param.put("portrait", neturl);

			RPCResult rpcResult = regConsumerService.hanleRegService("updateUserInfo", param);
			try {
				if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
						|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
					mapReturn.put(ReturnObject.RETURN_CODE, "100");
					mapReturn.put(ReturnObject.RETURN_OBJECT, returnMap);
					mapReturn.put(ReturnObject.RETURN_WHAT, "");
					return mapReturn;
				}

				mapReturn.put(ReturnObject.RETURN_CODE, "0");
				mapReturn.put(ReturnObject.RETURN_OBJECT, returnMap);
				mapReturn.put(ReturnObject.RETURN_WHAT, "");
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return mapReturn;
	}

	public Map<String, Object> setHeadPortrait(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> mapReturn = new HashMap<String, Object>();
		Map<String, String> returnMap = new HashMap<String, String>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapReturn.put(ReturnObject.RETURN_CODE, "100");
			mapReturn.put(ReturnObject.RETURN_OBJECT, "");
			mapReturn.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapReturn;
		}

		String newfilename = DateTimeUtil.getCurrentUtcDatetime("yyyyMMddHHmmss")
				+ Integer.toString(new Random().nextInt(10000)) + ".png";

		String localfileDir = env.getProperty("upload.path") + "/" + env.getProperty("device.folder");

		FileUtil.getFile(reqBody, localfileDir, newfilename);

		String relPath = env.getProperty("virtual.file.location") + "/" + env.getProperty("device.folder") + "/"
				+ newfilename;

		returnMap.put("headPortrait", relPath);
		returnMap.put("headerImage", relPath);
		returnMap.put("localpath", "");

		String neturl = "";
		if (null != returnMap) {
			neturl = returnMap.get("headPortrait");
		}

		int deviceId = daoUtil.getDeviceID(deviceSn);
		if (deviceId > 0) {
			if (daoUtil.changDeviceHeadPortrait(deviceId, neturl)) {
				Map<String, String> param = new HashMap<String, String>();
				param.put("function", "updatedeviceinfo");
				param.put("deviceSn", deviceSn);
				param.put("head_portrait", neturl);

				try {
					regConsumerService.hanleRegService("updatedeviceinfo", param);

					// add by ljl 20160505返回设备所在服务ip和port
					Object obj = getdeviceserverandusername(deviceSn);
					if (obj != null) {
						String serverIp = ((JSONObject) obj).getString("conn_name");
						String serverPort = ((JSONObject) obj).getString("conn_port");
						returnMap.put("conn_name", serverIp);
						returnMap.put("conn_port", serverPort);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}

			}
		}

		mapReturn.put(ReturnObject.RETURN_CODE, "0");
		mapReturn.put(ReturnObject.RETURN_OBJECT, returnMap);
		mapReturn.put(ReturnObject.RETURN_WHAT, "");
		return mapReturn;
	}

	/**
	 * 发语音消息
	 * 
	 * @param fileStream
	 * @param request
	 * @return
	 */
	public Map<String, Object> sendMessageVoice(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String type = DataUtil.getStringFromMap(params, "type"); // 0
																	// APP发送给设备，1设备发送给APP
		Map<String, Object> map = new HashMap<>();

		try {
			String recDeviceSn = DataUtil.getStringFromMap(params, "recDeviceSn");// 接收消息设备号（APP调必传，网关调不传）
			String sendDeviceSn = DataUtil.getStringFromMap(params, "sendDeviceSn");// 发送消息设备号（APP调不传，网关调必传）

			if (type.equals("0")) {// APP发送给设备
				// 消息发送给网关

				Object obj = getdeviceserverandusername(recDeviceSn);
				if (obj == null) {
					map.put(ReturnObject.RETURN_CODE, "200");
					map.put(ReturnObject.RETURN_OBJECT, null);
					map.put(ReturnObject.RETURN_WHAT, null);
					return map;
				} else {

					CastelMessage responseMsg = gatewayHttpHandler.sendVoiceMessage(recDeviceSn, reqBody);

					if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
							&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
						map.put("code", "0");
						map.put("ret", "");
						map.put("what", null);
						return map;
					} else {
						map.put(ReturnObject.RETURN_CODE, "300");
						map.put(ReturnObject.RETURN_OBJECT, null);
						map.put(ReturnObject.RETURN_WHAT, null);
					}

				}

			} else {// 设备发送给APP

				String newfilename = DateTimeUtil.getCurrentUtcDatetime("yyyyMMddHHmmss")
						+ Integer.toString(new Random().nextInt(10000)) + ".png";

				String localfileDir = env.getProperty("upload.path") + "/" + env.getProperty("user.folder");

				FileUtil.getFile(reqBody, localfileDir, newfilename);

				String localpath = localfileDir + File.separator + newfilename;
				File file = new File(localpath);
				if (null != localpath) {
					String base64str = FileUtil.encodeBase64File(localpath).trim().replaceAll("[\\t\\n\\r]", "");
					String[] messagePublishGroupToGroupId = { sendDeviceSn };

					// 发送群组消息方法（以一个用户身份向群组发送消息，单条消息最大 128k.每秒钟最多发送 20 条消息，每次最多向
					// 3 个群组发送，如：一次向 3 个群组发送消息，示为 3 条消息。）
					VoiceMessage voiceMessage = new VoiceMessage(base64str, null, FileUtil.getAmrDuration(file));
					CodeSuccessResult messagePublishGroupResult = RongCloud.getInstance(appKey, appSecret).message
							.publishGroup(sendDeviceSn, messagePublishGroupToGroupId, voiceMessage, null,
									"{\"pushData\":\"\"}", 1, 1, 0);

					if (messagePublishGroupResult.getCode() == 200) {
						map.put("code", "0");
						map.put("ret", "");
						map.put("what", null);
						return map;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();

			map.put(ReturnObject.RETURN_CODE, "400");
			map.put(ReturnObject.RETURN_OBJECT, null);
			map.put(ReturnObject.RETURN_WHAT, null);
			return map;
		}
		map.put(ReturnObject.RETURN_CODE, "500");
		map.put(ReturnObject.RETURN_OBJECT, null);
		map.put(ReturnObject.RETURN_WHAT, null);
		return map;
	}

	/**
	 * 获取注册时候的服务器编号(针对当前用户，无需处理)
	 */
	public Map<String, Object> getServerNo(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String username = DataUtil.getStringFromMap(params, "username");

		Map<String, Object> mapResponse = new HashMap<>();
		// 用户不存在就返回
		if (!daoUtil.isExistsUser(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, MessageFormat
					.format(LanguageManager.getMsg(headers.get("accept-language"), "forlogin.invalid_user"), username));
			return mapResponse;
		}
		Map<String, Object> mapRet = new HashMap<String, Object>();
		mapRet.put("serverNo", daoUtil.getServerNo(username));
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;
	}

	/**
	 * 绑定（需要将绑定结果同步到注册服务器）
	 * 
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> binding(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody)
			throws Exception {
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		if (deviceSn != null) {
			deviceSn = deviceSn.trim();
		}
		String simNo = DataUtil.getStringFromMap(params, "simNo") == null ? ""
				: DataUtil.getStringFromMap(params, "simNo");
		String ranges = DataUtil.getStringFromMap(params, "ranges");

		int protocoltype = 0;
		if (DataUtil.getStringFromMap(params, "protocoltype") != null
				&& !DataUtil.getStringFromMap(params, "protocoltype").isEmpty()) {
			protocoltype = Integer.parseInt(DataUtil.getStringFromMap(params, "protocoltype"));
		}

		String mobile1 = DataUtil.getStringFromMap(params, "mobile1") == null ? ""
				: DataUtil.getStringFromMap(params, "mobile1");
		String mobile2 = DataUtil.getStringFromMap(params, "mobile2") == null ? ""
				: DataUtil.getStringFromMap(params, "mobile2");
		String mobile3 = DataUtil.getStringFromMap(params, "mobile3") == null ? ""
				: DataUtil.getStringFromMap(params, "mobile3");
		// return binding(deviceSn, simNo, ranges,
		// mobile1, mobile2, mobile3, protocoltype, params);

		String aroundRanges = (DataUtil.getStringFromMap(params, "aroundRanges") != null
				&& !DataUtil.getStringFromMap(params, "aroundRanges").toString().equals(""))
						? DataUtil.getStringFromMap(params, "aroundRanges").toString() : "0";

		String nick_name = DataUtil.getStringFromMap(params, "nickname") != null
				? DataUtil.getStringFromMap(params, "nickname").toString() : "";// 授权设备备注
		Map<String, Object> mapResponse = new HashMap<>();

		// 超过用户最大绑定设备个数

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		int userID = daoUtil.getUserId(username);
		if (userID < 1) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_not_exist_or_timeout"));
			return mapResponse;
		}

		int quantityOfDevice = daoUtil.quantityOfUserDevice(userID);
		if (quantityOfDevice >= Integer.valueOf(env.getProperty("max.binding"))) {
			mapResponse.put(ReturnObject.RETURN_CODE, "310");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "forlogin.bindinglimit"));
			return mapResponse;
		}

		// 如果没有此台设备，则不能添加
		int deviceID = daoUtil.getDeviceID(deviceSn);

		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}

		// 一个设备只能绑定一次，已被绑定
		if (daoUtil.hasBinding(deviceID, 0) > 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "350");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.have_binding"));
			return mapResponse;
		}

		// 本用户已绑定，不能重复绑定
		if (daoUtil.hasBinding(deviceID, userID) > 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "380");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.already_bound"));
			return mapResponse;
		}

		// 获取设备productType信息
		Map<String, Object> productTypeMap = daoUtil.getDeviceType(deviceSn);
		// 获取设备producttype，productname信息
		List<Map<String, Object>> productInfoList = daoUtil.getProductInfo("");

		int productType = 0;
		if (productTypeMap != null) {
			productType = productTypeMap.get("product_type") != null
					? Integer.parseInt(productTypeMap.get("product_type").toString()) : 0;
		}

		Map<String, Object> deviceInfoMap = null;
		if (productType > 0) {
			deviceInfoMap = daoUtil.getDeviceInfoByProductType(productType);
		}

		if (deviceInfoMap != null) {
			protocoltype = deviceInfoMap.get("protocol_type") != null
					? Integer.parseInt(deviceInfoMap.get("protocol_type").toString()) : 0;
			ranges = deviceInfoMap.get("ranges") != null ? deviceInfoMap.get("ranges").toString() : "1";
		}

		String nickname = "";
		// 绑定时存入默认名称
		if (productInfoList != null && productInfoList.size() > 0) {
			for (Map<String, Object> obj : productInfoList) {
				int typeValue = obj.get("value") != null ? Integer.parseInt(obj.get("value").toString()) : 0;
				if (productType > 0 && productType == typeValue) {
					nickname = obj.get("name").toString();
					break;
				}

			}
		}
		// 更新设备int deviceID,int productType,String simNo,
		if (!daoUtil.updateDeviceCN(deviceID, productType, simNo, ranges, mobile1, mobile2, mobile3, protocoltype,
				nickname)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "320");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.save_tracker_failed"));
			return mapResponse;
		}

		// 激活注册邮件
		// temp mask
		// daoUtil.updateEmailVerifyStatus(username);

		// 添加设备默认过期日期
		Map<String, Object> mapObj = daoUtil.getOrderpackageByDeviceSn(deviceSn);
		int month = 12;// 默认12个月
		if (mapObj != null && mapObj.get("month") != null) {
			month = Integer.parseInt(mapObj.get("month").toString());
		}
		Calendar localdateDevice = null;
		if (month > 0) {
			localdateDevice = Calendar.getInstance(); // 得到日历
			localdateDevice.add(Calendar.MONTH, month);
		}

		// 存入设备范围

		if (daoUtil.changeDeviceAroundRanges(deviceID, Integer.parseInt(aroundRanges)) <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "update.aroundRanges.fail"));
			return mapResponse;
		}

		if (protocoltype == 0) {
			// 通知网关
			gatewayHandler.set1008(deviceSn, 1);
		}
		Map<String, Object> deviceinfo = daoUtil.getDevice(deviceID);
		String expiredTimeDe = "";
		// //设置设备过期时间 （设备有免费套餐，而且原数据为空）
		if (deviceinfo != null && localdateDevice != null && (deviceinfo.get("expired_time_de") == null
				|| deviceinfo.get("expired_time_de").toString().equals(""))) {
			daoUtil.setExpirationTimeDe(deviceID, localdateDevice.getTime());
			expiredTimeDe = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(localdateDevice.getTime());
		} else if (null != deviceinfo.get("expired_time_de")
				&& !deviceinfo.get("expired_time_de").toString().trim().equals("")) {// 没有免费套餐，原数据有值
			expiredTimeDe = deviceinfo.get("expired_time_de").toString();
		}

		if (true) {
			// 远程同步数据到注册服务器
			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "binding");
			// param.put("deviceSn", deviceSn);
			param.put("deviceSn",
					deviceinfo.get("device_sn") != null ? deviceinfo.get("device_sn").toString() : deviceSn);

			param.put("enabled", "1");
			param.put("expired_time",
					deviceinfo.get("expired_time") == null ? null : deviceinfo.get("expired_time").toString());
			param.put("expired_time_de", expiredTimeDe);
			param.put("ranges", deviceinfo.get("ranges") == null ? "1" : deviceinfo.get("ranges").toString());
			param.put("tracker_sim",
					deviceinfo.get("tracker_sim") == null ? "" : deviceinfo.get("tracker_sim").toString());
			param.put("protocoltype", Integer.toString(protocoltype));
			param.put("product_type",
					deviceinfo.get("product_type") == null ? "1" : deviceinfo.get("product_type").toString());
			param.put("username", username);
			param.put("nickname", nickname);// 设备昵称
			param.put("aroundRanges",
					deviceinfo.get("around_ranges") == null ? "1" : deviceinfo.get("around_ranges").toString());

			RPCResult rpcResult = regConsumerService.hanleRegService("binding", param);

			if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
					|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
				mapResponse.put(ReturnObject.RETURN_CODE, "320");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "forlogin.binding_tracker_failed"));
				return mapResponse;
			} else {
				if (!daoUtil.trackerBindUser(deviceID, userID, 1, nick_name)) {
					// TODO:ZSH 远程同步数据到注册服务器
					param = new HashMap<String, String>();
					param.put("function", "deletedevice");
					param.put("deviceSn", "'" + deviceSn + "'");
					regConsumerService.hanleRegService("deletedevice", param);

					mapResponse.put(ReturnObject.RETURN_CODE, "400");
					mapResponse.put(ReturnObject.RETURN_OBJECT, null);
					mapResponse.put(ReturnObject.RETURN_WHAT,
							LanguageManager.getMsg(headers.get("accept-language"), "forlogin.binding_tracker_failed"));
					return mapResponse;
				} else {
					if (protocoltype == 0) {
						gatewayHandler.set101E(deviceSn, mobile1, "", "");
					}
				}
			}
		}

		Map<String, Object> mapRet = new HashMap<String, Object>();
		Map<String, Object> mapUserStatus = daoUtil.getUserStatusForApp(username);
		mapRet.put("alert_mode", mapUserStatus.get("alert_mode"));
		mapRet.put("is_email_verify", mapUserStatus.get("is_email_verify"));

		// TODO:ZSH 登陆获取远程数据
		JSONArray obj = null;
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "getDeviceInfoCN");
		param.put("username", username);// 手机号码

		try {
			// JSONObject json = doPost_urlconn(requestPath, param);
			RPCResult rpcResult = regConsumerService.hanleRegService("getDeviceInfoCN", param);
			if (null != rpcResult.getRpcResult()) {
				obj = new JSONArray();
				JSONObject json = JSON.parseObject(rpcResult.getRpcResult().toString());
				JSONArray objArray = json.getJSONArray("data");
				for (Object curobj : objArray) {
					if (curobj != null) {
						JSONObject curJsonObj = (JSONObject) curobj;
						String product_type = curJsonObj.get("product_type") != null
								? curJsonObj.get("product_type").toString() : "";
						if (curJsonObj.containsKey("nickname") && curJsonObj.get("nickname") != null) {
							curJsonObj.put("nickname",
									URLDecoder.decode(curJsonObj.get("nickname").toString(), "UTF-8"));
						} else {
							// 添加设备时给设备设置默认名称
							if (productInfoList != null && productInfoList.size() > 0) {
								for (Map<String, Object> product : productInfoList) {
									String typeValue = product.get("value") != null ? product.get("value").toString()
											: "";
									String typeName = product.get("name") != null ? product.get("name").toString() : "";
									if (product_type.equals(typeValue)) {
										curJsonObj.put("nickname", typeName);
										break;
									}
								}
							}
						}
						obj.add(curJsonObj);
					}

				}
			}
			if (obj == null) {
				obj = new JSONArray();
			}
		} catch (Exception e) {
		}

		if (daoUtil.getdeviceprtocoltype(deviceSn) == 5 || daoUtil.getdeviceprtocoltype(deviceSn) == 6
				|| daoUtil.getdeviceprtocoltype(deviceSn) == 7 || daoUtil.getdeviceprtocoltype(deviceSn) == 8) { // 770设备

			System.out.println("************************Group********************");
			// 创建群组方法（创建群组，并将用户加入该群组，用户将可以收到该群的消息，同一用户最多可加入 500 个群，每个群最大至 3000
			// 人，App 内的群组数量没有限制.注：其实本方法是加入群组方法 /group/join 的别名。）
			String[] groupCreateUserId = { deviceSn, username };
			CodeSuccessResult groupCreateResult = RongCloud.getInstance(appKey, appSecret).group
					.create(groupCreateUserId, deviceSn, deviceSn);
			System.out.println("create:  " + groupCreateResult.toString());

			if (groupCreateResult.getCode() == 200) {

				param = new HashMap<String, String>();
				param.put("function", "addGroupUser");
				param.put("names", username + "," + deviceSn);
				param.put("deviceSn", deviceSn);

				regConsumerService.hanleRegService("addGroupUser", param);
			} else {
				mapResponse.put(ReturnObject.RETURN_CODE, "550");
				mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.creat_group_fail"));
			}

		}

		mapRet.put("device_list", obj);
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "forlogin.binding_tracker_success"));
		return mapResponse;

	}

	/**
	 * 忘记密码,找回密码(针对当前用户，无需处理)
	 */
	public Map<String, Object> forgetPassword(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String username = DataUtil.getStringFromMap(params, "username");
		String language = headers.get("accept-language");
		Map<String, Object> mapResponse = new HashMap<>();
		// 用户不存在
		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 发送找回密码邮件
		String pwd_verify_code = RandomUtil.getRandomString(10);
		if (!emailUtil.sendForgetPasswordEmail(username, pwd_verify_code, language)
				|| !daoUtil.updateUserForFindPassWord(username, 0, pwd_verify_code)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "email.send_email_failed_sys"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "email.note_check_email"));
		return mapResponse;

	}

	/**
	 * 实时点名位置（原来的获取追后的GPS数据）(添加访问远程逻辑)
	 */
	public Map<String, Object> getCurrentGPS(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String endTime = DataUtil.getStringFromMap(params, "endTime");
		// return getCurrentGPS(deviceSn, endTime,
		// params);

		Map<String, Object> mapResponse = new HashMap<>();

		// session中的超级用户

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (remoteServerRequest != "" && remoteServerRequest != null) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getCurrentGPS");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("endTime", endTime);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程getCurrentGPS异常：" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}

		if (!daoUtil.hasOperatingAuthority(trackerID, userID)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}

		int userTimezone = daoUtil.getUserTimezone(userID);

		int step = 0;// 今日步数 （米）
		int calorie = 0;// 卡路里

		Map<String, Object> gps = null;
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);

		gps = daoUtil.getLastGPS(deviceSn, timezone);

		// 通知网关进行点名操作，网关PUSH数据到APP
		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			gatewayHttpHandler.deviceNaming(deviceSn);
		} else {
			gatewayHandler.set1005(deviceSn);
		}

		if (daoUtil.getdeviceprtocoltype(deviceSn) == 0) {
			// 下发紧急联系人到终端
			Map<String, Object> devicemap = daoUtil.getDevice(trackerID);
			if (devicemap != null && !devicemap.isEmpty()) {
				gatewayHandler.set101E(deviceSn,
						devicemap.get("mobile1") == null ? "" : devicemap.get("mobile1").toString(),
						devicemap.get("mobile2") == null ? "" : devicemap.get("mobile2").toString(),
						devicemap.get("mobile3") == null ? "" : devicemap.get("mobile3").toString());
			}
		}

		int totalMileage = 0;// 总里程 (公里)
		int mileage = 0;// 今日里程 （米）

		Map<String, Object> mapDevice = daoUtil.getDeviceType(deviceSn);
		// 获取当前设备的身高体重等信息如果是人的话

		// 人类显示：总里程、今日里程、今日步数、今日卡路里
		// 里程数=∑（每个gps点的）速度/60*定位频率
		// 步数＝里程/步长 （ 步长＝0.5m）
		// 卡路里消耗＝体重（成人平均体重50KG）*系数（散步时系数为4）*时间（小时，有gps数据的时间）
		// 体重和步长，用户有填写的就用用户填写的，没有填写的就用平均值。
		// 宠物、汽车、摩托车只显示总里程、今日里程
		/*
		 * float human_step = (float) 0.5;// 步长 float human_weight = (float)
		 * 50;// 体重 float gps_interval = 60;// 频率
		 */
		// OBD查询今日里程和总里程
		if (mapDevice.get("ranges").toString().equals("6")) {
			Date endDate = new Date();// 结束时间
			Date startDate = endDate;
			// 开始时间
			startDate.setHours(00);
			startDate.setMinutes(00);
			startDate.setSeconds(00);

			String date1 = UtilDate.getUTCDateString(startDate, userTimezone);
			String date2 = UtilDate.getUTCDateString(endDate, userTimezone);

			float sumMileage = daoUtil.getSumMileage(deviceSn, null, null);
			if (sumMileage > 0) {
				totalMileage = (int) (sumMileage / 1000);
			}
			// OBD取今日里程
			float m = daoUtil.getSumMileage(deviceSn, date1, date2);
			if (m > 0) {// 算今日里程
				mileage = (int) (m / 1000);
			}
		}

		if (null != gps) {
			gps.put("totalMileage", totalMileage);
			gps.put("mileage", mileage);
			gps.put("step", gps.get("steps"));
			// gps.put("calorie", calorie);

			// 根据在线状态获取是否休眠唤醒 begin
			Map<String, Object> sleepInfoMap = daoUtil.getTsleepinfo(deviceSn);
			if (sleepInfoMap != null) {
				Integer onlinestatus = Integer.parseInt(gps.get("onlinestatus").toString());

				String enable = sleepInfoMap.get("enable").toString();
				String boottime = sleepInfoMap.get("boottime").toString();
				String shutdowntime = sleepInfoMap.get("shutdowntime").toString();

				// 根据请求时间 +时区比较
				Date date = new Date();
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String currentTimeUTC = sdf.format(date);

				String currentTimeLocal = null;
				if (Integer.parseInt(enable) == 1) { // 启用
					// UTC 转成本地
					try {
						currentTimeLocal = DateTimeUtil.utc2Local(currentTimeUTC, userTimezone);
						if (onlinestatus == 0) {
							if (DateTimeUtil.comPareTime(currentTimeLocal, boottime) >= 0 // 請求時間在設置時間段裡面
									&& DateTimeUtil.comPareTime(currentTimeLocal, shutdowntime) <= 0) {
								// 不在线 休眠
								gps.put("onlinestatus", 4);
							} else { // 设备在线 唤醒
								if (DateTimeUtil.comPareTime(currentTimeLocal, boottime) >= 0 // 請求時間在設置時間段裡面
										&& DateTimeUtil.comPareTime(currentTimeLocal, shutdowntime) <= 0) {
									gps.put("onlinestatus", 3);
								}

							}
						}
					} catch (ParseException e) {
						e.printStackTrace();
					}
				}
			}
			// 根据在线状态获取是否休眠唤醒 end
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, gps);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;

	}

	/**
	 * 获取最后的GPS数据(添加访问远程逻辑)
	 */
	public Map<String, Object> getLasterGPS(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String endTime = DataUtil.getStringFromMap(params, "endTime");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		long functionStartTime = System.currentTimeMillis(); // 获取开始时间

		Map<String, Object> mapResponse = new HashMap<>();

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getLasterGPS");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("endTime", endTime);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程getCurrentGPS异常：" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}

		if (!daoUtil.hasOperatingAuthority(trackerID, userID)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}

		int step = 0;// 今日步数 （米）
		int calorie = 0;// 卡路里

		Map<String, Object> gps = null;
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);

		gps = daoUtil.getLastGPS(deviceSn, timezone);

		// 通知网关进行点名操作，网关PUSH数据到APP
		// gatewayHandler.set1005(deviceSn);

		if (daoUtil.getdeviceprtocoltype(deviceSn) == 0) {
			// 下发紧急联系人到终端
			Map<String, Object> devicemap = daoUtil.getDevice(trackerID);
			if (devicemap != null && !devicemap.isEmpty()) {
				gatewayHandler.set101E(deviceSn,
						devicemap.get("mobile1") == null ? "" : devicemap.get("mobile1").toString(),
						devicemap.get("mobile2") == null ? "" : devicemap.get("mobile2").toString(),
						devicemap.get("mobile3") == null ? "" : devicemap.get("mobile3").toString());
			}
		}

		int totalMileage = 0;// 总里程 (公里)
		int mileage = 0;// 今日里程 （米）

		Map<String, Object> mapDevice = daoUtil.getDeviceType(deviceSn);

		// 人类显示：总里程、今日里程、今日步数、今日卡路里
		// 里程数=∑（每个gps点的）速度/60*定位频率
		// 步数＝里程/步长 （ 步长＝0.5m）
		// 卡路里消耗＝体重（成人平均体重50KG）*系数（散步时系数为4）*时间（小时，有gps数据的时间）
		// 体重和步长，用户有填写的就用用户填写的，没有填写的就用平均值。

		// OBD 算今日里程和总里程
		if (mapDevice.get("ranges").toString().equals("6")) {

			Date endDate = UtilDate.getDate(endTime, UtilDate.simple);// 结束时间
			Date startDate = UtilDate.getDate(endTime, UtilDate.simple);
			// 开始时间
			startDate.setHours(00);
			startDate.setMinutes(00);
			startDate.setSeconds(00);

			String date1 = UtilDate.getUTCDateString(startDate, timezone);
			String date2 = UtilDate.getUTCDateString(endDate, timezone);

			float sumMileage = daoUtil.getSumMileage(deviceSn, null, null);
			if (sumMileage > 0) {
				totalMileage = (int) (sumMileage / 1000);
			}
			// OBD取今日里程
			float m = daoUtil.getSumMileage(deviceSn, date1, date2);
			if (m > 0) {// 算今日里程
				mileage = (int) (m / 1000);
			}
		}

		gps.put("totalMileage", totalMileage);
		gps.put("mileage", mileage);
		gps.put("step", gps.get("steps"));
		// gps.put("calorie", calorie);

		// 根据在线状态获取是否休眠唤醒 begin
		Map<String, Object> sleepInfoMap = daoUtil.getTsleepinfo(deviceSn);
		if (sleepInfoMap != null) {
			Integer onlinestatus = Integer.parseInt(gps.get("onlinestatus").toString());

			String enable = sleepInfoMap.get("enable").toString();
			String boottime = sleepInfoMap.get("boottime").toString();
			String shutdowntime = sleepInfoMap.get("shutdowntime").toString();

			// 根据请求时间 +时区比较
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String currentTimeUTC = sdf.format(date);

			String currentTimeLocal = null;
			if (Integer.parseInt(enable) == 1) { // 启用
				// UTC 转成本地
				try {
					currentTimeLocal = DateTimeUtil.utc2Local(currentTimeUTC, timezone);
					if (onlinestatus == 0) {
						if (DateTimeUtil.comPareTime(currentTimeLocal, boottime) >= 0 // 請求時間在設置時間段裡面
								&& DateTimeUtil.comPareTime(currentTimeLocal, shutdowntime) <= 0) {
							// 不在线 休眠
							gps.put("onlinestatus", 4);
						} else { // 设备在线 唤醒
							if (DateTimeUtil.comPareTime(currentTimeLocal, boottime) >= 0 // 請求時間在設置時間段裡面
									&& DateTimeUtil.comPareTime(currentTimeLocal, shutdowntime) <= 0) {
								gps.put("onlinestatus", 3);
							}

						}
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}
		// 根据在线状态获取是否休眠唤醒 end

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, gps);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);

		long functionEndTime = System.currentTimeMillis(); // 获取结束时间
		LogManager.info("functionName： getLasterGPS_____deviceSn:" + deviceSn + "_____functionTime:"
				+ (functionEndTime - functionStartTime) + "ms____row:1136");

		return mapResponse;

	}

	/**
	 * 设备在线状态(添加访问远程逻辑)
	 */
	public Map<String, Object> getOnlineStatus(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		Map<String, Object> mapResponse = new HashMap<>();

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getOnlineStatus");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getOnlineStatus异常：" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}

		// 去拿GPS
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);
		Map<String, Object> gps = daoUtil.getLastGPS(deviceSn, timezone);

		Map<String, Object> sleepInfoMap = daoUtil.getTsleepinfo(deviceSn);
		if (sleepInfoMap != null) {
			Integer onlinestatus = Integer.parseInt(gps.get("onlinestatus").toString());

			String enable = sleepInfoMap.get("enable").toString();
			String boottime = sleepInfoMap.get("boottime").toString();
			String shutdowntime = sleepInfoMap.get("shutdowntime").toString();

			// 根据请求时间 +时区比较
			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			String currentTimeUTC = sdf.format(date);

			String currentTimeLocal = null;
			if (Integer.parseInt(enable) == 1) { // 启用
				// UTC 转成本地
				try {
					currentTimeLocal = DateTimeUtil.utc2Local(currentTimeUTC, timezone);
					if (onlinestatus == 0) {
						if (DateTimeUtil.comPareTime(currentTimeLocal, boottime) >= 0 // 請求時間在設置時間段裡面
								&& DateTimeUtil.comPareTime(currentTimeLocal, shutdowntime) <= 0) {
							// 不在线 休眠
							gps.put("onlinestatus", 4);
						} else { // 设备在线 唤醒
							if (DateTimeUtil.comPareTime(currentTimeLocal, boottime) >= 0 // 請求時間在設置時間段裡面
									&& DateTimeUtil.comPareTime(currentTimeLocal, shutdowntime) <= 0) {
								gps.put("onlinestatus", 3);
							}

						}
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, gps);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;

	}

	/**
	 * 获取设备GPS数据(添加访问远程逻辑)
	 * 
	 * @return
	 */
	public Map<String, Object> getHistoricalGPSData(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String startTime = DataUtil.getStringFromMap(params, "startTime");
		String endTime = DataUtil.getStringFromMap(params, "endTime");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		Map<String, Object> mapResponse = new HashMap<>();

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getHistoricalGPSData");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("startTime", startTime);
					param.put("endTime", endTime);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getHistoricalGPSData异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 未绑定此设备
		if (!daoUtil.hasOperatingAuthority(deviceID, userId)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}
		// 获取GPS数据
		List<Map<String, Object>> gpsData = daoUtil.getHistoricalGPSData(deviceID, startTime, endTime);
		// 解决手表凌晨 步数和卡路里不清零的问题
		List<Map<String, Object>> gpsData2 = daoUtil.getHistoricalGPSDataOrderByTime(deviceID, startTime, endTime);
		int steps = 0;// 当前时间段步数 （米）
		double calorie = 0;// 卡路里
		if (gpsData != null && gpsData.size() > 0 && gpsData2 != null && gpsData2.size() > 0) {
			Map<String, Object> startStepData = gpsData2.get(0);
			Map<String, Object> endStepData = gpsData.get(gpsData.size() - 1);
			int startStep = startStepData.get("steps") != null ? Integer.parseInt(startStepData.get("steps").toString())
					: 0;
			int endStep = endStepData.get("steps") != null ? Integer.parseInt(endStepData.get("steps").toString()) : 0;
			double startCalorie = startStepData.get("calorie") != null
					? Double.parseDouble(startStepData.get("calorie").toString()) : 0;
			double endCalorie = endStepData.get("calorie") != null
					? Double.parseDouble(endStepData.get("calorie").toString()) : 0;

			steps = endStep - startStep;

			calorie = endCalorie - startCalorie;
			if (steps < 0) {
				steps = 0;
			}
			if (calorie < 0) {
				calorie = 0;
			}
		}

		int mileage = 0;// 当前时间段里程 （米）
		Map<String, Object> GPSInterval = daoUtil.getGPSInterval(deviceSn);

		// 获取当前设备的身高体重等信息如果是人的话
		Date endDate = UtilDate.getDate(endTime, UtilDate.nyrsf);// 结束时间
		Date startDate = UtilDate.getDate(startTime, UtilDate.nyrsf);
		int timezone = daoUtil.getUserTimezone(userId);
		String date1 = UtilDate.getUTCDateString(startDate, timezone);
		String date2 = UtilDate.getUTCDateString(endDate, timezone);

		// 计算时长
		Calendar dateStart = Calendar.getInstance();
		Calendar dateEnd = Calendar.getInstance();
		dateStart.setTime(startDate); // 设置为当前系统时间
		dateEnd.setTime(endDate);
		long timeOne = dateStart.getTimeInMillis();
		long timeTwo = dateEnd.getTimeInMillis();
		long timeLong = (timeTwo - timeOne) / (1000 * 60);// 转化minute

		// 人类显示：总里程、今日里程、今日步数、今日卡路里
		// 里程数=∑（每个gps点的）速度/60*定位频率
		// 步数＝里程/步长 （ 步长＝0.5m）
		// 卡路里消耗＝体重（成人平均体重50KG）*系数（散步时系数为4）*时间（小时，有gps数据的时间）
		// 体重和步长，用户有填写的就用用户填写的，没有填写的就用平均值。
		// 宠物、汽车、摩托车只显示总里程、今日里程
		float gps_interval = 60;// 频率

		if (GPSInterval != null && GPSInterval.containsKey("gps_interval") && GPSInterval.get("gps_interval") != null) {
			gps_interval = Float.parseFloat(GPSInterval.get("gps_interval").toString());// 定位频率（秒）
		}
		List<Map<String, Object>> list = daoUtil.getGPS(deviceID, date1, date2);
		// add by ljl 20160517存储上一位置的经纬度值
		float preLat = 0;
		float preLng = 0;
		if (null != list && list.size() > 0) {
			for (Map<String, Object> map : list) {
				float speed = map.get("speed") != null ? Float.parseFloat(map.get("speed").toString()) : 0;// 速度
																											// speed数据库
				float curLat = map.get("lat") != null ? Float.parseFloat(map.get("lat").toString()) : 0;// 纬度
																										// lat数据库
				float curLng = map.get("lng") != null ? Float.parseFloat(map.get("lng").toString()) : 0;// 经度
																										// lng数据库
				if (gps_interval > 0 && speed > 0) {// 速度/60*定位频率
					mileage += (speed / 3.6) * gps_interval;// ∑（每个gps点的）速度*定位频率
															// speed/3.6公里每小时转换成米每小时
				} else if ((gps_interval > 0 && speed <= 0) && (preLat > 0 || preLng > 0)) {
					// add by ljl 20160517 如果速度为零
					double distance = DistanceUtil.getDistance(preLat, preLng, curLat, curLng);
					mileage += distance;
				}
				preLat = curLat;
				preLng = curLng;
			}
		}

		// ############################################################################
		if (gpsData == null || gpsData.size() == 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "tracker.no_gps_data"));
			return mapResponse;
		}

		Map<String, Object> mapRet = new HashMap<>();

		if (daoUtil.getdeviceprtocoltype(deviceSn) == 2) {
			Map<String, Object> mapScore = getSafeDriveData(params, headers, reqBody);
			Map<String, Object> mapScoreData = (Map<String, Object>) mapScore.get("ret");
			Map<String, Object> core = (Map<String, Object>) mapScoreData.get("safeDriveData");
			mapRet.put("safescore", core.get("safescore"));
			mapRet.put("economicscore", core.get("economicscore"));

		}

		mapRet.put("deviceSn", deviceSn);
		mapRet.put("mileage", mileage);
		mapRet.put("steps", steps);
		mapRet.put("timeLong", timeLong);
		mapRet.put("calorie", calorie);
		mapRet.put("gps", gpsData);
		mapResponse.put("code", "0");
		mapResponse.put("ret", mapRet);
		mapResponse.put("what", null);
		return mapResponse;

	}

	/**
	 * 设置提醒(针对当前用户，无需处理)
	 */
	public Map<String, Object> setRemind(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String remindID = DataUtil.getStringFromMap(params, "remindID");
		String title = DataUtil.getStringFromMap(params, "title");
		String weekly = DataUtil.getStringFromMap(params, "weekly");
		String monthly = DataUtil.getStringFromMap(params, "monthly");
		String yearly = DataUtil.getStringFromMap(params, "yearly");
		String monday = DataUtil.getStringFromMap(params, "monday");
		String tuesday = DataUtil.getStringFromMap(params, "tuesday");
		String wednesday = DataUtil.getStringFromMap(params, "wednesday");
		String thursday = DataUtil.getStringFromMap(params, "thursday");
		String friday = DataUtil.getStringFromMap(params, "friday");
		String saturday = DataUtil.getStringFromMap(params, "saturday");
		String sunday = DataUtil.getStringFromMap(params, "sunday");
		String specificYear = DataUtil.getStringFromMap(params, "specificYear");
		String specificMonth = DataUtil.getStringFromMap(params, "specificMonth");
		String specificDay = DataUtil.getStringFromMap(params, "specificDay");
		String isEnd = DataUtil.getStringFromMap(params, "isEnd");
		String diabolo = DataUtil.getStringFromMap(params, "diabolo");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		Map<String, Object> mapResponse = new HashMap<>();
		// 查找用户名失败
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		boolean result = false;
		// 新增提醒
		if (remindID.equals("-1")) {
			result = addRemind(username, title, weekly, monthly, yearly, monday, tuesday, wednesday, thursday, friday,
					saturday, sunday, specificYear, specificMonth, specificDay, isEnd, diabolo);
		} else { // 修改提醒
			result = modifyRemind(remindID, title, weekly, monthly, yearly, monday, tuesday, wednesday, thursday,
					friday, saturday, sunday, specificYear, specificMonth, specificDay, isEnd, diabolo);
		}
		if (!result) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "saveclock.failed"));
			return mapResponse;
		} else {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, getRemind(params, headers, reqBody));
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "saveclock.success"));
			return mapResponse;
		}

	}

	/**
	 * 获取提醒(针对当前用户，无需处理)
	 * 
	 * @return
	 */
	public Map<String, Object> getRemind(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		Map<String, Object> mapResponse = new HashMap<>();
		// 查找用户名失败
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		int uid = daoUtil.getUserId(username);
		Map<String, Object> reminds = getRemind(uid);
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, reminds);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;

	}

	/**
	 * 删除提醒(针对当前用户，无需处理)
	 * 
	 * @param remindID
	 * @return
	 */
	public Map<String, Object> deleteRemind(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String remindID = DataUtil.getStringFromMap(params, "remindID");
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		// 查找用户名失败
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		if (!(daoUtil.deleteRemind(Integer.parseInt(remindID))
				&& daoUtil.deleteRemindTime(Integer.parseInt(remindID)))) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "deleteclock.failed"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "deleteclock.success"));
		return mapResponse;
	}

	/**
	 * 设置电子围栏（添加限定权限）
	 */
	public Map<String, Object> setGeoFence(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String lat = DataUtil.getStringFromMap(params, "lat");
		String lng = DataUtil.getStringFromMap(params, "lng");
		String radius = DataUtil.getStringFromMap(params, "radius");
		String areaid = DataUtil.getStringFromMap(params, "areaid");
		String defencename = DataUtil.getStringFromMap(params, "defencename");
		String defencestatus = DataUtil.getStringFromMap(params, "defencestatus");
		String isOut = DataUtil.getStringFromMap(params, "isOut");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		// 查找用户名失败
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		// 判断此设备是否可以进行围栏设置
		int deviceID = daoUtil.getDeviceID(deviceSn);
		int deviceGeoFenceAmount = daoUtil.getDeviceGeoFenceAmount(deviceID);
		if (deviceGeoFenceAmount >= maxGeoFenceAmount && TextUtils.isEmpty(areaid)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "fence.reach_maxnum"));
			return mapResponse;
		}

		CastelMessage responseMsg = null;
		if (true) {
			String type = "1";
			responseMsg = gatewayHttpHandler.setEnclosure(deviceSn, lat, lng, radius, areaid, type, isOut,
					defencestatus);
		}

		if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
			int userID = daoUtil.getUserId(username);

			boolean result = TextUtils.isEmpty(areaid)
					? daoUtil.setGeoFenceMulti(String.valueOf(deviceID), String.valueOf(userID), lat, lng, radius,
							defencename, defencestatus, isOut)
					: daoUtil.updateGeoFenceMulti(areaid, String.valueOf(deviceID), String.valueOf(userID), lat, lng,
							radius, defencename, defencestatus, isOut);
			if (result) {
				mapResponse.put(ReturnObject.RETURN_CODE, "0");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "fence.set_fence_success"));
				return mapResponse;
			}
		} else {
			mapResponse.put(ReturnObject.RETURN_CODE, "600");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "400");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "fence.save_fence_failed"));
		return mapResponse;

	}

	/**
	 * 检查更新（添加限定权限）(添加访问远程逻辑)
	 * 
	 * @param phoneOS
	 * @param deviceSn
	 * @return
	 */
	public Map<String, Object> checkForUpdate(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String customizedApp = DataUtil.getStringFromMap(params, "isCustomizedApp");

		String phoneOS = DataUtil.getStringFromMap(params, "phoneOS");
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String language = headers.get("accept-language");
		// 默认为国内版
		if (customizedApp == null || customizedApp.equals("") || customizedApp.isEmpty()) {
			customizedApp = "0_0";
		}

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (remoteServerRequest != "" && remoteServerRequest != null) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		if (null != deviceSn && !deviceSn.isEmpty()) {// 设备号为空

			try {
				Object obj = getdeviceserverandusername(deviceSn);
				if (obj == null) {
					mapResponse.put(ReturnObject.RETURN_CODE, "300");
					mapResponse.put(ReturnObject.RETURN_OBJECT, null);
					mapResponse.put(ReturnObject.RETURN_WHAT,
							LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
					return mapResponse;
				} else {
					String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
					String localServerIpAddress = env.getProperty("localServerIpAddress");

					if (!localServerIpAddress.equals(deviceServerIpAddress)) {
						String userName = ((JSONObject) obj).getString("name");

						Map<String, String> param = new HashMap<String, String>();
						param.put("function", "checkForUpdate");
						param.put("remoteServerRequest", "true");
						param.put("AcceptLanguage", language);
						param.put("username", userName);
						param.put("isCustomizedApp", customizedApp);
						param.put("phoneOS", phoneOS);
						param.put("deviceSn", deviceSn);

						String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
								+ EnumUtils.Version.THREE;
						JSONObject json = doPost_urlconn(requestPath, param);

						mapResponse.put("remotingReturn", true);
						mapResponse.put("result", json.toJSONString());

						return mapResponse;
					}
				}
			} catch (Exception e) {
				LogManager.error("远程访问getGeoFence异常" + e.getMessage());

				mapResponse.put(ReturnObject.RETURN_CODE, "1100");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
				return mapResponse;
			}
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 手机操作系统
		String phoneOs = "";
		// 最新的APP版本
		String phoneVersion = "";
		// 最新的APP版本链接
		String phoneUrl = "";

		String isForceUpdate = "";
		String description = "";
		String phoneUrlAPK = "";

		if (null != deviceSn && !deviceSn.isEmpty()) {
			Map<String, Object> mapDevice = daoUtil.getDeviceType(deviceSn);
			int ranges = Integer.parseInt(String.valueOf(mapDevice.get("ranges")));
			if (5 == ranges) {
				// ***获取手机APP版本******************************************************/
				Map<String, Object> mapPhoneVersion = daoUtil.getPhoneVersion(phoneOS, customizedApp);

				if (mapPhoneVersion != null) {
					phoneOs = mapPhoneVersion.get("os") != null ? mapPhoneVersion.get("os").toString() : "";
					phoneVersion = mapPhoneVersion.get("version") != null ? mapPhoneVersion.get("version").toString()
							: "";
					phoneUrl = mapPhoneVersion.get("url") != null ? mapPhoneVersion.get("url").toString() : "";

					isForceUpdate = mapPhoneVersion.get("is_force_update") != null
							? mapPhoneVersion.get("is_force_update").toString() : "";
					description = mapPhoneVersion.get("description") != null
							? mapPhoneVersion.get("description").toString() : "";
					phoneUrlAPK = mapPhoneVersion.get("url_apk") != null ? mapPhoneVersion.get("url_apk").toString()
							: "";
				}

				Map<String, Object> mapRet = new HashMap<String, Object>();
				mapRet.put("appOS", phoneOs);
				mapRet.put("appVersion", phoneVersion);
				mapRet.put("appUrl", phoneUrl);
				mapRet.put("isForceUpdate", isForceUpdate);
				mapRet.put("description", description);
				mapRet.put("phoneUrlAPK", phoneUrlAPK);
				mapRet.put("currentFirmwareVersion", "");// 当前固件版本号
				mapRet.put("lastFirmwareVersion", "");// 最新的固件版本号
				mapRet.put("firmwareUrl", "");

				mapResponse.put(ReturnObject.RETURN_CODE, "0");
				mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
				mapResponse.put(ReturnObject.RETURN_WHAT, null);
				return mapResponse;
			} else {
				// 设备型号
				int productType = Integer.parseInt(String.valueOf(mapDevice.get("product_type")));
				// 设备的firmware版本
				String deviceFirmware = "";
				if (mapDevice.get("firmware") != null) {
					deviceFirmware = mapDevice.get("firmware").toString();
				}
				String lastFirmware = "";
				String lastFirmwareUrl = "";

				Map<String, Object> mapFirmware = daoUtil.getLastFirmware(String.valueOf(productType));
				if (mapFirmware != null && !mapFirmware.get("version_name").toString().isEmpty()) {
					// ***获取终端固件版本信息******************************************************/
					// 最新的firmware版本
					lastFirmware = mapFirmware.get("version_name") != null ? mapFirmware.get("version_name").toString()
							: "";
					// 最新的firmware版本链接
					lastFirmwareUrl = mapFirmware.get("file") != null ? mapFirmware.get("file").toString() : "";
				} else {
					// 最新的firmware版本
					if (mapDevice.get("firmware") != null) {
						lastFirmware = mapDevice.get("firmware").toString();
					}
					lastFirmwareUrl = "";
				}
				// ***获取手机APP版本******************************************************/
				Map<String, Object> mapPhoneVersion = daoUtil.getPhoneVersion(phoneOS, customizedApp);

				if (mapPhoneVersion != null) {
					phoneOs = mapPhoneVersion.get("os") != null ? mapPhoneVersion.get("os").toString() : "";
					phoneVersion = mapPhoneVersion.get("version") != null ? mapPhoneVersion.get("version").toString()
							: "";
					phoneUrl = mapPhoneVersion.get("url") != null ? mapPhoneVersion.get("url").toString() : "";

					isForceUpdate = mapPhoneVersion.get("is_force_update") != null
							? mapPhoneVersion.get("is_force_update").toString() : "";
					description = mapPhoneVersion.get("description") != null
							? mapPhoneVersion.get("description").toString() : "";
					phoneUrlAPK = mapPhoneVersion.get("url_apk") != null ? mapPhoneVersion.get("url_apk").toString()
							: "";

				}

				Map<String, Object> mapRet = new HashMap<String, Object>();
				mapRet.put("appOS", phoneOs);
				mapRet.put("appVersion", phoneVersion);
				mapRet.put("appUrl", phoneUrl);
				mapRet.put("isForceUpdate", isForceUpdate);
				mapRet.put("description", description);
				mapRet.put("phoneUrlAPK", phoneUrlAPK);
				mapRet.put("currentFirmwareVersion", deviceFirmware);// 当前固件版本号
				mapRet.put("lastFirmwareVersion", lastFirmware);// 最新的固件版本号
				mapRet.put("firmwareUrl", lastFirmwareUrl);

				mapResponse.put(ReturnObject.RETURN_CODE, "0");
				mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
				mapResponse.put(ReturnObject.RETURN_WHAT, null);

				return mapResponse;
			}

		} else {// 如果deviceSn 为空
			Map<String, Object> mapPhoneVersion = daoUtil.getPhoneVersion(phoneOS, customizedApp);

			if (null != mapPhoneVersion) {
				phoneOs = mapPhoneVersion.get("os") != null ? mapPhoneVersion.get("os").toString() : "";
				phoneVersion = mapPhoneVersion.get("version") != null ? mapPhoneVersion.get("version").toString() : "";
				phoneUrl = mapPhoneVersion.get("url") != null ? mapPhoneVersion.get("url").toString() : "";
				isForceUpdate = mapPhoneVersion.get("is_force_update") != null
						? mapPhoneVersion.get("is_force_update").toString() : "";
				description = mapPhoneVersion.get("description") != null ? mapPhoneVersion.get("description").toString()
						: "";
				phoneUrlAPK = mapPhoneVersion.get("url_apk") != null ? mapPhoneVersion.get("url_apk").toString() : "";

				mapResponse.put(ReturnObject.RETURN_WHAT, null);
			} else {
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.app.softwareupgrad_status"));
			}
			Map<String, Object> mapRet = new HashMap<String, Object>();
			mapRet.put("appOS", phoneOs);
			mapRet.put("appVersion", phoneVersion);
			mapRet.put("appUrl", phoneUrl);
			mapRet.put("isForceUpdate", isForceUpdate);
			mapRet.put("description", description);
			mapRet.put("phoneUrlAPK", phoneUrlAPK);
			mapRet.put("currentFirmwareVersion", "");// 当前固件版本号
			mapRet.put("lastFirmwareVersion", "");// 最新的固件版本号
			mapRet.put("firmwareUrl", "");

			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);

			return mapResponse;
		}

	}

	/**
	 * 通知固件升级（添加限定权限）
	 */
	public Map<String, Object> upgradDeviceSoftware(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String targetVersion = DataUtil.getStringFromMap(params, "targetVersion");
		// 查找用户名
		String username = Session.getUser(sessionID);

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		// 通知网关
		CastelMessage responseMsg = gatewayHandler.set1009(deviceSn, targetVersion);
		if (responseMsg != null) {
			int result = Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString());
			String returnMsg = LanguageManager.getMsg(headers.get("accept-language"),
					"common.softwareupgrad_status_" + result);

			if (result == 0) {
				map.put("code", "0");
			} else {
				map.put("code", "100");
			}

			map.put("ret", result);
			map.put("what", returnMsg);
			return map;
		} else {
			map.put("code", "600");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return map;
		}
	}

	/**
	 * 授权(远程授权核心方法)
	 * 
	 * @param deviceSn
	 * @param username
	 * @return
	 */
	public Map<String, Object> authorizationBinding(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		if (deviceSn != null) {
			deviceSn = deviceSn.trim();
		}
		String username = DataUtil.getStringFromMap(params, "username").trim().toLowerCase();

		Map<String, Object> mapResponse = new HashMap<>();

		String nickname = DataUtil.getStringFromMap(params, "nickname");
		String isGpsS = DataUtil.getStringFromMap(params, "isGps");

		int isGps = (!TextUtils.isEmpty(isGpsS)) ? Integer.parseInt(isGpsS) : 1;

		String sessionID = headers.get("sessionID");
		String superUsername = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (superUsername == null || superUsername.isEmpty()) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "forlogin.find_authuser_failed"));
			return mapResponse;
		}
		int superUserID = daoUtil.getUserId(superUsername);
		if (superUserID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "forlogin.authuser_notexist"));
			return mapResponse;
		}
		if (!daoUtil.isSuperUser(superUsername, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, MessageFormat.format(
					LanguageManager.getMsg(headers.get("accept-language"), "forlogin.user_tracker_nopermission"),
					superUsername));
			return mapResponse;
		}
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		if (!daoUtil.hasOperatingAuthority(deviceID, superUserID)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		int devicebinduser_count = 0;
		/****** 增加访问远程代码 开始 ***********/
		try {
			Object obj = getauthorizationuserinfo(deviceSn, username);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "600");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT, MessageFormat.format(
						LanguageManager.getMsg(headers.get("accept-language"), "forlogin.invalid_user"), username));
				return mapResponse;
			} else {
				JSONObject jsonObj = (JSONObject) obj;
				String userServerIpAddress = jsonObj.getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				devicebinduser_count = jsonObj.getIntValue("devicebinduser_count");

				if (!localServerIpAddress.equals(userServerIpAddress)) {
					int userbinddevice_count = jsonObj.getIntValue("userbinddevice_count");
					int userhavebinddevice_count = jsonObj.getIntValue("userhavebinddevice_count");

					int maxBinding = Integer.valueOf(env.getProperty("max.binding"));
					if (userbinddevice_count >= maxBinding) {
						mapResponse.put(ReturnObject.RETURN_CODE, "650");
						mapResponse.put(ReturnObject.RETURN_OBJECT, null);
						mapResponse.put(ReturnObject.RETURN_WHAT,
								MessageFormat.format(
										LanguageManager.getMsg(headers.get("accept-language"), "forlogin.bindinglimit"),
										maxBinding));
						return mapResponse;
					}

					if (devicebinduser_count > 3) {
						mapResponse.put(ReturnObject.RETURN_CODE, "700");
						mapResponse.put(ReturnObject.RETURN_OBJECT, null);
						mapResponse.put(ReturnObject.RETURN_WHAT,
								LanguageManager.getMsg(headers.get("accept-language"), "forlogin.binding_three"));
						return mapResponse;
					}

					if (userhavebinddevice_count > 0) {
						mapResponse.put(ReturnObject.RETURN_CODE, "900");
						mapResponse.put(ReturnObject.RETURN_OBJECT, null);
						mapResponse.put(ReturnObject.RETURN_WHAT,
								LanguageManager.getMsg(headers.get("accept-language"), "forlogin.has_binding"));
						return mapResponse;
					}

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "authorization");
					param.put("username", username);
					param.put("deviceSn", deviceSn);
					param.put("nickname", nickname);// 账号昵称
					param.put("isGps", String.valueOf(isGps));// 授权用户是否开启定位设置：0:开启
																// 1:不开启

					/****** 增加访问远程接口代码 ***********/
					if (userServerIpAddress.equals(env.getProperty("remoteServerIpAddress"))) {
						String requestPath = env.getProperty("remotingServer_Reg");
						param.put("remoteServerRequest", "true");
						JSONObject json = doPost_urlconn(requestPath, param);
						if (json == null || !(json.get("code").equals("0"))) {
							mapResponse.put(ReturnObject.RETURN_CODE, "1500");
							mapResponse.put(ReturnObject.RETURN_OBJECT, null);
							mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager
									.getMsg(headers.get("accept-language"), "forlogin.binding_tracker_failed"));
							return mapResponse;
						}
					}
					/****** 增加访问远程接口代码 ***********/

					RPCResult rpcResult = regConsumerService.hanleRegService("authorization", param);

					if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS && JSON
							.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
						JSONArray objlist = null;
						if (null != rpcResult.getRpcResult()) {
							JSONObject json = JSON.parseObject(rpcResult.getRpcResult().toString());
							objlist = json.getJSONArray("data");
						}

						if (objlist == null) {
							objlist = new JSONArray();
						}

						mapResponse.put(ReturnObject.RETURN_CODE, "0");
						mapResponse.put(ReturnObject.RETURN_OBJECT, objlist);// daoUtil.getTrackerUser(deviceID));
						mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(headers.get("accept-language"),
								"forlogin.auth_binding_success"));
						return mapResponse;
					} else {
						mapResponse.put(ReturnObject.RETURN_CODE, "1000");
						mapResponse.put(ReturnObject.RETURN_OBJECT, null);
						mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(headers.get("accept-language"),
								"forlogin.binding_tracker_failed"));
						return mapResponse;
					}
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getauthorizationuserinfo异常：" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1000");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "forlogin.binding_tracker_failed"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		int bUserID = daoUtil.getUserId(username);
		if (bUserID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "600");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, MessageFormat
					.format(LanguageManager.getMsg(headers.get("accept-language"), "forlogin.invalid_user"), username));
			return mapResponse;
		}

		int maxBinding = Integer.valueOf(env.getProperty("max.binding"));
		if (daoUtil.quantityOfUserDevice(bUserID) >= maxBinding) {
			mapResponse.put(ReturnObject.RETURN_CODE, "650");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, MessageFormat.format(
					LanguageManager.getMsg(headers.get("accept-language"), "forlogin.bindinglimit"), maxBinding));
			return mapResponse;
		}

		if (devicebinduser_count > 3) {
			mapResponse.put(ReturnObject.RETURN_CODE, "700");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "forlogin.binding_three"));
			return mapResponse;
		}
		if (daoUtil.hasBinding(deviceID, bUserID) > 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "900");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "forlogin.has_binding"));
			return mapResponse;
		}

		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "authorization");
			param.put("username", username);
			param.put("deviceSn", deviceSn);
			param.put("nickname", nickname);
			param.put("isGps", String.valueOf(isGps));

			RPCResult rpcResult = regConsumerService.hanleRegService("authorization", param);

			if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS
					&& JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
				if (daoUtil.trackerBindUser(deviceID, bUserID, 0, nickname)) {
					JSONArray objlist = null;
					if (null != rpcResult.getRpcResult()) {
						JSONObject json = JSON.parseObject(rpcResult.getRpcResult().toString());
						objlist = json.getJSONArray("data");
					}

					if (objlist == null) {
						objlist = new JSONArray();
					}

					mapResponse.put(ReturnObject.RETURN_CODE, "0");
					mapResponse.put(ReturnObject.RETURN_OBJECT, objlist);// daoUtil.getTrackerUser(deviceID));
					mapResponse.put(ReturnObject.RETURN_WHAT,
							LanguageManager.getMsg(headers.get("accept-language"), "forlogin.auth_binding_success"));
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("进行授权authorizationBinding异常：" + e.getMessage());
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "1000");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "forlogin.binding_tracker_failed"));
		return mapResponse;

	}

	/**
	 * 解绑 取消授权(远程授权核心方法)
	 * 
	 * @throws Exception
	 */
	public Map<String, Object> cancelAuthorization(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) throws Exception {

		String sessionID = headers.get("sessionID");
		String language = headers.get("accept-language");

		Map<String, Object> map = new HashMap<>();
		// 追踪器编号
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		if (deviceSn != null) {
			deviceSn = deviceSn.trim();
		}
		// 被取消授权的用户名
		String userName = DataUtil.getStringFromMap(params, "username");
		// session中的超级用户
		String superUserName = Session.getUser(sessionID);

		if (!PropertyUtil.isNotBlank(superUserName)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int superUserId = daoUtil.getUserId(superUserName);
		if (superUserId <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what",
					MessageFormat.format(
							LanguageManager.getMsg(headers.get("accept-language"), "forlogin.unbind_user_notexist"),
							superUserName));
			return map;
		}

		/* 获取远程用户信息 */
		Object userobj = getauthorizationuserinfo(deviceSn, userName);
		if (userobj == null) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", MessageFormat.format(
					LanguageManager.getMsg(headers.get("accept-language"), "forlogin.unbind_user_notexist"), userName));
			return map;
		}

		/* 获取远程设备信息 */
		Object deviceobj = getdeviceserverandusername(deviceSn);
		if (deviceobj == null) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		String userServerIpAddress = ((JSONObject) userobj).getString("conn_name");
		String deviceServerIpAddress = ((JSONObject) deviceobj).getString("conn_name");
		if (userServerIpAddress.equals(deviceServerIpAddress)) // 在同一台服务器
		{
			int trackerId = daoUtil.getDeviceID(deviceSn);
			if (trackerId <= 0) {
				map.put("code", "300");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			}

			if (daoUtil.isSuperUser(userName, deviceSn)) // 超级用户
			{
				boolean isDelete = true;
				int result = 1;// 失败
				if (daoUtil.getdeviceprtocoltype(deviceSn) == 5 || daoUtil.getdeviceprtocoltype(deviceSn) == 6
						|| daoUtil.getdeviceprtocoltype(deviceSn) == 7) { // （目前手表才有）770,790,990设备下发指令，恢复出厂设置
					// 通知网关

					// 通知网关
					Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
					String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
					// 通知网关
					CastelMessage responseMsg = null;
					if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
						responseMsg = gatewayHttpHandler.bindingDevice(deviceSn, "0");
					} else {
						responseMsg = gatewayHandler.set1008(deviceSn, 0);
					}

					result = responseMsg == null ? result
							: Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString());
					// 指令下发失败
					if (result != 0) {
						isDelete = false;
					}
				}

				if (isDelete) {// 指令成功，删除数据
					// TODO:ZSH 远程同步数据到注册服务器
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "deletedevice");
					param.put("deviceSn", "'" + deviceSn + "'");

					RPCResult rpcResult = regConsumerService.hanleRegService("deletedevice", param);
					if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS || !JSON
							.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
						map.put("code", "550");
						map.put("ret", "");
						map.put("what", LanguageManager.getMsg(headers.get("accept-language"),
								"forlogin.unbind_superuser_failed"));
						return map;
					}

					// 删除关联数据
					daoUtil.deleteotherdata(deviceSn, trackerId);

					if (daoUtil.getdeviceprtocoltype(deviceSn) == 5 || daoUtil.getdeviceprtocoltype(deviceSn) == 6
							|| daoUtil.getdeviceprtocoltype(deviceSn) == 7) { // 删除群组
						showInfoNtf(deviceSn, null,
								LanguageManager.getMsg(headers.get("accept-language"), "common.dismiss_group"));

						// 向群组成员发送退出的系统消息
						GroupUserQueryResult groupQueryUserResult = RongCloud.getInstance(appKey, appSecret).group
								.queryUser(deviceSn);
						System.out.println("queryUser:  " + groupQueryUserResult.getUsers());

						JSONObject obj = null;
						JSONArray jsonArray = null;
						obj = JSONObject.parseObject(groupQueryUserResult.toString());
						jsonArray = obj.getJSONArray("users");
						String QuitUser = "";
						for (Object curobj : jsonArray) {
							JSONObject curJsonObj = (JSONObject) curobj;
							QuitUser += curJsonObj.get("id").toString();
							QuitUser += ",";
						}

						String[] groupQuitUser = QuitUser.split(",");

						sendPbSysInfoNtf(deviceSn, groupQuitUser, "0");

						// 解散群组方法。（将该群解散，所有用户都无法再接收该群的消息。）
						CodeSuccessResult groupDismissResult = RongCloud.getInstance(appKey, appSecret).group
								.dismiss(userName, deviceSn);
						System.out.println("dismiss:  " + groupDismissResult.toString());
						if (groupDismissResult.getCode() == 200) {
							Map<String, String> param1 = new HashMap<String, String>();
							param1.put("function", "deleteGroup");
							param1.put("deviceSn", deviceSn);

							rpcResult = regConsumerService.hanleRegService("deleteGroup", param);

							if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
									|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString()
											.equals("0")) {
								map.put("code", "550");
								map.put("ret", "");
								map.put("what", LanguageManager.getMsg(headers.get("accept-language"),
										"common.delete_group_fail"));
								return map;
							}
						}

					}

					map.put("code", "0");
					map.put("ret", daoUtil.getTrackerUser(trackerId));
					map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forlogin.unbind_success"));
					return map;

				} else {
					// 当指令下发失败，根据指令返回不同提示语句
					String returnMsg = LanguageManager.getMsg(headers.get("accept-language"),
							"common.softwareupgrad_status_" + result);

					map.put("code", "550");
					map.put("ret", "");
					map.put("what", returnMsg);
					return map;
				}

			} else // 非超级用户
			{
				Map<String, String> param = new HashMap<String, String>();
				param.put("function", "unauthorization");
				param.put("username", userName);
				param.put("deviceSn", deviceSn);

				RPCResult rpcResult = regConsumerService.hanleRegService("unauthorization", param);
				if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS
						&& JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
					int userId = daoUtil.getUserId(userName);
					if (!daoUtil.cancelBinding(trackerId, userId)) {
						map.put("code", "550");
						map.put("ret", "");
						map.put("what",
								LanguageManager.getMsg(headers.get("accept-language"), "forlogin.unbind_failed"));
						return map;
					} else {

						if (daoUtil.getdeviceprtocoltype(deviceSn) == 5 || daoUtil.getdeviceprtocoltype(deviceSn) == 6
								|| daoUtil.getdeviceprtocoltype(deviceSn) == 7) { // 删除群组成员
							if (getGroupUserByName(params, headers, reqBody).get("code").equals("0")) {
								if (!deleteGroupUser(params, headers, reqBody).get("code").equals("0")) {
									map.put("code", "550");
									map.put("ret", "");
									map.put("what", LanguageManager.getMsg(headers.get("accept-language"),
											"common.delete_group_user_fail"));
									return map;
								}
							}

						}

						map.put("code", "0");
						map.put("ret", daoUtil.getTrackerUser(trackerId));
						map.put("what",
								LanguageManager.getMsg(headers.get("accept-language"), "forlogin.unbind_success"));
						return map;
					}
				} else {
					map.put("code", "550");
					map.put("ret", "");
					map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forlogin.unbind_failed"));
					return map;
				}
			}
		} else // 不在一台服务器上
		{
			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "unauthorization");
			param.put("username", userName);
			param.put("deviceSn", deviceSn);

			/****** 增加访问远程接口代码 ***********/
			if (userServerIpAddress.equals(env.getProperty("remoteServerIpAddress"))) {
				String requestPath = env.getProperty("remotingServer_Reg");
				JSONObject json = doPost_urlconn(requestPath, param);
				if (json == null || !(json.get("code").equals("0"))) {
					LogManager.error("远程注册服务器解绑失败");
					map.put("code", "580");
					map.put("ret", "");
					map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forlogin.unbind_failed"));
					return map;
				}
			}
			/****** 增加访问远程接口代码 ***********/

			RPCResult rpcResult = regConsumerService.hanleRegService("unauthorization", param);

			if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS
					&& JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {

				if (daoUtil.getdeviceprtocoltype(deviceSn) == 5 || daoUtil.getdeviceprtocoltype(deviceSn) == 6
						|| daoUtil.getdeviceprtocoltype(deviceSn) == 7) { // 删除群组成员
					if (getGroupUserByName(params, headers, reqBody).get("code").equals("0")) {
						if (!deleteGroupUser(params, headers, reqBody).get("code").equals("0")) {
							map.put("code", "550");
							map.put("ret", "");
							map.put("what", LanguageManager.getMsg(headers.get("accept-language"),
									"common.delete_group_user_fail"));
							return map;
						}
					}

				}

				// TODO:ZSH 超级用户查看某个追踪器绑定的其他用户
				param = new HashMap<String, String>();
				param.put("function", "getTrackerUser");
				param.put("deviceSn", deviceSn);

				JSONArray obj = null;
				try {
					rpcResult = regConsumerService.hanleRegService("getDeviceInfoCN", param);
					if (null != rpcResult.getRpcResult()) {

						JSONObject json = JSON.parseObject(rpcResult.getRpcResult().toString());
						obj = json.getJSONArray("data");
					}

					if (obj == null) {
						obj = new JSONArray();
					}
				} catch (Exception e) {
					//
				}

				map.put("code", "0");
				map.put("ret", obj);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forlogin.unbind_success"));
				return map;
			} else {
				map.put("code", "550");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forlogin.unbind_failed"));
				return map;
			}
		}
	}

	/**
	 * 保存信息卡（添加限定权限）
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> setInfo(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		Map<String, Object> mapDeviceType = daoUtil.getDeviceType(deviceSn);

		String nickName = DataUtil.getStringFromMap(params, "nickName");
		String mobile1 = DataUtil.getStringFromMap(params, "mobile1");
		String mobile2 = DataUtil.getStringFromMap(params, "mobile2");
		String mobile3 = DataUtil.getStringFromMap(params, "mobile3");
		String simNo = DataUtil.getStringFromMap(params, "simNo");

		if (Integer.parseInt(mapDeviceType.get("ranges").toString()) != 5
				&& Integer.parseInt(mapDeviceType.get("ranges").toString()) != 6
				&& Integer.parseInt(mapDeviceType.get("ranges").toString()) != 7) {
			// 通知网关
			CastelMessage responseMsg = gatewayHandler.set101E(deviceSn, mobile1, mobile2, mobile3);
			if (responseMsg == null) {
				mobile1 = "";
				mobile2 = "";
				mobile3 = "";
			}

			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "updatedeviceinfo");
			param.put("deviceSn", deviceSn);
			param.put("nickname", nickName);

			try {
				regConsumerService.hanleRegService("updatedeviceinfo", param);
			} catch (Exception e) {
				//
			}

			switch (mapDeviceType.get("ranges").toString()) {
			case "1":// 个人
				String humanName = nickName;
				String humanSex = DataUtil.getStringFromMap(params, "humanSex");
				String humanAge = DataUtil.getStringFromMap(params, "humanAge");
				String humanHeight = DataUtil.getStringFromMap(params, "humanHeight");
				String humanWeight = DataUtil.getStringFromMap(params, "humanWeight");
				String humanStep = DataUtil.getStringFromMap(params, "humanStep");
				String humanFeature = DataUtil.getStringFromMap(params, "humanFeature");
				String humanAddr = DataUtil.getStringFromMap(params, "humanAddr");
				String humanLostAddr = DataUtil.getStringFromMap(params, "humanLostAddr");
				String humanBirthday = DataUtil.getStringFromMap(params, "humanBirthday");

				return setHumanInfo(deviceSn, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
						humanFeature, humanAddr, mobile1, mobile2, mobile3, humanLostAddr, humanBirthday, simNo,
						language);
			case "2":// 宠物
				String pet_name = nickName;
				String pet_sex = DataUtil.getStringFromMap(params, "pet_sex");
				String pet_breed = DataUtil.getStringFromMap(params, "pet_breed");
				String pet_age = DataUtil.getStringFromMap(params, "pet_age");
				String pet_weight = DataUtil.getStringFromMap(params, "pet_weight");
				String pet_feature = DataUtil.getStringFromMap(params, "pet_feature");
				String pet_addr = DataUtil.getStringFromMap(params, "pet_addr");
				String pet_lost_addr = DataUtil.getStringFromMap(params, "pet_lost_addr");
				String pet_birthday = DataUtil.getStringFromMap(params, "pet_birthday");

				return setPetInfo(deviceSn, pet_name, pet_sex, pet_breed, pet_age, pet_weight, pet_feature, pet_addr,
						pet_lost_addr, mobile1, mobile2, mobile3, pet_birthday, simNo, language);
			case "3":// 汽车
				String car_no = DataUtil.getStringFromMap(params, "car_no");
				String car_vin = DataUtil.getStringFromMap(params, "car_vin");
				String car_engin = DataUtil.getStringFromMap(params, "car_engin");
				String car_set = DataUtil.getStringFromMap(params, "car_set");
				String car_brand = DataUtil.getStringFromMap(params, "car_brand");
				String car_year = DataUtil.getStringFromMap(params, "car_year");
				String car_type = DataUtil.getStringFromMap(params, "car_type");
				String car_oil_type = DataUtil.getStringFromMap(params, "car_oil_type");
				String car_mileage = DataUtil.getStringFromMap(params, "car_mileage");
				String car_check_time = DataUtil.getStringFromMap(params, "car_check_time");
				String car_buytime = DataUtil.getStringFromMap(params, "car_buytime");

				return setCarInfo(deviceSn, nickName, car_no, car_vin, car_engin, car_set, car_brand, car_year,
						car_type, car_oil_type, mobile1, mobile2, mobile3, car_mileage, car_check_time, car_buytime,
						simNo, language);
			case "4":// 摩托车
				String motor_no = DataUtil.getStringFromMap(params, "motor_no");
				String moto_type = DataUtil.getStringFromMap(params, "moto_type");
				String motor_cc = DataUtil.getStringFromMap(params, "motor_cc");
				String motor_trademark = DataUtil.getStringFromMap(params, "motor_trademark");
				String motor_set = DataUtil.getStringFromMap(params, "motor_set");
				String motor_year = DataUtil.getStringFromMap(params, "motor_year");
				String motor_buytime = DataUtil.getStringFromMap(params, "motor_buytime");

				return setMotoInfo(deviceSn, nickName, motor_no, moto_type, motor_cc, motor_trademark, motor_set,
						motor_year, mobile1, mobile2, mobile3, motor_buytime, simNo, language);

			default:
				break;
			}

		} else // 手表、obd不需要下发指令
		{
			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "updatedeviceinfo");
			param.put("deviceSn", deviceSn);
			param.put("nickname", nickName);

			try {
				regConsumerService.hanleRegService("updatedeviceinfo", param);
			} catch (Exception e) {
				//
			}
			switch (mapDeviceType.get("ranges").toString()) {
			case "5":// 手表
				String humanName = nickName;
				String humanSex = DataUtil.getStringFromMap(params, "humanSex");
				String humanAge = DataUtil.getStringFromMap(params, "humanAge");
				String humanHeight = DataUtil.getStringFromMap(params, "humanHeight");
				String humanWeight = DataUtil.getStringFromMap(params, "humanWeight");
				String humanStep = DataUtil.getStringFromMap(params, "humanStep");
				String humanFeature = DataUtil.getStringFromMap(params, "humanFeature");
				String humanAddr = DataUtil.getStringFromMap(params, "humanAddr");
				String humanLostAddr = DataUtil.getStringFromMap(params, "humanLostAddr");
				String humanBirthday = DataUtil.getStringFromMap(params, "humanBirthday");

				return setWatchInfo(deviceSn, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
						humanFeature, humanAddr, mobile1, mobile2, mobile3, humanLostAddr, humanBirthday, simNo,
						language);
			case "6":// obd
				String obd_no = nickName;
				String obd_type = DataUtil.getStringFromMap(params, "obd_type");
				String obd_buytime = DataUtil.getStringFromMap(params, "obd_buytime");
				String car_vin = DataUtil.getStringFromMap(params, "car_vin");

				return setObdInfo(nickName, mobile1, mobile2, mobile3, deviceSn, obd_no, obd_type, obd_buytime, car_vin,
						simNo, language);
			case "7":// 蓝牙手表
				String human_Name = nickName;
				String human_Sex = DataUtil.getStringFromMap(params, "humanSex");
				String human_Height = DataUtil.getStringFromMap(params, "humanHeight");
				String human_Weight = DataUtil.getStringFromMap(params, "humanWeight");
				String human_Birthday = DataUtil.getStringFromMap(params, "humanBirthday");
				String human_Feature = DataUtil.getStringFromMap(params, "humanFeature");
				return setBluetoothWatchInfo(deviceSn, human_Name, human_Sex, human_Height, human_Weight,
						human_Birthday, mobile1, mobile2, mobile3, human_Feature, language);
			default:
				break;
			}

		}

		return null;
	}

	/**
	 * 获取信息卡信息(添加访问远程逻辑)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getInfo(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getInfo");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getInfo异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/
		//
		int deviceID = daoUtil.getDeviceID(deviceSn);
		Map<String, Object> mapDeviceType = daoUtil.getDeviceType(deviceSn);
		switch (mapDeviceType.get("ranges").toString()) {
		case "1":// 个人
			return getHumanInfo(deviceSn, deviceID, language);
		case "2":// 宠物
			return getPetInfo(deviceSn, deviceID, language);
		case "3":// 汽车
			return getCarInfo(deviceID, language);
		case "4":// 摩托车
			return getMotoInfo(deviceID, language);
		case "5":// 手表
			return getWatchInfo(deviceSn, deviceID, language);
		case "6":// obd
			return getObdInfo(deviceID, language);
		case "7":// 蓝牙手表
			return getBluetoothWatchInfo(deviceSn, deviceID, language);
		default:
			break;
		}
		return null;
	}

	/**
	 * 修改密码(针对当前用户，无需处理)
	 */
	public Map<String, Object> modifyuserpassword(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String sessionID = headers.get("sessionID");

		String language = headers.get("accept-language");

		// 用户名
		String username = Session.getUser(sessionID);
		// 原密码
		String currentPassword = DataUtil.getStringFromMap(params, "currentpassword");
		// 新密码
		String newPasssword = DataUtil.getStringFromMap(params, "newpassword");

		if (!PropertyUtil.isNotBlank(username) || !PropertyUtil.isNotBlank(currentPassword)
				|| !PropertyUtil.isNotBlank(newPasssword)) {
			return null;
		}

		// 给密码加密
		String entrycurrentPassword = MD5.toMD5(currentPassword);
		int result = daoUtil.login(username, entrycurrentPassword);
		if (result == -1) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.database_access_error"));
			return map;
		}

		if (result == 1) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.login_error"));
			return map;
		}

		if (!daoUtil.modifyUserPassword(username, MD5.toMD5(newPasssword))) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.save_password_failed"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.change_password_success"));
		return map;
	}

	/**
	 * 修改设备的sim卡号（添加限定权限，同步数据到远程）
	 */
	public Map<String, Object> modifysim(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String simno = DataUtil.getStringFromMap(params, "simNo");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		if (simno == null || simno.trim().isEmpty()) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "sim.failed"));
			return map;
		}

		if (deviceSn == null || deviceSn.trim().isEmpty()) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "sim.failed"));
			return map;
		}

		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "updatedeviceinfo");
		param.put("deviceSn", deviceSn);
		param.put("tracker_sim", simno);

		try {
			regConsumerService.hanleRegService("updatedeviceinfo", param);
		} catch (Exception e) {
			//
		}

		if (!daoUtil.modifySim(simno, deviceSn, username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "sim.failed"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "sim.success"));
		return map;
	}

	/**
	 * 我的账号获取我的设备和授权的设备（没有处理）
	 */
	public Map<String, Object> getaccount(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);

		map.put("code", "0");
		map.put("ret", daoUtil.getAccount(username));
		map.put("what", "");
		return map;
	}

	/**
	 * 设置报警开关（添加限定权限）
	 */
	public Map<String, Object> settoggle(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String boundary = DataUtil.getStringFromMap(params, "boundary") == null ? "1"
				: DataUtil.getStringFromMap(params, "boundary");
		String voltage = DataUtil.getStringFromMap(params, "voltage") == null ? "1"
				: DataUtil.getStringFromMap(params, "voltage");
		String tow = DataUtil.getStringFromMap(params, "tow") == null ? "1" : DataUtil.getStringFromMap(params, "tow");
		String clipping = DataUtil.getStringFromMap(params, "clipping") == null ? "1"
				: DataUtil.getStringFromMap(params, "clipping");
		String speed = DataUtil.getStringFromMap(params, "speed") == null ? "1"
				: DataUtil.getStringFromMap(params, "speed");
		String speedValue = DataUtil.getStringFromMap(params, "speedValue") == null ? "120"
				: DataUtil.getStringFromMap(params, "speedValue");
		String speedTime = DataUtil.getStringFromMap(params, "speedTime") == null ? "60"
				: DataUtil.getStringFromMap(params, "speedTime");
		String sos = DataUtil.getStringFromMap(params, "sos") == null ? "1" : DataUtil.getStringFromMap(params, "sos");

		String vibration = DataUtil.getStringFromMap(params, "vibration") == null ? "0"
				: DataUtil.getStringFromMap(params, "vibration");
		String vibrationAspeed = DataUtil.getStringFromMap(params, "vibrationAspeed") == null ? "3"
				: DataUtil.getStringFromMap(params, "vibrationAspeed");
		String vibrationTime = DataUtil.getStringFromMap(params, "vibrationTime") == null ? "30"
				: DataUtil.getStringFromMap(params, "vibrationTime");
		String takeOff = DataUtil.getStringFromMap(params, "takeOff") == null ? "1"
				: DataUtil.getStringFromMap(params, "takeOff");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		if (!daoUtil.hasOperatingAuthority(trackerID, userID)) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return map;
		}

		if (daoUtil.getdeviceprtocoltype(deviceSn) == 0) {
			Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
			bodyVo.put(1, boundary);
			bodyVo.put(2, voltage);
			bodyVo.put(3, tow);
			bodyVo.put(4, clipping);
			bodyVo.put(5, speed);
			bodyVo.put(6, sos);
			bodyVo.put(7, vibration);
			bodyVo.put(8, vibrationAspeed);
			bodyVo.put(9, vibrationTime);
			bodyVo.put(10, takeOff);

			// 通知网关
			Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
			String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
			// 通知网关
			CastelMessage responseMsg = null;
			if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
				responseMsg = gatewayHttpHandler.setAlarmSetting(deviceSn, bodyVo);
			} else {
				responseMsg = gatewayHandler.set1011(deviceSn, bodyVo);
			}

			// int
			// result=Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString());
			if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
				// 设置超速报警阈值
				if (Integer.parseInt(speed) == 1) {
					gatewayHandler.set1010(deviceSn, Integer.parseInt(speedValue), Integer.parseInt(speedTime));
				}

				if (daoUtil.isHaveAlarmSet(trackerID)) {
					daoUtil.updateAlarmSet(boundary, voltage, tow, clipping, speed, Integer.parseInt(speedValue),
							Integer.parseInt(speedTime), sos, userID, trackerID, vibration,
							Integer.parseInt(vibrationAspeed), Integer.parseInt(vibrationTime), takeOff);
				} else {
					daoUtil.insertAlarmSet(boundary, voltage, tow, clipping, speed, Integer.parseInt(speedValue),
							Integer.parseInt(speedTime), sos, userID, trackerID, vibration,
							Integer.parseInt(vibrationAspeed), Integer.parseInt(vibrationTime), takeOff);
				}
			} else {
				map.put("code", "600");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
				return map;
			}
		} else {
			Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
			bodyVo.put(1, boundary);
			bodyVo.put(2, voltage);
			bodyVo.put(3, tow);
			bodyVo.put(4, clipping);
			bodyVo.put(5, speed);
			bodyVo.put(6, sos);
			bodyVo.put(7, vibration);
			bodyVo.put(8, vibrationAspeed);
			bodyVo.put(9, vibrationTime);
			bodyVo.put(10, takeOff);

			// 通知网关
			Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
			String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
			// 通知网关
			CastelMessage responseMsg = null;
			if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
				responseMsg = gatewayHttpHandler.setAlarmSetting(deviceSn, bodyVo);
			} else {
				responseMsg = gatewayHandler.set1011(deviceSn, bodyVo);
			}
			if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
				if (daoUtil.isHaveAlarmSet(trackerID)) {
					daoUtil.updateAlarmSet(boundary, voltage, tow, clipping, speed, Integer.parseInt(speedValue),
							Integer.parseInt(speedTime), sos, userID, trackerID, vibration,
							Integer.parseInt(vibrationAspeed), Integer.parseInt(vibrationTime), takeOff);
				} else {
					daoUtil.insertAlarmSet(boundary, voltage, tow, clipping, speed, Integer.parseInt(speedValue),
							Integer.parseInt(speedTime), sos, userID, trackerID, vibration,
							Integer.parseInt(vibrationAspeed), Integer.parseInt(vibrationTime), takeOff);
				}
			} else {
				map.put("code", "600");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
				return map;
			}
		}

		map.put("code", "0");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "tracker.success"));
		return map;
	}

	/**
	 * 获取报警开关(添加访问远程逻辑)
	 */
	public Map<String, Object> gettoggle(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "gettoggle");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问gettoggle异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", daoUtil.getToggle(trackerID));
		map.put("what", "");

		return map;
	}

	/**
	 * 获取报警信息(添加访问远程逻辑)
	 */
	public Map<String, Object> getalarminfo(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String startTime = DataUtil.getStringFromMap(params, "start");
		String endTime = DataUtil.getStringFromMap(params, "end");
		String type = DataUtil.getStringFromMap(params, "type");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getalarminfo");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("start", startTime);
					param.put("end", endTime);
					param.put("type", type);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getalarminfo异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		int trackerId = daoUtil.getDeviceID(deviceSn);
		if (trackerId <= 0) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		if (!daoUtil.hasOperatingAuthority(trackerId, userId)) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return map;
		}

		List<Map<String, Object>> list = daoUtil.getAlarmInfo(trackerId, startTime, endTime, type);
		if (list == null || list.size() == 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "tracker.no_alarm"));
			return map;
		}

		Map<String, Object> ret = new HashMap<>();
		ret.put("deviceSn", deviceSn);
		ret.put("alarm", list);

		map.put("code", "0");
		map.put("ret", ret);
		map.put("what", "");
		return map;
	}

	/**
	 * 改变设备范围（添加限定权限，同步数据到远程）
	 */
	public Map<String, Object> changeDeviceRanges(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String deviceRanges = DataUtil.getStringFromMap(params, "deviceRanges");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerId = daoUtil.getDeviceID(deviceSn);
		if (trackerId <= 0) {
			map.put("code", "100"); // 不存在
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "updatedeviceinfo");
		param.put("deviceSn", deviceSn);
		param.put("ranges", deviceRanges);

		try {
			regConsumerService.hanleRegService("updatedeviceinfo", param);
		} catch (Exception e) {
			//
		}

		if (daoUtil.changDeviceRanges(trackerId, deviceRanges)) {
			map.put("code", "0"); // 成功
			map.put("ret", "");
			map.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.changedeviceranges_success"));
			return map;
		} else {
			map.put("code", "200");// 失败
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.changedeviceranges_failed"));
			return map;
		}

	}

	/**
	 * @author wj 登出接口(针对当前用户，无需处理)
	 * @param request
	 * @return
	 */
	public Map<String, Object> logout(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		// add by ljl
		if (true/* request.getSession() != null */) {

			String sessionID = headers.get("sessionID");
			String username = Session.getUser(sessionID);

			SessionUtil.sessionIdToUsernameMap.remove(sessionID);
			SessionUtil.usernameToSessionIdMap.remove(username);
			LogManager.info("##################退出登录#######################");
		}
		Map<String, Object> map = new HashMap<>();
		map.put("code", "0");
		map.put("ret", "");
		map.put("what", "");

		return map;
	}

	/**
	 * @author wj 保存崩溃日志(针对当前用户，无需处理)
	 * @param request
	 * @return
	 */
	public Map<String, Object> savelog(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String log = DataUtil.getStringFromMap(params, "log");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		int userid = daoUtil.getUserId(username);

		if (PropertyUtil.isNotBlank(log)) {
			String path = PropertyUtil.getWebConfig("lunixSaveHeadPortraitRelativePath")
					+ PropertyUtil.getWebConfig("SaveCrashFolder");
			File file = new File(path);

			// 判断目录是否存在，不存在则创建，存在则不管
			if (!file.exists() && !file.isDirectory()) {
				file.mkdir();
			}

			file = new File(path + "/" + System.currentTimeMillis() + "_" + userid + ".log");// 目标文件
			if (!file.exists())
				try {
					file.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}

			PrintStream ps = null;
			try {
				ps = new PrintStream(new FileOutputStream(file));
				ps.println(log);// 往文件里写入字符串
				ps.flush();
				ps.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		map.put("code", "0");
		map.put("ret", userid);
		map.put("what", "");
		return map;
	}

	/**
	 * @author wj 解锁车辆（添加限定权限）
	 * @param request
	 * @return
	 */
	public Map<String, Object> unlockVehicle(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		int ranges = daoUtil.getTypeOfTracker(trackerID);
		if (ranges <= 2) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.non_motor_vehicle"));
			return map;
		}

		// 通知网关
		CastelMessage responseMsg = gatewayHandler.set1007(deviceSn, 0);
		// int
		// result=Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString());
		if ((ranges == 6)
				|| (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0)) {
			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "updatedeviceinfo");
			param.put("deviceSn", deviceSn);
			param.put("defensive", "0");

			try {
				regConsumerService.hanleRegService("updatedeviceinfo", param);
			} catch (Exception e) {
				//
			}

			if (daoUtil.lockVehicle(trackerID, 0)) {
				map.put("code", "0");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_success"));
				return map;
			}
		}

		map.put("code", "600");
		map.put("ret", null);
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
		return map;
	}

	/**
	 * @author wj 锁定车辆（添加限定权限）
	 * @param request
	 * @return
	 */
	public Map<String, Object> lockVehicle(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		int ranges = daoUtil.getTypeOfTracker(trackerID);
		if (ranges <= 2) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.non_motor_vehicle"));
			return map;
		}

		// 通知网关
		CastelMessage responseMsg = gatewayHandler.set1007(deviceSn, 1);
		// int
		// result=Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString());
		if ((ranges == 6)
				|| (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0)) {
			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "updatedeviceinfo");
			param.put("deviceSn", deviceSn);
			param.put("defensive", "1");

			try {
				regConsumerService.hanleRegService("updatedeviceinfo", param);
			} catch (Exception e) {
				//
			}

			if (daoUtil.lockVehicle(trackerID, 1)) {
				map.put("code", "0");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_success"));
				return map;
			}
		}

		map.put("code", "600");
		map.put("ret", null);
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
		return map;
	}

	/**
	 * @author wj
	 *         设置推送提醒方式，在tuser表中，alertway，默认1，双开，2，双关，3，震动开，响铃关，4，震动关，响铃开(没有处理)
	 * @param request
	 * @return
	 */
	public Map<String, Object> setalertway(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String alertway = DataUtil.getStringFromMap(params, "alertway");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.setAlertMode(alertway, userID)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "setalertway.failed"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "setalertway.success"));
		return map;
	}

	/**
	 * @author wj 设置用户时区(同步数据到远程)
	 * @param request
	 * @return
	 */
	public Map<String, Object> settimezone(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String timezone = DataUtil.getStringFromMap(params, "timezone");// 秒
		int timezoneId = Integer.parseInt(DataUtil.getStringFromMap(params, "timezoneId"));

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		int timezoneCheck = Integer.parseInt(DataUtil.getStringFromMap(params, "timezoneCheck"));

		// 校验时区
		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "settimezone");
		param.put("username", username);
		param.put("timezone", timezone);
		param.put("timezoneId", Integer.toString(timezoneId));
		param.put("timezoneCheck", Integer.toString(timezoneCheck));

		try {
			regConsumerService.hanleRegService("settimezone", param);
		} catch (Exception e) {
			//
		}

		if (!daoUtil.setTimezone(userID, timezone, timezoneId, timezoneCheck)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "settimezone.failed"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "settimezone.success"));
		return map;
	}

	/**
	 * 设置设备时区（添加限定权限）
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> setdevicetimezone(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		int timezone = DataUtil.getStringFromMap(params, "timezone") == null ? 0
				: Integer.parseInt(DataUtil.getStringFromMap(params, "timezone"));// 秒
		int timezoneid = DataUtil.getStringFromMap(params, "timezoneId") == null ? 31
				: Integer.parseInt(DataUtil.getStringFromMap(params, "timezoneId"));
		int language = (DataUtil.getStringFromMap(params, "language") != null
				&& !DataUtil.getStringFromMap(params, "language").toString().equals(""))
						? Integer.parseInt(DataUtil.getStringFromMap(params, "language").toString()) : 0;// 手表语言
		// 只有760手表设备传这个
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		Map<String, Object> deviceMap = daoUtil.getDeviceInfoByDevicesn(deviceSn);
		int trackerID = Integer.parseInt(deviceMap.get("id").toString());
		if (deviceMap == null) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		// 通知网关

		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setTzAndLang(deviceSn, String.valueOf(timezone), String.valueOf(language));
		} else {
			gatewayHandler.set100E(deviceSn, timezone, language);
		}
		if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
			if (daoUtil.setDeviceTimezone(trackerID, timezone, timezoneid, language)) {
				map.put("code", "0");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "settimezone.success"));
				return map;
			}

		}

		map.put("code", "600");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "settimezone.failed"));
		return map;
	}

	/**
	 * 获取设备时区(添加访问远程逻辑)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getdevicetimezone(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getdevicetimezone");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getdevicetimezone异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}
		Map<String, Object> deviceMap = daoUtil.getDeviceTimezone(trackerID);
		map.put("code", "0");
		map.put("ret", deviceMap);
		map.put("what", "");
		return map;
	}

	/**
	 * @author wj 定位频率设置 设置追踪器固定上传时间间隔（添加限定权限）
	 * @param request
	 * @return
	 */
	public Map<String, Object> setGpsInterval(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String interval = DataUtil.getStringFromMap(params, "interval");
		if (interval == null || interval.equals("")) {
			interval = "10";// 遛狗定位频率设置为10秒
		}

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		if (!daoUtil.hasOperatingAuthority(trackerID, userID)) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return map;
		}

		// 通知网关
		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		CastelMessage responseMsg = null;
		// 通知网关
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setGpsInterval(deviceSn, interval);
		} else {
			responseMsg = gatewayHandler.set1003(deviceSn, Integer.parseInt(interval));
		}
		// int
		// result=Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString());
		if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "updatedeviceinfo");
			param.put("deviceSn", deviceSn);
			param.put("gps_interval", interval);

			try {
				regConsumerService.hanleRegService("updatedeviceinfo", param);
			} catch (Exception e) {
				//
			}

			if (!daoUtil.saveGPSInterval(trackerID, interval)) {
				map.put("code", "500");
				map.put("ret", "");
				map.put("what",
						LanguageManager.getMsg(headers.get("accept-language"), "tracker.save_gpsinterval_failed"));
				return map;
			}
		} else {
			map.put("code", "600");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_success"));
		return map;
	}

	/**
	 * @author wj 查询GPS固定上传时间间隔(添加访问远程逻辑)
	 * @param req
	 * @return
	 */
	public Map<String, Object> getGpsInterval(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getGpsInterval");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getGpsInterval异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		if (!daoUtil.hasOperatingAuthority(trackerID, userID)) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "tracker.nopms_query_gpsinterval"));
			return map;
		}

		Map<String, Object> trackerMap = daoUtil.getTrackerInfo(trackerID);
		String gps_interval = "600";// 默认五分钟
		if (trackerMap != null && trackerMap.get("gps_interval") != null) {
			gps_interval = trackerMap.get("gps_interval").toString();
		}

		if (trackerMap != null && trackerMap.get("ranges") != null && trackerMap.get("ranges").toString().equals("4")
				&& (trackerMap.get("gps_interval") == null)) {
			gps_interval = "120";// 摩托车默认2分钟
		}

		if (trackerMap != null && trackerMap.get("protocol_type") != null
				&& trackerMap.get("protocol_type").toString().equals("5") && (trackerMap.get("gps_interval") == null)) {
			gps_interval = "600";// PT770默认10分钟
		}

		Map<String, Object> ret = new HashMap<>();
		ret.put("deviceSn", deviceSn);
		ret.put("gps_interval", gps_interval);

		map.put("code", "0");
		map.put("ret", ret);
		map.put("what", "");
		return map;
	}

	/**
	 * 追踪器重置（添加限定权限）
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> reset(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		if (!daoUtil.hasOperatingAuthority(trackerID, userID)) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return map;
		}

		// 通知网关
		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.deviceReset(deviceSn);
		} else {
			responseMsg = gatewayHandler.set1006(deviceSn);
		}

		// int
		// result=Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString());
		if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
			// 删除紧急联系人
			daoUtil.deleteEmergencyContactNo(trackerID);

			// 删除定位频率
			daoUtil.deleteGPSInterval(trackerID);

			// 删除区域
			daoUtil.deleteGeoFence(String.valueOf(trackerID));

			// 删除警情开关
			String boundary = "1";
			String voltage = "1";
			String tow = "1";
			String clipping = "1";
			String speed = "1";
			String speedValue = "120";
			String speedTime = "60";
			String sos = "1";
			String vibration = "0";
			String vibrationAspeed = "3";
			String vibrationTime = "30";
			String takeOff = "1";

			if (daoUtil.isHaveAlarmSet(trackerID)) {
				daoUtil.updateAlarmSet(boundary, voltage, tow, clipping, speed, Integer.parseInt(speedValue),
						Integer.parseInt(speedTime), sos, userID, trackerID, vibration,
						Integer.parseInt(vibrationAspeed), Integer.parseInt(vibrationTime), takeOff);
			} else {
				daoUtil.insertAlarmSet(boundary, voltage, tow, clipping, speed, Integer.parseInt(speedValue),
						Integer.parseInt(speedTime), sos, userID, trackerID, vibration,
						Integer.parseInt(vibrationAspeed), Integer.parseInt(vibrationTime), takeOff);
			}

			map.put("code", "0");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_success"));
		} else {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
		}

		return map;
	}

	/**
	 * @author wj 手动再次发验证邮件(针对当前用户，无需处理)
	 * @param request
	 * @return
	 */
	public Map<String, Object> againsendmail(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (daoUtil.getUserEmailVerify(username) == 1) {
			map.put("code", "0");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.had_verify"));
			return map;
		} else if (!emailUtil.sendRegisterEmail(username, language)) {
			map.put(ReturnObject.RETURN_CODE, "300");
			map.put(ReturnObject.RETURN_OBJECT, null);
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "email.send_email_failed_sys"));
			return map;
		} else {
			map.put(ReturnObject.RETURN_CODE, "0");
			map.put(ReturnObject.RETURN_OBJECT, null);
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "email.note_check_email"));
			return map;
		}

		// return daoUtil.sendEmail(request, userName);
	}

	/**
	 * @author wj 超级用户查看某个追踪器绑定的其他用户(添加访问远程逻辑)
	 * @param request
	 * @return
	 */
	public Map<String, Object> getTrackerUser(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		// TODO:ZSH 超级用户查看某个追踪器绑定的其他用户
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "getTrackerUser");
		param.put("deviceSn", deviceSn);

		try {
			JSONArray obj = null;
			RPCResult rpcResult = regConsumerService.hanleRegService("getTrackerUser", param);
			if (null != rpcResult.getRpcResult()) {
				JSONObject json = JSON.parseObject(rpcResult.getRpcResult().toString());
				obj = json.getJSONArray("data");
			}

			if (obj == null) {
				obj = new JSONArray();
				// Map<String, Object> element = new HashMap<String, Object>();
				// element.put("name", null);
				// obj.add(element);
			}

			Map<String, Object> ret = new HashMap<>();
			ret.put("deviceSn", deviceSn);
			ret.put("users", obj); // daoUtil.getTrackerUser(trackerID));

			map.put("code", "0");
			map.put("ret", ret);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.get_user_success"));
			return map;
		} catch (Exception e) {
			LogManager.error("远程访问getTrackerUser异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
	}

	/**
	 * @author wj 保存意见反馈（添加限定权限）
	 * @param request
	 * @return
	 */
	public Map<String, Object> saveidea(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String title = DataUtil.getStringFromMap(params, "title");
		String content = DataUtil.getStringFromMap(params, "content");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (null == username) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "idea.failed"));
			return map;
		}

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		if (!daoUtil.saveIdea(deviceSn, username, title, content)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "idea.failed"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "idea.success"));
		return map;
	}

	/**
	 * 注册激活(针对当前用户，无需处理)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> verifyemail(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String param = DataUtil.getStringFromMap(params, "params");
		String lang = DataUtil.getStringFromMap(params, "lang");

		if (!TextUtils.isEmpty(param)) {
			String verifycode = new String(Base64.decodeBase64(param));
			if (verifycode.indexOf("|") != -1) {
				String username = verifycode.substring(0, verifycode.indexOf("|"));
				if (!TextUtils.isEmpty(username)) {
					int userId = daoUtil.getUserId(username);
					if (daoUtil.changeUserVerifyStatus(userId, param)) {
						map.put("code", "0");
						map.put("ret", "");
						map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "active.success"));
						return map;
					}
				}
			}
		}

		map.put("code", "1");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "active.failed"));
		return map;
	}

	/**
	 * 忘记密码，设置密码(针对当前用户，无需处理)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> forgotpasswordhand(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String lang = DataUtil.getStringFromMap(params, "lang");
		String param = DataUtil.getStringFromMap(params, "params");
		String curusername = DataUtil.getStringFromMap(params, "inname");
		String curpassword = DataUtil.getStringFromMap(params, "inpass");

		if (param == null || param.isEmpty()) {
			map.put("code", "1");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forgot.1"));
			return map;
		}

		if (curusername == null || curusername.isEmpty() || curpassword == null || curpassword.isEmpty()) {
			map.put("code", "1");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forgot.2"));
			return map;
		}

		String verifycodeparams = new String(Base64.decodeBase64(param));
		if (verifycodeparams.indexOf("|") == -1) {
			map.put("code", "1");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forgot.1"));
			return map;
		}

		String[] paramArray = verifycodeparams.split("\\|");
		String username = paramArray[0];
		String pwdverifycode = paramArray[2];
		if (username == null || username.isEmpty() || !username.equals(curusername)) {
			map.put("code", "1");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forgot.3"));
			return map;
		}

		int userId = daoUtil.getUserId(username);
		Map<String, Object> userinfo = daoUtil.getUserInfo(userId);
		if (userinfo == null) {
			map.put("code", "1");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forgot.6"));
			return map;
		}

		boolean pwd_is_done = (boolean) userinfo.get("pwd_is_done");
		String pwd_verify_code = userinfo.get("pwd_verify_code").toString();
		if (pwd_is_done) {
			map.put("code", "1");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forgot.4"));
			return map;
		}

		if (!pwd_verify_code.equals(pwdverifycode)) {
			map.put("code", "1");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forgot.3"));
			return map;
		}

		if (daoUtil.changepassword(userId, MD5.toMD5(curpassword))) {
			map.put("code", "0");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "forgot.7"));
			return map;
		} else {
			map.put("code", "0");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.change_password_success"));
			return map;
		}
	}

	/**
	 * 分享信息卡(针对当前用户，无需处理)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getlostcardforhtml(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<String, Object>();

		String lang = DataUtil.getStringFromMap(params, "lang");
		if (lang == null || lang.isEmpty()) {
			lang = PropertyUtil.getWebConfig("default_language");

			map.put("code", "50");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.param_error"));
			return map;
		}

		String trackerid = DataUtil.getStringFromMap(params, "trackerid");
		if (trackerid == null || trackerid.isEmpty()) {
			map.put("code", "50");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.param_error"));
			return map;
		}

		int deviceId = Integer.parseInt(trackerid);
		if (deviceId <= 0) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.device_not_exist"));
			return map;
		}

		Map<String, Object> deviceinfo = daoUtil.getDeviceInfoForShare(deviceId);
		if (deviceinfo == null || deviceinfo.size() <= 0) {
			map.put("code", "150");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.deviceinfo_not_exist"));
			return map;
		}

		String host = headers.get("host");
		Object headportrait = deviceinfo.get("head_portrait");
		if (headportrait != null && !headportrait.toString().isEmpty()) {
			deviceinfo.put("head_portrait", "http://" + host + headportrait.toString());
		}

		Map<String, Object> device = daoUtil.getTrackerInfo(deviceId);
		String deviceSn = device.get("device_sn").toString();
		String local = "CN";
		if (lang != null && lang.indexOf("-") != -1) {
			String[] localArray = lang.split("\\-");
			if (localArray != null && localArray.length >= 1) {
				local = localArray[1].toUpperCase();
			}

			if (local.equals("TR")) {
				local = "US";
			}
		}
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);
		Map<String, Object> gps = daoUtil.getCurrentGPS(deviceSn, timezone);
		if (gps != null) {
			deviceinfo.put("lat", gps.get("lat"));
			deviceinfo.put("lng", gps.get("lng"));

			CastelMessage message = gatewayHandler.set1201(deviceSn, gps.get("lat").toString(),
					gps.get("lng").toString(), local);
			if (message != null && message.getMsgBodyMapVo() != null && message.getMsgBodyMapVo().size() >= 1) {
				deviceinfo.put("lostlocation", message.getMsgBodyMapVo().get(1));
			} else {
				deviceinfo.put("lostlocation", "");
			}
		} else {
			deviceinfo.put("lat", "");
			deviceinfo.put("lng", "");
			deviceinfo.put("lostlocation", "");
		}

		map.put("code", "0");
		map.put("ret", deviceinfo);
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.success"));
		return map;
	}

	/**
	 * 保存用户TOKEN(同步数据到远程)
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> saveusertoken(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody)
			throws Exception {
		Map<String, Object> map = new HashMap<>();
		String token = DataUtil.getStringFromMap(params, "token");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (null == username) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		// TODO:ZSH 远程同步数据到注册服务器
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "saveUserToken");
		param.put("username", username);
		param.put("token", token);
		param.put("versions", "3");// 3：表示航通守护者3.0版本

		RPCResult rpcResult = regConsumerService.hanleRegService("saveUserToken", param);
		if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
				|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.save_token_faile"));
			return map;
		} else {
			if (!daoUtil.saveUserToken(token, username, 3)) {
				map.put("code", "100");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.save_token_faile"));
				return map;
			}
		}

		map.put("code", "0");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.save_token_success"));
		return map;
	}

	/**
	 * 获取升级进度(添加访问远程逻辑)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getupgradprogress(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getupgradprogress");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getupgradprogress异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		// 通知网关
		CastelMessage responseMsg = gatewayHandler.set101A(deviceSn);
		if (responseMsg != null) {
			Map<String, Object> upgradeMap = new HashMap<String, Object>();
			upgradeMap.put("upgradstatus", responseMsg.getMsgBodyMapVo().get(1));
			upgradeMap.put("upgradvalue", responseMsg.getMsgBodyMapVo().get(2));

			System.out.print("upgradstatus:" + responseMsg.getMsgBodyMapVo().get(1) + "---------" + "upgradvalue:"
					+ responseMsg.getMsgBodyMapVo().get(2));

			map.put("code", "0");
			map.put("ret", upgradeMap);
			map.put("what", "");
		} else {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
		}

		return map;
	}

	/**
	 * 获取套餐数据
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getOrderPackage(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		Map<String, Object> deviceMap = daoUtil.getDeviceAndPayeeByDevicesn(deviceSn);
		// 套餐信息
		Map<String, Object> packageMap = new HashMap<String, Object>();
		List<Map<String, Object>> list = daoUtil.getorderpackage(Integer.parseInt(deviceMap.get("org_id").toString()));
		Calendar calendar = Calendar.getInstance(); // 得到日历
		Date expiredTimeDeUtc = new Date();
		String expiredTimeDeStr = "";
		// 当前utc时间
		String currentUTCtime = DateTimeUtil.getCurrentUtcDatetime();
		try {
			if (list != null && list.size() > 0) {
				for (int i = 0; i < list.size(); i++) {
					Map<String, Object> mapObj = list.get(i);
					int month = mapObj.get("month") != null ? Integer.parseInt(mapObj.get("month").toString()) : 0;
					expiredTimeDeStr = DateTimeUtil.getDateString(deviceMap.get("expired_time_de"));
					// 还未过期
					if (expiredTimeDeStr.compareTo(currentUTCtime) > 0) {
						calendar.setTime(((Date) deviceMap.get("expired_time_de")));
						calendar.add(calendar.MONTH, month);
						expiredTimeDeUtc = calendar.getTime();
						expiredTimeDeStr = sdf.format(expiredTimeDeUtc);
						mapObj.put("expired_time_de", expiredTimeDeStr);

					} else {
						// 已过期
						calendar.setTime(sdf.parse(currentUTCtime));
						calendar.add(calendar.MONTH, month);
						expiredTimeDeUtc = calendar.getTime();
						expiredTimeDeStr = sdf.format(expiredTimeDeUtc);
						mapObj.put("expired_time_de", expiredTimeDeStr);
					}

				}
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		packageMap.put("packageList", list);
		packageMap.put("device", deviceMap);
		map.put("code", "0");
		map.put("ret", packageMap);
		map.put("what", "");
		return map;
	}

	/********************************************* 支付部分 ***************************************************************/
	/**
	 * 支付宝生成订单
	 */
	public Map<String, Object> getorderinfo(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		// 完整的符合支付宝参数规范的订单信息
		String payInfo;
		try {
			// 商品名称
			String subject = DataUtil.getStringFromMap(params, "subject");
			// 商品详情
			String body = DataUtil.getStringFromMap(params, "body");
			// 商品金额
			String price = DataUtil.getStringFromMap(params, "price");
			// 设备ID
			String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
			// 用户名
			String username = DataUtil.getStringFromMap(params, "username");
			// 所选择套餐ID
			String orderpackageid = DataUtil.getStringFromMap(params, "orderpackageid");
			// 货币单位
			String unit = DataUtil.getStringFromMap(params, "unit");

			// 签约合作者身份ID
			String orderInfo = "partner=" + "\"" + AlipayConfig.partner + "\"";
			// 签约卖家支付宝账号
			orderInfo += "&seller_id=" + "\"" + AlipayConfig.seller + "\"";

			// 商户网站唯一订单号
			Random rnd = new Random();
			String tailNo = Integer.toString(rnd.nextInt(1000));
			while (tailNo.length() < 4) {
				tailNo = "0" + tailNo;
			}
			String ordernum = UtilDate.getOrderNum();
			ordernum = ordernum + tailNo;
			orderInfo += "&out_trade_no=" + "\"" + ordernum + "\"";

			// 商品名称
			orderInfo += "&subject=" + "\"" + subject + "\"";
			// 商品详情
			orderInfo += "&body=" + "\"" + body + "\"";
			// 商品金额
			orderInfo += "&total_fee=" + "\"" + price + "\"";
			// 服务器异步通知页面路径
			String webpath = env.getProperty("webPath");
			orderInfo += "&notify_url=" + "\"" + webpath + "zfbnotify_url.jsp" + "\"";
			// 服务接口名称， 固定值
			orderInfo += "&service=\"mobile.securitypay.pay\"";
			// 支付类型， 固定值
			orderInfo += "&payment_type=\"1\"";
			// 参数编码， 固定值
			orderInfo += "&_input_charset=\"" + AlipayConfig.input_charset + "\"";
			// 设置未付款交易的超时时间
			// 默认30分钟，一旦超时，该笔交易就会自动被关闭。
			// 取值范围：1m～15d。
			// m-分钟，h-小时，d-天，1c-当天（无论交易何时创建，都在0点关闭）。
			// 该参数数值不接受小数点，如1.5h，可转换为90m。
			orderInfo += "&it_b_pay=\"30m\"";
			// extern_token为经过快登授权获取到的alipay_open_id,带上此参数用户将使用授权的账户进行支付
			// orderInfo += "&extern_token=" + "\"" + extern_token + "\"";

			// 支付宝处理完请求后，当前页面跳转到商户指定页面的路径，可空
			orderInfo += "&return_url=\"m.alipay.com\"";

			// 调用银行卡支付，需配置此参数，参与签名， 固定值 （需要签约《无线银行卡快捷支付》才能使用）
			// orderInfo += "&paymethod=\"expressGateway\"";

			// return orderInfo;

			// 对订单做RSA 签名
			String sign = "";
			// RSA.sign(orderInfo, AlipayConfig.private_key,
			// AlipayConfig.input_charset);
			try {
				// 仅需对sign 做URL编码
				sign = URLEncoder.encode(sign, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

			payInfo = orderInfo + "&sign=\"" + sign + "\"&" + "sign_type=\"RSA\"";

			// 做订单保存本地数据库工作
			// daoUtil.saveOrder(ordernum,username,orderpackageid,deviceSn,
			// payInfo, price, 1,unit);
		} catch (Exception e) {
			e.printStackTrace();

			Map<String, Object> map = new HashMap<>();
			map.put("code", "-1");
			map.put("ret", "");
			return map;
		}

		Map<String, Object> map = new HashMap<>();
		map.put("code", "0");
		map.put("ret", payInfo);
		return map;
	}

	/**
	 * 贝宝支付订单
	 */
	public Map<String, Object> saveOrder(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		// 支付宝、微信支付、贝宝支付各不相同 订单id
		String tradeNo = DataUtil.getStringFromMap(params, "tradeNo");
		// 设备ID
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		// 商品类型
		String payType = DataUtil.getStringFromMap(params, "payType");
		// 支付帐号
		String payer = DataUtil.getStringFromMap(params, "payer");
		// 商品金额
		String price = DataUtil.getStringFromMap(params, "price");
		// 货币单位
		String currencyUnit = DataUtil.getStringFromMap(params, "currencyUnit");
		// 备注
		String remark = DataUtil.getStringFromMap(params, "remark");
		// 用户名
		// 所选择套餐ID
		String orderpackageid = DataUtil.getStringFromMap(params, "orderPackageId");

		Map<String, Object> map = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}
		try {
			Map<String, Object> deviceMap = daoUtil.getDeviceAndPayeeByDevicesn(deviceSn);

			LogManager.info("deviceSn:" + deviceSn + "___orderpackageid：" + orderpackageid.toString() + "___clientId:"
					+ deviceMap.get("clientId").toString() + "___secret:" + deviceMap.get("secret").toString());

			OAuthTokenCredential tokenCredential = new OAuthTokenCredential(deviceMap.get("clientId").toString(),
					deviceMap.get("secret").toString());
			String accessToken = tokenCredential.getAccessToken();

			Payment payment = Payment.get(accessToken, tradeNo);
			remark = payment.toString();
			payer = payment.getPayer().getPayerInfo().getEmail();

			Map<String, Object> mapObj = daoUtil.getOrderpackageById(Integer.parseInt(orderpackageid.toString()));
			int month = mapObj.get("month") != null ? Integer.parseInt(mapObj.get("month").toString()) : 0;

			LogManager.info("deviceSn:" + deviceSn + "___orderpackageid：" + orderpackageid.toString()
					+ "___paymentState:" + payment.getState());

			if (payment != null && payment.getState() != null && payment.getState().equals("approved")) {
				// daoUtil.saveOrderForPP(id, trackerid, body,
				// resContent,Constanct.ORDER_SUCCESS, price, ppstatus)
				if (!daoUtil.saveOrder(tradeNo, deviceSn, username, payType, payer, price, currencyUnit, remark, month,
						orderpackageid)) {
					map.put("code", "400");
					map.put("ret", "");
					map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "renew.license.fail"));
					return map;
				} else {// 保存成功
					Calendar calendar = Calendar.getInstance(); // 得到日历
					Date expiredTimeDeUtc = new Date();
					String expiredTimeDeStr = "";
					// int day = month * 30;

					// 当前utc时间
					String currentUTCtime = DateTimeUtil.getCurrentUtcDatetime();
					expiredTimeDeStr = DateTimeUtil.getDateString(deviceMap.get("expired_time_de"));
					// 还未过期
					if (expiredTimeDeStr.compareTo(currentUTCtime) > 0) {
						calendar.setTime(((Date) deviceMap.get("expired_time_de")));
						calendar.add(calendar.MONTH, month);
						expiredTimeDeUtc = calendar.getTime();
					} else {
						// 已过期
						calendar.setTime(sdf.parse(currentUTCtime));
						calendar.add(calendar.MONTH, month);
						expiredTimeDeUtc = calendar.getTime();

					}
					// 同步注册服务器日期
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"); // 设置时间格式
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "updateDeviceExpirationTime");
					param.put("deviceSn", "'" + deviceSn + "'");
					param.put("expired_time_de", sdf.format(expiredTimeDeUtc));

					RPCResult rpcResult = regConsumerService.hanleRegService("updateDeviceExpirationTime", param);

					if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS || !JSON
							.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
						map.put("code", "500");
						map.put("ret", "");
						map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "renew.license.fail"));
						return map;
					}
					// 修改数据库日期
					daoUtil.addExpiredForDevice(deviceSn, expiredTimeDeUtc);
					map.put("code", "0");
					map.put("ret", "");
					map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "renew.license.success"));
					return map;
				}
			} else {
				map.put("code", "600");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "renew.license.fail"));

				return map;
			}
		} catch (Exception e) {
			LogManager.info("deviceSn:" + deviceSn + "___orderpackageid：" + orderpackageid.toString() + "___Exception:"
					+ e.getMessage());
			e.printStackTrace();
			map.put("code", "700");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "renew.license.fail"));
		}
		return map;
	}

	/**
	 * 测试贝宝的接口回调
	 * 
	 */
	public Map<String, String> pp(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, String> map = new HashMap<>();

		map.put("code", "0");
		return map;
	}

	/********************************** 手表接入 ******************************************/
	/**
	 * 添加电话本号码（添加限定权限）
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> addphonebook(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String phone = DataUtil.getStringFromMap(params, "phone");
		String photo = ",,,,,,,,,";
		photo = DataUtil.getStringFromMap(params, "photo") != null ? DataUtil.getStringFromMap(params, "photo") : photo;
		int adminindex = 0;
		if (DataUtil.getStringFromMap(params, "adminindex") != null
				&& !DataUtil.getStringFromMap(params, "adminindex").isEmpty()) {
			adminindex = Integer.parseInt(DataUtil.getStringFromMap(params, "adminindex"));
		}

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		Map<Integer, Object> phonemap = new HashMap<Integer, Object>();
		if (phone != null && phone.indexOf(",") != -1 && phone.split("\\,", 10).length == 10) {
			String[] phonearray = phone.split("\\,", 10);
			for (int i = 0; i <= phonearray.length - 1; i++) {
				phonemap.put(i + 1, phonearray[i]);
			}
			phonemap.put(11, 0);
		} else {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.add_phone_fail"));
			return map;
		}
		// 拆分 号码判断不能空
		if (adminindex != 0 && (phonemap.get(adminindex) == null || phonemap.get(adminindex).toString().isEmpty())) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.set_adminphone_phonenull"));
			return map;
		}

		// 获取设备protocolType信息
		Map<String, Object> protocolTypeMap = daoUtil.getDeviceType(deviceSn);
		int protocolType = 0;
		if (protocolTypeMap != null) {
			protocolType = protocolTypeMap.get("protocol_type") != null
					? Integer.parseInt(protocolTypeMap.get("protocol_type").toString()) : 0;
		}

		boolean isSetGW = true;
		CastelMessage responseMsg = null;

		if (protocolType != 8) {// 判断是否调用网关 。 协议类型是891 3G手表协议 ，不调用网关指令。

			String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
			// 通知网关
			if (gatewayProtocol.contains(String.valueOf(protocolType))) {
				responseMsg = gatewayHttpHandler.setPhoneBook(deviceSn, phonemap);
			} else {
				responseMsg = gatewayHandler.set100A(deviceSn, phonemap);
			}

			if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
				isSetGW = true;// 网关调用成功
			} else {
				isSetGW = false;// 网关调用失败
			}
		}

		if (isSetGW) {// 1，协议类型是891 3G手表协议 ，不调用网关指令，直接入DB。2，其它协议需要给网关指令成功后，才入DB

			if (!daoUtil.addphonebook(trackerID, deviceSn, phone, adminindex, photo)) {
				map.put("code", "500");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.add_phone_fail"));
				return map;
			} else {
				// 保存电话本号码前三个为紧急联系人
				daoUtil.updateurgencytel(phonemap.get(1).toString(), phonemap.get(2).toString(),
						phonemap.get(3).toString(), deviceSn);
				if (adminindex != 0 && protocolType == 1) {
					Map<Integer, Object> adminindexmap = new HashMap<Integer, Object>();
					adminindexmap.put(1, adminindex);
					responseMsg = gatewayHandler.set100D(deviceSn, adminindexmap);
					if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
						map.put("code", "0");
						map.put("ret", "");
						map.put("what",
								LanguageManager.getMsg(headers.get("accept-language"), "common.add_phone_success"));
						return map;
					} else {
						map.put("code", "600");
						map.put("ret", "");
						map.put("what", LanguageManager.getMsg(headers.get("accept-language"),
								"common.setphonebooksuc_setadminphonefail"));
						return map;
					}
				} else {
					map.put("code", "0");
					map.put("ret", "");
					map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.add_phone_success"));
					return map;
				}
			}
		} else {
			map.put("code", "600");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return map;
		}

	}

	/**
	 * 添加昵称电话本号码（添加限定权限）
	 * 
	 * @param request
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public Map<String, Object> addNamePhonebook(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) throws UnsupportedEncodingException {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String phone = DataUtil.getStringFromMap(params, "phone");
		int adminindex = 0;
		String photo = ",,,,,,,,,";
		photo = DataUtil.getStringFromMap(params, "photo") != null ? DataUtil.getStringFromMap(params, "photo") : photo;

		if (DataUtil.getStringFromMap(params, "adminindex") != null
				&& !DataUtil.getStringFromMap(params, "adminindex").isEmpty()) {
			adminindex = Integer.parseInt(DataUtil.getStringFromMap(params, "adminindex"));
		}
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		Map<Integer, Object> phonemap = new HashMap<Integer, Object>();
		if (phone != null && phone.indexOf(",") != -1 && phone.split("\\,", 10).length == 10) {
			String[] phonearray = phone.split("\\,", 10);
			for (int i = 0; i <= phonearray.length - 1; i++) {
				/*
				 * String [] tmp = phonearray[i].split("\\:", 2); String num =
				 * new String(tmp[1].getBytes("UTF-8"),"US-ASCII"); num = new
				 * String(tmp[1].getBytes(),"ASCII"); //String num =
				 * EncodingUtil.getCnASCII(tmp[1]); //String name = new
				 * String(tmp[0].getBytes("UTF-8"),"UTF-16"); String name =
				 * EncodingUtil.string2Unicode(tmp[0]);
				 * 
				 * phonemap.put(i + 1, name+":"+num);
				 */
				phonemap.put(i + 1, phonearray[i]);
			}
			phonemap.put(11, 1);
		} else {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.add_phone_fail"));
			return map;
		}

		// 拆分 昵称/号码判断不能空
		String[] namePhone = phonemap.get(adminindex).toString().split("\\:", 2);
		if (adminindex != 0 && (namePhone[1] == null || namePhone[1].isEmpty())) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.set_adminphone_phonenull"));
			return map;
		}

		CastelMessage responseMsg = null;
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);

		// 通知网关
		if (gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setPhoneBook(deviceSn, phonemap);
		} else {
			responseMsg = gatewayHandler.set100A(deviceSn, phonemap);
		}

		if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
			if (!daoUtil.addphonebook(trackerID, deviceSn, phone, adminindex, photo)) {
				map.put("code", "500");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.add_phone_fail"));
				return map;
			} else {
				// 保存电话本号码前三个为紧急联系人
				daoUtil.updateurgencytel(phonemap.get(1).toString().split("\\:", 2)[1],
						phonemap.get(2).toString().split("\\:", 2)[1], phonemap.get(3).toString().split("\\:", 2)[1],
						deviceSn);
				Map<String, Object> deviceMap = daoUtil.getDeviceInfoByDevicesn(deviceSn);
				if (adminindex != 0 && deviceMap != null && deviceMap.get("protocol_type") != null
						&& deviceMap.get("protocol_type").toString().equals("1")) {
					Map<Integer, Object> adminindexmap = new HashMap<Integer, Object>();
					adminindexmap.put(1, adminindex);
					responseMsg = gatewayHandler.set100D(deviceSn, adminindexmap);
					if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
						map.put("code", "0");
						map.put("ret", "");
						map.put("what",
								LanguageManager.getMsg(headers.get("accept-language"), "common.add_phone_success"));
						return map;
					} else {
						map.put("code", "600");
						map.put("ret", "");
						map.put("what", LanguageManager.getMsg(headers.get("accept-language"),
								"common.setphonebooksuc_setadminphonefail"));
						return map;
					}
				} else {
					map.put("code", "0");
					map.put("ret", "");
					map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.add_phone_success"));
					return map;
				}
			}
		} else {
			map.put("code", "600");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return map;
		}
	}

	/**
	 * 获取电话本数据(添加访问远程逻辑)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getphonebook(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getphonebook");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getphonebook异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		Map<String, Object> list = daoUtil.getallphone(trackerID);
		if (list == null || list.isEmpty()) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.phonebook_no_data"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", list);
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.phonebook_data_success"));
		return map;
	}

	/***
	 * 保存定时开关机设置（添加限定权限）
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> savetimeboot(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String enable = DataUtil.getStringFromMap(params, "enable");
		String boottime = DataUtil.getStringFromMap(params, "boottime");
		String shutdowntime = DataUtil.getStringFromMap(params, "shutdowntime");
		String repeatday = DataUtil.getStringFromMap(params, "repeatday");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		// int timezone = daoUtil.getusertimezone(userID);
		// int tzhour = timezone/3600;

		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, enable);
		bodyVo.put(2, boottime);
		bodyVo.put(3, shutdowntime);

		CastelMessage message = gatewayHandler.set100B(deviceSn, bodyVo);
		if (message != null && Integer.parseInt(message.getMsgBodyMapVo().get(1).toString()) == 0) {
			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "updatedeviceinfo");
			param.put("deviceSn", deviceSn);
			param.put("bt_enable", enable);

			try {
				regConsumerService.hanleRegService("updatedeviceinfo", param);
			} catch (Exception e) {
				//
			}

			if (!daoUtil.savetimeboot(trackerID, deviceSn, enable, boottime, shutdowntime, repeatday)) {
				map.put("code", "500");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.add_timeboot_fail"));
				return map;
			}
		} else {
			map.put("code", "600");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.add_timeboot_success"));
		return map;
	}

	/**
	 * 获取开关机时间设定
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> gettimeboot(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "gettimeboot");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问gettimeboot异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		Map<String, Object> timebootmap = daoUtil.gettimeboot(trackerID);
		if (timebootmap == null || timebootmap.isEmpty()) {
			timebootmap = new HashMap<String, Object>();
			timebootmap.put("did", trackerID);
			timebootmap.put("device_sn", deviceSn);
			timebootmap.put("enable", 0);
			timebootmap.put("boottime", "07:30");
			timebootmap.put("shutdowntime", "22:00");
			timebootmap.put("repeatday", "");
		}

		map.put("code", "0");
		map.put("ret", timebootmap);
		map.put("what", "");
		return map;
	}

	/***
	 * 保存课程禁用设置（添加限定权限）
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> savecoursedisabletime(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String enable = DataUtil.getStringFromMap(params, "enable");
		String amstarttime = DataUtil.getStringFromMap(params, "amstarttime");
		String amendtime = DataUtil.getStringFromMap(params, "amendtime");
		String tmstarttime = DataUtil.getStringFromMap(params, "tmstarttime");
		String tmendtime = DataUtil.getStringFromMap(params, "tmendtime");
		String starttime3 = DataUtil.getStringFromMap(params, "starttime3");
		String endtime3 = DataUtil.getStringFromMap(params, "endtime3");
		String repeatday = DataUtil.getStringFromMap(params, "repeatday");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();

		// 990
		if (daoUtil.getdeviceprtocoltype(deviceSn) == 7) {
			String[] enables = enable.split(",");

			bodyVo.put(1, 1);

			if (enables[0].equals("1")) {
				bodyVo.put(2, amstarttime);
				bodyVo.put(3, amendtime);
			} else {
				bodyVo.put(2, "00:00");
				bodyVo.put(3, "00:00");
			}

			if (enables[1].equals("1")) {
				bodyVo.put(4, tmstarttime);
				bodyVo.put(5, tmendtime);
			} else {
				bodyVo.put(4, "00:00");
				bodyVo.put(5, "00:00");
			}

			if (enables[2].equals("1")) {
				bodyVo.put(7, starttime3);
				bodyVo.put(8, endtime3);
			} else {
				bodyVo.put(7, "00:00");
				bodyVo.put(8, "00:00");
			}

			bodyVo.put(6, repeatday);

		} else {
			if ((daoUtil.getdeviceprtocoltype(deviceSn) == 5 || daoUtil.getdeviceprtocoltype(deviceSn) == 6)
					&& enable.equals("0")) {
				// 上课禁用关闭
				bodyVo.put(1, enable);
				bodyVo.put(2, "");
				bodyVo.put(3, "");
				bodyVo.put(4, "");
				bodyVo.put(5, "");
				bodyVo.put(6, "");
			} else {// 其它
				bodyVo.put(1, enable);
				bodyVo.put(2, amstarttime);
				bodyVo.put(3, amendtime);
				bodyVo.put(4, tmstarttime);
				bodyVo.put(5, tmendtime);
				bodyVo.put(6, repeatday);
				bodyVo.put(7, starttime3);
				bodyVo.put(8, endtime3);
			}
		}

		// 获取设备productType信息
		Map<String, Object> productTypeMap = daoUtil.getDeviceType(deviceSn);

		int productType = 0;
		if (productTypeMap != null) {
			productType = productTypeMap.get("product_type") != null
					? Integer.parseInt(productTypeMap.get("product_type").toString()) : 0;
		}

		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setCourseDisableTime(deviceSn, bodyVo);
		} else {
			responseMsg = gatewayHandler.set100C(deviceSn, bodyVo);
		}

		if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
			Map<String, String> param = new HashMap<String, String>();
			param.put("function", "updatedeviceinfo");
			param.put("deviceSn", deviceSn);
			param.put("cdt_enable", enable);

			try {
				regConsumerService.hanleRegService("updatedeviceinfo", param);
			} catch (Exception e) {
			}

			if (!daoUtil.savecoursedisabletime(trackerID, deviceSn, enable, amstarttime, amendtime, tmstarttime,
					tmendtime, repeatday, starttime3, endtime3)) {
				map.put("code", "500");
				map.put("ret", "");

				if (productType == 23 || productType == 26) {
					map.put("what",
							LanguageManager.getMsg(headers.get("accept-language"), "common.mute_notifications_fail"));
				} else {
					map.put("what", LanguageManager.getMsg(headers.get("accept-language"),
							"common.add_coursedisabletime_fail"));
				}
				return map;
			}
		} else {
			map.put("code", "600");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", "");

		if (productType == 23 || productType == 26) {
			map.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.mute_notifications_success"));
		} else {
			map.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.add_coursedisabletime_success"));
		}

		return map;
	}

	/**
	 * 获取课程禁用设置(添加访问远程逻辑)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getcoursedisabletime(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getcoursedisabletime");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getcoursedisabletime异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		Map<String, Object> coursedisabletimemap = daoUtil.getcoursedisabletime(trackerID);
		if (coursedisabletimemap == null || coursedisabletimemap.isEmpty()) {
			coursedisabletimemap = new HashMap<String, Object>();

			if (daoUtil.getdeviceprtocoltype(deviceSn) == 7) {
				coursedisabletimemap.put("did", trackerID);
				coursedisabletimemap.put("device_sn", deviceSn);
				coursedisabletimemap.put("enable", "0,0,0");
				coursedisabletimemap.put("amstarttime", "00:00");
				coursedisabletimemap.put("amendtime", "00:00");
				coursedisabletimemap.put("tmstarttime", "00:00");
				coursedisabletimemap.put("tmendtime", "00:00");
				coursedisabletimemap.put("starttime3", "00:00");
				coursedisabletimemap.put("endtime3", "00:00");
				coursedisabletimemap.put("repeatday", "1,2,3,4,5");
			} else {
				coursedisabletimemap.put("did", trackerID);
				coursedisabletimemap.put("device_sn", deviceSn);
				coursedisabletimemap.put("enable", "0");
				coursedisabletimemap.put("amstarttime", "07:30");
				coursedisabletimemap.put("amendtime", "12:00");
				coursedisabletimemap.put("tmstarttime", "14:00");
				coursedisabletimemap.put("tmendtime", "16:00");
				coursedisabletimemap.put("repeatday", "1,2,3,4,5");
			}

		}

		map.put("code", "0");
		map.put("ret", coursedisabletimemap);
		map.put("what", "");
		return map;
	}

	/**
	 * 设防（入库）/撤防（入库、下发指令）（添加限定权限）
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> setdefensivestatus(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		int defensivestatus = DataUtil.getStringFromMap(params, "defensivestatus") == null ? 0
				: Integer.parseInt(DataUtil.getStringFromMap(params, "defensivestatus"));

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "tracker.nopms_lock_device"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		int ranges = daoUtil.getTypeOfTracker(trackerID);
		if (ranges <= 2) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.non_motor_vehicle"));
			return map;
		}

		// 设防
		if (defensivestatus == 1) {
			if (daoUtil.lockVehicle(trackerID, 1)) {
				map.put("code", "0");
				map.put("ret", "");
				map.put("what",
						LanguageManager.getMsg(headers.get("accept-language"), "common.set_defensivestatus_success"));
				return map;
			} else {
				map.put("code", "600");
				map.put("ret", "");
				map.put("what",
						LanguageManager.getMsg(headers.get("accept-language"), "common.set_defensivestatus_failed"));
				return map;
			}
		} else // 撤防
		{
			// 通知网关
			CastelMessage responseMsg = gatewayHandler.set1007(deviceSn, 0);
			if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
				if (daoUtil.lockVehicle(trackerID, 0)) {
					map.put("code", "0");
					map.put("ret", "");
					map.put("what", LanguageManager.getMsg(headers.get("accept-language"),
							"common.set_undefensivestatus_success"));
					return map;
				}
			}

			map.put("code", "600");
			map.put("ret", null);
			map.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.set_undefensivestatus_failed"));
			return map;
		}
	}

	/**
	 * 获取设防状态(添加访问远程逻辑)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getdefensivestatus(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getdefensivestatus");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getdefensivestatus异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		int ranges = daoUtil.getTypeOfTracker(trackerID);
		if (ranges <= 2) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.non_motor_vehicle"));
			return map;
		}

		Map<String, Object> returnmap = daoUtil.getdefensivestatus(trackerID);
		if (returnmap == null) {
			returnmap = new HashMap<String, Object>();
			returnmap.put("defensive", 0);
		}

		map.put("code", "0");
		map.put("ret", returnmap);
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.get_defensivestatus_success"));
		return map;
	}

	/**
	 * 下发设防指令（添加限定权限）
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> senddefensiveorder(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "tracker.nopms_lock_device"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		int ranges = daoUtil.getTypeOfTracker(trackerID);
		if (ranges <= 2) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.non_motor_vehicle"));
			return map;
		}

		// 通知网关
		CastelMessage responseMsg = gatewayHandler.set1007(deviceSn, 1);
		if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
			/*
			 * if(daoUtil.lockVehicle(trackerID, 0)) { map.put("code", "0");
			 * map.put("ret", ""); map.put("what",
			 * LanguageManager.getMsg(headers.get("accept-language"),
			 * "common.send_order_success")); return map; }
			 */
			map.put("code", "0");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_success"));
			return map;
		}

		map.put("code", "600");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
		return map;
	}

	/**
	 * 设置休眠时间（添加限定权限）
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> setSleepInfo(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String enable = DataUtil.getStringFromMap(params, "enable");
		String boottime = DataUtil.getStringFromMap(params, "boottime");
		String shutdowntime = DataUtil.getStringFromMap(params, "shutdowntime");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "tracker.nopms_lock_device"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		int timezone = daoUtil.getUserTimezone(userID);

		String fullboottime = DateTimeUtil.getfulldatetime(boottime);
		String fullshutdowntime = DateTimeUtil.getfulldatetime(shutdowntime);

		try {
			String fullboottimeUTC = DateTimeUtil.local2utc(fullboottime, Integer.toString(timezone));
			String fullshutdowntimeUTC = DateTimeUtil.local2utc(fullshutdowntime, Integer.toString(timezone));

			String boottimeUTC = fullboottimeUTC.substring(11, 16);
			String shutdowntimeUTC = fullshutdowntimeUTC.substring(11, 16);

			// 构建消息体
			Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
			bodyVo.put(1, enable);
			bodyVo.put(2, boottimeUTC);
			bodyVo.put(3, shutdowntimeUTC);

			// 通知网关
			CastelMessage responseMsg = gatewayHandler.set100F(deviceSn, bodyVo);
			if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
				if (!daoUtil.setSleepInfo(enable, boottime, shutdowntime, deviceSn)) {
					map.put("code", "600");
					map.put("ret", "");
					map.put("what",
							LanguageManager.getMsg(headers.get("accept-language"), "common.set_sleepinfo_fail"));
					return map;
				}
			} else {
				map.put("code", "600");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.set_sleepinfo_fail"));
				return map;
			}

			String whatStr = "";
			if (Integer.parseInt(enable) == 0) {
				whatStr = LanguageManager.getMsg(headers.get("accept-language"), "common.close_sleep_model");
			} else {
				whatStr = LanguageManager.getMsg(headers.get("accept-language"), "common.set_sleepinfo_success");
			}

			map.put("code", "0");
			map.put("ret", daoUtil.getTsleepinfo(deviceSn));
			map.put("what", whatStr);
			return map;
		} catch (ParseException e) {
			e.printStackTrace();

			map.put("code", "600");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.set_sleepinfo_fail"));
			return map;
		}
	}

	/**
	 * 获取休眠设置(添加访问远程逻辑)
	 */
	public Map<String, Object> getSleepInfo(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getSleepInfo");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getSleepInfo异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		Map<String, Object> resultMap = daoUtil.getTsleepinfo(deviceSn);
		if (resultMap == null || resultMap.isEmpty()) {
			resultMap = new HashMap<String, Object>();
			resultMap.put("enable", "0");
			resultMap.put("boottime", "22:00");
			resultMap.put("shutdowntime", "07:30");
		}

		map.put("code", "0");
		map.put("ret", resultMap);
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.get_sleepinfo_success"));
		return map;
	}

	/* 以下跨服务器授权方法******************************************* */

	/**
	 * 验证是否设备超级用户
	 * 
	 * @param username
	 * @param deviceSn
	 * @return
	 * @throws Exception
	 */
	public boolean issuperuser(String username, String deviceSn) throws Exception {
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "issuperuser");
		param.put("username", username);
		param.put("deviceSn", deviceSn);

		RPCResult rpcResult = regConsumerService.hanleRegService("issuperuser", param);

		if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS
				&& JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
			return Boolean.parseBoolean(JSON.parseObject(rpcResult.getRpcResult().toString()).get("data").toString());
		} else {
			return false;
		}
	}

	/**
	 * 获取设备服务器和超级用户信息
	 * 
	 * @param deviceSn
	 * @return
	 * @throws Exception
	 */
	public Object getdeviceserverandusername(String deviceSn) throws Exception {
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "getdeviceserverandusername");
		param.put("deviceSn", deviceSn);

		RPCResult rpcResult = regConsumerService.hanleRegService("getdeviceserverandusername", param);
		if (null != rpcResult.getRpcResult()) {
			return JSON.parseObject(rpcResult.getRpcResult().toString()).getJSONObject("data");
		} else {
			return null;
		}
	}

	/**
	 * 获取授权用户相关信息
	 * 
	 * @param deviceSn
	 * @param username
	 * @return
	 * @throws Exception
	 */
	public Object getauthorizationuserinfo(String deviceSn, String username) throws Exception {
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "getauthorizationuserinfo");
		param.put("deviceSn", deviceSn);
		param.put("username", username);

		RPCResult rpcResult = regConsumerService.hanleRegService("getauthorizationuserinfo", param);

		if (null != rpcResult.getRpcResult()) {
			return JSON.parseObject(rpcResult.getRpcResult().toString()).getJSONObject("data");
		} else {
			return null;
		}
	}

	/**
	 * 验证设备数据是否在当前服务器上
	 * 
	 * @param deviceSn
	 * @return
	 * @throws Exception
	 */
	public boolean islocalserver(String deviceSn) throws Exception {
		Object ret = getdeviceserverandusername(deviceSn);
		if (ret != null) {
			JSONObject obj = (JSONObject) ret;

			String localServerIpAddress = env.getProperty("localServerIpAddress");
			String deviceServerIpAddress = obj.getString("conn_name");
			if (localServerIpAddress.equals(deviceServerIpAddress)) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * 获取设备超级用户
	 * 
	 * @param deviceSn
	 * @return
	 * @throws Exception
	 */
	public String getdevicesuperuser(String deviceSn) throws Exception {
		Object ret = getdeviceserverandusername(deviceSn);
		if (ret != null) {
			JSONObject obj = (JSONObject) ret;
			String deviceuser = obj.getString("name");
			return deviceuser;
		} else {
			return "";
		}
	}

	// ########################################################
	// 登录 以下方法为航通守护者3.0专用版
	public Map<String, Object> userLoginCN(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {

		String language = headers.get("accept-language");
		String sessionID = headers.get("sessionID");

		String username = DataUtil.getStringFromMap(params, "username");
		String password = DataUtil.getStringFromMap(params, "password");
		if (username != null && username != "" && !username.isEmpty()) {
			username = username.trim().toLowerCase();
		}

		int isCustomizedApp = (DataUtil.getStringFromMap(params, "isCustomizedApp") == null
				|| DataUtil.getStringFromMap(params, "isCustomizedApp").isEmpty()) ? 0
						: Integer.parseInt(DataUtil.getStringFromMap(params, "isCustomizedApp"));

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		// 邮箱验证
		if (username != null && !username.equals("") && username.contains("@") && !Verifier.validateEmail(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.error_email"));
			return mapResponse;
		}

		// 手机号码验证
		if (username != null && !username.equals("") && !username.contains("@") && !Verifier.validateMobile(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "commom.error_mobile"));
			return mapResponse;
		}

		String currentUTCtime = DateTimeUtil.getCurrentUtcDatetime();
		Map<String, Object> lrMap = daoUtil.getLoginError(username);
		Integer lr_count = 0;
		if (null != lrMap) {
			lr_count = Integer.parseInt(lrMap.get("login_err_count").toString());
			String lr_time;

			if (lrMap.get("login_limit_time") != null) {
				lr_time = lrMap.get("login_limit_time").toString().substring(0, 19);
			} else {
				lr_time = currentUTCtime;
			}

			if (DateTimeUtil.compareDateTime(currentUTCtime, lr_time) >= 0) {
				lr_count = 0;
			}

			if (lr_count > loginTryTimes - 1) {
				mapResponse.put(ReturnObject.RETURN_CODE, "400");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "commom.login_error_limit"));
				return mapResponse;
			}
		}
		// 验证用户名和密码
		int isEmailVerify = daoUtil.validateUserPasswordAndEmailCN(username, MD5.toMD5(password));

		if (isEmailVerify == -1) {
			if (null != lrMap) {
				lr_count = lr_count + 1;
				daoUtil.updateLoginError(username, lr_count);
			}
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.login_error"));
			return mapResponse;
		}

		lr_count = 0;
		daoUtil.updateLoginError(username, lr_count);

		// 更新用户登录时间
		daoUtil.updateLoginTimeCN(username);

		// 获取设备producttype，productname信息
		List<Map<String, Object>> productInfoList = daoUtil.getProductInfo(language);
		// 更新注册服务器isCustomizedApp信息
		Map<String, String> updateCustomizedParam = new HashMap<String, String>();
		updateCustomizedParam.put("function", "updateIsCustomizedApp");
		updateCustomizedParam.put("isCustomizedApp", String.valueOf(isCustomizedApp));
		updateCustomizedParam.put("username", username);
		try {
			regConsumerService.hanleRegService("updateIsCustomizedApp", updateCustomizedParam);
		} catch (Exception e2) {
			e2.printStackTrace();
		}
		JSONArray obj = null;
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "getDeviceInfoCN");
		param.put("username", username);// 手机号码
		try {
			RPCResult rpcResult = regConsumerService.hanleRegService("getDeviceInfoCN", param);
			if (null != rpcResult.getRpcResult()
					&& null != JSON.parseObject(rpcResult.getRpcResult().toString()).get("data")) {
				obj = new JSONArray();

				JSONObject json = JSON.parseObject(rpcResult.getRpcResult().toString());
				JSONArray objArray = json.getJSONArray("data");

				for (Object curobj : objArray) {
					if (curobj != null) {
						JSONObject curJsonObj = (JSONObject) curobj;
						String product_type = curJsonObj.get("product_type") != null
								? curJsonObj.get("product_type").toString() : "";
						String aroundRanges = curJsonObj.get("around_ranges") != null
								? curJsonObj.get("around_ranges").toString() : "0";
						curJsonObj.put("around_ranges", aroundRanges);

						if (curJsonObj.containsKey("nickname") && curJsonObj.get("nickname") != null) {
							curJsonObj.put("nickname",
									URLDecoder.decode(curJsonObj.get("nickname").toString(), "UTF-8"));
						} else {
							// 添加设备时给设备设置默认名称
							if (productInfoList != null && productInfoList.size() > 0) {
								for (Map<String, Object> product : productInfoList) {
									String typeValue = product.get("value") != null ? product.get("value").toString()
											: "";
									String typeName = product.get("name") != null ? product.get("name").toString() : "";
									if (product_type.equals(typeValue)) {
										curJsonObj.put("nickname", typeName);
										break;
									}
								}
							}
						}
						obj.add(curJsonObj);
					}
				}
			}

			if (obj == null) {
				obj = new JSONArray();
			}
		} catch (Exception e) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_not_exist_or_timeout"));
			return mapResponse;
		}

		// 保存用户名到session
		Session.setUser(sessionID, username);

		// 更新用户isCustomizedApp
		daoUtil.updateIsCustomizedApp(username, isCustomizedApp);

		Map<String, String> paramTokenMap = new HashMap<String, String>();
		paramTokenMap.put("function", "saveUserToken");
		paramTokenMap.put("username", username);
		paramTokenMap.put("token", "");
		paramTokenMap.put("versions", "3");// 3：表示航通守护者3.0版本

		try {
			regConsumerService.hanleRegService("saveUserToken", paramTokenMap);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		daoUtil.saveUserToken("", username, 3);

		// 查询用户ChatToken
		String chatToken = "";
		Map<String, Object> mapChatToken = daoUtil.getChatToken(username);
		if (mapChatToken != null) {

			chatToken = (String) mapChatToken.get("token");

		} else {

			try {
				TokenResult userGetTokenResult = RongCloud.getInstance(appKey, appSecret).user.getToken(username,
						username, "http://www.RongCloud.getInstance(appKey, appSecret).cn/images/logo.png");
				// System.out.println("getToken: " +
				// userGetTokenResult.toString());
				chatToken = userGetTokenResult.getToken();
				if (daoUtil.saveChatToken(username, chatToken)) {
					LogManager.info("Token save OK:" + username);
				} else {
					LogManager.info("Token save error:" + username);
				}

			} catch (Exception e) {
				LogManager.error("username:" + username + " error:" + e.getMessage());
			}
		}

		int adVersion = isCustomizedApp;
		if (adVersion != 4) {
			adVersion = 0;
		}

		// 返回登录数据
		Map<String, Object> mapRet = new HashMap<String, Object>();
		mapRet.put("advertising", daoUtil.getAdvertising(adVersion, username));

		Map<String, Object> mapUserStatus = daoUtil.getUserStatusForApp(username);
		mapRet.put("alert_mode", mapUserStatus.get("alert_mode"));
		mapRet.put("is_email_verify", mapUserStatus.get("is_email_verify"));
		mapRet.put("timezone_id", mapUserStatus.get("timezone_id"));
		mapRet.put("timezone_check", mapUserStatus.get("timezone_check"));
		mapRet.put("device_list", obj);
		mapRet.put("chat_token", chatToken);
		mapRet.put("chatProductType", env.getProperty("chat.product.type"));
		mapRet.put("portrait", mapUserStatus.get("portrait"));
		mapRet.put("nickname", mapUserStatus.get("nickname"));

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
		mapResponse.put(ReturnObject.RETURN_WHAT, "");

		return mapResponse;

	}

	/**
	 * 注册(针对当前用户，无需处理,只需将数据同步到注册服务器)
	 * 
	 * @throws Exception
	 */
	public Map<String, Object> registerCN(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody)
			throws Exception {
		String username = DataUtil.getStringFromMap(params, "username");
		if (!TextUtils.isEmpty(username)) {
			username = username.trim().toLowerCase();
		}

		String password = DataUtil.getStringFromMap(params, "password");
		String serverNo = DataUtil.getStringFromMap(params, "serverNo");
		String timezone = DataUtil.getStringFromMap(params, "timezone");
		String isCustomizedApp = DataUtil.getStringFromMap(params, "isCustomizedApp");

		if (TextUtils.isEmpty(isCustomizedApp)) {
			isCustomizedApp = "0";
		}

		String language = headers.get("accept-language");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		Integer timezoneInt = PropertyUtil.getTimezoneBylocalID(env.getProperty("timezone" + timezone));

		// 用户名不是邮箱则返回
		if (!Verifier.validateEmail(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "50");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "commom.error_email"));
			return mapResponse;
		}

		// 验证用户名是否已存在
		if (daoUtil.isExistsUserCN(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "forlogin.user_exist"));
			return mapResponse;
		}

		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "registerCN");
		param.put("username", username);
		param.put("serverNo", serverNo);
		param.put("timezone", Integer.toString(timezoneInt));
		param.put("timezoneid", timezone);
		param.put("isCustomizedApp", isCustomizedApp);

		RPCResult rpcResult = regConsumerService.hanleRegService("registerCN", param);
		if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
				|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
			if (null != rpcResult.getRpcResult()
					&& JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("100")) {
				mapResponse.put(ReturnObject.RETURN_CODE, "100");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "forlogin.user_exist"));
				return mapResponse;
			} else {
				mapResponse.put(ReturnObject.RETURN_CODE, "200");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "forlogin.save_user_failed"));
				return mapResponse;
			}
		}

		if (Verifier.validateEmail(username)) {
			// 注册用户 邮箱
			if (!daoUtil.registerUser(username, MD5.toMD5(password), Integer.parseInt(serverNo), timezoneInt, timezone,
					Integer.parseInt(isCustomizedApp))) {
				mapResponse.put(ReturnObject.RETURN_CODE, "200");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "forlogin.save_user_failed"));
				return mapResponse;
			}
			// 发送注册邮件
			if (!emailUtil.sendRegisterEmail(username, language)) {
				mapResponse.put(ReturnObject.RETURN_CODE, "250");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "forlogin.send_registeremail_failed"));
				return mapResponse;
			}
		} else {
			// 注册用户 手机
			if (!daoUtil.registerUserCN(username, MD5.toMD5(password), Integer.parseInt(serverNo), timezoneInt,
					timezone, Integer.parseInt(isCustomizedApp))) {
				mapResponse.put(ReturnObject.RETURN_CODE, "200");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "forlogin.save_user_failed"));
				return mapResponse;
			}
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "forlogin.phone_register_success"));
		return mapResponse;

	}

	public Map<String, Object> forgetPasswordCN(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) throws Exception {
		String username = DataUtil.getStringFromMap(params, "username");
		String password = DataUtil.getStringFromMap(params, "password");
		String language = headers.get("accept-language");
		Map<String, Object> map = new HashMap<String, Object>();
		if (username != null && username != "" && !username.isEmpty()) {
			// 手机用户找回密码(忘记密码)
			username = username.trim();

			Map<String, Object> mapResponse = new HashMap<>();
			// 用户不存在
			int userID = daoUtil.getUserIdCN(username);
			if (userID <= 0) {
				mapResponse.put(ReturnObject.RETURN_CODE, "100");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
				return mapResponse;
			}
			// 邮箱注册用户
			if (username != null && username.contains("@") && Verifier.validateEmail(username)) {
				// 发送找回密码邮件
				String pwd_verify_code = RandomUtil.getRandomString(10);
				if (!emailUtil.sendForgetPasswordEmail(username, pwd_verify_code, language)
						|| !daoUtil.updateUserForFindPassWord(username, 0, pwd_verify_code)) {
					mapResponse.put(ReturnObject.RETURN_CODE, "300");
					mapResponse.put(ReturnObject.RETURN_OBJECT, null);
					mapResponse.put(ReturnObject.RETURN_WHAT,
							LanguageManager.getMsg(headers.get("accept-language"), "email.send_email_failed_sys"));
					return mapResponse;
				}
				mapResponse.put(ReturnObject.RETURN_CODE, "0");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "email.note_check_email"));
				return mapResponse;
			}
			// 更新密码 手机注册用户
			if (!daoUtil.updateUserPasswordCN(username, password)) {
				mapResponse.put(ReturnObject.RETURN_CODE, "100");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.save_password_failed"));
			}

			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.change_password_success"));
			return mapResponse;

		}

		map.put("code", "300");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.save_password_failed"));
		return map;
	}

	// 删除报警信息
	public Map<String, Object> deleteAlarmInfo(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<String, Object>();

		String username = DataUtil.getStringFromMap(params, "username");
		String alarmID = DataUtil.getStringFromMap(params, "alarmIDS");
		if (alarmID.contains(",")) {
			alarmID = alarmID.replace(",", "','");
		}
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (remoteServerRequest != "" && remoteServerRequest != null) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		if (!PropertyUtil.isNotBlank(alarmID)) {
			map.put(ReturnObject.RETURN_CODE, "300");
			map.put(ReturnObject.RETURN_OBJECT, null);
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "deletealarm.failed"));
			return map;
		}

		// 判断是否是超级用户
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put(ReturnObject.RETURN_CODE, "400");
			map.put(ReturnObject.RETURN_OBJECT, "");
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "deleteAlarmInfo");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("alarmIDS", alarmID);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问deleteAlarmInfo异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}

		// 删除警情数据
		if (daoUtil.deleteAlarmInfo(alarmID)) {
			map.put(ReturnObject.RETURN_CODE, "0");
			map.put(ReturnObject.RETURN_OBJECT, "");
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "deletealarm.success"));
			return map;
		}
		map.put(ReturnObject.RETURN_CODE, "400");
		map.put(ReturnObject.RETURN_OBJECT, "");
		map.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(headers.get("accept-language"), "deletealarm.failed"));
		return map;

	}

	/**
	 * 设置电子围栏（添加限定权限）
	 */
	public Map<String, Object> setGeoFenceCN(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String lat = DataUtil.getStringFromMap(params, "lat");
		String lng = DataUtil.getStringFromMap(params, "lng");
		String radius = DataUtil.getStringFromMap(params, "radius");
		String areaid = DataUtil.getStringFromMap(params, "areaid");
		String defencename = DataUtil.getStringFromMap(params, "defencename");
		String defencestatus = DataUtil.getStringFromMap(params, "defencestatus");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");
		Map<String, Object> mapResponse = new HashMap<String, Object>();

		// 查找用户名失败
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		// if (daoUtil.getdeviceprtocoltype(deviceSn) == 0) {

		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setEnclosure(deviceSn, lat, lng, radius);
		} else {
			responseMsg = gatewayHandler.set1001(deviceSn, lat, lng, Integer.parseInt(radius));
		}

		// int
		// result=Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString());
		if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
			int userID = daoUtil.getUserId(username);
			if (setGeoFenceCN(areaid, deviceSn, String.valueOf(userID), lat, lng, radius, defencename, defencestatus)) {
				mapResponse.put(ReturnObject.RETURN_CODE, "0");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "fence.set_fence_success"));
				return mapResponse;
			}
		} else {
			mapResponse.put(ReturnObject.RETURN_CODE, "600");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "400");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "fence.save_fence_failed"));
		return mapResponse;
	}

	/**
	 * 获取电子围栏(添加访问远程逻辑)
	 * 
	 * @param deviceSn
	 * @return
	 */
	public Map<String, Object> getGeoFenceCN(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		// 查找用户名失败
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getGeoFenceCN");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getGeoFence异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		int deviceID = daoUtil.getDeviceID(deviceSn);
		Map<String, Object> mapRet = new HashMap<String, Object>();
		List<Map<String, Object>> defenceList = daoUtil.getGeoFenceCN(String.valueOf(deviceID));

		if (defenceList == null) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "fence.no_fence_data"));
			return mapResponse;
		}
		mapRet.put("defenceList", defenceList);
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
		// mapResponse.put("defenceList", defenceList);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;

	}

	/**
	 * 删除电子围栏（添加限定权限）
	 * 
	 * @param deviceSn
	 * @return
	 */
	public Map<String, Object> deleteGeoFenceCN(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String areaid = DataUtil.getStringFromMap(params, "areaid");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		// 查找用户名失败
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		// if (daoUtil.getdeviceprtocoltype(deviceSn) == 0) {
		// 通知网关
		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.deleteEnclosure(deviceSn);
		} else {
			responseMsg = gatewayHandler.set1002(deviceSn);
		}
		// int
		// result=Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString());
		if (responseMsg != null && Integer.parseInt(responseMsg.getMsgBodyMapVo().get(1).toString()) == 0) {
			int deviceID = daoUtil.getDeviceID(deviceSn);
			if (!daoUtil.deleteGeoFenceCN(String.valueOf(deviceID), areaid)) {
				mapResponse.put(ReturnObject.RETURN_CODE, "500");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "fence.cancel_fence_failed"));
				return mapResponse;
			}
		} else {
			mapResponse.put(ReturnObject.RETURN_CODE, "600");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "fence.save_fence_failed"));
			return mapResponse;
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "fence.cancel_fence_success"));
		return mapResponse;
	}

	/**
	 * 获取经济驾驶数据
	 */
	public Map<String, Object> getEconomicalDriveData(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String starttime = DataUtil.getStringFromMap(params, "startTime");
		String endtime = DataUtil.getStringFromMap(params, "endTime");

		Map<String, Object> mapResponse = new HashMap<>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getEconomicalDriveData");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("startTime", starttime);
					param.put("endTime", endtime);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getEconomicalDriveData异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 未绑定此设备
		if (!daoUtil.hasOperatingAuthority(deviceID, userId)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}

		// 时间转换
		Date startDate = UtilDate.getDate(starttime, UtilDate.simple);
		Date endDate = UtilDate.getDate(endtime, UtilDate.simple);
		int timezone = daoUtil.getUserTimezone(userId);

		String date1 = UtilDate.getUTCDateString(startDate, timezone);
		String date2 = UtilDate.getUTCDateString(endDate, timezone);

		// 精确查询某段行程的安全驾驶数据
		Map<String, Object> economicalDriveData = new HashMap<String, Object>();
		;
		Map<String, Integer> dsDayAlarmData = null;
		Map<String, Integer> dsDayAlarmTimeLongData = null;
		TdriverBehaviorAnalysis tdriverBehaviorAnalysis = null;
		// 默认查询最近一次行程的安全驾驶数据信息
		if (startDate == null || startDate.equals("")) {
			// 查询最近一次行程数据
			Map<String, Object> tripDataMap = daoUtil.getRecentlyTripData(deviceSn);
			if (tripDataMap != null && tripDataMap.size() > 0) {
				String tripStarttime = tripDataMap.get("start_time") == null ? ""
						: tripDataMap.get("start_time").toString();
				String tripEndtime = tripDataMap.get("end_time") == null ? "" : tripDataMap.get("end_time").toString();
				if (PropertyUtil.isNotBlank(tripStarttime)) {
					tripStarttime = tripStarttime.substring(0, 19);
				}
				if (PropertyUtil.isNotBlank(tripEndtime)) {
					tripEndtime = tripEndtime.substring(0, 19);
				}
				dsDayAlarmData = daoUtil.getAlarmCount(tripStarttime, tripEndtime, deviceSn);
				dsDayAlarmTimeLongData = daoUtil.getAlarmTimeLong(tripStarttime, tripEndtime, deviceSn);

				try {
					tripEndtime = DateTimeUtil.utc2Local(tripEndtime, timezone);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				economicalDriveData.put("tripEndtime", tripEndtime);

				// 获取这段时间的安全驾驶、经济驾驶数据
				tdriverBehaviorAnalysis = DriveBehaviorHandler.getDriverDataByDate(dsDayAlarmData,
						dsDayAlarmTimeLongData);

			}
		} else {
			dsDayAlarmData = daoUtil.getAlarmCount(date1, date2, deviceSn);
			dsDayAlarmTimeLongData = daoUtil.getAlarmTimeLong(date1, date2, deviceSn);
			// 获取这段时间的安全驾驶、经济驾驶数据
			tdriverBehaviorAnalysis = DriveBehaviorHandler.getDriverDataByDate(dsDayAlarmData, dsDayAlarmTimeLongData);
		}
		if (tdriverBehaviorAnalysis != null) {

			economicalDriveData.put("p2", tdriverBehaviorAnalysis.getP2());// p2
																			// 超速报警
																			// (次)
																			// (安全驾驶)
																			// (经济驾驶)
			economicalDriveData.put("p4", tdriverBehaviorAnalysis.getP4());// p4急加速报警(次)(安全驾驶)(经济驾驶)
			economicalDriveData.put("p5", tdriverBehaviorAnalysis.getP5());// p5急减速报警(次)(安全驾驶)(经济驾驶)
			economicalDriveData.put("p6", tdriverBehaviorAnalysis.getP6());// p6停车未熄火报警(次)(经济驾驶)
			economicalDriveData.put("p8", tdriverBehaviorAnalysis.getP8());// p8转速过高报警(次)(安全驾驶)(经济驾驶)
			economicalDriveData.put("p9", tdriverBehaviorAnalysis.getP9());// p9转速超标时长(秒)(安全驾驶)(经济驾驶)
			economicalDriveData.put("p10", tdriverBehaviorAnalysis.getP10());// p10停车未熄火告警(秒)(经济驾驶)
			economicalDriveData.put("p11", tdriverBehaviorAnalysis.getP11());// p11超速时长(秒)(安全驾驶)(经济驾驶)
			economicalDriveData.put("p12", tdriverBehaviorAnalysis.getP12());// p12疲劳驾驶次数(次)(安全驾驶)
			economicalDriveData.put("p13", tdriverBehaviorAnalysis.getP13());// p13疲劳驾驶时长(秒)(安全驾驶)
			economicalDriveData.put("p14", tdriverBehaviorAnalysis.getP14());// 14急转弯(次)(安全驾驶)(经济驾驶)
			economicalDriveData.put("p15", tdriverBehaviorAnalysis.getP15());// 15急变道(次)(安全驾驶)(经济驾驶)
			economicalDriveData.put("economicscore", tdriverBehaviorAnalysis.getEconomicscore());// 分数
			economicalDriveData.put("safescore", tdriverBehaviorAnalysis.getSafescore());// 分数

		} else {
			economicalDriveData.put("p2", 0);// p2 超速报警 (次)
			economicalDriveData.put("p4", 0);// p4 急加速报警 (次)
			economicalDriveData.put("p5", 0);// p5急减速报警(次)
			economicalDriveData.put("p6", 0);// p6停车未熄火报警(次)(经济驾驶)
			economicalDriveData.put("p8", 0);// p8转速过高报警(次)(安全驾驶)(经济驾驶)
			economicalDriveData.put("p9", 0);// p9转速超标时长(秒)(安全驾驶)(经济驾驶)
			economicalDriveData.put("p10", 0);// p10停车未熄火告警(秒)(经济驾驶)
			economicalDriveData.put("p11", 0);// p11超速时长(秒)(安全驾驶)(经济驾驶)
			economicalDriveData.put("p12", 0);// p12疲劳驾驶次数(次)(安全驾驶)
			economicalDriveData.put("p13", 0);// p13疲劳驾驶时长(秒)(安全驾驶)
			economicalDriveData.put("p14", 0);// 14急转弯(次)(安全驾驶)(经济驾驶)
			economicalDriveData.put("p15", 0);// 15急变道(次)(安全驾驶)(经济驾驶)
			economicalDriveData.put("economicscore", 100);// 分数
			economicalDriveData.put("safescore", 100);// 分数
		}

		Map<String, Object> mapRet = new HashMap<>();
		mapRet.put("deviceSn", deviceSn);
		mapRet.put("economicalDriveData", economicalDriveData);
		mapResponse.put("code", "0");
		mapResponse.put("ret", mapRet);
		mapResponse.put("what", null);

		return mapResponse;
	}

	/**
	 * 获取安全驾驶数据
	 */
	public Map<String, Object> getSafeDriveData(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String starttime = DataUtil.getStringFromMap(params, "startTime");
		String endtime = DataUtil.getStringFromMap(params, "endTime");

		Map<String, Object> mapResponse = new HashMap<>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getSafeDriveData");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("startTime", starttime);
					param.put("endTime", endtime);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getSafeDriveData异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 未绑定此设备
		if (!daoUtil.hasOperatingAuthority(deviceID, userId)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}

		// 时间转换
		Date startDate = UtilDate.getDate(starttime, UtilDate.simple);
		Date endDate = UtilDate.getDate(endtime, UtilDate.simple);
		int timezone = daoUtil.getUserTimezone(userId);

		String date1 = UtilDate.getUTCDateString(startDate, timezone);
		String date2 = UtilDate.getUTCDateString(endDate, timezone);
		// 精确查询某段行程的安全驾驶数据
		Map<String, Object> safeDriveData = new HashMap<String, Object>();
		Map<String, Integer> dsDayAlarmData = null;
		Map<String, Integer> dsDayAlarmTimeLongData = null;
		TdriverBehaviorAnalysis tdriverBehaviorAnalysis = null;
		// 默认查询最近一次行程的安全驾驶数据信息
		if (startDate == null || startDate.equals("")) {
			// 查询最近一次行程数据
			Map<String, Object> tripDataMap = daoUtil.getRecentlyTripData(deviceSn);
			if (tripDataMap != null && tripDataMap.size() > 0) {
				String tripStarttime = tripDataMap.get("start_time") == null ? ""
						: tripDataMap.get("start_time").toString();
				String tripEndtime = tripDataMap.get("end_time") == null ? "" : tripDataMap.get("end_time").toString();
				if (PropertyUtil.isNotBlank(tripStarttime)) {
					tripStarttime = tripStarttime.substring(0, 19);
				}
				if (PropertyUtil.isNotBlank(tripEndtime)) {
					tripEndtime = tripEndtime.substring(0, 19);
				}
				dsDayAlarmData = daoUtil.getAlarmCount(tripStarttime, tripEndtime, deviceSn);
				dsDayAlarmTimeLongData = daoUtil.getAlarmTimeLong(tripStarttime, tripEndtime, deviceSn);

				try {
					tripEndtime = DateTimeUtil.utc2Local(tripEndtime, timezone);
				} catch (ParseException e) {
					e.printStackTrace();
				}
				safeDriveData.put("tripEndtime", tripEndtime);

				// 获取这段时间的安全驾驶、经济驾驶数据
				tdriverBehaviorAnalysis = DriveBehaviorHandler.getDriverDataByDate(dsDayAlarmData,
						dsDayAlarmTimeLongData);

			}
		} else {
			dsDayAlarmData = daoUtil.getAlarmCount(date1, date2, deviceSn);
			dsDayAlarmTimeLongData = daoUtil.getAlarmTimeLong(date1, date2, deviceSn);
			// 获取这段时间的安全驾驶、经济驾驶数据
			tdriverBehaviorAnalysis = DriveBehaviorHandler.getDriverDataByDate(dsDayAlarmData, dsDayAlarmTimeLongData);
		}
		if (tdriverBehaviorAnalysis != null) {
			safeDriveData.put("p2", tdriverBehaviorAnalysis.getP2());// p2
																		// 超速报警(次)(安全驾驶)(经济驾驶)
			safeDriveData.put("p4", tdriverBehaviorAnalysis.getP4());// p4
																		// 急加速报警(次)(安全驾驶)(经济驾驶)
			safeDriveData.put("p5", tdriverBehaviorAnalysis.getP5());// p5
																		// 急减速报警(次)(安全驾驶)(经济驾驶)
			safeDriveData.put("p6", tdriverBehaviorAnalysis.getP6());// p6停车未熄火报警(次)(经济驾驶)
			safeDriveData.put("p8", tdriverBehaviorAnalysis.getP8());// p8转速过高报警(次)(安全驾驶)(经济驾驶)
			safeDriveData.put("p9", tdriverBehaviorAnalysis.getP9());// p9
																		// 转速超标时长
																		// (秒)
																		// (安全驾驶)
																		// (经济驾驶)
			safeDriveData.put("p10", tdriverBehaviorAnalysis.getP10());// p10停车未熄火告警
																		// (秒)
																		// (经济驾驶)
			safeDriveData.put("p11", tdriverBehaviorAnalysis.getP11());// p11超速时长
																		// (秒)
																		// (安全驾驶)(经济驾驶)
			safeDriveData.put("p12", tdriverBehaviorAnalysis.getP12());// p12
																		// 疲劳驾驶次数
																		// (次)
																		// (安全驾驶)
			safeDriveData.put("p13", tdriverBehaviorAnalysis.getP13());// p13
																		// 疲劳驾驶时长
																		// (秒)
																		// (安全驾驶)
			safeDriveData.put("p14", tdriverBehaviorAnalysis.getP14());// 14 急转弯
																		// (次)
																		// (安全驾驶)
																		// (经济驾驶)
			safeDriveData.put("p15", tdriverBehaviorAnalysis.getP15());// 15 急变道
																		// (次)
																		// (安全驾驶)
																		// (经济驾驶)
			safeDriveData.put("safescore", tdriverBehaviorAnalysis.getSafescore());// 分数
			safeDriveData.put("economicscore", tdriverBehaviorAnalysis.getEconomicscore());// 分数

		} else {
			safeDriveData.put("p2", 0);// p2 超速报警 (次) (安全驾驶) (经济驾驶)
			safeDriveData.put("p4", 0);// p4 急加速报警// (次)// (安全驾驶)/ (经济驾驶)
			safeDriveData.put("p5", 0);// p5 急减速报警 (次) (安全驾驶) (经济驾驶)
			safeDriveData.put("p6", 0);// p6 停车未熄火报警 (次)(经济驾驶)
			safeDriveData.put("p8", 0);// p8转速过高报警 (次) (安全驾驶) (经济驾驶)
			safeDriveData.put("p9", 0);// p9 转速超标时长 (秒) (安全驾驶) (经济驾驶)
			safeDriveData.put("p10", 0);// p10 停车未熄火告警 (秒) (经济驾驶)
			safeDriveData.put("p11", 0);// p11 超速时长 (秒) (安全驾驶) (经济驾驶)
			safeDriveData.put("p12", 0);// p12 疲劳驾驶次数 (次) (安全驾驶)
			safeDriveData.put("p13", 0);// p13 疲劳驾驶时长 (秒) (安全驾驶)
			safeDriveData.put("p14", 0);// 14 急转弯(次) (安全驾驶) (经济驾驶)
			safeDriveData.put("p15", 0);// 15 急变道 (次)(安全驾驶) (经济驾驶)
			safeDriveData.put("safescore", 100);// 分数
			safeDriveData.put("economicscore", 100);// 分数

		}

		Map<String, Object> mapRet = new HashMap<>();
		mapRet.put("deviceSn", deviceSn);
		mapRet.put("safeDriveData", safeDriveData);
		mapResponse.put("code", "0");
		mapResponse.put("ret", mapRet);
		mapResponse.put("what", null);

		return mapResponse;
	}

	/**
	 * 获取安全和经济统计数据
	 */
	public Map<String, Object> getSafeEcoStatisticsData(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String starttime = DataUtil.getStringFromMap(params, "startTime");
		String endtime = DataUtil.getStringFromMap(params, "endTime");
		String scoreType = DataUtil.getStringFromMap(params, "scoreType");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");
		Map<String, Object> mapResponse = new HashMap<>();

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getSafeEcoStatisticsData");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("startTime", starttime);
					param.put("endTime", endtime);
					param.put("scoreType", scoreType);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getSafeDriveData异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 未绑定此设备
		if (!daoUtil.hasOperatingAuthority(deviceID, userId)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}

		// 精确查询某段行程的安全驾驶数据
		Map<String, Object> OBDStatistics = new HashMap<String, Object>();

		OBDStatistics = daoUtil.getOBDAlarmScoreStatistics(starttime, endtime, scoreType, deviceSn);

		if (OBDStatistics.get("score") == null) {
			OBDStatistics.put("p2", 0);// p2 超速报警 (次) (安全驾驶) (经济驾驶)
			OBDStatistics.put("p4", 0);// p4 急加速报警// (次)// (安全驾驶)/ (经济驾驶)
			OBDStatistics.put("p5", 0);// p5 急减速报警 (次) (安全驾驶) (经济驾驶)
			OBDStatistics.put("p6", 0);// p6 停车未熄火报警 (次)(经济驾驶)
			OBDStatistics.put("p8", 0);// p8转速过高报警 (次) (安全驾驶) (经济驾驶)
			OBDStatistics.put("p9", 0);// p9 转速超标时长 (秒) (安全驾驶) (经济驾驶)
			OBDStatistics.put("p10", 0);// p10 停车未熄火告警 (秒) (经济驾驶)
			OBDStatistics.put("p11", 0);// p11 超速时长 (秒) (安全驾驶) (经济驾驶)
			OBDStatistics.put("p12", 0);// p12 疲劳驾驶次数 (次) (安全驾驶)
			OBDStatistics.put("p13", 0);// p13 疲劳驾驶时长 (秒) (安全驾驶)
			OBDStatistics.put("p14", 0);// 14 急转弯(次) (安全驾驶) (经济驾驶)
			OBDStatistics.put("p15", 0);// 15 急变道 (次)(安全驾驶) (经济驾驶)
			OBDStatistics.put("score", 100);// 分数
		} else {

			float a = Float.parseFloat(OBDStatistics.get("score").toString());// b为object类型
			int score = (int) a;
			OBDStatistics.put("score", score);
		}
		Map<String, Object> mapRet = new HashMap<>();
		mapRet.put("statisticsData", OBDStatistics);
		mapResponse.put("code", "0");
		mapResponse.put("ret", mapRet);
		mapResponse.put("what", null);

		return mapResponse;
	}

	/**
	 * 获取车况 查询车辆检测数据
	 */
	public Map<String, Object> getCarData(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String datetime = DataUtil.getStringFromMap(params, "datetime");

		Map<String, Object> mapResponse = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getCarData");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("datetime", datetime);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getSafeDriveData异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 未绑定此设备
		if (!daoUtil.hasOperatingAuthority(deviceID, userId)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}
		// 时间转换
		Date startDate = UtilDate.getDate(datetime, UtilDate.simple);
		Date endDate = UtilDate.getDate(datetime, UtilDate.simple);
		startDate.setHours(00);
		startDate.setMinutes(00);
		startDate.setSeconds(00);
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		// 查询油耗里程数据
		Map<String, Object> mileageAndFuel = daoUtil.getCarMileageAndFuelData(deviceSn, formatter.format(startDate),
				formatter.format(endDate));
		int carInspectionId = daoUtil.getCarInspectionId(deviceSn);
		// 获取检测分数和故障信息
		List<Map<String, Object>> carInspectionData = daoUtil.getCarInspectionScoreAndTroubleData(carInspectionId);

		Map<String, Object> mapRet = new HashMap<>();
		mapRet.put("deviceSn", deviceSn);
		mapRet.put("mileageAndFuel", mileageAndFuel);
		mapRet.put("carInspectionData", carInspectionData);
		mapResponse.put("code", "0");
		mapResponse.put("ret", mapRet);
		mapResponse.put("what", null);

		return mapResponse;

	}

	/**
	 * 轨迹信息
	 */
	public Map<String, Object> getDriveTrail(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String datetime = DataUtil.getStringFromMap(params, "datetime");

		Map<String, Object> mapResponse = new HashMap<>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getDriveTrail");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("datetime", datetime);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getSafeDriveData异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 未绑定此设备
		if (!daoUtil.hasOperatingAuthority(deviceID, userId)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}

		// 时间转换
		Date endDate = UtilDate.getDate(datetime, UtilDate.simple);// 结束时间
		Date startDate = UtilDate.getDate(datetime, UtilDate.simple);
		startDate.setHours(00);
		startDate.setMinutes(00);
		startDate.setSeconds(00);

		// 获取GPS数据
		List<Map<String, Object>> driveTrailData = daoUtil.getDriveTrail(deviceID, UtilDate.getDateString(startDate),
				UtilDate.getDateString(endDate));
		if (driveTrailData == null || driveTrailData.size() == 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_drive_trail_data"));
			return mapResponse;
		}
		// 计算平均速度
		if (null != driveTrailData && driveTrailData.size() > 0) {
			int sumSpendtime = 0;
			double sumMileage = 0;
			double sumFuelConsumption = 0;

			for (Map<String, Object> map : driveTrailData) {
				int spendtime = map.get("spendtime") != null ? Integer.parseInt(map.get("spendtime").toString()) : 0;// 耗时
				double mileage = map.get("mileage") != null ? Double.parseDouble(map.get("mileage").toString()) : 0;// 行程
				double fuelConsumption = map.get("fuel_consumption") != null
						? Double.parseDouble(map.get("fuel_consumption").toString()) : 0;// 行程
				if (mileage > 0) {
					mileage = mileage / 1000;// 转换成km
					map.put("mileage", String.format("%.2f", mileage));
				}

				if (spendtime > 0) {
					double speed = (mileage / (spendtime)) * 60;// 千米每小时
					map.put("speed", String.format("%.2f", speed));
				}
				sumSpendtime += spendtime;
				sumMileage += mileage;
				sumFuelConsumption += fuelConsumption;
			}

			for (Map<String, Object> map : driveTrailData) {
				map.put("sumSpendtime", sumSpendtime);
				map.put("sumMileage", String.format("%.2f", sumMileage));
				map.put("sumFuelConsumption", String.format("%.2f", sumFuelConsumption));
			}

		}

		Map<String, Object> mapRet = new HashMap<>();
		mapRet.put("deviceSn", deviceSn);
		mapRet.put("driveTrailData", driveTrailData);
		mapResponse.put("code", "0");
		mapResponse.put("ret", mapRet);
		mapResponse.put("what", null);

		return mapResponse;
	}

	public Map<String, Object> setAlarmStatus(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String alarmID = DataUtil.getStringFromMap(params, "alarmIDS");
		if (alarmID.contains(",")) {
			alarmID = alarmID.replace(",", "','");
		}
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		// 查找用户名失败
		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "setAlarmStatus");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("alarmIDS", alarmID);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问setAlarmStatus异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		if (!daoUtil.setAlarmStatus(alarmID)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "setalarm.failed"));
			return mapResponse;
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "setalarm.success"));
		return mapResponse;
	}

	/**
	 * 检测车辆
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> startCarInspection(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String datetime = DataUtil.getStringFromMap(params, "datetime");

		Map<String, Object> mapResponse = new HashMap<>();
		// 查找用户名失败
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "startCarInspection");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("datetime", datetime);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问startCarInspection异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 未绑定此设备
		if (!daoUtil.hasOperatingAuthority(deviceID, userId)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}
		// 检查条件：必须为停车未熄火状态
		if (!daoUtil.isIdling(deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "600");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.isIdling"));
			return mapResponse;
		}

		// 检测结果标志
		int wasteGasValue = 0;// 废气控制系统 正常
		int gasAndFuelValue = 0;// 进气及燃油系统 正常
		int speedValue = 0; // 车速及怠速控制系统 正常
		int fireValue = 0; // 点火系统 正常
		int troubleValue = 0;// 故障检测 正常
		int computeControllerValue = 0;// 电脑控制系统 正常
		int score = 100;// 满分100分

		// 类型相同的工况不重复扣分
		int fuheValue = 0;
		int wenduValue = 0;
		int shortfuelValue = 0;
		int longfuelValue = 0;
		int gasflowValue = 0;
		int pressValue = 0;
		int positionValue = 0;
		int rollrateValue = 0;
		int fire_Value = 0;

		int troubleTime = 5;

		Date endTime = new Date();
		Calendar startTime = Calendar.getInstance();

		Map<String, Object> acconTimeMap = daoUtil.getAcconTime(deviceSn);
		Date acconTime = null;
		if (null != acconTimeMap) {
			try {
				SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				acconTime = acconTimeMap.get("Last_accon_time") == null ? null
						: formatter.parse(acconTimeMap.get("Last_accon_time").toString());
				String timeLong = UtilDate.getDateDiff(formatter.format(acconTime), formatter.format(endTime));
				// 只取最后上传的数据：(当前时间 - 最后一次点火时间)>5分钟，则开始时间=当前时间-5分钟。最后一次点火时间
				// <=当前时间 5分钟，开始时间=最后一次点火时间
				if (timeLong.compareTo("00:05:00") > 1) {
					startTime.setTime(endTime);
					startTime.add(Calendar.MINUTE, -troubleTime);
				} else {
					startTime.setTime(acconTime);
				}

			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		// 获取工况减分项
		List<Map<String, Object>> workConditionMap = daoUtil.getWorkConditionInspectionItem(deviceSn,
				UtilDate.getDateString(startTime), UtilDate.getDateString(endTime));

		// 开始计算分数
		if (workConditionMap != null && workConditionMap.size() > 0) {
			for (Map<String, Object> workMap : workConditionMap) {
				// 废气控制系统
				// #计算负荷值 20~100%（正常区间值）扣2分 （乘用）（0x2104，0x15c0）
				String conkey = workMap.get("con_key").toString();
				if (conkey != null && (conkey.equals("0X2104") || conkey.equals("0X15c0"))) {
					int fuhe1 = workMap.get("con_value") != null ? Integer.parseInt(workMap.get("con_value").toString())
							: 0;
					if (((fuhe1 < 20 || fuhe1 > 100)) && fuheValue == 0) {
						wasteGasValue = 1;// 废气控制系统不正常
						score = score - 2;
						fuheValue = 1;
					}
				}

				// 进气及燃油系统
				// #发动机冷却液温度 60~110oC（正常区间值） 扣5分 （乘用） （0x2105，0x16e0）

				if (conkey != null && (conkey.equals("0X2105") || conkey.equals("0X16e0"))) {
					int wendu1 = workMap.get("con_value") != null
							? Integer.parseInt(workMap.get("con_value").toString()) : 0;
					if (((wendu1 < 60 || wendu1 > 110)) && wenduValue == 0) {
						gasAndFuelValue = 1;
						score = score - 5;
						wenduValue = 1;
					}
				}
				// #短时燃油修正(气缸列1和3) ±20%（正常区间值）扣2分 （0x2106，0x2108）（乘用）
				if (conkey != null && (conkey.equals("0X2106") || conkey.equals("0X2108"))) {
					int shortfuel1 = workMap.get("con_value") != null
							? Integer.parseInt(workMap.get("con_value").toString()) : 0;
					if (((shortfuel1 < -20 || shortfuel1 > 20)) && shortfuelValue == 0) {
						gasAndFuelValue = 1;
						score = score - 2;
						shortfuelValue = 1;
					}
				}
				// #长期燃油修正(气缸列1和3) ±20%（正常区间值） 扣2分 （0x2107，0x2109）（乘用）

				if (conkey != null && (conkey.equals("0X2107") || conkey.equals("0X2109"))) {
					int longfuel1 = workMap.get("con_value") != null
							? Integer.parseInt(workMap.get("con_value").toString()) : 0;
					if (((longfuel1 < -20 || longfuel1 > 20)) && longfuelValue == 0) {
						gasAndFuelValue = 1;
						score = score - 2;
						longfuelValue = 1;
					}
				}
				// #空气流量传感器的空气流量 0~35(kg/h) （正常区间值） 扣3分 （乘用）（0x2110，0x0084）

				if (conkey != null && (conkey.equals("0X2110") || conkey.equals("0X0084"))) {
					int gasflow1 = workMap.get("con_value") != null
							? Integer.parseInt(workMap.get("con_value").toString()) : 0;
					if (((gasflow1 < 0 || gasflow1 > 35)) && gasflowValue == 0) {
						gasAndFuelValue = 1;
						score = score - 3;
						gasflowValue = 1;
					}
				}
				// #进气歧管绝对压力 20~60kpa（正常区间值）扣3分 （乘用） 0x210b

				if (conkey != null && conkey.equals("0X210b")) {
					int press1 = workMap.get("con_value") != null
							? Integer.parseInt(workMap.get("con_value").toString()) : 0;
					if ((press1 < 20 || press1 > 60) && pressValue == 0) {
						gasAndFuelValue = 1;
						score = score - 3;
						pressValue = 1;
					}
				}
				// #绝对节气门位置 1~20%（正常区间值） 扣3分
				// （0x2111，0x2145，0x2147，0x2148，0x1330） （乘用）

				if (conkey != null && (conkey.equals("0X2111") || conkey.equals("0X2145") || conkey.equals("0X2147")
						|| conkey.equals("0X2148") || conkey.equals("0X1330"))) {
					int position1 = workMap.get("con_value") != null
							? Integer.parseInt(workMap.get("con_value").toString()) : 0;
					if (((position1 < 1 || position1 > 20)) && positionValue == 0) {
						gasAndFuelValue = 1;
						score = score - 3;
						positionValue = 1;
					}
				}
				// #################车速及怠速控制系统
				// #发动机转速 550~850（正常区间值）扣3分 （乘用）（0x210c，0x301c，0x1be0）

				if (conkey != null && (conkey.equals("0X210c") || conkey.equals("0X301c") || conkey.equals("0X1be0"))) {
					int rollrate1 = workMap.get("con_value") != null
							? Integer.parseInt(workMap.get("con_value").toString()) : 0;
					if (((rollrate1 < -20 || rollrate1 > 20)) && rollrateValue == 0) {
						speedValue = 1;
						score = score - 3;
						rollrateValue = 1;
					}
				}
				// ##########点火系统
				// #第一缸点火正时提前角 上止点前（BTDC）8~15o（正常区间值）扣7分 （乘用）0X210e 0x059c
				if (conkey != null && (conkey.equals("0X210e") || conkey.equals("0X059c"))) {
					int fire1 = workMap.get("con_value") != null ? Integer.parseInt(workMap.get("con_value").toString())
							: 0;
					if ((fire1 < 8 || fire1 > 15) && fire_Value == 0) {
						fireValue = 1;
						score = score - 7;
						fire_Value = 1;
					}
				}
			}

		}

		/*
		 * SimpleDateFormat formatter = new SimpleDateFormat(
		 * "yyyy-MM-dd HH:mm:ss"); Date date = null; Date troubleDate = null;
		 */

		// 获取此设备最近的工况日期
		Map<String, Object> obdTDCMap = daoUtil.getRecentlyTroubleOBD_TDC(deviceSn);
		List<String> faultMsgList = null;

		if (obdTDCMap != null && obdTDCMap.get("rcv_time") != null) {
			/*
			 * try { date =
			 * formatter.parse(obdTDCMap.get("rcv_time").toString()); } catch
			 * (ParseException e) { e.printStackTrace(); } Calendar
			 * troubleCalendar = Calendar.getInstance();
			 * troubleCalendar.setTime(date);
			 * troubleCalendar.add(Calendar.MINUTE, -troubleTime); troubleDate =
			 * troubleCalendar.getTime();
			 */
			// 获取故障减分项
			List<Map<String, Object>> faultCodeList = daoUtil.getTroubleInspectionItem(deviceSn,
					UtilDate.getDateString(startTime.getTime()), UtilDate.getDateString(endTime));
			// 开始计算分数
			if (null != faultCodeList && faultCodeList.size() > 0) {
				faultMsgList = new ArrayList<String>();
				for (Map<String, Object> faultCodeMap : faultCodeList) {
					// 故障码
					String faultCode = faultCodeMap.get("fault_code").toString();

					// 解析books.xml文件
					// 创建SAXReader的对象reader
					String lang = "en-us";
					if ("zh-cn,en-us,zh-tw".indexOf(language) != -1) {
						lang = language;// 包含这几种语言
					}
					SAXReader reader = new SAXReader();
					try {
						// 通过reader对象的read方法加载books.xml文件,获取docuemnt对象。
						String path = Thread.currentThread().getContextClassLoader()
								.getResource("/malfunction/0/" + lang).getPath();
						Document document = reader.read(new File(path + "malfunction_999.xml"));
						// 通过document对象获取根节点bookstore
						Element documentStore = document.getRootElement();
						// 通过element对象的elementIterator方法获取迭代器
						Iterator it = documentStore.elementIterator();
						// 遍历迭代器，获取根节点中的信息（书籍）
						while (it.hasNext()) {
							Element book = (Element) it.next();

							// 获取book的属性名以及 属性值
							List<Attribute> documentAttrs = book.attributes();
							for (Attribute attr : documentAttrs) {
								if (attr.getName().equals("displayCode") && attr.getValue().equals(faultCode)) {
									// 迭代当前节点的子节点
									Iterator itt = book.elementIterator();
									while (itt.hasNext()) {
										Element bookChild = (Element) itt.next();
										if (bookChild.getName().equals("content")) {
											faultMsgList.add(bookChild.getStringValue());
											break;
										}
									}
								}
							}
						}
					} catch (DocumentException e) {
						e.printStackTrace();
					}

				}
				// 获取故障次数
				troubleValue = 1;
				score = score - faultCodeList.size() * 20;
				if (score <= 0) {
					score = 0;
				}
			}

		}

		int timezone = daoUtil.getUserTimezone(userId);
		String currentUtcDate = "";
		try {
			currentUtcDate = DateTimeUtil.local2utc(datetime, Integer.toString(timezone));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		int carInspectionId = daoUtil.getCarInspectionId(deviceSn);
		if (carInspectionId > 0) {
			// 更新数据
			daoUtil.updateTCarTestScore(deviceID, deviceSn, currentUtcDate, score);
			if (daoUtil.getCarInspectionItemId(carInspectionId, 1) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 1, gasAndFuelValue);
			} else {
				daoUtil.updateTCarTestScoreDetail(carInspectionId, 1, gasAndFuelValue);
			}
			if (daoUtil.getCarInspectionItemId(carInspectionId, 1) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 2, fireValue);
			} else {
				daoUtil.updateTCarTestScoreDetail(carInspectionId, 2, fireValue);
			}
			if (daoUtil.getCarInspectionItemId(carInspectionId, 1) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 3, speedValue);
			} else {
				daoUtil.updateTCarTestScoreDetail(carInspectionId, 3, speedValue);
			}
			if (daoUtil.getCarInspectionItemId(carInspectionId, 1) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 4, wasteGasValue);
			} else {
				daoUtil.updateTCarTestScoreDetail(carInspectionId, 4, wasteGasValue);
			}
			if (daoUtil.getCarInspectionItemId(carInspectionId, 1) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 5, computeControllerValue);
			} else {
				daoUtil.updateTCarTestScoreDetail(carInspectionId, 5, computeControllerValue);
			}
			if (daoUtil.getCarInspectionItemId(carInspectionId, 1) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 6, troubleValue);
			} else {
				daoUtil.updateTCarTestScoreDetail(carInspectionId, 6, troubleValue);
			}
		} else {
			// 插入数据
			daoUtil.insertTCarTestScore(deviceID, deviceSn, score);
			carInspectionId = daoUtil.getCarInspectionId(deviceSn);
			// 1. 进气及燃油系统 2. 点火系统 3. 车速及怠速控制系统 4. 废气控制系统 5. 电脑控制系统 6.
			// 故障码检测，一个故障码暂定扣分20分。
			if (daoUtil.getCarInspectionItemId(carInspectionId, 1) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 1, gasAndFuelValue);
			}
			if (daoUtil.getCarInspectionItemId(carInspectionId, 2) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 2, fireValue);
			}
			if (daoUtil.getCarInspectionItemId(carInspectionId, 3) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 3, speedValue);
			}
			if (daoUtil.getCarInspectionItemId(carInspectionId, 4) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 4, wasteGasValue);
			}
			if (daoUtil.getCarInspectionItemId(carInspectionId, 5) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 5, computeControllerValue);
			}
			if (daoUtil.getCarInspectionItemId(carInspectionId, 6) <= 0) {
				daoUtil.insertTCarTestScoreDetail(carInspectionId, 6, troubleValue);
			}
		}
		// 获取车辆检测分数和故障信息
		List<Map<String, Object>> carInspectionData = daoUtil.getCarInspectionScoreAndTroubleData(carInspectionId);

		if (carInspectionData == null || carInspectionData.size() == 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_car_inspection_data"));
			return mapResponse;
		}
		Map<String, Object> mapRet = new HashMap<>();
		mapRet.put("deviceSn", deviceSn);
		mapRet.put("carInspectionData", carInspectionData);
		mapRet.put("faultMsgList", faultMsgList);
		mapResponse.put("code", "0");
		mapResponse.put("ret", mapRet);
		mapResponse.put("what", null);

		return mapResponse;
	}

	/*
	 * 开始遛狗
	 */
	public Map<String, Object> startWalkDog(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "startWalkDog");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问startWalkDog异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 返回数据
		Map<String, Object> mapRet = new HashMap<String, Object>();
		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}
		int trackID = daoUtil.getDeviceID(deviceSn);
		if (trackID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 获取宠物信息
		Map<String, Object> deviceMap = daoUtil.getDeviceInfo(trackID);
		if (deviceMap == null || deviceMap.get("pet_weight") == null
				|| deviceMap.get("pet_weight").toString().equals("")) {
			mapRet.put("statusValue", 1);// 宠物信息不完善
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "equipment.Promption"));
			return mapResponse;
		}

		// 去拿GPS
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);
		Map<String, Object> gps = daoUtil.getLastGPS(deviceSn, timezone);

		if (gps.get("onlinestatus") == null || gps.get("onlinestatus").toString().equals("0")) {
			mapRet.put("statusValue", 2);// 设备不在线
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4"));
			return mapResponse;
		}
		String lat = gps.get("lat") != null ? gps.get("lat").toString() : "0";
		String lng = gps.get("lng") != null ? gps.get("lng").toString() : "0";
		// 下发定位频率
		Map<String, Object> gpsIntervalMap = setGpsInterval(params, headers, reqBody);
		if (gpsIntervalMap == null || !gpsIntervalMap.containsKey("code")
				|| !gpsIntervalMap.get("code").toString().equals("0")) {
			mapRet.put("statusValue", 3);// 下发定位频率失败
			gpsIntervalMap.put(ReturnObject.RETURN_CODE, "400");
			gpsIntervalMap.put(ReturnObject.RETURN_OBJECT, mapRet);
			gpsIntervalMap.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return gpsIntervalMap;
		}
		// 往表中插入遛狗记录
		String start_latlon = lat + "," + lng;
		int flag = daoUtil.insertTTrip_Data(trackID, deviceSn, start_latlon);
		if (flag > 0) {
			int TTrip_DataId = daoUtil.getTTrip_DataIdByDeviceSn(deviceSn);
			mapRet.put("id", TTrip_DataId);
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "startWalkDog.success"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "startWalkDog.fail"));
		return mapResponse;

	}

	/*
	 * 结束遛狗
	 */
	public Map<String, Object> endWalkDog(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String id = DataUtil.getStringFromMap(params, "id");
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "endWalkDog");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("id", id);
					param.put("deviceSn", deviceSn);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问endWalkDog异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		// 返回数据
		Map<String, Object> mapRet = new HashMap<String, Object>();
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		int trackID = daoUtil.getDeviceID(deviceSn);
		if (trackID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 获取宠物信息
		Map<String, Object> deviceMap = daoUtil.getDeviceInfo(trackID);
		float pet_weight = 0;
		if (deviceMap == null || deviceMap.get("pet_weight") == null
				|| deviceMap.get("pet_weight").toString().equals("")) {
			mapRet.put("statusValue", 1);// 宠物信息不完善
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "equipment.Promption"));
			return mapResponse;
		}
		pet_weight = deviceMap.get("pet_weight") != null ? Float.parseFloat(deviceMap.get("pet_weight").toString()) : 0;
		// 设备是否在线
		// 去拿GPS
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);
		Map<String, Object> gps = daoUtil.getLastGPS(deviceSn, timezone);

		if (gps.get("onlinestatus") == null || gps.get("onlinestatus").toString().equals("0")) {
			mapRet.put("statusValue", 2);// 设备不在线
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4"));
			return mapResponse;
		}
		// 获取定位频率
		Map<String, Object> trackerMap = daoUtil.getTrackerInfo(trackID);
		int gps_interval = trackerMap.get("gps_interval") != null
				? Integer.parseInt(trackerMap.get("gps_interval").toString()) : 0;

		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			gatewayHttpHandler.setGpsInterval(deviceSn, "300");
		} else {
			gatewayHandler.set1003(deviceSn, 300);
		}

		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "updatedeviceinfo");
		param.put("deviceSn", deviceSn);
		param.put("gps_interval", "300");

		try {
			regConsumerService.hanleRegService("updatedeviceinfo", param);
		} catch (Exception e) {
			//
		}
		daoUtil.saveGPSInterval(trackID, "300");

		String lat = gps.get("lat") != null ? gps.get("lat").toString() : "0";
		String lng = gps.get("lng") != null ? gps.get("lng").toString() : "0";
		// 更新遛狗结束时间
		daoUtil.updateWalkDogTrailByID(Integer.parseInt(id));
		// 获取轨迹信息
		Map<String, Object> trackMap = daoUtil.getWalkDogTrailByID(Integer.parseInt(id));
		// 查询gps定位数据
		List<Map<String, Object>> gspMap = null;

		String diffTime = "";// 时间差
		int spend_time = trackMap.get("spendtime") != null ? Integer.parseInt(trackMap.get("spendtime").toString()) : 0;
		if (trackMap != null && trackMap.get("did") != null) {
			String startDate = trackMap.get("start_time").toString();
			String endDate = trackMap.get("end_time").toString();
			diffTime = UtilDate.getDateDiff(startDate, endDate);
			String did = trackMap.get("did") != null ? trackMap.get("did").toString() : "0";
			gspMap = daoUtil.getHistoricalGPSData(Integer.parseInt(did), startDate, endDate);
		}

		// 计算里程
		int mileage = 0;
		double calorie = 0;
		if (null != gspMap && gspMap.size() > 0) {
			float preLat = 0;
			float preLng = 0;
			double distance = 0;
			for (Map<String, Object> gpsdata : gspMap) {
				// 计算卡路里 跑步热量（kcal） = 体重(kg) * 运动时间(小时) * 指数k 指数k=30/速度(分钟/400米)
				double speed = gpsdata.get("speed") != null ? Double.parseDouble(gpsdata.get("speed").toString()) : 0;
				if (speed > 0) {
					calorie = calorie + pet_weight * (gps_interval / 3600.0) * (30 / (24.0 / speed));
				}
				float curLat = gpsdata.get("lat") != null ? Float.parseFloat(gpsdata.get("lat").toString()) : 0;// 纬度
				float curLng = gpsdata.get("lng") != null ? Float.parseFloat(gpsdata.get("lng").toString()) : 0;// 经度
				if (curLat != 0 && curLat != 0 && preLat != 0 && preLat != 0) {
					distance = DistanceUtil.getDistance(preLat, preLng, curLat, curLng);
					mileage = (int) (mileage + distance);
				}
				if (curLat != 0 && curLat != 0) {
					preLat = curLat;
					preLng = curLng;
				}
			}
		}
		// 计算遛狗分数
		int walkDogScore = 0;
		if (pet_weight <= 5) {
			walkDogScore = spend_time * 100 / 5;
		} else if (pet_weight > 5 && pet_weight < 10) {
			walkDogScore = spend_time * 100 / 15;
		} else if (pet_weight >= 10 && pet_weight < 15) {
			walkDogScore = spend_time * 100 / 30;
		} else if (pet_weight >= 15 && pet_weight < 20) {
			walkDogScore = spend_time * 100 / 60;
		} else if (pet_weight >= 20) {
			walkDogScore = spend_time * 100 / 120;
		}
		if (walkDogScore > 100) {
			walkDogScore = 100;
		}
		// 格式化卡路里
		DecimalFormat format = new DecimalFormat("0.00");
		calorie = Double.parseDouble(format.format(new BigDecimal(calorie)));

		// 根据id更新遛狗记录
		String end_latlon = lat + "," + lng;
		int flag = daoUtil.updateTTrip_Data(Integer.parseInt(id), end_latlon, mileage, calorie);
		if (flag > 0) {
			mapRet.put("walkDogMap", gspMap);
			mapRet.put("mileage", format.format(new BigDecimal(mileage / 1000.0)));// 换成千米
			mapRet.put("spendtime", diffTime);
			mapRet.put("calorie", calorie);
			mapRet.put("walkDogScore", walkDogScore);

			mapResponse.put("code", "0");
			mapResponse.put("ret", mapRet);
			mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "endWalkDog.success"));
			return mapResponse;
		}

		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "endWalkDog.fail"));
		return mapResponse;

	}

	/*
	 * 获取遛狗轨迹
	 */
	public Map<String, Object> walkDogTrail(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<String, Object>();
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String datetime = DataUtil.getStringFromMap(params, "datetime");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "walkDogTrail");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("datetime", datetime);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问walkDogTrail异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		// 返回数据
		Map<String, Object> mapRet = new HashMap<String, Object>();
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		int userId = daoUtil.getUserId(username);
		int timezone = daoUtil.getUserTimezone(userId);
		Date startDate = UtilDate.getDate(datetime, UtilDate.simple);
		/*
		 * startDate.setHours(00); startDate.setMinutes(00);
		 * startDate.setSeconds(00);
		 */
		String startUtcDate = UtilDate.getUTCDateString(startDate, timezone);
		startUtcDate = startUtcDate.substring(0, 7);
		// 获取当月遛狗记录
		List<Map<String, Object>> dogTrailMap = daoUtil.walkDogTrail(deviceSn, startUtcDate);
		// 格式化卡路里
		DecimalFormat format = new DecimalFormat("0.00");
		if (dogTrailMap != null) {
			for (Map<String, Object> dogData : dogTrailMap) {
				String spendtime = dogData.get("spendtime") != null ? dogData.get("spendtime").toString() : "0";
				spendtime = UtilDate.getHourMinSec(Integer.parseInt(spendtime));
				dogData.put("spendtime", spendtime);
				String calorie = dogData.get("calorie") != null
						? format.format(new BigDecimal(dogData.get("calorie").toString())) : "0.00";
				String mileage = dogData.get("mileage") != null
						? format.format(new BigDecimal(Integer.parseInt(dogData.get("mileage").toString()) / 1000.0))
						: "0.00";
				dogData.put("calorie", calorie);
				dogData.put("mileage", mileage);
			}
		}
		// 获取当月遛狗次数、里程、卡路里等
		Map<String, Object> walkDogDataStatisticsMap = daoUtil.walkDogDataStatistics(deviceSn, startUtcDate);
		if (walkDogDataStatisticsMap != null) {
			String spendtime = walkDogDataStatisticsMap.get("spendtime") != null
					? walkDogDataStatisticsMap.get("spendtime").toString() : "0";
			spendtime = UtilDate.getHourMinSec(Integer.parseInt(spendtime));
			walkDogDataStatisticsMap.put("spendtime", spendtime);
			String calorie = walkDogDataStatisticsMap.get("calorie") != null
					? format.format(new BigDecimal(walkDogDataStatisticsMap.get("calorie").toString())) : "0.00";
			String mileage = walkDogDataStatisticsMap.get("mileage") != null ? format.format(
					new BigDecimal(Integer.parseInt(walkDogDataStatisticsMap.get("mileage").toString()) / 1000.0))
					: "0.00";
			walkDogDataStatisticsMap.put("calorie", calorie);
			walkDogDataStatisticsMap.put("mileage", mileage);
		}
		mapRet.put("dogTrailMap", dogTrailMap);
		mapRet.put("walkDogDataStatisticsMap", walkDogDataStatisticsMap);
		mapResponse.put("code", "0");
		mapResponse.put("ret", mapRet);
		mapResponse.put("what", null);
		return mapResponse;
	}

	/*
	 * 获取遛狗轨迹详情
	 */
	public Map<String, Object> walkDogTrailDetail(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String id = DataUtil.getStringFromMap(params, "id");
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String startTime = DataUtil.getStringFromMap(params, "startTime");
		String endTime = DataUtil.getStringFromMap(params, "endTime");

		Map<String, Object> mapResponse = new HashMap<>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "walkDogTrailDetail");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("startTime", startTime);
					param.put("endTime", endTime);
					param.put("id", id);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问walkDogTrailDetail异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 未绑定此设备
		if (!daoUtil.hasOperatingAuthority(deviceID, userId)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}
		// 获取GPS数据
		List<Map<String, Object>> gpsData = daoUtil.getHistoricalGPSData(deviceID, startTime, endTime);

		Map<String, Object> trackMap = daoUtil.getWalkDogTrailByID(Integer.parseInt(id));
		String mileage = trackMap.get("mileage") != null ? trackMap.get("mileage").toString() : "0";// 行程
		String calorie = trackMap.get("calorie") != null ? trackMap.get("calorie").toString() : "0";// 卡路里
		String startDate = trackMap.get("start_time").toString();
		String endDate = trackMap.get("end_time").toString();
		String diffTime = UtilDate.getDateDiff(startDate, endDate);// 耗时

		// 计算遛狗分数
		Map<String, Object> deviceMap = daoUtil.getDeviceInfo(deviceID);// 获取宠物信息
		float pet_weight = deviceMap.get("pet_weight") != null
				? Float.parseFloat(deviceMap.get("pet_weight").toString()) : 0;
		int spend_time = trackMap.get("spendtime") != null ? Integer.parseInt(trackMap.get("spendtime").toString()) : 0;
		int walkDogScore = 0;
		if (pet_weight <= 5) {
			walkDogScore = spend_time * 100 / 5;
		} else if (pet_weight > 5 && pet_weight < 10) {
			walkDogScore = spend_time * 100 / 15;
		} else if (pet_weight >= 10 && pet_weight < 15) {
			walkDogScore = spend_time * 100 / 30;
		} else if (pet_weight >= 15 && pet_weight < 20) {
			walkDogScore = spend_time * 100 / 60;
		} else if (pet_weight >= 20) {
			walkDogScore = spend_time * 100 / 120;
		}
		if (walkDogScore > 100) {
			walkDogScore = 100;
		}

		Map<String, Object> mapRet = new HashMap<>();
		mapRet.put("deviceSn", deviceSn);
		mapRet.put("calorie", calorie);
		mapRet.put("mileage", mileage);
		mapRet.put("spendtime", diffTime);
		mapRet.put("walkDogScore", walkDogScore);

		mapRet.put("gps", gpsData);
		mapResponse.put("code", "0");
		mapResponse.put("ret", mapRet);
		mapResponse.put("what", null);
		return mapResponse;

	}

	/**
	 * 套餐查询
	 */
	public Map<String, Object> getPackage(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> mapResponse = new HashMap<>();

		List<Map<String, Object>> packageMap = daoUtil.getPackage();

		mapResponse.put("code", "0");
		mapResponse.put("ret", packageMap);
		mapResponse.put("what", null);
		return mapResponse;
	}

	/**
	 * 生成订单
	 */
	public Map<String, Object> generateOrder(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {

		String order_no = UtilDate.getOrderNum(); // 订单号(订单生成时间搓)
		String trade_no = DataUtil.getStringFromMap(params, "trade_no"); // 支付宝、微信支付、贝宝支付各不相同
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");// 设备编号
		String user = DataUtil.getStringFromMap(params, "user"); // 用户名/帐号
		int pay_type = DataUtil.getStringFromMap(params, "pay_type") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "pay_type").toString()) : 0;// 支付类型
		// //
		// 1代表支付宝支付；2代表paypal支付；3代表微信支付，4.内部续期
		String payer = DataUtil.getStringFromMap(params, "payer");// 支付账号
		double price = DataUtil.getStringFromMap(params, "price") != null
				? Double.parseDouble(DataUtil.getStringFromMap(params, "price").toString()) : 0;// 支付金钱
		String currency_unit = DataUtil.getStringFromMap(params, "currency_unit");// 代表人民币
																					// //
		// usd代表美元
		int status = DataUtil.getStringFromMap(params, "status") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "status").toString()) : 0;// 订单状态
		// //
		// 1代表支付成功；0代表未支付；2代表支付失败；3代表订单取消；4代表支付超时
		String remark = DataUtil.getStringFromMap(params, "remark");
		int order_package_id = DataUtil.getStringFromMap(params, "order_package_id") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "order_package_id").toString()) : 0;// 套餐id

		/****** 增加访问远程接口代码 开始 ***********/
		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "generateOrder");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("order_no", order_no);
					param.put("trade_no", trade_no);
					param.put("user", user);
					param.put("pay_type", "" + pay_type);
					param.put("payer", payer);
					param.put("price", "" + price);
					param.put("currency_unit", currency_unit);
					param.put("status", "" + status);
					param.put("remark", remark);
					param.put("order_package_id", "" + order_package_id);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问generateOrder异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		int flag = daoUtil.generateOrder(order_no, trade_no, deviceSn, user, pay_type, payer, price, currency_unit,
				status, remark, order_package_id);
		// 查询订单信息
		Map<String, Object> orderMap = daoUtil.getGenerateOrderByOrderno(order_no);

		if (flag > 0 && orderMap != null) {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, orderMap);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "generated.order.success"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "generated.order.fail"));
		return mapResponse;

	}

	/**
	 * 订单状态修改
	 */
	public Map<String, Object> updateOrderStatus(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String order_no = DataUtil.getStringFromMap(params, "order_no");// 订单号(订单生成时间搓)
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");// 设备编号
		String status = DataUtil.getStringFromMap(params, "status");// 订单状态
		// 1代表支付成功；0代表未支付；2代表支付失败；3代表订单取消；4代表支付超时

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "updateOrderStatus");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("order_no", order_no);
					param.put("status", status);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问updateOrderStatus异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		int flag = daoUtil.updateOrderStatus(order_no, status);
		if (flag > 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "update.order.success"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "update.order.fail"));
		return mapResponse;

	}

	public Map<String, Object> updateDeviceAroundRanges(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");// 设备编号
		int aroundRanges = DataUtil.getStringFromMap(params, "aroundRanges") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "aroundRanges").toString()) : 0;
		String simNo = DataUtil.getStringFromMap(params, "simNo") == null ? ""
				: DataUtil.getStringFromMap(params, "simNo");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "updateDeviceAroundRanges");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("aroundRanges", String.valueOf(aroundRanges));
					param.put("simNo", simNo);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问updateDeviceAroundRanges异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "updatedeviceinfo");
		param.put("deviceSn", deviceSn);
		param.put("aroundRanges", String.valueOf(aroundRanges));
		param.put("tracker_sim", simNo);

		try {
			regConsumerService.hanleRegService("updatedeviceinfo", param);
		} catch (Exception e) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "update.aroundRanges.fail"));
			return mapResponse;
		}
		// 修改设备 周边属性
		int flag = daoUtil.updateDeviceAroundRanges(deviceSn, aroundRanges, simNo);
		if (flag > 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "update.aroundRanges.success"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "update.aroundRanges.fail"));
		return mapResponse;
	}

	/*
	 * 获取商品信息
	 */
	public Map<String, Object> getGoodsInfo(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		Map<String, Object> mapRet = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		List<Map<String, Object>> goodsMap = daoUtil.getGoodsInfo();
		mapRet.put("goodsMap", goodsMap);
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;
	}

	/**
	 * 查询未完成的遛狗记录
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> unfinishWalkDog(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");// 设备编号

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		Map<String, Object> mapRet = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "unfinishWalkDog");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问unfinishWalkDog异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		Map<String, Object> unfinishWalkDogMap = daoUtil.unfinishWalkDog(deviceSn);
		mapRet.put("unfinishWalkDogMap", unfinishWalkDogMap);
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;
	}

	/*
	 * 获取最新定位点数据
	 */
	public Map<String, Object> recentGpsData(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");// 设备编号
		String datetime = DataUtil.getStringFromMap(params, "datetime");// 最后定位点时间或者遛狗开始时间

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		Map<String, Object> mapRet = new HashMap<>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "recentGpsData");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("datetime", datetime);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问recentGpsData异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		List<Map<String, Object>> recentGpsDataMap = daoUtil.recentGpsData(deviceID, datetime);
		mapRet.put("recentGpsDataMap", recentGpsDataMap);
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;
	}

	/*
	 * 删除遛狗记录
	 */
	public Map<String, Object> deleteWalkDog(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String id = DataUtil.getStringFromMap(params, "id");// 遛狗记录id
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");// 设备号

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "deleteWalkDog");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("id", id);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问deleteWalkDog异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		int flag = daoUtil.deleteWalkDog(Integer.parseInt(id));
		if (flag > 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, null);
			return mapResponse;
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;

	}

	/* 获取系统公告 */
	public Map<String, Object> getSystemNotice(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String language = headers.get("accept-language");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		Map<String, Object> noticeMap = daoUtil.getSystemNotice(language);
		if (noticeMap != null && noticeMap.get("content") != null) {
			int codeNum = noticeMap.get("content").toString().hashCode();
			noticeMap.put("code", String.valueOf(codeNum));
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, noticeMap);
		mapResponse.put(ReturnObject.RETURN_WHAT, "");
		return mapResponse;

	}

	/**
	 * 修改授权账号的备注
	 */
	public Map<String, Object> modifyAccountRemark(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String authorizedname = DataUtil.getStringFromMap(params, "authorizedname");// 要修改备注的用户名
		String nickname = DataUtil.getStringFromMap(params, "nickname");// 备注名
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");// 设备号
		String isGps = (DataUtil.getStringFromMap(params, "isGps") != null
				&& !DataUtil.getStringFromMap(params, "isGps").isEmpty())
						? DataUtil.getStringFromMap(params, "isGps").toString() : "1";

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}

		// 修改TDeviceUser表中的nickname字段
		// daoUtil.modifyAccountRemark(userID,deviceID,nickname);
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "modifyAccountRemark");
		param.put("authorizedname", authorizedname);
		param.put("nickname", nickname);
		param.put("deviceSn", deviceSn);
		param.put("username", username);
		param.put("isGps", isGps);

		try {
			RPCResult rpcResult = regConsumerService.hanleRegService("modifyAccountRemark", param);
			if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS
					&& JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
				mapResponse.put(ReturnObject.RETURN_CODE, "0");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "update.info.success"));
				return mapResponse;
			}
		} catch (Exception e) {
			//
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "update.info.fail"));
		return mapResponse;
	}

	/*
	 * 上传定位数据
	 */
	public Map<String, Object> uploadGpsData(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");// 设备号
		String collect_datetime = DataUtil.getStringFromMap(params, "collect_datetime");// 上传时间
		String lat = DataUtil.getStringFromMap(params, "lat") != null
				? DataUtil.getStringFromMap(params, "lat").toString() : "";// 经度
		String lng = DataUtil.getStringFromMap(params, "lng") != null
				? DataUtil.getStringFromMap(params, "lng").toString() : "";// 维度

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "uploadGpsData");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("deviceSn", deviceSn);
					param.put("username", username);
					param.put("collect_datetime", collect_datetime);
					param.put("lat", lat);
					param.put("lng", lng);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问uploadGpsData异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 时间转换
		Date collectdate = UtilDate.getDate(collect_datetime, UtilDate.simple);
		int timezone = daoUtil.getUserTimezone(userId);
		String collectdatetime = UtilDate.getUTCDateString(collectdate, timezone);
		// 保存定位数据
		int flag = 0;
		if (lat != null && !lat.equals("") && lng != null && !lng.equals("")) {
			flag = daoUtil.uploadGpsData(deviceID, deviceSn, collectdatetime, lat, lng);
		}
		if (flag > 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, null);
			return mapResponse;
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;
	}

	/**
	 * 获取蓝牙手表位置信息
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getCurrentBluetoothWatchGpsData(Map<String, String[]> params,
			Map<String, String> headers) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");// 设备号

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getCurrentBluetoothWatchGpsData");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("deviceSn", deviceSn);
					param.put("username", username);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getCurrentBluetoothWatchGpsData异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		Map<String, Object> retMap = new HashMap<String, Object>();
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "getGpsInfo");
		param.put("username", username);
		param.put("deviceSn", deviceSn);
		try {
			RPCResult rpcResult = regConsumerService.hanleRegService("getGpsInfo", param);
			retMap.put("gpsData", new HashMap<String, Object>());
			if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS
					&& JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")
					&& JSON.parseObject(rpcResult.getRpcResult().toString()).getJSONObject("data") == null) {
				// 没有权限获取定位数据
				mapResponse.put(ReturnObject.RETURN_CODE, "0");
				mapResponse.put(ReturnObject.RETURN_OBJECT, retMap);
				mapResponse.put(ReturnObject.RETURN_WHAT, null);
				return mapResponse;
			} else if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
					|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
				// 获取权限失败 返回成功标志 但是没有数据
				mapResponse.put(ReturnObject.RETURN_CODE, "0");
				mapResponse.put(ReturnObject.RETURN_OBJECT, retMap);
				mapResponse.put(ReturnObject.RETURN_WHAT, null);
				return mapResponse;
			}
		} catch (Exception e) {
			e.printStackTrace();
			// 获取权限失败
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, null);
			return mapResponse;
		}
		// 获取定位点数据
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);
		Map<String, Object> gpsData = daoUtil.getCurrentGPS(deviceSn, timezone);
		retMap.put("gpsData", gpsData);
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, retMap);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;
	}

	/**
	 * 推送警情信息
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> pushBluetoothWatchAlarmDataToApp(Map<String, String[]> params,
			Map<String, String> headers) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");// 设备号

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		// 查找用户名失败
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		int timezone = daoUtil.getUserTimezone(userId);
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String currentDate = df.format(new Date());
		try {
			currentDate = DateTimeUtil.local2utc(currentDate, Integer.toString(timezone));
		} catch (ParseException e) {
			e.printStackTrace();
		}

		String gpstime = DataUtil.getStringFromMap(params, "gpstime");
		if (gpstime != null && !gpstime.equals("")) {
			try {
				currentDate = DateTimeUtil.local2utc(gpstime, Integer.toString(timezone));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}

		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, DataUtil.getStringFromMap(params, "lat").isEmpty() ? ""
				: DataUtil.getStringFromMap(params, "lat").toString());
		bodyVo.put(2, DataUtil.getStringFromMap(params, "lng").isEmpty() ? ""
				: DataUtil.getStringFromMap(params, "lng").toString());
		bodyVo.put(3, currentDate);
		bodyVo.put(4, "");
		bodyVo.put(5, "");
		bodyVo.put(6, 3);
		bodyVo.put(7, 3);
		bodyVo.put(8, 0);
		bodyVo.put(9, 100);
		bodyVo.put(16, 6);

		int result = gatewayHandler.set3002(deviceSn, bodyVo);
		if (result > 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, "");
			return mapResponse;
		} else {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, "");
			return mapResponse;
		}

	}

	/**
	 * 设置蓝牙手表sos报警开关
	 */
	public Map<String, Object> setBluetoothWatchToggle(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String boundary = DataUtil.getStringFromMap(params, "boundary") == null ? "1"
				: DataUtil.getStringFromMap(params, "boundary");
		String voltage = DataUtil.getStringFromMap(params, "voltage") == null ? "1"
				: DataUtil.getStringFromMap(params, "voltage");
		String tow = DataUtil.getStringFromMap(params, "tow") == null ? "1" : DataUtil.getStringFromMap(params, "tow");
		String clipping = DataUtil.getStringFromMap(params, "clipping") == null ? "1"
				: DataUtil.getStringFromMap(params, "clipping");
		String speed = DataUtil.getStringFromMap(params, "speed") == null ? "1"
				: DataUtil.getStringFromMap(params, "speed");
		String speedValue = DataUtil.getStringFromMap(params, "speedValue") == null ? "120"
				: DataUtil.getStringFromMap(params, "speedValue");
		String speedTime = DataUtil.getStringFromMap(params, "speedTime") == null ? "60"
				: DataUtil.getStringFromMap(params, "speedTime");
		String sos = DataUtil.getStringFromMap(params, "sos") == null ? "1" : DataUtil.getStringFromMap(params, "sos");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		if (!daoUtil.hasOperatingAuthority(trackerID, userID)) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return map;
		}

		if (daoUtil.isHaveAlarmSet(trackerID)) {
			daoUtil.updateAlarmSet(boundary, voltage, tow, clipping, speed, Integer.parseInt(speedValue),
					Integer.parseInt(speedTime), sos, userID, trackerID, null, null, null, null);
		} else {
			daoUtil.insertAlarmSet(boundary, voltage, tow, clipping, speed, Integer.parseInt(speedValue),
					Integer.parseInt(speedTime), sos, userID, trackerID, null, null, null, null);
		}

		map.put("code", "0");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "tracker.success"));
		return map;
	}

	/*
	 * 获取蓝牙手表sos报警开关信息
	 */
	public Map<String, Object> getBluetoothWatchToggle(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getBluetoothWatchToggle");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getBluetoothWatchToggle异常" + e.getMessage());
			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", daoUtil.getToggle(trackerID));
		map.put("what", "");

		return map;
	}

	/**
	 * 设置蓝牙手表闹钟提醒
	 */
	public Map<String, Object> setBluetoothWatchRemind(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String user_id = DataUtil.getStringFromMap(params, "user_id") != null
				? DataUtil.getStringFromMap(params, "user_id").toString() : "";
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String week = DataUtil.getStringFromMap(params, "week");
		String time = DataUtil.getStringFromMap(params, "time");
		// ring=闹钟铃声，1到5，代表铃声1到铃声5
		int ring = DataUtil.getStringFromMap(params, "ring") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "ring").toString()) : 1;
		// alert_type=报警提示类型，0：仅响铃，1：仅震动，2：震动及响铃，默认0
		int alert_type = DataUtil.getStringFromMap(params, "alert_type") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "alert_type").toString()) : 0;
		// flag=闹钟是否开启使用，0：表示不开启，1表示开启
		int flag = DataUtil.getStringFromMap(params, "flag") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "flag").toString()) : 1;
		// type=提醒类型(0=默认1图片,1=默认2图片,2=自定义图片)
		int type = DataUtil.getStringFromMap(params, "type") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "type").toString()) : 0;
		// title_len=提醒字串长度
		int title_len = DataUtil.getStringFromMap(params, "title_len") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "title_len").toString()) : 0;
		// title = 提醒内容，Unicode编码
		String title = DataUtil.getStringFromMap(params, "title");
		// image_len=图片长度
		int image_len = DataUtil.getStringFromMap(params, "image_len") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "image_len").toString()) : 0;
		// 图片名称
		String image_name = DataUtil.getStringFromMap(params, "image_name");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "setBluetoothWatchRemind");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", username);
					param.put("user_id", user_id);
					param.put("deviceSn", deviceSn);
					param.put("week", week);
					param.put("time", time);
					param.put("ring", String.valueOf(ring));
					param.put("alert_type", String.valueOf(alert_type));
					param.put("flag", String.valueOf(flag));
					param.put("type", String.valueOf(type));
					param.put("title_len", String.valueOf(title_len));
					param.put("title", title);
					param.put("image_len", String.valueOf(image_len));
					param.put("image_name", image_name);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问setBluetoothWatchRemind异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put("code", "200");
			mapResponse.put("ret", "");
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 最多只能保存五个闹钟
		List<Map<String, Object>> bluetoothInfoMap = daoUtil.getBluetoothWatchRemind(0, deviceSn);
		if (bluetoothInfoMap != null && bluetoothInfoMap.size() >= 5) {
			mapResponse.put("code", "400");
			mapResponse.put("ret", "");
			mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "saveclock.max.failed"));
			return mapResponse;
		}

		// 添加闹钟提醒
		boolean addFlag = daoUtil.setBluetoothWatchRemind(user_id, deviceSn, week, time, ring, alert_type, flag, type,
				title_len, title, image_len, image_name);
		// 获取闹铃id
		List<Map<String, Object>> remindInfoList = daoUtil.getBluetoothWatchRemind(0, deviceSn);
		if (addFlag) {
			String id = "";
			Map<String, Object> remindMap = new HashMap<String, Object>();
			if (remindInfoList != null && remindInfoList.size() > 0) {
				id = remindInfoList.get(remindInfoList.size() - 1).get("id").toString();
				remindMap.put("id", id);
			}
			mapResponse.put("code", "0");
			mapResponse.put("ret", remindMap);
			mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "saveclock.success"));
			return mapResponse;
		}
		mapResponse.put("code", "200");
		mapResponse.put("ret", "");
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "saveclock.failed"));
		return mapResponse;
	}

	/**
	 * 获取蓝牙手表闹钟提醒
	 */
	public Map<String, Object> getBluetoothWatchRemind(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getBluetoothWatchRemind");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("deviceSn", deviceSn);
					param.put("username", username);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getBluetoothWatchRemind异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put("code", "200");
			mapResponse.put("ret", "");
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 查询闹钟提醒
		Map<String, Object> remindMap = new HashMap<String, Object>();
		List<Map<String, Object>> remind_Map = daoUtil.getBluetoothWatchRemind(0, deviceSn);
		remindMap.put("remindMap", remind_Map);
		mapResponse.put("code", "0");
		mapResponse.put("ret", remindMap);
		mapResponse.put("what", null);
		return mapResponse;
	}

	/**
	 * 修改蓝牙手表闹钟提醒
	 */
	public Map<String, Object> updateBluetoothWatchRemind(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		int id = DataUtil.getStringFromMap(params, "id") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "id").toString()) : 0;
		String user_id = DataUtil.getStringFromMap(params, "user_id") != null
				? DataUtil.getStringFromMap(params, "user_id").toString() : "";
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String week = DataUtil.getStringFromMap(params, "week");
		String time = DataUtil.getStringFromMap(params, "time");
		// ring=闹钟铃声，1到5，代表铃声1到铃声5
		int ring = DataUtil.getStringFromMap(params, "ring") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "ring").toString()) : 1;
		// alert_type=报警提示类型，0：仅响铃，1：仅震动，2：震动及响铃，默认0
		int alert_type = DataUtil.getStringFromMap(params, "alert_type") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "alert_type").toString()) : 0;
		// flag=闹钟是否开启使用，0：表示不开启，1表示开启
		int flag = DataUtil.getStringFromMap(params, "flag") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "flag").toString()) : 1;
		// type=提醒类型(0=默认1图片,1=默认2图片,2=自定义图片)
		int type = DataUtil.getStringFromMap(params, "type") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "type").toString()) : 0;
		// title_len=提醒字串长度
		int title_len = DataUtil.getStringFromMap(params, "title_len") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "title_len").toString()) : 0;
		// title = 提醒内容，Unicode编码
		String title = DataUtil.getStringFromMap(params, "title");
		// image_len=图片长度
		int image_len = DataUtil.getStringFromMap(params, "image_len") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "image_len").toString()) : 0;
		// 图片名称
		String image_name = DataUtil.getStringFromMap(params, "image_name");
		// 闹钟设备版本
		int version = DataUtil.getStringFromMap(params, "version") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "version")) : 1;

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "updateBluetoothWatchRemind");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", username);
					param.put("id", String.valueOf(id));
					param.put("user_id", user_id);
					param.put("deviceSn", deviceSn);
					param.put("week", String.valueOf(week));
					param.put("time", time);
					param.put("ring", String.valueOf(ring));
					param.put("alert_type", String.valueOf(alert_type));
					param.put("flag", String.valueOf(flag));
					param.put("type", String.valueOf(type));
					param.put("title_len", String.valueOf(title_len));
					param.put("title", title);
					param.put("image_len", String.valueOf(image_len));
					param.put("image_name", image_name);
					param.put("version", String.valueOf(version));

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问updateBluetoothWatchRemind异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put("code", "200");
			mapResponse.put("ret", "");
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 更新闹钟提醒
		List<Map<String, Object>> remindMap = daoUtil.getBluetoothWatchRemind(id, deviceSn);
		if (remindMap != null && remindMap.size() > 0) {
			int versionNum = remindMap.get(0).get("version") != null
					? Integer.parseInt(remindMap.get(0).get("version").toString()) : 1;
			if (version + 1 <= versionNum) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "saveclock.failed"));
				return mapResponse;
			}
			versionNum = version + 1;
			boolean updateFlag = daoUtil.updateBluetoothWatchRemind(id, user_id, deviceSn, week, time, ring, alert_type,
					flag, type, title_len, title, image_len, image_name, versionNum);
			if (updateFlag) {
				mapResponse.put(ReturnObject.RETURN_CODE, "0");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "saveclock.success"));
				return mapResponse;
			}
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "saveclock.failed"));
		return mapResponse;

	}

	/*
	 * 删除蓝牙手表提醒功能
	 */
	public Map<String, Object> deleteBluetoothWatchRemind(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		int id = DataUtil.getStringFromMap(params, "id") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "id").toString()) : 0;
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "deleteBluetoothWatchRemind");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", username);
					param.put("id", String.valueOf(id));
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问deleteBluetoothWatchRemind异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put("code", "200");
			mapResponse.put("ret", "");
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 删除闹钟提醒
		boolean deleteFlag = daoUtil.deleteBluetoothWatchRemind(id);
		if (deleteFlag) {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "deleteclock.success"));
			return mapResponse;
		}

		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "deleteclock.failed"));
		return mapResponse;
	}

	/**
	 * 查询设备和验证码是否存在
	 */
	public Map<String, Object> isExistDeviceCode(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String insur_code = DataUtil.getStringFromMap(params, "insur_code");
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put("code", "200");
			mapResponse.put("ret", "");
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}
		// 查询验证码是否正确
		Map<String, Object> insurCodeMap = daoUtil.isExistDeviceCode(insur_code, deviceSn);
		if (insurCodeMap != null && insurCodeMap.get("id") != null) {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "validation.success"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, "");
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "validation.fail"));
		return mapResponse;
	}

	/*
	 * 插入保险定单
	 */
	public Map<String, Object> setPetInsurance(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Date date = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String no = sdf.format(date);
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String user_name = DataUtil.getStringFromMap(params, "user_name");
		String real_name = DataUtil.getStringFromMap(params, "real_name");
		String mobile = DataUtil.getStringFromMap(params, "mobile");
		String dog_name = DataUtil.getStringFromMap(params, "dog_name");
		String type = DataUtil.getStringFromMap(params, "type");
		String colour = DataUtil.getStringFromMap(params, "colour");
		int tail_shape = DataUtil.getStringFromMap(params, "tail_shape") != null
				? Integer.parseInt(DataUtil.getStringFromMap(params, "tail_shape").toString()) : 1;
		int age = (DataUtil.getStringFromMap(params, "age") != null
				&& !DataUtil.getStringFromMap(params, "age").equals(""))
						? Integer.parseInt(DataUtil.getStringFromMap(params, "age").toString()) : 1;
		int sex = (DataUtil.getStringFromMap(params, "sex") != null
				&& !DataUtil.getStringFromMap(params, "sex").equals(""))
						? Integer.parseInt(DataUtil.getStringFromMap(params, "sex").toString()) : 1;

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put("code", "200");
			mapResponse.put("ret", "");
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}
		// 插入保险生效时间
		int userID = daoUtil.getUserId(username);
		if (userID < 1) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_not_exist_or_timeout"));
			return mapResponse;
		}
		int timezone = daoUtil.getUserTimezone(userID);
		Calendar calendar = Calendar.getInstance(); // 得到日历
		String dNow = sdf.format(new Date());
		Date startDateUtc = new Date();
		Date endDateUtc = new Date();
		String startDateStr = "";
		String endDateStr = "";
		try {
			// 后一天
			calendar.setTime(sdf.parse(dNow));
			calendar.add(Calendar.DAY_OF_YEAR, 1); // 设置为后一天 00:00:00
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MINUTE, 0);
			startDateUtc = calendar.getTime();
			startDateStr = sdf.format(startDateUtc);
			startDateStr = DateTimeUtil.local2utc(startDateStr, Integer.toString(timezone));
			// 后一年
			calendar.setTime(sdf.parse(dNow));
			calendar.add(Calendar.DAY_OF_YEAR, 365); // 设置为后一年 23:59:59
			calendar.set(Calendar.HOUR_OF_DAY, 23);
			calendar.set(Calendar.SECOND, 59);
			calendar.set(Calendar.MINUTE, 59);
			endDateUtc = calendar.getTime();
			endDateStr = sdf.format(endDateUtc);
			endDateStr = DateTimeUtil.local2utc(endDateStr, Integer.toString(timezone));
		} catch (Exception e) {
			e.printStackTrace();
		}
		// 判断是否存在宠物保险
		Map<String, Object> petInsurMap = daoUtil.getPetInsurance(deviceSn, timezone);
		if (petInsurMap != null && petInsurMap.size() > 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "add.pet.insurance.repeat"));
			return mapResponse;
		}
		// 插入保险定单
		boolean flag = daoUtil.setPetInsurance(no, deviceSn, user_name, real_name, mobile, dog_name, type, colour,
				tail_shape, age, sex, startDateStr, endDateStr);
		// 更新宠物信息
		int deviceID = daoUtil.getDeviceID(deviceSn);
		// 更新年龄、性别、名字等信息
		Map<String, Object> petMap = daoUtil.getPetInfo(deviceID);
		if (petMap == null || petMap.size() < 1 || petMap.get("did") == null || petMap.get("did").equals("")) {
			// 插入
			daoUtil.addPetInfo(deviceID, dog_name, String.valueOf(sex), type, String.valueOf(age), String.valueOf(0),
					null, null, null, null);
		} else {
			// 更新
			daoUtil.updatePetInfoByInsur(deviceID, String.valueOf(sex), String.valueOf(age), type, mobile);
		}
		// 修改昵称
		daoUtil.updateDeviceInfo(dog_name, deviceID);
		Map<String, String> param = new HashMap<String, String>();
		param.put("function", "updatedeviceinfo");
		param.put("deviceSn", deviceSn);
		param.put("nickname", dog_name);

		try {
			regConsumerService.hanleRegService("updatedeviceinfo", param);
		} catch (Exception e) {
		}
		if (flag) {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "save.info.success"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "300");
		mapResponse.put(ReturnObject.RETURN_OBJECT, "");
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "save.info.fail"));
		return mapResponse;

	}

	/**
	 * 查询保险定单
	 */
	public Map<String, Object> getPetInsurance(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put("code", "200");
			mapResponse.put("ret", "");
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}
		int userID = daoUtil.getUserId(username);
		if (userID < 1) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_not_exist_or_timeout"));
			return mapResponse;
		}
		int timezone = daoUtil.getUserTimezone(userID);

		Map<String, Object> retMap = new HashMap<String, Object>();
		Map<String, Object> insuranceMap = daoUtil.getPetInsurance(deviceSn, timezone);
		retMap.put("insuranceMap", insuranceMap);
		mapResponse.put("code", "0");
		mapResponse.put("ret", insuranceMap);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "get.info.success"));
		return mapResponse;
	}

	/*
	 * 设备续费
	 */
	public Map<String, Object> renewLicense(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn"); // 设备号
		int month = (DataUtil.getStringFromMap(params, "months") != null
				&& !DataUtil.getStringFromMap(params, "months").equals(""))
						? Integer.parseInt(DataUtil.getStringFromMap(params, "months")) : 3;// 续费月份
		String paymentID = DataUtil.getStringFromMap(params, "payId");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put("code", "200");
			mapResponse.put("ret", "");
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}
		int userID = daoUtil.getUserId(username);
		if (userID < 1) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_not_exist_or_timeout"));
			return mapResponse;
		}

		Calendar calendar = Calendar.getInstance(); // 得到日历
		Date expired_timeUtc = new Date();
		String expired_timeStr = "";
		// 判断订单是否支付成功
		String clientID = env.getProperty("client.id");
		String clientSecret = env.getProperty("client.secret");
		String mode = env.getProperty("mode");
		APIContext apiContext = new APIContext(clientID, clientSecret, mode);
		Payment payment = null;
		Map<String, Object> returnMap = new HashMap<String, Object>();
		Map<String, Object> paymentMap = new HashMap<String, Object>();
		try {
			payment = Payment.get(apiContext, paymentID);
			if (payment != null && payment.getState() != null && payment.getState().equals("approved")) {
				// 支付成功
				paymentMap.put("state", payment.getState());
				returnMap.put("paymentMap", paymentMap);
			}
		} catch (PayPalRESTException e) {
			mapResponse.put("code", "300");
			paymentMap.put("state", "failed");
			returnMap.put("paymentMap", paymentMap);
			mapResponse.put("ret", returnMap);
			mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "renew.license.fail"));
			return mapResponse;
		}
		// 修改设备过期时间
		String currentUTCtime = DateTimeUtil.getCurrentUtcDatetime();
		Map<String, Object> map = daoUtil.getDeviceInfoByDevicesn(deviceSn);
		String expired_time_de = "";
		if (map != null && map.get("expired_time_de") != null && !map.get("expired_time_de").equals("")) {
			expired_time_de = map.get("expired_time_de").toString();
			// 判断是否已过期
			if (expired_time_de.compareTo(currentUTCtime) > 0) {
				// 还未过期
				try {
					calendar.setTime(sdf.parse(expired_time_de));
					calendar.add(Calendar.DAY_OF_YEAR, 0);
					calendar.set(Calendar.HOUR_OF_DAY, 0);
					calendar.set(Calendar.MONTH, month);
					calendar.set(Calendar.SECOND, 0);
					calendar.set(Calendar.MINUTE, 0);
					expired_timeUtc = calendar.getTime();
					expired_timeStr = sdf.format(expired_timeUtc);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				// 已过期
				try {
					calendar.setTime(sdf.parse(currentUTCtime));
					calendar.add(Calendar.DAY_OF_YEAR, 0);
					calendar.set(Calendar.HOUR_OF_DAY, 0);
					calendar.set(Calendar.MONTH, month);
					calendar.set(Calendar.SECOND, 0);
					calendar.set(Calendar.MINUTE, 0);
					expired_timeUtc = calendar.getTime();
					expired_timeStr = sdf.format(expired_timeUtc);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} else {
			// 更新
			try {
				calendar.setTime(sdf.parse(currentUTCtime));
				calendar.add(Calendar.DAY_OF_YEAR, 0);
				calendar.set(Calendar.HOUR_OF_DAY, 0);
				calendar.set(Calendar.MONTH, month);
				calendar.set(Calendar.SECOND, 0);
				calendar.set(Calendar.MINUTE, 0);
				expired_timeUtc = calendar.getTime();
				expired_timeStr = sdf.format(expired_timeUtc);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 修改注册服务器设备过期时间
		Map<String, String> paramTokenMap = new HashMap<String, String>();
		paramTokenMap.put("function", "updateDeviceExpirationTime");
		paramTokenMap.put("deviceSn", "'" + deviceSn + "'");
		paramTokenMap.put("expired_time_de", expired_time_de);
		try {
			regConsumerService.hanleRegService("updateDeviceExpirationTime", paramTokenMap);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		// 更新设备过期日期
		boolean flag = daoUtil.setDeviceExpirationTime(deviceSn, expired_timeStr);
		if (flag) {
			mapResponse.put("code", "0");
			returnMap.put("paymentMap", paymentMap);
			mapResponse.put("ret", returnMap);
			mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "renew.license.success"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		paymentMap.put("state", "failed");
		returnMap.put("paymentMap", paymentMap);
		mapResponse.put("ret", returnMap);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "renew.license.fail"));
		return mapResponse;

	}

	// 2.3.46 设置闹钟信息到远程,消息ID：0x1012 设置770设备闹钟信息
	public Map<String, Object> setDeviceRemind(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		int remindIndex = (DataUtil.getStringFromMap(params, "index") != null
				&& !DataUtil.getStringFromMap(params, "index").toString().equals(""))
						? Integer.parseInt(DataUtil.getStringFromMap(params, "index").toString()) : 1;
		String remindValue = DataUtil.getStringFromMap(params, "time");
		String profile = DataUtil.getStringFromMap(params, "profile");
		// 获取闹铃开关
		String clockStatus = "0";// 0关闭 1打开
		if (remindValue != null && remindValue.length() >= 7) {
			clockStatus = remindValue.substring(6, 7);
		}

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		Map<Integer, Object> remindmap = daoUtil.getDeviceAllRemindByDeviceSn(deviceSn);

		if (clockStatus != null && clockStatus.equals("1")) {
			remindmap.put(remindIndex, remindValue);
		} else {
			remindmap.put(remindIndex, "11:30-0-3-0000000");
		}

		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setClock(deviceSn, remindmap);
		} else {
			responseMsg = gatewayHandler.set1012(deviceSn, remindmap);
		}

		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {

			Integer productType = daoUtil.getDeviceProductType(deviceSn);
			if (productType == 24) {// HT790

				Map<String, String[]> paramsProfile = new HashMap<String, String[]>();
				// deviceSn, "", profile
				String[] deviceSnP = new String[1];
				deviceSnP[0] = deviceSn;
				paramsProfile.put("deviceSn", deviceSnP);

				String[] nameP = new String[1];
				nameP[0] = "";
				paramsProfile.put("name", nameP);

				String[] profileP = new String[1];
				profileP[0] = profile;
				paramsProfile.put("mode", profileP);

				Map<String, Object> profileMapResponse = setDeviceProfile(paramsProfile, headers, reqBody);
				if (!profileMapResponse.get("code").equals("0")) {
					mapResponse.put("code", "300");
					mapResponse.put("ret", null);
					mapResponse.put("what",
							LanguageManager.getMsg(headers.get("accept-language"), "set.device.remind.fail"));
					return mapResponse;
				}
			}

			Map<String, Object> remind = daoUtil.getDeviceRemindByDeviceSn(deviceSn, remindIndex);
			if (remind == null) {
				// 插入
				daoUtil.insertDeviceRemind(deviceSn, remindIndex, remindValue);
			} else {
				daoUtil.updateDeviceRemind(deviceSn, remindIndex, remindValue);
			}

			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "set.device.remind.success"));
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "300");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "set.device.remind.fail"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "set.device.remind.fail"));
		return mapResponse;

	}

	// 获取770设备闹钟信息
	public Map<String, Object> getDeviceRemind(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getDeviceRemind");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		List<Map<String, Object>> remindMap = daoUtil.getDeviceRemind(deviceSn);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("remindMap", remindMap);
		map.put("profile", daoUtil.getDeviceProfile(deviceSn));
		mapResponse.put("code", "0");
		mapResponse.put("ret", map);
		mapResponse.put("what", null);
		return mapResponse;

	}

	// 2.3.48 设置监听,消息ID：0x1013。
	public Map<String, Object> setDeviceMonitoring(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String mobile = DataUtil.getStringFromMap(params, "mobile");
		if (mobile == "" || mobile.isEmpty()) {
			mobile = "0";
		}

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setMonitor(deviceSn, mobile);
		} else {
			responseMsg = gatewayHandler.set1013(deviceSn, mobile);
		}

		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			// 更新设备中心号码
			daoUtil.updateDeviceMonitoringPhone(deviceSn, mobile);
			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_success"));
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "300");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
		return mapResponse;
	}

	// 设置远程关机,消息ID：0x1014。
	public Map<String, Object> setDeviceShutdown(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setTurnoffDevice(deviceSn);
		} else {
			responseMsg = gatewayHandler.set1014(deviceSn);
		}

		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_success"));
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "300");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
		return mapResponse;
	}

	// 设置找设备（找手表）关机,消息ID：0x1015。
	public Map<String, Object> setDeviceSearch(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");
		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}
		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setFindDevice(deviceSn);
		} else {
			responseMsg = gatewayHandler.set1015(deviceSn);
		}

		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_success"));
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "300");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
		return mapResponse;

	}

	// 获取设备监听号码
	public Map<String, Object> getDeviceMonitoringPhone(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getDeviceMonitoringPhone");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getDeviceMonitoringPhone异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		Map<String, Object> deviceMap = daoUtil.getDeviceInfoByDevicesn(deviceSn);
		String center_mobile = "";
		if (deviceMap != null) {
			center_mobile = deviceMap.get("center_mobile") != null ? deviceMap.get("center_mobile").toString() : "";
		}
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("mobile", center_mobile);
		mapResponse.put("code", "0");
		mapResponse.put("ret", returnMap);
		mapResponse.put("what", null);
		return mapResponse;
	}

	// 获取设备信息（获取本地或者跨服务器设备信息）
	public Map<String, Object> getLocalOrRemoteDeviceInfo(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getLocalOrRemoteDeviceInfo");
					param.put("deviceSn", deviceSn);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getLocalOrRemoteDeviceInfo异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/
		// 访问本地数据库查询信息
		Map<String, Object> deviceMap = daoUtil.getDeviceInfoByDevicesn(deviceSn);
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("deviceMap", deviceMap);
		mapResponse.put("code", "0");
		mapResponse.put("ret", returnMap);
		mapResponse.put("what", null);
		return mapResponse;
	}

	// 设置设备计步开关
	public Map<String, Object> setDeviceStep(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String step = DataUtil.getStringFromMap(params, "step");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		int deviceStep = Integer.parseInt(step);
		CastelMessage responseMsg = gatewayHandler.set1017(deviceSn, deviceStep);
		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			// 修改设备计步设置
			daoUtil.updateDeviceStep(deviceSn, deviceStep);

			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "set.device.step.success"));
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "300");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "set.device.step.fail"));
		return mapResponse;
	}

	// 查询设备计步开关
	public Map<String, Object> getDeviceStep(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getDeviceStep");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getLocalOrRemoteDeviceInfo异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 访问本地数据库查询信息
		int step = daoUtil.getDeviceStep(deviceSn);
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("step", step);
		mapResponse.put("code", "0");
		mapResponse.put("ret", returnMap);
		mapResponse.put("what", null);
		return mapResponse;
	}

	// 设置APN
	public Map<String, Object> setDeviceAPN(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String name = DataUtil.getStringFromMap(params, "name");
		String user_name = DataUtil.getStringFromMap(params, "user_name");
		String password = DataUtil.getStringFromMap(params, "password");
		String user_data = DataUtil.getStringFromMap(params, "user_data");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		CastelMessage responseMsg = gatewayHandler.set1016(deviceSn, name, user_name, password, user_data);
		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			// 修改设备APN
			daoUtil.updateDeviceAPN(deviceSn, name, user_name, password, user_data);

			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "save.info.success"));
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "300");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "save.info.fail"));
		return mapResponse;
	}

	// 查询APN
	public Map<String, Object> getDeviceAPN(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getLocalOrRemoteDeviceInfo");
					param.put("deviceSn", deviceSn);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getLocalOrRemoteDeviceInfo异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 访问本地数据库查询信息
		List<Map<String, Object>> deviceAPN = daoUtil.getDeviceAPN(deviceSn);
		Map<String, Object> returnMap = new HashMap<String, Object>();
		if (null != deviceAPN && deviceAPN.size() > 0) {
			returnMap.put("deviceAPN", deviceAPN);
			mapResponse.put("code", "0");
			mapResponse.put("ret", returnMap);
			mapResponse.put("what", null);
			return mapResponse;
		} else {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}

	}

	// 设置Wifi
	public Map<String, Object> setDeviceWifi(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String name = DataUtil.getStringFromMap(params, "name");
		String password = DataUtil.getStringFromMap(params, "password");
		String isSetGw = DataUtil.getStringFromMap(params, "isSetGw");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 只调用数据库
		if (null != isSetGw && isSetGw.equals("0")) {
			daoUtil.updateDeviceWifi(deviceSn, name, password);
			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "save.info.success"));
			return mapResponse;
		} else {
			CastelMessage responseMsg = gatewayHandler.set1018(deviceSn, name, password);
			if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
					&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {

				daoUtil.updateDeviceWifi(deviceSn, name, password);

				mapResponse.put("code", "0");
				mapResponse.put("ret", null);
				mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "save.info.success"));
				return mapResponse;
			} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
					&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
				mapResponse.put("code", "300");
				mapResponse.put("ret", null);
				mapResponse.put("what",
						LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
								+ LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
				return mapResponse;
			}
		}

		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "save.info.fail"));
		return mapResponse;

	}

	// 查询Wifi
	public Map<String, Object> getDeviceWifi(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		List<Map<String, Object>> deviceWifi = daoUtil.getDeviceWifi(deviceSn);
		Map<String, Object> returnMap = new HashMap<String, Object>();

		returnMap.put("deviceWifi", deviceWifi);
		mapResponse.put("code", "0");
		mapResponse.put("ret", returnMap);
		mapResponse.put("what", null);
		return mapResponse;
	}

	/**
	 * 添加聊天成员
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> addGroupUser(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String name = DataUtil.getStringFromMap(params, "name");

		Map<String, Object> map = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (null == username || name == "") {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		try {
			// 将用户加入指定群组，用户将可以收到该群的消息，同一用户最多可加入 500 个群，每个群最大至 3000 人。

			String[] groupJoinUserId = name.split(",");
			CodeSuccessResult groupJoinResult = RongCloud.getInstance(appKey, appSecret).group.join(groupJoinUserId,
					deviceSn, deviceSn);
			if (groupJoinResult.getCode() == 200) {// 成功
				// TODO:ZSH 远程同步数据到注册服务器
				Map<String, String> param = new HashMap<String, String>();
				param.put("function", "addGroupUser");
				param.put("names", name);
				param.put("deviceSn", deviceSn);

				RPCResult rpcResult = regConsumerService.hanleRegService("getDeviceInfoCN", param);

				if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
						|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
					map.put("code", "100");
					map.put("ret", "");
					map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.addGroupUser_fail"));
					return map;
				}
			}

			sendPbSysInfoNtf(deviceSn, groupJoinUserId, "1");

			showInfoNtf(deviceSn, name, LanguageManager.getMsg(headers.get("accept-language"), "common.in_group"));

		} catch (Exception e) {
			LogManager.error("远程addGroupUser异常：" + e.getMessage());

			map.put(ReturnObject.RETURN_CODE, "1100");
			map.put(ReturnObject.RETURN_OBJECT, null);
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.addGroupUser_fail"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.addGroupUser_success"));
		return map;

	}

	/***
	 * 获取全部群组成员
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getGroupUser(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> map = new HashMap<>();
		Map<String, String> param = new HashMap<String, String>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (null == username) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}
		try {
			param.put("deviceSn", deviceSn);
			RPCResult rpcResult = regConsumerService.hanleRegService("getGroupUserForLiteG", param);

			if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
					|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
				map.put("code", "100");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.getGroupUser_fail"));
				return map;
			}

			map.put("code", JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString());
			map.put("ret", JSON.parseObject(rpcResult.getRpcResult().toString()).get("data"));
			map.put("what", "");
			return map;

		} catch (Exception e) {
			LogManager.error("远程getGroupUser异常：" + e.getMessage());

			map.put(ReturnObject.RETURN_CODE, "1100");
			map.put(ReturnObject.RETURN_OBJECT, null);
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.getGroupUser_fail"));
			return map;
		}
	}

	/***
	 * 获取单个群组成员
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getGroupUserByName(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String name = DataUtil.getStringFromMap(params, "name");

		Map<String, Object> map = new HashMap<>();
		Map<String, String> param = new HashMap<String, String>();

		try {
			param.put("deviceSn", deviceSn);
			param.put("name", name);
			RPCResult rpcResult = regConsumerService.hanleRegService("getGroupUserByNameForLiteG", param);

			if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
					|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
				map.put("code", "100");
				map.put("ret", "");
				map.put("what",
						LanguageManager.getMsg(headers.get("accept-language"), "common.getGroupUserByName_fail"));
				return map;
			}

			map.put("code", JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString());
			map.put("ret", JSON.parseObject(rpcResult.getRpcResult().toString()).get("data"));
			map.put("what", "");
			return map;

		} catch (Exception e) {
			LogManager.error("远程getGroupUserByName异常：" + e.getMessage());

			map.put(ReturnObject.RETURN_CODE, "1100");
			map.put(ReturnObject.RETURN_OBJECT, null);
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.getGroupUserByName_fail"));
			return map;
		}
	}

	/**
	 * 删除群组成员
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> deleteGroupUser(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String name = DataUtil.getStringFromMap(params, "name");

		Map<String, Object> map = new HashMap<>();

		Map<String, String> param = new HashMap<String, String>();

		if (null == name || name == "") {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}

		try {

			String[] groupQuitUserId = name.split(",");
			CodeSuccessResult groupQuitResult = RongCloud.getInstance(appKey, appSecret).group.quit(groupQuitUserId,
					deviceSn);
			System.out.println("quit:  " + groupQuitResult.toString());

			if (groupQuitResult.getCode() == 200) {// 成功

				sendPbSysInfoNtf(deviceSn, groupQuitUserId, "0");
				showInfoNtf(deviceSn, name, LanguageManager.getMsg(headers.get("accept-language"), "common.out_group"));

				param.put("function", "deleteGroupUser");
				param.put("deviceSn", deviceSn);
				param.put("name", name);

				RPCResult rpcResult = regConsumerService.hanleRegService("deleteGroupUser", param);
				if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
						|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
					map.put(ReturnObject.RETURN_CODE, "100");
					map.put(ReturnObject.RETURN_OBJECT, "");
					map.put(ReturnObject.RETURN_WHAT,
							LanguageManager.getMsg(headers.get("accept-language"), "common.delete_group_user_fail"));
					return map;
				}

			}

		} catch (Exception e) {
			LogManager.error("远程getGroupUser异常：" + e.getMessage());

			map.put(ReturnObject.RETURN_CODE, "1100");
			map.put(ReturnObject.RETURN_OBJECT, "");
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.delete_group_user_fail"));
			return map;
		}

		map.put("code", "0");
		map.put("ret", "");
		map.put("what", LanguageManager.getMsg(headers.get("accept-language"), "common.delete_group_user_success"));
		return map;

	}

	/**
	 * 查询设备可邀请授权用户
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getInviteAuthUser(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> map = new HashMap<>();
		Map<String, String> param = new HashMap<String, String>();

		try {
			param.put("function", "getInviteAuthUser");
			param.put("deviceSn", deviceSn);

			RPCResult rpcResult = regConsumerService.hanleRegService("getInviteAuthUser", param);
			if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
					|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
				map.put("code", "100");
				map.put("ret", "");
				map.put("what",
						LanguageManager.getMsg(headers.get("accept-language"), "common.get_inviteauthuser_fail"));
				return map;
			}

			map.put("code", JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString());
			map.put("ret", JSON.parseObject(rpcResult.getRpcResult().toString()).get("data"));
			map.put("what", "");
			return map;

		} catch (Exception e) {
			LogManager.error("远程getGroupUser异常：" + e.getMessage());

			map.put(ReturnObject.RETURN_CODE, "1100");
			map.put(ReturnObject.RETURN_OBJECT, null);
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.get_inviteauthuser_fail"));
			return map;
		}
	}

	/**
	 * 设置用户信息
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> setUserInfo(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String nickname = DataUtil.getStringFromMap(params, "nickname");
		String sex = DataUtil.getStringFromMap(params, "sex");
		String age = DataUtil.getStringFromMap(params, "age");
		String area = DataUtil.getStringFromMap(params, "area");
		String mark = DataUtil.getStringFromMap(params, "mark");

		Map<String, Object> map = new HashMap<>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (null == username) {
			map.put(ReturnObject.RETURN_CODE, "100");
			map.put(ReturnObject.RETURN_OBJECT, "");
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return map;
		}
		if (nickname != null && !nickname.isEmpty()) {
			try {

				{// 成功
					// TODO:ZSH 远程同步数据到注册服务器
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "updateUserInfo");
					param.put("username", username);
					param.put("nickname", nickname);
					param.put("portrait", "");

					RPCResult rpcResult = regConsumerService.hanleRegService("updateUserInfo", param);
					if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS || !JSON
							.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {
						map.put(ReturnObject.RETURN_CODE, "100");
						map.put(ReturnObject.RETURN_OBJECT, "");
						map.put(ReturnObject.RETURN_WHAT,
								LanguageManager.getMsg(headers.get("accept-language"), "common.set_user_info_fail"));
						return map;
					}
				}

			} catch (Exception e) {
				LogManager.error("远程setUserInfo异常：" + e.getMessage());

				map.put(ReturnObject.RETURN_CODE, "1100");
				map.put(ReturnObject.RETURN_OBJECT, null);
				map.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.set_user_info_fail"));
				return map;
			}
		}

		if (!daoUtil.updateUserInfo(username, nickname, sex, age, area, mark)) {
			map.put(ReturnObject.RETURN_CODE, "300");
			map.put(ReturnObject.RETURN_OBJECT, null);
			map.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.set_user_info_fail"));
		}
		map.put(ReturnObject.RETURN_CODE, "0");
		map.put(ReturnObject.RETURN_OBJECT, "");
		map.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "common.set_user_info_success"));
		return map;
	}

	/**
	 * 获取用户信息
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getUserInfo(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		Map<String, Object> mapRet = new HashMap<>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}
		Map<String, Object> userInfo = daoUtil.getUserInfo(username);
		mapRet.put("userInfo", userInfo);

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, mapRet);
		mapResponse.put(ReturnObject.RETURN_WHAT,
				LanguageManager.getMsg(headers.get("accept-language"), "common.get_user_info_success"));
		return mapResponse;
	}

	/**
	 * 发送文本消息
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> sendMessageTxt(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String msg = DataUtil.getStringFromMap(params, "msg"); // 消息内容
		String type = DataUtil.getStringFromMap(params, "type"); // 0
																	// APP发送给设备，1设备发送给APP
		Map<String, Object> map = new HashMap<>();

		try {
			if (type.equals("0")) {// APP发送给设备
				String recDeviceSn = DataUtil.getStringFromMap(params, "recDeviceSn");// 接收消息设备号（APP调必传，网关调不传）

				Object obj = getdeviceserverandusername(recDeviceSn);
				if (obj == null) {
					map.put(ReturnObject.RETURN_CODE, "200");
					map.put(ReturnObject.RETURN_OBJECT, null);
					map.put(ReturnObject.RETURN_WHAT, null);
					return map;
				} else {
					String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
					String localServerIpAddress = env.getProperty("localServerIpAddress");

					// 通知网关
					CastelMessage responseMsg = null;
					if (!localServerIpAddress.equals(deviceServerIpAddress)) {// 设备不在当前服务器
						responseMsg = gatewayHttpHandler.sendTxtMessage(recDeviceSn, msg, deviceServerIpAddress);

					} else {// 设备在当前服务器
						responseMsg = gatewayHttpHandler.sendTxtMessage(recDeviceSn, msg, null);
					}
					if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
							&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
						map.put(ReturnObject.RETURN_CODE, "0");
						map.put(ReturnObject.RETURN_OBJECT, "");
						map.put(ReturnObject.RETURN_WHAT, null);
						return map;
					} else {
						map.put(ReturnObject.RETURN_CODE, "300");
						map.put(ReturnObject.RETURN_OBJECT, null);
						map.put(ReturnObject.RETURN_WHAT, null);
						return map;
					}
				}

			} else {// 设备发送给APP
				String sendDeviceSn = DataUtil.getStringFromMap(params, "sendDeviceSn"); // 发送消息设备号（APP调不传，网关调必传）
				String[] messagePublishGroupToGroupId = { sendDeviceSn };

				// 发送群组消息方法（以一个用户身份向群组发送消息，单条消息最大 128k.每秒钟最多发送 20 条消息，每次最多向 3
				// 个群组发送，如：一次向 3 个群组发送消息，示为 3 条消息。）
				TxtMessage messagePublishGroupTxtMessage = new TxtMessage(msg, null);
				CodeSuccessResult messagePublishGroupResult = RongCloud.getInstance(appKey, appSecret).message
						.publishGroup(sendDeviceSn, messagePublishGroupToGroupId, messagePublishGroupTxtMessage, null,
								"{\"pushData\":\"\"}", 1, 1, 0);
				System.out.println("publishGroup:  " + messagePublishGroupResult.toString());
				if (messagePublishGroupResult.getCode() == 200) {
					map.put(ReturnObject.RETURN_CODE, "0");
					map.put(ReturnObject.RETURN_OBJECT, "");
					map.put(ReturnObject.RETURN_WHAT, null);
					return map;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			LogManager.error("sendMessageTxt异常：" + e.getMessage());

			map.put(ReturnObject.RETURN_CODE, "400");
			map.put(ReturnObject.RETURN_OBJECT, null);
			map.put(ReturnObject.RETURN_WHAT, null);
			return map;
		}
		map.put(ReturnObject.RETURN_CODE, "500");
		map.put(ReturnObject.RETURN_OBJECT, null);
		map.put(ReturnObject.RETURN_WHAT, null);
		return map;
	}

	/***
	 * 解绑注册服务器和删除群聊 供家庭业务平台解绑调用
	 * 
	 * @return
	 */
	public Map<String, Object> unbound(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		CodeSuccessResult groupDismissResult;
		Map<String, Object> map = new HashMap<>();

		try {
			String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
			groupDismissResult = RongCloud.getInstance(appKey, appSecret).group.dismiss(deviceSn, deviceSn);
			if (groupDismissResult.getCode() == 200) {
				Map<String, String> param1 = new HashMap<String, String>();
				param1.put("function", "deleteGroup");
				param1.put("deviceSn", deviceSn);

				RPCResult rpcResult = regConsumerService.hanleRegService("deleteGroup", param1);

				Map<String, String> param = new HashMap<String, String>();
				param.put("function", "deletedevice");
				param.put("deviceSn", "'" + deviceSn + "'");

				RPCResult rpcDeResult = regConsumerService.hanleRegService("deletedevice", param);

				if (rpcResult.getRpcErrCode() != Errors.ERR_SUCCESS
						|| !JSON.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")
						|| rpcDeResult.getRpcErrCode() != Errors.ERR_SUCCESS
						|| !JSON.parseObject(rpcDeResult.getRpcResult().toString()).get("code").equals("0")) {
					map.put("code", "100");
					map.put("ret", "");
					map.put("what", "");
					return map;
				}

				map.put("code", JSON.parseObject(rpcDeResult.getRpcResult().toString()).get("code"));
				map.put("ret", JSON.parseObject(rpcDeResult.getRpcResult().toString()).get("data"));
				map.put("what", "");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	/**
	 * 保存基站信息
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> setLatlng(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		String latlng = DataUtil.getStringFromMap(params, "latlng");
		Map<String, Object> mapResponse = new HashMap<String, Object>();

		// latlng = latlng;

		String bgl_code = "0";
		String lngWay = "0";
		String latWay = "0";
		String type = "1";
		String lat = null;
		String lng = null;

		// String latlng =
		// "{\"BSBean\":{\"cid\":45613547,\"lac\":42336,\"mcc\":460},\"latitude\":22.547451666666667,\"longitude\":113.93651333333334,\"wifiList\":[{\"mac\":\"88:25:93:10:34:38\"},{\"mac\":\"52:bd:5f:73:74:b1\"},{\"mac\":\"24:05:0f:44:15:40\"},{\"mac\":\"64:09:80:67:27:6d\"}]}";
		JSONObject obj = null;
		ArrayList List = new ArrayList();

		obj = JSONObject.parseObject(latlng);

		if (obj.containsKey("latitude") && obj.get("latitude") != null) {
			lat = obj.get("latitude").toString();
		}

		if (obj.containsKey("longitude") && obj.get("longitude") != null) {
			lng = obj.get("longitude").toString();
		}

		if (obj.containsKey("BSBean") && obj.get("BSBean") != null) {
			JSONObject objB = JSONObject.parseObject(obj.get("BSBean").toString());
			List.add(Long.parseLong(objB.get("cid").toString()));
			List.add(Long.parseLong(objB.get("lac").toString()));
			List.add(Long.valueOf(objB.get("mcc").toString(), 16));
		}

		if (obj.containsKey("wifiList") && obj.getJSONArray("wifiList") != null) {
			JSONArray jsonArray = obj.getJSONArray("wifiList");
			for (Object curobj : jsonArray) {
				if (curobj != null) {
					JSONObject curJsonObj = (JSONObject) curobj;
					if (curJsonObj.containsKey("mac") && curJsonObj.get("mac") != null) {
						String mac = curJsonObj.get("mac").toString();
						String[] str = mac.split(":");
						StringBuffer sb = new StringBuffer();
						for (int i = str.length - 1; i >= 0; i--) {
							sb.append(str[i]);
						}
						String s = sb.toString();
						List.add(Long.valueOf(s, 16));
					}
				}
			}
		}

		Collections.sort(List);
		StringBuffer sb = new StringBuffer();

		for (Iterator iter = List.iterator(); iter.hasNext();) {
			sb.append(iter.next().toString());
			sb.append("-");
		}
		String latlngKey = sb.toString();

		daoUtil.saveLatlng(latlngKey, bgl_code, lat, lng, latWay, lngWay, type);
		// for(Iterator iter = List.iterator(); iter.hasNext(); ) {
		// System.out.println("next is: "+ iter.next());
		// }

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, "");
		mapResponse.put(ReturnObject.RETURN_WHAT, "");

		return mapResponse;

	}

	/**
	 * 获取里程油耗时长数据
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getDriveDataCount(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String startTime = DataUtil.getStringFromMap(params, "startTime");
		Map<String, Object> mapResponse = new HashMap<>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getDriveDataCount");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("starTtime", startTime);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getDriveDataCount异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}
		// 无此设备
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (deviceID <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
			return mapResponse;
		}
		// 未绑定此设备
		if (!daoUtil.hasOperatingAuthority(deviceID, userId)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "500");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.illegal_operation"));
			return mapResponse;
		}

		// 时间转换
		Date startDate = UtilDate.getDate(startTime, UtilDate.simple);
		int timezone = daoUtil.getUserTimezone(userId);

		Calendar c = Calendar.getInstance();
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();

		for (int i = 0; i < 12; i++) {
			c.setTime(startDate);

			c.add(Calendar.MONTH, -i);
			c.set(Calendar.DAY_OF_MONTH, 1);
			c.set(Calendar.HOUR_OF_DAY, 0);
			c.set(Calendar.MINUTE, 0);
			c.set(Calendar.SECOND, 0);

			String sDate = UtilDate.getUTCDateString(c.getTime(), timezone);

			String dateSmall = UtilDate.getDateSmall(c.getTime());

			c.setTime(startDate);

			c.add(Calendar.MONTH, -i);
			c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
			c.set(Calendar.HOUR_OF_DAY, 23);
			c.set(Calendar.MINUTE, 59);
			c.set(Calendar.SECOND, 59);

			String eDate = UtilDate.getUTCDateString(c.getTime(), timezone);

			float sumMileage;
			float sumFuel;
			float sumTime;

			sumMileage = daoUtil.getSumMileage(deviceSn, sDate, eDate);
			sumFuel = daoUtil.getSumFuel(deviceSn, sDate, eDate);
			sumTime = daoUtil.getSumTime(deviceSn, sDate, eDate);

			DecimalFormat df = new DecimalFormat("#.##");

			Map<String, Object> driveData = new HashMap<String, Object>();

			driveData.put("date", dateSmall);
			driveData.put("km", df.format(sumMileage / 1000));
			driveData.put("fuel", df.format(sumFuel));
			driveData.put("time", sumTime);
			if (sumFuel == 0) {
				driveData.put("mpg", 0);
			} else {
				driveData.put("mpg", df.format(sumMileage / 1000 / 100 / sumFuel));
			}

			list.add(driveData);

		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, list);
		mapResponse.put(ReturnObject.RETURN_WHAT, "");
		return mapResponse;
	}

	/**
	 * 设置情景模式(HT-990)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> setDeviceProfile(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String name = DataUtil.getStringFromMap(params, "name");
		String profile = DataUtil.getStringFromMap(params, "mode");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		int deviceProfile = Integer.parseInt(profile);
		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");

		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setProfile(deviceSn, profile);
		} else {
			responseMsg = gatewayHandler.set1021(deviceSn, deviceProfile);
		}

		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {

			daoUtil.updateDeviceProfile(deviceSn, deviceProfile);

			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "set.device.profile.success"));
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "300");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(headers.get("accept-language"), "set.device.profile.fail"));
		return mapResponse;
	}

	/**
	 * 查询情景模式(HT-990)
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getDeviceProfile(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getDeviceProfile");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getDeviceProfile异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 访问本地数据库查询信息
		int mode = daoUtil.getDeviceProfile(deviceSn);
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("mode", mode);
		mapResponse.put("code", "0");
		mapResponse.put("ret", returnMap);
		mapResponse.put("what", null);
		return mapResponse;
	}

	/**
	 * 设置吃药提醒
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> setMedicationRemind(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		int index = (DataUtil.getStringFromMap(params, "index") != null
				&& !DataUtil.getStringFromMap(params, "index").toString().equals(""))
						? Integer.parseInt(DataUtil.getStringFromMap(params, "index").toString()) : 1;
		String time = DataUtil.getStringFromMap(params, "time");
		String message = DataUtil.getStringFromMap(params, "message");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		Map<Integer, Object> remindmap = new HashMap<Integer, Object>();

		remindmap.put(1, time);
		remindmap.put(2, index);

		if (message.equals("")) {
			remindmap.put(3, " ");
		} else {
			remindmap.put(3, message);
		}

		remindmap.put(4, ""); // 语音不设置

		// 下发指令
		CastelMessage responseMsg = gatewayHandler.set1022(deviceSn, remindmap);
		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			// 查询数据库
			daoUtil.updateMedicationRemind(deviceSn, time, index, message);

			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "set.device.medication.remind.success"));
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "100");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "set.device.remind.fail"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what",
				LanguageManager.getMsg(headers.get("accept-language"), "set.device.medication.remind.fail"));
		return mapResponse;
	}

	/**
	 * 获取吃药提醒
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getMedicationRemind(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getMedicationRemind");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getMedicationRemind异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		List<Map<String, Object>> remindMap = daoUtil.getMedicationRemind(deviceSn);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("remindMap", remindMap);
		mapResponse.put("code", "0");
		mapResponse.put("ret", map);
		mapResponse.put("what", null);
		return mapResponse;
	}

	/**
	 * 获取周边商户
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getAroundStore(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String storeType = DataUtil.getStringFromMap(params, "storeType");
		String longitude = DataUtil.getStringFromMap(params, "longitude");
		String latitude = DataUtil.getStringFromMap(params, "latitude");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getAroundStore");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("storeType", storeType);
					param.put("longitude", longitude);
					param.put("latitude", latitude);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getAroundStore异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		Map<String, Double> pointMap = DistanceUtil.LLSquareMaxMinPoint(Double.parseDouble(longitude),
				Double.parseDouble(latitude), Double.parseDouble(env.getProperty("search.radius")));

		Double maxLat = pointMap.get("maxLatitude");
		Double minLat = pointMap.get("minLatitude");
		Double maxLon = pointMap.get("maxLongitude");
		Double minLon = pointMap.get("minLongitude");

		List<Map<String, Object>> storeMap = daoUtil.getAroundStore(storeType, maxLat, minLat, maxLon, minLon);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("storeMap", storeMap);
		mapResponse.put("code", "0");
		mapResponse.put("ret", map);
		mapResponse.put("what", null);
		return mapResponse;
	}

	/**
	 * 获取设备续费定制类型
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getDeviceCustomized(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT,
						LanguageManager.getMsg(headers.get("accept-language"), "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getDeviceCustomized");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getDeviceCustomized异常" + e.getMessage());

			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		Map<String, Object> customizedMap = daoUtil.getDeviceCustomized(deviceSn);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("map", customizedMap);
		mapResponse.put("code", "0");
		mapResponse.put("ret", map);
		mapResponse.put("what", null);
		return mapResponse;
	}

	/**
	 * 获取设备续费定制类型
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getDeviceAtHomePage(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String username = DataUtil.getStringFromMap(params, "username");
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		String webapth = env.getProperty("webPath");

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.find_username_failed"));
			return mapResponse;
		}

		int n = PropertyUtil.ordinalIndexOf(webapth, "/", 3);
		String webAddress = webapth.substring(0, n);

		List<Map<String, Object>> customizedMap = daoUtil.getDeviceAtHomePage(username, webAddress);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("map", customizedMap);
		mapResponse.put("code", "0");
		mapResponse.put("ret", map);
		mapResponse.put("what", null);
		return mapResponse;
	}

	/**
	 * 保险业务数据同步接口 参数用加密
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> setHumanInsurance(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) throws Exception {
		String data = DataUtil.getStringFromMap(params, "data");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		data = data.replace(' ', '+');

		String orderNum = null;
		String insured = null;
		String amount = null;
		String orderTime = null;
		String email = null;

		if (keyPair == null) {
			LogManager.info("must get public key first!");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "must get key first");
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			return mapResponse;
		}

		LogManager.info("Encrypt data:" + data);

		try {
			RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

			{
				// String privateKeyStr = EncryptRSA.encryptBASE64(((Key)
				// privateKey).getEncoded());
				// LogManager.info("privateKey:" +privateKeyStr);
			}

			byte[] resultBytes1 = EncryptRSA.decryptBASE64(data);

			EncryptRSA rsa = new EncryptRSA();
			// 用私钥解密
			byte[] decBytes = rsa.decrypt(privateKey, resultBytes1);

			String decryptMsg = new String(decBytes, "UTF-8");
			LogManager.info("ClearText data:" + decryptMsg);

			JSONObject json = JSONObject.parseObject(decryptMsg);
			;
			if (json.containsKey("orderNum")) {
				orderNum = json.getString("orderNum");
			}
			if (json.containsKey("insured")) {
				insured = json.getString("insured");
			}
			if (json.containsKey("amount")) {
				amount = json.getString("amount");
			}
			if (json.containsKey("orderTime")) {
				orderTime = json.getString("orderTime");
			}
			if (json.containsKey("email")) {
				email = json.getString("email");
			}
		} catch (ClassCastException e) {
			e.printStackTrace();
			LogManager.info("ClearText format error!");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "ClearText format error!");
			mapResponse.put(ReturnObject.RETURN_CODE, "300");
			return mapResponse;
		} catch (Exception e) {
			e.printStackTrace();
			LogManager.info("decrypt error!");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "decrypt error!");
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			return mapResponse;
		}

		LogManager.info("setHumanInsurance decrypt orderNum:" + orderNum + " insured:" + insured + " amount:" + amount
				+ " orderTime:" + orderTime + " email:" + email);

		if (orderNum == null) {
			mapResponse.put(ReturnObject.RETURN_OBJECT, "parameter error!");
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			return mapResponse;
		}

		if (daoUtil.setHumanInsurance(orderNum, email, insured, amount, orderTime)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "0");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			return mapResponse;
		}

		mapResponse.put(ReturnObject.RETURN_OBJECT, "");
		mapResponse.put(ReturnObject.RETURN_CODE, "300");

		return mapResponse;

	}

	/**
	 * 获取广告
	 * 
	 * @param request
	 * @return
	 */
	public Map<String, Object> getAdvertising(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		String pageCode = DataUtil.getStringFromMap(params, "page_code");

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);

		String currentUTCtime = DateTimeUtil.getCurrentUtcDatetime();
		int timezone = daoUtil.getUserTimezone(daoUtil.getUserId(username));
		String currentTime = null;

		try {
			currentTime = DateTimeUtil.utc2Local(currentUTCtime, timezone);
		} catch (ParseException e) {
			e.printStackTrace();
		}

		int userCustomizedApp = Integer.parseInt(daoUtil.getUserCustomizedApp(username));
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("map", daoUtil.getAdvertising(pageCode, currentTime, userCustomizedApp));

		mapResponse.put(ReturnObject.RETURN_OBJECT, map);
		mapResponse.put(ReturnObject.RETURN_WHAT, "");
		mapResponse.put(ReturnObject.RETURN_CODE, "0");

		return mapResponse;

	}

	/**
	 * 获取加密公钥
	 * 
	 * @param request
	 * @return
	 * @throws Exception
	 */
	public Map<String, Object> getPublicKey(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody)
			throws Exception {

		if (keyPair == null) {
			// KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象
			KeyPairGenerator keyPairGen;
			keyPairGen = KeyPairGenerator.getInstance("RSA");
			// 初始化密钥对生成器，密钥大小为2048位
			keyPairGen.initialize(2048);
			// 生成一个密钥对，保存在keyPair中
			keyPair = keyPairGen.generateKeyPair();
		}

		// 得到公钥
		RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();

		Key key = publicKey;

		String publicKeyStr = EncryptRSA.encryptBASE64(key.getEncoded());
		LogManager.info("publicKey data:" + publicKeyStr);

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		mapResponse.put(ReturnObject.RETURN_OBJECT, publicKeyStr);
		mapResponse.put(ReturnObject.RETURN_CODE, "0");

		return mapResponse;

	}

	/**
	 * 
	 */
	public boolean addRemind(String username, String title, String weekly, String monthly, String yearly, String monday,
			String tuesday, String wednesday, String thursday, String friday, String saturday, String sunday,
			String specificYear, String specificMonth, String specificDay, String isEnd, String diabolo) {

		int uid = daoUtil.getUserId(username);

		if (daoUtil.addRemind(uid, title, weekly, monthly, yearly, monday, tuesday, wednesday, thursday, friday,
				saturday, sunday, specificYear, specificMonth, specificDay, isEnd)) {

			int remindId = daoUtil.getMaxRemindID();
			String[] times = diabolo.split(",");
			for (String time : times) {
				daoUtil.addRemindTime(remindId, time);
			}
			return true;
		}
		return false;
	}

	public boolean modifyRemind(String id, String title, String weekly, String monthly, String yearly, String monday,
			String tuesday, String wednesday, String thursday, String friday, String saturday, String sunday,
			String specificYear, String specificMonth, String specificDay, String isEnd, String diabolo) {

		if (daoUtil.modifyRemind(id, title, weekly, monthly, yearly, monday, tuesday, wednesday, thursday, friday,
				saturday, sunday, specificYear, specificMonth, specificDay, isEnd, diabolo)) {

			String[] times = diabolo.split(",");
			int remindID = Integer.parseInt(id);
			daoUtil.deleteRemindTime(remindID);
			for (String time : times) {
				daoUtil.addRemindTime(remindID, time);
			}
			return true;
		}
		return false;
	}

	public Map<String, Object> getRemind(int userID) {
		Map<String, Object> result = new HashMap<String, Object>();
		List<Map<String, Object>> listReminds = new ArrayList<Map<String, Object>>();
		List<Map<String, Object>> reminds = daoUtil.getRemind(userID);
		if (reminds.size() > 0) {
			for (Iterator iterator = reminds.iterator(); iterator.hasNext();) {
				Map<String, Object> map = (Map<String, Object>) iterator.next();
				int id = Integer.parseInt(String.valueOf(map.get("id")));
				map.put("diabolo", daoUtil.getRemindTime(id));
				listReminds.add(map);
			}
		}
		result.put("reminds", listReminds);
		return result;
	}

	public boolean setGeoFenceCN(String areaid, String deviceSn, String userID, String lat, String lng, String radius,
			String defencename, String defencestatus) {
		int deviceID = daoUtil.getDeviceID(deviceSn);
		areaid = daoUtil.getDeviceGeoFenceAreaid(deviceID); // 只能一个围栏
		if (PropertyUtil.isNotBlank(areaid)) {
			return daoUtil.updateGeoFenceCN(areaid, String.valueOf(deviceID), userID, lat, lng, radius, defencename,
					defencestatus);
		} else {
			return daoUtil.setGeoFenceCN(String.valueOf(deviceID), userID, lat, lng, radius, defencename,
					defencestatus);
		}
	}

	/**
	 * 发送系统消息
	 * 
	 * @param deviceSn
	 * @param groupQuitUserId
	 * @param type
	 */
	public void sendPbSysInfoNtf(String deviceSn, String[] groupUserId, String type) {
		try {
			CmdNtfMessage messagePublishSystemNtfMessage = new CmdNtfMessage("", "{\"type\":\"" + type + "\"}"); // type:1表示加入
																													// 0删除
			CodeSuccessResult messagePublishSystemResult = RongCloud.getInstance(appKey, appSecret).message
					.PublishSystem(deviceSn, groupUserId, messagePublishSystemNtfMessage, null, null, 0, 0);

			if (messagePublishSystemResult.getCode() == 200) {
				System.out.println("---sendPbSysInfoNtf Ok---");
			} else {
				System.out.println("---sendPbSysInfoNtf Fail---");
			}
		} catch (Exception e) {

		}
	}

	/**
	 * 加入,退出群组小灰条
	 * 
	 * @param deviceSn
	 * @param name
	 * @param info
	 * @return
	 */
	public boolean showInfoNtf(String deviceSn, String name, String info) {
		try {

			Map<String, String> param = new HashMap<String, String>();

			String infoMessage = name;
			String finalInfoMessage = "";

			if (name != null) {

				String[] names = name.split(",");
				int idx = 0;
				for (String string : names) {
					idx++;
					if (!string.isEmpty()) {
						param.put("function", "getGroupUserByName");
						param.put("deviceSn", deviceSn);
						param.put("name", string);
						RPCResult rpcResult = regConsumerService.hanleRegService("getGroupUserByName", param);

						if (rpcResult.getRpcErrCode() == Errors.ERR_SUCCESS && JSON
								.parseObject(rpcResult.getRpcResult().toString()).get("code").toString().equals("0")) {

							JSONObject obj = null;
							JSONArray jsonArray = null;
							if (null != rpcResult.getRpcResult()) {
								obj = JSONObject.parseObject(
										JSON.parseObject(rpcResult.getRpcResult().toString()).get("ret").toString());

								jsonArray = obj.getJSONArray("obj");
								for (Object curobj : jsonArray) {
									if (curobj != null) {
										JSONObject curJsonObj = (JSONObject) curobj;
										if (curJsonObj.containsKey("remark") && curJsonObj.get("remark") != null) {
											infoMessage = curJsonObj.get("remark").toString();
										} else if (curJsonObj.containsKey("nickname")
												&& curJsonObj.get("nickname") != null) {
											infoMessage = curJsonObj.get("nickname").toString();
										} else {
											infoMessage = string;
										}
									}
								}
							}
							if (idx != names.length) {
								finalInfoMessage += "\"" + infoMessage + "\",";
							} else {
								finalInfoMessage += "\"" + infoMessage + "\"";
							}
						} else {
							finalInfoMessage += null;
						}
					}
				}

				finalInfoMessage += info;

				String[] messagePublishGroupToGroupId = { deviceSn };
				InfoNtfMessage messagePublishGroupTxtMessage = new InfoNtfMessage(finalInfoMessage, null);
				CodeSuccessResult messagePublishGroupResult = RongCloud.getInstance(appKey, appSecret).message
						.publishGroup(deviceSn, messagePublishGroupToGroupId, messagePublishGroupTxtMessage, null,
								"{\"pushData\":\"\"}", 1, 1, 0);
				System.out.println("publishGroup:  " + messagePublishGroupResult.toString());
				if (messagePublishGroupResult.getCode() != 200) {
					return false;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * 公共方法
	 * 
	 * @param url
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public JSONObject doPost_urlconn(String url, Map<String, String> params) throws Exception {
		OutputStream outputStream = null;
		OutputStreamWriter outputStreamWriter = null;
		InputStream inputStream = null;
		InputStreamReader inputStreamReader = null;
		BufferedReader reader = null;

		StringBuffer resultBuffer = new StringBuffer();
		String tempLine = null;

		String AcceptLanguage = "";
		if (params.containsKey("AcceptLanguage") && params.get("AcceptLanguage") != null) {
			AcceptLanguage = params.get("AcceptLanguage");
			params.remove("AcceptLanguage");
		}

		try {
			String requestData = "";
			if (params != null && params.size() >= 1) {
				if (params.size() > 1) {
					for (String key : params.keySet()) {
						requestData += key + "=" + URLEncoder.encode(params.get(key), "UTF-8") + "&";
					}

					requestData = requestData.substring(0, requestData.length() - 1);
				} else {
					for (String key : params.keySet()) {
						requestData += key + "=" + URLEncoder.encode(params.get(key), "UTF-8");
					}
				}
			}

			URL req_url = new URL(url);
			URLConnection conn = req_url.openConnection();
			HttpURLConnection urlconn = (HttpURLConnection) conn;
			urlconn.setDoInput(true);
			urlconn.setDoOutput(true);

			urlconn.setRequestMethod("POST");
			urlconn.setRequestProperty("Accept-Charset", "utf-8");
			urlconn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
			urlconn.setRequestProperty("Content-Length", String.valueOf(requestData.length()));

			if (AcceptLanguage != "") {
				urlconn.setRequestProperty("Accept-Language", AcceptLanguage);
			}

			outputStream = urlconn.getOutputStream();
			outputStreamWriter = new OutputStreamWriter(outputStream);

			outputStreamWriter.write(requestData);
			outputStreamWriter.flush();

			if (urlconn.getResponseCode() >= 300) {
				throw new Exception("HTTP Request is not success, Response code is " + urlconn.getResponseCode());
			}

			inputStream = urlconn.getInputStream();
			inputStreamReader = new InputStreamReader(inputStream);
			reader = new BufferedReader(inputStreamReader);

			tempLine = reader.readLine();
			while (tempLine != null) {
				resultBuffer.append(tempLine);

				tempLine = reader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (outputStreamWriter != null) {
				outputStreamWriter.close();
			}

			if (outputStream != null) {
				outputStream.close();
			}

			if (reader != null) {
				reader.close();
			}

			if (inputStreamReader != null) {
				inputStreamReader.close();
			}

			if (inputStream != null) {
				inputStream.close();
			}
		}

		JSONObject json = JSONObject.parseObject(resultBuffer.toString());
		return json;
	}

	/**
	 * 防走失信息卡开始
	 * 
	 * @param deviceSn
	 * @param username
	 * @return
	 ***********************************************************************************************************************************/
	// 人
	public Map<String, Object> setHumanInfo(String deviceSn, String humanName, String humanSex, String humanAge,
			String humanHeight, String humanWeight, String humanStep, String humanFeature, String humanAddr,
			String mobile1, String mobile2, String mobile3, String humanLostAddr, String humanBirthday, String simNo,
			String language) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (!daoUtil.setDeviceInfo(deviceSn, humanName, mobile1, mobile2, mobile3, simNo)
				|| !setHumanInfo(deviceID, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
						humanFeature, humanAddr, humanLostAddr, humanBirthday)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_failed"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_success"));
		return mapResponse;
	}

	public boolean setHumanInfo(int deviceID, String humanName, String humanSex, String humanAge, String humanHeight,
			String humanWeight, String humanStep, String humanFeature, String humanAddr, String humanLostAddr,
			String humanBirthday) {
		if (daoUtil.hasHumanInfo(deviceID)) {
			return daoUtil.updateHumanInfo(deviceID, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
					humanFeature, humanAddr, humanLostAddr, humanBirthday);
		} else {
			return daoUtil.addHumanInfo(deviceID, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
					humanFeature, humanAddr, humanLostAddr, humanBirthday);
		}
	}

	public Map<String, Object> getHumanInfo(String deviceSn, int deviceID, String language) {
		Map<String, Object> humaninfo = daoUtil.getHumanInfo(deviceID);
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);
		Map<String, Object> gps = daoUtil.getCurrentGPS(deviceSn, timezone);

		if (gps != null) {
			humaninfo.put("lat", gps.get("lat"));
			humaninfo.put("lng", gps.get("lng"));
		} else {
			humaninfo.put("lat", "");
			humaninfo.put("lng", "");
		}

		String webapth = env.getProperty("webPath");
		String local = "CN";
		if (language != null && language.indexOf("-") != -1) {
			String[] localArray = language.split("\\-");
			if (localArray != null && localArray.length >= 1) {
				local = localArray[1].toUpperCase();
			}
		}
		String shareUrl = webapth + "lostcard_" + local + ".htm?params=" + Integer.toString(deviceID) + "&lang="
				+ language;
		humaninfo.put("shareUrl", shareUrl);

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, humaninfo);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.get_success"));
		return mapResponse;
	}

	// 狗
	public Map<String, Object> setPetInfo(String deviceSn, String pet_name, String pet_sex, String pet_breed,
			String pet_age, String pet_weight, String pet_feature, String pet_addr, String pet_lost_addr,
			String mobile1, String mobile2, String mobile3, String petBirthday, String simNo, String language) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (!daoUtil.setDeviceInfo(deviceSn, pet_name, mobile1, mobile2, mobile3, simNo) || !setPetInfo(deviceID,
				pet_name, pet_sex, pet_breed, pet_age, pet_weight, pet_feature, pet_addr, pet_lost_addr, petBirthday)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_failed"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_success"));
		return mapResponse;
	}

	public boolean setPetInfo(int deviceID, String pet_name, String pet_sex, String pet_breed, String pet_age,
			String pet_weight, String pet_feature, String pet_addr, String pet_lost_addr, String pet_birthday) {
		if (daoUtil.hasHumanInfo(deviceID)) {
			return daoUtil.updatePetInfo(deviceID, pet_name, pet_sex, pet_breed, pet_age, pet_weight, pet_feature,
					pet_addr, pet_lost_addr, pet_birthday);
		} else {
			return daoUtil.addPetInfo(deviceID, pet_name, pet_sex, pet_breed, pet_age, pet_weight, pet_feature,
					pet_addr, pet_lost_addr, pet_birthday);
		}
	}

	public Map<String, Object> getPetInfo(String deviceSn, int deviceID, String language) {
		Map<String, Object> petinfo = daoUtil.getPetInfo(deviceID);
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);
		Map<String, Object> gps = daoUtil.getCurrentGPS(deviceSn, timezone);

		if (gps != null) {
			petinfo.put("lat", gps.get("lat"));
			petinfo.put("lng", gps.get("lng"));
		} else {
			petinfo.put("lat", "");
			petinfo.put("lng", "");
		}

		String webapth = env.getProperty("webPath");
		String local = "CN";
		if (language != null && language.indexOf("-") != -1) {
			String[] localArray = language.split("\\-");
			if (localArray != null && localArray.length >= 1) {
				local = localArray[1].toUpperCase();
			}
		}
		String shareUrl = webapth + "lostcard_" + local + ".htm?params=" + Integer.toString(deviceID) + "&lang="
				+ language;
		petinfo.put("shareUrl", shareUrl);

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, petinfo);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.get_success"));
		return mapResponse;
	}

	// 车
	public Map<String, Object> setCarInfo(String deviceSn, String nickName, String car_no, String car_vin,
			String car_engin, String car_set, String car_brand, String car_year, String car_type, String car_oil_type,
			String mobile1, String mobile2, String mobile3, String car_mileage, String car_check_time,
			String car_buytime, String simNo, String language) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (!daoUtil.setDeviceInfo(deviceSn, nickName, mobile1, mobile2, mobile3, simNo)
				|| !setCarInfo(deviceID, car_no, car_vin, car_engin, car_set, car_brand, car_year, car_type,
						car_oil_type, car_mileage, car_check_time, car_buytime)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_failed"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_success"));
		return mapResponse;
	}

	public boolean setCarInfo(int deviceID, String car_no, String car_vin, String car_engin, String car_set,
			String car_brand, String car_year, String car_type, String car_oil_type, String car_mileage,
			String car_check_time, String car_buytime) {
		if (daoUtil.hasHumanInfo(deviceID)) {
			return daoUtil.updateCarInfo(deviceID, car_no, car_vin, car_engin, car_set, car_brand, car_year, car_type,
					car_oil_type, car_mileage, car_check_time, car_buytime);
		} else {
			return daoUtil.addCarInfo(deviceID, car_no, car_vin, car_engin, car_set, car_brand, car_year, car_type,
					car_oil_type, car_mileage, car_check_time, car_buytime);
		}
	}

	public Map<String, Object> getCarInfo(int deviceID, String language) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		Map<String, Object> carinfo = daoUtil.getCarInfo(deviceID);

		String webapth = env.getProperty("webPath");
		String local = "CN";
		if (language != null && language.indexOf("-") != -1) {
			String[] localArray = language.split("\\-");
			if (localArray != null && localArray.length >= 1) {
				local = localArray[1].toUpperCase();
			}
		}
		String shareUrl = webapth + "lostcard_" + local + ".htm?params=" + Integer.toString(deviceID) + "&lang="
				+ language;
		carinfo.put("shareUrl", shareUrl);

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, carinfo);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.get_success"));
		return mapResponse;
	}

	// 摩托
	public Map<String, Object> setMotoInfo(String deviceSn, String nickName, String motor_no, String moto_type,
			String motor_cc, String motor_trademark, String motor_set, String motor_year, String mobile1,
			String mobile2, String mobile3, String motor_buytime, String simNo, String language) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (!daoUtil.setDeviceInfo(deviceSn, nickName, mobile1, mobile2, mobile3, simNo) || !setMotoInfo(deviceID,
				motor_no, moto_type, motor_cc, motor_trademark, motor_set, motor_year, motor_buytime)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_failed"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_success"));
		return mapResponse;
	}

	public boolean setMotoInfo(int deviceID, String motor_no, String moto_type, String motor_cc, String motor_trademark,
			String motor_set, String motor_year, String motor_buytime) {
		if (daoUtil.hasHumanInfo(deviceID)) {
			return daoUtil.updateMotoInfo(deviceID, motor_no, moto_type, motor_cc, motor_trademark, motor_set,
					motor_year, motor_buytime);
		} else {
			return daoUtil.addMotoInfo(deviceID, motor_no, moto_type, motor_cc, motor_trademark, motor_set, motor_year,
					motor_buytime);
		}
	}

	public Map<String, Object> getMotoInfo(int deviceID, String language) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		Map<String, Object> motoinfo = daoUtil.getMotoInfo(deviceID);

		String webapth = env.getProperty("webPath");
		String local = "CN";
		if (language != null && language.indexOf("-") != -1) {
			String[] localArray = language.split("\\-");
			if (localArray != null && localArray.length >= 1) {
				local = localArray[1].toUpperCase();
			}
		}
		String shareUrl = webapth + "lostcard_" + local + ".htm?params=" + Integer.toString(deviceID) + "&lang="
				+ language;
		motoinfo.put("shareUrl", shareUrl);

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, motoinfo);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.get_success"));
		return mapResponse;
	}

	// 手表
	public Map<String, Object> setWatchInfo(String deviceSn, String humanName, String humanSex, String humanAge,
			String humanHeight, String humanWeight, String humanStep, String humanFeature, String humanAddr,
			String mobile1, String mobile2, String mobile3, String humanLostAddr, String humanBirthday, String simNo,
			String language) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (!daoUtil.setDeviceInfo(deviceSn, humanName, mobile1, mobile2, mobile3, simNo)
				|| !setWatchInfo(deviceID, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
						humanFeature, humanAddr, humanLostAddr, humanBirthday)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_failed"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_success"));
		return mapResponse;
	}

	public boolean setWatchInfo(int deviceID, String humanName, String humanSex, String humanAge, String humanHeight,
			String humanWeight, String humanStep, String humanFeature, String humanAddr, String humanLostAddr,
			String humanBirthday) {
		if (daoUtil.hasHumanInfo(deviceID)) {
			return daoUtil.updateWatchInfo(deviceID, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
					humanFeature, humanAddr, humanLostAddr, humanBirthday);
		} else {
			return daoUtil.addWatchInfo(deviceID, humanName, humanSex, humanAge, humanHeight, humanWeight, humanStep,
					humanFeature, humanAddr, humanLostAddr, humanBirthday);
		}
	}

	public Map<String, Object> getWatchInfo(String deviceSn, int deviceID, String language) {
		Map<String, Object> humaninfo = daoUtil.getWatchInfo(deviceID);
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);
		Map<String, Object> gps = daoUtil.getCurrentGPS(deviceSn, timezone);

		if (gps != null) {
			humaninfo.put("lat", gps.get("lat"));
			humaninfo.put("lng", gps.get("lng"));
		} else {
			humaninfo.put("lat", "");
			humaninfo.put("lng", "");
		}

		String webapth = env.getProperty("webPath");
		String local = "CN";
		if (language != null && language.indexOf("-") != -1) {
			String[] localArray = language.split("\\-");
			if (localArray != null && localArray.length >= 1) {
				local = localArray[1].toUpperCase();
			}
		}
		String shareUrl = webapth + "lostcard_" + local + ".htm?params=" + Integer.toString(deviceID) + "&lang="
				+ language;
		humaninfo.put("shareUrl", shareUrl);

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, humaninfo);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.get_success"));
		return mapResponse;
	}

	// obd
	public Map<String, Object> setObdInfo(String nickName, String mobile1, String mobile2, String mobile3,
			String deviceSn, String obd_no, String obd_type, String obd_buytime, String car_vin, String simNo,
			String language) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (!daoUtil.setDeviceInfo(deviceSn, nickName, mobile1, mobile2, mobile3, simNo)
				|| !setObdInfo(deviceID, obd_no, obd_type, obd_buytime, car_vin)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_failed"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_success"));
		return mapResponse;
	}

	public Map<String, Object> setBluetoothWatchInfo(String deviceSn, String humanName, String humanSex,
			String humanHeight, String humanWeight, String humanBirthday, String mobile1, String mobile2,
			String mobile3, String humanFeature, String language) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		int deviceID = daoUtil.getDeviceID(deviceSn);
		if (!daoUtil.setDeviceInfo(deviceSn, humanName, mobile1, mobile2, mobile3) || !setBluetoothWatchInfo(deviceID,
				humanName, humanSex, humanHeight, humanWeight, humanBirthday, humanFeature)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_failed"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.save_success"));
		return mapResponse;
	}

	public boolean setBluetoothWatchInfo(int deviceID, String humanName, String humanSex, String humanHeight,
			String humanWeight, String humanBirthday, String humanFeature) {
		if (daoUtil.hasHumanInfo(deviceID)) {
			return daoUtil.updateBluetoothWatchInfo(deviceID, humanName, humanSex, humanHeight, humanWeight,
					humanBirthday, humanFeature);
		} else {
			return daoUtil.addBluetoothWatchInfo(deviceID, humanName, humanSex, humanHeight, humanWeight, humanBirthday,
					humanFeature);
		}
	}

	public Map<String, Object> getBluetoothWatchInfo(String deviceSn, int deviceID, String language) {
		Map<String, Object> humaninfo = daoUtil.getBluetoothWatchInfo(deviceID);
		int timezone = daoUtil.getDeviceSuperUserTimezone(deviceSn);
		Map<String, Object> gps = daoUtil.getCurrentGPS(deviceSn, timezone);

		if (gps != null) {
			humaninfo.put("lat", gps.get("lat"));
			humaninfo.put("lng", gps.get("lng"));
		} else {
			humaninfo.put("lat", "");
			humaninfo.put("lng", "");
		}

		String webapth = env.getProperty("webPath");
		String local = "CN";
		if (language != null && language.indexOf("-") != -1) {
			String[] localArray = language.split("\\-");
			if (localArray != null && localArray.length >= 1) {
				local = localArray[1].toUpperCase();
			}
		}
		String shareUrl = webapth + "lostcard_" + local + ".htm?params=" + Integer.toString(deviceID) + "&lang="
				+ language;
		humaninfo.put("shareUrl", shareUrl);

		Map<String, Object> mapResponse = new HashMap<String, Object>();
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, humaninfo);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.get_success"));
		return mapResponse;
	}

	public boolean setObdInfo(int deviceID, String obd_no, String obd_type, String obd_buytime, String car_vin) {
		if (daoUtil.hasHumanInfo(deviceID)) {
			return daoUtil.updateObdInfo(deviceID, obd_no, obd_type, obd_buytime, car_vin);
		} else {
			return daoUtil.addObdInfo(deviceID, obd_no, obd_type, obd_buytime, car_vin);
		}
	}

	public Map<String, Object> getObdInfo(int deviceID, String language) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();
		Map<String, Object> obdinfo = daoUtil.getObdInfo(deviceID);

		String webapth = env.getProperty("webPath");
		String local = "CN";
		if (language != null && language.indexOf("-") != -1) {
			String[] localArray = language.split("\\-");
			if (localArray != null && localArray.length >= 1) {
				local = localArray[1].toUpperCase();
			}
		}
		String shareUrl = webapth + "lostcard_" + local + ".htm?params=" + Integer.toString(deviceID) + "&lang="
				+ language;
		obdinfo.put("shareUrl", shareUrl);

		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, obdinfo);
		mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "info.get_success"));
		return mapResponse;
	}

	/******* 防走失信息开完 **************************************************************************************************/

	/**
	 * 设备拍照
	 * @param deviceSn
	 * @param mode
	 * @param request
	 * @return
	 */
	public Map<String, Object> setDevicePhoto(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String mode = DataUtil.getStringFromMap(params, "mode");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.user_notexist"));
			return mapResponse;
		}

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(headers.get("accept-language"), "common.not_superuser"));
			return mapResponse;
		}

		// 通知网关
		CastelMessage responseMsg = gatewayHttpHandler.setImg(deviceSn);

		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			mapResponse.put("code", "0");
			mapResponse.put("ret", "");
			mapResponse.put("what", "");
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "300");
			mapResponse.put("ret", "");
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}
		mapResponse.put("code", "400");
		mapResponse.put("ret", "");
		mapResponse.put("what",
				LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_1"));
		return mapResponse;

	}

	/**
	 * 获取设备拍照图片
	 * @param deviceSn
	 * @param request
	 * @return
	 */
	public Map<String, Object> getDevicePhoto(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (!TextUtils.isEmpty(remoteServerRequest)) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getDevicePhoto");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", username);
					param.put("deviceSn", deviceSn);
					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getDevicePhoto异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(language, "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.user_notexist"));
			return mapResponse;
		}

		// 访问本地数据库查询信息
		List<Map<String, Object>> imgMap = daoUtil.getDevicePhoto(deviceSn);
		Map<String, Object> returnMap = new HashMap<String, Object>();

		returnMap.put("imgMap", imgMap);
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, returnMap);
		mapResponse.put(ReturnObject.RETURN_WHAT, null);
		return mapResponse;

	}

	/**
	 * 删除设备拍照图片
	 * @param remindID
	 * @param request
	 * @return
	 */
	public Map<String, Object> deleteDevicePhoto(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {

		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String devicePhotoID = DataUtil.getStringFromMap(params, "devicePhotoID");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.not_superuser"));
			return mapResponse;
		}

		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.user_notexist"));
			return mapResponse;
		}

		if (!daoUtil.deleteDevicePhoto(devicePhotoID)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "400");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(language, "common.softwareupgrad_status_1"));
			return mapResponse;
		}
		mapResponse.put(ReturnObject.RETURN_CODE, "0");
		mapResponse.put(ReturnObject.RETURN_OBJECT, null);
		mapResponse.put(ReturnObject.RETURN_WHAT, "");
		return mapResponse;
	}

	/**
	 * 设备重启
	 * @param deviceSn
	 * @param request
	 * @return
	 */
	public Map<String, Object> setDeviceRestart(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.not_superuser"));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.user_notexist"));
			return mapResponse;
		}

		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setRebootDevice(deviceSn);
		} else {
			responseMsg = gatewayHandler.set1014(deviceSn);
		}

		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what", LanguageManager.getMsg(language, "common.send_order_success"));
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "300");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(language, "common.send_order_failed"));
		return mapResponse;
	}

	/**
	 * 添加修改电话本联系人(单个添加)
	 * @param deviceSn
	 * @param phoneNum
	 * @param nickname
	 * @param index
	 * @param photo
	 * @param isAdmin
	 * @param request
	 * @return
	 */
	public Map<String, Object> addOrModifyContact(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String phoneNum = DataUtil.getStringFromMap(params, "phoneNum");
		String nickname = DataUtil.getStringFromMap(params, "nickname");
		String index = DataUtil.getStringFromMap(params, "index");
		String photo = DataUtil.getStringFromMap(params, "photo");
		String isAdmin = DataUtil.getStringFromMap(params, "isAdmin");

		int adminindex = 0;

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.no_such_device"));
			return map;
		}

		if (TextUtils.isEmpty(phoneNum) || TextUtils.isEmpty(nickname) || TextUtils.isEmpty(photo)) {
			map.put("code", "700");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.param_error"));
			return map;
		}

		Map<Integer, Object> phonemap = new HashMap<Integer, Object>();

		List<Map<String, Object>> phoneList = daoUtil.getDeviceContactToDevice(deviceSn);
		if (null == index) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("contact", nickname + ":" + phoneNum);
			m.put("photo", photo);
			phoneList.add(m);
		} else {
			Map<String, Object> m = daoUtil.getDeviceContactByID(index);
			if (null != m) {
				for (Map<String, Object> phoneMap : phoneList) {
					if (phoneMap.get("id").toString().equals(index)) {
						phoneMap.put("contact", nickname + ":" + phoneNum);
						phoneMap.put("photo", photo);
					}
				}
			}
		}
		Map<String, Object> flagMap = new HashMap<String, Object>();
		flagMap.put("flag", "1");
		phoneList.add(flagMap);

		CastelMessage responseMsg = null;
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);

		// 通知网关
		if (gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setPhoneBook(deviceSn, phoneList);
		} else {
			responseMsg = gatewayHandler.set100A(deviceSn, phonemap);
		}

		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			boolean addOrmodify;
			if (TextUtils.isEmpty(index)) {
				addOrmodify = daoUtil.addDeviceContact(deviceSn, nickname, phoneNum, photo);
			} else {
				addOrmodify = daoUtil.modifyDeviceContact(index, nickname, phoneNum, photo);
			}

			if (!addOrmodify) {
				map.put("code", "500");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(language, "common.add_phone_fail"));
				return map;
			} else {
				// 保存电话本号码前三个为紧急联系人
				daoUtil.updateurgencytel("", "", "", deviceSn);
				Map<String, Object> deviceMap = daoUtil.getDeviceInfoByDevicesn(deviceSn);
				if (adminindex != 0 && deviceMap != null && deviceMap.get("protocol_type") != null
						&& deviceMap.get("protocol_type").toString().equals("1")) {
					map.put("code", "0");
					map.put("ret", "");
					map.put("what", LanguageManager.getMsg(language, "common.add_phone_success"));
					return map;
				} else {

					Map<String, Object> m = new HashMap<String, Object>();
					if (TextUtils.isEmpty(index)) {
						m.put("contactMap", daoUtil.getDeviceContactLast(deviceSn));
					} else {
						m.put("contactMap", daoUtil.getDeviceContactByID(index));
					}
					map.put("code", "0");
					map.put("ret", m);
					map.put("what", "");
					return map;
				}
			}
		} else {
			map.put("code", "600");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.send_order_failed"));
			return map;
		}
	}

	/**
	 * 获取联系人
	 * @param deviceSn
	 * @param request
	 * @return
	 */
	public Map<String, Object> getContact(Map<String, String[]> params, Map<String, String> headers, byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (remoteServerRequest != "" && remoteServerRequest != null) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				map.put("code", "300");
				map.put("ret", null);
				map.put("what", LanguageManager.getMsg(language, "common.no_such_device"));
				return map;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");

				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getContact");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);

					Map<String, Object> mapResponse = new HashMap<String, Object>();
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());

					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getphonebook异常" + e.getMessage());

			map.put("code", "1100");
			map.put("ret", null);
			map.put("what", LanguageManager.getMsg(language, "common.package_info_not_exist"));
			return map;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.user_notexist"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.no_such_device"));
			return map;
		}

		List<Map<String, Object>> phoneList = daoUtil.getDeviceContact(deviceSn);

		if (phoneList == null || phoneList.isEmpty()) {
			map.put("code", "500");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.phonebook_no_data"));
			return map;
		}
		Map<String, Object> m = new HashMap<String, Object>();
		m.put("contactMap", phoneList);

		map.put("code", "0");
		map.put("ret", m);
		map.put("what", LanguageManager.getMsg(language, "common.phonebook_data_success"));
		return map;
	}

	/**
	 * 删除联系人
	 * @param deviceSn
	 * @param index
	 * @param request
	 * @return
	 */
	public Map<String, Object> deleteContact(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> map = new HashMap<>();
		int adminindex = 0;

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String index = DataUtil.getStringFromMap(params, "index");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		if (!PropertyUtil.isNotBlank(username)) {
			map.put("code", "100");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.find_username_failed"));
			return map;
		}

		int userID = daoUtil.getUserId(username);
		if (userID <= 0) {
			map.put("code", "200");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.user_notexist"));
			return map;
		}

		if (!daoUtil.isSuperUser(username, deviceSn)) {
			map.put("code", "400");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.not_superuser"));
			return map;
		}

		int trackerID = daoUtil.getDeviceID(deviceSn);
		if (trackerID <= 0) {
			map.put("code", "300");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.no_such_device"));
			return map;
		}

		if (TextUtils.isEmpty(index)) {
			map.put("code", "700");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.param_error"));
			return map;
		}

		Map<Integer, Object> phonemap = new HashMap<Integer, Object>();

		List<Map<String, Object>> phoneList = daoUtil.getDeviceContactToDevice(deviceSn);

		Map<String, Object> m = daoUtil.getDeviceContactByID(index);
		if (null != m) {

			Iterator<Map<String, Object>> iterator = phoneList.iterator();
			while (iterator.hasNext()) {
				if (iterator.next().get("id").toString().equals(index)) {
					iterator.remove();
				}
			}

		}

		Map<String, Object> flagMap = new HashMap<String, Object>();
		flagMap.put("flag", "1");
		phoneList.add(flagMap);

		CastelMessage responseMsg = null;
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);

		// 通知网关
		if (gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setPhoneBook(deviceSn, phoneList);
		} else {
			responseMsg = gatewayHandler.set100A(deviceSn, phonemap);
		}

		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			if (!daoUtil.deleteDeviceContact(index)) {
				map.put("code", "500");
				map.put("ret", "");
				map.put("what", LanguageManager.getMsg(language, "common.softwareupgrad_status_1"));
				return map;
			} else {
				// 保存电话本号码前三个为紧急联系人
				daoUtil.updateurgencytel("", "", "", deviceSn);
				Map<String, Object> deviceMap = daoUtil.getDeviceInfoByDevicesn(deviceSn);
				if (adminindex != 0 && deviceMap != null && deviceMap.get("protocol_type") != null
						&& deviceMap.get("protocol_type").toString().equals("1")) {
					map.put("code", "0");
					map.put("ret", "");
					map.put("what", LanguageManager.getMsg(language, "common.add_phone_success"));
					return map;
				} else {
					map.put("code", "0");
					map.put("ret", "");
					map.put("what", "");
					return map;
				}
			}
		} else {
			map.put("code", "600");
			map.put("ret", "");
			map.put("what", LanguageManager.getMsg(language, "common.send_order_failed"));
			return map;
		}
	}

	/**
	 * 获取设备步数多天
	 * @param deviceSn
	 * @param startDate
	 * @param endDate
	 * @param request
	 * @return
	 */
	public Map<String, Object> getDeviceSteps(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");
		String startDate = DataUtil.getStringFromMap(params, "startDate");
		String endDate = DataUtil.getStringFromMap(params, "endDate");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (remoteServerRequest != "" && remoteServerRequest != null) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, null);
				mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getDeviceSteps");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);
					param.put("startDate", startDate);
					param.put("endDate", endDate);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getDeviceSteps异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(language, "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.find_username_failed"));
			return mapResponse;
		}

		// 访问本地数据库查询信息
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("stepMap", daoUtil.getDeviceSteps(deviceSn, startDate, endDate));
		mapResponse.put("code", "0");
		mapResponse.put("ret", returnMap);
		mapResponse.put("what", null);
		return mapResponse;

	}

	/**
	 * 获取设备最后一天步数
	 * @param deviceSn
	 * @param startDate
	 * @param endDate
	 * @param request
	 * @return
	 */
	public Map<String, Object> getDeviceLastStep(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		/****** 增加访问远程接口代码 开始 ***********/
		String remoteServerRequest = DataUtil.getStringFromMap(params, "remoteServerRequest"); // 远程服务器访问参数
		if (remoteServerRequest != "" && remoteServerRequest != null) {
			username = DataUtil.getStringFromMap(params, "username");
		}

		try {
			Object obj = getdeviceserverandusername(deviceSn);
			if (obj == null) {
				mapResponse.put(ReturnObject.RETURN_CODE, "300");
				mapResponse.put(ReturnObject.RETURN_OBJECT, "");
				mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.no_such_device"));
				return mapResponse;
			} else {
				String deviceServerIpAddress = ((JSONObject) obj).getString("conn_name");
				String localServerIpAddress = env.getProperty("localServerIpAddress");
				if (!localServerIpAddress.equals(deviceServerIpAddress)) {
					String userName = ((JSONObject) obj).getString("name");

					Map<String, String> param = new HashMap<String, String>();
					param.put("function", "getDeviceLastStep");
					param.put("remoteServerRequest", "true");
					param.put("AcceptLanguage", language);
					param.put("username", userName);
					param.put("deviceSn", deviceSn);

					String requestPath = env.getProperty("remotingServer_ApiPath_" + deviceServerIpAddress)
							+ EnumUtils.Version.THREE;
					JSONObject json = doPost_urlconn(requestPath, param);
					mapResponse.put("remotingReturn", true);
					mapResponse.put("result", json.toJSONString());
					return mapResponse;
				}
			}
		} catch (Exception e) {
			LogManager.error("远程访问getDeviceSteps异常" + e.getMessage());
			mapResponse.put(ReturnObject.RETURN_CODE, "1100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT,
					LanguageManager.getMsg(language, "common.package_info_not_exist"));
			return mapResponse;
		}
		/****** 增加访问远程接口代码 结束 ***********/

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.find_username_failed"));
			return mapResponse;
		}

		// 访问本地数据库查询信息
		Map<String, Object> returnMap = new HashMap<String, Object>();
		returnMap.put("stepMap", daoUtil.getDeviceLastStep(deviceSn));
		mapResponse.put("code", "0");
		mapResponse.put("ret", returnMap);
		mapResponse.put("what", "");
		return mapResponse;

	}

	/**
	 * 远程录音
	 * @param deviceSn
	 * @param request
	 * @return
	 */
	public Map<String, Object> setDeviceRecord(Map<String, String[]> params, Map<String, String> headers,
			byte[] reqBody) {
		Map<String, Object> mapResponse = new HashMap<String, Object>();

		String deviceSn = DataUtil.getStringFromMap(params, "deviceSn");

		String sessionID = headers.get("sessionID");
		String username = Session.getUser(sessionID);
		String language = headers.get("accept-language");

		// 非超级用户不能操作
		if (!daoUtil.isSuperUser(username, deviceSn)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "100");
			mapResponse.put(ReturnObject.RETURN_OBJECT, "");
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg("common.not_superuser", language));
			return mapResponse;
		}

		if (!PropertyUtil.isNotBlank(username)) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg(language, "common.find_username_failed"));
			return mapResponse;
		}

		// 用户不存在
		int userId = daoUtil.getUserId(username);
		if (userId <= 0) {
			mapResponse.put(ReturnObject.RETURN_CODE, "200");
			mapResponse.put(ReturnObject.RETURN_OBJECT, null);
			mapResponse.put(ReturnObject.RETURN_WHAT, LanguageManager.getMsg("common.user_notexist", language));
			return mapResponse;
		}

		Integer prtocoltype = daoUtil.getdeviceprtocoltype(deviceSn);
		String gatewayProtocol = env.getProperty("htdz_gateway_protocol");
		// 通知网关
		CastelMessage responseMsg = null;
		if (null != prtocoltype && gatewayProtocol.contains(prtocoltype.toString())) {
			responseMsg = gatewayHttpHandler.setTurnoffDevice(deviceSn);
		} else {
			responseMsg = gatewayHandler.set1014(deviceSn);
		}

		if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("0")) {
			mapResponse.put("code", "0");
			mapResponse.put("ret", null);
			mapResponse.put("what", LanguageManager.getMsg(language, "common.send_order_success"));
			return mapResponse;
		} else if (responseMsg != null && responseMsg.getMsgBodyMapVo().get(1) != null
				&& responseMsg.getMsgBodyMapVo().get(1).toString().equals("4")) {
			mapResponse.put("code", "300");
			mapResponse.put("ret", null);
			mapResponse.put("what",
					LanguageManager.getMsg(headers.get("accept-language"), "common.softwareupgrad_status_4") + ","
							+ LanguageManager.getMsg(headers.get("accept-language"), "common.send_order_failed"));
			return mapResponse;
		}
		mapResponse.put("code", "300");
		mapResponse.put("ret", null);
		mapResponse.put("what", LanguageManager.getMsg(language, "common.send_order_failed"));
		return mapResponse;
	}

}
