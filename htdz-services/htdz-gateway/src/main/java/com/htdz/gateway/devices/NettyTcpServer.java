package com.htdz.gateway.devices;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.htdz.common.LogManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;


public class NettyTcpServer {
	/** 用于分配处理业务线程的线程组个数 */  
    protected static final int BIZGROUPSIZE = Runtime.getRuntime().availableProcessors() * 2;  
    /** 业务出现线程大小 */  
    protected static final int BIZTHREADSIZE = 4;
   
    private String servername;
    private String ip;
   	private int port;
   	private int rwIdelSeconds = 0;
    private ChannelFuture future;
    private ExecutorService executor;
	
    
	public NettyTcpServer(String ip, int port, String servername, int rwIdelSeconds) {
		this.ip = ip;
		this.port = port;
		this.servername = servername;
		this.executor = Executors.newFixedThreadPool(1);
		this.rwIdelSeconds = rwIdelSeconds;
	}
	
	public boolean start() {
		if (future != null) {
			LogManager.info("设备监听服务{} {}:{} 已经启动过了!", this.servername, this.ip, this.port);
			return false;
		}
		
		LogManager.info("{}设备监听服务启动 {}:{}", this.servername, this.ip, this.port);
		
		executor.submit(new Runnable() {
			public void run() {
				 //boss线程监听端口，worker线程负责数据读写
	            EventLoopGroup boss = new NioEventLoopGroup(BIZGROUPSIZE);
	            EventLoopGroup worker = new NioEventLoopGroup(BIZTHREADSIZE);
	            
		        try {
		            ServerBootstrap bootstrap = new ServerBootstrap();
		            bootstrap.group(boss, worker);
		            bootstrap.channel(NioServerSocketChannel.class);

		            //设置管道工厂
		            bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
		                @Override
		                protected void initChannel(SocketChannel socketChannel) throws Exception {
		                    //获取管道
		                    ChannelPipeline pipeline = socketChannel.pipeline();
		                    
		                    // 设置空闲时间
		                    pipeline.addLast(new IdleStateHandler(0, 0, rwIdelSeconds, TimeUnit.SECONDS));
		                    
		                    //字符串解码器
		                    //pipeline.addLast(new StringDecoder());
		                    
		                    //字符串编码器
		                    //pipeline.addLast(new StringEncoder());
		                    
		                    //处理类
		                    DeviceXXXHandler dxh = new DeviceXXXHandler(servername);
		                    pipeline.addLast(dxh);
		                }
		            });

		            //1.链接缓冲池的大小（ServerSocketChannel的设置）
		            bootstrap.option(ChannelOption.SO_BACKLOG, 1024);
		            //维持链接的活跃，清除死链接(SocketChannel的设置)
		            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
		            //关闭延迟发送
		            bootstrap.childOption(ChannelOption.TCP_NODELAY, true);

		            future = bootstrap.bind(ip, port).sync();

		            LogManager.info("{}服务正在监听端口 {}:{} ...", servername, ip, port);
		           
		            future.channel().closeFuture().sync();
		            
		            LogManager.info("{}服务监听端口 {}:{} 已关闭！", servername, ip, port);
		        } catch (Exception e) {
		        	LogManager.exception(e.getMessage(), e);
		        } finally {
		        	LogManager.info("{}服务退出，释放线程池资源 {}:{}", servername, ip, port);
		            boss.shutdownGracefully();
		            worker.shutdownGracefully();
		        }
			}
		});
		
		return true;
	}
	
	public void stop() {
		if (future != null) {
			future.channel().close();
			future = null;
		}
	}
	
	public String getServername() {
		return servername;
	}

	public void setServername(String servername) {
		this.servername = servername;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}

