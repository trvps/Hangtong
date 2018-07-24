package com.htdz.liteguardian.message;

import java.net.InetSocketAddress;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.mina.core.RuntimeIoException;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.springframework.beans.factory.annotation.Value;

public class MessageListener implements ServletContextListener {

	@Value("${gw.server.ip}")
	private static String ip;
	@Value("${gw.server.port}")
	private static int port;

	private static final long CONNECT_TIMEOUT = 100 * 1000L; // 30 seconds
	private static NioSocketConnector connector = new NioSocketConnector();

	@Override
	public void contextDestroyed(ServletContextEvent servletContextEvent) {
		if (connector.isActive())
			connector.dispose();
	}

	@Override
	public void contextInitialized(ServletContextEvent servletContextEvent) {

		connector.setConnectTimeoutMillis(CONNECT_TIMEOUT);
		connector.getFilterChain().addLast("ExecutorFilter",
				new ExecutorFilter());
		connector.getFilterChain().addLast("codec",
				new ProtocolCodecFilter(new CastelTextLineCodecFactory()));
		connector.getFilterChain().addLast("logger", new LoggingFilter());

		connector.setHandler(new ClientSessionHandler());

		IoSession session;
		for (;;) {
			try {
				session = createConnection();
				break;
			} catch (RuntimeIoException e) {
				System.err.println("Failed to connect.");
				e.printStackTrace();
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	public static IoSession createConnection() {
		IoSession session;
		ConnectFuture future = connector
				.connect(new InetSocketAddress(ip, port));
		future.awaitUninterruptibly();
		session = future.getSession();
		return session;
	}

}
