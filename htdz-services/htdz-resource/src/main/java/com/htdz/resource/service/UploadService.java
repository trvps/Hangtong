package com.htdz.resource.service;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.htdz.common.utils.DateTimeUtil;
import com.htdz.db.service.ResourceLogService;
import com.htdz.def.data.ApiParameter;
import com.htdz.def.dbmodel.ResourceLog;
import com.htdz.def.view.ResourceInfo;

@Service
public class UploadService {

	@Autowired
	private Environment env;

	@Autowired
	private ResourceLogService resourceLogService;

	public ResourceInfo upload(MultipartFile mf, String type, String name) throws IOException {
		String foldername = new String();
		String filename = mf.getOriginalFilename();
		String filenameExt = filename.substring(filename.lastIndexOf("."));
		String newfilename = new String();
		if (filenameExt.equals(".apk")) {
			newfilename = filename;
		} else {
			newfilename = DateTimeUtil.getCurrentUtcDatetime("yyyyMMddHHmmss")
					+ Integer.toString(new Random().nextInt(10000)) + filenameExt;
		}

		String localfileName = new String();
		String relPath = new String();
		if (!TextUtils.isEmpty(type)) {
			if (type.equals(ApiParameter.UploadFileStatus.DEVICEPORTRAIT.toString())) {
				foldername = env.getProperty("device.folder");
			} else if (type.equals(ApiParameter.UploadFileStatus.USERPORTRAIT.toString())) {
				foldername = env.getProperty("user.folder");
			} else if (type.equals(ApiParameter.UploadFileStatus.DEVICEPHOTO.toString())) {
				foldername = env.getProperty("device.photo.folder");
			} else if (type.equals(ApiParameter.UploadFileStatus.ANDRIODAPK.toString())) {
				foldername = env.getProperty("andriod.apk.folder");
			}
		}

		if (!TextUtils.isEmpty(foldername)) {

			String localfileDir = env.getProperty("upload.path") + File.separator + foldername;
			File dir = new File(localfileDir);
			if (!dir.exists()) {
				dir.mkdir();
			}

			localfileName = localfileDir + File.separator + newfilename;
			relPath = env.getProperty("virtual.file.location") + "/" + foldername + "/" + newfilename;

		} else {
			localfileName = env.getProperty("upload.path") + File.separator + newfilename;
			relPath = env.getProperty("virtual.file.location") + "/" + newfilename;
		}

		File source = new File(localfileName);
		mf.transferTo(source);

		String url = env.getProperty("webpath") + relPath;

		ResourceLog resourceLog = new ResourceLog();
		resourceLog.setAddress(localfileName);
		resourceLog.setCreatTime(DateTimeUtil.strToDateLong(DateTimeUtil.getCurrentUtcDatetime()));
		resourceLog.setName(name);
		if (!TextUtils.isEmpty(type)) {
			resourceLog.setType(Integer.parseInt(type));
		}
		resourceLogService.add(resourceLog);

		ResourceInfo resourceInfo = new ResourceInfo();
		resourceInfo.setOrgfilename(filename);
		resourceInfo.setNewfilepath(localfileName);
		resourceInfo.setUrl(url);

		return resourceInfo;
	}
}
