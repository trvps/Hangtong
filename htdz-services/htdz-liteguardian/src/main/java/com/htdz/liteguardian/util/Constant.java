package com.htdz.liteguardian.util;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

public class Constant {
	public static final String charset = "utf-8";
	public static final CharsetEncoder charsetEncoder = Charset.forName(charset).newEncoder();
	public static final CharsetDecoder charsetDecoder = Charset.forName(charset).newDecoder();
}
