package com.htdz.gpsmessage;

import java.util.ArrayList;
import java.util.List;
/**
 * 打包协议
 * @author user
 *
 */
public class GpsMessageProtocolPackage {
	private GpsMessageHeadVO messageHeadVo;

	public GpsMessageProtocolPackage(GpsMessageHeadVO messageHeadVo) {
		this.messageHeadVo = messageHeadVo;
	}

	//将Byte列表转换为byte数组
	public byte[] packMsgByteArray(List<Byte> bMsgBodyList) {
		List<Byte> bMsgList = packMsg(bMsgBodyList);

		int bMsgListLength = bMsgList.size();
		byte[] bMsgByteArray = new byte[bMsgListLength];

		for (int i = 0; i <= bMsgList.size() - 1; i++) {
			bMsgByteArray[i] = bMsgList.get(i).byteValue();
		}

		return bMsgByteArray;
	}
	
	private List<Byte> packMsg(List<Byte> bMsgBodyList) {
		List<Byte> bMsgList = new ArrayList<Byte>();

		GpsMessageHeadVO msgHeadVo = this.messageHeadVo;
		short msgId = msgHeadVo.getMsgId();
		short protocolType = msgHeadVo.getProtocolType();
		String equipId = msgHeadVo.getEquipId();
		short msgSquice = msgHeadVo.getMsgSquice();

		byte[] bMsgId = short2byte(msgId);
		bMsgList.add(bMsgId[0]);
		bMsgList.add(bMsgId[1]);

		byte[] bMsgLength = short2byte(bMsgBodyList.size());
		bMsgList.add(bMsgLength[0]);
		bMsgList.add(bMsgLength[1]);

		byte[] bProtocolType = short2byte(protocolType);
		bMsgList.add(bProtocolType[0]);
		bMsgList.add(bProtocolType[1]);

		byte[] bEquipId = equipId.getBytes();
		for (int i = 0; i <= 19; i++) {
			if (i <= bEquipId.length - 1) {
				bMsgList.add(bEquipId[i]);
			} else {
				bMsgList.add((byte) 0);
			}
		}

		byte[] bMsgSquice = short2byte(msgSquice);
		bMsgList.add(bMsgSquice[0]);
		bMsgList.add(bMsgSquice[1]);

		for (int i = 0; i <= bMsgBodyList.size() - 1; i++) {
			bMsgList.add(bMsgBodyList.get(i).byteValue());
		}

		byte checkCode = generateCheckCode(bMsgList);
		bMsgList.add(checkCode);

		bMsgList = convertMsg(bMsgList);

		bMsgList.add(0, (byte) 0x7e);
		bMsgList.add((byte) 0x7e);
		return bMsgList;
	}

	private List<Byte> convertMsg(List<Byte> bMsgList) {
		int i = 0;
		int p = bMsgList.size();
		while (i < p) {
			if (bMsgList.get(i).byteValue() == (byte) 0x7e) {
				bMsgList.set(i, (byte) 0x7d);
				bMsgList.add(i + 1, (byte) 0x02);
				i++;
				p++;
				
				continue;
			}

			if (bMsgList.get(i).byteValue() == (byte) 0x7d) {
				bMsgList.add(i + 1, (byte) 0x01);
				p++;
			}

			i++;
		}

		return bMsgList;
	}

	private byte generateCheckCode(List<Byte> bMsgList) {
		byte checkCode = 0;

		if ((bMsgList != null) && (bMsgList.size() >= 28)) {
			checkCode = bMsgList.get(0).byteValue();

			for (int i = 1; i <= bMsgList.size() - 1; i++) {
				checkCode = (byte) (checkCode ^ bMsgList.get(i).byteValue());
			}
		}

		return checkCode;
	}

	public byte[] short2byte(int n) {
		byte[] b = new byte[2];
		b[0] = ((byte) (n >> 8));
		b[1] = ((byte) n);
		return b;
	}

	public byte[] intToByte(int n) {
		byte[] b = new byte[4];
		b[0] = ((byte) (n >> 24));
		b[1] = ((byte) (n >> 16));
		b[2] = ((byte) (n >> 8));
		b[3] = ((byte) n);
		return b;
	}

	public byte[] long2byte(long n) {
		byte[] b = new byte[8];
		b[0] = ((byte) (int) (n >> 56));
		b[1] = ((byte) (int) (n >> 48));
		b[2] = ((byte) (int) (n >> 40));
		b[3] = ((byte) (int) (n >> 32));
		b[4] = ((byte) (int) (n >> 24));
		b[5] = ((byte) (int) (n >> 16));
		b[6] = ((byte) (int) (n >> 8));
		b[7] = ((byte) (int) n);
		return b;
	}
}
