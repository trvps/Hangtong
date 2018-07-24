package com.htdz.liteguardian.message;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.htdz.liteguardian.util.EncodingUtil;

@Service
public class GatewayHttpHandler {

	@Value("${htdz.gateway.server.url}")
	private String url;

	// 2.2.2 设置电子围栏,消息ID：setEnclosure
	public CastelMessage setEnclosure(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			param.put("lat", str[1]);
			param.put("lng", str[2]);
			param.put("radius", str[3]);
			if (str.length > 7) {
				param.put("areaid", str[4]);
				param.put("type", str[5]);
				param.put("is_out", str[6]);
				param.put("enabled", str[7]);
			}

			String requestPath = url + "/setEnclosure";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.3 取消电子围栏,消息ID：deleteEnclosure
	public CastelMessage deleteEnclosure(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			String requestPath = url + "/deleteEnclosure";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return getCastelMessage(json);
	}

	// 2.2.4 GPS定位时间间隔设置,消息ID：setGpsInterval
	public CastelMessage setGpsInterval(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			param.put("gpsinterval", str[1]);
			String requestPath = url + "/setGpsInterval";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.5 获取最后设置最后一次上传的位置信息,消息ID：getLastGps
	public CastelMessage getLastGps(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			String requestPath = url + "/getLastGps";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.6 设备点名,消息ID：deviceNaming
	public CastelMessage deviceNaming(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			String requestPath = url + "/deviceNaming";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.7 设备重置,消息ID：deviceReset
	public CastelMessage deviceReset(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			String requestPath = url + "/deviceReset";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	public CastelMessage bindingDevice(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			param.put("optype", str[1]);
			String requestPath = url + "/bindingDevice";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.17 设置电话本信息到远程,消息ID：setPhoneBook
	public CastelMessage setPhoneBook(String str, Map<Integer, Object> map) {
		JSONObject json = null;
		try {

			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str);
			param.put("tel1", map.get(1).toString());
			param.put("tel2", map.get(2).toString());
			param.put("tel3", map.get(3).toString());
			param.put("tel4", map.get(4).toString());
			param.put("tel5", map.get(5).toString());
			param.put("tel6", map.get(6).toString());
			param.put("tel7", map.get(7).toString());
			param.put("tel8", map.get(8).toString());
			param.put("tel9", map.get(9).toString());
			param.put("tel10", map.get(10).toString());
			param.put("flag", map.get(11).toString());

			String requestPath = url + "/setPhoneBook";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.17 设置电话本信息到远程,消息ID:setPhoneBook List参数
	public CastelMessage setPhoneBook(String str, List<Map<String, Object>> list) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str);

			for (int i = 0; i < list.size() - 1; i++) {
				param.put("tel" + i, list.get(i).get("contact").toString());
			}

			param.put("flag", list.get(list.size() - 1).get("flag").toString());

			String requestPath = url + "/setPhoneBook";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.19 设置上课禁用信息到远程,消息ID：setCourseDisableTime
	public CastelMessage setCourseDisableTime(String str, Map<Integer, Object> map) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str);
			param.put("status", map.get(1).toString());
			param.put("starttime1", map.get(2).toString());
			param.put("endtime1", map.get(3).toString());
			param.put("starttime2", map.get(4).toString());
			param.put("endtime2", map.get(5).toString());
			param.put("repeat", map.get(6).toString());
			/*
			 * param.put("starttime3", map.get(7).toString());
			 * param.put("endtime3", map.get(8).toString());
			 */

			String requestPath = url + "/setCourseDisableTime";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.21 设置设备时区、语言到远程,消息ID：setTzAndLang
	public CastelMessage setTzAndLang(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			param.put("tz", str[1]);
			param.put("lang", str[2]);

			String requestPath = url + "/setTzAndLang";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.23 设置警情开关,消息ID：setAlarmSetting
	public CastelMessage setAlarmSetting(String str, Map<Integer, Object> map) {

		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str);
			param.put("boundary", map.get(1).toString());
			param.put("voltage", map.get(2).toString());
			param.put("tow", map.get(3).toString());
			param.put("clipping", map.get(4).toString());
			param.put("speed", map.get(5).toString());
			param.put("sos", map.get(6).toString());
			param.put("vibration", map.get(7).toString());
			param.put("vibrationAspeed", map.get(8).toString());
			param.put("vibrationTime", map.get(9).toString());
			param.put("takeOff", map.get(10).toString());

			String requestPath = url + "/setAlarmSetting";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.24 设置闹钟信息到远程,消息ID：setClock
	public CastelMessage setClock(String str, Map<Integer, Object> map) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str);
			param.put("clock1", map.get(1).toString());
			param.put("clock2", map.get(2).toString());
			param.put("clock3", map.get(3).toString());

			String requestPath = url + "/setClock";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);

	}

	// 2.2.25 设置监听,消息ID：setMonitor
	public CastelMessage setMonitor(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			param.put("centertel", str[1]);

			String requestPath = url + "/setMonitor";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);

	}

	// 2.2.26 设置远程关机,消息ID：setTurnoffDevice
	public CastelMessage setTurnoffDevice(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);

			String requestPath = url + "/setTurnoffDevice";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.27 设置找设备（找手表）关机,消息ID：set setFindDevice
	public CastelMessage setFindDevice(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);

			String requestPath = url + "/setFindDevice";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.31 发送文本消息,消息ID：sendTxtMessage
	public CastelMessage sendTxtMessage(String... str) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			param.put("txtmessage", str[1]);

			String requestPath = url + "/sendTxtMessage";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.32 发送语音消息,消息ID：sendVoiceMessage
	public CastelMessage sendVoiceMessage(String deviceSn, byte[] voicemessage) {
		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			String sendString = EncodingUtil.bytesToHexString(voicemessage);
			param.put("deviceSn", deviceSn);
			param.put("voicemessage", sendString);

			String requestPath = url + "/sendVoiceMessage";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return getCastelMessage(json);
	}

	// 2.2.33 设置情景模式,消息ID：setProfile
	public CastelMessage setProfile(String... str) {

		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);
			param.put("profile", str[1]);

			String requestPath = url + "/setProfile";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return getCastelMessage(json);
	}

	// 2.2.33 拍照,消息ID: setImg
	public CastelMessage setImg(String... str) {

		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);

			String requestPath = url + "/setImg";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return getCastelMessage(json);
	}

	// 2.2.35 重启,消息ID: setImg
	public CastelMessage setRebootDevice(String... str) {

		JSONObject json = null;
		try {
			Map<String, String> param = new HashMap<String, String>();
			param.put("deviceSn", str[0]);

			String requestPath = url + "/setRebootDevice";
			json = doPost_urlconn(requestPath, param);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return getCastelMessage(json);
	}

	private CastelMessage getCastelMessage(JSONObject json) {
		CastelMessage castelMessage = null;
		if (null != json && null != json.get("result")) {
			castelMessage = new CastelMessage();
			Map<Integer, Object> msgBodyMapVo = new HashMap<Integer, Object>();
			msgBodyMapVo.put(1, json.get("result"));
			castelMessage.setMsgBodyMapVo(msgBodyMapVo);
		}
		return castelMessage;
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
}