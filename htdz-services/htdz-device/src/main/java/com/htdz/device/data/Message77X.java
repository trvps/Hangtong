package com.htdz.device.data;


import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import com.htdz.common.Consts;
import com.htdz.common.LogManager;
import com.htdz.common.utils.ByteArrayBuffer;
import com.htdz.common.utils.DataUtil;
import com.htdz.def.data.RPCResult;
import com.htdz.device.handler.DevicePackageHandler;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message77X {
	protected String factoryName = "";
	protected String deviceSn = "";
	protected Integer datalen = 0;
	protected String method = "";
	protected byte[] data = null;

	/**
	 * 数据包检测
	 * @param deviceName
	 * @param deviceSession
	 * @param data
	 * @param dsi
	 * @param dph
	 * @return
	 */
	public static RPCResult packageCheck(String deviceName, 
											String deviceSession, 
											byte[] data, 
											DeviceSessionInfo dsi,
											DevicePackageHandler dph) {
		RPCResult result = RPCResult.failed();

		try {
			// 更新时间戳
			dsi.updateTimeStame();

			// 检测data是否包含结束标识
			int endTAG_index = DataUtil.indexOfByteArray(data, 0, Consts.TAG_BracketR);
			if (endTAG_index == -1) {
				// 不包含结束标识，表明这是一个不完整的包，需要缓存数据
				dsi.getBaBuffer().append(data, 0, data.length);
				// return null;
				return result;
			} else {
				// 包含结束标识，需要缓存处理数据分包和粘包问题
				dsi.getBaBuffer().append(data, 0, data.length);

				byte[] buffer = dsi.getBaBuffer().buffer();
				int startTAG_index = 0;
				int preStartTAG_index = 0;
				endTAG_index = 0;

				while (startTAG_index < buffer.length) {
					// 查找开始标识
					startTAG_index = DataUtil.indexOfByteArray(buffer, preStartTAG_index, Consts.TAG_BracketL);
					if (startTAG_index == -1) {
						// 数据错误，所有数据全部抛掉
						int blen = buffer.length - preStartTAG_index;
						byte[] b = DataUtil.bytesFromBytes(buffer, preStartTAG_index, blen);
						LogManager.info("抛掉错误数据：deviceName={} deviceSession={} data={}", deviceName, deviceSession,
								DataUtil.bytesToString(b));

						dsi.getBaBuffer().clear();
						return RPCResult.success();
					} else if (startTAG_index - endTAG_index > 0) {
						// 抛掉中间冗余数据
						int blen = startTAG_index - endTAG_index;
						byte[] b = DataUtil.bytesFromBytes(buffer, endTAG_index, blen);
						LogManager.info("抛掉冗余数据：deviceName={} deviceSession={} data={}", deviceName, deviceSession,
								DataUtil.bytesToString(b));
					}

					// 查找结束标识
					endTAG_index = DataUtil.indexOfByteArray(buffer, startTAG_index, Consts.TAG_BracketR);
					if (endTAG_index == -1) {
						break;
					} else {
						// 考虑语音数据包含Consts.TAG_BracketR字符的情况
						// 这里暂不考虑语音数据片段包含Consts.TAG_BracketR，
						// 并且位置在buffer最后字节的情况

						while (endTAG_index != -1 && endTAG_index < buffer.length - 1) {
							if (buffer[endTAG_index + 1] == Consts.TAG_BracketL || buffer[endTAG_index + 1] == 0) {
								// 找到完整包
								break;
							} else {
								// 语音数据包含Consts.TAG_BracketL
								// 继续往后查找结束标识Consts.TAG_BracketL
								endTAG_index = DataUtil.indexOfByteArray(buffer, endTAG_index + 1, Consts.TAG_BracketR);
							}
						}

						if (endTAG_index == -1) {
							break;
						}
					}

					// 有完整的包 startTAG_index, endTAG_index
					endTAG_index += 1;
					int len = endTAG_index - startTAG_index;
					final byte[] b = DataUtil.bytesFromBytes(buffer, startTAG_index, len);

					// 如果有多个包，则只会返回最后一个包的处理
					// 如果设备需要接收每个指令包的处理结果，则
					// 以通过gsConsumer.pushMessageToDevice返回
					result = dph.handleDevicePackage(deviceName, deviceSession, b);

					// 往后查找
					startTAG_index = endTAG_index;
					preStartTAG_index = startTAG_index;
				}

				// 清除startTAG_index前面已处理过的数据
				if (preStartTAG_index > 0) {
					ByteArrayBuffer baBuffer = new ByteArrayBuffer();
					int leftlen = buffer.length - preStartTAG_index;
					if (leftlen > 0)
						baBuffer.append(buffer, preStartTAG_index, leftlen);
					dsi.setBaBuffer(baBuffer);
				}
			}
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
		}

		return result;
	}

	/**
	 * 从数据包解析
	 * 
	 * @param pkgData
	 * @return
	 */
	public static Message77X fromPackage(byte[] pkgData) {
		try {
			String method = "";
			String factoryName = "";
			String deviceSn = "";
			String datalen = "";
			byte[] reqData = null;

			int startIndex = 0;

			// factoryName
			byte[] bFactoryName = DataUtil.bytesFromBytes(pkgData, startIndex, Consts.TAG_BracketL, Consts.TAG_Star,
					false);
			factoryName = DataUtil.bytesToString(bFactoryName);
			if (factoryName.length() == 0) {
				return null;
			}

			// deviceSn
			startIndex += bFactoryName.length + 1;
			byte[] bDeviceSn = DataUtil.bytesFromBytes(pkgData, startIndex, Consts.TAG_Star, Consts.TAG_Star, false);
			deviceSn = DataUtil.bytesToString(bDeviceSn);
			if (deviceSn.length() == 0) {
				return null;
			}

			// datalen
			startIndex += bDeviceSn.length + 1;
			byte[] bDatalen = DataUtil.bytesFromBytes(pkgData, startIndex, Consts.TAG_Star, Consts.TAG_Star, false);
			datalen = DataUtil.bytesToString(bDatalen);
			if (datalen.length() == 0) {
				return null;
			}

			// method
			startIndex += bDatalen.length + 1;
			byte[] bMethod = DataUtil.bytesFromBytes(pkgData, startIndex, Consts.TAG_Star, Consts.TAG_Comma, false);
			if (bMethod == null || bMethod.length == 0) {
				// 单指令，没有数据
				bMethod = DataUtil.bytesFromBytes(pkgData, startIndex, Consts.TAG_Star, Consts.TAG_BracketR, false);
				method = DataUtil.bytesToString(bMethod);
			} else {
				method = DataUtil.bytesToString(bMethod);

				// 解析数据
				startIndex += bMethod.length + 1;
				reqData = DataUtil.bytesFromBytes(pkgData, startIndex, Consts.TAG_Comma, Consts.TAG_BracketR, false);
			}

			if (method.length() == 0) {
				return null;
			}

			// "method,reqData"字符串的长度
			int len = Integer.parseInt(datalen, 16);
			int dl = method.length() + (reqData == null ? 0 : 1 + reqData.length);
			if (len != dl) {
				LogManager.info("设备数据错误，长度不相等");
				return null;
			}

			Message77X msg = new Message77X();
			msg.setFactoryName(factoryName);
			msg.setDeviceSn(deviceSn);
			msg.setDatalen(len);
			msg.setMethod(method);
			msg.setData(reqData);

			return msg;
		} catch (Exception e) {
			LogManager.exception(e.getMessage(), e);
			return null;
		}
	}

	public static String lenToHexString(int length) {
		String strlen = Integer.toHexString(length);
		strlen = "000" + strlen;
		return strlen.substring(strlen.length() - 4);
	}

	public byte[] buildBytesResponse(String cmd, byte[] data) {
		String strlen = lenToHexString(cmd.getBytes().length + 1 + data.length);

		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(factoryName);
		sb.append("*");
		sb.append(deviceSn);
		sb.append("*");
		sb.append(strlen);
		sb.append("*");
		sb.append(cmd);
		sb.append(",");
		// sb.append("]");

		byte[] head = sb.toString().getBytes();

		byte[] result = new byte[head.length + data.length + 1];
		System.arraycopy(head, 0, result, 0, head.length);
		System.arraycopy(data, 0, result, head.length, data.length);

		result[result.length - 1] = ']';

		return result;
	}

	public String buildResponse(String cmd) {
		String strlen = lenToHexString(cmd.length());
		
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(factoryName);
		sb.append("*");
		sb.append(deviceSn);
		sb.append("*");
		sb.append(strlen);
		sb.append("*");
		sb.append(cmd);
		sb.append("]");

		return sb.toString();
	}

	public String getData() {
		return DataUtil.bytesToString(data);
	}

	public byte[] getDataBytes() {
		return data;
	}

	private static class Transfer77XAudioData {
		public Transfer77XAudioData(byte key1, int index, int preIndex) {
			this(key1, (byte) 0, index, preIndex);
		}

		public Transfer77XAudioData(byte key1, byte key2, int index, int preIndex) {
			this.key1 = key1;
			this.key2 = key2;

			this.index = index;
			this.preIndex = preIndex;
		}

		public byte key1 = 0;
		public byte key2 = 0;
		public int index = -1;
		public int preIndex = -1;
	}

	public static byte[] to77XAudio(byte[] data) {
		/*
		 * 0X7D --> 0X7D 0X01 0X5B --> 0X7D 0X02 0X5D --> 0X7D 0X03 0X2C --> 0X7D 0X04
		 * 0X2A --> 0X7D 0X05
		 */
		if (data == null || data.length == 0)
			return new byte[0];

		List<Transfer77XAudioData> list = new ArrayList<Transfer77XAudioData>();
		int preIndex = -1;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == (byte) 0X7D || data[i] == (byte) 0X5B || data[i] == (byte) 0X5D || data[i] == (byte) 0X2C
					|| data[i] == (byte) 0X2A) {
				list.add(new Transfer77XAudioData(data[i], i, preIndex));
				preIndex = i;
			}
		}

		// 无需转换
		if (list.size() == 0)
			return data;

		int newlen = data.length + list.size();
		byte[] retBuf = new byte[newlen];

		int index = 0;
		int lastindex = -1;
		for (int i = 0; i < list.size(); i++) {
			Transfer77XAudioData tad = list.get(i);
			lastindex = tad.index;
			int len = tad.index - (tad.preIndex + 1);

			if (len > 0) {
				System.arraycopy(data, tad.preIndex + 1, retBuf, index, len);
				index += len;
			}

			retBuf[index++] = (byte) 0X7D;
			if (tad.key1 == (byte) 0X7D) {
				retBuf[index++] = (byte) 0X01;
			} else if (tad.key1 == (byte) 0X5B) {
				retBuf[index++] = (byte) 0X02;
			} else if (tad.key1 == (byte) 0X5D) {
				retBuf[index++] = (byte) 0X03;
			} else if (tad.key1 == (byte) 0X2C) {
				retBuf[index++] = (byte) 0X04;
			} else if (tad.key1 == (byte) 0X2A) {
				retBuf[index++] = (byte) 0X05;
			}
		}

		// 处理剩余数据
		lastindex += 1;
		if (lastindex <= data.length - 1) {
			int len = data.length - lastindex;
			System.arraycopy(data, lastindex, retBuf, index, len);
		}

		return retBuf;
	}

	public static byte[] from77XAudio(byte[] data) {
		/*
		 * 0X7D 0X01 --> 0X7D 0X7D 0X02 --> 0X5B 0X7D 0X03 --> 0X5D 0X7D 0X04 --> 0X2C
		 * 0X7D 0X05 --> 0X2A
		 */
		if (data == null || data.length == 0)
			return new byte[0];

		List<Transfer77XAudioData> list = new ArrayList<Transfer77XAudioData>();
		int preIndex = -2;
		for (int i = 0; i < data.length; i++) {
			if (data[i] == (byte) 0X7D) {
				if (i < data.length - 1) {
					if (data[i + 1] == (byte) 0X01 || data[i + 1] == (byte) 0X02 || data[i + 1] == (byte) 0X03
							|| data[i + 1] == (byte) 0X04 || data[i + 1] == (byte) 0X05) {
						list.add(new Transfer77XAudioData(data[i], data[i + 1], i, preIndex));
						preIndex = i;
						i++;
					}
				}
			}
		}

		// 无需转换
		if (list.size() == 0)
			return data;

		int newlen = data.length - list.size();
		byte[] retBuf = new byte[newlen];

		int index = 0;
		int lastindex = -1;
		for (int i = 0; i < list.size(); i++) {
			Transfer77XAudioData tad = list.get(i);
			lastindex = tad.index;
			int len = tad.index - (tad.preIndex + 2);

			if (len > 0) {
				System.arraycopy(data, tad.preIndex + 2, retBuf, index, len);
				index += len;
			}

			if (tad.key1 == (byte) 0X7D && tad.key2 == (byte) 0X01) {
				retBuf[index++] = (byte) 0X7D;
			} else if (tad.key1 == (byte) 0X7D && tad.key2 == (byte) 0X02) {
				retBuf[index++] = (byte) 0X5B;
			} else if (tad.key1 == (byte) 0X7D && tad.key2 == (byte) 0X03) {
				retBuf[index++] = (byte) 0X5D;
			} else if (tad.key1 == (byte) 0X7D && tad.key2 == (byte) 0X04) {
				retBuf[index++] = (byte) 0X2C;
			} else if (tad.key1 == (byte) 0X7D && tad.key2 == (byte) 0X05) {
				retBuf[index++] = (byte) 0X2A;
			}
		}

		// 处理剩余数据
		lastindex += 2;
		if (lastindex <= data.length - 1) {
			int len = data.length - lastindex;
			System.arraycopy(data, lastindex, retBuf, index, len);
		}

		return retBuf;
	}

	// public static byte[] platformToWatch(byte[] data) {
	// int count = 0;
	// for (int i = 0; i < data.length - 1; i++) {
	// if (data[i] == (byte) 0x7D || data[i] == (byte) 0x5B
	// || data[i] == (byte) 0x5D || data[i] == (byte) 0x2C
	// || data[i] == (byte) 0x2A) {
	// count++;
	// }
	// }
	//
	// byte[] targetByte = new byte[data.length + count];
	// int index = 0;
	//
	// for (int i = 0; i < data.length - 1; i++) {
	// targetByte[index] = (byte) 0x7D;
	//
	// if ((byte) 0x7D == data[i]) {
	// targetByte[index + 1] = (byte) 0x01;
	// index++;
	// } else if ((byte) 0x5B == data[i]) {
	// targetByte[index + 1] = (byte) 0x02;
	// index++;
	// } else if ((byte) 0x5D == data[i]) {
	// targetByte[index + 1] = (byte) 0x03;
	// index++;
	// } else if ((byte) 0x2C == data[i]) {
	// targetByte[index + 1] = (byte) 0x04;
	// index++;
	// } else if ((byte) 0x2A == data[i]) {
	// targetByte[index + 1] = (byte) 0x05;
	// index++;
	// } else {
	// targetByte[index] = data[i];
	// }
	//
	// index++;
	// }
	//
	// return targetByte;
	// }
	//
	// public static byte[] watchToPlatform(byte[] data) {
	// int count = 0;
	// for (int i = 0; i < data.length - 1; i++) {
	// if (data[i] == (byte) 0x7D
	// && (data[i + 1] == (byte) 0x01
	// || data[i + 1] == (byte) 0x02
	// || data[i + 1] == (byte) 0x03
	// || data[i + 1] == (byte) 0x04 || data[i + 1] == (byte) 0x05))
	// count++;
	// }
	//
	// byte[] targetByte = new byte[data.length - count];
	// int index = 0;
	//
	// for (int i = 0; i < data.length - 1; i++) {
	// if ((byte) 0x7D == data[i]) {
	// if ((byte) 0x01 == data[i + 1]) {
	// targetByte[index] = (byte) 0x7D;
	// } else if ((byte) 0x02 == data[i + 1]) {
	// targetByte[index] = (byte) 0x5B;
	// } else if ((byte) 0x03 == data[i + 1]) {
	// targetByte[index] = (byte) 0x5D;
	// } else if ((byte) 0x04 == data[i + 1]) {
	// targetByte[index] = (byte) 0x2C;
	// } else if ((byte) 0x05 == data[i + 1]) {
	// targetByte[index] = (byte) 0x2A;
	// }
	//
	// i++;
	// } else {
	// targetByte[index] = data[i];
	// }
	//
	// index++;
	// }
	//
	// return targetByte;
	// }

	public static void parseTest() {
		String text1 = "[ABC*123*0002*LK]";
		String text2 = "[3G*3917437764*015C*UD2,220518,102245,V,22.548025,N,113.9362233,E,0.00,0.0,0.0,0,100,91,0,0,00000000,7,255,460,0,9763,3853,146,9763,3593,137,9763,3770,135,9763,4261,131,9763,4481,131,9763,3623,128,9763,4482,127,5,HT-HTDZ,80:89:17:94:30:c0,-65,DT,24:5:f:44:15:41,-67,HT-HTDZ-2,80:89:17:94:30:70,-73,360免费WiFi-89,24:5:f:22:77:89,-73,DTEC-2,30:fc:68:d2:42:8f,-85,51.2]";

		try {
			byte[] data = text2.getBytes("utf-8");

			String factoryName = "";
			String deviceSn = "";
			String datalen = "";
			String method = "";
			byte[] reqData = null;

			int startIndex = 0;

			byte[] bFactoryName = DataUtil.bytesFromBytes(data, startIndex, Consts.TAG_BracketL, Consts.TAG_Star,
					false);
			factoryName = DataUtil.bytesToString(bFactoryName);

			startIndex += bFactoryName.length + 1;
			byte[] bDeviceSn = DataUtil.bytesFromBytes(data, startIndex, Consts.TAG_Star, Consts.TAG_Star, false);
			deviceSn = DataUtil.bytesToString(bDeviceSn);

			startIndex += bDeviceSn.length + 1;
			byte[] bDatalen = DataUtil.bytesFromBytes(data, startIndex, Consts.TAG_Star, Consts.TAG_Star, false);
			datalen = DataUtil.bytesToString(bDatalen);

			startIndex += bDatalen.length + 1;

			byte[] bMethod = DataUtil.bytesFromBytes(data, startIndex, Consts.TAG_Star, Consts.TAG_Comma, false);
			if (bMethod == null || bMethod.length == 0) {
				// 单指令，没有数据
				bMethod = DataUtil.bytesFromBytes(data, startIndex, Consts.TAG_Star, Consts.TAG_BracketR, false);
				method = DataUtil.bytesToString(bMethod);
			} else {
				method = DataUtil.bytesToString(bMethod);

				// 解析数据
				startIndex += bMethod.length + 1;
				reqData = DataUtil.bytesFromBytes(data, startIndex, Consts.TAG_Comma, Consts.TAG_BracketR, false);
			}

			System.out.println(factoryName);
			System.out.println(deviceSn);
			System.out.println(datalen);
			System.out.println(method);
			System.out.println(DataUtil.bytesToString(reqData));
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		System.out.println("--------------- Message77X Main ---------------");

		// parseTest();

		byte[] data1 = new byte[] { (byte) 0X7D, (byte) 0X02, (byte) 0X5D, (byte) 0X7D, (byte) 0X05, (byte) 0X06,
				(byte) 0X7D };
		byte[] newdata1 = from77XAudio(data1);
		byte[] data2 = to77XAudio(newdata1);

		System.out.println("end");
	}
}
