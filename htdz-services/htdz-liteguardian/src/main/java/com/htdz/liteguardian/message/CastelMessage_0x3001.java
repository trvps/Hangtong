package com.htdz.liteguardian.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CastelMessage_0x3001 extends CastelMessage {
	// 获取消息体对象
	@Override
	public  Map<Integer, Object> getMsgBodyMap() {
		Map<Integer, Object> returnMsgBodyMap = new HashMap<Integer, Object>();

		short msgBodyLength = msgAnalyze.getMsgHeadLength();
		if ((msgAnalyze.checkStatus) && (msgBodyLength > 0) && (msgBodyLength == msgAnalyze.msgBody.length) && (msgAnalyze.msgBody != null)) {
			int i = 0;
			while (i < msgAnalyze.msgBody.length) {
				// 获取消息体一条消息KEY
				int keyStartIndex = i;
				short key = msgAnalyze.byte2short(new byte[] {
						msgAnalyze.msgBody[keyStartIndex].byteValue(),
						msgAnalyze.msgBody[(keyStartIndex + 1)].byteValue() });

				// 获取消息体一条消息长度
				int lengthStartIndex = i + 2;
				short length = msgAnalyze
						.byte2short(new byte[] {
								msgAnalyze.msgBody[lengthStartIndex]
										.byteValue(),
								msgAnalyze.msgBody[(lengthStartIndex + 1)]
										.byteValue() });

				// 获取消息体一条消息内容的BYTE数组
				int valueStartIndex = i + 4;
				int valueEndIndex = i + 4 + length - 1;
				byte[] msgValue = new byte[length];
				for (int k = 0; k <= length - 1; k++) {
					if (length>1 && msgAnalyze.msgBody[valueStartIndex + k].byteValue() == (byte) 0) {
						break;
					}

					msgValue[k] = msgAnalyze.msgBody[valueStartIndex + k]
							.byteValue();
				}

				Object Value;
				if (key == 6 || key == 7 || key == 8 || key == 9) {
					Value=(int)msgValue[0]; //char
				} else if (key == 16) {
					if (msgValue.length >= 1) {
						Value = msgAnalyze.byte2int(msgValue); //int
					} else {
						Value = 0;
					}
				} else {
					Value = msgAnalyze.msgValue2String(msgValue); //string
				}
				
				returnMsgBodyMap.put((int) key, Value);
				i = valueEndIndex + 1;
			}
		}

		return returnMsgBodyMap;
	}

	//消息打包
	@Override
	public byte[] getMsgByte() {
		List<Byte> bMsgBodyList = new ArrayList<Byte>();

		Map<Integer, Object> msgbody = this.getMsgBodyMapVo();
		if ((msgbody != null) && (msgbody.size() >= 1)) {
			Set<Integer> keys = msgbody.keySet();
			Iterator<Integer> iterator = keys.iterator();
			while (iterator.hasNext()) {
				int key = iterator.next().intValue();
				
				Object value = msgbody.get(key);
				if (value != null) {
					byte[] bKey = msgPackage.short2byte(key);
					bMsgBodyList.add(bKey[0]);
					bMsgBodyList.add(bKey[1]);

					
					byte[] bMsgValue;
					if (key == 6 || key == 7 || key == 8 || key == 9) {
						Integer valueChar = (Integer) value;
						bMsgValue = new byte[1];
						bMsgValue[0] = valueChar.byteValue();
					} else if (key == 16) {
						Integer valueInt = (Integer) value;
						bMsgValue = msgPackage.intToByte(valueInt.intValue());
					} else {
						bMsgValue = value.toString().getBytes();
					}
					
					

					byte[] bLength;
					if ((bMsgValue == null) || (bMsgValue.length == 0)) {
						bLength = msgPackage.short2byte(0);
					} else {
						bLength = msgPackage.short2byte(bMsgValue.length);
					}

					bMsgBodyList.add(bLength[0]);
					bMsgBodyList.add(bLength[1]);

					for (int k = 0; k <= bMsgValue.length - 1; k++) {
						bMsgBodyList.add(bMsgValue[k]);
					}
				}
			}
		}

		return msgPackage.packMsgByteArray(bMsgBodyList);
	}
}
