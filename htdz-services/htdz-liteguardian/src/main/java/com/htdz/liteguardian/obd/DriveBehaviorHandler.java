package com.htdz.liteguardian.obd;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * 驾驶行为分析
 * @author user
 */

public class DriveBehaviorHandler {
	private static Logger logger = Logger.getLogger(DriveBehaviorHandler.class);
	
	
    /// 统计某个司机在某天的数据
    public static TdriverBehaviorAnalysis getDriverDataByDate( Map<String, Integer> dsDayAlarmData, Map<String, Integer> dsDayAlarmTimeLongData)
    {
    	
    	TdriverBehaviorAnalysis driverBehaviorAnalysis = new TdriverBehaviorAnalysis ();
        //经济驾驶扣分数
        int economicDrivingScore = 0;
        //安全驾驶扣分数
        int safetyDrivingScore = 0;

        //p2,p11,p4,p5,p8,p9,p12,p13 安全驾驶
        //p2,p11,p6,p10,p4,p5,p8,p9  经济驾驶
        //超速报警 (次) (安全驾驶) (经济驾驶)
        int p2 = dsDayAlarmData.get(""+EnumUtils.AlarmType.SPEED) == null ? 0 : dsDayAlarmData.get(""+EnumUtils.AlarmType.SPEED);
        driverBehaviorAnalysis.setP2(p2);
        economicDrivingScore += evaluationDriving(EnumUtils.AlarmType.SPEED, p2, EnumUtils.DrivingBehaviorType.ECONOMIC);
        safetyDrivingScore += evaluationDriving(EnumUtils.AlarmType.SPEED, p2,EnumUtils.DrivingBehaviorType.SAFETY);

        //急加速报警 (次) (安全驾驶) (经济驾驶)
        int p4 = dsDayAlarmData.get(""+EnumUtils.AlarmType.SPEED_UP) == null ? 0 : dsDayAlarmData.get(""+EnumUtils.AlarmType.SPEED_UP);
        driverBehaviorAnalysis.setP4(p4);
        economicDrivingScore += evaluationDriving(EnumUtils.AlarmType.SPEED_UP, p4, EnumUtils.DrivingBehaviorType.ECONOMIC);
        safetyDrivingScore += evaluationDriving(EnumUtils.AlarmType.SPEED_UP, p4,EnumUtils.DrivingBehaviorType.SAFETY);

        //急减速报警 (次) (安全驾驶) (经济驾驶)
        int p5 = dsDayAlarmData.get(""+EnumUtils.AlarmType.SPEED_DOWN) == null ? 0 : dsDayAlarmData.get(""+EnumUtils.AlarmType.SPEED_DOWN);
        driverBehaviorAnalysis.setP5(p5);
        economicDrivingScore += evaluationDriving(EnumUtils.AlarmType.SPEED_DOWN, p5, EnumUtils.DrivingBehaviorType.ECONOMIC);
        safetyDrivingScore += evaluationDriving(EnumUtils.AlarmType.SPEED_DOWN, p5,EnumUtils.DrivingBehaviorType.SAFETY);

        //停车未熄火报警 (次) (经济驾驶)
        int p6 = dsDayAlarmData.get(""+EnumUtils.AlarmType.UNFLAMEOUT) == null ? 0 : dsDayAlarmData.get(""+EnumUtils.AlarmType.UNFLAMEOUT);
        driverBehaviorAnalysis.setP6(p6);
        economicDrivingScore += evaluationDriving(EnumUtils.AlarmType.UNFLAMEOUT, p6, EnumUtils.DrivingBehaviorType.ECONOMIC);

        //转速过高报警 (次) (安全驾驶) (经济驾驶)
        int p8 = dsDayAlarmData.get(""+EnumUtils.AlarmType.SPEED_HIGH) == null ? 0 :dsDayAlarmData.get(""+EnumUtils.AlarmType.SPEED_HIGH);
        driverBehaviorAnalysis.setP8(p8);
        economicDrivingScore += evaluationDriving(EnumUtils.AlarmType.SPEED_HIGH, p8, EnumUtils.DrivingBehaviorType.ECONOMIC);
        safetyDrivingScore += evaluationDriving(EnumUtils.AlarmType.SPEED_HIGH, p8,EnumUtils.DrivingBehaviorType.SAFETY);

          
        if (null != dsDayAlarmTimeLongData)
        {  //转速超标时长 (秒) (安全驾驶) (经济驾驶)
        	 int p9 = dsDayAlarmTimeLongData.get(""+EnumUtils.AlarmType.SPEED_HIGH) == null ? 0 :dsDayAlarmTimeLongData.get(""+EnumUtils.AlarmType.SPEED_HIGH);
        	 driverBehaviorAnalysis.setP9(p9);
	        economicDrivingScore += evaluationDriving(9, p9, EnumUtils.DrivingBehaviorType.ECONOMIC);
	        safetyDrivingScore += evaluationDriving(9, p9,EnumUtils.DrivingBehaviorType.SAFETY);
	
	        //停车未熄火告警 (秒) (经济驾驶)
	        int p10 = dsDayAlarmData.get(""+EnumUtils.AlarmType.UNFLAMEOUT) == null ? 0 :dsDayAlarmData.get(""+EnumUtils.AlarmType.UNFLAMEOUT);	       
	        driverBehaviorAnalysis.setP10(p10);
	        economicDrivingScore += evaluationDriving(10, p10, EnumUtils.DrivingBehaviorType.ECONOMIC);
	
	        //超速时长 (秒) (安全驾驶) (经济驾驶)
	        int p11 = dsDayAlarmTimeLongData.get(""+EnumUtils.AlarmType.SPEED) == null ? 0 :dsDayAlarmTimeLongData.get(""+EnumUtils.AlarmType.SPEED);
	        driverBehaviorAnalysis.setP11(p11);
	        economicDrivingScore += evaluationDriving(11, p11, EnumUtils.DrivingBehaviorType.ECONOMIC);
	        safetyDrivingScore += evaluationDriving(11, p11,EnumUtils.DrivingBehaviorType.SAFETY);
	        
        }
        
        //疲劳驾驶次数 (次) (安全驾驶)
        int p12 = dsDayAlarmData.get(""+EnumUtils.AlarmType.DRIVER_FATIGUE) == null ? 0 : dsDayAlarmData.get(""+EnumUtils.AlarmType.DRIVER_FATIGUE);
        driverBehaviorAnalysis.setP12(p12);
        safetyDrivingScore += evaluationDriving(EnumUtils.AlarmType.DRIVER_FATIGUE, p12,EnumUtils.DrivingBehaviorType.SAFETY);

        //疲劳驾驶时长 (秒) (安全驾驶)
        int p13 = 0;
        if (null != dsDayAlarmTimeLongData)
        {
             p13 = dsDayAlarmTimeLongData.get(""+EnumUtils.AlarmType.DRIVER_FATIGUE) == null ? 0 :dsDayAlarmTimeLongData.get(""+EnumUtils.AlarmType.DRIVER_FATIGUE);;
        }
        driverBehaviorAnalysis.setP13(p13);
        safetyDrivingScore += evaluationDriving(13, p13,EnumUtils.DrivingBehaviorType.SAFETY);

        //急转弯 (次) (安全驾驶) (经济驾驶)
        int p14 = dsDayAlarmData.get(""+EnumUtils.AlarmType.WHEEL) == null ? 0 : dsDayAlarmData.get(""+EnumUtils.AlarmType.WHEEL);
        driverBehaviorAnalysis.setP14(p14);
        economicDrivingScore += evaluationDriving(EnumUtils.AlarmType.WHEEL, p14, EnumUtils.DrivingBehaviorType.ECONOMIC);
        safetyDrivingScore += evaluationDriving(EnumUtils.AlarmType.WHEEL, p14,EnumUtils.DrivingBehaviorType.SAFETY);

        //急变道 (次) (安全驾驶) (经济驾驶)
        int p15 = dsDayAlarmData.get(""+EnumUtils.AlarmType.CHANGE_LANES) == null ? 0 : dsDayAlarmData.get(""+EnumUtils.AlarmType.CHANGE_LANES);
        driverBehaviorAnalysis.setP15(p15);
        economicDrivingScore += evaluationDriving(EnumUtils.AlarmType.CHANGE_LANES, p15, EnumUtils.DrivingBehaviorType.ECONOMIC);
        safetyDrivingScore += evaluationDriving(EnumUtils.AlarmType.CHANGE_LANES, p15,EnumUtils.DrivingBehaviorType.SAFETY);

        //安全驾驶/经济驾驶, 得分
        economicDrivingScore = economicDrivingScore + 100;
        safetyDrivingScore = safetyDrivingScore + 100;
        driverBehaviorAnalysis.setEconomicscore(economicDrivingScore >= 100 ? 100 : (economicDrivingScore <= 0 ? 0 : economicDrivingScore));
        driverBehaviorAnalysis.setSafescore(safetyDrivingScore >= 100 ? 100 : (safetyDrivingScore <= 0 ? 0 : safetyDrivingScore));
        return driverBehaviorAnalysis;
    }

