package com.htdz.gateway;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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

		// Pattern pattern =
		// Pattern.compile("^https*://(.+)/service/(.+?)/(.+)");
		// String url =
		// "https://localhost:7000/service/device/test/test1/test2";
		// Matcher matcher = pattern.matcher(url);
		// if (matcher.find()) {
		// LogManager.info("count={}", matcher.groupCount());
		// for (int i=0; i<matcher.groupCount() + 1; i++) {
		// LogManager.info("index={} content={}", i, matcher.group(i));
		// }
		// }

		// pushMessage();

		// transferURL();
		// transferURLTest();
		multiPargTest();
		// StringSubUtil.getRelativeURL("http://218.17.161.66:10230/image/HeadPortrait/20180517011645898.png");
	}

	public static void pushMessage() {
		Map<String, String> headers = new HashMap<String, String>();

		byte[] body;
		try {
			body = "hello device3".getBytes("utf-8");
			String devicesession = "internal-127.0.0.1-56982-0-927";
			HttpResponse response = HttpUtils.doPost("http://127.0.0.1:7000", "/push2device/device1/" + devicesession,
					null, headers, null, body);
			String respbody = EntityUtils.toString(response.getEntity(), "utf-8");
			LogManager.info("pushMessage result: {}", respbody);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}

	public static void transferURL() {
		for (int i = 0; i < 1; i++) {
			Map<String, String> headers = new HashMap<String, String>();
			Map<String, String> querys = new HashMap<String, String>();
			querys.put("name", "jack");
			querys.put("age", "好");

			// headers.put("Content-Type",
			// "application/x-www-form-urlencoded;charset=utf-8");
			// headers.put("Content-Type", "multipart/form-data;
			// boundary=----WebKitFormBoundaryrGKCBY7qhFd3TrwA");
			// headers.put("Content-Type", "application/json;charset=utf-8");
			headers.put("Content-Type", "text/xml;charset=utf-8");

			byte[] body;
			try {
				String strbody = "";

				// application/x-www-form-urlencoded
				strbody = "sex=male";

				// multipart/form-data
				strbody = "------WebKitFormBoundaryrGKCBY7qhFd3TrwA \r\n"
						+ "Content-Disposition: form-data; name=\"text\" \r\n" + "\r\n" + "title \r\n";

				// application/json, text/xml
				strbody = "{\"title\":\"test\",\"sub\":[1,2,3]}";

				body = strbody.getBytes("utf-8");
				LogManager.info("发送请求-----------------------------------------");
				HttpResponse response = HttpUtils.doPost("http://127.0.0.1:8050", "/reg/test", null, headers, querys,
						body);
				String respbody = EntityUtils.toString(response.getEntity(), "utf-8");

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

			strBodyParts.add(new StringBodyPair("function", new StringBody("setUserPortrait", ContentType.TEXT_PLAIN)));
			strBodyParts.add(
					new StringBodyPair("username", new StringBody("tangrui@castelbds.com", ContentType.TEXT_PLAIN)));

			// fileBodyParts.add(new FileBodyPair("setUserPortrait",
			// new FileBody(new
			// File("C:\\Users\\user\\Desktop\\1525246425.png"),
			// ContentType.IMAGE_PNG,
			// "1525246425.png")));

			// strBodyParts.add(new StringBodyPair("name", new
			// StringBody("8308628499", ContentType.TEXT_PLAIN)));
			// strBodyParts.add(new StringBodyPair("type", new StringBody("1",
			// ContentType.TEXT_PLAIN)));

			InputStream inputStream = new FileInputStream("C:\\Users\\user\\Desktop\\1525246425.png");
			InputStreamBody inputstreambody = new InputStreamBody(inputStream, ContentType.IMAGE_PNG, "1525246425.png");
			InputStreamBodyPair isbp = new InputStreamBodyPair("1525246425.png", inputstreambody);
			inputstreamBodyParts.add(isbp);

			// fileBodyParts.add(new FileBodyPair("file2",
			// new FileBody(new
			// File("C:\\Users\\user\\Desktop\\[2018-05-03]log.log"),
			// ContentType.TEXT_PLAIN,
			// "[2018-05-03]log.txt")));

			byte[] b = "ByteArrayBodyPair test".getBytes("utf-8");
			ByteArrayBodyPair babp = new ByteArrayBodyPair("123.jpg",
					new ByteArrayBody(b, ContentType.APPLICATION_OCTET_STREAM, "123.jpg"));
			bytearrayBodyParts.add(babp);

			// LogManager.info("发送请求-----------------------------------------");
			// HttpResponse response =
			// HttpUtils.doPostMultiPart("http://172.18.11.168:7600/upload/",
			// null, null, headers,
			// querys, strBodyParts, fileBodyParts, inputstreamBodyParts,
			// bytearrayBodyParts);
			// String respbody = EntityUtils.toString(response.getEntity(),
			// "utf-8");

			LogManager.info("发送请求-----------------------------------------");
			HttpResponse response = HttpUtils.doPostMultiPart("http://172.18.11.168:7600/upload/", null, null, headers,
					querys, strBodyParts, fileBodyParts, inputstreamBodyParts, null);
			String respbody = EntityUtils.toString(response.getEntity(), "utf-8");

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

	public static void transferURLTest() {
		for (int i = 0; i < 1; i++) {
			Map<String, String> headers = new HashMap<String, String>();
			Map<String, String> querys = null;// new HashMap<String, String>();
			headers.put("Content-Type", "application/x-www-form-urlencoded;charset=utf8");

			byte[] body;
			try {
				body = "function=setUserInfo&nickname=%E4%B8%AD".getBytes("utf-8");
				LogManager.info("发送请求-----------------------------------------");
				HttpResponse response = HttpUtils.doPost("http://127.0.0.1:8051", "/WebApi2d/WebAPIVersion3", null,
						headers, querys, body);

				String respbody = EntityUtils.toString(response.getEntity(), "utf-8");

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
}
