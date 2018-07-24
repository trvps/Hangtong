package com.htdz.liteguardian.message;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class CastelTextLineCodecFactory implements ProtocolCodecFactory {

	private CastelProtocolEncoder casEncoder;
	private CastelProtocolDecoder casDecoder;
//	private CastelCumulativeProtocolDecoder casCumulativeDecoder;
	
	public CastelTextLineCodecFactory() {
		casEncoder = new CastelProtocolEncoder();
		casDecoder = new CastelProtocolDecoder();
//		casCumulativeDecoder = new CastelCumulativeProtocolDecoder();
	}
	
	@Override
	public ProtocolEncoder getEncoder(IoSession arg0) throws Exception {
		return casEncoder;
	}
	
	@Override
	public ProtocolDecoder getDecoder(IoSession arg0) throws Exception {
		return casDecoder;
//		return casCumulativeDecoder;
	}

}
