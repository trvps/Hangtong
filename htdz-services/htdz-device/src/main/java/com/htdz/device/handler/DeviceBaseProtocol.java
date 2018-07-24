package com.htdz.device.handler;


import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.util.EntityUtils;
import com.alibaba.fastjson.JSON;
import com.htdz.common.LogManager;
import com.htdz.common.utils.HttpUtils;
import com.htdz.common.utils.StringEncoding;
import com.htdz.db.service.DevicePhotoService;
import com.htdz.common.utils.HttpUtils.ByteArrayBodyPair;
import com.htdz.common.utils.HttpUtils.StringBodyPair;
import com.htdz.def.dbmodel.DevicePhoto;


public class DeviceBaseProtocol {
	public boolean saveDevicePhoto(String deviceSn, byte[] image, String resServerURL, DevicePhotoService devicePhotoService) {
		try {
			LogManager.info("设备上传图片数据为：" + StringEncoding.bytesToHexString(image));

			Map<String, String> headers = new HashMap<String, String>();
			List<StringBodyPair> strBodyParts = new ArrayList<StringBodyPair>();
			List<ByteArrayBodyPair> bytearrayBodyParts = new ArrayList<ByteArrayBodyPair>();

			strBodyParts.add(new StringBodyPair("name", new StringBody(deviceSn, ContentType.TEXT_PLAIN)));
			strBodyParts.add(new StringBodyPair("type", new StringBody("3", ContentType.TEXT_PLAIN)));

			String photoName = "device.jpg";
			ByteArrayBody bab = new ByteArrayBody(image, ContentType.APPLICATION_OCTET_STREAM, photoName); 
			ByteArrayBodyPair babp = new ByteArrayBodyPair(bab.getFilename(), bab);
			bytearrayBodyParts.add(babp);

			LogManager.info("发送请求-----------------------------------------");
			HttpResponse response = HttpUtils.doPostMultiPart(resServerURL, 
																null,
																null, 
																headers, 
																null, 
																strBodyParts, 
																null,
																null, 
																bytearrayBodyParts);

			String respbody = EntityUtils.toString(response.getEntity(),
					"utf-8");

			com.alibaba.fastjson.JSONObject objApi = JSON
					.parseObject(respbody);
			if (objApi.containsKey("code")
					&& objApi.get("code").toString().equals("0")) {
				String obj = JSON.toJSONString(objApi.get("data"));
				List<DevicePhoto> devicePhotoList = JSON.parseArray(obj,
						DevicePhoto.class);
				if (null != devicePhotoList && devicePhotoList.size() > 0) {
					for (DevicePhoto devicePhoto : devicePhotoList) {
						devicePhoto.setDeviceSn(deviceSn);
						devicePhoto.setCreateTime(new Date());
						devicePhotoService.add(devicePhoto);
					}
				}
			}
			
			LogManager.info("transferURL result: {}", respbody);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
			return false;
		}

		return true;
	}
}
