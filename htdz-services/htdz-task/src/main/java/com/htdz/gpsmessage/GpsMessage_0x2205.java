package com.htdz.gpsmessage;

import java.util.HashMap;
import java.util.Map;

public class GpsMessage_0x2205 extends GpsMessage {
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
					if(key != 17 && key != 19 && key != 20)
					{
						if (length>1 && msgAnalyze.msgBody[valueStartIndex + k].byteValue() == (byte) 0) {
							break;
						}
					}
					
					msgValue[k] = msgAnalyze.msgBody[valueStartIndex + k].byteValue();
				}

				Object Value;
				if (key == 5) {
					if (msgValue.length >= 1) {
						Value = msgAnalyze.byte2short(msgValue); //int
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

	@Override
	public byte[] getMsgByte() {
		// TODO Auto-generated method stub
		return null;
	}
}
