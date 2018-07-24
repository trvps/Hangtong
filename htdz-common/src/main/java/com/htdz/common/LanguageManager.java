package com.htdz.common;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
public class LanguageManager {
	private static MessageSource messageSource;

	public static String getMsg(String key) {
		Locale locale = LocaleContextHolder.getLocale();
		return messageSource.getMessage(key, null, locale);
	}

	public static String getMsg(String language, String key) {
		Locale locale = null;

		if (language.length() == 5) {
			locale = new Locale(language.substring(0, 2), language.substring(3, 5));
		} else if (language.length() == 2) {
			locale = new Locale(language);
		} else {
			locale = LocaleContextHolder.getLocale();
		}

		return messageSource.getMessage(key, null, locale);
	}

	@Autowired(required = true)
	public void setMessageSource(MessageSource messageSource) {
		LanguageManager.messageSource = messageSource;
	}
}
