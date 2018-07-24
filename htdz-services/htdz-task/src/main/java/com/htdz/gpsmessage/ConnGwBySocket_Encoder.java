package com.htdz.gpsmessage;

import com.htdz.common.LogManager;
import com.htdz.common.utils.StringEncoding;



public class ConnGwBySocket_Encoder {
	public static byte[] encode(GpsMessage message) {
		if (message != null && (message instanceof GpsMessage)) {
			GpsMessage curMsgObj = (GpsMessage) message;
			curMsgObj.InitPackage();

			byte[] bMsgPackArray = curMsgObj.getMsgByte();
			
			LogManager.info("设备{}请求GPS数据为：{}",message.getMsgHeadVo().getEquipId(),StringEncoding.bytesToHexString(bMsgPackArray));
			
			return bMsgPackArray;
		} else {
			return null;
		}
	}
}
