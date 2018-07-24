package com.htdz.liteguardian.message;

import java.util.ArrayList;
import java.util.List;

public class CastelMessage_0x101A extends CastelMessage {
	//消息打包
	@Override
	public byte[] getMsgByte() {
		List<Byte> bMsgBodyList = new ArrayList<Byte>();
		return msgPackage.packMsgByteArray(bMsgBodyList);
	}
}
