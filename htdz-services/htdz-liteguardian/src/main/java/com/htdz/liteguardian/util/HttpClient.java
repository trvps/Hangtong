package com.htdz.liteguardian.util;

import java.io.IOException;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClient {
	private static final Logger log = LoggerFactory.getLogger(HttpClient.class);
	public static final RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(30000).setConnectTimeout(30000).build();
	public static final String encoding = "UTF-8";
	public static final String remoteUrl = PropertyUtil.getWebConfig("center_server_url");
	
	public static String getPost(List<NameValuePair> httpParameters, String language) {
		// http客户端
		HttpPost httpPost = new HttpPost(remoteUrl);
		httpPost.setConfig(requestConfig);
		CloseableHttpClient httpClient = HttpClients.createDefault();
		UrlEncodedFormEntity uefEntity;
		String result = "";

		try {
			uefEntity = new UrlEncodedFormEntity(httpParameters, encoding);
			httpPost.setEntity(uefEntity);
			// 设置头部信息，类似于K--V形式的
			httpPost.setHeader("Accept-Language", language);
			// System.out.println("executing request " + httppost.getURI());
			// 执行post请求
			CloseableHttpResponse response = httpClient.execute(httpPost);
			try {
				HttpEntity entity = response.getEntity();
				if (entity != null) {
					// 返回的结果
					result = EntityUtils.toString(entity, encoding);
					return result;
				}
			} finally {
				response.close();
			}
		} catch (Exception e) {
			log.error("调用中心服务器接口出错:" + e.getMessage());
			e.printStackTrace();
		} finally {
			// 关闭连接,释放资源
			try {
				httpClient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}
}
