package com.htdz.task.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.htdz.common.LogManager;
import com.htdz.common.utils.EnumUtils;
import com.htdz.def.view.UserConn;

import javapns.devices.Device;
import javapns.devices.implementations.basic.BasicDevice;
import javapns.notification.AppleNotificationServerBasicImpl;
import javapns.notification.PushNotificationManager;
import javapns.notification.PushNotificationPayload;
import javapns.notification.PushedNotification;

@Service
public class IosMsgPushHandler {

	@Autowired
	private Environment env;

	@Value("${isReleaseVersion}")
	private Boolean isReleaseVersion;
	@Value("${environment}")
	private String environment;
	@Value("${certificate.password}")
	private String password;

	public int initPushData(UserConn userConn, Integer msgType, String content, String msgHead) throws IOException {

		// 图标小红圈的数值
		int badge = 1;

		// 航通守护者 APP 证书 ,litefamily APP 证书
		String certificate = EnumUtils.CertificateType.LITE_GUARDIAN;
		if (null != userConn.getCertificate() && userConn.getCertificate() == 1) {
			certificate = EnumUtils.CertificateType.LITE_FAMILY;
		}

		// 证书目录
		// File file = ResourceUtils.getFile("classpath:certificate\\" +
		// certificate + "\\" + environment + "\\"
		// + userConn.getVersions() + "_" + userConn.getIsCustomizedApp() +
		// ".p12");
		// String path = file.getPath();

		InputStream stream = (InputStream) getClass().getClassLoader().getResourceAsStream("certificate/" + certificate
				+ "/" + environment + "/" + userConn.getVersions() + "_" + userConn.getIsCustomizedApp() + ".p12");

		// 获取警情提醒头
		String connCountry = userConn.getConnCountry() == null ? "SG" : userConn.getConnCountry();

		if (msgType == EnumUtils.PushMsgType.ALARM) {
			msgHead = env.getProperty("pushalarm_" + connCountry);
			msgHead = new String(msgHead.getBytes("iso-8859-1"));
			msgHead += "(" + userConn.getDeviceSn() + ")";
		}

		return pushDataToIosServer(msgHead, content, badge, userConn.getToken(), userConn.getVersions(), stream,
				password, isReleaseVersion);
	}

	private int pushDataToIosServer(String alarmMsgHead, String content, int badge, String tokens, String versions,
			Object certificatePath, String certificatePassword, boolean isReleaseVersion) {

		LogManager.info("pushDataToIosServer:" + content + "      " + "certificatePath:" + certificatePath + "       "
				+ "certificatePassword:" + certificatePassword);

		int successCount = 0;
		// 铃音
		String sound = "default";
		try {
			PushNotificationPayload payLoad = PushNotificationPayload.complex();

			// 消息提醒
			payLoad.addAlert(alarmMsgHead);
			// 消息内容
			payLoad.addCustomDictionary("content", content);

			// iphone应用图标上小红圈上的数值
			// payLoad.addBadge(badge);

			// 铃音
			if (!StringUtils.isBlank(sound)) {
				payLoad.addSound(sound);
			}

			PushNotificationManager pushManager = new PushNotificationManager();
			// true：表示的是产品发布推送服务 false：表示的是产品测试推送服务
			pushManager.initializeConnection(
					new AppleNotificationServerBasicImpl(certificatePath, certificatePassword, isReleaseVersion));

			List<PushedNotification> notifications = new ArrayList<PushedNotification>();

			// 发送push消息
			Device device = new BasicDevice();
			device.setToken(tokens);
			PushedNotification notification = pushManager.sendNotification(device, payLoad, true);
			notifications.add(notification);

			List<PushedNotification> failedNotifications = PushedNotification.findFailedNotifications(notifications);
			List<PushedNotification> successfulNotifications = PushedNotification
					.findSuccessfulNotifications(notifications);

			successCount = successfulNotifications.size();

			LogManager.info("===============end push ios msg successCount============:" + successCount);

			pushManager.stopConnection();
		} catch (Exception e) {
			e.printStackTrace();
			LogManager.exception(e.getMessage(), e);

		}
		return successCount;
	}
}