	/**
	 * 驾驶行为统计
	 * @param code
	 * @param value
	 * @param type
	 * @return
	 */
    public static Double evaluationDriving(int code, double value, int type)
    {
      switch (code)
        {
            case 0:
                return null;
            case 1:
                return null;
            case EnumUtils.AlarmType.SPEED:
                switch (type)
                {
                    case EnumUtils.DrivingBehaviorType.SAFETY:
                        return safetyOverSpeedNumber(value);
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        return economicOverSpeedNumber(value);
                }
            case 3:
                return null;
            case EnumUtils.AlarmType.SPEED_UP:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                        return safetySuddenAccelerationNumber(value);
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        return economicSuddenAccelerationNumber(value);
                }
                break;
            case EnumUtils.AlarmType.SPEED_DOWN:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                        return safetySuddenDecelerationNumber(value);
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        return economicSuddenDecelerationNumber(value);
                }
                break;
            case EnumUtils.AlarmType.UNFLAMEOUT:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        return economicLongIdlingNumber(value);
                }
                break;
            case 7:
                return null;
            case  EnumUtils.AlarmType.SPEED_HIGH:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                        return safetyOverRPMNumber(value);
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        return economicOverRPMNumber(value);
                }
                break;
            case 9:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                        return safetyOverRPMTime(value);
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        return economicOverRPMTime(value);
                }
                break;
            case 10:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                        break;
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        return economicLongIdlingTime(value);
                }
                break;
            case 11:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                        return safetyOverSpeedTime(value);
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        return economicOverSpeedTime(value);
                }
                break;
            case EnumUtils.AlarmType.DRIVER_FATIGUE:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                        return safetyFatigueDrivingNumber(value);
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        break;
                }
                break;
            case 13:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                        return safetyFatigueDrivingTime(value);
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        break;
                }
                break;
            case EnumUtils.AlarmType.WHEEL:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                        return safetyTurnCornerNumber(value);
				case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        return economicTurnCornerNumber(value);
                }
                break;
            case EnumUtils.AlarmType.CHANGE_LANES:
                switch (type)
                {
                     case EnumUtils.DrivingBehaviorType.SAFETY:
                        return safetyChangeRoadNumber(value);
                    case EnumUtils.DrivingBehaviorType.ECONOMIC:
                        return economicChangeRoadNumber(value);
                }
                break;
        }
        return null;
    }

    
   //region ?
    public static double economicLongIdlingNumber(double value)
    {
        return -value * 4;
    }

    public static double economicLongIdlingTime(double value)
    {
        double time = value / (60);

        return -((int)((time) / 10)) * 4;
    }

    public static double economicSuddenAccelerationNumber(double value)
    {
        return -value * 4;
    }

    public static double economicSuddenDecelerationNumber(double value)
    {
        return -value * 3;
    }

    public static double economicOverSpeedNumber(double value)
    {
        return -value * 3;
    }

    public static double economicOverSpeedTime(double value)
    {
        double time = value;
        return -((int)(time / 30)) * 2;
    }

    public static double economicOverRPMNumber(double value)
    {
        return -value * 2;
    }

    public static double economicOverRPMTime(double value)
    {
        double time = value;
        return -((int)(time / 30)) * 2;
    }

    public static double safetyFatigueDrivingNumber(double value)
    {
        return -value * 5;
    }

    public static double safetyFatigueDrivingTime(double value)
    {
        double time = value / (60);
        return -((int)(time) / 30) * 2;
    }

    public static double safetySuddenAccelerationNumber(double value)
    {
        return -value * 4;
    }

    public static double safetySuddenDecelerationNumber(double value)
    {
        return -value * 4;
    }

    public static double safetyOverSpeedNumber(double value)
    {
        return -value * 2;
    }

    public static double safetyOverSpeedTime(double value)
    {
        double time = value;

        return -((int)(time / 30)) * 2;
    }

    public static double safetyOverRPMNumber(double value)
    {
        return -value * 2;
    }

    public static double safetyOverRPMTime(double value)
    {
        double time = value;
        return -((int)(time / 30)) * 2;
    }

    public static double safetyChangeRoadNumber(double value)
    {
        return -value * 4;
    }

    public static double economicChangeRoadNumber(double value)
    {
        return -value * 1;
    }

    public static double safetyTurnCornerNumber(double value)
    {
        return -value * 4;
    }

    public static double economicTurnCornerNumber(double value)
    {
        return -value * 1;
    }
    
	public static void main(String[] args) {
		Map<String,Integer> dsDayAlarmData = new HashMap<String,Integer>();
		dsDayAlarmData.put("6", 1);
		dsDayAlarmData.put("18", 1);
		dsDayAlarmData.put("29", 1);
		dsDayAlarmData.put("81", 1);
		dsDayAlarmData.put("82", 1);
		dsDayAlarmData.put("83", 1);
		dsDayAlarmData.put("84", 1);
		dsDayAlarmData.put("85", 1);
		dsDayAlarmData.put("86", 1);
		dsDayAlarmData.put("87", 1);
		dsDayAlarmData.put("88", 1);
		dsDayAlarmData.put("89", 1);
		dsDayAlarmData.put("90", 1);
		dsDayAlarmData.put("91", 1);
		dsDayAlarmData.put("92", 1);
		dsDayAlarmData.put("93", 1);
		
    	Map<String,Integer> dsDayAlarmTimeLongData = new HashMap<String,Integer>();
    	dsDayAlarmTimeLongData.put("6", null);
    	dsDayAlarmTimeLongData.put("18", null);
    	dsDayAlarmTimeLongData.put("29", 5);
    	dsDayAlarmTimeLongData.put("81", 5);
    	dsDayAlarmTimeLongData.put("82", 5);
    	dsDayAlarmTimeLongData.put("83", 5);
    	dsDayAlarmTimeLongData.put("84", 5);
    	dsDayAlarmTimeLongData.put("85", 5);
    	dsDayAlarmTimeLongData.put("86", 5);
    	dsDayAlarmTimeLongData.put("87", 5);
    	dsDayAlarmTimeLongData.put("88", 5);
    	dsDayAlarmTimeLongData.put("89", 5);
    	dsDayAlarmTimeLongData.put("90", 5);
    	dsDayAlarmTimeLongData.put("91", 5);
    	dsDayAlarmTimeLongData.put("92", 5);
    	dsDayAlarmTimeLongData.put("93", 5);
    	
    	getDriverDataByDate(dsDayAlarmData,dsDayAlarmTimeLongData);
    	
	
		
	}
}