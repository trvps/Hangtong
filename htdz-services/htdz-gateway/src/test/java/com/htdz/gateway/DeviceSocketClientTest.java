package com.htdz.gateway;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Random;

public class DeviceSocketClientTest {
    // 搭建客户端
    public static void main(String[] args) throws IOException {
    	for (int i=0; i<1; i++) {
    		int count = 1;
    		int sleepSeconds = 60+new Random().nextInt(100);
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		new Thread(new DeviceTask((i+1), count, sleepSeconds)).start();
    	}
    }
    
    public static class DeviceTask implements Runnable {
    	private int id;
    	private int count;
    	private int sleepSeconds;
    	
    	public DeviceTask(int id, int count, int sleepSeconds) {
    		this.id = id;
    		this.count = count;
    		this.sleepSeconds = sleepSeconds;
    	}
    	
    	@Override
    	public void run() {
    		for (int i=0; i< count; i++) {
    	    	Socket socket = null;  
    	        try {  
    	        	System.out.println(System.currentTimeMillis()+ " id: " + id + " doing...");
    	        	
    	            //创建一个流套接字并将其连接到指定主机上的指定端口号  
    	            //socket = new Socket("54.179.149.239", 13333);   
    	            socket = new Socket("127.0.0.1", 10291); 
    	
    	            //读取服务器端数据    
    	            DataInputStream input = new DataInputStream(socket.getInputStream());    
    	            //向服务器端发送数据    
    	            DataOutputStream out = new DataOutputStream(socket.getOutputStream());    
    	  
    	            String str = "[3G*3703002703*0002*LK]";  
    	            out.write(str.getBytes());   
    	            
    	            
//    	            if (sleepSeconds > 0)
//    	            	Thread.sleep(sleepSeconds*1000);
//    	            
//    	            out.write(str.getBytes());  
//    	            if (sleepSeconds > 0)
//    	            	Thread.sleep(10);
    	            
    	            byte[] rdata = new byte[1024];
    	            int readlen = input.read(rdata);     
    	            if (readlen != 0) {
    	            	String rstr = new String(rdata, 0, readlen);
    	            	System.out.println(System.currentTimeMillis()+ " id: " + id +" 服务器端返回过来的是: " + rstr);  
    	            }
    	            
    	            for (int j=0; j<3; j++) {
    		            readlen = input.read(rdata);     
    		            if (readlen != 0) {
    		            	String rstr = new String(rdata, 0, readlen);
    		            	System.out.println(System.currentTimeMillis()+ " id: " + id +" 服务器端推送过来的是: " + rstr);  
    		            }
    	            }
    	            
    	            out.close();  
    	            input.close();  
    	            socket.close(); 
    	            System.out.println(System.currentTimeMillis()+ " id: " + id +" socket 关闭"); 
    	        } catch (Exception e) {  
    	            System.out.println(System.currentTimeMillis()+ " id: " + id +" 客户端异常:" + e.getMessage());   
    	        }
        	}
    	}
    }
}
