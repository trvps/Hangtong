package com.htdz.gateway;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.htdz.common.BaseController;
import com.htdz.common.Consts;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;
import com.htdz.common.utils.HttpUtils;
import com.htdz.common.utils.HttpUtils.InputStreamBodyPair;
import com.htdz.common.utils.HttpUtils.StringBodyPair;
import com.htdz.common.utils.NetUtils;
import com.htdz.def.data.RPCResult;
import com.htdz.def.data.RouteInfo;
import com.htdz.gateway.service.dubbo.DataServiceConsumer;
import com.htdz.gateway.service.dubbo.DeviceServiceConsumer;
import com.htdz.gateway.service.dubbo.LitefamilyServiceConsumer;
import com.htdz.gateway.service.dubbo.LiteguardianServiceConsumer;
import com.htdz.gateway.service.dubbo.RegServiceConsumer;

@Service
public class APIEngine {
	@Autowired
	private DeviceServiceConsumer deviceConsumerService;

	@Autowired
	private RegServiceConsumer regConsumerService;

	@Autowired
	private LitefamilyServiceConsumer litefamilyService;

	@Autowired
	private DataServiceConsumer dataConsumerService;

	@Autowired
	private LiteguardianServiceConsumer LiteguardianConsumer;

	public void handleDeviceRegisted(RouteInfo ri, String deviceName, String deviceSession) {
		try {
			deviceConsumerService.handleDeviceRegisted(ri, deviceName, deviceSession);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}

	public void handleDeviceUnregisted(RouteInfo ri, String deviceName, String deviceSession) {
		try {
			deviceConsumerService.handleDeviceUnregisted(ri, deviceName, deviceSession);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}

	public RPCResult deviceRequestDispatch(RouteInfo ri, String deviceName, String deviceSession, byte[] data) {
		try {
			if (data.length < Consts.LOG_MAX_DATASIZE) {
				LogManager.info("[收到设备请求] {} {} \n  text: {} \n  bytes: {}", deviceName, deviceSession,
						DataUtil.bytesToString(data), DataUtil.bytesToHexString(data, "[", "]", null, false));
			} else {
				LogManager.info("[收到设备请求] {} {} datalen: {}", deviceName, deviceSession, data.length);
			}

			RPCResult result = deviceConsumerService.handleDeviceMessage(ri, deviceName, deviceSession, data);

			if (result != null && result.getRpcResult() != null) {
				byte[] retdata = result.resultToBytes();
				if (retdata.length < Consts.LOG_MAX_DATASIZE) {
					LogManager.info("[发送设备响应] {} {} \n  text: {} \n  bytes: {}", deviceName, deviceSession,
							result.toString(),
							DataUtil.bytesToHexString(result.resultToBytes(), "[", "]", null, false));
				} else {
					LogManager.info("[发送设备响应] {} {} datalen: {}", deviceName, deviceSession, result.toString());
				}
			} else {
				LogManager.info("[无需发送设备响应]--------------------------");
			}

			return result;
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
			return RPCResult.failed();
		}
	}

	public RPCResult serviceRequestDispatch(RouteInfo ri, String serviceName, String path, Map<String, String> headers,
			Map<String, String[]> params, byte[] reqBody) {
		RPCResult result = new RPCResult();
		if (serviceName.equals("reg") || serviceName.equals("RemotingAPI"))
			result = regConsumerService.hanleHttpRequest(ri, path, headers, params, reqBody);
		else if (serviceName.equals("data"))
			result = dataConsumerService.hanleHttpRequest(ri, path, headers, params, reqBody);
		else if (serviceName.equals("litefamily"))
			result = litefamilyService.hanleHttpRequest(ri, path, headers, params, reqBody);
		else if (serviceName.equals("device"))
			result = deviceConsumerService.hanleHttpRequest(ri, path, headers, params, reqBody);
		else if (serviceName.equals("WebAPIVersion3"))
			result = LiteguardianConsumer.hanleHttpRequest(ri, path, headers, params, reqBody);
		return result;
	}

	public Matcher UrlPatternMatch(String url, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(url);
		return matcher;
	}

	public RPCResult serviceRequestDispatch(HttpServletRequest request) {
		try {
			// http://localhost:7000/service/device/test/test1/test2
			String url = request.getRequestURL().toString();

			String serviceName = "";
			String serviceMethod = "";

			// if (!matcher.find())
			// return RPCResult.failed();

			Matcher matcherService = UrlPatternMatch(url, "^https*://(.+)/service/(.+?)/(.+)");
			Matcher matcherWebApi2d = UrlPatternMatch(url, "^https*://(.+)/WebApi2d/(.+)");
			Matcher matcherSyncData = UrlPatternMatch(url, "^https*://(.+)/SyncData/(.+)");

			Map<String, String> headers = new HashMap<String, String>();

			if (matcherService.find()) {
				serviceName = matcherService.group(2);
				serviceMethod = matcherService.group(3);
			} else if (matcherWebApi2d.find()) {
				serviceName = matcherWebApi2d.group(2);
				serviceMethod = "";
				headers.put("sessionID", request.getSession().getId());
				LogManager.info("request.getSession().getId() = {}", request.getSession().getId());
			} else if (matcherSyncData.find()) {
				serviceName = matcherSyncData.group(2);
				serviceMethod = "";
			} else {
				return RPCResult.failed();
			}

			boolean content_type_is_multipart = false;
			// header
			Enumeration<String> enumheaders = request.getHeaderNames();
			while (enumheaders.hasMoreElements()) {
				String header = enumheaders.nextElement();
				String value = request.getHeader(header);
				headers.put(header, request.getHeader(header));

				if (header.equalsIgnoreCase("Content-Type")) {
					// multipart/form-data
					if (value.indexOf("multipart/form-data") != -1)
						content_type_is_multipart = true;
				}
			}

			// 传入客户端ip
			headers.put("HTDZ-Client-IP", NetUtils.getRemoteIpAddr(request));

			// params
			Map<String, String[]> params = request.getParameterMap();

			// body
			byte[] reqBody = null;
			if (content_type_is_multipart) {
				MultipartHttpServletRequest mrequest = (MultipartHttpServletRequest) request;
				MultiValueMap<String, MultipartFile> mvm = mrequest.getMultiFileMap();
				if (mvm.size() > 0) {
					for (Entry<String, List<MultipartFile>> entry : mvm.entrySet()) {
						for (MultipartFile mf : entry.getValue()) {
							reqBody = mf.getBytes();
						}
					}
				}
			} else {
				reqBody = BaseController.readRequestBody(request);
			}

			if (!content_type_is_multipart) {
				LogManager.info("收到请求--------------------------" + "\n  {} \n \n  {}",
						BaseController.printRequestHeader(request), DataUtil.bytesToString(reqBody));
			} else {
				LogManager.info("收到请求--------------------------" + "\n  {} \n ",
						BaseController.printRequestHeader(request));
			}

			RouteInfo ri = RouteInfo.build(GatewayEngine.getServerLocal());
			RPCResult result = serviceRequestDispatch(ri, serviceName, serviceMethod, headers, params, reqBody);

			LogManager.info("发送响应--------------------------" + "\n  {}", result.toString());

			return result;
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return RPCResult.failed();
	}

	public Map<String, String> convertParameterMap(HttpServletRequest request) {
		// params
		Map<String, String[]> params = request.getParameterMap();

		Map<String, String> map = new HashMap<String, String>();
		Iterator<Entry<String, String[]>> iterator = params.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<String, String[]> entry = iterator.next();

			// 抛弃多余的value，实际场景不会有一个key对应多个value的情形，如果有，需要再完善
			map.put(entry.getKey(), entry.getValue()[0]);
		}

		return map;
	}

	/**
	 * 转发请求
	 * @param request
	 * @param transferURL
	 * @param response
	 * @param downloadmode
	 */
	public void serviceTransfer_HTTP(HttpServletRequest request, String transferURL, HttpServletResponse response) {
		try {
			boolean content_type_is_multipart = false;

			// 请求header
			Map<String, String> headers = new HashMap<String, String>();
			Enumeration<String> enumheaders = request.getHeaderNames();
			while (enumheaders.hasMoreElements()) {
				String header = enumheaders.nextElement();
				String value = request.getHeader(header);
				if (!header.equalsIgnoreCase("Content-Length"))
					headers.put(header, request.getHeader(header));

				if (header.equalsIgnoreCase("Content-Type")) {
					// multipart/form-data
					if (value.indexOf("multipart/form-data") != -1)
						content_type_is_multipart = true;
				}
			}

			// 如果是multipart/form-data格式的请求
			if (content_type_is_multipart) {
				serviceTransfer_HTTP_Upload(request, transferURL, response);
				return;
			}

			// 请求params
			Map<String, String> querys = convertParameterMap(request);

			// 请求body
			byte[] reqBody = null;// BaseController.readRequestBody(request);

			StringBuilder sbQuery = new StringBuilder();
			for (Map.Entry<String, String> query : querys.entrySet()) {
				if (0 < sbQuery.length()) {
					sbQuery.append("&");
				}
				if (StringUtils.isBlank(query.getKey()) && !StringUtils.isBlank(query.getValue())) {
					sbQuery.append(query.getValue());
				}
				if (!StringUtils.isBlank(query.getKey())) {
					sbQuery.append(query.getKey());
					if (!StringUtils.isBlank(query.getValue())) {
						sbQuery.append("=");
						sbQuery.append(URLEncoder.encode(query.getValue(), "utf-8"));
					}
				}
			}

			reqBody = sbQuery.toString().getBytes("utf-8");

			LogManager.info("收到中转请求--------------------------" + "\n  {} \n \n  {}",
					BaseController.printRequestHeader(request), DataUtil.bytesToString(reqBody));

			// 转发请求
			HttpResponse resp = HttpUtils.doPost(transferURL, null, null, headers, null, reqBody);

			// 转发响应header
			Header[] allheaders = resp.getAllHeaders();
			for (Header header : allheaders) {
				response.setHeader(header.getName(), header.getValue());
			}

			// 转发数据
			if (resp.getEntity().getContentLength() > 10 * 1024) {
				LogManager.info("发送中转数据--------------------------");
				// 下载类的大数据直接转发，不缓存打印
				IOUtils.copy(resp.getEntity().getContent(), response.getOutputStream());
			} else {
				byte[] respBytes = EntityUtils.toByteArray(resp.getEntity());
				response.getOutputStream().write(respBytes);

				LogManager.info("发送中转响应--------------------------" + "\n  {}", DataUtil.bytesToString(respBytes));
			}
			response.flushBuffer();
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}

	/**
	 * 转发请求
	 * @param request
	 * @param transferURL
	 * @param response
	 * @param downloadmode
	 */
	public void serviceTransfer_HTTP(HttpServletRequest request, String transferURL, HttpServletResponse response,
			byte[] body) {
		try {
			boolean content_type_is_form = false;

			// 请求header
			Map<String, String> headers = new HashMap<String, String>();
			Enumeration<String> enumheaders = request.getHeaderNames();
			while (enumheaders.hasMoreElements()) {
				String header = enumheaders.nextElement();
				String value = request.getHeader(header);
				if (!header.equalsIgnoreCase("Content-Length"))
					headers.put(header, request.getHeader(header));

				if (header.equalsIgnoreCase("Content-Type")) {
					// application/x-www-form-urlencoded;charset=utf-8
					if (value.indexOf("x-www-form-urlencoded") != -1)
						content_type_is_form = true;
				}
			}

			// 请求params
			Map<String, String> querys = null;

			// 请求body
			byte[] reqBody = body;

			if (!content_type_is_form) {
				querys = convertParameterMap(request);
				// 接收不到了，已经在body参数中
				// reqBody = BaseController.readRequestBody(request);
			}

			LogManager.info("收到中转请求--------------------------" + "\n  {} \n \n  {}",
					BaseController.printRequestHeader(request), DataUtil.bytesToString(reqBody));

			// 转发请求
			HttpResponse resp = HttpUtils.doPost(transferURL, null, null, headers, querys, reqBody);

			// 转发响应header
			Header[] allheaders = resp.getAllHeaders();
			for (Header header : allheaders) {
				response.setHeader(header.getName(), header.getValue());
			}

			// 转发数据
			byte[] respBytes = EntityUtils.toByteArray(resp.getEntity());
			response.getOutputStream().write(respBytes);
			response.flushBuffer();

			LogManager.info("发送中转响应--------------------------" + "\n  {}", DataUtil.bytesToString(respBytes));
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}

	/**
	 * 转发请求
	 * @param request
	 * @param transferURL
	 * @param response
	 */
	public void serviceTransfer_HTTP_Download(HttpServletRequest request, String transferURL,
			HttpServletResponse response) {
		try {
			boolean content_type_is_form = false;

			// 请求header
			Map<String, String> headers = new HashMap<String, String>();
			Enumeration<String> enumheaders = request.getHeaderNames();
			while (enumheaders.hasMoreElements()) {
				String header = enumheaders.nextElement();
				String value = request.getHeader(header);
				if (!header.equalsIgnoreCase("Content-Length"))
					headers.put(header, request.getHeader(header));

				if (header.equalsIgnoreCase("Content-Type")) {
					// application/x-www-form-urlencoded;charset=utf-8
					if (value.indexOf("x-www-form-urlencoded") != -1)
						content_type_is_form = true;
				}
			}

			// 请求params
			Map<String, String> querys = null;

			if (!content_type_is_form) {
				querys = convertParameterMap(request);
			}

			LogManager.info("收到中转请求--------------------------" + "\n  {}}", BaseController.printRequestHeader(request));

			// 转发请求
			HttpResponse resp = HttpUtils.doGet(transferURL, null, null, headers, querys);

			// 转发响应header
			Header[] allheaders = resp.getAllHeaders();
			for (Header header : allheaders) {
				response.setHeader(header.getName(), header.getValue());
			}

			// 转发数据
			LogManager.info("发送中转数据--------------------------");
			// 下载类的大数据直接转发，不缓存打印
			IOUtils.copy(resp.getEntity().getContent(), response.getOutputStream());
			response.flushBuffer();
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}

	/**
	 * 转发请求
	 * @param request
	 * @param transferURL
	 * @param response
	 */
	public void serviceTransfer_HTTP_Upload(HttpServletRequest request, String transferURL,
			HttpServletResponse response) {
		try {
			// 请求header
			Map<String, String> headers = new HashMap<String, String>();
			Enumeration<String> enumheaders = request.getHeaderNames();
			while (enumheaders.hasMoreElements()) {
				String header = enumheaders.nextElement();
				String value = request.getHeader(header);
				if (!header.equalsIgnoreCase("Content-Length") && !header.equalsIgnoreCase("Content-Type"))
					headers.put(header, request.getHeader(header));
			}

			// 请求params
			Map<String, String> querys = null;

			LogManager.info("收到中转请求--------------------------" + "\n  {}}", BaseController.printRequestHeader(request));

			List<StringBodyPair> strBodyParts = new ArrayList<StringBodyPair>();
			List<InputStreamBodyPair> inputstreamBodyParts = new ArrayList<InputStreamBodyPair>();

			Map<String, String[]> params = request.getParameterMap();
			for (Entry<String, String[]> entry : params.entrySet()) {
				StringBody strbody = new StringBody(entry.getValue()[0], ContentType.MULTIPART_FORM_DATA);
				StringBodyPair sbp = new StringBodyPair(entry.getKey(), strbody);
				strBodyParts.add(sbp);
			}

			MultipartHttpServletRequest mrequest = (MultipartHttpServletRequest) request;
			MultiValueMap<String, MultipartFile> mvm = mrequest.getMultiFileMap();
			if (mvm.size() > 0) {
				for (Entry<String, List<MultipartFile>> entry : mvm.entrySet()) {
					for (MultipartFile mf : entry.getValue()) {
						InputStreamBody inputstreambody = new InputStreamBody(mf.getInputStream(),
								ContentType.create(mf.getContentType()), mf.getOriginalFilename());
						InputStreamBodyPair isbp = new InputStreamBodyPair(mf.getName(), inputstreambody);
						inputstreamBodyParts.add(isbp);
					}
				}
			}

			// 转发请求
			HttpResponse resp = HttpUtils.doPostMultiPart(transferURL, null, null, headers, querys, strBodyParts, null,
					inputstreamBodyParts, null);

			// 转发响应header
			Header[] allheaders = resp.getAllHeaders();
			for (Header header : allheaders) {
				response.setHeader(header.getName(), header.getValue());
			}

			// 转发数据
			byte[] respBytes = EntityUtils.toByteArray(resp.getEntity());
			response.getOutputStream().write(respBytes);
			response.flushBuffer();

			LogManager.info("发送中转响应--------------------------" + "\n  {}", DataUtil.bytesToString(respBytes));
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}
	}
}
