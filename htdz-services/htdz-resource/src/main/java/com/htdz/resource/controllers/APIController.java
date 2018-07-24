package com.htdz.resource.controllers;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;
import com.htdz.def.data.ApiParameter;
import com.htdz.def.data.ApiResult;
import com.htdz.def.data.Errors;
import com.htdz.def.view.ResourceInfo;
import com.htdz.resource.service.ThumbnailUploadService;
import com.htdz.resource.service.UploadService;

@RestController
public class APIController {
	@Autowired
	private ThumbnailUploadService thumbnailUploadService;
	@Autowired
	private UploadService uploadService;

	@RequestMapping("/info")
	public String info() {
		return "resource APIController";
	}

	/**
	 * 上传文件
	 */
	@RequestMapping("/upload")
	public String dataHeadPortrait(HttpServletRequest request, MultipartFile response) {
		ApiResult result = new ApiResult();
		Map<String, String[]> params = request.getParameterMap();
		List<ResourceInfo> resourceInfoList = new ArrayList<ResourceInfo>();

		String name = DataUtil.getStringFromMap(params, "name");
		String type = DataUtil.getStringFromMap(params, "type");

		try {
			MultipartHttpServletRequest mrequest = (MultipartHttpServletRequest) request;
			MultiValueMap<String, MultipartFile> mvm = mrequest.getMultiFileMap();
			if (mvm.size() > 0) {
				Iterator<Entry<String, List<MultipartFile>>> iterator = mvm.entrySet().iterator();
				while (iterator.hasNext()) {
					Entry<String, List<MultipartFile>> entry = iterator.next();
					for (MultipartFile mf : entry.getValue()) {
						ResourceInfo resourceInfo = new ResourceInfo();
						resourceInfo = uploadService.upload(mf, type, name);

						if (!TextUtils.isEmpty(type)
								&& type.equals(ApiParameter.UploadFileStatus.DEVICEPHOTO.toString())) {
							ResourceInfo tResourceInfo = thumbnailUploadService.thumbnail(resourceInfo.getNewfilepath(),
									type, name);
							resourceInfo.setUrlThumbnail(tResourceInfo.getUrlThumbnail());
						}

						resourceInfoList.add(resourceInfo);
					}
				}

				result.setData(resourceInfoList);
				result.setCode(Errors.ERR_SUCCESS);
			} else {
				result.setCode(Errors.ERR_FAILED);
				result.setMsg("no file");
			}

		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
			result.setCode(Errors.ERR_FAILED);
			result.setMsg("exception!");
		}

		return JSON.toJSONString(result, SerializerFeature.WriteDateUseDateFormat,
				SerializerFeature.DisableCircularReferenceDetect, SerializerFeature.WriteMapNullValue);

	}
}
