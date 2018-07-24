package com.htdz.liteguardian.message;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.htdz.common.LogManager;

@Service
public class GatewayHandler {
	// 修改为socket通讯时注释
	// private final SessionManager sessionManager =
	// SessionManager.getInstance();
	private short counter = 1;
	@Autowired
	private Environment env;

	// 设置消息体 发送并接收消息
	private CastelMessage sentMessage(CastelMessageHeadVO msgHeadVo, CastelMessage castelmessage,
			Map<Integer, Object>... map) {
		CastelMessage returnCastelMessage = null;
		castelmessage.setMsgHeadVo(msgHeadVo);

		String strIP = env.getProperty("gw.server.ip");
		int iPort = Integer.parseInt(env.getProperty("gw.server.port"));

		if (map.length > 1) {// 如果有2个map说明有传递目标服务器地址
			strIP = map[1].get(1).toString();
		}
		castelmessage.setMsgBodyMapVo(map[0]);

		// 修改为socket通讯时注释
		/*
		 * IoSession session = sessionManager.getIdleSession();
		 * session.getConfig().setUseReadOperation(true);
		 * 
		 * WriteFuture writeFuture = session.write(castelmessage);
		 * writeFuture.awaitUninterruptibly();
		 * 
		 * if (writeFuture.isWritten()) {
		 * 
		 * ReadFuture readFuture = session.read(); //阻塞方法，等待write写完，线程才继续往下进行
		 * if(readFuture.awaitUninterruptibly(100, TimeUnit.SECONDS)) { Object
		 * message = readFuture.getMessage(); CastelMessage
		 * responseCastelMessage=(CastelMessage)message;
		 * 
		 * String respEquipId =
		 * responseCastelMessage.getMsgHeadVo().getEquipId(); short respMsgId =
		 * responseCastelMessage.getMsgHeadVo().getMsgId(); short respSquice =
		 * responseCastelMessage.getMsgHeadVo().getMsgSquice();
		 * 
		 * if(msgHeadVo.getEquipId().equals(respEquipId) &&
		 * msgHeadVo.getMsgSquice()==respSquice) { returnCastelMessage =
		 * responseCastelMessage; } } }
		 */

		ConnGwBySocket socket = new ConnGwBySocket();
		try {
			int connStatus = socket.Conn2GW(strIP, iPort);
			if (connStatus > 0) {
				LogManager.info("建立连接成功：设备号=" + msgHeadVo.getEquipId() + "  消息流水=" + msgHeadVo.getMsgSquice());

				int sendStatus = socket.Send2GW(ConnGwBySocket_Encoder.encode(castelmessage));
				if (sendStatus > 0) {
					LogManager.info("发送消息成功：设备号=" + msgHeadVo.getEquipId() + "  消息流水=" + msgHeadVo.getMsgSquice());
					// 开始接受响应消息
					byte[] recMsg = socket.RcvFromGW();
					LogManager.info("接收消息长度" + Integer.toString(recMsg.length));

					socket.release();
					// 等待
					/*
					 * int waitresponsetime=Integer.parseInt(PropertyUtil.
					 * getPropertyValue("web", "waitresponsetime")); Calendar
					 * currenttime=Calendar.getInstance(); long
					 * startmillsecond=currenttime.getTimeInMillis(); long
					 * currentmillsecond=startmillsecond;
					 * 
					 * while((currentmillsecond-startmillsecond)<=(
					 * waitresponsetime *1000)) {
					 * currenttime=Calendar.getInstance();
					 * currentmillsecond=currenttime.getTimeInMillis(); }
					 */

					// 接受完成进行解析
					CastelMessage responseCastelMessage = ConnGwBySocket_Decoder.decoder(recMsg);

					LogManager.info("解析消息完成：消息内容是否为空" + (responseCastelMessage == null ? "true" : "false"));

					// 进行比较确认该响应消息与请求消息对应
					String respEquipId = responseCastelMessage.getMsgHeadVo().getEquipId();
					// short respMsgId =
					// responseCastelMessage.getMsgHeadVo().getMsgId();
					short respSquice = responseCastelMessage.getMsgHeadVo().getMsgSquice();
					if (msgHeadVo.getEquipId().equals(respEquipId) && msgHeadVo.getMsgSquice() == respSquice) {
						LogManager.info("进行响应消息筛选:");

						returnCastelMessage = responseCastelMessage;
					}
				} else {
					LogManager.info("发送消息失败：设备号=" + msgHeadVo.getEquipId() + "  消息流水=" + msgHeadVo.getMsgSquice());

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

	// (GPS位置解析)设置消息体 发送并接收消息
	private CastelMessage sentMessageForGps(CastelMessageHeadVO msgHeadVo, CastelMessage castelmessage,
			Map<Integer, Object> map) {
		CastelMessage returnCastelMessage = null;

		castelmessage.setMsgHeadVo(msgHeadVo);
		castelmessage.setMsgBodyMapVo(map);

		try {
			String strIP = env.getProperty("gps.server.ip");
			int iPort = Integer.parseInt(env.getProperty("gps.server.ip"));

			ConnGwBySocket socket = new ConnGwBySocket();
			int connStatus = socket.Conn2GW(strIP, iPort);
			if (connStatus > 0) {
				LogManager.info("建立连接成功：设备号=" + msgHeadVo.getEquipId() + "  消息流水=" + msgHeadVo.getMsgSquice());

				int sendStatus = socket.Send2GW(ConnGwBySocket_Encoder.encode(castelmessage));
				if (sendStatus > 0) {
					LogManager.info("发送消息成功：设备号=" + msgHeadVo.getEquipId() + "  消息流水=" + msgHeadVo.getMsgSquice());

					// 开始接受响应消息
					byte[] recMsg = socket.RcvFromGW();

					LogManager.info("接收消息长度" + Integer.toString(recMsg.length));

					socket.release();

					// 接受完成进行解析
					CastelMessage responseCastelMessage = ConnGwBySocket_Decoder.decoder(recMsg);

					LogManager.info("解析消息完成：消息内容是否为空" + (responseCastelMessage == null ? "true" : "false"));

					// 进行比较确认该响应消息与请求消息对应
					String respEquipId = responseCastelMessage.getMsgHeadVo().getEquipId();
					// short respMsgId =
					// responseCastelMessage.getMsgHeadVo().getMsgId();
					short respSquice = responseCastelMessage.getMsgHeadVo().getMsgSquice();
					if (msgHeadVo.getEquipId().equals(respEquipId) && msgHeadVo.getMsgSquice() == respSquice) {
						LogManager.info("进行响应消息筛选");

						returnCastelMessage = responseCastelMessage;

						LogManager.info("地理位置解析内容：" + responseCastelMessage.getMsgBodyMapVo().get(1).toString());
					}
				} else {
					LogManager.info("发送消息失败：设备号=" + msgHeadVo.getEquipId() + "  消息流水=" + msgHeadVo.getMsgSquice());

					socket.release();
				}
			}
		} catch (Exception e) {
			LogManager.error(e.getMessage());
		}

		LogManager.info("返回响应消息");

		return returnCastelMessage;
	}

	/**
	 * 推送警情到推送服务(1:成功，-1：失败)
	 * 
	 * @param msgHeadVo
	 * @param castelmessage
	 * @param map
	 * @return
	 */
	private int sentMessageToPushServer(CastelMessageHeadVO msgHeadVo, CastelMessage castelmessage,
			Map<Integer, Object> map) {
		castelmessage.setMsgHeadVo(msgHeadVo);
		castelmessage.setMsgBodyMapVo(map);

		try {
			String strIP = env.getProperty("push.server.ip");
			int iPort = Integer.parseInt(env.getProperty("push.server.ip"));

			ConnGwBySocket socket = new ConnGwBySocket();
			int connStatus = socket.Conn2GW(strIP, iPort);
			if (connStatus > 0) {
				LogManager.info("建立连接成功：设备号=" + msgHeadVo.getEquipId() + "  消息流水=" + msgHeadVo.getMsgSquice());

				int sendStatus = socket.Send2GW(ConnGwBySocket_Encoder.encode(castelmessage));
				if (sendStatus > 0) {
					LogManager.info("发送消息成功：设备号=" + msgHeadVo.getEquipId() + "  消息流水=" + msgHeadVo.getMsgSquice());
				} else {
					LogManager.info("发送消息失败：设备号=" + msgHeadVo.getEquipId() + "  消息流水=" + msgHeadVo.getMsgSquice());
				}

				socket.release();

				return sendStatus;
			}
		} catch (Exception e) {
			LogManager.error(e.getMessage());
		}

		return -1;
	}

	// 设置消息头
	private CastelMessageHeadVO setHead(String equipId, short msgId) {
		short protocolType = Short.parseShort("1010");
		short msgSquice = getCounter();

		CastelMessageHeadVO msgHeadVo = new CastelMessageHeadVO();
		msgHeadVo.setEquipId(equipId);
		msgHeadVo.setMsgId(msgId);
		msgHeadVo.setMsgSquice(msgSquice);
		msgHeadVo.setProtocolType(protocolType);
		return msgHeadVo;
	}

	// API调用
	// 设置电子围栏,消息ID：0x1001。
	public CastelMessage set1001(String equipId, String lat, String lng, int radius) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1001", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1001();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();

		bodyVo.put(1, lat);
		bodyVo.put(2, lng);
		bodyVo.put(3, radius);

		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	// API向网关设备发起取消电子围栏请求。
	public CastelMessage set1002(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1002", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1002();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1003(String equipId, Integer timeInterval) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1003", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1003();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, timeInterval);
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1004(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1004", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1004();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1005(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1005", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1005();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1006(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1006", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1006();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1007(String equipId, int operationType) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1007", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1007();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, operationType);
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1008(String equipId, int operationType) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1008", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1008();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, operationType);
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1009(String equipId, String targetVersion) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1009", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1009();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, targetVersion);
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1010(String equipId, int speed, int time) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1010", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1010();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, speed);
		bodyVo.put(2, time);
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1201(String equipId, String lat, String lng, String local) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1201", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1201();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();

		bodyVo.put(1, lng);
		bodyVo.put(2, lat);
		bodyVo.put(3, local);

		return sentMessageForGps(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set101A(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("101A", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x101A();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set101E(String equipId, String concatPhoneOne, String concatPhoneTwo,
			String concatPhoneThree) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("101E", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x101E();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, concatPhoneOne);
		bodyVo.put(2, concatPhoneTwo);
		bodyVo.put(3, concatPhoneThree);

		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1011(String equipId, Map<Integer, Object> bodyVo) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1011", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1011();

		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	/************************* 以下手表接入 ************************/
	public CastelMessage set100A(String equipId, Map<Integer, Object> bodyVo) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("100A", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x100A();

		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set100B(String equipId, Map<Integer, Object> bodyVo) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("100B", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x100B();

		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set100C(String equipId, Map<Integer, Object> bodyVo) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("100C", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x100C();

		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set100D(String equipId, Map<Integer, Object> bodyVo) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("100D", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x100D();

		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set100E(String equipId, int timezone, int language) {
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, timezone);
		// 设置760手表设备时区时，需要设置手表语言信息
		bodyVo.put(2, String.valueOf(language));
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("100E", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x100E();

		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set100F(String equipId, Map<Integer, Object> bodyVo) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("100F", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x100F();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public int set3002(String equipId, Map<Integer, Object> bodyVo) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("3002", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x3002();
		return sentMessageToPushServer(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1012(String equipId, Map<Integer, Object> bodyVo) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1012", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1012();

		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set2012(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("2012", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x2012();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1013(String equipId, String mobile) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1013", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1013();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, mobile);
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set2013(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("2013", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x2013();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1014(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1014", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1014();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set2014(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("2014", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x2014();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1015(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1015", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1015();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set2015(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("2015", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x2015();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1016(String equipId, String apn, String username, String password, String data) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1016", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1016();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, apn);
		bodyVo.put(2, username);
		bodyVo.put(3, password);
		bodyVo.put(4, data);
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set2016(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("2016", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x2016();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1017(String equipId, int step) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1017", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1016();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, step);
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set2017(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("2017", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x2016();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1018(String equipId, String username, String password) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1018", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1018();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, username);
		bodyVo.put(2, password);
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set2018(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("2018", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x2018();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set101F(String equipId, String messageTxt, String deviceServerIpAddress) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("101F", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x101F();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, messageTxt);

		if (null != deviceServerIpAddress) {// 传递网关目标服务器的IP地址
			Map<Integer, Object> deviceServerIpAddressMap = new HashMap<Integer, Object>();
			deviceServerIpAddressMap.put(1, deviceServerIpAddress);
			return sentMessage(msgHeadVo, castelmessage, bodyVo, deviceServerIpAddressMap);
		}
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set201F(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("201F", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x201F();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1020(String equipId, byte[] messageTxt, String deviceServerIpAddress) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1020", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1020();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, messageTxt);

		if (null != deviceServerIpAddress) {// 传递网关目标服务器的IP地址
			Map<Integer, Object> deviceServerIpAddressMap = new HashMap<Integer, Object>();
			deviceServerIpAddressMap.put(1, deviceServerIpAddress);
			return sentMessage(msgHeadVo, castelmessage, bodyVo, deviceServerIpAddressMap);
		}
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set2020(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("2020", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x2020();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1021(String equipId, int mode) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1021", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1021();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		bodyVo.put(1, mode);
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set2021(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("2021", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x2021();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set1022(String equipId, Map<Integer, Object> bodyVo) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("1022", 16));

		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x1022();

		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	public CastelMessage set2022(String equipId) {
		// 设置消息头
		CastelMessageHeadVO msgHeadVo = setHead(equipId, Short.parseShort("2022", 16));
		// 设置消息体 发送接收消息
		CastelMessage castelmessage = new CastelMessage_0x2022();
		Map<Integer, Object> bodyVo = new HashMap<Integer, Object>();
		return sentMessage(msgHeadVo, castelmessage, bodyVo);
	}

	private short getCounter() {
		if (counter == 65536) {
			counter = 1;
		}
		return ++counter;
	}
}
