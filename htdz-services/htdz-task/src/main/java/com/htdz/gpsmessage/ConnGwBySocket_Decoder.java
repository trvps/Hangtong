package com.htdz.gpsmessage;

import java.nio.ByteBuffer;
import java.util.Map;

import com.htdz.common.LogManager;
import com.htdz.common.utils.StringEncoding;

public class ConnGwBySocket_Decoder {
	public static GpsMessage decoder(byte[] recByte) {
		GpsMessage curMsgObj = null;

		if (recByte != null) {
			ByteBuffer buff = ByteBuffer.allocate(500);
			LogManager.info(StringEncoding.bytesToHexString(recByte));

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

							if (msgByteArray != null
									&& msgByteArray.length >= 31
									&& msgByteArray[0] == (byte) 0x7e
									&& msgByteArray[msgByteArray.length - 1] == (byte) 0x7e) {

								LogManager.info("CastelMessage.getMsgHeadId");
								short msgID = GpsMessage
										.getMsgHeadId(msgByteArray);

								try {
									Class<?> castelMessage = Class
											.forName("com.htdz.gpsmessage.GpsMessage_0x"
													+ Integer
															.toHexString(msgID)
															.toUpperCase());
									Object obj = castelMessage.newInstance();
									curMsgObj = (GpsMessage) obj;
								} catch (Exception e) {
									LogManager.exception(e.getMessage(), e);
								}

								LogManager.info("CastelMessage.InitAnalyze");
								curMsgObj.InitAnalyze(msgByteArray);
								if (curMsgObj.CkeckCodeIsCorrect()) {
									GpsMessageHeadVO msgHeadVo = curMsgObj
											.getMsgHead();
									Map<Integer, Object> msgBodyVo = curMsgObj
											.getMsgBodyMap();

									curMsgObj.setMsgBodyMapVo(msgBodyVo);
									curMsgObj.setMsgHeadVo(msgHeadVo);
								}
							}
						}

						byteCount = 0;
						flagCount = 0;
						buff = ByteBuffer.allocate(500);
					}
				}

				index++;
			}
		}

		return curMsgObj;
	}
}
