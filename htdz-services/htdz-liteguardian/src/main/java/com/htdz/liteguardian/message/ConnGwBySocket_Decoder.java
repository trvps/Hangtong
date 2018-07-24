package com.htdz.liteguardian.message;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;

public class ConnGwBySocket_Decoder {
	private static final Logger log = Logger.getLogger(ConnGwBySocket_Decoder.class);

	public static CastelMessage decoder(byte[] recByte) {
		CastelMessage curMsgObj = null;

		if (recByte != null) {
			// 可变的IoBuffer数据缓冲区
			IoBuffer buff = IoBuffer.allocate(200).setAutoExpand(true);

			log.info(bytesToHexString(recByte));

			int index = 0;
			int byteCount = 0;
			int flagCount = 0;
			while (index <= recByte.length - 1) {
				byte curByte = recByte[index];
				buff.put(curByte);
				byteCount++;

				if (curByte == (byte) 0x7e) {
					flagCount += 1;

					if (flagCount == 2) {
						if (byteCount >= 31) {
							buff.flip();
							byte[] msgByteArray = new byte[buff.limit()];
							buff.get(msgByteArray);

							if (msgByteArray != null && msgByteArray.length >= 31 && msgByteArray[0] == (byte) 0x7e
									&& msgByteArray[msgByteArray.length - 1] == (byte) 0x7e) {
								log.info("CastelMessage.getMsgHeadId");
								short msgID = CastelMessage.getMsgHeadId(msgByteArray);

								try {
									Class<?> castelMessage = Class
											.forName("com.htdz.liteguardian.message.CastelMessage_0x"
													+ Integer.toHexString(msgID).toUpperCase());
									Object obj = castelMessage.newInstance();
									curMsgObj = (CastelMessage) obj;
								} catch (Exception e) {
									e.printStackTrace();
								}

								log.info("CastelMessage.InitAnalyze");
								curMsgObj.InitAnalyze(msgByteArray);
								if (curMsgObj.CkeckCodeIsCorrect()) {
									CastelMessageHeadVO msgHeadVo = curMsgObj.getMsgHead();
									Map<Integer, Object> msgBodyVo = curMsgObj.getMsgBodyMap();

									curMsgObj.setMsgBodyMapVo(msgBodyVo);
									curMsgObj.setMsgHeadVo(msgHeadVo);
								}
							}
						}

						byteCount = 0;
						flagCount = 0;
						buff = IoBuffer.allocate(200).setAutoExpand(true);
					}
				}

				index++;
			}
		}

		return curMsgObj;
	}

	// BYTE转化位16进制字符串
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}

		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}

		return stringBuilder.toString();
	}
}
