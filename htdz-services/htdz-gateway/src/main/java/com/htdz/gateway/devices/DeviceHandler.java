package com.htdz.gateway.devices;


import com.htdz.common.LogManager;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;


public abstract class DeviceHandler extends ChannelInboundHandlerAdapter {
	public abstract void handleRegistered(ChannelHandlerContext ctx);
	public abstract void handleData(byte[] data, ChannelHandlerContext ctx);
	public abstract void handleUnregisered(ChannelHandlerContext ctx);
	
	public DeviceHandler() {
		LogManager.info("DeviceHandler: {}", this);
	}
	
	@Override
	public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
		super.channelRegistered(ctx);
		//handleRegistered(ctx);
	}

	@Override
	public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
		super.channelUnregistered(ctx);
		//handleUnregisered(ctx);
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		handleRegistered(ctx);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		handleUnregisered(ctx);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		super.exceptionCaught(ctx, cause);
		LogManager.exception(cause.getMessage());
	}

	@Override
	public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
		super.userEventTriggered(ctx, evt);
		
		if (IdleStateEvent.class.isAssignableFrom(evt.getClass())) {  
            IdleStateEvent event = (IdleStateEvent) evt;  
            if (event.state() == IdleState.READER_IDLE) {
            	LogManager.info("read idle");  
            	//ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
            	LogManager.info("write idle");  
            } else if (event.state() == IdleState.ALL_IDLE) {
            	LogManager.info("all idle");  
            	ctx.close();
            }
        } 
	}
	
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		//super.channelRead(ctx, msg);

		ByteBuf bytebuf = (ByteBuf) msg;  
		byte[] data = new byte[bytebuf.readableBytes()];    
        // msg中存储的是ByteBuf类型的数据，把数据读取到byte[]中    
		bytebuf.readBytes(data);
  
        // 释放资源，这行很关键    
		bytebuf.release(); 
        
        handleData(data, ctx);
	}
	
	@Override  
	public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {  
		super.channelReadComplete(ctx);  
		ctx.flush();  
	} 
	
	public static void write(byte[] data, ChannelHandlerContext ctx) {
		ByteBuf encoded = ctx.alloc().buffer(data.length);    
        encoded.writeBytes(data);    
        ctx.write(encoded);    
        ctx.flush();
        
        //ctx.close();
	}
}
