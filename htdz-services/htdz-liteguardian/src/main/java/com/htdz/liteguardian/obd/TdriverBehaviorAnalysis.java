package com.htdz.liteguardian.obd;

public class TdriverBehaviorAnalysis {
	//score_typeint(11) NOT NULL类型 0安全驾驶，1经济驾驶
	private int scoretype;
	//d_timedate NOT NULL日期
	private String time;
	//didint(11) NOT NULL设备ID
	private int did;
	//device_snvarchar(30) NOT NULL设备号
	private String device_sn;
	//p2int(11) NULL超速报警 (次) (安全驾驶) (经济驾驶)
	private int p2 = 0;
	//p4int(11) NULL急加速报警 (次) (安全驾驶) (经济驾驶)
	private int p4 = 0;
	//p5int(11) NULL急减速报警 (次) (安全驾驶) (经济驾驶)
	private int p5 = 0;
	//p6int(11) NULL停车未熄火报警 (次) (经济驾驶)
	private int p6 = 0;
	//p8int(11) NULL转速过高报警 (次) (安全驾驶) (经济驾驶)
	private int p8 = 0;
	//p9int(11) NULL转速超标时长 (秒) (安全驾驶) (经济驾驶)
	private int p9 = 0;
	//p10int(11) NULL长时间空闲时长 (秒) (经济驾驶)
	private int p10 = 0;
	//p11int(11) NULL超速时长 (秒) (安全驾驶) (经济驾驶)
	private int p11 = 0;
	//p12int(11) NULL疲劳驾驶次数 (次) (安全驾驶)
	private int p12 = 0;
	//p13int(11) NULL疲劳驾驶时长 (秒) (安全驾驶)
	private int p13 = 0;
	//p14int(11) NULL
	private int p14 = 0;
	//p15int(11) NULL
	private int p15 = 0;
	//scoreint(11) NULL
	private int safescore = 0;
	private int economicscore = 0;
	public int getScoretype() {
		return scoretype;
	}
	public void setScoretype(int scoretype) {
		this.scoretype = scoretype;
	}
	public String getTime() {
		return time;
	}
	public void setTime(String time) {
		this.time = time;
	}
	public int getDid() {
		return did;
	}
	public void setDid(int did) {
		this.did = did;
	}
	public String getDevice_sn() {
		return device_sn;
	}
	public void setDevice_sn(String device_sn) {
		this.device_sn = device_sn;
	}
	public int getP2() {
		return p2;
	}
	public void setP2(int p2) {
		this.p2 = p2;
	}
	public int getP4() {
		return p4;
	}
	public void setP4(int p4) {
		this.p4 = p4;
	}
	public int getP5() {
		return p5;
	}
	public void setP5(int p5) {
		this.p5 = p5;
	}
	public int getP6() {
		return p6;
	}
	public void setP6(int p6) {
		this.p6 = p6;
	}
	public int getP8() {
		return p8;
	}
	public void setP8(int p8) {
		this.p8 = p8;
	}

	public int getP11() {
		return p11;
	}
	public void setP11(int p11) {
		this.p11 = p11;
	}
	public int getP12() {
		return p12;
	}
	public void setP12(int p12) {
		this.p12 = p12;
	}
	public int getP13() {
		return p13;
	}
	public void setP13(int p13) {
		this.p13 = p13;
	}
	public int getP14() {
		return p14;
	}
	public void setP14(int p14) {
		this.p14 = p14;
	}
	public int getP15() {
		return p15;
	}
	public void setP15(int p15) {
		this.p15 = p15;
	}
	public int getSafescore() {
		return safescore;
	}
	public void setSafescore(int safescore) {
		this.safescore = safescore;
	}
	public int getEconomicscore() {
		return economicscore;
	}
	public void setEconomicscore(int economicscore) {
		this.economicscore = economicscore;
	}
	public int getP9() {
		return p9;
	}
	public void setP9(int p9) {
		this.p9 = p9;
	}
	public int getP10() {
		return p10;
	}
	public void setP10(int p10) {
		this.p10 = p10;
	}

}
