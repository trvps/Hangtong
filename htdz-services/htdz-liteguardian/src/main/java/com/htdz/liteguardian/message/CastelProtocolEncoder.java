package com.htdz.liteguardian.message;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class CastelProtocolEncoder implements ProtocolEncoder {
	public void dispose(IoSession arg0) throws Exception {
		//
	}

	public void encode(IoSession session, Object message,ProtocolEncoderOutput output) throws Exception {
		if (message != null && (message instanceof CastelMessage)) {
			CastelMessage curMsgObj = (CastelMessage) message;
			curMsgObj.InitPackage();
			
			byte[] bMsgPackArray = curMsgObj.getMsgByte();

			IoBuffer ioBuffer = IoBuffer.allocate(bMsgPackArray.length);
			ioBuffer.setAutoExpand(true);
			ioBuffer.put(bMsgPackArray);

			ioBuffer.flip();
			output.write(ioBuffer);
		}
	}
}
