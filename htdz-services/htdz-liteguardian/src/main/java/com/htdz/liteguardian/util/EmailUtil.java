package com.htdz.liteguardian.util;

import java.text.MessageFormat;

import org.apache.commons.codec.binary.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.htdz.common.LanguageManager;
import com.htdz.common.LogManager;

@Service
public class EmailUtil {

	@Autowired
	private DaoUtil daoUtil;
	@Autowired
	private Environment env;

	public boolean sendRegisterEmail(String receiveEmail, String language) {
		String username = receiveEmail;

		String title = LanguageManager.getMsg(language, "email.verify_trackeremail")
				+ LanguageManager.getMsg(language, "common.company");
		String param = username + "|" + DateTimeUtil.getCurrentUtcDatetime() + "|" + RandomUtil.getRandomString(6);
		String verifycode = Base64.encodeBase64String(param.getBytes());

		String link = env.getProperty("webPath") + "verify_email.htm?lang=" + language + "&params=" + verifycode;
		String linkText = env.getProperty("webPath") + "verify_email.htm?lang=" + language + "&amp;params="
				+ verifycode;

		String content = MessageFormat.format(LanguageManager.getMsg(language, "email.verify_email"), link, linkText);

		// 获取用户版本定制的值
		String userCustomizeValue = daoUtil.getUserCustomizedApp(username);
		if (PropertyUtil.isNotBlank(userCustomizeValue)) {
			// 不是定制版
			if (userCustomizeValue.equals("0") || userCustomizeValue.equals("2") || userCustomizeValue.equals("0")
					|| userCustomizeValue.equals("4")) {
				title = LanguageManager.getMsg(language, "email.verify_trackeremail.isCustomizedApp_0");
				content = MessageFormat.format(LanguageManager.getMsg(language, "email.verify_email.isCustomizedApp_0"),
						link, linkText);
			} else {
				// 是定制版
				title = LanguageManager.getMsg(language,
						"email.verify_trackeremail.isCustomizedApp_" + userCustomizeValue);
				content = MessageFormat.format(
						LanguageManager.getMsg(language, "email.verify_email.isCustomizedApp_" + userCustomizeValue),
						link, linkText);
			}

		}

		String html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
				+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" >" + "<head>" + "    <title>Untitled Page</title>"
				+ "    <meta charset=\"utf-8\" />" + "    <style type=\"text/css\">" + "        *{margin:0;padding:0;}"
				+ "        body{font-size:14px;font-family:\"宋体\";}" + "        dl,dt,dd{list-style-type:none;}"
				+ "        h3{font-size:14px;font-weight:normal;line-height:2.4em;}"
				+ "        p{text-indent:2em;line-height:1.8em;}"
				+ "        dl dt,dl dd{text-indent:2em;line-height:1.8em;}" + "        dl dt{font-weight:bold;}"
				+ "        p i{font-style:normal;font-weight:bold;}" + "    </style>" + "</head>" + "<body>" + content
				+ "</body>" + "</html>";
		content = html;
		LogManager.info("sendRegisterEmail content:" + content);
		int userId = daoUtil.getUserId(username);
		if (userId > 0 && daoUtil.saveVerifyCode(userId, verifycode)) {
			return daoUtil.sendEmail(receiveEmail, receiveEmail, title, content, null,
					DateTimeUtil.getCurrentUtcDatetime());
		} else {
			return false;
		}
	}

	public boolean sendForgetPasswordEmail(String username, String pwd_verify_code, String language) {
		String title = LanguageManager.getMsg(language, "email.forgot_trackerpwd")
				+ LanguageManager.getMsg(language, "common.company");
		String param = username + "|" + DateTimeUtil.getCurrentUtcDatetime() + "|" + pwd_verify_code;

		String local = "CN";
		if (language != null && language.indexOf("-") != -1) {
			String[] localArray = language.split("\\-");
			if (localArray != null && localArray.length >= 1) {
				local = localArray[1].toUpperCase();
			}
		}
		// 找回密码时，把用户名带过去
		String link = env.getProperty("webPath") + "forgot_password_" + local + ".htm?lang=" + language + "&params="
				+ Base64.encodeBase64String(param.getBytes()) + "&username=" + username;
		String linkText = env.getProperty("webPath") + "forgot_password_" + local + ".htm?lang=" + language
				+ "&amp;params=" + Base64.encodeBase64String(param.getBytes()) + "&amp;username=" + username;

		String content = MessageFormat.format(LanguageManager.getMsg(language, "email.forgot_email"), username, link,
				linkText);

		// 获取用户版本定制的值
		String userCustomizeValue = daoUtil.getUserCustomizedApp(username);
		if (PropertyUtil.isNotBlank(userCustomizeValue)) {
			// 不是定制版
			if (userCustomizeValue.equals("0") || userCustomizeValue.equals("2")) {
				title = LanguageManager.getMsg(language, "email.forgot_trackerpwd.isCustomizedApp_0");
				content = MessageFormat.format(LanguageManager.getMsg(language, "email.forgot_email.isCustomizedApp_0"),
						username, link, linkText);
			} else {
				// 是定制版
				title = LanguageManager.getMsg(language,
						"email.forgot_trackerpwd.isCustomizedApp_" + userCustomizeValue);
				content = MessageFormat.format(
						LanguageManager.getMsg(language, "email.forgot_email.isCustomizedApp_" + userCustomizeValue),
						username, link, linkText);
			}

		}

		String html = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">"
				+ "<html xmlns=\"http://www.w3.org/1999/xhtml\" >" + "<head>" + "    <title>Untitled Page</title>"
				+ "    <meta charset=\"utf-8\" />" + "    <style type=\"text/css\">" + "        *{margin:0;padding:0;}"
				+ "        body{font-size:14px;font-family:\"微软雅黑\";}" + "        dl,dt,dd{list-style-type:none;}"
				+ "        h3{font-size:14px;font-weight:normal;line-height:2.4em;}"
				+ "        p{text-indent:2em;line-height:1.8em;}"
				+ "        dl dt,dl dd{text-indent:2em;line-height:1.8em;}" + "        dl dt{font-weight:bold;}"
				+ "        p i{font-style:normal;font-weight:bold;}" + "    </style>" + "</head>" + "<body>" + content
				+ "</body>" + "</html>";
		content = html;
		LogManager.info("sendForgetPasswordEmail content:" + content);
		return daoUtil.sendEmail(username, username, title, content, null, DateTimeUtil.getCurrentUtcDatetime());
	}
}
