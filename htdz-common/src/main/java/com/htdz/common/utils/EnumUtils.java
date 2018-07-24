package com.htdz.common.utils;

/**
 * 枚举，内部类形式
 * 
 * @author wj
 *
 */
public class EnumUtils {

	/**
	 * 
	 * @author wj 推送消息类型 msgType ：0表示位置数据，1表示警情数据，：2表示上下线状态数据，3点火数据, 4
	 *         litefamily 提醒消息
	 *
	 */
	public class PushMsgType {

		/** 位置数据 */
		public static final int GPS = 0;
		/** 警情数据 */
		public static final int ALARM = 1;
		/** 上下线状态数据 */
		public static final int ONLINE_STATUS = 2;
		/** 点火数据 */
		public static final int OCC = 3;
		/** litefamily 提醒消息 */
		public static final int REMIND = 4;
		/** 发送语音消息 */
		public static final int VOICE = 5;
	}

	/**
	 * 
	 * @author wj
	 *
	 */
	public class AlarmType {

		/** SOS紧急报警 */
		public static final int SOS = 6;
		/** 越界报警 */
		public static final int OUT = 29;
		/** 超速告警 */
		public static final int SPEED = 81;
		/** 低电压告警 */
		public static final int LOW_VOLTAGE = 82;
		/** 水温告警 */
		public static final int WATER = 83;
		/** 急加速告警 */
		public static final int SPEED_UP = 84;
		/** 急减速告警 */
		public static final int SPEED_DOWN = 85;
		/** 停车未熄火告警 */
		public static final int UNFLAMEOUT = 86;
		/** 拖吊告警 */
		public static final int TOWED = 87;
		/** 转速高告警 */
		public static final int SPEED_HIGH = 88;
		/** 上电告警 */
		public static final int POWER_ON = 89;
		/** 尾气超标 */
		public static final int OFF_GAS = 90;
		/** 急变道告警 */
		public static final int CHANGE_LANES = 91;
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
		/** 碰撞告警 */
		public static final int COLLISION = 97;
		/** 防拆告警 */
		public static final int STEALS = 98;
		/** 非法进入告警 */
		public static final int ENTER = 99;
		/** 非法点火告警 */
		public static final int UNIGNITION = 100;
		/** OBD剪线告警 */
		public static final int TRIM = 101;
		/** 点火告警 */
		public static final int IGNITION = 102;
		/** 熄火告警 */
		public static final int FLAMEOUT = 103;
		/** MIL故障告警 */
		public static final int MIL = 104;
		/** 未锁车告警 */
		public static final int UNLOCK = 105;
		/** 未刷卡告警 */
		public static final int UNCARD = 106;
		/** 危险驾驶告警 */
		public static final int DANGEROUS_DRIVING = 107;
		/** 震动告警 */
		public static final int SHAKE = 108;
	}

	/**
	 * 
	 * @author wj 手机类型：0：安卓 1：IOS
	 *
	 */
	public class PhoneType {

		public static final int ANDROID = 0;
		public static final int IOS = 1;
	}

	/**
	 * 
	 * @author wj 消息是否推送成功：0:未推送 1：已推送
	 *
	 */
	public class PushState {

		public static final int FAILED = 0;
		public static final int SUCCESS = 1;
	}

	/**
	 * 证书类型
	 * 
	 * @author wj
	 *
	 */
	public class CertificateType {

		/** 航通守护者 APP 证书 */
		public static final String LITE_GUARDIAN = "liteguardian";
		/** litefamily APP 证书 */
		public static final String LITE_FAMILY = "litefamily";
	}

	/**
	 * 订单状态
	 * 
	 * @author wj
	 *
	 */
	public class Constanct {

		/*
		 * 订单状态待支付
		 */
		public static final int LITE_GUARDIAN = 1;

		/*
		 * 订单支付成功
		 */
		public static final int ORDER_SUCCESS = 2;

		/*
		 * 订单支付失败
		 */
		public static final int ORDER_FAILED = 3;
	}

	/** 状态枚举 */
	public class Status {
		/** 删除 */
		public static final int DELETE = -1;
		/** 正常 */
		public static final int NORMAL = 0;
		/** 锁定 */
		public static final int LOCK = 1;
	}

	/** 结果类型枚举 */
	public class ResultCode {
		/** 成功 */
		public static final int SUCCESS = 0;
		/** 失败 */
		public static final int ERROR = 1;

	}

	/** API版本类型枚举 */
	public class Version {
		/** API版本2 */
		public static final String TWO = "Version2";
		/** API版本3 */
		public static final String THREE = "Version3";

	}
}
