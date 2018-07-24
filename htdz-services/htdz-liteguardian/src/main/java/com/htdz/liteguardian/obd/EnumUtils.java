package com.htdz.liteguardian.obd;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 枚举，enum构造比较麻烦，故采用内部类形式
 * 
 * @项目名 htfamily
 * @类名 Enum.java
 * @包名 com.sinocastel.htfamily.util
 * @创建人 ym
 * @创建时间 2014年11月17日 上午9:41:18
 * @修改人
 * @修改时间
 * @修改备注
 * @版本 V1.0
 * @版权 Copyright © 航通. All rights reserved.
 */
public class EnumUtils {

	/**
	 * 
	 * @author wj
	 *
	 */
	public class AlarmType {
		
		/** 超速告警 */
		public static final int SPEED  = 81;
		/** 低电压告警 */
		public static final int LOW_VOLTAGE = 82;
		/** 水温告警 */
		public static final int WATER  = 83;
		/** 急加速告警 */
		public static final int SPEED_UP  = 84;
		/** 急减速告警 */
		public static final int SPEED_DOWN = 85;
		/** 停车未熄火告警 */
		public static final int UNFLAMEOUT = 86;
		/** 拖吊告警 */
		public static final int TOWED = 87;
		/** 转速高告警 */
		public static final int  SPEED_HIGH  = 88;
		/** 上电告警 */
		public static final int POWER_ON = 89;
		/** 尾气超标  */
		public static final int OFF_GAS = 90;
		/** 急变道告警 */
		public static final int CHANGE_LANES  = 91;
		/** 急转弯告警 */
		public static final int WHEEL = 92;
		/** 疲劳驾驶告警 */
		public static final int DRIVER_FATIGUE = 93;
		/** 断电告警 */
		public static final int OUTAGE = 94;
		/** 区域告警 */
		public static final int AREA = 95;
		/** 紧急告警 */
		public static final int URGENT = 96;
		/** 碰撞告警*/
		public static final int COLLISION = 97;
		/** 防拆告警 */
		public static final int STEALS = 98;
		/** 非法进入告警 */
		public static final int ENTER = 99;
		/** 非法点火告警 */
		public static final int UNIGNITION  = 100;
		/** OBD剪线告警   */
		public static final int TRIM = 101;
		/** 点火告警  */
		public static final int IGNITION = 102;
		/** 熄火告警  */
		public static final int FLAMEOUT = 103;
		/** MIL故障告警 */
		public static final int  MIL = 104;
		/** 未锁车告警  */
		public static final int UNLOCK = 105;
		/** 未刷卡告警  */
		public static final int UNCARD = 106;
		/** 危险驾驶告警  */
		public static final int DANGEROUS_DRIVING = 107;
		/** 震动告警  */
		public static final int SHAKE = 108;
	}
	
	/**
	 * 
	 * @author wj
	 *
	 */
	public class DrivingBehaviorType {
		
		/** 安全驾驶 */
		public static final int SAFETY  = 0;
		/** 经济驾驶 */
		public static final int ECONOMIC = 1;
		
	}
}
