package com.htdz.common.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class HttpUtils {
	public static class StringBodyPair {
		public String name;
		public StringBody strbody;

		public StringBodyPair(String name, StringBody strbody) {
			this.name = name;
			this.strbody = strbody;
		}
	}

	public static class FileBodyPair {
		public String name;
		public FileBody filebody;

		public FileBodyPair(String name, FileBody filebody) {
			this.name = name;
			this.filebody = filebody;
		}
	}

	public static class InputStreamBodyPair {
		public String name;
		public InputStreamBody inputstreambody;

		public InputStreamBodyPair(String name, InputStreamBody inputstreambody) {
			this.name = name;
			this.inputstreambody = inputstreambody;
		}
	}

	public static class ByteArrayBodyPair {
		public String name;
		public ByteArrayBody bytearraybody;

		public ByteArrayBodyPair(String name, ByteArrayBody bytearraybody) {
			this.name = name;
			this.bytearraybody = bytearraybody;
		}
	}

	/**
	 * get
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doGet(String host, String path, String method,
			Map<String, String> headers, Map<String, String> querys)
			throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpGet request = new HttpGet(buildUrl(host, path, querys));
		if (headers != null && headers.size() > 0) {
			for (Map.Entry<String, String> e : headers.entrySet()) {
				request.addHeader(e.getKey(), e.getValue());
			}
		}

		return httpClient.execute(request);
	}

	/**
	 * post form
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param bodys
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPost(String host, String path, String method,
			Map<String, String> headers, Map<String, String> querys,
			Map<String, String> bodys) throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPost request = new HttpPost(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (bodys != null) {
			List<NameValuePair> nameValuePairList = new ArrayList<NameValuePair>();

			for (String key : bodys.keySet()) {
				nameValuePairList.add(new BasicNameValuePair(key, bodys
						.get(key)));
			}
			UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(
					nameValuePairList, "utf-8");
			formEntity
					.setContentType("application/x-www-form-urlencoded; charset=UTF-8");
			request.setEntity(formEntity);

			System.out.println("request body: "
					+ EntityUtils.toString(formEntity));
		}

		return httpClient.execute(request);
	}

	/**
	 * Post String
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param body
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPost(String host, String path, String method,
			Map<String, String> headers, Map<String, String> querys, String body)
			throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPost request = new HttpPost(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (StringUtils.isNotBlank(body)) {
			request.setEntity(new StringEntity(body, "utf-8"));

		}

		System.out.println("request body:" + body);

		return httpClient.execute(request);
	}

	/**
	 * Post stream
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param body
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPost(String host, String path, String method,
			Map<String, String> headers, Map<String, String> querys, byte[] body)
			throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPost request = new HttpPost(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (body != null) {
			request.setEntity(new ByteArrayEntity(body));

		}

		return httpClient.execute(request);
	}

	/**
	 * Post MultiPart Form Data
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param strBodyParts
	 * @param fileBodyParts
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPostMultiPart(String host, String path,
			String method, Map<String, String> headers,
			Map<String, String> querys, List<StringBodyPair> strBodyParts,
			List<FileBodyPair> fileBodyParts,
			List<InputStreamBodyPair> inputstreamBodyParts,
			List<ByteArrayBodyPair> bytearrayBodyParts) throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPost request = new HttpPost(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		MultipartEntityBuilder meb = MultipartEntityBuilder.create();
		if (strBodyParts != null) {
			for (StringBodyPair sbp : strBodyParts)
				meb.addPart(sbp.name, sbp.strbody);
		}

		if (fileBodyParts != null) {
			for (FileBodyPair fbp : fileBodyParts)
				meb.addPart(fbp.name, fbp.filebody);
		}

		if (inputstreamBodyParts != null) {
			for (InputStreamBodyPair isbp : inputstreamBodyParts)
				meb.addPart(isbp.name, isbp.inputstreambody);
		}

		if (bytearrayBodyParts != null) {
			for (ByteArrayBodyPair babp : bytearrayBodyParts)
				meb.addPart(babp.name, babp.bytearraybody);
		}

		request.setEntity(meb.build());

		return httpClient.execute(request);
	}

	/**
	 * Put String
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param body
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPut(String host, String path, String method,
			Map<String, String> headers, Map<String, String> querys, String body)
			throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPut request = new HttpPut(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (StringUtils.isNotBlank(body)) {
			request.setEntity(new StringEntity(body, "utf-8"));
		}

		return httpClient.execute(request);
	}

	/**
	 * Put stream
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @param body
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doPut(String host, String path, String method,
			Map<String, String> headers, Map<String, String> querys, byte[] body)
			throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpPut request = new HttpPut(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		if (body != null) {
			request.setEntity(new ByteArrayEntity(body));
		}

		return httpClient.execute(request);
	}

	/**
	 * Delete
	 * 
	 * @param host
	 * @param path
	 * @param method
	 * @param headers
	 * @param querys
	 * @return
	 * @throws Exception
	 */
	public static HttpResponse doDelete(String host, String path,
			String method, Map<String, String> headers,
			Map<String, String> querys) throws Exception {
		HttpClient httpClient = wrapClient(host);

		HttpDelete request = new HttpDelete(buildUrl(host, path, querys));
		for (Map.Entry<String, String> e : headers.entrySet()) {
			request.addHeader(e.getKey(), e.getValue());
		}

		return httpClient.execute(request);
	}

	private static String buildUrl(String host, String path,
			Map<String, String> querys) throws UnsupportedEncodingException {
		StringBuilder sbUrl = new StringBuilder();
		sbUrl.append(host);
		if (!StringUtils.isBlank(path)) {
			sbUrl.append(path);
		}
		if (null != querys) {
			StringBuilder sbQuery = new StringBuilder();
			for (Map.Entry<String, String> query : querys.entrySet()) {
				if (0 < sbQuery.length()) {
					sbQuery.append("&");
				}
				if (StringUtils.isBlank(query.getKey())
						&& !StringUtils.isBlank(query.getValue())) {
					sbQuery.append(query.getValue());
				}
				if (!StringUtils.isBlank(query.getKey())) {
					sbQuery.append(query.getKey());
					if (!StringUtils.isBlank(query.getValue())) {
						sbQuery.append("=");
						sbQuery.append(URLEncoder.encode(query.getValue(),
								"utf-8"));
						// sbQuery.append(query.getValue());
					}
				}
			}
			if (0 < sbQuery.length()) {
				sbUrl.append("?").append(sbQuery);
			}
		}

		System.out.println("request url: " + sbUrl.toString());

		return sbUrl.toString();
	}

	private static HttpClient wrapClient(String host) {
		HttpClient httpClient = new DefaultHttpClient();
		if (host.startsWith("https://")) {
			sslClient(httpClient);
		}

		return httpClient;
	}

	private static void sslClient(HttpClient httpClient) {
		try {
			SSLContext ctx = SSLContext.getInstance("TLS");
			X509TrustManager tm = new X509TrustManager() {
				public X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(X509Certificate[] xcs, String str) {

				}

				public void checkServerTrusted(X509Certificate[] xcs, String str) {

				}
			};
			ctx.init(null, new TrustManager[] { tm }, null);
			SSLSocketFactory ssf = new SSLSocketFactory(ctx);
			ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
			ClientConnectionManager ccm = httpClient.getConnectionManager();
			SchemeRegistry registry = ccm.getSchemeRegistry();
			registry.register(new Scheme("https", 443, ssf));
		} catch (KeyManagementException ex) {
			throw new RuntimeException(ex);
		} catch (NoSuchAlgorithmException ex) {
			throw new RuntimeException(ex);
		}
	}

	/**
	 * 封装了采用HttpClient发送HTTP请求的方法
	 * 
	 * @see 本工具所采用的是HttpComponents-Client-4.2.1
	 * @see 
	 *      ======================================================================
	 *      =============================
	 * @see 开发HTTPS应用时，时常会遇到两种情况
	 * @see 1、测试服务器没有有效的SSL证书,客户端连接时就会抛异常
	 * @see javax.net.ssl.SSLPeerUnverifiedException: peer not authenticated
	 * @see 2、测试服务器有SSL证书,但可能由于各种不知名的原因,它还是会抛一堆烂码七糟的异常,诸如下面这两种
	 * @see javax.net.ssl.SSLException: hostname in certificate didn't match:
	 *      <123.125.97.66> != <123.125.97.241>
	 * @see javax.net.ssl.SSLHandshakeException:
	 *      sun.security.validator.ValidatorException: PKIX path building
	 *      failed: sun.security.provider.certpath.SunCertPathBuilderException:
	 *      unable to find valid certification path to requested target
	 * @see 
	 *      ======================================================================
	 *      =============================
	 * @see 这里使用的是HttpComponents-Client-4.2.1创建的连接,所以就要告诉它使用一个不同的TrustManager
	 * @see 由于SSL使用的模式是X.509,对于该模式,Java有一个特定的TrustManager,称为X509TrustManager
	 * @see TrustManager是一个用于检查给定的证书是否有效的类,所以我们自己创建一个X509TrustManager实例
	 * @see 而在X509TrustManager实例中
	 *      ,若证书无效,那么TrustManager在它的checkXXX()方法中将抛出CertificateException
	 * @see 既然我们要接受所有的证书,那么X509TrustManager里面的方法体中不抛出异常就行了
	 * @see 然后创建一个SSLContext并使用X509TrustManager实例来初始化之
	 * @see 接着通过SSLContext创建SSLSocketFactory
	 *      ,最后将SSLSocketFactory注册给HttpClient就可以了 /** 发送HTTP_GET请求
	 * 
	 * @see 1)该方法会自动关闭连接,释放资源
	 * @see 2)方法内设置了连接和读取超时时间,单位为毫秒,超时或发生其它异常时方法会自动返回"通信失败"字符串
	 * @see 3)请求参数含中文时,经测试可直接传入中文,HttpClient会自动编码发给Server,应用时应根据实际效果决定传入前是否转码
	 * @see 4)该方法会自动获取到响应消息头中[Content-Type:text/html;
	 *      charset=GBK]的charset值作为响应报文的解码字符集
	 * @see 若响应消息头中无Content-Type属性,则会使用HttpClient内部默认的ISO-8859-1作为响应报文的解码字符集
	 * @param requestURL
	 *            请求地址(含参数)
	 * @return 远程主机响应正文
	 */
	public static String get(String reqURL) {
		String respContent = "通信失败"; // 响应内容
		HttpClient httpClient = new DefaultHttpClient(); // 创建默认的httpClient实例
		// 设置代理服务器
		// httpClient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY,
		// new HttpHost("10.0.0.4", 8080));
		httpClient.getParams().setParameter(
				CoreConnectionPNames.CONNECTION_TIMEOUT, 10000); // 连接超时10s
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
				20000); // 读取超时20s
		HttpGet httpGet = new HttpGet(reqURL); // 创建org.apache.http.client.methods.HttpGet
		try {
			HttpResponse response = httpClient.execute(httpGet); // 执行GET请求
			HttpEntity entity = response.getEntity(); // 获取响应实体
			if (null != entity) {
				// respCharset=EntityUtils.getContentCharSet(entity)也可以获取响应编码,但从4.1.3开始不建议使用这种方式
				Charset respCharset = ContentType.getOrDefault(entity)
						.getCharset();
				respContent = EntityUtils.toString(entity, respCharset);
				// Consume response content
				EntityUtils.consume(entity);
			}
			System.out
					.println("-------------------------------------------------------------------------------------------");
			StringBuilder respHeaderDatas = new StringBuilder();
			for (Header header : response.getAllHeaders()) {
				respHeaderDatas.append(header.toString()).append("\r\n");
			}
			String respStatusLine = response.getStatusLine().toString(); // HTTP应答状态行信息
			String respHeaderMsg = respHeaderDatas.toString().trim(); // HTTP应答报文头信息
			String respBodyMsg = respContent; // HTTP应答报文体信息
			System.out.println("HTTP应答完整报文=[" + respStatusLine + "\r\n"
					+ respHeaderMsg + "\r\n\r\n" + respBodyMsg + "]");
			System.out
					.println("-------------------------------------------------------------------------------------------");
		} catch (ConnectTimeoutException cte) {
			// Should catch ConnectTimeoutException, and don`t catch
			// org.apache.http.conn.HttpHostConnectException
			System.out.println("请求通信[" + reqURL + "]时连接超时");
		} catch (SocketTimeoutException ste) {
			System.out.println("请求通信[" + reqURL + "]时读取超时");
		} catch (ClientProtocolException cpe) {
			// 该异常通常是协议错误导致:比如构造HttpGet对象时传入协议不对(将'http'写成'htp')or响应内容不符合HTTP协议要求等
			System.out.println("请求通信[" + reqURL + "]时协议异常");
		} catch (ParseException pe) {
			System.out.println("请求通信[" + reqURL + "]时解析异常");
		} catch (IOException ioe) {
			// 该异常通常是网络原因引起的,如HTTP服务器未启动等
			System.out.println("请求通信[" + reqURL + "]时网络异常");
		} catch (Exception e) {
			System.out.println("请求通信[" + reqURL + "]时偶遇异常");
		} finally {
			// 关闭连接,释放资源
			httpClient.getConnectionManager().shutdown();
		}
		return respContent;
	}

	/**
	 * 发送HTTP_POST请求
	 * 
	 * @see 1)该方法允许自定义任何格式和内容的HTTP请求报文体
	 * @see 2)该方法会自动关闭连接,释放资源
	 * @see 3)方法内设置了连接和读取超时时间,单位为毫秒,超时或发生其它异常时方法会自动返回"通信失败"字符串
	 * @see 4)请求参数含中文等特殊字符时,可直接传入本方法,并指明其编码字符集encodeCharset参数,方法内部会自动对其转码
	 * @see 5)该方法在解码响应报文时所采用的编码,取自响应消息头中的[Content-Type:text/html;
	 *      charset=GBK]的charset值
	 * @see 若响应消息头中未指定Content-Type属性,则会使用HttpClient内部默认的ISO-8859-1
	 * @param reqURL
	 *            请求地址
	 * @param reqData
	 *            请求参数,若有多个参数则应拼接为param11=value11&22=value22&33=value33的形式
	 * @param encodeCharset
	 *            编码字符集,编码请求数据时用之,此参数为必填项(不能为""或null)
	 * @return 远程主机响应正文
	 */
	public static String post(String reqURL, String reqData,
			String encodeCharset) {
		String reseContent = "通信失败";
		HttpClient httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(
				CoreConnectionPNames.CONNECTION_TIMEOUT, 10000);
		httpClient.getParams().setParameter(CoreConnectionPNames.SO_TIMEOUT,
				20000);
		HttpPost httpPost = new HttpPost(reqURL);
		// 由于下面使用的是new
		// StringEntity(....),所以默认发出去的请求报文头中CONTENT_TYPE值为text/plain;
		// charset=ISO-8859-1
		// 这就有可能会导致服务端接收不到POST过去的参数,比如运行在Tomcat6.0.36中的Servlet,所以我们手工指定CONTENT_TYPE头消息
		httpPost.setHeader(HTTP.CONTENT_TYPE,
				"application/x-www-form-urlencoded; charset=" + encodeCharset);
		try {
			httpPost.setEntity(new StringEntity(reqData == null ? "" : reqData,
					encodeCharset));
			HttpResponse response = httpClient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				reseContent = EntityUtils.toString(entity, ContentType
						.getOrDefault(entity).getCharset());
				EntityUtils.consume(entity);
			}
		} catch (ConnectTimeoutException cte) {
			System.out.println("请求通信[" + reqURL + "]时连接超时");
		} catch (SocketTimeoutException ste) {
			System.out.println("请求通信[" + reqURL + "]时读取超时");
		} catch (Exception e) {
			System.out.println("请求通信[" + reqURL + "]时偶遇异常");
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
		return reseContent;
	}

}
