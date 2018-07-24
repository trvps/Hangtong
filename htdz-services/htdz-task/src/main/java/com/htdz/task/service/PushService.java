package com.htdz.task.service;

import io.rong.RongCloud;
import io.rong.messages.ProfileNtfMessage;
import io.rong.messages.VoiceMessage;
import io.rong.models.CodeSuccessResult;

import java.text.ParseException;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DateTimeUtil;
import com.htdz.common.utils.EnumUtils;
import com.htdz.db.service.PushLogService;
import com.htdz.def.dbmodel.PushLog;
import com.htdz.def.view.PushInfo;
import com.htdz.def.view.UserConn;
import com.htdz.def.view.VoiceMsg;
import com.htdz.task.util.IosMsgPushHandler;

@Service
public class PushService {

	@Autowired
	private PushLogService pushLogService;

	@Autowired
	private IosMsgPushHandler iosMsgPushHandler;
	@Value("${appKey}")
	private String appKey;
	@Value("${appSecret}")
	private String appSecret;

	/**
	 * 推送消息给APP 安卓和IOS分发 msgType：0表示位置数据，1表示警情数据，2表示状态数据 ,3点火状态 ，4litefamily提醒推送
	 * 
	 * @param pushInfo
	 * @param userConn
	 * @return
	 */
	public Integer pushMsg(PushInfo pushInfo, UserConn userConn) {

		Integer pushState = 0;
		try {
			PushLog pushLog = null;
			String msg = null;
			Integer phoneType = EnumUtils.PhoneType.ANDROID;
			if (null != userConn.getToken() && !userConn.getToken().isEmpty()) {
				phoneType = EnumUtils.PhoneType.IOS;
				StringBuffer stringBuffer = getIOSMsg(pushInfo, userConn);
				if (stringBuffer.length() > 0) {// IOS消息内容构建
					msg = stringBuffer.toString();
				}
			} else {// 安卓消息内容构建
				pushInfo.setRanges(userConn.getRanges());
				if (null == pushInfo.getDatetime()) {
					pushInfo.setDatetime(new Date());
				}
				msg = JSON.toJSONString(pushInfo,
						SerializerFeature.WriteDateUseDateFormat,
						SerializerFeature.DisableCircularReferenceDetect,
						SerializerFeature.WriteMapNullValue);
			}

			LogManager.info("推送消息：" + msg + "__msgType:"
					+ pushInfo.getMsgType() + "__userName:"
					+ userConn.getName());

			// 警情消息和提醒消息 做纪录
			if (pushInfo.getMsgType() == EnumUtils.PushMsgType.ALARM
					|| pushInfo.getMsgType() == EnumUtils.PushMsgType.REMIND) {
				pushLog = new PushLog();
				pushLog.setDeviceSn(pushInfo.getEquipId());
				pushLog.setCreateTime(new Date());
				pushLog.setMsgType(pushInfo.getMsgType());
				pushLog.setMsg(msg);
				pushLog.setPushUser(userConn.getName());
				pushLog.setIsPush(EnumUtils.PushState.FAILED);
				pushLog.setPhoneType(phoneType);
				pushLogService.add(pushLog); // 推送信息入DB
			}

			// IOS 只推送警情和 消息提醒
			if (phoneType == EnumUtils.PhoneType.IOS
					&& (pushInfo.getMsgType() == EnumUtils.PushMsgType.ALARM || pushInfo
							.getMsgType() == EnumUtils.PushMsgType.REMIND)) {
				pushState = iosMsgPushHandler.initPushData(userConn,
						pushInfo.getMsgType(), msg.toString(),
						pushInfo.getTitle());
			} else if (phoneType == EnumUtils.PhoneType.ANDROID) {// 安卓推送
				pushState = pushAndiorMsgRongcloud(userConn, msg);
			}

			// 推送成功修改状态 /时间
			if (pushState > 0 && null != pushLog && pushLog.getId() > 0) {
				pushLog.setIsPush(EnumUtils.PushState.SUCCESS);
				pushLog.setUpdateTime(new Date());
				pushLogService.modify(pushLog);
			}
		} catch (Exception e) {
			e.printStackTrace();
			LogManager.exception("pushMsg异常", e);
		}
		return pushState;

	}

