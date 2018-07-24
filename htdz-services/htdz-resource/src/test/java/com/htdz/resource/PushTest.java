package com.htdz.resource;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;

import com.htdz.common.LogManager;
import com.htdz.common.utils.HttpUtils;
import com.htdz.common.utils.HttpUtils.ByteArrayBodyPair;
import com.htdz.common.utils.HttpUtils.FileBodyPair;
import com.htdz.common.utils.HttpUtils.InputStreamBodyPair;
import com.htdz.common.utils.HttpUtils.StringBodyPair;

public class PushTest {

	public static void main(String[] args) {
		LogManager.info("----------PushTest----------");
		multiPargTest();
		// testRemove6();
	}

	public static void testRemove6() {
		List<String> strings = new ArrayList<>();
		strings.add("a");
		strings.add("b");
		strings.add("c");
		strings.add("d");

		Iterator<String> iterator = strings.iterator();
		while (iterator.hasNext()) {
			String next = iterator.next();
			strings.remove(next);
		}

		System.out.println(strings);
	}

	public static void multiPargTest() {
		Map<String, String> headers = new HashMap<String, String>();
		Map<String, String> querys = null;

		// headers.put("Content-Type", "multipart/form-data;
		// boundary=----WebKitFormBoundaryrGKCBY7qhFd3TrwA");

		try {
			List<StringBodyPair> strBodyParts = new ArrayList<StringBodyPair>();
			List<FileBodyPair> fileBodyParts = new ArrayList<FileBodyPair>();
			List<InputStreamBodyPair> inputstreamBodyParts = new ArrayList<InputStreamBodyPair>();
			List<ByteArrayBodyPair> bytearrayBodyParts = new ArrayList<ByteArrayBodyPair>();

			// strBodyParts.add(new StringBodyPair("function", new
			// StringBody("setUserPortrait", ContentType.TEXT_PLAIN)));
			// strBodyParts.add(
			// new StringBodyPair("username", new
			// StringBody("tangrui@castelbds.com", ContentType.TEXT_PLAIN)));
			strBodyParts
					.add(new StringBodyPair("name", new StringBody("tangrui@castelbds.com", ContentType.TEXT_PLAIN)));
			strBodyParts.add(new StringBodyPair("type", new StringBody("2", ContentType.TEXT_PLAIN)));

			// fileBodyParts.add(new FileBodyPair("setUserPortrait",
			// new FileBody(new
			// File("C:\\Users\\user\\Desktop\\1525246425.png"),
			// ContentType.IMAGE_PNG,
			// "1525246425.png")));

			// strBodyParts.add(new StringBodyPair("name", new
			// StringBody("8308628499", ContentType.TEXT_PLAIN)));
			// strBodyParts.add(new StringBodyPair("type", new StringBody("1",
			// ContentType.TEXT_PLAIN)));

			InputStream inputStream = new FileInputStream("C:\\Users\\user\\Desktop\\789456.png");
			InputStreamBody inputstreambody = new InputStreamBody(inputStream, ContentType.IMAGE_PNG, "789456.png");
			InputStreamBodyPair isbp = new InputStreamBodyPair("789456.png", inputstreambody);
			inputstreamBodyParts.add(isbp);

			// InputStream inputStream1 = new
			// FileInputStream("C:\\Users\\user\\Desktop\\789456.png");
			// InputStreamBody inputstreambody1 = new
			// InputStreamBody(inputStream1, ContentType.IMAGE_PNG,
			// "789456.png");
			// InputStreamBodyPair isbp1 = new InputStreamBodyPair("789456.png",
			// inputstreambody1);
			// inputstreamBodyParts.add(isbp1);

			// inputstreamBodyParts.add(isbp);
			// fileBodyParts.add(new FileBodyPair("file2",
			// new FileBody(new
			// File("C:\\Users\\user\\Desktop\\[2018-05-03]log.log"),
			// ContentType.TEXT_PLAIN,
			// "[2018-05-03]log.txt")));

			byte[] b = "ByteArrayBodyPair test".getBytes("utf-8");
			ByteArrayBodyPair babp = new ByteArrayBodyPair("123.jpg",
					new ByteArrayBody(b, ContentType.APPLICATION_OCTET_STREAM, "123.jpg"));
			bytearrayBodyParts.add(babp);

			LogManager.info("发送请求-----------------------------------------");
			// http://172.18.11.37:7600/upload
			// http://47.75.81.174:10100/upload
			HttpResponse response = HttpUtils.doPostMultiPart("http://218.17.161.66:10235/upload/", null, null, headers,
					querys, strBodyParts, fileBodyParts, inputstreamBodyParts, null);
			String respbody = EntityUtils.toString(response.getEntity(), "utf-8");
			// 218.17.161.66
			// 转发响应header
			Header[] allheaders = response.getAllHeaders();
			for (Header header : allheaders) {
				LogManager.info("{}: {}", header.getName(), header.getValue());
			}

			LogManager.info("transferURL result: {}", respbody);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}

}
