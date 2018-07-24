package com.htdz.resource.service;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.http.util.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import com.htdz.common.LogManager;
import com.htdz.common.utils.DateTimeUtil;
import com.htdz.db.service.ResourceLogService;
import com.htdz.def.dbmodel.ResourceLog;
import com.htdz.def.view.ResourceInfo;

import lombok.Data;
import net.coobird.thumbnailator.Thumbnails;

@Service
public class ThumbnailUploadService implements ApplicationListener<ContextRefreshedEvent> {

	@Autowired
	private Environment env;
	@Autowired
	private ResourceLogService resourceLogService;
	
	// 生成缩略图的请求量不大，初始只创建一个线程
	private ExecutorService executor = Executors.newFixedThreadPool(1);
	// 同步队列
	private BlockingQueue<ThumbInfo> queue = new LinkedBlockingQueue<ThumbInfo>(10);
	
	@Data
	private static class ThumbInfo {
		private String srcFile;
		private String destFile;
		private double scale;
		private int width;
		private int height;
	}
	
	public void onApplicationEvent(ContextRefreshedEvent event) {
		executor.execute(new Runnable() {
			public void run() {
				while (true) {
					try {
						ThumbInfo ti = queue.take();
						
						// 优先scale
						if (ti.getScale() > 0)
							Thumbnails.of(ti.getSrcFile()).scale(ti.getScale()).toFile(ti.getDestFile());
						else
							Thumbnails.of(ti.getSrcFile()).size(100, 100).toFile(ti.getDestFile());
					} catch (Exception e) {
						LogManager.exception(e.getMessage(), e);
					}
				}
			}
		});
	}

	public ResourceInfo thumbnail(String original, String type, String name) throws IOException {
		String newfilename = DateTimeUtil.getCurrentUtcDatetime("yyyyMMddHHmmss")
				+ Integer.toString(new Random().nextInt(10000)) + original.substring(original.lastIndexOf("."));

		String localfileDir = env.getProperty("upload.path") + File.separator
				+ env.getProperty("device.photo.thumbnail.folder");
		File dir = new File(localfileDir);
		if (!dir.exists()) {
			dir.mkdir();
		}
		String localfileName = localfileDir + File.separator + newfilename;

		String relPath = env.getProperty("virtual.file.location") + "/"
				+ env.getProperty("device.photo.thumbnail.folder") + "/" + newfilename;

		//Thumbnails.of(original).size(100, 100).toFile(localfileName);
		// 异步处理
		try {
			ThumbInfo ti = new ThumbInfo();
			ti.setSrcFile(original);
			ti.setDestFile(localfileName);
			ti.setWidth(100);
			ti.setHeight(100);
			queue.put(ti);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		
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
		resourceInfo.setOrgfilename(original);
		resourceInfo.setNewfilepath(localfileName);
		resourceInfo.setUrlThumbnail(url);

		return resourceInfo;
	}
}
