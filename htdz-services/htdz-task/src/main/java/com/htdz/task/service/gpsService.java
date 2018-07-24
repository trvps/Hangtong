package com.htdz.task.service;

import java.util.Map;

import com.htdz.common.LogManager;
import com.htdz.common.utils.PropertyUtil;
import com.htdz.gpsmessage.ConnGwBySocket;
import com.htdz.gpsmessage.ConnGwBySocket_Decoder;
import com.htdz.gpsmessage.ConnGwBySocket_Encoder;
import com.htdz.gpsmessage.GpsMessage;
import com.htdz.gpsmessage.GpsMessageHeadVO;
import com.htdz.gpsmessage.GpsMessage_0x1205;
import com.htdz.gpsmessage.GpsMessage_0x1206;

public class gpsService {
	private static short counter = 1;

	// 设置消息体 发送并接收消息
	private static GpsMessage sentMessage(GpsMessage gpsmessage) {
		GpsMessage returnCastelMessage = null;
		GpsMessageHeadVO msgHeadVo = gpsmessage.getMsgHeadVo();

		String config = PropertyUtil.getPropertyValue("application",
				"spring.profiles.active");
		String strIP = PropertyUtil.getPropertyValue("application-" + config,
				"GPSServerIP");
		String iPort = PropertyUtil.getPropertyValue("application-" + config,
				"GPSConnPort");

		ConnGwBySocket socket = new ConnGwBySocket();
		try {
			int connStatus = socket.Conn2GW(strIP, iPort);
			if (connStatus > 0) {
				LogManager.info("建立连接成功：设备号=" + msgHeadVo.getEquipId()+ "  消息流水=" + msgHeadVo.getMsgSquice());

				int sendStatus = socket.Send2GW(ConnGwBySocket_Encoder
						.encode(gpsmessage));
				if (sendStatus > 0) {
					LogManager.info("发送消息成功：设备号=" + msgHeadVo.getEquipId()
							+ "  消息流水=" + msgHeadVo.getMsgSquice());

					// 开始接受响应消息
					byte[] recMsg = socket.RcvFromGW();
					LogManager.info("接收消息长度" + Integer.toString(recMsg.length));

					socket.release();

					// 接受完成进行解析
					GpsMessage responseCastelMessage = ConnGwBySocket_Decoder
							.decoder(recMsg);

					LogManager
							.info("解析消息完成：消息内容是否为空"
									+ (responseCastelMessage == null ? "true"
											: "false"));

					// 进行比较确认该响应消息与请求消息对应
					String respEquipId = responseCastelMessage.getMsgHeadVo()
							.getEquipId();
					// short respMsgId =
					// responseCastelMessage.getMsgHeadVo().getMsgId();
					short respSquice = responseCastelMessage.getMsgHeadVo()
							.getMsgSquice();

					if (msgHeadVo.getEquipId().equals(respEquipId)
							&& msgHeadVo.getMsgSquice() == respSquice) {
						LogManager.info("进行响应消息筛选:");
						returnCastelMessage = responseCastelMessage;
					}
				} else {
					LogManager.info("发送消息失败：设备号=" + msgHeadVo.getEquipId()
							+ "  消息流水=" + msgHeadVo.getMsgSquice());
					socket.release();
				}
			}
		} catch (Exception e) {
			LogManager.error(e.getMessage());
			socket.release();
		}
		LogManager.info("返回响应消息:");

		return returnCastelMessage;
	}

	// 位置解析
	public static GpsMessage getGpsByWifiAndLbs(String equipId, String mapType,
			Map<Integer, Object> bodyVo) {
		GpsMessage gpsmessage = null;

		GpsMessageHeadVO msgHeadVo = new GpsMessageHeadVO();
		msgHeadVo.setEquipId(equipId);
		msgHeadVo.setMsgSquice(getCounter());
		msgHeadVo.setProtocolType((short) 1010);

		if (mapType.equals("google")) {
			msgHeadVo.setMsgId(Short.parseShort("1205", 16));
			gpsmessage = new GpsMessage_0x1205();
		}

		if (mapType.equals("amap")) {
			msgHeadVo.setMsgId(Short.parseShort("1206", 16));
			gpsmessage = new GpsMessage_0x1206();
		}

		if (gpsmessage != null) {
			gpsmessage.setMsgHeadVo(msgHeadVo);
			gpsmessage.setMsgBodyMapVo(bodyVo);

			return sentMessage(gpsmessage);
		} else {
			return null;
		}
	}

	private static short getCounter() {
		if (counter == 65536) {
			counter = 1;
		}
		return ++counter;
	}
}
