package com.htdz.liteguardian.message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CastelMessage {	
	private CastelMessageHeadVO msgHeadVo;
	private Map<Integer, Object> msgBodyMapVo;
	
	protected CastelMessageProtocolAnalyze msgAnalyze;
	protected CastelMessageProtocolPackage msgPackage;
	
	public CastelMessageHeadVO getMsgHeadVo() {
		return msgHeadVo;
	}
	public void setMsgHeadVo(CastelMessageHeadVO msgHeadVo) {
		this.msgHeadVo = msgHeadVo;
	}

	public Map<Integer, Object> getMsgBodyMapVo() {
		return msgBodyMapVo;
	}
	public void setMsgBodyMapVo(Map<Integer, Object> msgBodyMapVo) {
		this.msgBodyMapVo = msgBodyMapVo;
	}
	
	// 初始化
	public void InitAnalyze(byte[] msgbyte) {
		msgAnalyze = new CastelMessageProtocolAnalyze(msgbyte);
	}
	public void InitPackage() {
		msgPackage = new CastelMessageProtocolPackage(this.getMsgHeadVo());
	}
	
	//获取消息头
	public CastelMessageHeadVO getMsgHead() {
		return msgAnalyze.getMsgHeadObj();
	}
	
	//验证消息验证码
	public boolean CkeckCodeIsCorrect()
	{
		return msgAnalyze.CkeckCodeIsCorrect();
	}
	
	//获取消息ID静态方法
	public static short getMsgHeadId(byte[] msgByte)
	{
		return CastelMessageProtocolAnalyze.getMsgHeadId(msgByte);
	}
	
	//在继承类实现重写,解包
	public  Map<Integer, Object> getMsgBodyMap() {
		Map<Integer, Object> returnMsgBodyMap = new HashMap<Integer, Object>();

		return returnMsgBodyMap;
	}
	
	//在继承类实现重写,组包
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
					
					byte[] bMsgValue =null;					
					
					if(value.getClass() == Integer.class){
						bMsgValue = msgPackage.intToByte(Integer.parseInt(value.toString()));		
					}else if(value.getClass() == String.class){
						bMsgValue = value.toString().getBytes();
					}else if(value.getClass() == Byte.class){
						bMsgValue = new byte[1];
						Integer intValue=Integer.parseInt(value.toString());
						bMsgValue[0] = intValue.byteValue();
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
