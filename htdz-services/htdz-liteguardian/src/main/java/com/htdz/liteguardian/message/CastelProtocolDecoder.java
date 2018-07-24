package com.htdz.liteguardian.message;

import java.util.Map;

import org.apache.log4j.Logger;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class CastelProtocolDecoder implements ProtocolDecoder {
	private static final Logger log = Logger.getLogger(CastelProtocolDecoder.class);	
	
	// 可变的IoBuffer数据缓冲区
    private IoBuffer buff = IoBuffer.allocate(200).setAutoExpand(true);
    public void decode(IoSession session, IoBuffer ioBuffer,ProtocolDecoderOutput output) throws Exception {
        log.info(bytesToHexString(ioBuffer.array()));
		
		int byteCount = 0;
		int flagCount=0;
		while (ioBuffer.hasRemaining()) 
		{			
			byte curByte = ioBuffer.get();
			buff.put(curByte);
			byteCount++;
			
            if (curByte == (byte)0x7e) 
            {
            	flagCount+=1;
            	
            	if(flagCount==2)
            	{
            		if(byteCount>=31)
            		{
	            		buff.flip();
	            		byte [] msgByteArray=new byte[buff.limit()];
	            		buff.get(msgByteArray);
	            
						if(msgByteArray!=null && msgByteArray.length>=31 && msgByteArray[0]==(byte)0x7e && msgByteArray[msgByteArray.length-1]==(byte)0x7e)
						{
							log.info("CastelMessage.getMsgHeadId");
							short msgID=CastelMessage.getMsgHeadId(msgByteArray);
							Class<?> castelMessage= Class.forName("com.castel.family.message.CastelMessage_0x"+Integer.toHexString(msgID));
							Object obj= castelMessage.newInstance();
							CastelMessage curMsgObj=(CastelMessage) obj;
							
							log.info("CastelMessage.InitAnalyze");
							curMsgObj.InitAnalyze(msgByteArray);
							if (curMsgObj.CkeckCodeIsCorrect()) 
							{
								CastelMessageHeadVO msgHeadVo= curMsgObj.getMsgHead();
								Map<Integer,Object> msgBodyVo = curMsgObj.getMsgBodyMap();
								
								curMsgObj.setMsgBodyMapVo(msgBodyVo);
								curMsgObj.setMsgHeadVo(msgHeadVo);
								
								output.write(curMsgObj);
							}
						}
            		}
            		
            		byteCount = 0;
            		flagCount=0;
            		buff = IoBuffer.allocate(200).setAutoExpand(true);
            	}
            }
		}
	}
	
	//截取数组
	public byte[] byteArraySub(byte[] array,int startindex,int endindex)
	{
		if(endindex<=startindex)
		{
			return null;
		}
		else
		{
			byte[] returnArray=new byte[endindex-startindex+1];
			
			for(int i=0;i<=returnArray.length-1;i++)
			{
				returnArray[i]=array[startindex+i];
			}
			
			return returnArray;
		}
	}

	//BYTE转化位16进制字符串
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		
		return stringBuilder.toString();
	}

	public void dispose(IoSession arg0) throws Exception {
		// TODO Auto-generated method stub
		
	}

	public void finishDecode(IoSession arg0, ProtocolDecoderOutput arg1)
			throws Exception {
		// TODO Auto-generated method stub
		
	}
}
