package com.alibaba.dubbo.rpc.cluster.loadbalance;


import java.util.List;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.htdz.common.LogManager;
import com.htdz.common.utils.DataUtil;

/*
 * 设备请求转发采用自定义负载算法
 * 因为设备是通过tcp和网关连接，存在分包和粘包问题，
 * 而网关不处理该问题，由设备服务处理。因此，同一设
 * 备同一个连接发送的数据需要路由到同一个设备服务，
 * deviceSession代表同一个设备的同一个连接，用它来做
 * hash算法，相同的deviceSession路由到相同的设备服务
 * 
 * 在dubbo-2.6.0.jar\META-INF\dubbo\internal\com.alibaba.dubbo.rpc.cluster.LoadBalance
 * 文件中添加devicelb=com.alibaba.dubbo.rpc.cluster.loadbalance.DeviceServiceLoadBalance
 * 并且需要在Reference中指定loadbalance="devicelb"
 */
public class DeviceServiceLoadBalance extends AbstractLoadBalance {
	@Override
	protected <T> Invoker<T> doSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
		LogManager.debug("DeviceServiceLoadBalance {}", invocation.getMethodName());
		for (int i=0; i<invocation.getArguments().length; i++) {
			LogManager.debug("args[{}] {}", i, invocation.getArguments()[i].toString());
		}
		
		// args[2] is String deviceSession
		
		int index = 0;
		try {
			// 预防参数个数不足的情形，实际定义需要关注这个问题
			index = DataUtil.stringToHash(invocation.getArguments()[2].toString()) % invokers.size();
		} catch (Exception e) {
			index = 0;
			LogManager.exception(e.getMessage(), e);
		}
		
		return invokers.get(index);
	}
}