	/**
	 * 推送message到IOS APP ,IOS数据需以 | 分隔拼接
	 * 
	 * @param pushInfo
	 * @param userConn
	 * @param pushLogId
	 * @return
	 * @throws Exception
	 */
	private StringBuffer getIOSMsg(PushInfo pushInfo, UserConn userConn)
			throws Exception {

		String datetime = DateTimeUtil.getDateString(pushInfo.getDatetime());
		Integer timezone = userConn.getTimezone();
		String localDateTime = DateTimeUtil.utc2Local(datetime, timezone);

		// 如果lng,lat为空 或者为0 则不推送
		StringBuffer msg = new StringBuffer();

		switch (pushInfo.getMsgType()) {
		case EnumUtils.PushMsgType.GPS:
			msg.append(pushInfo.getEquipId());
			msg.append("|" + pushInfo.getMsgType());
			msg.append("|" + pushInfo.getLng());
			msg.append("|" + pushInfo.getLat());
			msg.append("|" + pushInfo.getSpeed());
			msg.append("|" + pushInfo.getDirection());
			msg.append("|" + userConn.getRanges());
			msg.append("|" + userConn.getTimezone());
			msg.append("|" + localDateTime);

			return msg;
		case EnumUtils.PushMsgType.ALARM:
			msg.append(pushInfo.getEquipId());
			msg.append("|" + pushInfo.getMsgType());
			msg.append("|" + pushInfo.getAlarmtype());
			msg.append("|" + pushInfo.getLng());
			msg.append("|" + pushInfo.getLat());
			msg.append("|" + userConn.getRanges());
			msg.append("|" + userConn.getTimezone());
			msg.append("|" + localDateTime);

			return msg;
		case EnumUtils.PushMsgType.ONLINE_STATUS:

			msg.append(pushInfo.getEquipId());
			msg.append("|" + pushInfo.getMsgType());
			msg.append("|" + pushInfo.getOnlinestatus());
			msg.append("|" + localDateTime);

			return msg;
		case EnumUtils.PushMsgType.REMIND:
			msg.append(pushInfo.getEquipId());
			msg.append("|" + pushInfo.getMsgType());
			msg.append("|" + pushInfo.getTitle()); // 提醒内容
			msg.append("|" + localDateTime);

			return msg;
		default:
			return msg;

		}
	}

	/**
	 * 推送message到Andior APP 用第三方融云推送
	 * 
	 * @param userConn
	 * @param msg
	 * @return
	 * @throws JSONException
	 * @throws ParseException
	 */
	private Integer pushAndiorMsgRongcloud(UserConn userConn, String msg) {
		try {

			JSONObject msgObj = new JSONObject(msg);

			Integer timezone = userConn.getTimezone();
			String datetime = msgObj.getString("datetime");
			String localDateTime = DateTimeUtil.utc2Local(datetime, timezone);
			msgObj.put("localDateTime", localDateTime);
			msgObj.put("ranges", userConn.getRanges());
			msgObj.put("timezone", timezone);

			msg = msgObj.toString();

			RongCloud rongCloud = RongCloud.getInstance(appKey, appSecret);
			String[] messagePublishSystemToUserId = { userConn.getName() };

			ProfileNtfMessage messagePublishSystemTxtMessage = new ProfileNtfMessage(
					userConn.getDeviceSn(), null, msg);
			CodeSuccessResult messagePublishSystemResult = rongCloud.message
					.PublishSystem("admin", messagePublishSystemToUserId,
							messagePublishSystemTxtMessage, null,
							"{\"pushData\":\"\"}", 0, 0);
			if (null != messagePublishSystemResult
					&& messagePublishSystemResult.getCode() == 200) {
				LogManager.info("融云 消息推送成功,code:"
						+ messagePublishSystemResult.getCode() + "---userName:"
						+ userConn.getName() + "---msg:"
						+ messagePublishSystemTxtMessage);

				return messagePublishSystemResult.getCode();
			}
			LogManager.info("融云 消息推送失败,code:"
					+ messagePublishSystemResult.getCode() + "---userName:"
					+ userConn.getName() + "---msg:"
					+ messagePublishSystemTxtMessage);

		} catch (Exception e) {
			e.printStackTrace();
			LogManager.exception("融云 消息推送异常:" + e);
		}
		return 0;
	}

	/**
	 * 设备发语音给APP
	 * 
	 * @param voiceMsg
	 * @return
	 */
	public Boolean sendMessageVoice(VoiceMsg voiceMsg) {
		Boolean pushState = false;

		try {
			LogManager.info("设备发语音给APP deviceSn:" + voiceMsg.getDeviceSn());

			String[] messagePublishGroupToGroupId = { voiceMsg.getDeviceSn() };

			RongCloud rongCloud = RongCloud.getInstance(appKey, appSecret);
			// 发送群组消息方法（以一个用户身份向群组发送消息，单条消息最大 128k.每秒钟最多发送 20 条消息，每次最多向
			// 3 个群组发送，如：一次向 3 个群组发送消息，示为 3 条消息。）
			VoiceMessage voiceMessage = new VoiceMessage(voiceMsg.getContent(),
					null, voiceMsg.getDuration());
			CodeSuccessResult messagePublishGroupResult = rongCloud.message
					.publishGroup(voiceMsg.getDeviceSn(),
							messagePublishGroupToGroupId, voiceMessage, null,
							"{\"pushData\":\"\"}", 1, 1, 0);
			if (messagePublishGroupResult.getCode() == 200) {
				LogManager.info("融云 语音推送成功,code:"
						+ messagePublishGroupResult.getCode() + "---deviceSn:"
						+ voiceMsg.getDeviceSn());
				return true;
			}
			LogManager.info("融云 语音推送成功,code:"
					+ messagePublishGroupResult.getCode() + "---deviceSn:"
					+ voiceMsg.getDeviceSn());
		} catch (Exception e) {
			e.printStackTrace();
			LogManager.exception("融云 语音推送：" + e);
			return pushState;
		}
		return pushState;
	}
}
