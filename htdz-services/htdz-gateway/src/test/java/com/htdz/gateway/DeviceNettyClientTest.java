package com.htdz.gateway;


import com.htdz.common.LogManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;


public class DeviceNettyClientTest {
	public static String HOST = "127.0.0.1";  
    public static int PORT = 8100;  
  
    public static Bootstrap bootstrap = getBootstrap();  
    public static Channel channel = getChannel(HOST, PORT);  
  
    /** 
     * 初始化Bootstrap 
     */  
    public static final Bootstrap getBootstrap() {  
        EventLoopGroup group = new NioEventLoopGroup();  
        Bootstrap b = new Bootstrap();  
        b.group(group).channel(NioSocketChannel.class);  
        b.handler(new ChannelInitializer<Channel>() {  
            @Override  
            protected void initChannel(Channel ch) throws Exception {  
                ChannelPipeline pipeline = ch.pipeline();  
                //pipeline.addLast("frameDecoder", new LengthFieldBasedFrameDecoder(Integer.MAX_VALUE, 0, 4, 0, 4));  
                //pipeline.addLast("frameEncoder", new LengthFieldPrepender(4));  
                
                //pipeline.addLast("decoder", new StringDecoder(CharsetUtil.UTF_8));  
                //pipeline.addLast("encoder", new StringEncoder(CharsetUtil.UTF_8));  
                
                pipeline.addLast("handler", new TcpClientHandler());  
            }  
        });  
        b.option(ChannelOption.SO_KEEPALIVE, true);  
        return b;  
    }  
  
    public static final Channel getChannel(String host, int port) {  
        Channel channel = null;  
        try {  
            channel = bootstrap.connect(host, port).sync().channel();  
        } catch (Exception e) {  
            LogManager.error("连接Server(IP{},PORT{})失败", host, port, e);  
            return null;  
        }  
        return channel;  
    }  
  
    public static void sendMsg(byte[] data) throws Exception {  
        if (channel != null) {  
        	ByteBuf bb = Unpooled.buffer(data.length); 
        	bb.writeBytes(data); 
            
            channel.writeAndFlush(bb).sync();  
        } else {  
        	LogManager.warn("消息发送失败,连接尚未建立!");  
        }  
    }
    
    public static class TcpClientHandler extends SimpleChannelInboundHandler<Object> {
        @Override  
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        	ByteBuf result = (ByteBuf) msg;  
        	byte[] result1 = new byte[result.readableBytes()];
	        result.readBytes(result1);    
	        String resultStr = new String(result1);  

        	LogManager.info("client接收到服务器返回的消息:" + resultStr);  
        }
    }  
  
    /*
    public static void main(String[] args) throws Exception {  
        try {  
            long t0 = System.nanoTime();   
        	DeviceNettyClientTest.sendMsg("deviceid1".getBytes("utf-8"));    
            long t1 = System.nanoTime();  
            LogManager.info("time used:{}", t1 - t0);  
        } catch (Exception e) {  
        	LogManager.error("main err:", e);  
        }
    }
    */
}
