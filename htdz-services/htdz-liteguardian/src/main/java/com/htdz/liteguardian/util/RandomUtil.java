package com.htdz.liteguardian.util;

import java.util.Random;

public class RandomUtil {
	public static String getRandomString(int length) {
		if (length < 1) {
			return null;
		}
		char[] numbersAndLetters = ("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ").toCharArray();
		char[] randomBuffer = new char[length];
		for(int i = 0; i < randomBuffer.length; i++) {
			randomBuffer[i] = numbersAndLetters[new Random().nextInt(61)];
		}
		return new String(randomBuffer);
	}
}
