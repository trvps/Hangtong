package com.htdz.gpsmessage;

import java.util.Map;

public abstract class GpsMessage {	
	private GpsMessageHeadVO msgHeadVo;
	private Map<Integer, Object> msgBodyMapVo;
	
	protected GpsMessageProtocolAnalyze msgAnalyze;
	protected GpsMessageProtocolPackage msgPackage;
	
	public GpsMessageHeadVO getMsgHeadVo() {
		return msgHeadVo;
	}
	public void setMsgHeadVo(GpsMessageHeadVO msgHeadVo) {
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
		msgAnalyze = new GpsMessageProtocolAnalyze(msgbyte);
	}
	public void InitPackage() {
		msgPackage = new GpsMessageProtocolPackage(this.getMsgHeadVo());
	}
	
	//获取消息头
	public GpsMessageHeadVO getMsgHead() {
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
		return GpsMessageProtocolAnalyze.getMsgHeadId(msgByte);
	}
	
	//在继承类实现重写,解包
	public abstract  Map<Integer, Object> getMsgBodyMap();
	
	//在继承类实现重写,组包
	public abstract byte[] getMsgByte();
}
