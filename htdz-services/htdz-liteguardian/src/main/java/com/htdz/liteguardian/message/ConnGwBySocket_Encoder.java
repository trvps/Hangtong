package com.htdz.liteguardian.message;

public class ConnGwBySocket_Encoder {	
	public static byte[] encode(CastelMessage message)
	{
		if (message != null && (message instanceof CastelMessage)) 
		{
			CastelMessage curMsgObj = (CastelMessage) message;
			curMsgObj.InitPackage();
			
			byte[] bMsgPackArray = curMsgObj.getMsgByte();
	
			return bMsgPackArray;
		}
		else
		{
			return null;
		}
	}
}
