package com.htdz.liteguardian.message;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class CastelCodecFactory implements ProtocolCodecFactory {
	private CastelProtocolEncoder protocolEncoder;
	private CastelProtocolDecoder protocolDecoder;

	public CastelCodecFactory() {
		this.protocolEncoder = new CastelProtocolEncoder();
		this.protocolDecoder = new CastelProtocolDecoder();
	}

	public ProtocolDecoder getDecoder(IoSession arg0) throws Exception {
		return this.protocolDecoder;
	}

	public ProtocolEncoder getEncoder(IoSession arg0) throws Exception {
		return this.protocolEncoder;
	}
}
