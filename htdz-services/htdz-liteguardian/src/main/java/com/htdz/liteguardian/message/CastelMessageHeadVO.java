package com.htdz.liteguardian.message;

public class CastelMessageHeadVO {
	private short msgId;
	private short protocolType;
	private String equipId;
	private short msgSquice;

	public short getMsgId() {
		return this.msgId;
	}

	public void setMsgId(short msgId) {
		this.msgId = msgId;
	}

	public short getProtocolType() {
		return this.protocolType;
	}

	public void setProtocolType(short protocolType) {
		this.protocolType = protocolType;
	}

	public String getEquipId() {
		return this.equipId;
	}

	public void setEquipId(String equipId) {
		this.equipId = equipId;
	}

	public short getMsgSquice() {
		return this.msgSquice;
	}

	public void setMsgSquice(short msgSquice) {
		this.msgSquice = msgSquice;
	}
}