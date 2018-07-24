package com.htdz.liteguardian.message;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClientSessionHandler extends IoHandlerAdapter {
	@Override
	public void sessionCreated(IoSession session) throws Exception {
		session.getConfig().setIdleTime(IdleStatus.BOTH_IDLE, 500);
	}

	private final static Logger LOGGER = LoggerFactory.getLogger(ClientSessionHandler.class);

	@Override
	public void sessionOpened(IoSession session) {
		SessionManager sessionManager = SessionManager.getInstance();
        sessionManager.addSession(session);
	}

	@Override
	public void messageReceived(IoSession session, Object message) {
		
	}

	@Override
	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		if (session.getBothIdleCount()> 50) {
			session.close(true);
		} 
		/*else {
			// TODO heartbeat
			session.write(" ");
		}*/
	}

	@Override
	public void exceptionCaught(IoSession session, Throwable cause) {
	    session.close(true);
	    
	}
}
