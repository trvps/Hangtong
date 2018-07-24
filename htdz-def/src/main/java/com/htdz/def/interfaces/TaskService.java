package com.htdz.def.interfaces;

import java.util.Map;

import com.htdz.def.data.RPCResult;
import com.htdz.def.view.GpsAddress;
import com.htdz.def.view.GpsAreaInfo;
import com.htdz.def.view.PushInfo;
import com.htdz.def.view.UserConn;
import com.htdz.def.view.VoiceMsg;

public interface TaskService {

	/**
	 * 是否越界围栏 并推送消息
	 * 
	 * @param gpsAreaInfoView
	 * @return
	 */
	public RPCResult regionAnalysis(GpsAreaInfo gpsAreaInfoView);

	/**
	 * 推送消息
	 * 
	 * @param pushInfo
	 *            推送信息
	 * @param userConn
	 *            用户信息
	 * @return
	 */
	public RPCResult pushMsg(PushInfo pushInfo, UserConn userConn);

	/**
	 * 推送群聊语音消息
	 * 
	 * @param voiceMsg
	 * @return
	 */
	public RPCResult pushVoiceMsg(VoiceMsg voiceMsg);

	/**
	 * 根据用户名获取 融云token
	 * 
	 * @param name
	 * @return
	 */
	public RPCResult getRongcloudToken(String name);

	/**
	 * 根据经纬度 逆地理位置解析 获取
	 * 
	 * @param gpsAddress
	 *            必传 location 经度在前，纬度在后，经纬度间以“,”分割，经纬度小数点后不要超过 6 位
	 * @return
	 */
	public RPCResult getGpsAddress(GpsAddress gpsAddress);
	
	/**
	 * 根据基站或WIFI信息获取GPS位置
	 * @param equipId
	 * @param mapType
	 * @param bodyVo
	 * @return
	 */
	public RPCResult getGpsByWifiAndLbs(String equipId, String mapType,Map<Integer, Object> bodyVo);
}
