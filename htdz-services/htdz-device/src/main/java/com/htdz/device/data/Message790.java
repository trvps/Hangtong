package com.htdz.device.data;


import com.htdz.common.LogManager;
import com.htdz.common.utils.EnDecoderUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@AllArgsConstructor
@NoArgsConstructor
public class Message790 extends Message77X {
	protected String secret;
	
	/**
	 	设备待传输接口数据加上私钥进行 md5 算法（用于做签名的传输接口数据无需包括数据实际内容，
	 	即只需带指令名即可，得出32位小写的md5 数据，最后把md5值放到传输接口数据的前面进行传输。
		例如
		设备待传输数据为 [SG*8800000015*0002*LK,50,100,100]， 
		私钥为 VMg3oCnND6NzhfA6dmNfSnBWZuKyLdeY。
		则抽取实际数据的 SG*8800000015*0002*LK，再加上私钥， 那么需对以下字符串做md5 
		算法：SG*8800000015*0002*LKVMg3oCnND6NzhfA6dmNfSnBWZuKyLdeY，
		Md5算出的值为 9fa56f7e41aaf7273f30faff536619e6。 
		最终终端传输数据为 [9fa56f7e41aaf7273f30faff536619e6SG*8800000015*0002*LK]
	*/
	public static String calcSign(String factoryName, 
									String deviceSn, 
									String hexDatalen, 
									String method, 
									String secret) {
		// 校验sign
		// SG*8800000015*0002*LKVMg3oCnND6NzhfA6dmNfSnBWZuKyLdeY
		StringBuilder sb = new StringBuilder();
		sb.append(factoryName);
		sb.append("*");
		sb.append(deviceSn);
		sb.append("*");
		sb.append(hexDatalen);
		sb.append("*");
		sb.append(method);
		sb.append(secret);
		
		return EnDecoderUtil.MD5Encode(sb.toString());
	}
	
	/**
	 * 从数据包解析
	 * 
	 * @param pkgData
	 * @return
	 */
	public static Message790 fromPackage(byte[] pkgData, String secret) {
		Message77X msg77x = Message77X.fromPackage(pkgData);
		if (msg77x != null) {
			String factoryName = msg77x.getFactoryName();
			String sign = "";
			if (factoryName.length() > 32) {
				sign = factoryName.substring(0, 32);
				factoryName = factoryName.substring(32);
				
				// 校验sign
				String _sign = calcSign(factoryName, 
										msg77x.getDeviceSn(), 
										Message77X.lenToHexString(msg77x.getDatalen()).toUpperCase(),
										msg77x.getMethod(),
										secret);
				if (!sign.equalsIgnoreCase(_sign)) {
					LogManager.info("设备数据签名错误, sign={}, _sign={}", sign, _sign);
					return null;
				}
			}
			
			Message790 msg = new Message790();
			msg.setFactoryName(factoryName);
			msg.setDeviceSn(msg77x.getDeviceSn());
			msg.setDatalen(msg77x.getDatalen());
			msg.setMethod(msg77x.getMethod());
			msg.setData(msg77x.getDataBytes());
			msg.setSecret(secret);

			return msg;
		}
		
		return null;
	}

	public byte[] buildBytesResponse(String cmd, byte[] data) {
		String strlen = lenToHexString(cmd.getBytes().length + 1 + data.length);
		
		String sign = calcSign(factoryName, 
								deviceSn, 
								strlen,
								cmd,
								secret);
		
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(sign+factoryName);
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
		
		String sign = calcSign(factoryName, 
								deviceSn, 
								strlen,
								cmd,
								secret);
		
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(sign+factoryName);
		sb.append("*");
		sb.append(deviceSn);
		sb.append("*");
		sb.append(strlen);
		sb.append("*");
		sb.append(cmd);
		sb.append("]");

		return sb.toString();
	}

	public static void main(String[] args) {
		System.out.println("--------------- Message77X Main ---------------");
	}
}

