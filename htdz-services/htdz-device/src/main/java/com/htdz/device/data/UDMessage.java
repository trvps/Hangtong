package com.htdz.device.data;


import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class UDMessage {
	// 日期时间
	private int year;
	private int month;
	private int day;
	private int hour;
	private int minute;
	private int second;
	
	private String AVFlag;				// A: 定位 V: 未定位
	
	// 纬度及标识
	private double latitude;
	private String latitudeFlag;		// N: 北纬 S: 南纬
	
	// 经度及标识
	private double longitude;
	private String longitudeFlag;		// E: 东经 W: 西经
	
	// 速度
	private double speed;				// 公里/小时
	
	// 方向
	private double directDegree;		// 度
	
	// 海拔
	private double altitude;			// 米
	
	// 卫星个数
	private int satelliteNumber;
	
	// 信号强度
	private int signalStrength;		// GSM信号强度 0-100
	
	// 电量
	private int batteryPercent;		// 百分比
	
	// 计步
	private int stepCount;
	
	// 翻滚次数
	private int rollCount;
	
	// 终端设备状态
	private String deviceState;
	
	// 基站信息
	private GSMBSGroup gsmBSGrpup = new GSMBSGroup();
	
	// Wifi信息
	private WifiGroup wifiGroup = new WifiGroup();
	
	public static UDMessage parse(String data) {
		UDMessage udMsg = new UDMessage();
		
		try {
			String[] valuearr = data.split(",");
			int index = 0;
			
			// 年月日 220114
			String date = valuearr[index++];
			udMsg.setDay(DataUtil.stringToInt(date.substring(0, 2), 0));
			udMsg.setMonth(DataUtil.stringToInt(date.substring(2, 4), 0));
			udMsg.setYear(2000+DataUtil.stringToInt(date.substring(4, 6), 0));
			
			// 时分秒 134652
			String time = valuearr[index++];
			udMsg.setHour(DataUtil.stringToInt(time.substring(0, 2), 0));
			udMsg.setMinute(DataUtil.stringToInt(time.substring(2, 4), 0));
			udMsg.setSecond(DataUtil.stringToInt(time.substring(4, 6), 0));
			
			// 是否定位
			udMsg.setAVFlag(valuearr[index++]);
			
			// 纬度及标识
			udMsg.setLatitude(DataUtil.stringToDouble(valuearr[index++], 0d));
			udMsg.setLatitudeFlag(valuearr[index++]);
			
			// 经度及标识
			udMsg.setLongitude(DataUtil.stringToDouble(valuearr[index++], 0d));
			udMsg.setLongitudeFlag(valuearr[index++]);
			
			// 速度
			udMsg.setSpeed(DataUtil.stringToDouble(valuearr[index++], 0d));
			
			// 方向
			udMsg.setDirectDegree(DataUtil.stringToDouble(valuearr[index++], 0d));
			
			// 海拔
			udMsg.setAltitude(DataUtil.stringToDouble(valuearr[index++], 0d));
			
			// 卫星个数
			udMsg.setSatelliteNumber(DataUtil.stringToInt(valuearr[index++], 0));
			
			// 信号强度
			udMsg.setSignalStrength(DataUtil.stringToInt(valuearr[index++], 0));
			
			// 电量
			udMsg.setBatteryPercent(DataUtil.stringToInt(valuearr[index++], 0));
			
			// 计步
			udMsg.setStepCount(DataUtil.stringToInt(valuearr[index++], 0));
			
			// 翻滚次数
			udMsg.setRollCount(DataUtil.stringToInt(valuearr[index++], 0));
			
			// 终端设备状态
			udMsg.setDeviceState(valuearr[index++]);
			
			
			// 解析基站信息
			int bscount = DataUtil.stringToInt(valuearr[index++], 0);
			if (bscount > 0) {
				GSMBSGroup gsmBSGrpup = udMsg.getGsmBSGrpup();
				//udMsg.setGsmBSGrpup(gsmBSGrpup);
				
				// GSM时延
				gsmBSGrpup.setTa(DataUtil.stringToInt(valuearr[index++], 0));
				
				// MCC国家码
				gsmBSGrpup.setMcc(valuearr[index++]);
				
				// MNC网号
				gsmBSGrpup.setMnc(valuearr[index++]);
				
				for (int i=0; i<bscount; i++) {
					GSMBSItem gsmBSItem = new GSMBSItem();
					
					// 区域码
					gsmBSItem.setAreaCode(valuearr[index++]);
					
					// 基站编号
					gsmBSItem.setBscode(valuearr[index++]);
					
					// 基站信号强度
					gsmBSItem.setBsSignalStrength(DataUtil.stringToInt(valuearr[index++], 0));
					
					gsmBSGrpup.getBsItems().add(gsmBSItem);
				}
			}
			
			
			
			// 解析Wifi信息
			int wificount = DataUtil.stringToInt(valuearr[index++], 0);
			if (wificount > 0) {
				WifiGroup wifiGroup = udMsg.getWifiGroup();
				//udMsg.setWifiGroup(wifiGroup);
				
				for (int i=0; i<wificount; i++) {
					WifiItem wifiItem = new WifiItem();
					
					// 名字
					wifiItem.setName(valuearr[index++]);
					
					// mac地址
					wifiItem.setMacaddr(valuearr[index++]);
					
					// 信号强度
					wifiItem.setSignalStrength(DataUtil.stringToInt(valuearr[index++], 0));
					
					wifiGroup.getBsItems().add(wifiItem);
				}
			}
			
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
		
		return udMsg;
	}

	public static void main(String[] args) {
		System.out.println("--------------------- main ---------------------");
		
		String data = "220414,134652,A,22.571707,N,113.8613968,E,0.1,0.0,100,7,60,90,1000,50,0000,4,1,460,0,9360,4082,131,9360,4092,148,9360,4091,143,9360,4153,141,2,rrr,1c:fa68:13:a5:b4,-61,abc,1c:fa68:13:a5:b5,-87";
		UDMessage udMsg = UDMessage.parse(data);
		
		System.out.println(udMsg);
		
		char c = 0x72;
		System.out.println(c);
		
		System.out.println("--------------------- End ---------------------");
	}
}
