package com.htdz.common.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.apache.commons.codec.binary.Base64;

import com.htdz.common.LogManager;

public class FileUtils {
	/**
	 * 根据BYTE数字获取文件
	 * 
	 * @param bfile
	 * @param filePath
	 * @param fileName
	 */
	public static void saveFile(byte[] bfile, String filePath, String fileName) {
		BufferedOutputStream bos = null;
		FileOutputStream fos = null;
		File file = null;

		try {
			File dir = new File(filePath);

			// 判断文件目录是否存在
			if (!dir.exists()) {
				dir.mkdirs();
			}

			file = new File(filePath + File.separator + fileName);

			fos = new FileOutputStream(file);
			bos = new BufferedOutputStream(fos);

			bos.write(bfile);
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		} finally {
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					LogManager.exception(e.getMessage(), e);
				}
			}
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException e) {
					LogManager.exception(e.getMessage(), e);
				}
			}
		}
	}

	/**
	 * 将文件转成base64 字符串
	 * 
	 * @param path文件路径
	 * @return *
	 * @throws Exception
	 */
	public static String encodeBase64File(String path) throws Exception {
		File file = new File(path);
		FileInputStream inputFile = new FileInputStream(file);
		byte[] buffer = new byte[(int) file.length()];
		inputFile.read(buffer);
		inputFile.close();
		return new Base64().encodeBase64String(buffer);
	}

	/**
	 * 将base64字符解码保存文件
	 * 
	 * @param base64Code
	 * @param targetPath
	 * @throws Exception
	 */
	public static void decoderBase64File(String base64Code, String targetPath)
			throws Exception {
		byte[] buffer = new Base64().decodeBase64(base64Code);
		FileOutputStream out = new FileOutputStream(targetPath);
		out.write(buffer);
		out.close();

	}

	/**
	 * 将base64字符保存文本文件
	 * 
	 * @param base64Code
	 * @param targetPath
	 * @throws Exception
	 */
	public static void toFile(String base64Code, String targetPath)
			throws Exception {

		byte[] buffer = base64Code.getBytes();
		FileOutputStream out = new FileOutputStream(targetPath);
		out.write(buffer);
		out.close();
	}

	/**
	 * 得到amr的时长
	 * 
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static long getAmrDuration(File file) throws IOException {
		long duration = -1;
		int[] packedSize = { 12, 13, 15, 17, 19, 20, 26, 31, 5, 0, 0, 0, 0, 0,
				0, 0 };
		RandomAccessFile randomAccessFile = null;
		try {
			randomAccessFile = new RandomAccessFile(file, "rw");
			long length = file.length();// 文件的长度
			int pos = 6;// 设置初始位置
			int frameCount = 0;// 初始帧数
			int packedPos = -1;
			// ///////////////////////////////////////////////////
			byte[] datas = new byte[1];// 初始数据值
			while (pos <= length) {
				randomAccessFile.seek(pos);
				if (randomAccessFile.read(datas, 0, 1) != 1) {
					duration = length > 0 ? ((length - 6) / 650) : 0;
					break;
				}
				packedPos = (datas[0] >> 3) & 0x0F;
				pos += packedSize[packedPos] + 1;
				frameCount++;
			}
			// ///////////////////////////////////////////////////
			duration += frameCount * 20;// 帧数*20
		} finally {
			if (randomAccessFile != null) {
				randomAccessFile.close();
			}
		}
		// 删除文件
		file.delete();
		return duration / 1000;
	}

	public static byte[] File2byte(String filePath) {
		byte[] buffer = null;
		try {
			File file = new File(filePath);
			FileInputStream fis = new FileInputStream(file);
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] b = new byte[8192];
			int bytesRead;
			while ((bytesRead = fis.read(b, 0, 8192)) != -1) {
				bos.write(b, 0, bytesRead);
			}
			fis.close();
			bos.close();
			buffer = bos.toByteArray();
			// 删除文件
			file.delete();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return buffer;
	}

	public static void main(String[] args) {
		try {
			String base64Code = encodeBase64File("D:/0101-2011-qqqq.tif");
			System.out.println(base64Code);
			decoderBase64File(base64Code, "D:/2.tif");
			toFile(base64Code, "D:\\three.txt");
		} catch (Exception e) {
			e.printStackTrace();

		}

	}
}
