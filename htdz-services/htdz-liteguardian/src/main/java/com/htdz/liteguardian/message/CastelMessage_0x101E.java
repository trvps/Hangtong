package com.htdz.liteguardian.message;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CastelMessage_0x101E extends CastelMessage {
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

					byte[] bMsgValue = value.toString().getBytes();
					
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
