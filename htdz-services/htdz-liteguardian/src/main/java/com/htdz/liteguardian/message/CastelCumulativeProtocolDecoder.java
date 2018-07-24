package com.htdz.liteguardian.message;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.CumulativeProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;

public class CastelCumulativeProtocolDecoder extends CumulativeProtocolDecoder {

	@Override
	protected boolean doDecode(IoSession session, IoBuffer ioBuffer,
			ProtocolDecoderOutput output) throws Exception {

		int startPosition = ioBuffer.position();

		while (ioBuffer.hasRemaining()) {
			byte b = ioBuffer.get();
			if ('\n' == b) {
				int currentPosition = ioBuffer.position();
				int limit = ioBuffer.limit();

				ioBuffer.position(startPosition);
				ioBuffer.limit(currentPosition);

				IoBuffer ioBuf = ioBuffer.slice();
				byte[] dest = new byte[ioBuf.limit()];
				ioBuf.get(dest);
				String s = new String(dest);
				output.write(s);

				ioBuffer.position(currentPosition);
				ioBuffer.limit(limit);

				return true;
			}
		}
		ioBuffer.position(startPosition);
		return false;
	}

}
