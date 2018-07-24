package com.htdz.liteguardian.message;

import java.util.ArrayList;
import java.util.List;

public class CastelMessage_0x1004 extends CastelMessage {
	// 消息打包
	@Override
	public byte[] getMsgByte() {
		List<Byte> bMsgBodyList = new ArrayList<Byte>();

		return msgPackage.packMsgByteArray(bMsgBodyList);
	}
}
